package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateSceneVersionCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDiffDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneVersionDTO;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneVersionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ContractViewMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CoverageDeclarationMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.InputSlotSchemaMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanPolicyRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PolicyMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceIntakeContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneVersionPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.ContractViewPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CoverageDeclarationPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EvidenceFragmentPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.InputSlotSchemaPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.OutputContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPolicyRefPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PolicyPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceIntakeContractPO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SceneVersionAppService {

    private final SceneMapper sceneMapper;
    private final SceneVersionMapper sceneVersionMapper;
    private final PlanMapper planMapper;
    private final OutputContractMapper outputContractMapper;
    private final CoverageDeclarationMapper coverageDeclarationMapper;
    private final PolicyMapper policyMapper;
    private final PlanPolicyRefMapper planPolicyRefMapper;
    private final SourceIntakeContractMapper sourceIntakeContractMapper;
    private final EvidenceFragmentMapper evidenceFragmentMapper;
    private final InputSlotSchemaMapper inputSlotSchemaMapper;
    private final ContractViewMapper contractViewMapper;
    private final SourceContractMapper sourceContractMapper;
    private final ObjectMapper objectMapper;
    private com.cmbchina.datadirect.caliber.application.service.command.graphrag.ExperimentalRetrievalIndexSyncService experimentalRetrievalIndexSyncService;

    public SceneVersionAppService(SceneMapper sceneMapper,
                                  SceneVersionMapper sceneVersionMapper,
                                  PlanMapper planMapper,
                                  OutputContractMapper outputContractMapper,
                                  CoverageDeclarationMapper coverageDeclarationMapper,
                                  PolicyMapper policyMapper,
                                  PlanPolicyRefMapper planPolicyRefMapper,
                                  SourceIntakeContractMapper sourceIntakeContractMapper,
                                  EvidenceFragmentMapper evidenceFragmentMapper,
                                  InputSlotSchemaMapper inputSlotSchemaMapper,
                                  ContractViewMapper contractViewMapper,
                                  SourceContractMapper sourceContractMapper,
                                  ObjectMapper objectMapper) {
        this(sceneMapper,
                sceneVersionMapper,
                planMapper,
                outputContractMapper,
                coverageDeclarationMapper,
                policyMapper,
                planPolicyRefMapper,
                sourceIntakeContractMapper,
                evidenceFragmentMapper,
                inputSlotSchemaMapper,
                contractViewMapper,
                sourceContractMapper,
                objectMapper,
                null);
    }

    @Autowired
    public SceneVersionAppService(SceneMapper sceneMapper,
                                  SceneVersionMapper sceneVersionMapper,
                                  PlanMapper planMapper,
                                  OutputContractMapper outputContractMapper,
                                  CoverageDeclarationMapper coverageDeclarationMapper,
                                  PolicyMapper policyMapper,
                                  PlanPolicyRefMapper planPolicyRefMapper,
                                  SourceIntakeContractMapper sourceIntakeContractMapper,
                                  EvidenceFragmentMapper evidenceFragmentMapper,
                                  InputSlotSchemaMapper inputSlotSchemaMapper,
                                  ContractViewMapper contractViewMapper,
                                  SourceContractMapper sourceContractMapper,
                                  ObjectMapper objectMapper,
                                  com.cmbchina.datadirect.caliber.application.service.command.graphrag.ExperimentalRetrievalIndexSyncService experimentalRetrievalIndexSyncService) {
        this.sceneMapper = sceneMapper;
        this.sceneVersionMapper = sceneVersionMapper;
        this.planMapper = planMapper;
        this.outputContractMapper = outputContractMapper;
        this.coverageDeclarationMapper = coverageDeclarationMapper;
        this.policyMapper = policyMapper;
        this.planPolicyRefMapper = planPolicyRefMapper;
        this.sourceIntakeContractMapper = sourceIntakeContractMapper;
        this.evidenceFragmentMapper = evidenceFragmentMapper;
        this.inputSlotSchemaMapper = inputSlotSchemaMapper;
        this.contractViewMapper = contractViewMapper;
        this.sourceContractMapper = sourceContractMapper;
        this.objectMapper = objectMapper;
        this.experimentalRetrievalIndexSyncService = experimentalRetrievalIndexSyncService;
    }

    @Autowired(required = false)
    void setExperimentalRetrievalIndexSyncService(
            com.cmbchina.datadirect.caliber.application.service.command.graphrag.ExperimentalRetrievalIndexSyncService experimentalRetrievalIndexSyncService) {
        this.experimentalRetrievalIndexSyncService = experimentalRetrievalIndexSyncService;
    }

    @Transactional
    public SceneVersionDTO create(Long sceneId, CreateSceneVersionCmd cmd) {
        ScenePO scene = loadScene(sceneId);
        return createSnapshot(scene, cmd.changeSummary(), normalizeOperator(cmd.operator()), false);
    }

    @Transactional
    public SceneVersionDTO createPublishedSnapshot(Long sceneId, String changeSummary, String operator) {
        ScenePO scene = loadScene(sceneId);
        return createSnapshot(scene, changeSummary, normalizeOperator(operator), true);
    }

    public List<SceneVersionDTO> list(Long sceneId) {
        return sceneVersionMapper.findBySceneIdOrderByVersionNoDesc(sceneId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public SceneDiffDTO diff(Long sceneId, Integer from, Integer to) {
        SceneVersionPO left = sceneVersionMapper.findBySceneIdAndVersionNo(sceneId, from)
                .orElseThrow(() -> new ResourceNotFoundException("scene version not found: " + sceneId + "#" + from));
        SceneVersionPO right = sceneVersionMapper.findBySceneIdAndVersionNo(sceneId, to)
                .orElseThrow(() -> new ResourceNotFoundException("scene version not found: " + sceneId + "#" + to));
        JsonNode leftJson = readJson(left.getSnapshotJson());
        JsonNode rightJson = readJson(right.getSnapshotJson());
        List<String> changed = new ArrayList<>();
        Iterator<String> fields = rightJson.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            JsonNode leftValue = leftJson.path(field);
            JsonNode rightValue = rightJson.path(field);
            if (!leftValue.equals(rightValue)) {
                changed.add(field);
            }
        }
        return new SceneDiffDTO(sceneId, from, to, changed);
    }

    private SceneVersionDTO createSnapshot(ScenePO scene, String changeSummary, String operator, boolean bindAssets) {
        int nextVersion = sceneVersionMapper.findTopBySceneIdOrderByVersionNoDesc(scene.getId())
                .map(item -> item.getVersionNo() + 1)
                .orElse(1);
        OffsetDateTime now = OffsetDateTime.now();
        String versionTag = scene.getSceneCode() + "-V" + String.format("%03d", nextVersion);

        SceneVersionPO po = new SceneVersionPO();
        po.setSceneId(scene.getId());
        po.setVersionNo(nextVersion);
        po.setVersionTag(versionTag);
        po.setSnapshotJson(writeJson(buildSnapshotPayload(scene)));
        po.setSnapshotSummaryJson(writeJson(buildSnapshotSummary(scene)));
        po.setChangeSummary(changeSummary);
        po.setPublishStatus(scene.getStatus() == null ? null : scene.getStatus().name());
        po.setPublishedBy(bindAssets ? operator : scene.getPublishedBy());
        po.setPublishedAt(bindAssets ? now : scene.getPublishedAt());
        po.setCreatedBy(operator);
        po.setCreatedAt(now);
        SceneVersionPO saved = sceneVersionMapper.save(po);
        if (bindAssets) {
            bindAssets(scene.getId(), saved.getId(), versionTag, now);
            if (experimentalRetrievalIndexSyncService != null) {
                experimentalRetrievalIndexSyncService.syncSnapshotManifest(
                        scene.getId(),
                        saved.getId(),
                        scene.getSceneCode(),
                        versionTag,
                        po.getPublishStatus(),
                        po.getSnapshotSummaryJson(),
                        operator
                );
            }
        }
        return toDTO(saved);
    }

    private ScenePO loadScene(Long sceneId) {
        return sceneMapper.findById(sceneId)
                .orElseThrow(() -> new ResourceNotFoundException("scene not found: " + sceneId));
    }

    private Map<String, Object> buildSnapshotPayload(ScenePO scene) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scene", scene);
        payload.put("plans", planMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()));
        payload.put("outputContracts", outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()));
        payload.put("inputSlots", inputSlotSchemaMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()));
        payload.put("sourceIntakeContracts", sourceIntakeContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()));
        payload.put("contractViews", contractViewMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()));
        payload.put("sourceContracts", sourceContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()));
        payload.put("evidenceFragments", evidenceFragmentMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()));
        payload.put("policies", resolvePolicies(scene.getId()));
        payload.put("coverageDeclarations", resolveCoverage(scene.getId()));
        return payload;
    }

    private Map<String, Object> buildSnapshotSummary(ScenePO scene) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("sceneCode", scene.getSceneCode());
        summary.put("sceneTitle", scene.getSceneTitle());
        summary.put("sceneStatus", scene.getStatus() == null ? null : scene.getStatus().name());
        summary.put("planCount", planMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size());
        summary.put("outputContractCount", outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size());
        summary.put("inputSlotCount", inputSlotSchemaMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size());
        summary.put("sourceIntakeCount", sourceIntakeContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size());
        summary.put("contractViewCount", contractViewMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size());
        summary.put("sourceContractCount", sourceContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size());
        summary.put("evidenceCount", evidenceFragmentMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size());
        summary.put("policyCount", resolvePolicies(scene.getId()).size());
        summary.put("coverageCount", resolveCoverage(scene.getId()).size());
        return summary;
    }

    private List<PolicyPO> resolvePolicies(Long sceneId) {
        Set<Long> planIds = planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream().map(PlanPO::getId).collect(Collectors.toSet());
        Set<Long> planPolicyIds = planIds.stream()
                .flatMap(planId -> planPolicyRefMapper.findByPlanId(planId).stream())
                .map(PlanPolicyRefPO::getPolicyId)
                .collect(Collectors.toSet());
        return policyMapper.findAll().stream()
                .filter(policy -> sceneId.equals(policy.getScopeRefId()) && "SCENE".equalsIgnoreCase(policy.getScopeType())
                        || (policy.getScopeRefId() != null && planIds.contains(policy.getScopeRefId()) && "PLAN".equalsIgnoreCase(policy.getScopeType()))
                        || planPolicyIds.contains(policy.getId()))
                .distinct()
                .toList();
    }

    private List<CoverageDeclarationPO> resolveCoverage(Long sceneId) {
        return planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream()
                .flatMap(plan -> coverageDeclarationMapper.findByPlanIdOrderByUpdatedAtDesc(plan.getId()).stream())
                .toList();
    }

    private void bindAssets(Long sceneId, Long snapshotId, String versionTag, OffsetDateTime now) {
        for (PlanPO plan : planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId)) {
            plan.setSnapshotId(snapshotId);
            plan.setVersionTag(versionTag);
            plan.setUpdatedAt(now);
            planMapper.save(plan);
            for (CoverageDeclarationPO coverage : coverageDeclarationMapper.findByPlanIdOrderByUpdatedAtDesc(plan.getId())) {
                coverage.setSnapshotId(snapshotId);
                coverage.setVersionTag(versionTag);
                coverage.setUpdatedAt(now);
                coverageDeclarationMapper.save(coverage);
            }
        }

        for (OutputContractPO contract : outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId)) {
            contract.setSnapshotId(snapshotId);
            contract.setVersionTag(versionTag);
            contract.setUpdatedAt(now);
            outputContractMapper.save(contract);
        }
        for (InputSlotSchemaPO slot : inputSlotSchemaMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId)) {
            slot.setSnapshotId(snapshotId);
            slot.setVersionTag(versionTag);
            slot.setUpdatedAt(now);
            inputSlotSchemaMapper.save(slot);
        }
        for (SourceIntakeContractPO intake : sourceIntakeContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId)) {
            intake.setSnapshotId(snapshotId);
            intake.setVersionTag(versionTag);
            intake.setUpdatedAt(now);
            sourceIntakeContractMapper.save(intake);
        }
        for (EvidenceFragmentPO evidence : evidenceFragmentMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId)) {
            evidence.setSnapshotId(snapshotId);
            evidence.setVersionTag(versionTag);
            evidence.setUpdatedAt(now);
            evidenceFragmentMapper.save(evidence);
        }
        for (ContractViewPO view : contractViewMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId)) {
            view.setSnapshotId(snapshotId);
            view.setVersionTag(versionTag);
            view.setUpdatedAt(now);
            contractViewMapper.save(view);
        }
        for (SourceContractPO sourceContract : sourceContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId)) {
            sourceContract.setSnapshotId(snapshotId);
            sourceContract.setVersionTag(versionTag);
            sourceContract.setUpdatedAt(now);
            sourceContractMapper.save(sourceContract);
        }

        Set<Long> planIds = planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream().map(PlanPO::getId).collect(Collectors.toSet());
        for (PolicyPO policy : resolvePolicies(sceneId)) {
            policy.setSnapshotId(snapshotId);
            policy.setVersionTag(versionTag);
            policy.setUpdatedAt(now);
            policyMapper.save(policy);
        }
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new IllegalStateException("invalid version snapshot json", ex);
        }
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("write scene snapshot failed", ex);
        }
    }

    private String normalizeOperator(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }

    private SceneVersionDTO toDTO(SceneVersionPO po) {
        return new SceneVersionDTO(
                po.getId(),
                po.getSceneId(),
                po.getVersionNo(),
                po.getVersionTag(),
                po.getSnapshotJson(),
                po.getSnapshotSummaryJson(),
                po.getChangeSummary(),
                po.getPublishStatus(),
                po.getPublishedBy(),
                po.getPublishedAt(),
                po.getCreatedBy(),
                po.getCreatedAt()
        );
    }
}
