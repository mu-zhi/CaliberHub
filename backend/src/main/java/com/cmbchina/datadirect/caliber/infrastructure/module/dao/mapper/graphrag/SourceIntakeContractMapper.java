package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceIntakeContractPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SourceIntakeContractMapper extends JpaRepository<SourceIntakeContractPO, Long> {

    @Query("""
            SELECT s FROM SourceIntakeContractPO s
            WHERE (:sceneId IS NULL OR s.sceneId = :sceneId)
              AND (:status IS NULL OR s.status = :status)
            ORDER BY s.updatedAt DESC
            """)
    List<SourceIntakeContractPO> findByFilter(@Param("sceneId") Long sceneId, @Param("status") String status);

    List<SourceIntakeContractPO> findBySceneIdOrderByUpdatedAtDesc(Long sceneId);
}
