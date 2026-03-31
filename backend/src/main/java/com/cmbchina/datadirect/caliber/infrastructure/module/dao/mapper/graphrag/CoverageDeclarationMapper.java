package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CoverageDeclarationPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CoverageDeclarationMapper extends JpaRepository<CoverageDeclarationPO, Long> {

    @Query("""
            SELECT c FROM CoverageDeclarationPO c
            WHERE (:planId IS NULL OR c.planId = :planId)
              AND (:status IS NULL OR c.status = :status)
            ORDER BY c.updatedAt DESC
            """)
    List<CoverageDeclarationPO> findByFilter(@Param("planId") Long planId, @Param("status") String status);

    List<CoverageDeclarationPO> findByPlanIdOrderByUpdatedAtDesc(Long planId);
}
