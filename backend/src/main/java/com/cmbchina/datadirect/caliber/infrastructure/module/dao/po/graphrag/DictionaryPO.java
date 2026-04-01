package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "caliber_dictionary",
        indexes = {
                @Index(name = "idx_dictionary_scene_status", columnList = "scene_id,status,updated_at")
        })
public class DictionaryPO extends AbstractSnapshotGraphAuditablePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "dict_code", nullable = false, unique = true, length = 64)
    private String dictCode;

    @Column(name = "dict_name", nullable = false, length = 200)
    private String dictName;

    @Column(name = "dict_category", length = 64)
    private String dictCategory;

    @Column(name = "dict_version", length = 64)
    private String dictVersion;

    @Column(name = "release_status", length = 32)
    private String releaseStatus;

    @Column(name = "entries_json", columnDefinition = "LONGTEXT")
    private String entriesJson;

    @Column(name = "referenced_by_json", columnDefinition = "LONGTEXT")
    private String referencedByJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getDictCode() { return dictCode; }
    public void setDictCode(String dictCode) { this.dictCode = dictCode; }
    public String getDictName() { return dictName; }
    public void setDictName(String dictName) { this.dictName = dictName; }
    public String getDictCategory() { return dictCategory; }
    public void setDictCategory(String dictCategory) { this.dictCategory = dictCategory; }
    public String getDictVersion() { return dictVersion; }
    public void setDictVersion(String dictVersion) { this.dictVersion = dictVersion; }
    public String getReleaseStatus() { return releaseStatus; }
    public void setReleaseStatus(String releaseStatus) { this.releaseStatus = releaseStatus; }
    public String getEntriesJson() { return entriesJson; }
    public void setEntriesJson(String entriesJson) { this.entriesJson = entriesJson; }
    public String getReferencedByJson() { return referencedByJson; }
    public void setReferencedByJson(String referencedByJson) { this.referencedByJson = referencedByJson; }
}

