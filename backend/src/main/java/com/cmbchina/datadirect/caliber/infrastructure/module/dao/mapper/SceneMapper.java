package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SceneMapper extends JpaRepository<ScenePO, Long> {

    @Query("""
            SELECT s FROM ScenePO s
            WHERE (:domainId IS NULL OR s.domainId = :domainId)
              AND (:domain IS NULL OR s.domain = :domain)
              AND (:status IS NULL OR s.status = :status)
              AND (:keyword IS NULL OR :keyword = ''
                   OR lower(s.sceneTitle) LIKE lower(concat('%', :keyword, '%'))
                   OR lower(cast(coalesce(s.sceneDescription, '') as string)) LIKE lower(concat('%', :keyword, '%'))
                   OR lower(cast(coalesce(s.sqlVariantsJson, '') as string)) LIKE lower(concat('%', :keyword, '%'))
                   OR lower(cast(coalesce(s.sqlBlocksJson, '') as string)) LIKE lower(concat('%', :keyword, '%')))
            ORDER BY s.updatedAt DESC
            """)
    List<ScenePO> findByCondition(@Param("domainId") Long domainId,
                                  @Param("domain") String domain,
                                  @Param("status") SceneStatus status,
                                  @Param("keyword") String keyword);
}
