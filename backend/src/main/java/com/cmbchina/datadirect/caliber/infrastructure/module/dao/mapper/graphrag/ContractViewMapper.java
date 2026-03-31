package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.ContractViewPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContractViewMapper extends JpaRepository<ContractViewPO, Long> {

    @Query("""
            SELECT v FROM ContractViewPO v
            WHERE (:sceneId IS NULL OR v.sceneId = :sceneId)
              AND (:status IS NULL OR v.status = :status)
            ORDER BY v.updatedAt DESC
            """)
    List<ContractViewPO> findByFilter(@Param("sceneId") Long sceneId, @Param("status") String status);

    List<ContractViewPO> findBySceneIdOrderByUpdatedAtDesc(Long sceneId);

    List<ContractViewPO> findByPlanIdOrderByUpdatedAtDesc(Long planId);
}
