package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.TimeSemanticSelectorPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimeSemanticSelectorMapper extends JpaRepository<TimeSemanticSelectorPO, Long> {

    @Query("""
            SELECT t FROM TimeSemanticSelectorPO t
            WHERE (:sceneId IS NULL OR t.sceneId = :sceneId)
              AND (:status IS NULL OR t.status = :status)
            ORDER BY t.updatedAt DESC
            """)
    List<TimeSemanticSelectorPO> findByFilter(@Param("sceneId") Long sceneId, @Param("status") String status);

    List<TimeSemanticSelectorPO> findBySceneIdOrderByUpdatedAtDesc(Long sceneId);

    List<TimeSemanticSelectorPO> findByPlanIdOrderByUpdatedAtDesc(Long planId);
}

