package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertCoverageDeclarationCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertContractViewCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertEvidenceFragmentCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertInputSlotSchemaCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertOutputContractCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertPlanCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertPolicyCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertSourceContractCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertSourceIntakeContractCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.CoverageDeclarationDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.ContractViewDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.EvidenceFragmentDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.InputSlotSchemaDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.OutputContractDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.PlanDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.PolicyDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.SourceContractDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.SourceIntakeContractDTO;
import com.cmbchina.datadirect.caliber.application.exception.BusinessConflictException;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.application.service.graphrag.GraphAssetSupport;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CoverageDeclarationMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ContractViewMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.InputSlotSchemaMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanEvidenceRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanPolicyRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PolicyMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceIntakeContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CoverageDeclarationPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.ContractViewPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EvidenceFragmentPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.InputSlotSchemaPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.OutputContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanEvidenceRefPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPolicyRefPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PolicyPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceIntakeContractPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GraphAssetAppService {

    private final PlanMapper planMapper;
    private final EvidenceFragmentMapper evidenceFragmentMapper;
    private final CoverageDeclarationMapper coverageDeclarationMapper;
    private final PolicyMapper policyMapper;
    private final OutputContractMapper outputContractMapper;
    private final InputSlotSchemaMapper inputSlotSchemaMapper;
    private final ContractViewMapper contractViewMapper;
    private final SourceContractMapper sourceContractMapper;
    private final SourceIntakeContractMapper sourceIntakeContractMapper;
    private final PlanEvidenceRefMapper planEvidenceRefMapper;
    private final PlanPolicyRefMapper planPolicyRefMapper;
    private final SceneMapper sceneMapper;
    private final GraphAssetSupport graphAssetSupport;
    private final SceneGraphAssetSyncService sceneGraphAssetSyncService;
    private final GraphProjectionAppService graphProjectionAppService;

    public GraphAssetAppService(PlanMapper planMapper,
                                EvidenceFragmentMapper evidenceFragmentMapper,
                                CoverageDeclarationMapper coverageDeclarationMapper,
                                PolicyMapper policyMapper,
                                OutputContractMapper outputContractMapper,
                                InputSlotSchemaMapper inputSlotSchemaMapper,
                                ContractViewMapper contractViewMapper,
                                SourceContractMapper sourceContractMapper,
                                SourceIntakeContractMapper sourceIntakeContractMapper,
                                PlanEvidenceRefMapper planEvidenceRefMapper,
                                PlanPolicyRefMapper planPolicyRefMapper,
                                SceneMapper sceneMapper,
                                GraphAssetSupport graphAssetSupport,
                                SceneGraphAssetSyncService sceneGraphAssetSyncService,
                                GraphProjectionAppService graphProjectionAppService) {
        this.planMapper = planMapper;
        this.evidenceFragmentMapper = evidenceFragmentMapper;
        this.coverageDeclarationMapper = coverageDeclarationMapper;
        this.policyMapper = policyMapper;
        this.outputContractMapper = outputContractMapper;
        this.inputSlotSchemaMapper = inputSlotSchemaMapper;
        this.contractViewMapper = contractViewMapper;
        this.sourceContractMapper = sourceContractMapper;
        this.sourceIntakeContractMapper = sourceIntakeContractMapper;
        this.planEvidenceRefMapper = planEvidenceRefMapper;
        this.planPolicyRefMapper = planPolicyRefMapper;
        this.sceneMapper = sceneMapper;
        this.graphAssetSupport = graphAssetSupport;
        this.sceneGraphAssetSyncService = sceneGraphAssetSyncService;
        this.graphProjectionAppService = graphProjectionAppService;
    }

    @Transactional(readOnly = true)
    public List<PlanDTO> listPlans(Long sceneId, Long domainId, String status) {
        List<PlanPO> plans = planMapper.findByFilter(sceneId, normalizeFilterStatus(status));
        plans = filterPlansByDomain(plans, domainId);
        Map<Long, List<Long>> evidenceIdsByPlan = planEvidenceRefMapper.findByPlanIdIn(plans.stream().map(PlanPO::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(PlanEvidenceRefPO::getPlanId,
                        Collectors.mapping(PlanEvidenceRefPO::getEvidenceId, Collectors.toList())));
        Map<Long, List<Long>> policyIdsByPlan = planPolicyRefMapper.findByPlanIdIn(plans.stream().map(PlanPO::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(PlanPolicyRefPO::getPlanId,
                        Collectors.mapping(PlanPolicyRefPO::getPolicyId, Collectors.toList())));
        return plans.stream().map(plan -> toPlanDTO(plan, evidenceIdsByPlan.get(plan.getId()), policyIdsByPlan.get(plan.getId()))).toList();
    }

    @Transactional
    public PlanDTO upsertPlan(Long id, UpsertPlanCmd cmd) {
        ScenePO scene = requireScene(cmd.sceneId());
        assertSceneWritable(scene);
        OffsetDateTime now = OffsetDateTime.now();
        PlanPO plan = id == null ? new PlanPO() : planMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("plan not found: " + id));
        assertExpectedVersion(plan.getRowVersion(), cmd.expectedVersion());
        plan.setSceneId(scene.getId());
        plan.setPlanCode(isBlank(cmd.planCode()) ? scene.getSceneCode() + "-PLN-" + String.format(Locale.ROOT, "%02d", planMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size() + 1) : cmd.planCode().trim());
        plan.setPlanName(graphAssetSupport.requireText(cmd.planName(), "planName"));
        plan.setApplicablePeriod(trimToNull(cmd.applicablePeriod()));
        plan.setDefaultTimeSemantic(trimToNull(cmd.defaultTimeSemantic()));
        plan.setSourceTablesJson(graphAssetSupport.writeJson(graphAssetSupport.parseJson(cmd.sourceTablesJson(), "[]"), "[]"));
        plan.setNotes(trimToNull(cmd.notes()));
        plan.setSqlText(trimToNull(cmd.sqlText()));
        plan.setRetrievalText(buildPlanRetrievalText(scene, plan));
        plan.setConfidenceScore(cmd.confidenceScore() == null ? 0.8d : cmd.confidenceScore());
        plan.setStatus(scene.getStatus() == SceneStatus.PUBLISHED ? "PUBLISHED" : "DRAFT");
        if (plan.getCreatedAt() == null) {
            plan.setCreatedBy(normalizeOperator(cmd.operator()));
            plan.setCreatedAt(now);
        }
        plan.setUpdatedBy(normalizeOperator(cmd.operator()));
        plan.setUpdatedAt(now);
        PlanPO saved = planMapper.save(plan);
        if (cmd.evidenceIds() != null) {
            rewritePlanEvidenceRefs(saved.getId(), cmd.evidenceIds(), normalizeOperator(cmd.operator()));
        }
        if (cmd.policyIds() != null) {
            rewritePlanPolicyRefs(saved.getId(), cmd.policyIds(), normalizeOperator(cmd.operator()));
        }
        sceneGraphAssetSyncService.rebuildPlanDerivedAssets(saved.getId(), normalizeOperator(cmd.operator()));
        sceneGraphAssetSyncService.syncSceneLegacyFields(scene.getId(), normalizeOperator(cmd.operator()));
        refreshProjectionIfPublished(scene, normalizeOperator(cmd.operator()));
        return toPlanDTO(saved,
                planEvidenceRefMapper.findByPlanId(saved.getId()).stream().map(PlanEvidenceRefPO::getEvidenceId).toList(),
                planPolicyRefMapper.findByPlanId(saved.getId()).stream().map(PlanPolicyRefPO::getPolicyId).toList());
    }

    @Transactional(readOnly = true)
    public List<EvidenceFragmentDTO> listEvidenceFragments(Long sceneId, Long domainId, String status) {
        List<EvidenceFragmentPO> fragments = evidenceFragmentMapper.findByFilter(sceneId, normalizeFilterStatus(status));
        if (domainId != null) {
            fragments = fragments.stream().filter(fragment -> sceneMatchesDomain(fragment.getSceneId(), domainId)).toList();
        }
        Map<Long, List<Long>> planIdsByEvidence = new HashMap<>();
        List<PlanPO> plans = planMapper.findAll();
        for (PlanPO plan : plans) {
            for (PlanEvidenceRefPO ref : planEvidenceRefMapper.findByPlanId(plan.getId())) {
                planIdsByEvidence.computeIfAbsent(ref.getEvidenceId(), key -> new ArrayList<>()).add(ref.getPlanId());
            }
        }
        return fragments.stream().map(fragment -> toEvidenceDTO(fragment, planIdsByEvidence.get(fragment.getId()))).toList();
    }

    @Transactional
    public EvidenceFragmentDTO upsertEvidenceFragment(Long id, UpsertEvidenceFragmentCmd cmd) {
        ScenePO scene = requireScene(cmd.sceneId());
        assertSceneWritable(scene);
        OffsetDateTime now = OffsetDateTime.now();
        EvidenceFragmentPO evidence = id == null ? new EvidenceFragmentPO() : evidenceFragmentMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("evidence not found: " + id));
        assertExpectedVersion(evidence.getRowVersion(), cmd.expectedVersion());
        evidence.setSceneId(scene.getId());
        evidence.setEvidenceCode(isBlank(cmd.evidenceCode()) ? scene.getSceneCode() + "-EVD-" + String.format(Locale.ROOT, "%02d", evidenceFragmentMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size() + 1) : cmd.evidenceCode().trim());
        evidence.setTitle(graphAssetSupport.requireText(cmd.title(), "title"));
        evidence.setFragmentText(graphAssetSupport.requireText(cmd.fragmentText(), "fragmentText"));
        evidence.setSourceAnchor(trimToNull(cmd.sourceAnchor()));
        evidence.setSourceType(trimToNull(cmd.sourceType()));
        evidence.setSourceRef(trimToNull(cmd.sourceRef()));
        evidence.setConfidenceScore(cmd.confidenceScore() == null ? 0.8d : cmd.confidenceScore());
        evidence.setStatus(scene.getStatus() == SceneStatus.PUBLISHED ? "PUBLISHED" : "DRAFT");
        if (evidence.getCreatedAt() == null) {
            evidence.setCreatedBy(normalizeOperator(cmd.operator()));
            evidence.setCreatedAt(now);
        }
        evidence.setUpdatedBy(normalizeOperator(cmd.operator()));
        evidence.setUpdatedAt(now);
        EvidenceFragmentPO saved = evidenceFragmentMapper.save(evidence);
        if (cmd.planIds() != null) {
            rewriteEvidencePlanRefs(saved.getId(), scene.getId(), cmd.planIds(), normalizeOperator(cmd.operator()));
        }
        refreshProjectionIfPublished(scene, normalizeOperator(cmd.operator()));
        return toEvidenceDTO(saved, resolvePlanIdsByEvidence(saved.getId(), scene.getId()));
    }

    @Transactional(readOnly = true)
    public List<CoverageDeclarationDTO> listCoverageDeclarations(Long sceneId, Long domainId, String status) {
        List<PlanPO> plans = filterPlansByDomain(planMapper.findByFilter(sceneId, null), domainId);
        List<CoverageDeclarationDTO> result = new ArrayList<>();
        for (PlanPO plan : plans) {
            for (CoverageDeclarationPO coverage : coverageDeclarationMapper.findByFilter(plan.getId(), normalizeFilterStatus(status))) {
                result.add(toCoverageDTO(coverage));
            }
        }
        return result;
    }

    @Transactional
    public CoverageDeclarationDTO upsertCoverageDeclaration(Long id, UpsertCoverageDeclarationCmd cmd) {
        PlanPO plan = planMapper.findById(cmd.planId()).orElseThrow(() -> new ResourceNotFoundException("plan not found: " + cmd.planId()));
        ScenePO scene = requireScene(plan.getSceneId());
        assertSceneWritable(scene);
        OffsetDateTime now = OffsetDateTime.now();
        CoverageDeclarationPO coverage = id == null ? new CoverageDeclarationPO() : coverageDeclarationMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("coverage not found: " + id));
        assertExpectedVersion(coverage.getRowVersion(), cmd.expectedVersion());
        coverage.setPlanId(plan.getId());
        coverage.setCoverageCode(isBlank(cmd.coverageCode()) ? plan.getPlanCode() + "-COV-" + String.format(Locale.ROOT, "%02d", coverageDeclarationMapper.findByPlanIdOrderByUpdatedAtDesc(plan.getId()).size() + 1) : cmd.coverageCode().trim());
        coverage.setCoverageTitle(graphAssetSupport.requireText(cmd.coverageTitle(), "coverageTitle"));
        coverage.setCoverageType(graphAssetSupport.normalizeCoverageType(cmd.coverageType()));
        coverage.setCoverageStatus(graphAssetSupport.normalizeCoverageStatus(cmd.coverageStatus()));
        coverage.setStatementText(graphAssetSupport.requireText(cmd.statementText(), "statementText"));
        coverage.setApplicablePeriod(trimToNull(cmd.applicablePeriod()));
        coverage.setTimeSemantic(trimToNull(cmd.timeSemantic()));
        coverage.setSourceSystem(trimToNull(cmd.sourceSystem()));
        coverage.setSourceTablesJson(graphAssetSupport.writeJson(graphAssetSupport.parseJson(cmd.sourceTablesJson(), "[]"), "[]"));
        coverage.setGapText(trimToNull(cmd.gapText()));
        coverage.setActive(Boolean.TRUE.equals(cmd.active()));
        coverage.setStartDate(graphAssetSupport.parseDate(cmd.startDate()));
        coverage.setEndDate(graphAssetSupport.parseDate(cmd.endDate()));
        coverage.setStatus(plan.getStatus());
        if (coverage.getCreatedAt() == null) {
            coverage.setCreatedBy(normalizeOperator(cmd.operator()));
            coverage.setCreatedAt(now);
        }
        coverage.setUpdatedBy(normalizeOperator(cmd.operator()));
        coverage.setUpdatedAt(now);
        CoverageDeclarationPO saved = coverageDeclarationMapper.save(coverage);
        sceneGraphAssetSyncService.syncSceneLegacyFields(scene.getId(), normalizeOperator(cmd.operator()));
        refreshProjectionIfPublished(scene, normalizeOperator(cmd.operator()));
        return toCoverageDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<PolicyDTO> listPolicies(Long sceneId, Long domainId, String status) {
        List<PolicyPO> policies = policyMapper.findAll();
        if (!isBlank(status)) {
            String expected = status.trim().toUpperCase(Locale.ROOT);
            policies = policies.stream().filter(policy -> expected.equalsIgnoreCase(policy.getStatus())).toList();
        }
        policies = policies.stream().filter(policy -> matchPolicyScope(policy, sceneId, domainId)).toList();
        Map<Long, List<Long>> planIdsByPolicy = resolvePlanIdsByPolicy();
        return policies.stream().sorted((left, right) -> right.getUpdatedAt().compareTo(left.getUpdatedAt()))
                .map(policy -> toPolicyDTO(policy, planIdsByPolicy.get(policy.getId()))).toList();
    }

    @Transactional
    public PolicyDTO upsertPolicy(Long id, UpsertPolicyCmd cmd) {
        ScenePO scene = resolvePolicyScene(cmd.scopeType(), cmd.scopeRefId(), cmd.planIds());
        if (scene != null) {
            assertSceneWritable(scene);
        }
        OffsetDateTime now = OffsetDateTime.now();
        PolicyPO policy = id == null ? new PolicyPO() : policyMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("policy not found: " + id));
        assertExpectedVersion(policy.getRowVersion(), cmd.expectedVersion());
        policy.setPolicyCode(isBlank(cmd.policyCode()) ? graphAssetSupport.generateCode("PLC") : cmd.policyCode().trim());
        policy.setPolicyName(graphAssetSupport.requireText(cmd.policyName(), "policyName"));
        policy.setScopeType(graphAssetSupport.normalizeScopeType(cmd.scopeType()));
        policy.setScopeRefId(cmd.scopeRefId());
        policy.setEffectType(graphAssetSupport.normalizeEffectType(cmd.effectType()));
        policy.setConditionText(trimToNull(cmd.conditionText()));
        policy.setSourceType(trimToNull(cmd.sourceType()));
        policy.setSensitivityLevel(graphAssetSupport.normalizeSensitivityLevel(cmd.sensitivityLevel()));
        policy.setMaskingRule(trimToNull(cmd.maskingRule()));
        policy.setStatus(scene != null && scene.getStatus() == SceneStatus.PUBLISHED ? "ACTIVE" : "DRAFT");
        if (policy.getCreatedAt() == null) {
            policy.setCreatedBy(normalizeOperator(cmd.operator()));
            policy.setCreatedAt(now);
        }
        policy.setUpdatedBy(normalizeOperator(cmd.operator()));
        policy.setUpdatedAt(now);
        PolicyPO saved = policyMapper.save(policy);
        if (cmd.planIds() != null) {
            rewritePolicyPlanRefs(saved.getId(), cmd.planIds(), normalizeOperator(cmd.operator()));
        }
        if (scene != null) {
            refreshProjectionIfPublished(scene, normalizeOperator(cmd.operator()));
        }
        return toPolicyDTO(saved, resolvePlanIdsByPolicy().get(saved.getId()));
    }

    @Transactional(readOnly = true)
    public List<OutputContractDTO> listOutputContracts(Long sceneId, Long domainId, String status) {
        List<OutputContractPO> contracts = outputContractMapper.findByFilter(sceneId, normalizeFilterStatus(status));
        if (domainId != null) {
            contracts = contracts.stream().filter(contract -> sceneMatchesDomain(contract.getSceneId(), domainId)).toList();
        }
        return contracts.stream().map(this::toOutputContractDTO).toList();
    }

    @Transactional
    public OutputContractDTO upsertOutputContract(Long id, UpsertOutputContractCmd cmd) {
        ScenePO scene = requireScene(cmd.sceneId());
        assertSceneWritable(scene);
        OffsetDateTime now = OffsetDateTime.now();
        OutputContractPO contract = id == null ? new OutputContractPO() : outputContractMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("output contract not found: " + id));
        assertExpectedVersion(contract.getRowVersion(), cmd.expectedVersion());
        contract.setSceneId(scene.getId());
        contract.setContractCode(isBlank(cmd.contractCode()) ? scene.getSceneCode() + "-OUT-" + String.format(Locale.ROOT, "%02d", outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size() + 1) : cmd.contractCode().trim());
        contract.setContractName(graphAssetSupport.requireText(cmd.contractName(), "contractName"));
        contract.setSummaryText(trimToNull(cmd.summaryText()));
        contract.setFieldsJson(graphAssetSupport.writeJson(graphAssetSupport.parseJson(cmd.fieldsJson(), "[]"), "[]"));
        contract.setMaskingRulesJson(graphAssetSupport.writeJson(graphAssetSupport.parseJson(cmd.maskingRulesJson(), "[]"), "[]"));
        contract.setUsageConstraints(trimToNull(cmd.usageConstraints()));
        contract.setTimeCaliberNote(trimToNull(cmd.timeCaliberNote()));
        contract.setStatus(scene.getStatus() == SceneStatus.PUBLISHED ? "PUBLISHED" : "DRAFT");
        if (contract.getCreatedAt() == null) {
            contract.setCreatedBy(normalizeOperator(cmd.operator()));
            contract.setCreatedAt(now);
        }
        contract.setUpdatedBy(normalizeOperator(cmd.operator()));
        contract.setUpdatedAt(now);
        OutputContractPO saved = outputContractMapper.save(contract);
        sceneGraphAssetSyncService.syncSceneLegacyFields(scene.getId(), normalizeOperator(cmd.operator()));
        refreshProjectionIfPublished(scene, normalizeOperator(cmd.operator()));
        return toOutputContractDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<InputSlotSchemaDTO> listInputSlotSchemas(Long sceneId, Long domainId, String status) {
        List<InputSlotSchemaPO> slots = inputSlotSchemaMapper.findByFilter(sceneId, normalizeFilterStatus(status));
        if (domainId != null) {
            slots = slots.stream().filter(slot -> sceneMatchesDomain(slot.getSceneId(), domainId)).toList();
        }
        return slots.stream().map(this::toInputSlotSchemaDTO).toList();
    }

    @Transactional
    public InputSlotSchemaDTO upsertInputSlotSchema(Long id, UpsertInputSlotSchemaCmd cmd) {
        ScenePO scene = requireScene(cmd.sceneId());
        assertSceneWritable(scene);
        OffsetDateTime now = OffsetDateTime.now();
        InputSlotSchemaPO slot = id == null ? new InputSlotSchemaPO() : inputSlotSchemaMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("input slot not found: " + id));
        assertExpectedVersion(slot.getRowVersion(), cmd.expectedVersion());
        slot.setSceneId(scene.getId());
        slot.setSlotCode(isBlank(cmd.slotCode()) ? scene.getSceneCode() + "-SLOT-" + String.format(Locale.ROOT, "%02d", inputSlotSchemaMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size() + 1) : cmd.slotCode().trim());
        slot.setSlotName(graphAssetSupport.requireText(cmd.slotName(), "slotName"));
        slot.setSlotType(isBlank(cmd.slotType()) ? "TEXT" : cmd.slotType().trim().toUpperCase(Locale.ROOT));
        slot.setRequiredFlag(Boolean.TRUE.equals(cmd.requiredFlag()));
        slot.setIdentifierCandidatesJson(graphAssetSupport.writeJson(graphAssetSupport.parseJson(cmd.identifierCandidatesJson(), "[]"), "[]"));
        slot.setNormalizationRule(trimToNull(cmd.normalizationRule()));
        slot.setClarificationHint(trimToNull(cmd.clarificationHint()));
        slot.setStatus(scene.getStatus() == SceneStatus.PUBLISHED ? "PUBLISHED" : "DRAFT");
        if (slot.getCreatedAt() == null) {
            slot.setCreatedBy(normalizeOperator(cmd.operator()));
            slot.setCreatedAt(now);
        }
        slot.setUpdatedBy(normalizeOperator(cmd.operator()));
        slot.setUpdatedAt(now);
        InputSlotSchemaPO saved = inputSlotSchemaMapper.save(slot);
        sceneGraphAssetSyncService.syncSceneLegacyFields(scene.getId(), normalizeOperator(cmd.operator()));
        refreshProjectionIfPublished(scene, normalizeOperator(cmd.operator()));
        return toInputSlotSchemaDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<SourceIntakeContractDTO> listSourceIntakeContracts(Long sceneId, Long domainId, String status) {
        List<SourceIntakeContractPO> contracts = sourceIntakeContractMapper.findByFilter(sceneId, normalizeFilterStatus(status));
        if (domainId != null) {
            contracts = contracts.stream().filter(contract -> sceneMatchesDomain(contract.getSceneId(), domainId)).toList();
        }
        return contracts.stream().map(this::toSourceIntakeContractDTO).toList();
    }

    @Transactional
    public SourceIntakeContractDTO upsertSourceIntakeContract(Long id, UpsertSourceIntakeContractCmd cmd) {
        ScenePO scene = requireScene(cmd.sceneId());
        assertSceneWritable(scene);
        OffsetDateTime now = OffsetDateTime.now();
        SourceIntakeContractPO contract = id == null ? new SourceIntakeContractPO() : sourceIntakeContractMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("source intake contract not found: " + id));
        assertExpectedVersion(contract.getRowVersion(), cmd.expectedVersion());
        contract.setSceneId(scene.getId());
        contract.setIntakeCode(isBlank(cmd.intakeCode()) ? scene.getSceneCode() + "-SRC-" + String.format(Locale.ROOT, "%02d", sourceIntakeContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size() + 1) : cmd.intakeCode().trim());
        contract.setIntakeName(graphAssetSupport.requireText(cmd.intakeName(), "intakeName"));
        contract.setSourceType(graphAssetSupport.requireText(cmd.sourceType(), "sourceType"));
        contract.setRequiredFieldsJson(graphAssetSupport.writeJson(graphAssetSupport.parseJson(cmd.requiredFieldsJson(), "[]"), "[]"));
        contract.setCompletenessRule(trimToNull(cmd.completenessRule()));
        contract.setGapTaskHint(trimToNull(cmd.gapTaskHint()));
        contract.setSourceTableHintsJson(graphAssetSupport.writeJson(graphAssetSupport.parseJson(cmd.sourceTableHintsJson(), "[]"), "[]"));
        contract.setKnownCoverageJson(graphAssetSupport.writeJson(graphAssetSupport.parseJson(cmd.knownCoverageJson(), "[]"), "[]"));
        contract.setSensitivityLevel(graphAssetSupport.normalizeSensitivityLevel(cmd.sensitivityLevel()));
        contract.setDefaultTimeSemantic(trimToNull(cmd.defaultTimeSemantic()));
        contract.setMaterialSourceNote(trimToNull(cmd.materialSourceNote()));
        contract.setStatus(scene.getStatus() == SceneStatus.PUBLISHED ? "PUBLISHED" : "DRAFT");
        if (contract.getCreatedAt() == null) {
            contract.setCreatedBy(normalizeOperator(cmd.operator()));
            contract.setCreatedAt(now);
        }
        contract.setUpdatedBy(normalizeOperator(cmd.operator()));
        contract.setUpdatedAt(now);
        SourceIntakeContractPO saved = sourceIntakeContractMapper.save(contract);
        refreshProjectionIfPublished(scene, normalizeOperator(cmd.operator()));
        return toSourceIntakeContractDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ContractViewDTO> listContractViews(Long sceneId, Long domainId, String status) {
        List<ContractViewPO> views = contractViewMapper.findByFilter(sceneId, normalizeFilterStatus(status));
        if (domainId != null) {
            views = views.stream().filter(view -> sceneMatchesDomain(view.getSceneId(), domainId)).toList();
        }
        return views.stream().map(this::toContractViewDTO).toList();
    }

    @Transactional
    public ContractViewDTO upsertContractView(Long id, UpsertContractViewCmd cmd) {
        ScenePO scene = requireScene(cmd.sceneId());
        assertSceneWritable(scene);
        OffsetDateTime now = OffsetDateTime.now();
        ContractViewPO view = id == null ? new ContractViewPO() : contractViewMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("contract view not found: " + id));
        assertExpectedVersion(view.getRowVersion(), cmd.expectedVersion());
        if (cmd.outputContractId() != null) {
            OutputContractPO contract = outputContractMapper.findById(cmd.outputContractId())
                    .orElseThrow(() -> new ResourceNotFoundException("output contract not found: " + cmd.outputContractId()));
            if (!scene.getId().equals(contract.getSceneId())) {
                throw new DomainValidationException("contract view output contract does not belong to scene");
            }
        }
        if (cmd.planId() != null) {
            PlanPO plan = planMapper.findById(cmd.planId())
                    .orElseThrow(() -> new ResourceNotFoundException("plan not found: " + cmd.planId()));
            if (!scene.getId().equals(plan.getSceneId())) {
                throw new DomainValidationException("contract view plan does not belong to scene");
            }
        }
        view.setSceneId(scene.getId());
        view.setPlanId(cmd.planId());
        view.setOutputContractId(cmd.outputContractId());
        view.setViewCode(isBlank(cmd.viewCode()) ? scene.getSceneCode() + "-VIEW-" + String.format(Locale.ROOT, "%02d", contractViewMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size() + 1) : cmd.viewCode().trim());
        view.setViewName(graphAssetSupport.requireText(cmd.viewName(), "viewName"));
        view.setRoleScope(graphAssetSupport.requireText(cmd.roleScope(), "roleScope"));
        view.setVisibleFieldsJson(graphAssetSupport.writeJson(graphAssetSupport.parseJson(cmd.visibleFieldsJson(), "[]"), "[]"));
        view.setMaskedFieldsJson(graphAssetSupport.writeJson(graphAssetSupport.parseJson(cmd.maskedFieldsJson(), "[]"), "[]"));
        view.setRestrictedFieldsJson(graphAssetSupport.writeJson(graphAssetSupport.parseJson(cmd.restrictedFieldsJson(), "[]"), "[]"));
        view.setForbiddenFieldsJson(graphAssetSupport.writeJson(graphAssetSupport.parseJson(cmd.forbiddenFieldsJson(), "[]"), "[]"));
        view.setApprovalTemplate(trimToNull(cmd.approvalTemplate()));
        view.setStatus(scene.getStatus() == SceneStatus.PUBLISHED ? "PUBLISHED" : "DRAFT");
        if (view.getCreatedAt() == null) {
            view.setCreatedBy(normalizeOperator(cmd.operator()));
            view.setCreatedAt(now);
        }
        view.setUpdatedBy(normalizeOperator(cmd.operator()));
        view.setUpdatedAt(now);
        ContractViewPO saved = contractViewMapper.save(view);
        refreshProjectionIfPublished(scene, normalizeOperator(cmd.operator()));
        return toContractViewDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<SourceContractDTO> listSourceContracts(Long sceneId, Long domainId, String status) {
        List<SourceContractPO> contracts = sourceContractMapper.findByFilter(sceneId, normalizeFilterStatus(status));
        if (domainId != null) {
            contracts = contracts.stream().filter(contract -> sceneMatchesDomain(contract.getSceneId(), domainId)).toList();
        }
        return contracts.stream().map(this::toSourceContractDTO).toList();
    }

    @Transactional
    public SourceContractDTO upsertSourceContract(Long id, UpsertSourceContractCmd cmd) {
        ScenePO scene = requireScene(cmd.sceneId());
        assertSceneWritable(scene);
        OffsetDateTime now = OffsetDateTime.now();
        SourceContractPO contract = id == null ? new SourceContractPO() : sourceContractMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("source contract not found: " + id));
        assertExpectedVersion(contract.getRowVersion(), cmd.expectedVersion());
        if (cmd.planId() != null) {
            PlanPO plan = planMapper.findById(cmd.planId())
                    .orElseThrow(() -> new ResourceNotFoundException("plan not found: " + cmd.planId()));
            if (!scene.getId().equals(plan.getSceneId())) {
                throw new DomainValidationException("source contract plan does not belong to scene");
            }
        }
        if (cmd.intakeContractId() != null) {
            SourceIntakeContractPO intake = sourceIntakeContractMapper.findById(cmd.intakeContractId())
                    .orElseThrow(() -> new ResourceNotFoundException("source intake contract not found: " + cmd.intakeContractId()));
            if (!scene.getId().equals(intake.getSceneId())) {
                throw new DomainValidationException("source contract intake does not belong to scene");
            }
        }
        contract.setSceneId(scene.getId());
        contract.setPlanId(cmd.planId());
        contract.setIntakeContractId(cmd.intakeContractId());
        contract.setSourceContractCode(isBlank(cmd.sourceContractCode()) ? scene.getSceneCode() + "-SRC-CON-" + String.format(Locale.ROOT, "%02d", sourceContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size() + 1) : cmd.sourceContractCode().trim());
        contract.setSourceName(graphAssetSupport.requireText(cmd.sourceName(), "sourceName"));
        contract.setPhysicalTable(graphAssetSupport.requireText(cmd.physicalTable(), "physicalTable"));
        contract.setSourceRole(graphAssetSupport.requireText(cmd.sourceRole(), "sourceRole").toUpperCase(Locale.ROOT));
        contract.setIdentifierType(trimToNull(cmd.identifierType()));
        contract.setOutputIdentifierType(trimToNull(cmd.outputIdentifierType()));
        contract.setSourceSystem(trimToNull(cmd.sourceSystem()));
        contract.setTimeSemantic(trimToNull(cmd.timeSemantic()));
        contract.setCompletenessLevel(isBlank(cmd.completenessLevel()) ? null : cmd.completenessLevel().trim().toUpperCase(Locale.ROOT));
        contract.setSensitivityLevel(graphAssetSupport.normalizeSensitivityLevel(cmd.sensitivityLevel()));
        contract.setStartDate(graphAssetSupport.parseDate(cmd.startDate()));
        contract.setEndDate(graphAssetSupport.parseDate(cmd.endDate()));
        contract.setMaterialSourceNote(trimToNull(cmd.materialSourceNote()));
        contract.setNotes(trimToNull(cmd.notes()));
        contract.setStatus(scene.getStatus() == SceneStatus.PUBLISHED ? "PUBLISHED" : "DRAFT");
        if (contract.getCreatedAt() == null) {
            contract.setCreatedBy(normalizeOperator(cmd.operator()));
            contract.setCreatedAt(now);
        }
        contract.setUpdatedBy(normalizeOperator(cmd.operator()));
        contract.setUpdatedAt(now);
        SourceContractPO saved = sourceContractMapper.save(contract);
        refreshProjectionIfPublished(scene, normalizeOperator(cmd.operator()));
        return toSourceContractDTO(saved);
    }

    private List<PlanPO> filterPlansByDomain(List<PlanPO> plans, Long domainId) {
        if (domainId == null) {
            return plans;
        }
        return plans.stream().filter(plan -> sceneMatchesDomain(plan.getSceneId(), domainId)).toList();
    }

    private boolean sceneMatchesDomain(Long sceneId, Long domainId) {
        return sceneMapper.findById(sceneId)
                .map(scene -> domainId.equals(scene.getDomainId()))
                .orElse(false);
    }

    private ScenePO requireScene(Long sceneId) {
        if (sceneId == null) {
            throw new DomainValidationException("sceneId is required");
        }
        return sceneMapper.findById(sceneId).orElseThrow(() -> new ResourceNotFoundException("scene not found: " + sceneId));
    }

    private void assertSceneWritable(ScenePO scene) {
        if (scene.getStatus() == SceneStatus.PUBLISHED) {
            throw new DomainValidationException("published scene is read-only; please create a draft clone first");
        }
        if (scene.getStatus() == SceneStatus.DISCARDED) {
            throw new DomainValidationException("discarded scene is not writable");
        }
    }

    private void assertExpectedVersion(Long actual, Long expected) {
        if (expected == null) {
            return;
        }
        if (!expected.equals(actual)) {
            throw new BusinessConflictException("CAL-GR-409", "asset version conflict, expected=" + expected + ", actual=" + actual);
        }
    }

    private void rewritePlanEvidenceRefs(Long planId, List<Long> evidenceIds, String operator) {
        Set<Long> expected = new LinkedHashSet<>(evidenceIds == null ? List.of() : evidenceIds);
        List<PlanEvidenceRefPO> existing = new ArrayList<>(planEvidenceRefMapper.findByPlanId(planId));
        for (PlanEvidenceRefPO ref : existing) {
            if (!expected.contains(ref.getEvidenceId())) {
                planEvidenceRefMapper.delete(ref);
            }
        }
        Set<Long> existingIds = existing.stream().map(PlanEvidenceRefPO::getEvidenceId).collect(Collectors.toSet());
        OffsetDateTime now = OffsetDateTime.now();
        for (Long evidenceId : expected) {
            if (existingIds.contains(evidenceId)) {
                continue;
            }
            PlanEvidenceRefPO ref = new PlanEvidenceRefPO();
            ref.setPlanId(planId);
            ref.setEvidenceId(evidenceId);
            ref.setRelationType("PRIMARY");
            ref.setCreatedBy(operator);
            ref.setCreatedAt(now);
            planEvidenceRefMapper.save(ref);
        }
    }

    private void rewritePlanPolicyRefs(Long planId, List<Long> policyIds, String operator) {
        Set<Long> expected = new LinkedHashSet<>(policyIds == null ? List.of() : policyIds);
        List<PlanPolicyRefPO> existing = new ArrayList<>(planPolicyRefMapper.findByPlanId(planId));
        for (PlanPolicyRefPO ref : existing) {
            if (!expected.contains(ref.getPolicyId())) {
                planPolicyRefMapper.delete(ref);
            }
        }
        Set<Long> existingIds = existing.stream().map(PlanPolicyRefPO::getPolicyId).collect(Collectors.toSet());
        OffsetDateTime now = OffsetDateTime.now();
        for (Long policyId : expected) {
            if (existingIds.contains(policyId)) {
                continue;
            }
            PlanPolicyRefPO ref = new PlanPolicyRefPO();
            ref.setPlanId(planId);
            ref.setPolicyId(policyId);
            ref.setRelationType("ENFORCED");
            ref.setCreatedBy(operator);
            ref.setCreatedAt(now);
            planPolicyRefMapper.save(ref);
        }
    }

    private void rewriteEvidencePlanRefs(Long evidenceId, Long sceneId, List<Long> planIds, String operator) {
        Set<Long> expected = new LinkedHashSet<>(planIds == null ? List.of() : planIds);
        for (PlanPO plan : planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId)) {
            List<PlanEvidenceRefPO> refs = new ArrayList<>(planEvidenceRefMapper.findByPlanId(plan.getId()));
            boolean exists = refs.stream().anyMatch(ref -> evidenceId.equals(ref.getEvidenceId()));
            if (expected.contains(plan.getId())) {
                if (!exists) {
                    PlanEvidenceRefPO ref = new PlanEvidenceRefPO();
                    ref.setPlanId(plan.getId());
                    ref.setEvidenceId(evidenceId);
                    ref.setRelationType("PRIMARY");
                    ref.setCreatedBy(operator);
                    ref.setCreatedAt(OffsetDateTime.now());
                    planEvidenceRefMapper.save(ref);
                }
            } else {
                refs.stream().filter(ref -> evidenceId.equals(ref.getEvidenceId())).forEach(planEvidenceRefMapper::delete);
            }
        }
    }

    private void rewritePolicyPlanRefs(Long policyId, List<Long> planIds, String operator) {
        Set<Long> expected = new LinkedHashSet<>(planIds == null ? List.of() : planIds);
        for (PlanPO plan : planMapper.findAll()) {
            List<PlanPolicyRefPO> refs = new ArrayList<>(planPolicyRefMapper.findByPlanId(plan.getId()));
            boolean exists = refs.stream().anyMatch(ref -> policyId.equals(ref.getPolicyId()));
            if (expected.contains(plan.getId())) {
                if (!exists) {
                    PlanPolicyRefPO ref = new PlanPolicyRefPO();
                    ref.setPlanId(plan.getId());
                    ref.setPolicyId(policyId);
                    ref.setRelationType("ENFORCED");
                    ref.setCreatedBy(operator);
                    ref.setCreatedAt(OffsetDateTime.now());
                    planPolicyRefMapper.save(ref);
                }
            } else {
                refs.stream().filter(ref -> policyId.equals(ref.getPolicyId())).forEach(planPolicyRefMapper::delete);
            }
        }
    }

    private List<Long> resolvePlanIdsByEvidence(Long evidenceId, Long sceneId) {
        List<Long> planIds = new ArrayList<>();
        for (PlanPO plan : planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId)) {
            boolean exists = planEvidenceRefMapper.findByPlanId(plan.getId()).stream().anyMatch(ref -> evidenceId.equals(ref.getEvidenceId()));
            if (exists) {
                planIds.add(plan.getId());
            }
        }
        return planIds;
    }

    private Map<Long, List<Long>> resolvePlanIdsByPolicy() {
        Map<Long, List<Long>> result = new HashMap<>();
        for (PlanPO plan : planMapper.findAll()) {
            for (PlanPolicyRefPO ref : planPolicyRefMapper.findByPlanId(plan.getId())) {
                result.computeIfAbsent(ref.getPolicyId(), key -> new ArrayList<>()).add(ref.getPlanId());
            }
        }
        return result;
    }

    private ScenePO resolvePolicyScene(String scopeType, Long scopeRefId, List<Long> planIds) {
        String normalizedScope = isBlank(scopeType) ? "GLOBAL" : scopeType.trim().toUpperCase(Locale.ROOT);
        if ("SCENE".equals(normalizedScope) && scopeRefId != null) {
            return requireScene(scopeRefId);
        }
        if ("PLAN".equals(normalizedScope) && scopeRefId != null) {
            PlanPO plan = planMapper.findById(scopeRefId).orElseThrow(() -> new ResourceNotFoundException("plan not found: " + scopeRefId));
            return requireScene(plan.getSceneId());
        }
        if (planIds != null && !planIds.isEmpty()) {
            PlanPO plan = planMapper.findById(planIds.get(0)).orElseThrow(() -> new ResourceNotFoundException("plan not found: " + planIds.get(0)));
            return requireScene(plan.getSceneId());
        }
        return null;
    }

    private String buildPlanRetrievalText(ScenePO scene, PlanPO plan) {
        return String.join(" ", List.of(
                scene.getSceneTitle() == null ? "" : scene.getSceneTitle(),
                scene.getSceneDescription() == null ? "" : scene.getSceneDescription(),
                plan.getPlanName() == null ? "" : plan.getPlanName(),
                plan.getApplicablePeriod() == null ? "" : plan.getApplicablePeriod(),
                plan.getNotes() == null ? "" : plan.getNotes(),
                plan.getSourceTablesJson() == null ? "" : plan.getSourceTablesJson(),
                scene.getSceneType() == null ? "FACT_DETAIL" : scene.getSceneType()
        )).trim();
    }

    private void refreshProjectionIfPublished(ScenePO scene, String operator) {
        if (scene.getStatus() == SceneStatus.PUBLISHED) {
            graphProjectionAppService.refreshProjection(scene.getId(), scene.getSceneCode(), operator);
        }
    }

    private PlanDTO toPlanDTO(PlanPO plan, List<Long> evidenceIds, List<Long> policyIds) {
        return new PlanDTO(
                plan.getId(),
                plan.getSceneId(),
                plan.getPlanCode(),
                plan.getPlanName(),
                plan.getApplicablePeriod(),
                plan.getDefaultTimeSemantic(),
                plan.getSourceTablesJson(),
                plan.getNotes(),
                plan.getRetrievalText(),
                plan.getSqlText(),
                plan.getConfidenceScore(),
                plan.getStatus(),
                evidenceIds == null ? List.of() : evidenceIds,
                policyIds == null ? List.of() : policyIds,
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                plan.getRowVersion()
        );
    }

    private EvidenceFragmentDTO toEvidenceDTO(EvidenceFragmentPO evidence, List<Long> planIds) {
        return new EvidenceFragmentDTO(
                evidence.getId(),
                evidence.getSceneId(),
                evidence.getEvidenceCode(),
                evidence.getTitle(),
                evidence.getFragmentText(),
                evidence.getSourceAnchor(),
                evidence.getSourceType(),
                evidence.getSourceRef(),
                evidence.getConfidenceScore(),
                evidence.getStatus(),
                planIds == null ? List.of() : planIds,
                evidence.getCreatedAt(),
                evidence.getUpdatedAt(),
                evidence.getRowVersion()
        );
    }

    private CoverageDeclarationDTO toCoverageDTO(CoverageDeclarationPO coverage) {
        return new CoverageDeclarationDTO(
                coverage.getId(),
                coverage.getPlanId(),
                coverage.getCoverageCode(),
                coverage.getCoverageTitle(),
                coverage.getCoverageType(),
                coverage.getCoverageStatus(),
                coverage.getStatementText(),
                coverage.getApplicablePeriod(),
                coverage.getTimeSemantic(),
                coverage.getSourceSystem(),
                coverage.getSourceTablesJson(),
                coverage.getGapText(),
                coverage.isActive(),
                coverage.getStatus(),
                coverage.getStartDate(),
                coverage.getEndDate(),
                coverage.getCreatedAt(),
                coverage.getUpdatedAt(),
                coverage.getRowVersion()
        );
    }

    private PolicyDTO toPolicyDTO(PolicyPO policy, List<Long> planIds) {
        return new PolicyDTO(
                policy.getId(),
                policy.getPolicyCode(),
                policy.getPolicyName(),
                policy.getScopeType(),
                policy.getScopeRefId(),
                policy.getEffectType(),
                policy.getConditionText(),
                policy.getSourceType(),
                policy.getSensitivityLevel(),
                policy.getMaskingRule(),
                policy.getStatus(),
                planIds == null ? List.of() : planIds,
                policy.getCreatedAt(),
                policy.getUpdatedAt(),
                policy.getRowVersion()
        );
    }

    private OutputContractDTO toOutputContractDTO(OutputContractPO contract) {
        return new OutputContractDTO(
                contract.getId(),
                contract.getSceneId(),
                contract.getContractCode(),
                contract.getContractName(),
                contract.getSummaryText(),
                contract.getFieldsJson(),
                contract.getMaskingRulesJson(),
                contract.getUsageConstraints(),
                contract.getTimeCaliberNote(),
                contract.getStatus(),
                contract.getCreatedAt(),
                contract.getUpdatedAt(),
                contract.getRowVersion()
        );
    }

    private InputSlotSchemaDTO toInputSlotSchemaDTO(InputSlotSchemaPO slot) {
        return new InputSlotSchemaDTO(
                slot.getId(),
                slot.getSceneId(),
                slot.getSlotCode(),
                slot.getSlotName(),
                slot.getSlotType(),
                slot.isRequiredFlag(),
                slot.getIdentifierCandidatesJson(),
                slot.getNormalizationRule(),
                slot.getClarificationHint(),
                slot.getStatus(),
                slot.getCreatedAt(),
                slot.getUpdatedAt(),
                slot.getRowVersion()
        );
    }

    private SourceIntakeContractDTO toSourceIntakeContractDTO(SourceIntakeContractPO contract) {
        return new SourceIntakeContractDTO(
                contract.getId(),
                contract.getSceneId(),
                contract.getIntakeCode(),
                contract.getIntakeName(),
                contract.getSourceType(),
                contract.getRequiredFieldsJson(),
                contract.getCompletenessRule(),
                contract.getGapTaskHint(),
                contract.getSourceTableHintsJson(),
                contract.getKnownCoverageJson(),
                contract.getSensitivityLevel(),
                contract.getDefaultTimeSemantic(),
                contract.getMaterialSourceNote(),
                contract.getSnapshotId(),
                contract.getVersionTag(),
                contract.getStatus(),
                contract.getCreatedAt(),
                contract.getUpdatedAt(),
                contract.getRowVersion()
        );
    }

    private ContractViewDTO toContractViewDTO(ContractViewPO view) {
        return new ContractViewDTO(
                view.getId(),
                view.getSceneId(),
                view.getPlanId(),
                view.getOutputContractId(),
                view.getViewCode(),
                view.getViewName(),
                view.getRoleScope(),
                view.getVisibleFieldsJson(),
                view.getMaskedFieldsJson(),
                view.getRestrictedFieldsJson(),
                view.getForbiddenFieldsJson(),
                view.getApprovalTemplate(),
                view.getSnapshotId(),
                view.getVersionTag(),
                view.getStatus(),
                view.getCreatedAt(),
                view.getUpdatedAt(),
                view.getRowVersion()
        );
    }

    private SourceContractDTO toSourceContractDTO(SourceContractPO contract) {
        return new SourceContractDTO(
                contract.getId(),
                contract.getSceneId(),
                contract.getPlanId(),
                contract.getIntakeContractId(),
                contract.getSourceContractCode(),
                contract.getSourceName(),
                contract.getPhysicalTable(),
                contract.getSourceRole(),
                contract.getIdentifierType(),
                contract.getOutputIdentifierType(),
                contract.getSourceSystem(),
                contract.getTimeSemantic(),
                contract.getCompletenessLevel(),
                contract.getSensitivityLevel(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getMaterialSourceNote(),
                contract.getNotes(),
                contract.getSnapshotId(),
                contract.getVersionTag(),
                contract.getStatus(),
                contract.getCreatedAt(),
                contract.getUpdatedAt(),
                contract.getRowVersion()
        );
    }

    private String normalizeFilterStatus(String status) {
        return isBlank(status) ? null : status.trim().toUpperCase(Locale.ROOT);
    }

    private boolean matchPolicyScope(PolicyPO policy, Long sceneId, Long domainId) {
        if (sceneId == null && domainId == null) {
            return true;
        }
        if (sceneId != null) {
            if ("SCENE".equalsIgnoreCase(policy.getScopeType()) && sceneId.equals(policy.getScopeRefId())) {
                return true;
            }
            if ("PLAN".equalsIgnoreCase(policy.getScopeType()) && policy.getScopeRefId() != null) {
                return planMapper.findById(policy.getScopeRefId()).map(plan -> sceneId.equals(plan.getSceneId())).orElse(false);
            }
        }
        if (domainId != null) {
            if ("DOMAIN".equalsIgnoreCase(policy.getScopeType()) && domainId.equals(policy.getScopeRefId())) {
                return true;
            }
            if ("SCENE".equalsIgnoreCase(policy.getScopeType()) && policy.getScopeRefId() != null) {
                return sceneMatchesDomain(policy.getScopeRefId(), domainId);
            }
            if ("PLAN".equalsIgnoreCase(policy.getScopeType()) && policy.getScopeRefId() != null) {
                return planMapper.findById(policy.getScopeRefId()).map(plan -> sceneMatchesDomain(plan.getSceneId(), domainId)).orElse(false);
            }
            return "GLOBAL".equalsIgnoreCase(policy.getScopeType());
        }
        return false;
    }

    private String normalizeOperator(String operator) {
        return isBlank(operator) ? "system" : operator.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
