package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "caliber_time_semantic_selector",
        indexes = {
                @Index(name = "idx_time_semantic_selector_scene_status", columnList = "scene_id,status,updated_at")
        })
public class TimeSemanticSelectorPO extends AbstractSnapshotGraphAuditablePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "selector_code", nullable = false, unique = true, length = 64)
    private String selectorCode;

    @Column(name = "selector_name", nullable = false, length = 200)
    private String selectorName;

    @Column(name = "default_semantic", length = 128)
    private String defaultSemantic;

    @Column(name = "candidate_semantics_json", columnDefinition = "LONGTEXT")
    private String candidateSemanticsJson;

    @Column(name = "clarification_terms_json", columnDefinition = "LONGTEXT")
    private String clarificationTermsJson;

    @Column(name = "priority_rules_json", columnDefinition = "LONGTEXT")
    private String priorityRulesJson;

    @Column(name = "must_clarify_flag", nullable = false)
    private boolean mustClarifyFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getSelectorCode() { return selectorCode; }
    public void setSelectorCode(String selectorCode) { this.selectorCode = selectorCode; }
    public String getSelectorName() { return selectorName; }
    public void setSelectorName(String selectorName) { this.selectorName = selectorName; }
    public String getDefaultSemantic() { return defaultSemantic; }
    public void setDefaultSemantic(String defaultSemantic) { this.defaultSemantic = defaultSemantic; }
    public String getCandidateSemanticsJson() { return candidateSemanticsJson; }
    public void setCandidateSemanticsJson(String candidateSemanticsJson) { this.candidateSemanticsJson = candidateSemanticsJson; }
    public String getClarificationTermsJson() { return clarificationTermsJson; }
    public void setClarificationTermsJson(String clarificationTermsJson) { this.clarificationTermsJson = clarificationTermsJson; }
    public String getPriorityRulesJson() { return priorityRulesJson; }
    public void setPriorityRulesJson(String priorityRulesJson) { this.priorityRulesJson = priorityRulesJson; }
    public boolean isMustClarifyFlag() { return mustClarifyFlag; }
    public void setMustClarifyFlag(boolean mustClarifyFlag) { this.mustClarifyFlag = mustClarifyFlag; }
}

