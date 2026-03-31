package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalResolutionAuditPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanonicalResolutionAuditMapper extends JpaRepository<CanonicalResolutionAuditPO, Long> {

    List<CanonicalResolutionAuditPO> findBySceneAssetTypeAndSceneAssetIdOrderByUpdatedAtDesc(String sceneAssetType, Long sceneAssetId);
}
