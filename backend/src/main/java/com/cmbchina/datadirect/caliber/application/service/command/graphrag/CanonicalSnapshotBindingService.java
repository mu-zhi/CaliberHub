package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityMembershipMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityRelationMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalSnapshotMembershipMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalSnapshotRelationVisibilityMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityMembershipPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityRelationPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalSnapshotMembershipPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalSnapshotRelationVisibilityPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CanonicalSnapshotBindingService {

    private final CanonicalEntityMembershipMapper membershipMapper;
    private final CanonicalSnapshotMembershipMapper snapshotMembershipMapper;
    private final CanonicalEntityRelationMapper relationMapper;
    private final CanonicalSnapshotRelationVisibilityMapper snapshotRelationVisibilityMapper;

    public CanonicalSnapshotBindingService(CanonicalEntityMembershipMapper membershipMapper,
                                           CanonicalSnapshotMembershipMapper snapshotMembershipMapper,
                                           CanonicalEntityRelationMapper relationMapper,
                                           CanonicalSnapshotRelationVisibilityMapper snapshotRelationVisibilityMapper) {
        this.membershipMapper = membershipMapper;
        this.snapshotMembershipMapper = snapshotMembershipMapper;
        this.relationMapper = relationMapper;
        this.snapshotRelationVisibilityMapper = snapshotRelationVisibilityMapper;
    }

    @Transactional
    public void bindSceneSnapshot(Long sceneId, Long snapshotId, String operator) {
        if (sceneId == null || snapshotId == null) {
            return;
        }

        snapshotMembershipMapper.deleteBySnapshotId(snapshotId);
        snapshotRelationVisibilityMapper.deleteBySnapshotId(snapshotId);
        OffsetDateTime now = OffsetDateTime.now();
        String normalizedOperator = normalizeOperator(operator);

        java.util.List<CanonicalEntityMembershipPO> activeMemberships =
                membershipMapper.findBySceneIdAndActiveFlagTrueOrderByUpdatedAtDesc(sceneId);
        for (CanonicalEntityMembershipPO membership : activeMemberships) {
            CanonicalSnapshotMembershipPO snapshotMembership = new CanonicalSnapshotMembershipPO();
            snapshotMembership.setSnapshotId(snapshotId);
            snapshotMembership.setSceneId(sceneId);
            snapshotMembership.setCanonicalEntityId(membership.getCanonicalEntityId());
            snapshotMembership.setSceneAssetType(membership.getSceneAssetType());
            snapshotMembership.setSceneAssetId(membership.getSceneAssetId());
            snapshotMembership.setSourceMembershipId(membership.getId());
            snapshotMembership.setCreatedBy(normalizedOperator);
            snapshotMembership.setCreatedAt(now);
            snapshotMembership.setUpdatedBy(normalizedOperator);
            snapshotMembership.setUpdatedAt(now);
            snapshotMembershipMapper.save(snapshotMembership);
        }

        bindVisibleRelations(sceneId, snapshotId, normalizedOperator, now, activeMemberships);
    }

    private void bindVisibleRelations(Long sceneId,
                                      Long snapshotId,
                                      String operator,
                                      OffsetDateTime now,
                                      java.util.List<CanonicalEntityMembershipPO> activeMemberships) {
        Set<Long> activeCanonicalEntityIds = activeMemberships.stream()
                .map(CanonicalEntityMembershipPO::getCanonicalEntityId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (activeCanonicalEntityIds.isEmpty()) {
            return;
        }

        for (Long sourceCanonicalEntityId : activeCanonicalEntityIds) {
            for (CanonicalEntityRelationPO relation : relationMapper.findBySourceCanonicalEntityIdOrderByUpdatedAtDesc(sourceCanonicalEntityId)) {
                if (relation == null || relation.getId() == null || !relation.isVisibleInSnapshotBinding()) {
                    continue;
                }
                if (!activeCanonicalEntityIds.contains(relation.getTargetCanonicalEntityId())) {
                    continue;
                }
                CanonicalSnapshotRelationVisibilityPO visibility = new CanonicalSnapshotRelationVisibilityPO();
                visibility.setSnapshotId(snapshotId);
                visibility.setSceneId(sceneId);
                visibility.setCanonicalRelationId(relation.getId());
                visibility.setSourceCanonicalEntityId(relation.getSourceCanonicalEntityId());
                visibility.setTargetCanonicalEntityId(relation.getTargetCanonicalEntityId());
                visibility.setRelationType(relation.getRelationType());
                visibility.setSourceRelationId(relation.getId());
                visibility.setCreatedBy(operator);
                visibility.setCreatedAt(now);
                visibility.setUpdatedBy(operator);
                visibility.setUpdatedAt(now);
                snapshotRelationVisibilityMapper.save(visibility);
            }
        }
    }

    private String normalizeOperator(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }
}
