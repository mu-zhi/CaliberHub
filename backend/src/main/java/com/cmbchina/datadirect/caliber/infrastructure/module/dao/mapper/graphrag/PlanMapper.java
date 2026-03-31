package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlanMapper extends JpaRepository<PlanPO, Long> {

    @Query("""
            SELECT p FROM PlanPO p
            WHERE (:sceneId IS NULL OR p.sceneId = :sceneId)
              AND (:status IS NULL OR p.status = :status)
            ORDER BY p.updatedAt DESC
            """)
    List<PlanPO> findByFilter(@Param("sceneId") Long sceneId, @Param("status") String status);

    List<PlanPO> findBySceneIdOrderByUpdatedAtDesc(Long sceneId);

    Optional<PlanPO> findByPlanCode(String planCode);
}
