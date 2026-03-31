package com.cmbchina.datadirect.caliber.application.service.query.datamap;

import com.cmbchina.datadirect.caliber.application.api.dto.request.SceneListQuery;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.application.service.command.graphrag.GraphAssetAppService;
import com.cmbchina.datadirect.caliber.application.service.query.SceneQueryAppService;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ContractViewMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CoverageDeclarationMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanPolicyRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PolicyMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceIntakeContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPolicyRefPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PolicyPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class GraphReadService {

    private final SceneQueryAppService sceneQueryAppService;
    private final GraphAssetAppService graphAssetAppService;
    private final PlanMapper planMapper;
    private final OutputContractMapper outputContractMapper;
    private final ContractViewMapper contractViewMapper;
    private final CoverageDeclarationMapper coverageDeclarationMapper;
    private final PolicyMapper policyMapper;
    private final EvidenceFragmentMapper evidenceFragmentMapper;
    private final SourceContractMapper sourceContractMapper;
    private final SourceIntakeContractMapper sourceIntakeContractMapper;
    private final PlanPolicyRefMapper planPolicyRefMapper;

    public GraphReadService(SceneQueryAppService sceneQueryAppService,
                            GraphAssetAppService graphAssetAppService,
                            PlanMapper planMapper,
                            OutputContractMapper outputContractMapper,
                            ContractViewMapper contractViewMapper,
                            CoverageDeclarationMapper coverageDeclarationMapper,
                            PolicyMapper policyMapper,
                            EvidenceFragmentMapper evidenceFragmentMapper,
                            SourceContractMapper sourceContractMapper,
                            SourceIntakeContractMapper sourceIntakeContractMapper,
                            PlanPolicyRefMapper planPolicyRefMapper) {
        this.sceneQueryAppService = sceneQueryAppService;
        this.graphAssetAppService = graphAssetAppService;
        this.planMapper = planMapper;
        this.outputContractMapper = outputContractMapper;
        this.contractViewMapper = contractViewMapper;
        this.coverageDeclarationMapper = coverageDeclarationMapper;
        this.policyMapper = policyMapper;
        this.evidenceFragmentMapper = evidenceFragmentMapper;
        this.sourceContractMapper = sourceContractMapper;
        this.sourceIntakeContractMapper = sourceIntakeContractMapper;
        this.planPolicyRefMapper = planPolicyRefMapper;
    }

    @Transactional(readOnly = true)
    public GraphSceneBundle loadBundle(String rootType, Long rootId) {
        Long sceneId = resolveSceneId(rootType, rootId);
        SceneDTO scene = sceneQueryAppService.getById(sceneId);
        Long domainId = scene.domainId();
        return new GraphSceneBundle(
                scene,
                graphAssetAppService.listPlans(sceneId, domainId, null),
                graphAssetAppService.listOutputContracts(sceneId, domainId, null),
                graphAssetAppService.listContractViews(sceneId, domainId, null),
                graphAssetAppService.listCoverageDeclarations(sceneId, domainId, null),
                graphAssetAppService.listPolicies(sceneId, domainId, null),
                graphAssetAppService.listEvidenceFragments(sceneId, domainId, null),
                graphAssetAppService.listSourceContracts(sceneId, domainId, null),
                graphAssetAppService.listSourceIntakeContracts(sceneId, domainId, null)
        );
    }

    @Transactional(readOnly = true)
    public GraphSceneBundle loadBundleByAssetRef(String assetRef) {
        ResolvedAssetRef resolved = parseAssetRef(assetRef);
        return loadBundle(resolved.objectType(), resolved.numericId());
    }

    @Transactional(readOnly = true)
    public ResolvedAssetRef parseAssetRef(String assetRef) {
        String text = assetRef == null ? "" : assetRef.trim();
        int separator = text.indexOf(':');
        if (separator <= 0 || separator >= text.length() - 1) {
            throw new DomainValidationException("assetRef is invalid");
        }
        String rawType = text.substring(0, separator).trim();
        String rawId = text.substring(separator + 1).trim();
        Long numericId;
        try {
            numericId = Long.valueOf(rawId);
        } catch (NumberFormatException ex) {
            throw new DomainValidationException("assetRef numeric id is invalid");
        }
        return new ResolvedAssetRef(normalizeRootType(rawType), numericId);
    }

    @Transactional(readOnly = true)
    public Long resolveSceneId(String rootType, Long rootId) {
        if (rootId == null || rootId <= 0) {
            throw new DomainValidationException("rootId must be positive");
        }
        String normalizedType = normalizeRootType(rootType);
        return switch (normalizedType) {
            case "SCENE" -> rootId;
            case "PLAN" -> planMapper.findById(rootId)
                    .map(plan -> plan.getSceneId())
                    .orElseThrow(() -> new ResourceNotFoundException("plan not found: " + rootId));
            case "OUTPUT_CONTRACT" -> outputContractMapper.findById(rootId)
                    .map(contract -> contract.getSceneId())
                    .orElseThrow(() -> new ResourceNotFoundException("output contract not found: " + rootId));
            case "CONTRACT_VIEW" -> contractViewMapper.findById(rootId)
                    .map(view -> view.getSceneId())
                    .orElseThrow(() -> new ResourceNotFoundException("contract view not found: " + rootId));
            case "COVERAGE_DECLARATION" -> coverageDeclarationMapper.findById(rootId)
                    .flatMap(coverage -> planMapper.findById(coverage.getPlanId()).map(plan -> plan.getSceneId()))
                    .orElseThrow(() -> new ResourceNotFoundException("coverage declaration not found: " + rootId));
            case "POLICY" -> resolveSceneIdByPolicy(rootId);
            case "EVIDENCE_FRAGMENT" -> evidenceFragmentMapper.findById(rootId)
                    .map(item -> item.getSceneId())
                    .orElseThrow(() -> new ResourceNotFoundException("evidence fragment not found: " + rootId));
            case "SOURCE_CONTRACT" -> sourceContractMapper.findById(rootId)
                    .map(item -> item.getSceneId())
                    .orElseThrow(() -> new ResourceNotFoundException("source contract not found: " + rootId));
            case "SOURCE_INTAKE_CONTRACT" -> sourceIntakeContractMapper.findById(rootId)
                    .map(item -> item.getSceneId())
                    .orElseThrow(() -> new ResourceNotFoundException("source intake contract not found: " + rootId));
            case "PATH_TEMPLATE" -> planMapper.findById(rootId)
                    .map(plan -> plan.getSceneId())
                    .orElseThrow(() -> new ResourceNotFoundException("path template plan not found: " + rootId));
            case "DOMAIN" -> resolveSceneIdByDomain(rootId);
            case "VERSION_SNAPSHOT" -> resolveSceneIdBySnapshot(rootId);
            default -> throw new DomainValidationException("unsupported rootType: " + normalizedType);
        };
    }

    private Long resolveSceneIdByPolicy(Long policyId) {
        PolicyPO policy = policyMapper.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("policy not found: " + policyId));
        if ("SCENE".equalsIgnoreCase(policy.getScopeType()) && policy.getScopeRefId() != null) {
            return policy.getScopeRefId();
        }
        if ("PLAN".equalsIgnoreCase(policy.getScopeType()) && policy.getScopeRefId() != null) {
            return planMapper.findById(policy.getScopeRefId())
                    .map(plan -> plan.getSceneId())
                    .orElseThrow(() -> new ResourceNotFoundException("plan not found for policy: " + policyId));
        }
        return planMapper.findAll().stream()
                .filter(plan -> planPolicyRefMapper.findByPlanId(plan.getId()).stream().map(PlanPolicyRefPO::getPolicyId).anyMatch(policyId::equals))
                .map(plan -> plan.getSceneId())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("policy scene not found: " + policyId));
    }

    private Long resolveSceneIdByDomain(Long domainId) {
        return sceneQueryAppService.list(new SceneListQuery(domainId, null, null, null)).stream()
                .filter(scene -> !Objects.equals(scene.status(), "DISCARDED"))
                .max(Comparator.comparing(SceneDTO::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(SceneDTO::id)
                .orElseThrow(() -> new ResourceNotFoundException("scene not found for domain: " + domainId));
    }

    private Long resolveSceneIdBySnapshot(Long snapshotId) {
        Optional<Long> sceneId = planMapper.findAll().stream()
                .filter(plan -> Objects.equals(plan.getSnapshotId(), snapshotId))
                .map(plan -> plan.getSceneId())
                .findFirst();
        if (sceneId.isPresent()) {
            return sceneId.get();
        }
        sceneId = sourceContractMapper.findAll().stream()
                .filter(item -> Objects.equals(item.getSnapshotId(), snapshotId))
                .map(item -> item.getSceneId())
                .findFirst();
        if (sceneId.isPresent()) {
            return sceneId.get();
        }
        sceneId = contractViewMapper.findAll().stream()
                .filter(item -> Objects.equals(item.getSnapshotId(), snapshotId))
                .map(item -> item.getSceneId())
                .findFirst();
        if (sceneId.isPresent()) {
            return sceneId.get();
        }
        return sourceIntakeContractMapper.findAll().stream()
                .filter(item -> Objects.equals(item.getSnapshotId(), snapshotId))
                .map(item -> item.getSceneId())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("scene not found for snapshot: " + snapshotId));
    }

    private String normalizeRootType(String rootType) {
        String text = rootType == null ? "" : rootType.trim();
        if (text.isBlank()) {
            throw new DomainValidationException("rootType is required");
        }
        return text
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}
