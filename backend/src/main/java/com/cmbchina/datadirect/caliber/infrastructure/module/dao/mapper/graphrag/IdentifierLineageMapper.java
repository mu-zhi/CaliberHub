package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.IdentifierLineagePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IdentifierLineageMapper extends JpaRepository<IdentifierLineagePO, Long> {

    @Query("""
            SELECT l FROM IdentifierLineagePO l
            WHERE (:sceneId IS NULL OR l.sceneId = :sceneId)
              AND (:status IS NULL OR l.status = :status)
            ORDER BY l.updatedAt DESC
            """)
    List<IdentifierLineagePO> findByFilter(@Param("sceneId") Long sceneId, @Param("status") String status);

    List<IdentifierLineagePO> findBySceneIdOrderByUpdatedAtDesc(Long sceneId);

    List<IdentifierLineagePO> findByPlanIdOrderByUpdatedAtDesc(Long planId);
}

