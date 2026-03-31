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
@Table(name = "caliber_coverage_declaration",
        indexes = {
                @Index(name = "idx_coverage_plan_status", columnList = "plan_id,status,updated_at")
        })
public class CoverageDeclarationPO extends AbstractSnapshotGraphAuditablePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "coverage_code", nullable = false, unique = true, length = 64)
    private String coverageCode;

    @Column(name = "coverage_title", nullable = false, length = 200)
    private String coverageTitle;

    @Column(name = "coverage_type", nullable = false, length = 32)
    private String coverageType;

    @Column(name = "coverage_status", length = 16)
    private String coverageStatus;

    @Column(name = "time_semantic", length = 128)
    private String timeSemantic;

    @Column(name = "source_system", length = 128)
    private String sourceSystem;

    @Column(name = "statement_text", nullable = false, columnDefinition = "LONGTEXT")
    private String statementText;

    @Column(name = "applicable_period", length = 255)
    private String applicablePeriod;

    @Column(name = "source_tables_json", columnDefinition = "LONGTEXT")
    private String sourceTablesJson;

    @Column(name = "gap_text", columnDefinition = "LONGTEXT")
    private String gapText;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

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

    public String getCoverageCode() {
        return coverageCode;
    }

    public void setCoverageCode(String coverageCode) {
        this.coverageCode = coverageCode;
    }

    public String getCoverageTitle() {
        return coverageTitle;
    }

    public void setCoverageTitle(String coverageTitle) {
        this.coverageTitle = coverageTitle;
    }

    public String getCoverageType() {
        return coverageType;
    }

    public void setCoverageType(String coverageType) {
        this.coverageType = coverageType;
    }

    public String getCoverageStatus() {
        return coverageStatus;
    }

    public void setCoverageStatus(String coverageStatus) {
        this.coverageStatus = coverageStatus;
    }

    public String getTimeSemantic() {
        return timeSemantic;
    }

    public void setTimeSemantic(String timeSemantic) {
        this.timeSemantic = timeSemantic;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getStatementText() {
        return statementText;
    }

    public void setStatementText(String statementText) {
        this.statementText = statementText;
    }

    public String getApplicablePeriod() {
        return applicablePeriod;
    }

    public void setApplicablePeriod(String applicablePeriod) {
        this.applicablePeriod = applicablePeriod;
    }

    public String getSourceTablesJson() {
        return sourceTablesJson;
    }

    public void setSourceTablesJson(String sourceTablesJson) {
        this.sourceTablesJson = sourceTablesJson;
    }

    public String getGapText() {
        return gapText;
    }

    public void setGapText(String gapText) {
        this.gapText = gapText;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
