package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "caliber_evidence_fragment",
        indexes = {
                @Index(name = "idx_evidence_scene_status", columnList = "scene_id,status,updated_at")
        })
public class EvidenceFragmentPO extends AbstractSnapshotGraphAuditablePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "evidence_code", nullable = false, unique = true, length = 64)
    private String evidenceCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "fragment_text", nullable = false, columnDefinition = "LONGTEXT")
    private String fragmentText;

    @Column(name = "source_anchor", length = 500)
    private String sourceAnchor;

    @Column(name = "source_type", length = 64)
    private String sourceType;

    @Column(name = "source_ref", length = 500)
    private String sourceRef;

    @Column(name = "origin_type", length = 64)
    private String originType;

    @Column(name = "origin_ref", length = 500)
    private String originRef;

    @Column(name = "origin_locator", length = 500)
    private String originLocator;

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

    public String getEvidenceCode() {
        return evidenceCode;
    }

    public void setEvidenceCode(String evidenceCode) {
        this.evidenceCode = evidenceCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFragmentText() {
        return fragmentText;
    }

    public void setFragmentText(String fragmentText) {
        this.fragmentText = fragmentText;
    }

    public String getSourceAnchor() {
        return sourceAnchor;
    }

    public void setSourceAnchor(String sourceAnchor) {
        this.sourceAnchor = sourceAnchor;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getOriginType() {
        return originType;
    }

    public void setOriginType(String originType) {
        this.originType = originType;
    }

    public String getOriginRef() {
        return originRef;
    }

    public void setOriginRef(String originRef) {
        this.originRef = originRef;
    }

    public String getOriginLocator() {
        return originLocator;
    }

    public void setOriginLocator(String originLocator) {
        this.originLocator = originLocator;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}
