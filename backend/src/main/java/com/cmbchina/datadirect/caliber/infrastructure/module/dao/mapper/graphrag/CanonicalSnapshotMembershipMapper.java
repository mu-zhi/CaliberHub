package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalSnapshotMembershipPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanonicalSnapshotMembershipMapper extends JpaRepository<CanonicalSnapshotMembershipPO, Long> {

    void deleteBySnapshotId(Long snapshotId);

    List<CanonicalSnapshotMembershipPO> findBySnapshotIdOrderByIdAsc(Long snapshotId);

    List<CanonicalSnapshotMembershipPO> findBySnapshotIdAndSceneIdOrderByUpdatedAtDesc(Long snapshotId, Long sceneId);
}
