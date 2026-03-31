package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "caliber_contract_view",
        indexes = {
                @Index(name = "idx_contract_view_scene_status", columnList = "scene_id,status,updated_at")
        })
public class ContractViewPO extends AbstractSnapshotGraphAuditablePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "output_contract_id")
    private Long outputContractId;

    @Column(name = "view_code", nullable = false, unique = true, length = 64)
    private String viewCode;

    @Column(name = "view_name", nullable = false, length = 200)
    private String viewName;

    @Column(name = "role_scope", nullable = false, length = 64)
    private String roleScope;

    @Column(name = "visible_fields_json", columnDefinition = "LONGTEXT")
    private String visibleFieldsJson;

    @Column(name = "masked_fields_json", columnDefinition = "LONGTEXT")
    private String maskedFieldsJson;

    @Column(name = "restricted_fields_json", columnDefinition = "LONGTEXT")
    private String restrictedFieldsJson;

    @Column(name = "forbidden_fields_json", columnDefinition = "LONGTEXT")
    private String forbiddenFieldsJson;

    @Column(name = "approval_template", columnDefinition = "LONGTEXT")
    private String approvalTemplate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getOutputContractId() { return outputContractId; }
    public void setOutputContractId(Long outputContractId) { this.outputContractId = outputContractId; }
    public String getViewCode() { return viewCode; }
    public void setViewCode(String viewCode) { this.viewCode = viewCode; }
    public String getViewName() { return viewName; }
    public void setViewName(String viewName) { this.viewName = viewName; }
    public String getRoleScope() { return roleScope; }
    public void setRoleScope(String roleScope) { this.roleScope = roleScope; }
    public String getVisibleFieldsJson() { return visibleFieldsJson; }
    public void setVisibleFieldsJson(String visibleFieldsJson) { this.visibleFieldsJson = visibleFieldsJson; }
    public String getMaskedFieldsJson() { return maskedFieldsJson; }
    public void setMaskedFieldsJson(String maskedFieldsJson) { this.maskedFieldsJson = maskedFieldsJson; }
    public String getRestrictedFieldsJson() { return restrictedFieldsJson; }
    public void setRestrictedFieldsJson(String restrictedFieldsJson) { this.restrictedFieldsJson = restrictedFieldsJson; }
    public String getForbiddenFieldsJson() { return forbiddenFieldsJson; }
    public void setForbiddenFieldsJson(String forbiddenFieldsJson) { this.forbiddenFieldsJson = forbiddenFieldsJson; }
    public String getApprovalTemplate() { return approvalTemplate; }
    public void setApprovalTemplate(String approvalTemplate) { this.approvalTemplate = approvalTemplate; }
}
