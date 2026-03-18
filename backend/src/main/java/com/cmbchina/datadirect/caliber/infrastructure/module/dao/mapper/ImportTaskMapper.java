package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportTaskPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImportTaskMapper extends JpaRepository<ImportTaskPO, String> {

    @Query("""
            SELECT t FROM ImportTaskPO t
            WHERE (:status IS NULL OR upper(t.status) = :status)
              AND (:operator IS NULL OR t.operator = :operator)
            ORDER BY t.updatedAt DESC
            """)
    List<ImportTaskPO> findRecent(@Param("status") String status,
                                  @Param("operator") String operator,
                                  org.springframework.data.domain.Pageable pageable);
}
