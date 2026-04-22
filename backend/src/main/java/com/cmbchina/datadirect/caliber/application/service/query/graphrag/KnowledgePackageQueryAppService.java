package com.cmbchina.datadirect.caliber.application.service.query.graphrag;

import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.KnowledgePackageQueryCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.KnowledgePackageClarificationDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.KnowledgePackageContractDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.KnowledgePackageCoverageDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.KnowledgePackageDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.KnowledgePackageEvidenceDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.KnowledgePackageExperimentDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.KnowledgePackagePathDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.KnowledgePackagePlanDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.KnowledgePackagePolicyDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.KnowledgePackageRiskDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.KnowledgePackageSceneDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.KnowledgePackageTraceDTO;
import com.cmbchina.datadirect.caliber.application.support.RetrievalExperimentSupport;
import com.cmbchina.datadirect.caliber.application.service.command.graphrag.ExperimentalRetrievalIndexSyncService;
import com.cmbchina.datadirect.caliber.application.service.command.graphrag.GraphAuditEventAppService;
import com.cmbchina.datadirect.caliber.application.service.graphrag.GraphAssetSupport;
import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneVersionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ContractViewMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CoverageDeclarationMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.InputSlotSchemaMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanEvidenceRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanPolicyRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PolicyMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneVersionPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.ContractViewPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CoverageDeclarationPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EvidenceFragmentPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.InputSlotSchemaPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.OutputContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanEvidenceRefPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPolicyRefPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PolicyPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceContractPO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class KnowledgePackageQueryAppService {

    private static final List<String> PAYROLL_KEYWORDS = List.of("代发", "工资", "薪资", "payroll");
    private static final List<String> DETAIL_KEYWORDS = List.of("明细", "协议", "协议号", "protocol", "detail");
    private static final List<String> BATCH_KEYWORDS = List.of("批次", "公司户", "批量", "汇总", "结果", "batch");

    private final SceneMapper sceneMapper;
    private final SceneVersionMapper sceneVersionMapper;
    private final PlanMapper planMapper;
    private final CoverageDeclarationMapper coverageDeclarationMapper;
    private final OutputContractMapper outputContractMapper;
    private final ContractViewMapper contractViewMapper;
    private final SourceContractMapper sourceContractMapper;
    private final PlanPolicyRefMapper planPolicyRefMapper;
    private final PolicyMapper policyMapper;
    private final PlanEvidenceRefMapper planEvidenceRefMapper;
    private final EvidenceFragmentMapper evidenceFragmentMapper;
    private final InputSlotSchemaMapper inputSlotSchemaMapper;
    private final GraphAssetSupport graphAssetSupport;
    private final GraphAuditEventAppService graphAuditEventAppService;
    private final RetrievalExperimentSupport retrievalExperimentSupport;
    private final ExperimentalRetrievalIndexSyncService experimentalRetrievalIndexSyncService;

    public KnowledgePackageQueryAppService(SceneMapper sceneMapper,
                                           SceneVersionMapper sceneVersionMapper,
                                           PlanMapper planMapper,
                                           CoverageDeclarationMapper coverageDeclarationMapper,
                                           OutputContractMapper outputContractMapper,
                                           ContractViewMapper contractViewMapper,
                                           SourceContractMapper sourceContractMapper,
                                           PlanPolicyRefMapper planPolicyRefMapper,
                                           PolicyMapper policyMapper,
                                           PlanEvidenceRefMapper planEvidenceRefMapper,
                                           EvidenceFragmentMapper evidenceFragmentMapper,
                                           InputSlotSchemaMapper inputSlotSchemaMapper,
                                           GraphAssetSupport graphAssetSupport,
                                           GraphAuditEventAppService graphAuditEventAppService,
                                           RetrievalExperimentSupport retrievalExperimentSupport,
                                           ExperimentalRetrievalIndexSyncService experimentalRetrievalIndexSyncService) {
        this.sceneMapper = sceneMapper;
        this.sceneVersionMapper = sceneVersionMapper;
        this.planMapper = planMapper;
        this.coverageDeclarationMapper = coverageDeclarationMapper;
        this.outputContractMapper = outputContractMapper;
        this.contractViewMapper = contractViewMapper;
        this.sourceContractMapper = sourceContractMapper;
        this.planPolicyRefMapper = planPolicyRefMapper;
        this.policyMapper = policyMapper;
        this.planEvidenceRefMapper = planEvidenceRefMapper;
        this.evidenceFragmentMapper = evidenceFragmentMapper;
        this.inputSlotSchemaMapper = inputSlotSchemaMapper;
        this.graphAssetSupport = graphAssetSupport;
        this.graphAuditEventAppService = graphAuditEventAppService;
        this.retrievalExperimentSupport = retrievalExperimentSupport;
        this.experimentalRetrievalIndexSyncService = experimentalRetrievalIndexSyncService;
    }

    public KnowledgePackageDTO query(KnowledgePackageQueryCmd cmd) {
        String traceId = "KP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        String operator = safe(cmd.operator()).isBlank() ? "system" : safe(cmd.operator());
        String queryText = safe(cmd.queryText());
        String identifierType = normalizeIdentifierType(cmd.identifierType());
        String identifierValue = safe(cmd.identifierValue());
        LocalDate dateFrom = graphAssetSupport.parseDate(cmd.dateFrom());
        LocalDate dateTo = graphAssetSupport.parseDate(cmd.dateTo());
        List<String> requestedFields = normalizeFields(cmd.requestedFields());
        RetrievalExperimentSupport.RetrievalExperimentResult retrievalExperiment = retrieveExperiment(
                traceId,
                cmd,
                queryText,
                identifierType,
                identifierValue,
                requestedFields
        );

        SceneRecallResult sceneRecall = recallScene(cmd, identifierType, queryText);
        if (sceneRecall.requiresClarification()) {
            return clarification(traceId, sceneRecall.candidates(), operator, retrievalExperiment);
        }

        if (identifierValue.isBlank() && queryText.isBlank()) {
            return deny(traceId, null, null, identifierType, identifierValue, null, null, null,
                    "GAP", null, null, List.of(),
                    "IDENTIFIER_REQUIRED", "INPUT_INVALID", List.of("IDENTIFIER_REQUIRED"), "HIGH", List.of("标识值不能为空"), operator, retrievalExperiment);
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            return deny(traceId, null, null, identifierType, identifierValue, null, null, null,
                    "GAP", null, null, List.of(),
                    "INVALID_DATE_RANGE", "INPUT_INVALID", List.of("INVALID_DATE_RANGE"), "HIGH", List.of("查询起始日期晚于结束日期"), operator, retrievalExperiment);
        }

        ScenePO scene = sceneRecall.scene();
        if (scene == null) {
            return deny(traceId, null, null, identifierType, identifierValue, null, null, null,
                    "GAP", null, null, List.of(),
                    "SCENE_NOT_FOUND", "NO_MATCH_SCENE", List.of("SCENE_NOT_FOUND"), "HIGH", List.of("未找到可用的代发明细已发布场景"), operator, retrievalExperiment);
        }
        SceneVersionPO latestVersion = resolveSnapshot(scene.getId(), cmd.snapshotId());
        Long snapshotId = latestVersion == null ? null : latestVersion.getId();
        if (scene != null && snapshotId != null) {
            experimentalRetrievalIndexSyncService.ensureSnapshotLock(scene.getId(), snapshotId, scene.getSceneCode(), operator);
        }
        if (identifierValue.isBlank()) {
            return deny(traceId, scene, null, identifierType, identifierValue, null, null, latestVersion,
                    "GAP", null, null, List.of(),
                    "IDENTIFIER_REQUIRED", "INPUT_INVALID", List.of("IDENTIFIER_REQUIRED"), "HIGH", List.of("标识值不能为空"), operator, retrievalExperiment);
        }

        List<PlanPO> plans = publishedPlans(scene.getId(), snapshotId).stream()
                .filter(plan -> cmd.selectedPlanId() == null || cmd.selectedPlanId().equals(plan.getId()))
                .sorted(Comparator.comparing(PlanPO::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        OutputContractPO outputContract = resolveOutputContract(scene.getId(), snapshotId);

        MatchResult match = matchPlan(plans, dateFrom, dateTo);
        if (match.plan() == null) {
            return deny(traceId, scene, null, identifierType, identifierValue, outputContract, null, latestVersion,
                    match.coverageStatus(), match.segmentLabel(), match.coverageExplanation(), List.of(),
                    match.reasonCode(), deriveRuntimeMode("deny", match.coverageStatus(), match.reasonCode()),
                    normalizeDegradeReasonCodes(match.reasonCode()), riskLevel("deny"),
                    List.of(match.coverageExplanation()), operator, retrievalExperiment);
        }

        ContractViewPO contractView = resolveContractView(scene.getId(), match.plan(), snapshotId);
        List<SourceContractPO> matchedSourceContracts = resolveMatchedSourceContracts(scene.getId(), match.plan(), identifierType, snapshotId);
        List<EvidenceFragmentPO> evidences = resolveEvidence(match.plan(), snapshotId);
        List<PolicyPO> policies = resolvePolicies(scene.getId(), match.plan(), snapshotId);
        FieldDecision fieldDecision = evaluateFields(outputContract, contractView, requestedFields);
        String decision = evaluateDecision(match.coverageStatus(), policies, fieldDecision);
        String reasonCode = resolveReasonCode(match.coverageStatus(), fieldDecision, policies, decision);
        List<String> riskReasons = buildRiskReasons(match.coverageStatus(), fieldDecision, policies, match.coverageExplanation());

        String runtimeMode = deriveRuntimeMode(decision, match.coverageStatus(), reasonCode);
        List<String> degradeReasonCodes = normalizeDegradeReasonCodes(reasonCode, riskReasons);
        KnowledgePackageDTO result = buildResult(
                decision,
                reasonCode,
                runtimeMode,
                degradeReasonCodes,
                scene,
                match.plan(),
                identifierType,
                identifierValue,
                outputContract,
                contractView,
                fieldDecision,
                match.coverageStatus(),
                match.segmentLabel(),
                match.coverageExplanation(),
                matchedSourceContracts,
                policies,
                evidences,
                latestVersion,
                retrievalExperiment,
                buildResolutionSteps(identifierType, match.plan(), reasonCode),
                riskLevel(decision),
                riskReasons
        );
        result = withTrace(result, traceId);
        recordAudit(scene.getId(), traceId, latestVersion, operator, reasonCode, result);
        return result;
    }

    private SceneRecallResult recallScene(KnowledgePackageQueryCmd cmd, String identifierType, String queryText) {
        if (cmd.selectedSceneId() != null) {
            return publishedPayrollScenes().stream()
                    .filter(item -> cmd.selectedSceneId().equals(item.scene().getId()))
                    .findFirst()
                    .map(item -> new SceneRecallResult(item.scene(), List.of(item), false))
                    .orElse(new SceneRecallResult(null, List.of(), false));
        }

        List<SceneCandidate> candidates = publishedPayrollScenes().stream()
                .filter(item -> identifierType.isBlank()
                        || supportsIdentifier(item.scene().getId(), identifierType, item.snapshotId())
                        || scoreScene(item.scene(), queryText) > 0)
                .sorted(Comparator
                        .comparing((SceneCandidate item) -> scoreScene(item.scene(), queryText), Comparator.reverseOrder())
                        .thenComparing(item -> item.scene().getUpdatedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        boolean detailIntent = detailIntent(queryText, identifierType);
        boolean batchIntent = batchIntent(queryText, identifierType);
        if (detailIntent && batchIntent) {
            List<SceneCandidate> clarificationCandidates = new ArrayList<>();
            candidates.stream().filter(item -> isDetailScene(item.scene())).findFirst().ifPresent(clarificationCandidates::add);
            candidates.stream().filter(item -> isBatchScene(item.scene())).findFirst().ifPresent(clarificationCandidates::add);
            if (clarificationCandidates.isEmpty()) {
                clarificationCandidates = candidates.stream().limit(2).toList();
            }
            return new SceneRecallResult(null, clarificationCandidates, true);
        }

        if (detailIntent) {
            ScenePO detailScene = candidates.stream()
                    .map(SceneCandidate::scene)
                    .filter(this::isDetailScene)
                    .findFirst()
                    .orElse(null);
            return new SceneRecallResult(detailScene, candidates, false);
        }
        if (batchIntent) {
            ScenePO batchScene = candidates.stream()
                    .map(SceneCandidate::scene)
                    .filter(this::isBatchScene)
                    .findFirst()
                    .orElse(null);
            return new SceneRecallResult(batchScene, candidates, false);
        }
        return new SceneRecallResult(candidates.isEmpty() ? null : candidates.get(0).scene(), candidates, false);
    }

    private List<SceneCandidate> publishedPayrollScenes() {
        return sceneMapper.findAll().stream()
                .filter(scene -> scene.getStatus() == SceneStatus.PUBLISHED)
                .filter(this::isPayrollScene)
                .map(scene -> {
                    SceneVersionPO latestVersion = sceneVersionMapper.findTopBySceneIdOrderByVersionNoDesc(scene.getId()).orElse(null);
                    return latestVersion == null ? null : new SceneCandidate(scene, latestVersion);
                })
                .filter(item -> item != null)
                .toList();
    }

    private SceneVersionPO resolveSnapshot(Long sceneId, Long requestedSnapshotId) {
        if (requestedSnapshotId != null) {
            return sceneVersionMapper.findById(requestedSnapshotId)
                    .filter(version -> sceneId.equals(version.getSceneId()))
                    .orElse(null);
        }
        return sceneVersionMapper.findTopBySceneIdOrderByVersionNoDesc(sceneId).orElse(null);
    }

    private boolean isPayrollScene(ScenePO scene) {
        String text = (safe(scene.getSceneTitle()) + " "
                + safe(scene.getSceneCode()) + " "
                + safe(scene.getSceneType()) + " "
                + safe(scene.getSceneDescription()) + " "
                + safe(scene.getRawInput()))
                .toLowerCase(Locale.ROOT);
        return PAYROLL_KEYWORDS.stream().anyMatch(text::contains);
    }

    private boolean isDetailScene(ScenePO scene) {
        String text = sceneKeywords(scene);
        return DETAIL_KEYWORDS.stream().anyMatch(text::contains)
                || "FACT_DETAIL".equalsIgnoreCase(safe(scene.getSceneType()));
    }

    private boolean isBatchScene(ScenePO scene) {
        String text = sceneKeywords(scene);
        return BATCH_KEYWORDS.stream().anyMatch(text::contains)
                || "FACT_AGGREGATION".equalsIgnoreCase(safe(scene.getSceneType()));
    }

    private int scoreScene(ScenePO scene, String queryText) {
        String query = safe(queryText).toLowerCase(Locale.ROOT);
        if (query.isBlank()) {
            return 0;
        }
        String sceneText = sceneKeywords(scene);
        int score = 0;
        for (String keyword : DETAIL_KEYWORDS) {
            if (query.contains(keyword) && sceneText.contains(keyword)) {
                score += 2;
            }
        }
        for (String keyword : BATCH_KEYWORDS) {
            if (query.contains(keyword) && sceneText.contains(keyword)) {
                score += 2;
            }
        }
        if (query.contains("协议") && isDetailScene(scene)) {
            score += 2;
        }
        if (query.contains("公司户") && isBatchScene(scene)) {
            score += 2;
        }
        return score;
    }

    private String sceneKeywords(ScenePO scene) {
        return (safe(scene.getSceneTitle()) + " "
                + safe(scene.getSceneCode()) + " "
                + safe(scene.getSceneType()) + " "
                + safe(scene.getSceneDescription()) + " "
                + safe(scene.getRawInput()))
                .toLowerCase(Locale.ROOT);
    }

    private boolean detailIntent(String queryText, String identifierType) {
        String query = safe(queryText).toLowerCase(Locale.ROOT);
        return "PROTOCOL_NBR".equals(identifierType)
                || "CUST_ID".equals(identifierType)
                || DETAIL_KEYWORDS.stream().anyMatch(query::contains);
    }

    private boolean batchIntent(String queryText, String identifierType) {
        String query = safe(queryText).toLowerCase(Locale.ROOT);
        return "ORG_ACCOUNT".equals(identifierType)
                || "BATCH_NBR".equals(identifierType)
                || BATCH_KEYWORDS.stream().anyMatch(query::contains);
    }

    private boolean supportsIdentifier(Long sceneId, String identifierType, Long snapshotId) {
        return publishedInputSlots(sceneId, snapshotId).stream()
                .anyMatch(slot -> supportsIdentifier(slot, identifierType));
    }

    private boolean supportsIdentifier(InputSlotSchemaPO slot, String identifierType) {
        if (slot == null) {
            return false;
        }
        if (safe(slot.getSlotCode()).toUpperCase(Locale.ROOT).contains(identifierType)) {
            return true;
        }
        if (safe(slot.getSlotName()).toUpperCase(Locale.ROOT).contains(identifierType)) {
            return true;
        }
        return graphAssetSupport.parseStringList(slot.getIdentifierCandidatesJson()).stream()
                .map(item -> item.toUpperCase(Locale.ROOT))
                .anyMatch(item -> item.contains(identifierType));
    }

    private MatchResult matchPlan(List<PlanPO> plans, LocalDate dateFrom, LocalDate dateTo) {
        if (plans.isEmpty()) {
            return new MatchResult(null, "GAP", "NO_PLAN", "PLAN_NOT_FOUND", "未找到已发布方案");
        }
        LocalDate from = dateFrom != null ? dateFrom : (dateTo != null ? dateTo : LocalDate.now());
        LocalDate to = dateTo != null ? dateTo : from;

        List<SegmentHit> hits = new ArrayList<>();
        List<SegmentHit> overlappingEffectiveHits = new ArrayList<>();
        for (PlanPO plan : plans) {
            for (CoverageDeclarationPO coverage : publishedCoverages(plan.getId(), plan.getSnapshotId())) {
                if (!coverage.isActive()) {
                    continue;
                }
                if (overlaps(coverage, from, to) && !"GAP".equalsIgnoreCase(coverage.getCoverageStatus())) {
                    overlappingEffectiveHits.add(new SegmentHit(plan, coverage));
                }
                if (contains(coverage, from, to)) {
                    hits.add(new SegmentHit(plan, coverage));
                }
            }
        }

        List<SegmentHit> effectiveHits = hits.stream()
                .filter(hit -> !"GAP".equalsIgnoreCase(hit.coverage().getCoverageStatus()))
                .toList();
        long distinctPlanCount = effectiveHits.stream().map(hit -> hit.plan().getId()).distinct().count();
        if (distinctPlanCount > 1) {
            return new MatchResult(null, "PARTIAL", "跨方案时段", "CROSS_PLAN_RANGE_UNSUPPORTED", "时间范围跨越多个方案覆盖分段");
        }
        if (!effectiveHits.isEmpty()) {
            SegmentHit hit = effectiveHits.stream()
                    .sorted(Comparator.comparing((SegmentHit item) -> coveragePriority(item.coverage().getCoverageStatus()))
                            .thenComparing(item -> item.plan().getUpdatedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                    .findFirst()
                    .orElseThrow();
            return new MatchResult(hit.plan(), normalizedCoverage(hit.coverage()), segmentLabel(hit.coverage()), "OK", coverageExplanation(hit.coverage()));
        }
        long overlappingPlanCount = overlappingEffectiveHits.stream().map(hit -> hit.plan().getId()).distinct().count();
        if (overlappingPlanCount > 1) {
            return new MatchResult(null, "PARTIAL", "跨方案时段", "CROSS_PLAN_RANGE_UNSUPPORTED", "时间范围跨越多个方案覆盖分段");
        }
        SegmentHit gapHit = hits.stream().findFirst().orElse(null);
        if (gapHit != null) {
            return new MatchResult(gapHit.plan(), "GAP", segmentLabel(gapHit.coverage()), "COVERAGE_GAP", coverageExplanation(gapHit.coverage()));
        }
        return new MatchResult(null, "GAP", "未命中覆盖分段", "COVERAGE_GAP", "请求时间范围未命中已声明覆盖");
    }

    private int coveragePriority(String coverageStatus) {
        if ("FULL".equalsIgnoreCase(coverageStatus)) {
            return 0;
        }
        if ("PARTIAL".equalsIgnoreCase(coverageStatus)) {
            return 1;
        }
        return 2;
    }

    private String normalizedCoverage(CoverageDeclarationPO coverage) {
        return coverage == null ? "GAP" : safe(coverage.getCoverageStatus()).isBlank() ? "FULL" : coverage.getCoverageStatus().trim().toUpperCase(Locale.ROOT);
    }

    private boolean contains(CoverageDeclarationPO coverage, LocalDate from, LocalDate to) {
        LocalDate start = coverage.getStartDate();
        LocalDate end = coverage.getEndDate();
        boolean afterStart = start == null || !from.isBefore(start);
        boolean beforeEnd = end == null || !to.isAfter(end);
        return afterStart && beforeEnd;
    }

    private boolean overlaps(CoverageDeclarationPO coverage, LocalDate from, LocalDate to) {
        LocalDate start = coverage.getStartDate();
        LocalDate end = coverage.getEndDate();
        boolean startsBeforeRangeEnd = start == null || !start.isAfter(to);
        boolean endsAfterRangeStart = end == null || !end.isBefore(from);
        return startsBeforeRangeEnd && endsAfterRangeStart;
    }

    private String segmentLabel(CoverageDeclarationPO coverage) {
        if (coverage == null) {
            return "未命中覆盖分段";
        }
        if (!safe(coverage.getApplicablePeriod()).isBlank()) {
            return safe(coverage.getApplicablePeriod());
        }
        if (!safe(coverage.getCoverageTitle()).isBlank()) {
            return safe(coverage.getCoverageTitle());
        }
        return safe(coverage.getCoverageCode());
    }

    private String coverageExplanation(CoverageDeclarationPO coverage) {
        if (coverage == null) {
            return "未命中覆盖声明";
        }
        if (!safe(coverage.getGapText()).isBlank()) {
            return safe(coverage.getGapText());
        }
        if (!safe(coverage.getStatementText()).isBlank()) {
            return safe(coverage.getStatementText());
        }
        return segmentLabel(coverage);
    }

    private OutputContractPO resolveOutputContract(Long sceneId, Long snapshotId) {
        return publishedOutputContracts(sceneId, snapshotId).stream().findFirst().orElse(null);
    }

    private ContractViewPO resolveContractView(Long sceneId, PlanPO plan, Long snapshotId) {
        List<ContractViewPO> views = publishedContractViews(sceneId, snapshotId);
        if (plan != null) {
            ContractViewPO planView = views.stream()
                    .filter(view -> plan.getId().equals(view.getPlanId()))
                    .findFirst()
                    .orElse(null);
            if (planView != null) {
                return planView;
            }
        }
        return views.stream().findFirst().orElse(null);
    }

    private List<SourceContractPO> resolveMatchedSourceContracts(Long sceneId, PlanPO plan, String identifierType, Long snapshotId) {
        List<SourceContractPO> contracts = publishedSourceContracts(sceneId, snapshotId);
        LinkedHashSet<SourceContractPO> matched = new LinkedHashSet<>();
        if ("CUST_ID".equals(identifierType)) {
            contracts.stream()
                    .filter(contract -> "CUST_ID".equalsIgnoreCase(safe(contract.getIdentifierType()))
                            || safe(contract.getSourceRole()).toUpperCase(Locale.ROOT).contains("IDENTIFIER"))
                    .findFirst()
                    .ifPresent(matched::add);
        }
        if (plan != null) {
            contracts.stream()
                    .filter(contract -> plan.getId().equals(contract.getPlanId()))
                    .forEach(matched::add);
        }
        return new ArrayList<>(matched);
    }

    private List<EvidenceFragmentPO> resolveEvidence(PlanPO plan, Long snapshotId) {
        if (plan == null) {
            return List.of();
        }
        return planEvidenceRefMapper.findByPlanId(plan.getId()).stream()
                .map(PlanEvidenceRefPO::getEvidenceId)
                .map(id -> evidenceFragmentMapper.findById(id).orElse(null))
                .filter(item -> item != null)
                .filter(item -> matchesSnapshot(item.getSnapshotId(), snapshotId))
                .toList();
    }

    private List<PolicyPO> resolvePolicies(Long sceneId, PlanPO plan, Long snapshotId) {
        LinkedHashSet<PolicyPO> policies = new LinkedHashSet<>();
        if (plan != null) {
            for (PlanPolicyRefPO ref : planPolicyRefMapper.findByPlanId(plan.getId())) {
                policyMapper.findById(ref.getPolicyId())
                        .filter(policy -> isRuntimePolicy(policy, snapshotId))
                        .ifPresent(policies::add);
            }
            policyMapper.findAll().stream()
                    .filter(policy -> "PLAN".equalsIgnoreCase(policy.getScopeType()) && plan.getId().equals(policy.getScopeRefId()))
                    .filter(policy -> isRuntimePolicy(policy, snapshotId))
                    .forEach(policies::add);
        }
        policyMapper.findAll().stream()
                .filter(policy -> "SCENE".equalsIgnoreCase(policy.getScopeType()) && sceneId.equals(policy.getScopeRefId()))
                .filter(policy -> isRuntimePolicy(policy, snapshotId))
                .forEach(policies::add);
        return new ArrayList<>(policies);
    }

    private List<PlanPO> publishedPlans(Long sceneId, Long snapshotId) {
        return planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream()
                .filter(plan -> "PUBLISHED".equalsIgnoreCase(plan.getStatus()))
                .filter(plan -> matchesSnapshot(plan.getSnapshotId(), snapshotId))
                .toList();
    }

    private List<InputSlotSchemaPO> publishedInputSlots(Long sceneId, Long snapshotId) {
        return inputSlotSchemaMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream()
                .filter(slot -> "PUBLISHED".equalsIgnoreCase(slot.getStatus()))
                .filter(slot -> matchesSnapshot(slot.getSnapshotId(), snapshotId))
                .toList();
    }

    private List<CoverageDeclarationPO> publishedCoverages(Long planId, Long snapshotId) {
        return coverageDeclarationMapper.findByPlanIdOrderByUpdatedAtDesc(planId).stream()
                .filter(coverage -> "PUBLISHED".equalsIgnoreCase(coverage.getStatus()) || "ACTIVE".equalsIgnoreCase(coverage.getStatus()))
                .filter(coverage -> matchesSnapshot(coverage.getSnapshotId(), snapshotId))
                .toList();
    }

    private List<OutputContractPO> publishedOutputContracts(Long sceneId, Long snapshotId) {
        return outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream()
                .filter(contract -> "PUBLISHED".equalsIgnoreCase(contract.getStatus()))
                .filter(contract -> matchesSnapshot(contract.getSnapshotId(), snapshotId))
                .toList();
    }

    private List<ContractViewPO> publishedContractViews(Long sceneId, Long snapshotId) {
        return contractViewMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream()
                .filter(view -> "PUBLISHED".equalsIgnoreCase(view.getStatus()) || "ACTIVE".equalsIgnoreCase(view.getStatus()))
                .filter(view -> matchesSnapshot(view.getSnapshotId(), snapshotId))
                .toList();
    }

    private List<SourceContractPO> publishedSourceContracts(Long sceneId, Long snapshotId) {
        return sourceContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream()
                .filter(contract -> "PUBLISHED".equalsIgnoreCase(contract.getStatus()) || "ACTIVE".equalsIgnoreCase(contract.getStatus()))
                .filter(contract -> matchesSnapshot(contract.getSnapshotId(), snapshotId))
                .toList();
    }

    private boolean isRuntimePolicy(PolicyPO policy, Long snapshotId) {
        if (policy == null) {
            return false;
        }
        boolean active = "ACTIVE".equalsIgnoreCase(policy.getStatus()) || "PUBLISHED".equalsIgnoreCase(policy.getStatus());
        return active && matchesSnapshot(policy.getSnapshotId(), snapshotId);
    }

    private boolean matchesSnapshot(Long assetSnapshotId, Long snapshotId) {
        if (snapshotId == null) {
            return assetSnapshotId == null;
        }
        return snapshotId.equals(assetSnapshotId);
    }

    private FieldDecision evaluateFields(OutputContractPO outputContract, ContractViewPO contractView, List<String> requestedFields) {
        List<String> visible = contractView == null
                ? fallbackVisibleFields(outputContract)
                : graphAssetSupport.parseStringList(contractView.getVisibleFieldsJson());
        List<String> masked = contractView == null ? List.of() : graphAssetSupport.parseStringList(contractView.getMaskedFieldsJson());
        List<String> restricted = contractView == null ? List.of() : graphAssetSupport.parseStringList(contractView.getRestrictedFieldsJson());
        List<String> forbidden = contractView == null ? List.of() : graphAssetSupport.parseStringList(contractView.getForbiddenFieldsJson());
        boolean hasForbidden = requestedFields.stream().anyMatch(field -> containsIgnoreCase(forbidden, field));
        boolean hasRestricted = requestedFields.stream().anyMatch(field -> containsIgnoreCase(restricted, field));
        return new FieldDecision(visible, masked, restricted, forbidden, hasRestricted, hasForbidden);
    }

    private List<String> fallbackVisibleFields(OutputContractPO outputContract) {
        if (outputContract == null || safe(outputContract.getFieldsJson()).isBlank()) {
            return List.of();
        }
        JsonNode node = graphAssetSupport.parseJson(outputContract.getFieldsJson(), "[]");
        if (!node.isArray()) {
            return List.of();
        }
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        node.forEach(item -> {
            if (item.isTextual()) {
                String value = safe(item.asText());
                if (!value.isBlank()) {
                    fields.add(value);
                }
                return;
            }
            if (item.isObject()) {
                for (String key : List.of("fieldCode", "fieldName", "code", "name", "columnName")) {
                    String value = safe(item.path(key).asText());
                    if (!value.isBlank()) {
                        fields.add(value);
                        break;
                    }
                }
            }
        });
        return new ArrayList<>(fields);
    }

    private boolean containsIgnoreCase(List<String> values, String target) {
        return values.stream().anyMatch(item -> item.equalsIgnoreCase(target));
    }

    private String evaluateDecision(String coverageStatus, List<PolicyPO> policies, FieldDecision fieldDecision) {
        if ("GAP".equalsIgnoreCase(coverageStatus) || fieldDecision.hasForbidden() || hasDenyPolicy(policies)) {
            return "deny";
        }
        if ("PARTIAL".equalsIgnoreCase(coverageStatus) || fieldDecision.hasRestricted() || hasApprovalPolicy(policies)) {
            return "need_approval";
        }
        return "allow";
    }

    private boolean hasDenyPolicy(List<PolicyPO> policies) {
        return policies.stream().anyMatch(policy ->
                "DENY".equalsIgnoreCase(policy.getEffectType()) || "S4".equalsIgnoreCase(policy.getSensitivityLevel()));
    }

    private boolean hasApprovalPolicy(List<PolicyPO> policies) {
        return policies.stream().anyMatch(policy ->
                "REQUIRE_APPROVAL".equalsIgnoreCase(policy.getEffectType()) || "S3".equalsIgnoreCase(policy.getSensitivityLevel()));
    }

    private String resolveReasonCode(String coverageStatus, FieldDecision fieldDecision, List<PolicyPO> policies, String decision) {
        if (fieldDecision.hasForbidden()) {
            return "FIELD_FORBIDDEN";
        }
        if (hasDenyPolicy(policies)) {
            return "POLICY_DENY";
        }
        if ("GAP".equalsIgnoreCase(coverageStatus)) {
            return "COVERAGE_GAP";
        }
        if ("PARTIAL".equalsIgnoreCase(coverageStatus) && "need_approval".equalsIgnoreCase(decision)) {
            return "PARTIAL_COVERAGE_APPROVAL";
        }
        if (fieldDecision.hasRestricted()) {
            return "FIELD_RESTRICTED";
        }
        if (hasApprovalPolicy(policies)) {
            return "APPROVAL_REQUIRED";
        }
        return "ALLOW";
    }

    private List<String> buildRiskReasons(String coverageStatus,
                                          FieldDecision fieldDecision,
                                          List<PolicyPO> policies,
                                          String coverageExplanation) {
        List<String> reasons = new ArrayList<>();
        if ("PARTIAL".equalsIgnoreCase(coverageStatus)) {
            reasons.add("命中部分覆盖分段，需要人工确认历史完整性");
        }
        if ("GAP".equalsIgnoreCase(coverageStatus) && !safe(coverageExplanation).isBlank()) {
            reasons.add(coverageExplanation);
        }
        if (fieldDecision.hasRestricted()) {
            reasons.add("请求字段命中受限字段，需要审批");
        }
        if (fieldDecision.hasForbidden()) {
            reasons.add("请求字段命中禁止字段，当前直接拒绝");
        }
        policies.stream()
                .filter(policy -> "REQUIRE_APPROVAL".equalsIgnoreCase(policy.getEffectType()) || "DENY".equalsIgnoreCase(policy.getEffectType()))
                .map(PolicyPO::getPolicyName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .forEach(name -> reasons.add("命中策略：" + name));
        if (reasons.isEmpty()) {
            reasons.add("规则判定完成");
        }
        return reasons;
    }

    private List<String> buildResolutionSteps(String identifierType, PlanPO plan, String reasonCode) {
        List<String> steps = new ArrayList<>();
        if ("CUST_ID".equals(identifierType)) {
            steps.add("通过客户号入口来源先解析协议号（首刀仅输出解析路径，不执行真实映射）");
        } else {
            steps.add("使用协议号直达代发明细查询路径");
        }
        if (plan != null) {
            steps.add("按时间范围匹配方案：" + safe(plan.getPlanName()));
        }
        if ("FIELD_RESTRICTED".equals(reasonCode) || "PARTIAL_COVERAGE_APPROVAL".equals(reasonCode) || "APPROVAL_REQUIRED".equals(reasonCode)) {
            steps.add("按契约视图裁剪输出字段，并标记审批要求");
        } else if ("FIELD_FORBIDDEN".equals(reasonCode)) {
            steps.add("请求字段命中禁止字段，当前不返回知识包结果");
        } else {
            steps.add("按契约视图裁剪输出字段");
        }
        return steps;
    }

    private KnowledgePackageDTO buildResult(String decision,
                                            String reasonCode,
                                            String runtimeMode,
                                            List<String> degradeReasonCodes,
                                            ScenePO scene,
                                            PlanPO plan,
                                            String identifierType,
                                            String identifierValue,
                                            OutputContractPO outputContract,
                                            ContractViewPO contractView,
                                            FieldDecision fieldDecision,
                                            String coverageStatus,
                                            String matchedSegment,
                                            String coverageExplanation,
                                            List<SourceContractPO> matchedSourceContracts,
                                            List<PolicyPO> policies,
                                            List<EvidenceFragmentPO> evidences,
                                            SceneVersionPO latestVersion,
                                            RetrievalExperimentSupport.RetrievalExperimentResult retrievalExperiment,
                                            List<String> resolutionSteps,
                                            String riskLevel,
                                            List<String> riskReasons) {
        String sensitivity = policies.stream()
                .map(PolicyPO::getSensitivityLevel)
                .filter(level -> level != null && !level.isBlank())
                .max(String::compareTo)
                .orElse("S1");
        String maskingPlan = policies.stream()
                .map(PolicyPO::getMaskingRule)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");

        return new KnowledgePackageDTO(
                decision,
                reasonCode,
                runtimeMode,
                normalizeDegradeReasonCodes(reasonCode, degradeReasonCodes),
                scene == null ? null : new KnowledgePackageSceneDTO(
                        scene.getId(),
                        scene.getSceneCode(),
                        scene.getSceneTitle(),
                        scene.getSceneType()
                ),
                plan == null ? null : new KnowledgePackagePlanDTO(
                        plan.getId(),
                        plan.getPlanCode(),
                        plan.getPlanName(),
                        "CUST_ID".equals(identifierType) ? "PROTOCOL_NBR" : identifierType,
                        identifierValue
                ),
                new KnowledgePackageContractDTO(
                        outputContract == null ? null : outputContract.getContractCode(),
                        contractView == null ? null : contractView.getViewCode(),
                        fieldDecision.visibleFields(),
                        fieldDecision.maskedFields(),
                        fieldDecision.restrictedFields(),
                        fieldDecision.forbiddenFields()
                ),
                new KnowledgePackageCoverageDTO(
                        coverageStatus,
                        matchedSegment,
                        matchedSourceContracts.stream().map(SourceContractPO::getSourceContractCode).toList(),
                        coverageExplanation
                ),
                new KnowledgePackagePolicyDTO(
                        decision,
                        sensitivity,
                        "need_approval".equalsIgnoreCase(decision),
                        maskingPlan
                ),
                new KnowledgePackagePathDTO(
                        resolutionSteps,
                        matchedSourceContracts.stream().map(SourceContractPO::getSourceContractCode).toList()
                ),
                evidences.stream()
                        .map(evidence -> new KnowledgePackageEvidenceDTO(
                                evidence.getEvidenceCode(),
                                evidence.getTitle(),
                                evidence.getSourceAnchor(),
                                "FORMAL_PLAN_EVIDENCE",
                                evidence.getSourceAnchor(),
                                evidence.getConfidenceScore()
                        ))
                        .toList(),
                new KnowledgePackageRiskDTO(riskLevel, riskReasons),
                new KnowledgePackageTraceDTO(
                        null,
                        latestVersion == null ? null : latestVersion.getId(),
                        latestVersion == null ? null : latestVersion.getId(),
                        latestVersion == null ? null : latestVersion.getVersionTag(),
                        retrievalExperiment == null ? null : retrievalExperiment.adapterName(),
                        retrievalExperiment == null ? null : retrievalExperiment.status(),
                        retrievalExperiment != null && retrievalExperiment.fallbackToFormal()
                ),
                toExperiment(retrievalExperiment),
                null
        );
    }

    private KnowledgePackageDTO clarification(String traceId,
                                              List<SceneCandidate> candidates,
                                              String operator,
                                              RetrievalExperimentSupport.RetrievalExperimentResult retrievalExperiment) {
        List<SceneCandidate> selectedCandidates = candidates.stream().limit(2).toList();

        List<KnowledgePackageClarificationDTO.SceneCandidateDTO> sceneCandidateDtos = new ArrayList<>();
        List<KnowledgePackageClarificationDTO.PlanCandidateDTO> planCandidateDtos = new ArrayList<>();
        for (SceneCandidate candidate : selectedCandidates) {
            sceneCandidateDtos.add(new KnowledgePackageClarificationDTO.SceneCandidateDTO(
                    candidate.scene().getId(),
                    candidate.scene().getSceneCode(),
                    candidate.scene().getSceneTitle(),
                    candidate.snapshotId()
            ));
            publishedPlans(candidate.scene().getId(), candidate.snapshotId()).stream().findFirst().ifPresent(plan ->
                    planCandidateDtos.add(new KnowledgePackageClarificationDTO.PlanCandidateDTO(
                            candidate.scene().getSceneCode(),
                            plan.getId(),
                            plan.getPlanCode(),
                            plan.getPlanName()
                    ))
            );
        }

        KnowledgePackageClarificationDTO clarification = new KnowledgePackageClarificationDTO(
                "当前问题同时命中代发明细查询和代发批次结果查询，请拆分后分别检索",
                sceneCandidateDtos,
                planCandidateDtos,
                List.of("按协议号查询代发明细", "按公司户查询代发批次结果"),
                List.of("请先选择「代发明细查询」或「代发批次结果查询」，再分别提交运行请求"),
                List.of("本次是查询协议号对应的代发明细，还是公司户对应的代发批次结果？")
        );

        KnowledgePackageDTO result = new KnowledgePackageDTO(
                "clarification_only",
                "MULTI_SCENE_AMBIGUOUS",
                "CLARIFICATION",
                List.of("MULTI_SCENE_AMBIGUOUS"),
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                new KnowledgePackageRiskDTO("MEDIUM", List.of("问题包含跨场景多意图，必须拆分为受控子问题后再继续检索")),
                new KnowledgePackageTraceDTO(
                        null,
                        null,
                        null,
                        null,
                        retrievalExperiment == null ? null : retrievalExperiment.adapterName(),
                        retrievalExperiment == null ? null : retrievalExperiment.status(),
                        retrievalExperiment != null && retrievalExperiment.fallbackToFormal()
                ),
                toExperiment(retrievalExperiment),
                clarification
        );
        result = withTrace(result, traceId);
        recordAudit(null, traceId, null, operator, "MULTI_SCENE_AMBIGUOUS", result);
        return result;
    }

    private void recordAudit(Long sceneId,
                             String traceId,
                             SceneVersionPO latestVersion,
                             String operator,
                             String reasonCode,
                             KnowledgePackageDTO result) {
        graphAuditEventAppService.record(
                sceneId,
                "KNOWLEDGE_PACKAGE_QUERY",
                traceId,
                latestVersion == null ? null : latestVersion.getId(),
                operator,
                null,
                reasonCode,
                "RECORDED",
                result
        );
    }

    private KnowledgePackageDTO deny(String traceId,
                                     ScenePO scene,
                                     PlanPO plan,
                                     String identifierType,
                                     String identifierValue,
                                     OutputContractPO outputContract,
                                     ContractViewPO contractView,
                                     SceneVersionPO latestVersion,
                                     String coverageStatus,
                                     String matchedSegment,
                                     String coverageExplanation,
                                     List<SourceContractPO> matchedSourceContracts,
                                     String reasonCode,
                                     String runtimeMode,
                                     List<String> degradeReasonCodes,
                                     String riskLevel,
                                     List<String> riskReasons,
                                     String operator,
                                     RetrievalExperimentSupport.RetrievalExperimentResult retrievalExperiment) {
        FieldDecision fieldDecision = evaluateFields(outputContract, contractView, List.of());
        KnowledgePackageDTO result = buildResult(
                "deny",
                reasonCode,
                runtimeMode,
                normalizeDegradeReasonCodes(reasonCode, degradeReasonCodes),
                scene,
                plan,
                identifierType,
                identifierValue,
                outputContract,
                contractView,
                fieldDecision,
                safe(coverageStatus).isBlank() ? "GAP" : coverageStatus,
                matchedSegment,
                safe(coverageExplanation).isBlank() ? (riskReasons.isEmpty() ? "当前请求被拒绝" : riskReasons.get(0)) : coverageExplanation,
                matchedSourceContracts,
                List.of(),
                List.of(),
                latestVersion,
                retrievalExperiment,
                buildResolutionSteps(identifierType, plan, reasonCode),
                riskLevel,
                riskReasons
        );
        result = withTrace(result, traceId);
        recordAudit(scene == null ? null : scene.getId(), traceId, latestVersion, operator, reasonCode, result);
        return result;
    }

    private KnowledgePackageDTO withTrace(KnowledgePackageDTO result, String traceId) {
        return new KnowledgePackageDTO(
                result.decision(),
                result.reasonCode(),
                result.runtimeMode(),
                result.degradeReasonCodes(),
                result.scene(),
                result.plan(),
                result.contract(),
                result.coverage(),
                result.policy(),
                result.path(),
                result.evidence(),
                result.risk(),
                new KnowledgePackageTraceDTO(
                        traceId,
                        result.trace() == null ? null : result.trace().snapshotId(),
                        result.trace() == null ? null : result.trace().inferenceSnapshotId(),
                        result.trace() == null ? null : result.trace().versionTag(),
                        result.trace() == null ? null : result.trace().retrievalAdapter(),
                        result.trace() == null ? null : result.trace().retrievalStatus(),
                        result.trace() != null && result.trace().fallbackToFormal()
                ),
                result.experiment(),
                result.clarification()
        );
    }

    private RetrievalExperimentSupport.RetrievalExperimentResult retrieveExperiment(String traceId,
                                                                                    KnowledgePackageQueryCmd cmd,
                                                                                    String queryText,
                                                                                    String identifierType,
                                                                                    String identifierValue,
                                                                                    List<String> requestedFields) {
        return retrievalExperimentSupport.retrieve(new RetrievalExperimentSupport.RetrievalExperimentRequest(
                traceId,
                queryText,
                Map.of(
                        "identifierType", safe(identifierType),
                        "identifierValue", safe(identifierValue),
                        "dateFrom", safe(cmd.dateFrom()),
                        "dateTo", safe(cmd.dateTo())
                ),
                null,
                cmd.selectedSceneId(),
                cmd.snapshotId(),
                requestedFields,
                "published_evidence_only",
                safe(cmd.operator()).isBlank() ? "system" : safe(cmd.operator())
        ));
    }

    private KnowledgePackageExperimentDTO toExperiment(RetrievalExperimentSupport.RetrievalExperimentResult retrievalExperiment) {
        if (retrievalExperiment == null) {
            return null;
        }
        return new KnowledgePackageExperimentDTO(
                retrievalExperiment.adapterName(),
                retrievalExperiment.adapterVersion(),
                retrievalExperiment.status(),
                retrievalExperiment.fallbackToFormal(),
                retrievalExperiment.summary(),
                retrievalExperiment.referenceRefs(),
                retrievalExperiment.candidateScenes().stream()
                        .map(item -> new KnowledgePackageExperimentDTO.ExperimentSceneCandidateDTO(
                                item.sceneId(),
                                item.sceneCode(),
                                item.sceneTitle(),
                                item.snapshotId(),
                                item.score(),
                                item.source()
                        ))
                        .toList(),
                retrievalExperiment.candidateEvidence().stream()
                        .map(item -> new KnowledgePackageEvidenceDTO(
                                item.evidenceCode(),
                                item.title(),
                                item.sourceAnchor(),
                                retrievalExperiment.adapterName(),
                                item.referenceRef(),
                                item.score()
                        ))
                        .toList(),
                retrievalExperiment.scoreBreakdown().stream()
                        .map(item -> new KnowledgePackageExperimentDTO.ExperimentScoreDTO(item.label(), item.score()))
                        .toList()
        );
    }

    private String deriveRuntimeMode(String decision, String coverageStatus, String reasonCode) {
        if ("clarification_only".equalsIgnoreCase(decision)) {
            return "CLARIFICATION";
        }
        if ("allow".equalsIgnoreCase(decision) && "FULL".equalsIgnoreCase(coverageStatus)) {
            return "FULL_MATCH";
        }
        if ("allow".equalsIgnoreCase(decision)) {
            return "PARTIAL_MATCH";
        }
        if ("need_approval".equalsIgnoreCase(decision)) {
            return "PARTIAL_WITH_APPROVAL";
        }
        if ("deny".equalsIgnoreCase(decision)) {
            return "DENIED";
        }
        if (!safe(reasonCode).isBlank()) {
            return "DEGRADED_" + safe(reasonCode);
        }
        return "DEGRADED";
    }

    private List<String> normalizeDegradeReasonCodes(String reasonCode) {
        return normalizeDegradeReasonCodes(reasonCode, List.of());
    }

    private List<String> normalizeDegradeReasonCodes(String reasonCode, List<String> reasonCodes) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (!safe(reasonCode).isBlank()) {
            result.add(safe(reasonCode));
        }
        if (reasonCodes != null) {
            for (String reason : reasonCodes) {
                if (!safe(reason).isBlank()) {
                    result.add(safe(reason));
                }
            }
        }
        return result.stream().toList();
    }

    private List<String> normalizeFields(List<String> requestedFields) {
        if (requestedFields == null) {
            return List.of();
        }
        return requestedFields.stream()
                .map(this::safe)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeIdentifierType(String identifierType) {
        String value = safe(identifierType).toUpperCase(Locale.ROOT);
        if (value.isBlank()) {
            return "";
        }
        return List.of("PROTOCOL_NBR", "CUST_ID", "ORG_ACCOUNT", "BATCH_NBR").contains(value) ? value : value;
    }

    private String riskLevel(String decision) {
        if ("allow".equalsIgnoreCase(decision)) {
            return "LOW";
        }
        if ("need_approval".equalsIgnoreCase(decision)) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record SegmentHit(PlanPO plan, CoverageDeclarationPO coverage) {
    }

    private record SceneCandidate(ScenePO scene, SceneVersionPO snapshot) {
        Long snapshotId() {
            return snapshot == null ? null : snapshot.getId();
        }
    }

    private record SceneRecallResult(ScenePO scene,
                                     List<SceneCandidate> candidates,
                                     boolean requiresClarification) {
    }

    private record MatchResult(PlanPO plan,
                               String coverageStatus,
                               String segmentLabel,
                               String reasonCode,
                               String coverageExplanation) {
    }

    private record FieldDecision(List<String> visibleFields,
                                 List<String> maskedFields,
                                 List<String> restrictedFields,
                                 List<String> forbiddenFields,
                                 boolean hasRestricted,
                                 boolean hasForbidden) {
    }
}
