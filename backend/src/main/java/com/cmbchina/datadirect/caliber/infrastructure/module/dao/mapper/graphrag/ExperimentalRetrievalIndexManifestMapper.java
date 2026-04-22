package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.ExperimentalRetrievalIndexManifestPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExperimentalRetrievalIndexManifestMapper extends JpaRepository<ExperimentalRetrievalIndexManifestPO, Long> {

    Optional<ExperimentalRetrievalIndexManifestPO> findBySceneIdAndSnapshotId(Long sceneId, Long snapshotId);

    List<ExperimentalRetrievalIndexManifestPO> findBySceneIdOrderByCreatedAtDesc(Long sceneId);
}
