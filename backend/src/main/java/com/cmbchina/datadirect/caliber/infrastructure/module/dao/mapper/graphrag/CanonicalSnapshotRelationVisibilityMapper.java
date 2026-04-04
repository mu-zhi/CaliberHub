package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalSnapshotRelationVisibilityPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanonicalSnapshotRelationVisibilityMapper extends JpaRepository<CanonicalSnapshotRelationVisibilityPO, Long> {

    void deleteBySnapshotId(Long snapshotId);

    List<CanonicalSnapshotRelationVisibilityPO> findBySnapshotIdAndSceneIdOrderByUpdatedAtDesc(Long snapshotId, Long sceneId);
}
