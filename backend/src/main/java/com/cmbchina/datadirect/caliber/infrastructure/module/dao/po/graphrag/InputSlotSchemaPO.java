package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "caliber_input_slot_schema",
        indexes = {
                @Index(name = "idx_input_slot_scene_status", columnList = "scene_id,status,updated_at")
        })
public class InputSlotSchemaPO extends AbstractSnapshotGraphAuditablePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "slot_code", nullable = false, unique = true, length = 64)
    private String slotCode;

    @Column(name = "slot_name", nullable = false, length = 128)
    private String slotName;

    @Column(name = "slot_type", nullable = false, length = 64)
    private String slotType;

    @Column(name = "required_flag", nullable = false)
    private boolean requiredFlag;

    @Column(name = "identifier_candidates_json", columnDefinition = "LONGTEXT")
    private String identifierCandidatesJson;

    @Column(name = "normalization_rule", columnDefinition = "LONGTEXT")
    private String normalizationRule;

    @Column(name = "clarification_hint", columnDefinition = "LONGTEXT")
    private String clarificationHint;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public String getSlotCode() { return slotCode; }
    public void setSlotCode(String slotCode) { this.slotCode = slotCode; }
    public String getSlotName() { return slotName; }
    public void setSlotName(String slotName) { this.slotName = slotName; }
    public String getSlotType() { return slotType; }
    public void setSlotType(String slotType) { this.slotType = slotType; }
    public boolean isRequiredFlag() { return requiredFlag; }
    public void setRequiredFlag(boolean requiredFlag) { this.requiredFlag = requiredFlag; }
    public String getIdentifierCandidatesJson() { return identifierCandidatesJson; }
    public void setIdentifierCandidatesJson(String identifierCandidatesJson) { this.identifierCandidatesJson = identifierCandidatesJson; }
    public String getNormalizationRule() { return normalizationRule; }
    public void setNormalizationRule(String normalizationRule) { this.normalizationRule = normalizationRule; }
    public String getClarificationHint() { return clarificationHint; }
    public void setClarificationHint(String clarificationHint) { this.clarificationHint = clarificationHint; }
}
