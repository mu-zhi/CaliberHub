package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceContractPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SourceContractMapper extends JpaRepository<SourceContractPO, Long> {

    @Query("""
            SELECT s FROM SourceContractPO s
            WHERE (:sceneId IS NULL OR s.sceneId = :sceneId)
              AND (:status IS NULL OR s.status = :status)
            ORDER BY s.updatedAt DESC
            """)
    List<SourceContractPO> findByFilter(@Param("sceneId") Long sceneId, @Param("status") String status);

    List<SourceContractPO> findBySceneIdOrderByUpdatedAtDesc(Long sceneId);

    List<SourceContractPO> findBySceneIdAndNormalizedPhysicalTableIgnoreCaseOrderByUpdatedAtDesc(Long sceneId,
                                                                                                  String normalizedPhysicalTable);

    List<SourceContractPO> findByPlanIdOrderByUpdatedAtDesc(Long planId);
}
