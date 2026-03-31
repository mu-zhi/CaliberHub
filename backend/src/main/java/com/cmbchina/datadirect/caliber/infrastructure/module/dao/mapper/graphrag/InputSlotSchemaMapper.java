package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.InputSlotSchemaPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InputSlotSchemaMapper extends JpaRepository<InputSlotSchemaPO, Long> {

    @Query("""
            SELECT s FROM InputSlotSchemaPO s
            WHERE (:sceneId IS NULL OR s.sceneId = :sceneId)
              AND (:status IS NULL OR s.status = :status)
            ORDER BY s.updatedAt DESC
            """)
    List<InputSlotSchemaPO> findByFilter(@Param("sceneId") Long sceneId, @Param("status") String status);

    List<InputSlotSchemaPO> findBySceneIdOrderByUpdatedAtDesc(Long sceneId);
}
