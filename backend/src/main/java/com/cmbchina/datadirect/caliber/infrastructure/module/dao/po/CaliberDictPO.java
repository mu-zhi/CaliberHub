package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "caliber_dict",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dict_scope_code_value", columnNames = {"domain_scope", "code", "value_code"})
        },
        indexes = {
                @Index(name = "idx_dict_scope_code", columnList = "domain_scope,code"),
                @Index(name = "idx_dict_updated_at", columnList = "updated_at")
        })
public class CaliberDictPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "domain_scope", nullable = false, length = 64)
    private String domainScope;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "code", nullable = false, length = 128)
    private String code;

    @Column(name = "value_code", nullable = false, length = 128)
    private String valueCode;

    @Column(name = "value_name", length = 500)
    private String valueName;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "last_scene_id")
    private Long lastSceneId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDomainScope() {
        return domainScope;
    }

    public void setDomainScope(String domainScope) {
        this.domainScope = domainScope;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getValueCode() {
        return valueCode;
    }

    public void setValueCode(String valueCode) {
        this.valueCode = valueCode;
    }

    public String getValueName() {
        return valueName;
    }

    public void setValueName(String valueName) {
        this.valueName = valueName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getLastSceneId() {
        return lastSceneId;
    }

    public void setLastSceneId(Long lastSceneId) {
        this.lastSceneId = lastSceneId;
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
