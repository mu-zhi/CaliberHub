package com.caliberhub.domain.audit;

import java.util.List;

/**
 * 审计日志仓储接口
 */
public interface AuditLogRepository {
    
    /**
     * 保存审计日志
     */
    void save(AuditLog auditLog);
    
    /**
     * 根据场景ID查询审计日志
     */
    List<AuditLog> findBySceneId(String sceneId);
    
    /**
     * 根据场景ID和操作类型查询
     */
    List<AuditLog> findBySceneIdAndAction(String sceneId, AuditAction action);
}
