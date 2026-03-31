package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SnapshotProjectionPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SnapshotProjectionMapper extends JpaRepository<SnapshotProjectionPO, Long> {

    Optional<SnapshotProjectionPO> findBySceneIdAndSnapshotId(Long sceneId, Long snapshotId);

    Optional<SnapshotProjectionPO> findTopBySceneIdAndVerificationStatusOrderByVerifiedAtDesc(Long sceneId, String verificationStatus);
}
