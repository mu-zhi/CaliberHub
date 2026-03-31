package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "caliber_import_scene_candidate",
        indexes = {
                @Index(name = "idx_import_scene_candidate_task_material", columnList = "task_id,material_id,updated_at")
        })
public class ImportSceneCandidatePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;

    @Column(name = "material_id", nullable = false, length = 64)
    private String materialId;

    @Column(name = "candidate_code", nullable = false, unique = true, length = 64)
    private String candidateCode;

    @Column(name = "scene_index", nullable = false)
    private Integer sceneIndex;

    @Column(name = "scene_title", nullable = false, length = 255)
    private String sceneTitle;

    @Column(name = "scene_description", columnDefinition = "LONGTEXT")
    private String sceneDescription;

    @Column(name = "candidate_payload_json", columnDefinition = "LONGTEXT")
    private String candidatePayloadJson;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "confirmation_status", nullable = false, length = 32)
    private String confirmationStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getCandidateCode() {
        return candidateCode;
    }

    public void setCandidateCode(String candidateCode) {
        this.candidateCode = candidateCode;
    }

    public Integer getSceneIndex() {
        return sceneIndex;
    }

    public void setSceneIndex(Integer sceneIndex) {
        this.sceneIndex = sceneIndex;
    }

    public String getSceneTitle() {
        return sceneTitle;
    }

    public void setSceneTitle(String sceneTitle) {
        this.sceneTitle = sceneTitle;
    }

    public String getSceneDescription() {
        return sceneDescription;
    }

    public void setSceneDescription(String sceneDescription) {
        this.sceneDescription = sceneDescription;
    }

    public String getCandidatePayloadJson() {
        return candidatePayloadJson;
    }

    public void setCandidatePayloadJson(String candidatePayloadJson) {
        this.candidatePayloadJson = candidatePayloadJson;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getConfirmationStatus() {
        return confirmationStatus;
    }

    public void setConfirmationStatus(String confirmationStatus) {
        this.confirmationStatus = confirmationStatus;
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
}
