package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "caliber_experimental_retrieval_index_manifest",
        indexes = {
                @Index(name = "idx_exp_retrieval_manifest_scene_snapshot", columnList = "scene_id,snapshot_id"),
                @Index(name = "idx_exp_retrieval_manifest_status", columnList = "manifest_status,updated_at")
        })
public class ExperimentalRetrievalIndexManifestPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    @Column(name = "scene_code", nullable = false, length = 128)
    private String sceneCode;

    @Column(name = "version_tag", length = 128)
    private String versionTag;

    @Column(name = "index_version", nullable = false, length = 128)
    private String indexVersion;

    @Column(name = "fallback_index_version", length = 128)
    private String fallbackIndexVersion;

    @Column(name = "source_status", nullable = false, length = 32)
    private String sourceStatus;

    @Column(name = "manifest_status", nullable = false, length = 32)
    private String manifestStatus;

    @Column(name = "draft_leak_count", nullable = false)
    private Integer draftLeakCount;

    @Column(name = "summary_json", columnDefinition = "LONGTEXT")
    private String summaryJson;

    @Column(name = "failure_reason", columnDefinition = "LONGTEXT")
    private String failureReason;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public String getSceneCode() { return sceneCode; }
    public void setSceneCode(String sceneCode) { this.sceneCode = sceneCode; }
    public String getVersionTag() { return versionTag; }
    public void setVersionTag(String versionTag) { this.versionTag = versionTag; }
    public String getIndexVersion() { return indexVersion; }
    public void setIndexVersion(String indexVersion) { this.indexVersion = indexVersion; }
    public String getFallbackIndexVersion() { return fallbackIndexVersion; }
    public void setFallbackIndexVersion(String fallbackIndexVersion) { this.fallbackIndexVersion = fallbackIndexVersion; }
    public String getSourceStatus() { return sourceStatus; }
    public void setSourceStatus(String sourceStatus) { this.sourceStatus = sourceStatus; }
    public String getManifestStatus() { return manifestStatus; }
    public void setManifestStatus(String manifestStatus) { this.manifestStatus = manifestStatus; }
    public Integer getDraftLeakCount() { return draftLeakCount; }
    public void setDraftLeakCount(Integer draftLeakCount) { this.draftLeakCount = draftLeakCount; }
    public String getSummaryJson() { return summaryJson; }
    public void setSummaryJson(String summaryJson) { this.summaryJson = summaryJson; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
