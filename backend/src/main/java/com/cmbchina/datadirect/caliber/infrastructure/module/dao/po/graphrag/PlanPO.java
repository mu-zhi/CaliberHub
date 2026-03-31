package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "caliber_plan",
        indexes = {
                @Index(name = "idx_plan_scene_status", columnList = "scene_id,status,updated_at")
        })
public class PlanPO extends AbstractSnapshotGraphAuditablePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "plan_code", nullable = false, unique = true, length = 64)
    private String planCode;

    @Column(name = "plan_name", nullable = false, length = 200)
    private String planName;

    @Column(name = "applicable_period", length = 255)
    private String applicablePeriod;

    @Column(name = "default_time_semantic", columnDefinition = "LONGTEXT")
    private String defaultTimeSemantic;

    @Column(name = "source_tables_json", columnDefinition = "LONGTEXT")
    private String sourceTablesJson;

    @Column(name = "notes", columnDefinition = "LONGTEXT")
    private String notes;

    @Column(name = "retrieval_text", columnDefinition = "LONGTEXT")
    private String retrievalText;

    @Column(name = "sql_text", columnDefinition = "LONGTEXT")
    private String sqlText;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSceneId() {
        return sceneId;
    }

    public void setSceneId(Long sceneId) {
        this.sceneId = sceneId;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getApplicablePeriod() {
        return applicablePeriod;
    }

    public void setApplicablePeriod(String applicablePeriod) {
        this.applicablePeriod = applicablePeriod;
    }

    public String getDefaultTimeSemantic() {
        return defaultTimeSemantic;
    }

    public void setDefaultTimeSemantic(String defaultTimeSemantic) {
        this.defaultTimeSemantic = defaultTimeSemantic;
    }

    public String getSourceTablesJson() {
        return sourceTablesJson;
    }

    public void setSourceTablesJson(String sourceTablesJson) {
        this.sourceTablesJson = sourceTablesJson;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRetrievalText() {
        return retrievalText;
    }

    public void setRetrievalText(String retrievalText) {
        this.retrievalText = retrievalText;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}
