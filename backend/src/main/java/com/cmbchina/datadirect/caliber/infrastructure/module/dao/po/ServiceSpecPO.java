package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "caliber_service_spec",
        indexes = {
                @Index(name = "uk_service_spec_version", columnList = "spec_code,spec_version", unique = true),
                @Index(name = "idx_service_spec_scene", columnList = "scene_id,exported_at")
        })
public class ServiceSpecPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "spec_code", nullable = false, length = 64)
    private String specCode;

    @Column(name = "spec_version", nullable = false)
    private Integer specVersion;

    @Lob
    @Column(name = "spec_json", nullable = false)
    private String specJson;

    @Column(name = "exported_by", nullable = false, length = 64)
    private String exportedBy;

    @Column(name = "exported_at", nullable = false)
    private OffsetDateTime exportedAt;

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

    public String getSpecCode() {
        return specCode;
    }

    public void setSpecCode(String specCode) {
        this.specCode = specCode;
    }

    public Integer getSpecVersion() {
        return specVersion;
    }

    public void setSpecVersion(Integer specVersion) {
        this.specVersion = specVersion;
    }

    public String getSpecJson() {
        return specJson;
    }

    public void setSpecJson(String specJson) {
        this.specJson = specJson;
    }

    public String getExportedBy() {
        return exportedBy;
    }

    public void setExportedBy(String exportedBy) {
        this.exportedBy = exportedBy;
    }

    public OffsetDateTime getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(OffsetDateTime exportedAt) {
        this.exportedAt = exportedAt;
    }
}

