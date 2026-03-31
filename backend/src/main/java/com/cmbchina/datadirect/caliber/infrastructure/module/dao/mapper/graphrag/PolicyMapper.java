package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PolicyPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PolicyMapper extends JpaRepository<PolicyPO, Long> {

    @Query("""
            SELECT p FROM PolicyPO p
            WHERE (:domainId IS NULL OR (p.scopeType = 'DOMAIN' AND p.scopeRefId = :domainId))
              AND (:sceneId IS NULL OR (p.scopeType = 'SCENE' AND p.scopeRefId = :sceneId))
              AND (:status IS NULL OR p.status = :status)
            ORDER BY p.updatedAt DESC
            """)
    List<PolicyPO> findByFilter(@Param("domainId") Long domainId,
                                @Param("sceneId") Long sceneId,
                                @Param("status") String status);

    List<PolicyPO> findByStatusOrderByUpdatedAtDesc(String status);

    List<PolicyPO> findByScopeTypeAndScopeRefIdAndPolicySemanticKeyOrderByUpdatedAtDesc(String scopeType,
                                                                                         Long scopeRefId,
                                                                                         String policySemanticKey);
}
