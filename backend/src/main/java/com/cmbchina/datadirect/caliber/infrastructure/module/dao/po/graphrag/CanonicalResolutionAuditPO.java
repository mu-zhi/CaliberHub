package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;

@Entity
@Table(name = "caliber_canonical_resolution_audit",
        indexes = @Index(name = "idx_canonical_audit_asset", columnList = "scene_asset_type,scene_asset_id,updated_at"))
public class CanonicalResolutionAuditPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "scene_asset_type", nullable = false, length = 64)
    private String sceneAssetType;

    @Column(name = "scene_asset_id", nullable = false)
    private Long sceneAssetId;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "canonical_entity_id")
    private Long canonicalEntityId;

    @Column(name = "suggested_canonical_key", length = 255)
    private String suggestedCanonicalKey;

    @Column(name = "decision", nullable = false, length = 32)
    private String decision;

    @Column(name = "match_basis", length = 128)
    private String matchBasis;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "resolution_rule_version", length = 64)
    private String resolutionRuleVersion;

    @Column(name = "decision_reason", columnDefinition = "LONGTEXT")
    private String decisionReason;

    @Column(name = "manual_override", nullable = false)
    private boolean manualOverride;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_by", nullable = false, length = 64)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    public Long getAuditId() { return auditId; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getSceneAssetType() { return sceneAssetType; }
    public void setSceneAssetType(String sceneAssetType) { this.sceneAssetType = sceneAssetType; }
    public Long getSceneAssetId() { return sceneAssetId; }
    public void setSceneAssetId(Long sceneAssetId) { this.sceneAssetId = sceneAssetId; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public Long getCanonicalEntityId() { return canonicalEntityId; }
    public void setCanonicalEntityId(Long canonicalEntityId) { this.canonicalEntityId = canonicalEntityId; }
    public String getSuggestedCanonicalKey() { return suggestedCanonicalKey; }
    public void setSuggestedCanonicalKey(String suggestedCanonicalKey) { this.suggestedCanonicalKey = suggestedCanonicalKey; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getMatchBasis() { return matchBasis; }
    public void setMatchBasis(String matchBasis) { this.matchBasis = matchBasis; }
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getResolutionRuleVersion() { return resolutionRuleVersion; }
    public void setResolutionRuleVersion(String resolutionRuleVersion) { this.resolutionRuleVersion = resolutionRuleVersion; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
    public boolean isManualOverride() { return manualOverride; }
    public void setManualOverride(boolean manualOverride) { this.manualOverride = manualOverride; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getRowVersion() { return rowVersion; }
    public void setRowVersion(Long rowVersion) { this.rowVersion = rowVersion; }
}
