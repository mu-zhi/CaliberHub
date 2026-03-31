package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "caliber_plan_schema_link",
        indexes = {
                @Index(name = "idx_schema_link_plan", columnList = "plan_id,status,updated_at")
        })
public class PlanSchemaLinkPO extends AbstractGraphAuditablePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "table_name", nullable = false, length = 255)
    private String tableName;

    @Column(name = "column_name", length = 255)
    private String columnName;

    @Column(name = "link_role", nullable = false, length = 32)
    private String linkRole;

    @Column(name = "evidence_id")
    private Long evidenceId;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getLinkRole() {
        return linkRole;
    }

    public void setLinkRole(String linkRole) {
        this.linkRole = linkRole;
    }

    public Long getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(Long evidenceId) {
        this.evidenceId = evidenceId;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}
