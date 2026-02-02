package com.caliberhub.domain.audit;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 审计日志 - 实体
 */
@Getter
@Builder
public class AuditLog {
    
    private final String id;
    private final String sceneId;
    private final String versionId;
    private final AuditAction action;
    private final String actor;
    private final LocalDateTime occurredAt;
    private final String summary;
    private final Map<String, Object> diff;
    private final Map<String, Object> extra;
    
    /**
     * 创建审计日志
     */
    public static AuditLog create(String sceneId, String versionId, AuditAction action, 
                                   String actor, String summary) {
        return AuditLog.builder()
                .id(UUID.randomUUID().toString())
                .sceneId(sceneId)
                .versionId(versionId)
                .action(action)
                .actor(actor)
                .occurredAt(LocalDateTime.now())
                .summary(summary)
                .diff(Map.of())
                .extra(Map.of())
                .build();
    }
    
    /**
     * 创建带 diff 的审计日志
     */
    public static AuditLog createWithDiff(String sceneId, String versionId, AuditAction action,
                                           String actor, String summary, Map<String, Object> diff) {
        return AuditLog.builder()
                .id(UUID.randomUUID().toString())
                .sceneId(sceneId)
                .versionId(versionId)
                .action(action)
                .actor(actor)
                .occurredAt(LocalDateTime.now())
                .summary(summary)
                .diff(diff != null ? diff : Map.of())
                .extra(Map.of())
                .build();
    }
}
