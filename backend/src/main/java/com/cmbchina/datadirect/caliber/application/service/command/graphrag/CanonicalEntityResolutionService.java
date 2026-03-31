package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityMembershipMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PolicyMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityMembershipPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EvidenceFragmentPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.OutputContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PolicyPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceContractPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class CanonicalEntityResolutionService {

    private static final String SOURCE_CONTRACT = "SOURCE_CONTRACT";
    private static final String POLICY = "POLICY";
    private static final String EVIDENCE = "EVIDENCE";
    private static final String OUTPUT_CONTRACT = "OUTPUT_CONTRACT";

    private final CanonicalKeyFactory canonicalKeyFactory;
    private final CanonicalEntityMapper canonicalEntityMapper;
    private final CanonicalEntityMembershipMapper membershipMapper;
    private final SourceContractMapper sourceContractMapper;
    private final PolicyMapper policyMapper;
    private final EvidenceFragmentMapper evidenceFragmentMapper;
    private final OutputContractMapper outputContractMapper;

    public CanonicalEntityResolutionService(CanonicalKeyFactory canonicalKeyFactory,
                                            CanonicalEntityMapper canonicalEntityMapper,
                                            CanonicalEntityMembershipMapper membershipMapper,
                                            SourceContractMapper sourceContractMapper,
                                            PolicyMapper policyMapper,
                                            EvidenceFragmentMapper evidenceFragmentMapper,
                                            OutputContractMapper outputContractMapper) {
        this.canonicalKeyFactory = canonicalKeyFactory;
        this.canonicalEntityMapper = canonicalEntityMapper;
        this.membershipMapper = membershipMapper;
        this.sourceContractMapper = sourceContractMapper;
        this.policyMapper = policyMapper;
        this.evidenceFragmentMapper = evidenceFragmentMapper;
        this.outputContractMapper = outputContractMapper;
    }

    @Transactional
    public void resolveScene(Long sceneId, String operator) {
        if (sceneId == null) {
            return;
        }

        resolveSourceContracts(sceneId, operator);
        resolvePolicies(sceneId, operator);
        resolveEvidences(sceneId, operator);
        resolveOutputContracts(sceneId, operator);
    }

    private void resolveSourceContracts(Long sceneId, String operator) {
        List<SourceContractPO> contracts = sourceContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        Set<Long> activeAssetIds = new LinkedHashSet<>();
        contracts.forEach(contract -> {
            activeAssetIds.add(contract.getId());
            resolveAsset(
                    SOURCE_CONTRACT,
                    contract.getId(),
                    contract.getSceneId(),
                    contract.getStatus(),
                    canonicalKeyFactory.buildSourceContractKey(contract),
                    contract.getSourceName(),
                    sourceProfile(contract),
                    operator
            );
        });
        deactivateMissingMemberships(sceneId, SOURCE_CONTRACT, activeAssetIds, operator);
    }

    private void resolvePolicies(Long sceneId, String operator) {
        List<PolicyPO> policies = policyMapper.findByFilter(null, sceneId, null);
        Set<Long> activeAssetIds = new LinkedHashSet<>();
        policies.forEach(policy -> {
            activeAssetIds.add(policy.getId());
            resolveAsset(
                    POLICY,
                    policy.getId(),
                    sceneId,
                    policy.getStatus(),
                    canonicalKeyFactory.buildPolicyKey(policy),
                    policy.getPolicyName(),
                    policyProfile(policy),
                    operator
            );
        });
        deactivateMissingMemberships(sceneId, POLICY, activeAssetIds, operator);
    }

    private void resolveEvidences(Long sceneId, String operator) {
        List<EvidenceFragmentPO> evidences = evidenceFragmentMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        Set<Long> activeAssetIds = new LinkedHashSet<>();
        evidences.forEach(evidence -> {
            activeAssetIds.add(evidence.getId());
            resolveAsset(
                    EVIDENCE,
                    evidence.getId(),
                    evidence.getSceneId(),
                    evidence.getStatus(),
                    canonicalKeyFactory.buildEvidenceKey(evidence),
                    evidence.getTitle(),
                    evidenceProfile(evidence),
                    operator
            );
        });
        deactivateMissingMemberships(sceneId, EVIDENCE, activeAssetIds, operator);
    }

    private void resolveOutputContracts(Long sceneId, String operator) {
        List<OutputContractPO> contracts = outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        Set<Long> activeAssetIds = new LinkedHashSet<>();
        contracts.forEach(contract -> {
            activeAssetIds.add(contract.getId());
            resolveAsset(
                    OUTPUT_CONTRACT,
                    contract.getId(),
                    contract.getSceneId(),
                    contract.getStatus(),
                    canonicalKeyFactory.buildOutputContractKey(contract),
                    contract.getContractName(),
                    outputProfile(contract),
                    operator
            );
        });
        deactivateMissingMemberships(sceneId, OUTPUT_CONTRACT, activeAssetIds, operator);
    }

    private void resolveAsset(String entityType,
                              Long sceneAssetId,
                              Long sceneId,
                              String assetStatus,
                              Optional<String> stableKey,
                              String displayName,
                              String profileJson,
                              String operator) {
        if (sceneAssetId == null || sceneId == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        boolean activeAsset = isActiveAsset(assetStatus);
        boolean hasStableKey = stableKey.isPresent();
        String canonicalKey = hasStableKey
                ? stableKey.get()
                : entityType + "::REVIEW::" + sceneAssetId;

        CanonicalEntityPO entity = canonicalEntityMapper.findByEntityTypeAndCanonicalKey(entityType, canonicalKey)
                .orElseGet(CanonicalEntityPO::new);
        if (entity.getId() == null) {
            entity.setCreatedBy(normalizeOperator(operator));
            entity.setCreatedAt(now);
        }
        entity.setEntityType(entityType);
        entity.setCanonicalKey(canonicalKey);
        entity.setDisplayName(isBlank(displayName) ? canonicalKey : displayName.trim());
        entity.setResolutionStatus(hasStableKey ? "ACTIVE" : "NEEDS_REVIEW");
        entity.setLifecycleStatus(activeAsset ? "ACTIVE" : "INACTIVE");
        entity.setProfileJson(profileJson);
        entity.setUpdatedBy(normalizeOperator(operator));
        entity.setUpdatedAt(now);
        CanonicalEntityPO savedEntity = canonicalEntityMapper.save(entity);

        CanonicalEntityMembershipPO membership = membershipMapper.findBySceneAssetTypeAndSceneAssetId(entityType, sceneAssetId)
                .orElseGet(CanonicalEntityMembershipPO::new);
        if (membership.getId() == null) {
            membership.setCreatedBy(normalizeOperator(operator));
            membership.setCreatedAt(now);
        }
        membership.setCanonicalEntityId(savedEntity.getId());
        membership.setSceneAssetType(entityType);
        membership.setSceneAssetId(sceneAssetId);
        membership.setSceneId(sceneId);
        membership.setMatchBasis(hasStableKey ? "canonical_key" : "missing_key");
        membership.setConfidenceScore(hasStableKey ? 1.0d : 0.0d);
        membership.setManualOverride(false);
        membership.setActiveFlag(activeAsset);
        membership.setUpdatedBy(normalizeOperator(operator));
        membership.setUpdatedAt(now);
        membershipMapper.save(membership);

        refreshEntityLifecycle(savedEntity.getId(), operator);
    }

    private void deactivateMissingMemberships(Long sceneId,
                                              String entityType,
                                              Set<Long> activeAssetIds,
                                              String operator) {
        OffsetDateTime now = OffsetDateTime.now();
        membershipMapper.findBySceneIdAndSceneAssetTypeOrderByUpdatedAtDesc(sceneId, entityType).stream()
                .filter(CanonicalEntityMembershipPO::isActiveFlag)
                .filter(membership -> !activeAssetIds.contains(membership.getSceneAssetId()))
                .forEach(membership -> {
                    membership.setActiveFlag(false);
                    membership.setUpdatedBy(normalizeOperator(operator));
                    membership.setUpdatedAt(now);
                    membershipMapper.save(membership);
                    refreshEntityLifecycle(membership.getCanonicalEntityId(), operator);
                });
    }

    private void refreshEntityLifecycle(Long canonicalEntityId, String operator) {
        CanonicalEntityPO entity = canonicalEntityMapper.findById(canonicalEntityId).orElse(null);
        if (entity == null) {
            return;
        }
        boolean anyActiveMembership = !membershipMapper
                .findByCanonicalEntityIdAndActiveFlagTrueOrderByUpdatedAtDesc(canonicalEntityId)
                .isEmpty();
        entity.setLifecycleStatus(anyActiveMembership ? "ACTIVE" : "INACTIVE");
        entity.setUpdatedBy(normalizeOperator(operator));
        entity.setUpdatedAt(OffsetDateTime.now());
        canonicalEntityMapper.save(entity);
    }

    private boolean isActiveAsset(String assetStatus) {
        if (isBlank(assetStatus)) {
            return true;
        }
        return !"DEPRECATED".equalsIgnoreCase(assetStatus)
                && !"DISCARDED".equalsIgnoreCase(assetStatus)
                && !"INACTIVE".equalsIgnoreCase(assetStatus);
    }

    private String sourceProfile(SourceContractPO contract) {
        Map<String, String> profile = new LinkedHashMap<>();
        profile.put("sourceSystem", contract.getSourceSystem());
        profile.put("normalizedPhysicalTable", contract.getNormalizedPhysicalTable());
        profile.put("physicalTable", contract.getPhysicalTable());
        return toJson(profile);
    }

    private String policyProfile(PolicyPO policy) {
        Map<String, String> profile = new LinkedHashMap<>();
        profile.put("policySemanticKey", policy.getPolicySemanticKey());
        profile.put("scopeType", policy.getScopeType());
        profile.put("scopeRefId", policy.getScopeRefId() == null ? null : String.valueOf(policy.getScopeRefId()));
        return toJson(profile);
    }

    private String evidenceProfile(EvidenceFragmentPO evidence) {
        Map<String, String> profile = new LinkedHashMap<>();
        profile.put("originType", evidence.getOriginType());
        profile.put("originRef", evidence.getOriginRef());
        profile.put("originLocator", evidence.getOriginLocator());
        return toJson(profile);
    }

    private String outputProfile(OutputContractPO contract) {
        Map<String, String> profile = new LinkedHashMap<>();
        profile.put("contractSemanticKey", contract.getContractSemanticKey());
        profile.put("contractName", contract.getContractName());
        return toJson(profile);
    }

    private String toJson(Map<String, String> profile) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : profile.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(escape(entry.getKey())).append('"').append(':');
            if (entry.getValue() == null) {
                builder.append("null");
            } else {
                builder.append('"').append(escape(entry.getValue())).append('"');
            }
            first = false;
        }
        builder.append('}');
        return builder.toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String normalizeOperator(String operator) {
        return isBlank(operator) ? "system" : operator.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
