package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.DictionaryPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DictionaryMapper extends JpaRepository<DictionaryPO, Long> {

    @Query("""
            SELECT d FROM DictionaryPO d
            WHERE (:sceneId IS NULL OR d.sceneId = :sceneId)
              AND (:status IS NULL OR d.status = :status)
            ORDER BY d.updatedAt DESC
            """)
    List<DictionaryPO> findByFilter(@Param("sceneId") Long sceneId, @Param("status") String status);

    List<DictionaryPO> findBySceneIdOrderByUpdatedAtDesc(Long sceneId);

    List<DictionaryPO> findByPlanIdOrderByUpdatedAtDesc(Long planId);
}

