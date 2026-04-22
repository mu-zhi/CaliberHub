package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.AuditEventMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.AuditEventPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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
        applyRetrievalExperimentPayload(event, payload);
        event.setPayloadJson(writeJson(payload));
        event.setStatus(status == null || status.isBlank() ? "RECORDED" : status.trim().toUpperCase());
        event.setCreatedBy(normalize(operator));
        event.setCreatedAt(now);
        event.setUpdatedBy(normalize(operator));
        event.setUpdatedAt(now);
        auditEventMapper.save(event);
    }

    @Transactional
    public void recordRetrievalExperiment(Long sceneId,
                                          String traceId,
                                          Long snapshotId,
                                          String operator,
                                          String jobId,
                                          RetrievalExperimentAuditPayload payload) {
        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("adapter_name", payload.adapterName());
        payloadMap.put("index_version", payload.indexVersion());
        payloadMap.put("latency_ms", payload.latencyMs());
        payloadMap.put("reference_count", payload.referenceCount());
        payloadMap.put("candidate_count", payload.candidateCount());
        payloadMap.put("shadow_mode_enabled", payload.shadowModeEnabled());
        payloadMap.put("gray_release_scope", payload.grayReleaseScope());
        payloadMap.put("false_allow_risk", payload.falseAllowRisk());
        payloadMap.put("rollback_recommendation", payload.rollbackRecommendation());
        record(
                sceneId,
                "RUNTIME_RETRIEVAL_EXPERIMENT",
                traceId,
                snapshotId,
                operator,
                jobId,
                payload.falseAllowRisk() ? "FALSE_ALLOW_RISK" : "RETRIEVAL_EXPERIMENT_OK",
                "RECORDED",
                payloadMap
        );
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

    @SuppressWarnings("unchecked")
    private void applyRetrievalExperimentPayload(AuditEventPO event, Object payload) {
        if (payload instanceof RetrievalExperimentAuditPayload retrievalPayload) {
            event.setAdapterName(retrievalPayload.adapterName());
            event.setIndexVersion(retrievalPayload.indexVersion());
            event.setLatencyMs(retrievalPayload.latencyMs());
            event.setReferenceCount(retrievalPayload.referenceCount());
            event.setCandidateCount(retrievalPayload.candidateCount());
            event.setShadowModeEnabled(retrievalPayload.shadowModeEnabled());
            event.setGrayReleaseScope(retrievalPayload.grayReleaseScope());
            event.setFalseAllowRisk(retrievalPayload.falseAllowRisk());
            event.setRollbackRecommendation(retrievalPayload.rollbackRecommendation());
            return;
        }
        if (!(payload instanceof Map<?, ?> payloadMap)) {
            return;
        }
        event.setAdapterName(textValue(payloadMap.get("adapter_name")));
        event.setIndexVersion(textValue(payloadMap.get("index_version")));
        event.setLatencyMs(longValue(payloadMap.get("latency_ms")));
        event.setReferenceCount(intValue(payloadMap.get("reference_count")));
        event.setCandidateCount(intValue(payloadMap.get("candidate_count")));
        event.setShadowModeEnabled(booleanValue(payloadMap.get("shadow_mode_enabled")));
        event.setGrayReleaseScope(textValue(payloadMap.get("gray_release_scope")));
        event.setFalseAllowRisk(booleanValue(payloadMap.get("false_allow_risk")));
        event.setRollbackRecommendation(textValue(payloadMap.get("rollback_recommendation")));
    }

    private String textValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean booleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return "true".equalsIgnoreCase(String.valueOf(value).trim()) || "1".equals(String.valueOf(value).trim());
    }

    public record RetrievalExperimentAuditPayload(
            String adapterName,
            String indexVersion,
            Long latencyMs,
            Integer referenceCount,
            Integer candidateCount,
            Boolean shadowModeEnabled,
            String grayReleaseScope,
            Boolean falseAllowRisk,
            String rollbackRecommendation
    ) {
    }
}
