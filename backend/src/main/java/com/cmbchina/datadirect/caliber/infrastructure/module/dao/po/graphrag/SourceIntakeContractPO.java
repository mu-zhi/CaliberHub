package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "caliber_source_intake_contract",
        indexes = {
                @Index(name = "idx_source_intake_scene_status", columnList = "scene_id,status,updated_at")
        })
public class SourceIntakeContractPO extends AbstractSnapshotGraphAuditablePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "intake_code", nullable = false, unique = true, length = 64)
    private String intakeCode;

    @Column(name = "intake_name", nullable = false, length = 200)
    private String intakeName;

    @Column(name = "source_type", nullable = false, length = 64)
    private String sourceType;

    @Column(name = "required_fields_json", columnDefinition = "LONGTEXT")
    private String requiredFieldsJson;

    @Column(name = "completeness_rule", columnDefinition = "LONGTEXT")
    private String completenessRule;

    @Column(name = "gap_task_hint", columnDefinition = "LONGTEXT")
    private String gapTaskHint;

    @Column(name = "source_table_hints_json", columnDefinition = "LONGTEXT")
    private String sourceTableHintsJson;

    @Column(name = "known_coverage_json", columnDefinition = "LONGTEXT")
    private String knownCoverageJson;

    @Column(name = "sensitivity_level", length = 8)
    private String sensitivityLevel;

    @Column(name = "default_time_semantic", length = 128)
    private String defaultTimeSemantic;

    @Column(name = "material_source_note", columnDefinition = "LONGTEXT")
    private String materialSourceNote;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public String getIntakeCode() { return intakeCode; }
    public void setIntakeCode(String intakeCode) { this.intakeCode = intakeCode; }
    public String getIntakeName() { return intakeName; }
    public void setIntakeName(String intakeName) { this.intakeName = intakeName; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getRequiredFieldsJson() { return requiredFieldsJson; }
    public void setRequiredFieldsJson(String requiredFieldsJson) { this.requiredFieldsJson = requiredFieldsJson; }
    public String getCompletenessRule() { return completenessRule; }
    public void setCompletenessRule(String completenessRule) { this.completenessRule = completenessRule; }
    public String getGapTaskHint() { return gapTaskHint; }
    public void setGapTaskHint(String gapTaskHint) { this.gapTaskHint = gapTaskHint; }
    public String getSourceTableHintsJson() { return sourceTableHintsJson; }
    public void setSourceTableHintsJson(String sourceTableHintsJson) { this.sourceTableHintsJson = sourceTableHintsJson; }
    public String getKnownCoverageJson() { return knownCoverageJson; }
    public void setKnownCoverageJson(String knownCoverageJson) { this.knownCoverageJson = knownCoverageJson; }
    public String getSensitivityLevel() { return sensitivityLevel; }
    public void setSensitivityLevel(String sensitivityLevel) { this.sensitivityLevel = sensitivityLevel; }
    public String getDefaultTimeSemantic() { return defaultTimeSemantic; }
    public void setDefaultTimeSemantic(String defaultTimeSemantic) { this.defaultTimeSemantic = defaultTimeSemantic; }
    public String getMaterialSourceNote() { return materialSourceNote; }
    public void setMaterialSourceNote(String materialSourceNote) { this.materialSourceNote = materialSourceNote; }
}
