package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.AuditEventMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.AuditEventPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class GraphAuditEventAppService {

    private final AuditEventMapper auditEventMapper;
    private final ObjectMapper objectMapper;

    public GraphAuditEventAppService(AuditEventMapper auditEventMapper, ObjectMapper objectMapper) {
        this.auditEventMapper = auditEventMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(Long sceneId,
                       String eventName,
                       String traceId,
                       Long snapshotId,
                       String operator,
                       String jobId,
                       String reasonCode,
                       String status,
                       Object payload) {
        OffsetDateTime now = OffsetDateTime.now();
        AuditEventPO event = new AuditEventPO();
        event.setSceneId(sceneId);
        event.setEventName(eventName);
        event.setTraceId(traceId);
        event.setSnapshotId(snapshotId);
        event.setOperatorId(normalize(operator));
        event.setJobId(jobId);
        event.setReasonCode(reasonCode);
        event.setPayloadJson(writeJson(payload));
        event.setStatus(status == null || status.isBlank() ? "RECORDED" : status.trim().toUpperCase());
        event.setCreatedBy(normalize(operator));
        event.setCreatedAt(now);
        event.setUpdatedBy(normalize(operator));
        event.setUpdatedAt(now);
        auditEventMapper.save(event);
    }

    private String writeJson(Object payload) {
        try {
            return payload == null ? null : objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{\"serializationError\":true}";
        }
    }

    private String normalize(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }
}
