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
@Table(name = "caliber_import_candidate_graph_edge",
        indexes = {
                @Index(name = "idx_candidate_graph_edge_task_graph", columnList = "task_id,graph_id,review_status,updated_at")
        })
public class ImportCandidateGraphEdgePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "edge_id", nullable = false)
    private Long edgeId;

    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;

    @Column(name = "material_id", nullable = false, length = 64)
    private String materialId;

    @Column(name = "graph_id", nullable = false, length = 128)
    private String graphId;

    @Column(name = "edge_code", nullable = false, unique = true, length = 96)
    private String edgeCode;

    @Column(name = "scene_candidate_code", length = 64)
    private String sceneCandidateCode;

    @Column(name = "edge_type", nullable = false, length = 48)
    private String edgeType;

    @Column(name = "source_node_code", nullable = false, length = 96)
    private String sourceNodeCode;

    @Column(name = "target_node_code", nullable = false, length = 96)
    private String targetNodeCode;

    @Column(name = "edge_label", length = 255)
    private String edgeLabel;

    @Column(name = "review_status", nullable = false, length = 32)
    private String reviewStatus;

    @Column(name = "risk_level", nullable = false, length = 16)
    private String riskLevel;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getEdgeId() {
        return edgeId;
    }

    public void setEdgeId(Long edgeId) {
        this.edgeId = edgeId;
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

    public String getGraphId() {
        return graphId;
    }

    public void setGraphId(String graphId) {
        this.graphId = graphId;
    }

    public String getEdgeCode() {
        return edgeCode;
    }

    public void setEdgeCode(String edgeCode) {
        this.edgeCode = edgeCode;
    }

    public String getSceneCandidateCode() {
        return sceneCandidateCode;
    }

    public void setSceneCandidateCode(String sceneCandidateCode) {
        this.sceneCandidateCode = sceneCandidateCode;
    }

    public String getEdgeType() {
        return edgeType;
    }

    public void setEdgeType(String edgeType) {
        this.edgeType = edgeType;
    }

    public String getSourceNodeCode() {
        return sourceNodeCode;
    }

    public void setSourceNodeCode(String sourceNodeCode) {
        this.sourceNodeCode = sourceNodeCode;
    }

    public String getTargetNodeCode() {
        return targetNodeCode;
    }

    public void setTargetNodeCode(String targetNodeCode) {
        this.targetNodeCode = targetNodeCode;
    }

    public String getEdgeLabel() {
        return edgeLabel;
    }

    public void setEdgeLabel(String edgeLabel) {
        this.edgeLabel = edgeLabel;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
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
