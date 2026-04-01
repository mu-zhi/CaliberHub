package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "caliber_identifier_lineage",
        indexes = {
                @Index(name = "idx_identifier_lineage_scene_status", columnList = "scene_id,status,updated_at")
        })
public class IdentifierLineagePO extends AbstractSnapshotGraphAuditablePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "lineage_code", nullable = false, unique = true, length = 64)
    private String lineageCode;

    @Column(name = "lineage_name", nullable = false, length = 200)
    private String lineageName;

    @Column(name = "identifier_type", length = 64)
    private String identifierType;

    @Column(name = "source_identifier_type", length = 64)
    private String sourceIdentifierType;

    @Column(name = "target_identifier_type", length = 64)
    private String targetIdentifierType;

    @Column(name = "mapping_rules_json", columnDefinition = "LONGTEXT")
    private String mappingRulesJson;

    @Column(name = "evidence_refs_json", columnDefinition = "LONGTEXT")
    private String evidenceRefsJson;

    @Column(name = "confirmation_status", length = 32)
    private String confirmationStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getLineageCode() { return lineageCode; }
    public void setLineageCode(String lineageCode) { this.lineageCode = lineageCode; }
    public String getLineageName() { return lineageName; }
    public void setLineageName(String lineageName) { this.lineageName = lineageName; }
    public String getIdentifierType() { return identifierType; }
    public void setIdentifierType(String identifierType) { this.identifierType = identifierType; }
    public String getSourceIdentifierType() { return sourceIdentifierType; }
    public void setSourceIdentifierType(String sourceIdentifierType) { this.sourceIdentifierType = sourceIdentifierType; }
    public String getTargetIdentifierType() { return targetIdentifierType; }
    public void setTargetIdentifierType(String targetIdentifierType) { this.targetIdentifierType = targetIdentifierType; }
    public String getMappingRulesJson() { return mappingRulesJson; }
    public void setMappingRulesJson(String mappingRulesJson) { this.mappingRulesJson = mappingRulesJson; }
    public String getEvidenceRefsJson() { return evidenceRefsJson; }
    public void setEvidenceRefsJson(String evidenceRefsJson) { this.evidenceRefsJson = evidenceRefsJson; }
    public String getConfirmationStatus() { return confirmationStatus; }
    public void setConfirmationStatus(String confirmationStatus) { this.confirmationStatus = confirmationStatus; }
}

