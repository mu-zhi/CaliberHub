package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po;

import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "caliber_scene",
        indexes = {
                @Index(name = "idx_scene_domain_status", columnList = "domain,status"),
                @Index(name = "idx_scene_domain_id_status", columnList = "domain_id,status"),
                @Index(name = "idx_scene_updated_at", columnList = "updated_at")
        })
public class ScenePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_code", nullable = false, unique = true, length = 64)
    private String sceneCode;

    @Column(name = "scene_title", nullable = false, length = 200)
    private String sceneTitle;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "domain", length = 100)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SceneStatus status;

    @Lob
    @Column(name = "scene_description")
    private String sceneDescription;

    @Lob
    @Column(name = "caliber_definition")
    private String caliberDefinition;

    @Lob
    @Column(name = "applicability")
    private String applicability;

    @Lob
    @Column(name = "boundaries")
    private String boundaries;

    @Lob
    @Column(name = "inputs_json")
    private String inputsJson;

    @Lob
    @Column(name = "outputs_json")
    private String outputsJson;

    @Lob
    @Column(name = "sql_variants_json")
    private String sqlVariantsJson;

    @Lob
    @Column(name = "code_mappings_json")
    private String codeMappingsJson;

    @Column(name = "contributors", length = 500)
    private String contributors;

    @Lob
    @Column(name = "sql_blocks_json")
    private String sqlBlocksJson;

    @Lob
    @Column(name = "source_tables_json")
    private String sourceTablesJson;

    @Lob
    @Column(name = "caveats_json")
    private String caveatsJson;

    @Lob
    @Column(name = "unmapped_text")
    private String unmappedText;

    @Lob
    @Column(name = "quality_json")
    private String qualityJson;

    @Lob
    @Column(name = "raw_input")
    private String rawInput;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Lob
    @Column(name = "change_summary")
    private String changeSummary;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "published_by", length = 64)
    private String publishedBy;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSceneCode() {
        return sceneCode;
    }

    public void setSceneCode(String sceneCode) {
        this.sceneCode = sceneCode;
    }

    public String getSceneTitle() {
        return sceneTitle;
    }

    public void setSceneTitle(String sceneTitle) {
        this.sceneTitle = sceneTitle;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public SceneStatus getStatus() {
        return status;
    }

    public void setStatus(SceneStatus status) {
        this.status = status;
    }

    public String getSceneDescription() {
        return sceneDescription;
    }

    public void setSceneDescription(String sceneDescription) {
        this.sceneDescription = sceneDescription;
    }

    public String getCaliberDefinition() {
        return caliberDefinition;
    }

    public void setCaliberDefinition(String caliberDefinition) {
        this.caliberDefinition = caliberDefinition;
    }

    public String getApplicability() {
        return applicability;
    }

    public void setApplicability(String applicability) {
        this.applicability = applicability;
    }

    public String getBoundaries() {
        return boundaries;
    }

    public void setBoundaries(String boundaries) {
        this.boundaries = boundaries;
    }

    public String getInputsJson() {
        return inputsJson;
    }

    public void setInputsJson(String inputsJson) {
        this.inputsJson = inputsJson;
    }

    public String getOutputsJson() {
        return outputsJson;
    }

    public void setOutputsJson(String outputsJson) {
        this.outputsJson = outputsJson;
    }

    public String getSqlVariantsJson() {
        return sqlVariantsJson;
    }

    public void setSqlVariantsJson(String sqlVariantsJson) {
        this.sqlVariantsJson = sqlVariantsJson;
    }

    public String getCodeMappingsJson() {
        return codeMappingsJson;
    }

    public void setCodeMappingsJson(String codeMappingsJson) {
        this.codeMappingsJson = codeMappingsJson;
    }

    public String getContributors() {
        return contributors;
    }

    public void setContributors(String contributors) {
        this.contributors = contributors;
    }

    public String getSqlBlocksJson() {
        return sqlBlocksJson;
    }

    public void setSqlBlocksJson(String sqlBlocksJson) {
        this.sqlBlocksJson = sqlBlocksJson;
    }

    public String getSourceTablesJson() {
        return sourceTablesJson;
    }

    public void setSourceTablesJson(String sourceTablesJson) {
        this.sourceTablesJson = sourceTablesJson;
    }

    public String getCaveatsJson() {
        return caveatsJson;
    }

    public void setCaveatsJson(String caveatsJson) {
        this.caveatsJson = caveatsJson;
    }

    public String getUnmappedText() {
        return unmappedText;
    }

    public void setUnmappedText(String unmappedText) {
        this.unmappedText = unmappedText;
    }

    public String getQualityJson() {
        return qualityJson;
    }

    public void setQualityJson(String qualityJson) {
        this.qualityJson = qualityJson;
    }

    public String getRawInput() {
        return rawInput;
    }

    public void setRawInput(String rawInput) {
        this.rawInput = rawInput;
    }

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(OffsetDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public String getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(String publishedBy) {
        this.publishedBy = publishedBy;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
