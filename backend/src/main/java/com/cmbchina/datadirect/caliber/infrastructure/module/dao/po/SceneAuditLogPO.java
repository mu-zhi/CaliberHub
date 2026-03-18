package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "caliber_scene_audit_log",
        indexes = {
                @Index(name = "idx_scene_audit_scene_created", columnList = "scene_id,created_at")
        })
public class SceneAuditLogPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "operator", nullable = false, length = 64)
    private String operator;

    @Lob
    @Column(name = "detail_json")
    private String detailJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
