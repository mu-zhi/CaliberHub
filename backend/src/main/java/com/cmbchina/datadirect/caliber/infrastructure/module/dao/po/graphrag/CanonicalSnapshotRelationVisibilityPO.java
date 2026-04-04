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
@Table(name = "caliber_canonical_snapshot_relation_visibility",
        uniqueConstraints = @UniqueConstraint(name = "uk_canonical_snapshot_relation_visibility", columnNames = {"snapshot_id", "canonical_relation_id"}),
        indexes = @Index(name = "idx_canonical_snapshot_relation_visibility_scene", columnList = "snapshot_id,scene_id,relation_type,updated_at"))
public class CanonicalSnapshotRelationVisibilityPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "canonical_relation_id", nullable = false)
    private Long canonicalRelationId;

    @Column(name = "source_canonical_entity_id", nullable = false)
    private Long sourceCanonicalEntityId;

    @Column(name = "target_canonical_entity_id", nullable = false)
    private Long targetCanonicalEntityId;

    @Column(name = "relation_type", nullable = false, length = 64)
    private String relationType;

    @Column(name = "source_relation_id")
    private Long sourceRelationId;

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
    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public Long getCanonicalRelationId() { return canonicalRelationId; }
    public void setCanonicalRelationId(Long canonicalRelationId) { this.canonicalRelationId = canonicalRelationId; }
    public Long getSourceCanonicalEntityId() { return sourceCanonicalEntityId; }
    public void setSourceCanonicalEntityId(Long sourceCanonicalEntityId) { this.sourceCanonicalEntityId = sourceCanonicalEntityId; }
    public Long getTargetCanonicalEntityId() { return targetCanonicalEntityId; }
    public void setTargetCanonicalEntityId(Long targetCanonicalEntityId) { this.targetCanonicalEntityId = targetCanonicalEntityId; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public Long getSourceRelationId() { return sourceRelationId; }
    public void setSourceRelationId(Long sourceRelationId) { this.sourceRelationId = sourceRelationId; }
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
