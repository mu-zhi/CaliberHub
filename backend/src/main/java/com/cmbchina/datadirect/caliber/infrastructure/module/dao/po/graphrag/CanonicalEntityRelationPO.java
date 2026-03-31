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
@Table(name = "caliber_canonical_entity_relation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_canonical_relation",
                columnNames = {"source_canonical_entity_id", "target_canonical_entity_id", "relation_type"}
        ),
        indexes = @Index(name = "idx_canonical_relation_source_type", columnList = "source_canonical_entity_id,relation_type,updated_at"))
public class CanonicalEntityRelationPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_canonical_entity_id", nullable = false)
    private Long sourceCanonicalEntityId;

    @Column(name = "target_canonical_entity_id", nullable = false)
    private Long targetCanonicalEntityId;

    @Column(name = "relation_type", nullable = false, length = 64)
    private String relationType;

    @Column(name = "relation_label", length = 255)
    private String relationLabel;

    @Column(name = "relation_payload_json", columnDefinition = "LONGTEXT")
    private String relationPayloadJson;

    @Column(name = "visible_in_snapshot_binding", nullable = false)
    private boolean visibleInSnapshotBinding;

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
    public Long getSourceCanonicalEntityId() { return sourceCanonicalEntityId; }
    public void setSourceCanonicalEntityId(Long sourceCanonicalEntityId) { this.sourceCanonicalEntityId = sourceCanonicalEntityId; }
    public Long getTargetCanonicalEntityId() { return targetCanonicalEntityId; }
    public void setTargetCanonicalEntityId(Long targetCanonicalEntityId) { this.targetCanonicalEntityId = targetCanonicalEntityId; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public String getRelationLabel() { return relationLabel; }
    public void setRelationLabel(String relationLabel) { this.relationLabel = relationLabel; }
    public String getRelationPayloadJson() { return relationPayloadJson; }
    public void setRelationPayloadJson(String relationPayloadJson) { this.relationPayloadJson = relationPayloadJson; }
    public boolean isVisibleInSnapshotBinding() { return visibleInSnapshotBinding; }
    public void setVisibleInSnapshotBinding(boolean visibleInSnapshotBinding) { this.visibleInSnapshotBinding = visibleInSnapshotBinding; }
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
