package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;

@Entity
@Table(name = "caliber_canonical_entity_membership",
        uniqueConstraints = @UniqueConstraint(name = "uk_canonical_membership_asset", columnNames = {"scene_asset_type", "scene_asset_id"}),
        indexes = @Index(name = "idx_canonical_membership_entity_active", columnList = "canonical_entity_id,active_flag,updated_at"))
public class CanonicalEntityMembershipPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canonical_entity_id", nullable = false)
    private Long canonicalEntityId;

    @Column(name = "scene_asset_type", nullable = false, length = 64)
    private String sceneAssetType;

    @Column(name = "scene_asset_id", nullable = false)
    private Long sceneAssetId;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "match_basis", nullable = false, length = 128)
    private String matchBasis;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "manual_override", nullable = false)
    private boolean manualOverride;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCanonicalEntityId() { return canonicalEntityId; }
    public void setCanonicalEntityId(Long canonicalEntityId) { this.canonicalEntityId = canonicalEntityId; }
    public String getSceneAssetType() { return sceneAssetType; }
    public void setSceneAssetType(String sceneAssetType) { this.sceneAssetType = sceneAssetType; }
    public Long getSceneAssetId() { return sceneAssetId; }
    public void setSceneAssetId(Long sceneAssetId) { this.sceneAssetId = sceneAssetId; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public String getMatchBasis() { return matchBasis; }
    public void setMatchBasis(String matchBasis) { this.matchBasis = matchBasis; }
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    public boolean isManualOverride() { return manualOverride; }
    public void setManualOverride(boolean manualOverride) { this.manualOverride = manualOverride; }
    public boolean isActiveFlag() { return activeFlag; }
    public void setActiveFlag(boolean activeFlag) { this.activeFlag = activeFlag; }
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
