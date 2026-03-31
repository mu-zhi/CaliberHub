package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityMembershipPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CanonicalEntityMembershipMapper extends JpaRepository<CanonicalEntityMembershipPO, Long> {

    Optional<CanonicalEntityMembershipPO> findBySceneAssetTypeAndSceneAssetId(String sceneAssetType, Long sceneAssetId);

    List<CanonicalEntityMembershipPO> findByCanonicalEntityIdAndActiveFlagTrueOrderByUpdatedAtDesc(Long canonicalEntityId);

    List<CanonicalEntityMembershipPO> findBySceneIdAndActiveFlagTrueOrderByUpdatedAtDesc(Long sceneId);

    List<CanonicalEntityMembershipPO> findBySceneIdAndSceneAssetTypeOrderByUpdatedAtDesc(Long sceneId, String sceneAssetType);
}
