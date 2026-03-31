package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "caliber_source_contract",
        indexes = {
                @Index(name = "idx_source_contract_scene_status", columnList = "scene_id,status,updated_at"),
                @Index(name = "idx_source_contract_plan_status", columnList = "plan_id,status,updated_at")
        })
public class SourceContractPO extends AbstractSnapshotGraphAuditablePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "intake_contract_id")
    private Long intakeContractId;

    @Column(name = "source_contract_code", nullable = false, unique = true, length = 64)
    private String sourceContractCode;

    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;

    @Column(name = "physical_table", nullable = false, length = 255)
    private String physicalTable;

    @Column(name = "normalized_physical_table", length = 255)
    private String normalizedPhysicalTable;

    @Column(name = "source_role", nullable = false, length = 64)
    private String sourceRole;

    @Column(name = "identifier_type", length = 64)
    private String identifierType;

    @Column(name = "output_identifier_type", length = 64)
    private String outputIdentifierType;

    @Column(name = "source_system", length = 128)
    private String sourceSystem;

    @Column(name = "time_semantic", length = 128)
    private String timeSemantic;

    @Column(name = "completeness_level", length = 16)
    private String completenessLevel;

    @Column(name = "sensitivity_level", length = 8)
    private String sensitivityLevel;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "material_source_note", columnDefinition = "LONGTEXT")
    private String materialSourceNote;

    @Column(name = "notes", columnDefinition = "LONGTEXT")
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getIntakeContractId() { return intakeContractId; }
    public void setIntakeContractId(Long intakeContractId) { this.intakeContractId = intakeContractId; }
    public String getSourceContractCode() { return sourceContractCode; }
    public void setSourceContractCode(String sourceContractCode) { this.sourceContractCode = sourceContractCode; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getPhysicalTable() { return physicalTable; }
    public void setPhysicalTable(String physicalTable) { this.physicalTable = physicalTable; }
    public String getNormalizedPhysicalTable() { return normalizedPhysicalTable; }
    public void setNormalizedPhysicalTable(String normalizedPhysicalTable) { this.normalizedPhysicalTable = normalizedPhysicalTable; }
    public String getSourceRole() { return sourceRole; }
    public void setSourceRole(String sourceRole) { this.sourceRole = sourceRole; }
    public String getIdentifierType() { return identifierType; }
    public void setIdentifierType(String identifierType) { this.identifierType = identifierType; }
    public String getOutputIdentifierType() { return outputIdentifierType; }
    public void setOutputIdentifierType(String outputIdentifierType) { this.outputIdentifierType = outputIdentifierType; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    public String getTimeSemantic() { return timeSemantic; }
    public void setTimeSemantic(String timeSemantic) { this.timeSemantic = timeSemantic; }
    public String getCompletenessLevel() { return completenessLevel; }
    public void setCompletenessLevel(String completenessLevel) { this.completenessLevel = completenessLevel; }
    public String getSensitivityLevel() { return sensitivityLevel; }
    public void setSensitivityLevel(String sensitivityLevel) { this.sensitivityLevel = sensitivityLevel; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getMaterialSourceNote() { return materialSourceNote; }
    public void setMaterialSourceNote(String materialSourceNote) { this.materialSourceNote = materialSourceNote; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
