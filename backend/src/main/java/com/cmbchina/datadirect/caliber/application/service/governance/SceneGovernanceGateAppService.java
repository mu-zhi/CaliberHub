package com.cmbchina.datadirect.caliber.application.service.governance;

import com.cmbchina.datadirect.caliber.application.api.dto.response.governance.GovernanceGapDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.governance.GovernanceRuleResultDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.governance.SceneGovernanceSummaryDTO;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.DictionaryMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.GapTaskMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.IdentifierLineageMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.TimeSemanticSelectorMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.GapTaskPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class SceneGovernanceGateAppService {

    public static final String STAGE_IMPORT_CONFIRM = "IMPORT_CONFIRM";
    public static final String STAGE_PRE_PUBLISH = "PRE_PUBLISH";
    public static final String STATUS_PASSED = "PASSED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_WAIVED = "WAIVED";
    public static final String BLOCKING = "BLOCKING";

    private static final String TASK_TYPE = "GOVERNANCE_RULE";
    private static final Set<String> OPEN_GAP_STATUSES = Set.of("OPEN", "ASSIGNED", "IN_PROGRESS");
    private static final Set<String> ACTIVE_ASSET_STATUSES = Set.of("DRAFT", "ACTIVE", "PUBLISHED");
    private static final Set<String> ACTIVE_RELEASE_STATUSES = Set.of("ACTIVE", "PUBLISHED", "CONFIRMED");

    private final DictionaryMapper dictionaryMapper;
    private final IdentifierLineageMapper identifierLineageMapper;
    private final TimeSemanticSelectorMapper timeSemanticSelectorMapper;
    private final GapTaskMapper gapTaskMapper;

    public SceneGovernanceGateAppService(DictionaryMapper dictionaryMapper,
                                         IdentifierLineageMapper identifierLineageMapper,
                                         TimeSemanticSelectorMapper timeSemanticSelectorMapper,
                                         GapTaskMapper gapTaskMapper) {
        this.dictionaryMapper = dictionaryMapper;
        this.identifierLineageMapper = identifierLineageMapper;
        this.timeSemanticSelectorMapper = timeSemanticSelectorMapper;
        this.gapTaskMapper = gapTaskMapper;
    }

    @Transactional
    public SceneGovernanceSummaryDTO evaluateAndSync(Long sceneId, String stage, String operator) {
        List<GovernanceRuleResultDTO> rules = evaluate(sceneId, stage);
        syncGapTasks(sceneId, stage, operator, rules);
        return buildSummary(sceneId, stage, rules, currentOpenBlockingGaps(sceneId));
    }

    @Transactional(readOnly = true)
    public SceneGovernanceSummaryDTO summarize(Long sceneId, String stage) {
        List<GovernanceRuleResultDTO> rules = evaluate(sceneId, stage);
        return buildSummary(sceneId, stage, rules, currentOpenBlockingGaps(sceneId));
    }

    @Transactional
    public void assertPublishable(Long sceneId, String operator) {
        SceneGovernanceSummaryDTO summary = evaluateAndSync(sceneId, STAGE_PRE_PUBLISH, operator);
        if (summary.publishReady()) {
            return;
        }
        throw new DomainValidationException("发布失败，治理规则未通过：" + summary.summary());
    }

    private List<GovernanceRuleResultDTO> evaluate(Long sceneId, String stage) {
        List<GovernanceRuleResultDTO> rules = new ArrayList<>();
        rules.add(rule(
                "GR-DICT-001",
                stage,
                "字典治理对象",
                hasActiveDictionary(sceneId),
                "场景至少需要 1 个活动中的 Dictionary（字典）"
        ));
        rules.add(rule(
                "GR-IDL-001",
                stage,
                "标识链治理对象",
                hasActiveIdentifierLineage(sceneId),
                "场景至少需要 1 个活动中的 Identifier Lineage（标识链）"
        ));
        rules.add(rule(
                "GR-TIME-001",
                stage,
                "时间语义治理对象",
                hasActiveTimeSemanticSelector(sceneId),
                "场景至少需要 1 个活动中的 Time Semantic Selector（时间语义选择器）"
        ));
        return rules;
    }

    private GovernanceRuleResultDTO rule(String ruleCode, String stage, String name, boolean passed, String failureMessage) {
        return new GovernanceRuleResultDTO(
                ruleCode,
                stage,
                name,
                passed ? STATUS_PASSED : STATUS_FAILED,
                BLOCKING,
                passed ? "已满足" : failureMessage,
                passed
        );
    }

    private SceneGovernanceSummaryDTO buildSummary(Long sceneId,
                                                   String stage,
                                                   List<GovernanceRuleResultDTO> rules,
                                                   List<GovernanceGapDTO> openBlockingGaps) {
        List<GovernanceRuleResultDTO> failedRules = rules.stream()
                .filter(rule -> !rule.passed())
                .toList();
        Set<String> failedRuleCodes = failedRules.stream()
                .map(GovernanceRuleResultDTO::ruleCode)
                .collect(java.util.stream.Collectors.toSet());
        List<GovernanceGapDTO> effectiveOpenBlockingGaps = openBlockingGaps.stream()
                .filter(gap -> {
                    String sourceRef = gap.sourceRef() == null ? "" : gap.sourceRef();
                    int index = sourceRef.lastIndexOf(':');
                    String ruleCode = index >= 0 ? sourceRef.substring(index + 1) : sourceRef;
                    return failedRuleCodes.contains(ruleCode);
                })
                .toList();
        boolean publishReady = failedRules.isEmpty() && effectiveOpenBlockingGaps.isEmpty();
        String summary;
        if (publishReady) {
            summary = "治理规则已通过，当前无阻断级缺口。";
        } else {
            List<String> segments = new ArrayList<>();
            if (!failedRules.isEmpty()) {
                segments.add("失败规则：" + failedRules.stream().map(GovernanceRuleResultDTO::name).distinct().reduce((l, r) -> l + "、" + r).orElse(""));
            }
            if (!effectiveOpenBlockingGaps.isEmpty()) {
                segments.add("阻断缺口：" + effectiveOpenBlockingGaps.stream().map(GovernanceGapDTO::taskTitle).distinct().reduce((l, r) -> l + "、" + r).orElse(""));
            }
            summary = String.join("；", segments);
        }
        return new SceneGovernanceSummaryDTO(sceneId, stage, publishReady, rules, failedRules, effectiveOpenBlockingGaps, summary);
    }

    private void syncGapTasks(Long sceneId, String stage, String operator, List<GovernanceRuleResultDTO> rules) {
        OffsetDateTime now = OffsetDateTime.now();
        String resolvedOperator = normalizeOperator(operator);
        for (GovernanceRuleResultDTO rule : rules) {
            String taskCode = buildTaskCode(sceneId, rule.ruleCode());
            GapTaskPO task = gapTaskMapper.findByTaskCode(taskCode).orElseGet(GapTaskPO::new);
            if (task.getTaskCode() == null) {
                task.setSceneId(sceneId);
                task.setTaskCode(taskCode);
                task.setTaskType(TASK_TYPE);
                task.setCreatedBy(resolvedOperator);
                task.setCreatedAt(now);
            }
            task.setTaskTitle(rule.name() + "缺口");
            task.setSeverity(rule.blockingLevel());
            task.setDetailText(rule.message());
            task.setSourceRef(stage + ":" + rule.ruleCode());
            task.setUpdatedBy(resolvedOperator);
            task.setUpdatedAt(now);
            task.setStatus(rule.passed() ? "VERIFIED" : "OPEN");
            gapTaskMapper.save(task);
        }
    }

    private List<GovernanceGapDTO> currentOpenBlockingGaps(Long sceneId) {
        return gapTaskMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream()
                .filter(task -> TASK_TYPE.equalsIgnoreCase(task.getTaskType()))
                .filter(task -> BLOCKING.equalsIgnoreCase(task.getSeverity()))
                .filter(task -> OPEN_GAP_STATUSES.contains(normalize(task.getStatus())))
                .map(task -> new GovernanceGapDTO(
                        task.getId(),
                        task.getTaskCode(),
                        task.getTaskTitle(),
                        task.getTaskType(),
                        task.getStatus(),
                        task.getSeverity(),
                        task.getDetailText(),
                        task.getSourceRef()
                ))
                .toList();
    }

    private boolean hasActiveDictionary(Long sceneId) {
        return dictionaryMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream()
                .anyMatch(dictionary -> ACTIVE_ASSET_STATUSES.contains(normalize(dictionary.getStatus()))
                        && ACTIVE_RELEASE_STATUSES.contains(normalize(dictionary.getReleaseStatus())));
    }

    private boolean hasActiveIdentifierLineage(Long sceneId) {
        return identifierLineageMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream()
                .anyMatch(lineage -> ACTIVE_ASSET_STATUSES.contains(normalize(lineage.getStatus()))
                        && ACTIVE_RELEASE_STATUSES.contains(normalize(lineage.getConfirmationStatus())));
    }

    private boolean hasActiveTimeSemanticSelector(Long sceneId) {
        return timeSemanticSelectorMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream()
                .anyMatch(selector -> ACTIVE_ASSET_STATUSES.contains(normalize(selector.getStatus()))
                        && selector.getDefaultSemantic() != null
                        && !selector.getDefaultSemantic().isBlank());
    }

    private String buildTaskCode(Long sceneId, String ruleCode) {
        return "GAP-GOV-" + sceneId + "-" + ruleCode;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOperator(String operator) {
        String normalized = operator == null ? "" : operator.trim();
        return normalized.isEmpty() ? "system" : normalized;
    }
}
