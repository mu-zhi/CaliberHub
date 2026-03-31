package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "caliber_output_contract",
        indexes = {
                @Index(name = "idx_output_contract_scene_status", columnList = "scene_id,status,updated_at")
        })
public class OutputContractPO extends AbstractSnapshotGraphAuditablePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "contract_code", nullable = false, unique = true, length = 64)
    private String contractCode;

    @Column(name = "contract_name", nullable = false, length = 200)
    private String contractName;

    @Column(name = "contract_semantic_key", length = 255)
    private String contractSemanticKey;

    @Column(name = "summary_text", columnDefinition = "LONGTEXT")
    private String summaryText;

    @Column(name = "fields_json", columnDefinition = "LONGTEXT")
    private String fieldsJson;

    @Column(name = "masking_rules_json", columnDefinition = "LONGTEXT")
    private String maskingRulesJson;

    @Column(name = "usage_constraints", columnDefinition = "LONGTEXT")
    private String usageConstraints;

    @Column(name = "time_caliber_note", columnDefinition = "LONGTEXT")
    private String timeCaliberNote;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public String getContractCode() { return contractCode; }
    public void setContractCode(String contractCode) { this.contractCode = contractCode; }
    public String getContractName() { return contractName; }
    public void setContractName(String contractName) { this.contractName = contractName; }
    public String getContractSemanticKey() { return contractSemanticKey; }
    public void setContractSemanticKey(String contractSemanticKey) { this.contractSemanticKey = contractSemanticKey; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public String getFieldsJson() { return fieldsJson; }
    public void setFieldsJson(String fieldsJson) { this.fieldsJson = fieldsJson; }
    public String getMaskingRulesJson() { return maskingRulesJson; }
    public void setMaskingRulesJson(String maskingRulesJson) { this.maskingRulesJson = maskingRulesJson; }
    public String getUsageConstraints() { return usageConstraints; }
    public void setUsageConstraints(String usageConstraints) { this.usageConstraints = usageConstraints; }
    public String getTimeCaliberNote() { return timeCaliberNote; }
    public void setTimeCaliberNote(String timeCaliberNote) { this.timeCaliberNote = timeCaliberNote; }
}
