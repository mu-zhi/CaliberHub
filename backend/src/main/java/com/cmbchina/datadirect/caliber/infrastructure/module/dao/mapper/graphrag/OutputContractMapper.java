package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.OutputContractPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutputContractMapper extends JpaRepository<OutputContractPO, Long> {

    @Query("""
            SELECT c FROM OutputContractPO c
            WHERE (:sceneId IS NULL OR c.sceneId = :sceneId)
              AND (:status IS NULL OR c.status = :status)
            ORDER BY c.updatedAt DESC
            """)
    List<OutputContractPO> findByFilter(@Param("sceneId") Long sceneId, @Param("status") String status);

    List<OutputContractPO> findBySceneIdOrderByUpdatedAtDesc(Long sceneId);

    List<OutputContractPO> findBySceneIdAndContractSemanticKeyOrderByUpdatedAtDesc(Long sceneId,
                                                                                    String contractSemanticKey);
}
