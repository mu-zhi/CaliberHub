package com.caliberhub.infrastructure.scene.dao.mapper;

import com.caliberhub.infrastructure.scene.dao.po.AuditLogPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审计日志 Mapper
 */
@Repository
public interface AuditLogMapper extends JpaRepository<AuditLogPO, String> {
    
    List<AuditLogPO> findBySceneIdOrderByOccurredAtDesc(String sceneId);
    
    List<AuditLogPO> findBySceneIdAndActionOrderByOccurredAtDesc(String sceneId, String action);
}
