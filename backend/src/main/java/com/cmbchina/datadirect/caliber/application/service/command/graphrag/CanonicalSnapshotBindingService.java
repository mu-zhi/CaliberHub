package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityMembershipMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalSnapshotMembershipMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityMembershipPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalSnapshotMembershipPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class CanonicalSnapshotBindingService {

    private final CanonicalEntityMembershipMapper membershipMapper;
    private final CanonicalSnapshotMembershipMapper snapshotMembershipMapper;

    public CanonicalSnapshotBindingService(CanonicalEntityMembershipMapper membershipMapper,
                                           CanonicalSnapshotMembershipMapper snapshotMembershipMapper) {
        this.membershipMapper = membershipMapper;
        this.snapshotMembershipMapper = snapshotMembershipMapper;
    }

    @Transactional
    public void bindSceneSnapshot(Long sceneId, Long snapshotId, String operator) {
        if (sceneId == null || snapshotId == null) {
            return;
        }

        snapshotMembershipMapper.deleteBySnapshotId(snapshotId);
        OffsetDateTime now = OffsetDateTime.now();
        String normalizedOperator = normalizeOperator(operator);

        for (CanonicalEntityMembershipPO membership : membershipMapper.findBySceneIdAndActiveFlagTrueOrderByUpdatedAtDesc(sceneId)) {
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
    }

    private String normalizeOperator(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }
}
