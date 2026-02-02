package com.caliberhub.infrastructure.scene.supportimpl;

import com.caliberhub.domain.audit.AuditAction;
import com.caliberhub.domain.audit.AuditLog;
import com.caliberhub.domain.audit.AuditLogRepository;
import com.caliberhub.infrastructure.scene.dao.mapper.AuditLogMapper;
import com.caliberhub.infrastructure.scene.dao.po.AuditLogPO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 审计日志仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepository {
    
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    @Override
    public void save(AuditLog auditLog) {
        AuditLogPO po = toPO(auditLog);
        auditLogMapper.save(po);
    }
    
    @Override
    public List<AuditLog> findBySceneId(String sceneId) {
        return auditLogMapper.findBySceneIdOrderByOccurredAtDesc(sceneId)
                .stream()
                .map(this::toDomain)
                .toList();
    }
    
    @Override
    public List<AuditLog> findBySceneIdAndAction(String sceneId, AuditAction action) {
        return auditLogMapper.findBySceneIdAndActionOrderByOccurredAtDesc(sceneId, action.name())
                .stream()
                .map(this::toDomain)
                .toList();
    }
    
    private AuditLogPO toPO(AuditLog auditLog) {
        return AuditLogPO.builder()
                .id(auditLog.getId())
                .sceneId(auditLog.getSceneId())
                .versionId(auditLog.getVersionId())
                .action(auditLog.getAction().name())
                .actor(auditLog.getActor())
                .occurredAt(auditLog.getOccurredAt().format(FORMATTER))
                .summary(auditLog.getSummary())
                .diffJson(toJson(auditLog.getDiff()))
                .extraJson(toJson(auditLog.getExtra()))
                .build();
    }
    
    private AuditLog toDomain(AuditLogPO po) {
        return AuditLog.builder()
                .id(po.getId())
                .sceneId(po.getSceneId())
                .versionId(po.getVersionId())
                .action(AuditAction.valueOf(po.getAction()))
                .actor(po.getActor())
                .occurredAt(LocalDateTime.parse(po.getOccurredAt(), FORMATTER))
                .summary(po.getSummary())
                .diff(fromJson(po.getDiffJson()))
                .extra(fromJson(po.getExtraJson()))
                .build();
    }
    
    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map != null ? map : Map.of());
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败", e);
            return "{}";
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("JSON 反序列化失败", e);
            return Map.of();
        }
    }
}
