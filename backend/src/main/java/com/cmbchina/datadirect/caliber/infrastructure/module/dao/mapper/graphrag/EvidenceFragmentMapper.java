package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EvidenceFragmentPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EvidenceFragmentMapper extends JpaRepository<EvidenceFragmentPO, Long> {

    @Query("""
            SELECT e FROM EvidenceFragmentPO e
            WHERE (:sceneId IS NULL OR e.sceneId = :sceneId)
              AND (:status IS NULL OR e.status = :status)
            ORDER BY e.updatedAt DESC
            """)
    List<EvidenceFragmentPO> findByFilter(@Param("sceneId") Long sceneId, @Param("status") String status);

    List<EvidenceFragmentPO> findBySceneIdOrderByUpdatedAtDesc(Long sceneId);

    List<EvidenceFragmentPO> findBySceneIdAndOriginTypeAndOriginRefOrderByUpdatedAtDesc(Long sceneId,
                                                                                         String originType,
                                                                                         String originRef);
}
