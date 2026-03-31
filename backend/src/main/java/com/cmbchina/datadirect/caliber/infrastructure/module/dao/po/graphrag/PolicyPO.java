package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "caliber_policy",
        indexes = {
                @Index(name = "idx_policy_scope_status", columnList = "scope_type,scope_ref_id,status,updated_at")
        })
public class PolicyPO extends AbstractSnapshotGraphAuditablePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_code", nullable = false, unique = true, length = 64)
    private String policyCode;

    @Column(name = "policy_name", nullable = false, length = 200)
    private String policyName;

    @Column(name = "policy_semantic_key", length = 255)
    private String policySemanticKey;

    @Column(name = "scope_type", nullable = false, length = 20)
    private String scopeType;

    @Column(name = "scope_ref_id")
    private Long scopeRefId;

    @Column(name = "effect_type", nullable = false, length = 32)
    private String effectType;

    @Column(name = "condition_text", columnDefinition = "LONGTEXT")
    private String conditionText;

    @Column(name = "source_type", length = 32)
    private String sourceType;

    @Column(name = "sensitivity_level", length = 8)
    private String sensitivityLevel;

    @Column(name = "masking_rule", columnDefinition = "LONGTEXT")
    private String maskingRule;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public void setPolicyCode(String policyCode) {
        this.policyCode = policyCode;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getPolicySemanticKey() {
        return policySemanticKey;
    }

    public void setPolicySemanticKey(String policySemanticKey) {
        this.policySemanticKey = policySemanticKey;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public Long getScopeRefId() {
        return scopeRefId;
    }

    public void setScopeRefId(Long scopeRefId) {
        this.scopeRefId = scopeRefId;
    }

    public String getEffectType() {
        return effectType;
    }

    public void setEffectType(String effectType) {
        this.effectType = effectType;
    }

    public String getConditionText() {
        return conditionText;
    }

    public void setConditionText(String conditionText) {
        this.conditionText = conditionText;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSensitivityLevel() {
        return sensitivityLevel;
    }

    public void setSensitivityLevel(String sensitivityLevel) {
        this.sensitivityLevel = sensitivityLevel;
    }

    public String getMaskingRule() {
        return maskingRule;
    }

    public void setMaskingRule(String maskingRule) {
        this.maskingRule = maskingRule;
    }
}
