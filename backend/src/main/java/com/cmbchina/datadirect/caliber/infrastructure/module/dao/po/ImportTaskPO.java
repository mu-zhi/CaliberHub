package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "caliber_import_task",
        indexes = {
                @Index(name = "idx_import_task_status_updated", columnList = "status,updated_at"),
                @Index(name = "idx_import_task_updated_at", columnList = "updated_at")
        })
public class ImportTaskPO {

    @Id
    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "current_step", nullable = false)
    private Integer currentStep;

    @Column(name = "source_type", length = 32)
    private String sourceType;

    @Column(name = "source_name", length = 255)
    private String sourceName;

    @Column(name = "operator", length = 64)
    private String operator;

    @Lob
    @Column(name = "raw_text")
    private String rawText;

    @Lob
    @Column(name = "preprocess_result_json")
    private String preprocessResultJson;

    @Column(name = "quality_confirmed", nullable = false)
    private Boolean qualityConfirmed;

    @Column(name = "compare_confirmed", nullable = false)
    private Boolean compareConfirmed;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = currentStep;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getPreprocessResultJson() {
        return preprocessResultJson;
    }

    public void setPreprocessResultJson(String preprocessResultJson) {
        this.preprocessResultJson = preprocessResultJson;
    }

    public Boolean getQualityConfirmed() {
        return qualityConfirmed;
    }

    public void setQualityConfirmed(Boolean qualityConfirmed) {
        this.qualityConfirmed = qualityConfirmed;
    }

    public Boolean getCompareConfirmed() {
        return compareConfirmed;
    }

    public void setCompareConfirmed(Boolean compareConfirmed) {
        this.compareConfirmed = compareConfirmed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
