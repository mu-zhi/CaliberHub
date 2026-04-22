package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "caliber_audit_event",
        indexes = {
                @Index(name = "idx_audit_event_scene_status", columnList = "scene_id,status,updated_at"),
                @Index(name = "idx_audit_event_trace", columnList = "trace_id,created_at")
        })
public class AuditEventPO extends AbstractGraphAuditablePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id")
    private Long sceneId;

    @Column(name = "event_name", nullable = false, length = 128)
    private String eventName;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Column(name = "operator_id", length = 64)
    private String operatorId;

    @Column(name = "job_id", length = 64)
    private String jobId;

    @Column(name = "reason_code", length = 64)
    private String reasonCode;

    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Transient
    private String adapterName;

    @Transient
    private String indexVersion;

    @Transient
    private Long latencyMs;

    @Transient
    private Integer referenceCount;

    @Transient
    private Integer candidateCount;

    @Transient
    private Boolean shadowModeEnabled;

    @Transient
    private String grayReleaseScope;

    @Transient
    private Boolean falseAllowRisk;

    @Transient
    private String rollbackRecommendation;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }
    public String getIndexVersion() { return indexVersion; }
    public void setIndexVersion(String indexVersion) { this.indexVersion = indexVersion; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public Integer getReferenceCount() { return referenceCount; }
    public void setReferenceCount(Integer referenceCount) { this.referenceCount = referenceCount; }
    public Integer getCandidateCount() { return candidateCount; }
    public void setCandidateCount(Integer candidateCount) { this.candidateCount = candidateCount; }
    public Boolean getShadowModeEnabled() { return shadowModeEnabled; }
    public void setShadowModeEnabled(Boolean shadowModeEnabled) { this.shadowModeEnabled = shadowModeEnabled; }
    public String getGrayReleaseScope() { return grayReleaseScope; }
    public void setGrayReleaseScope(String grayReleaseScope) { this.grayReleaseScope = grayReleaseScope; }
    public Boolean getFalseAllowRisk() { return falseAllowRisk; }
    public void setFalseAllowRisk(Boolean falseAllowRisk) { this.falseAllowRisk = falseAllowRisk; }
    public String getRollbackRecommendation() { return rollbackRecommendation; }
    public void setRollbackRecommendation(String rollbackRecommendation) { this.rollbackRecommendation = rollbackRecommendation; }
}
