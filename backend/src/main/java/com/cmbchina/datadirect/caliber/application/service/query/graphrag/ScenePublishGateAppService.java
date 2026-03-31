package com.cmbchina.datadirect.caliber.application.service.query.graphrag;

import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.PublishCheckDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.PublishCheckItemDTO;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.domain.model.Scene;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CoverageDeclarationMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ContractViewMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.InputSlotSchemaMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanEvidenceRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanPolicyRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanSchemaLinkMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PolicyMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceIntakeContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PolicyPO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ScenePublishGateAppService {

    private final OutputContractMapper outputContractMapper;
    private final InputSlotSchemaMapper inputSlotSchemaMapper;
    private final SourceIntakeContractMapper sourceIntakeContractMapper;
    private final ContractViewMapper contractViewMapper;
    private final SourceContractMapper sourceContractMapper;
    private final PlanMapper planMapper;
    private final CoverageDeclarationMapper coverageDeclarationMapper;
    private final PlanPolicyRefMapper planPolicyRefMapper;
    private final EvidenceFragmentMapper evidenceFragmentMapper;
    private final PlanEvidenceRefMapper planEvidenceRefMapper;
    private final PlanSchemaLinkMapper planSchemaLinkMapper;
    private final PolicyMapper policyMapper;

    public ScenePublishGateAppService(OutputContractMapper outputContractMapper,
                                      InputSlotSchemaMapper inputSlotSchemaMapper,
                                      SourceIntakeContractMapper sourceIntakeContractMapper,
                                      ContractViewMapper contractViewMapper,
                                      SourceContractMapper sourceContractMapper,
                                      PlanMapper planMapper,
                                      CoverageDeclarationMapper coverageDeclarationMapper,
                                      PlanPolicyRefMapper planPolicyRefMapper,
                                      EvidenceFragmentMapper evidenceFragmentMapper,
                                      PlanEvidenceRefMapper planEvidenceRefMapper,
                                      PlanSchemaLinkMapper planSchemaLinkMapper,
                                      PolicyMapper policyMapper) {
        this.outputContractMapper = outputContractMapper;
        this.inputSlotSchemaMapper = inputSlotSchemaMapper;
        this.sourceIntakeContractMapper = sourceIntakeContractMapper;
        this.contractViewMapper = contractViewMapper;
        this.sourceContractMapper = sourceContractMapper;
        this.planMapper = planMapper;
        this.coverageDeclarationMapper = coverageDeclarationMapper;
        this.planPolicyRefMapper = planPolicyRefMapper;
        this.evidenceFragmentMapper = evidenceFragmentMapper;
        this.planEvidenceRefMapper = planEvidenceRefMapper;
        this.planSchemaLinkMapper = planSchemaLinkMapper;
        this.policyMapper = policyMapper;
    }

    public PublishCheckDTO check(Scene scene) {
        List<PublishCheckItemDTO> items = new ArrayList<>();
        items.add(item("scene_type", "场景类型", scene != null && isNotBlank(scene.getSceneType()), "block", "发布前必须指定场景类型"));
        items.add(item("output_contract", "输出契约", scene != null && !outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).isEmpty(), "block", "至少需要 1 条输出契约"));
        items.add(item("input_slot", "输入槽位", scene != null && !inputSlotSchemaMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).isEmpty(), "block", "至少需要 1 条输入槽位定义"));
        items.add(item("source_intake", "来源接入契约", scene != null && !sourceIntakeContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).isEmpty(), "block", "至少需要 1 条来源接入契约"));

        List<PlanPO> plans = scene == null ? List.of() : planMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId());
        items.add(item("plan_count", "方案资产", !plans.isEmpty(), "block", "至少需要 1 条方案资产"));

        boolean planCoverageReady = !plans.isEmpty() && plans.stream().allMatch(plan -> !coverageDeclarationMapper.findByPlanIdOrderByUpdatedAtDesc(plan.getId()).isEmpty());
        items.add(item("plan_coverage", "覆盖声明", planCoverageReady, "block", "每个方案都需要覆盖声明"));

        boolean planPolicyReady = !plans.isEmpty() && plans.stream().allMatch(this::hasApplicablePolicy);
        items.add(item("plan_policy", "策略对象", planPolicyReady, "block", "每个方案都需要策略约束"));

        boolean planEvidenceReady = !plans.isEmpty() && plans.stream().allMatch(plan -> !planEvidenceRefMapper.findByPlanId(plan.getId()).isEmpty());
        items.add(item("plan_evidence", "证据片段", planEvidenceReady, "block", "每个方案都需要至少 1 条证据片段"));

        boolean planSchemaReady = !plans.isEmpty() && plans.stream().allMatch(plan -> {
            boolean hasDraft = !planSchemaLinkMapper.findByPlanIdAndStatus(plan.getId(), "DRAFT").isEmpty();
            boolean hasPublished = !planSchemaLinkMapper.findByPlanIdAndStatus(plan.getId(), "PUBLISHED").isEmpty();
            return hasDraft || hasPublished;
        });
        items.add(item("plan_schema", "模式链接", planSchemaReady, "block", "每个方案都需要至少 1 条模式链接"));

        boolean timeSemanticReady = !plans.isEmpty() && plans.stream().allMatch(plan -> isNotBlank(plan.getDefaultTimeSemantic()));
        items.add(item("plan_time_semantic", "默认时间语义", timeSemanticReady, "block", "每个方案都需要默认时间语义"));

        boolean contractViewReady = scene != null && !contractViewMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).isEmpty();
        items.add(item("contract_view", "契约视图", contractViewReady, "block", "至少需要 1 条契约视图"));

        boolean sourceContractReady = scene != null && !sourceContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).isEmpty();
        items.add(item("source_contract", "来源契约", sourceContractReady, "block", "至少需要 1 条来源契约"));

        boolean snapshotReady = scene != null
                && scene.getId() != null
                && !plans.isEmpty()
                && contractViewReady
                && sourceContractReady;
        items.add(item("snapshot_bindable", "版本快照", snapshotReady, "block", "发布前必须满足快照生成条件"));

        boolean publishReady = items.stream().filter(item -> !"warn".equalsIgnoreCase(item.level())).allMatch(item -> Boolean.TRUE.equals(item.passed()));
        return new PublishCheckDTO(scene == null ? null : scene.getId(), publishReady, items);
    }

    public void assertPublishable(Scene scene) {
        PublishCheckDTO check = check(scene);
        if (Boolean.TRUE.equals(check.publishReady())) {
            return;
        }
        String message = check.items().stream()
                .filter(item -> !Boolean.TRUE.equals(item.passed()))
                .map(item -> item.name() + "：" + item.message())
                .reduce((left, right) -> left + "；" + right)
                .orElse("发布门禁未通过");
        throw new DomainValidationException("发布失败，控制资产不完整：" + message);
    }

    private boolean hasApplicablePolicy(PlanPO plan) {
        if (plan == null) {
            return false;
        }
        if (!planPolicyRefMapper.findByPlanId(plan.getId()).isEmpty()) {
            return true;
        }
        return policyMapper.findAll().stream().anyMatch(policy -> applies(policy, plan));
    }

    private boolean applies(PolicyPO policy, PlanPO plan) {
        if (policy == null || plan == null || policy.getStatus() == null) {
            return false;
        }
        String status = policy.getStatus().toUpperCase(Locale.ROOT);
        if (!("ACTIVE".equals(status) || "DRAFT".equals(status) || "PUBLISHED".equals(status))) {
            return false;
        }
        return ("PLAN".equalsIgnoreCase(policy.getScopeType()) && plan.getId().equals(policy.getScopeRefId()))
                || ("SCENE".equalsIgnoreCase(policy.getScopeType()) && plan.getSceneId().equals(policy.getScopeRefId()))
                || "GLOBAL".equalsIgnoreCase(policy.getScopeType());
    }

    private PublishCheckItemDTO item(String key, String name, boolean passed, String level, String message) {
        return new PublishCheckItemDTO(key, name, passed, level, passed ? "已满足" : message);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
