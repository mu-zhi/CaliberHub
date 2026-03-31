package com.cmbchina.datadirect.caliber.domain.model;

import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class Scene {

    private Long id;
    private String sceneCode;
    private String sceneTitle;
    private Long domainId;
    private String domain;
    private String sceneType;
    private SceneStatus status;
    private String sceneDescription;
    private String caliberDefinition;
    private String applicability;
    private String boundaries;
    private String inputsJson;
    private String outputsJson;
    private String sqlVariantsJson;
    private String codeMappingsJson;
    private String contributors;
    private String sqlBlocksJson;
    private String sourceTablesJson;
    private String caveatsJson;
    private String unmappedText;
    private String qualityJson;
    private String rawInput;
    private OffsetDateTime verifiedAt;
    private String changeSummary;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String publishedBy;
    private OffsetDateTime publishedAt;
    private Long rowVersion;

    private Scene() {
    }

    public static Scene createDraft(String sceneTitle, String domain, String rawInput, String operator) {
        return createDraft(sceneTitle, null, domain, rawInput, operator);
    }

    public static Scene createDraft(String sceneTitle, Long domainId, String domain, String rawInput, String operator) {
        if (sceneTitle == null || sceneTitle.isBlank()) {
            throw new DomainValidationException("sceneTitle must not be blank");
        }
        if (operator == null || operator.isBlank()) {
            throw new DomainValidationException("operator must not be blank");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Scene scene = new Scene();
        scene.sceneCode = "SCN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        scene.sceneTitle = sceneTitle;
        scene.domainId = domainId;
        scene.domain = domain;
        scene.sceneType = "FACT_DETAIL";
        scene.status = SceneStatus.DRAFT;
        scene.rawInput = rawInput;
        scene.createdBy = operator;
        scene.createdAt = now;
        scene.updatedAt = now;
        return scene;
    }

    public void updateDraft(SceneDraftUpdate update, String operator) {
        if (this.status == SceneStatus.PUBLISHED) {
            throw new DomainValidationException("published scene is read-only in MVP");
        }
        if (update.sceneTitle() == null || update.sceneTitle().isBlank()) {
            throw new DomainValidationException("sceneTitle must not be blank");
        }

        String variantsJson = firstNotBlank(update.sqlVariantsJson(), update.sqlBlocksJson());

        this.sceneTitle = update.sceneTitle();
        this.domainId = update.domainId();
        this.domain = update.domain();
        this.sceneType = firstNotBlank(update.sceneType(), firstNotBlank(this.sceneType, "FACT_DETAIL"));
        this.sceneDescription = update.sceneDescription();
        this.caliberDefinition = update.caliberDefinition();
        this.applicability = update.applicability();
        this.boundaries = update.boundaries();
        this.inputsJson = update.inputsJson();
        this.outputsJson = update.outputsJson();
        this.sqlVariantsJson = variantsJson;
        this.codeMappingsJson = update.codeMappingsJson();
        this.contributors = update.contributors();
        this.sqlBlocksJson = firstNotBlank(update.sqlBlocksJson(), variantsJson);
        this.sourceTablesJson = update.sourceTablesJson();
        this.caveatsJson = update.caveatsJson();
        this.unmappedText = update.unmappedText();
        this.qualityJson = update.qualityJson();
        this.rawInput = update.rawInput();
        touch(operator);
    }

    public void publish(OffsetDateTime verifiedAt, String changeSummary, String operator) {
        if (this.status == SceneStatus.PUBLISHED) {
            throw new DomainValidationException("scene is already published");
        }
        if (this.status == SceneStatus.DISCARDED) {
            throw new DomainValidationException("discarded scene cannot be published");
        }
        if (verifiedAt == null) {
            throw new DomainValidationException("verifiedAt is required");
        }
        if (changeSummary == null || changeSummary.isBlank()) {
            throw new DomainValidationException("changeSummary is required");
        }
        if (operator == null || operator.isBlank()) {
            throw new DomainValidationException("operator must not be blank");
        }

        this.status = SceneStatus.PUBLISHED;
        this.verifiedAt = verifiedAt;
        this.changeSummary = changeSummary;
        this.publishedBy = operator;
        this.publishedAt = OffsetDateTime.now();
        touch(operator);
    }

    public void discard(String operator) {
        if (this.status == SceneStatus.PUBLISHED) {
            throw new DomainValidationException("published scene cannot be discarded");
        }
        if (this.status == SceneStatus.DISCARDED) {
            return;
        }
        this.status = SceneStatus.DISCARDED;
        touch(operator);
    }

    private void touch(String operator) {
        Objects.requireNonNull(operator, "operator must not be null");
        if (operator.isBlank()) {
            throw new DomainValidationException("operator must not be blank");
        }
        this.updatedAt = OffsetDateTime.now();
    }

    private static String firstNotBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Scene instance = new Scene();

        public Builder id(Long id) {
            instance.id = id;
            return this;
        }

        public Builder sceneCode(String sceneCode) {
            instance.sceneCode = sceneCode;
            return this;
        }

        public Builder sceneTitle(String sceneTitle) {
            instance.sceneTitle = sceneTitle;
            return this;
        }

        public Builder domainId(Long domainId) {
            instance.domainId = domainId;
            return this;
        }

        public Builder domain(String domain) {
            instance.domain = domain;
            return this;
        }

        public Builder sceneType(String sceneType) {
            instance.sceneType = sceneType;
            return this;
        }

        public Builder status(SceneStatus status) {
            instance.status = status;
            return this;
        }

        public Builder sceneDescription(String sceneDescription) {
            instance.sceneDescription = sceneDescription;
            return this;
        }

        public Builder caliberDefinition(String caliberDefinition) {
            instance.caliberDefinition = caliberDefinition;
            return this;
        }

        public Builder applicability(String applicability) {
            instance.applicability = applicability;
            return this;
        }

        public Builder boundaries(String boundaries) {
            instance.boundaries = boundaries;
            return this;
        }

        public Builder inputsJson(String inputsJson) {
            instance.inputsJson = inputsJson;
            return this;
        }

        public Builder outputsJson(String outputsJson) {
            instance.outputsJson = outputsJson;
            return this;
        }

        public Builder sqlVariantsJson(String sqlVariantsJson) {
            instance.sqlVariantsJson = sqlVariantsJson;
            return this;
        }

        public Builder codeMappingsJson(String codeMappingsJson) {
            instance.codeMappingsJson = codeMappingsJson;
            return this;
        }

        public Builder contributors(String contributors) {
            instance.contributors = contributors;
            return this;
        }

        public Builder sqlBlocksJson(String sqlBlocksJson) {
            instance.sqlBlocksJson = sqlBlocksJson;
            return this;
        }

        public Builder sourceTablesJson(String sourceTablesJson) {
            instance.sourceTablesJson = sourceTablesJson;
            return this;
        }

        public Builder caveatsJson(String caveatsJson) {
            instance.caveatsJson = caveatsJson;
            return this;
        }

        public Builder unmappedText(String unmappedText) {
            instance.unmappedText = unmappedText;
            return this;
        }

        public Builder qualityJson(String qualityJson) {
            instance.qualityJson = qualityJson;
            return this;
        }

        public Builder rawInput(String rawInput) {
            instance.rawInput = rawInput;
            return this;
        }

        public Builder verifiedAt(OffsetDateTime verifiedAt) {
            instance.verifiedAt = verifiedAt;
            return this;
        }

        public Builder changeSummary(String changeSummary) {
            instance.changeSummary = changeSummary;
            return this;
        }

        public Builder createdBy(String createdBy) {
            instance.createdBy = createdBy;
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            instance.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(OffsetDateTime updatedAt) {
            instance.updatedAt = updatedAt;
            return this;
        }

        public Builder publishedBy(String publishedBy) {
            instance.publishedBy = publishedBy;
            return this;
        }

        public Builder publishedAt(OffsetDateTime publishedAt) {
            instance.publishedAt = publishedAt;
            return this;
        }

        public Builder rowVersion(Long rowVersion) {
            instance.rowVersion = rowVersion;
            return this;
        }

        public Scene build() {
            return instance;
        }
    }

    public Long getId() {
        return id;
    }

    public String getSceneCode() {
        return sceneCode;
    }

    public String getSceneTitle() {
        return sceneTitle;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getDomain() {
        return domain;
    }

    public String getSceneType() {
        return sceneType;
    }

    public SceneStatus getStatus() {
        return status;
    }

    public String getSceneDescription() {
        return sceneDescription;
    }

    public String getCaliberDefinition() {
        return caliberDefinition;
    }

    public String getApplicability() {
        return applicability;
    }

    public String getBoundaries() {
        return boundaries;
    }

    public String getInputsJson() {
        return inputsJson;
    }

    public String getOutputsJson() {
        return outputsJson;
    }

    public String getSqlVariantsJson() {
        return sqlVariantsJson;
    }

    public String getCodeMappingsJson() {
        return codeMappingsJson;
    }

    public String getContributors() {
        return contributors;
    }

    public String getSqlBlocksJson() {
        return sqlBlocksJson;
    }

    public String getSourceTablesJson() {
        return sourceTablesJson;
    }

    public String getCaveatsJson() {
        return caveatsJson;
    }

    public String getUnmappedText() {
        return unmappedText;
    }

    public String getQualityJson() {
        return qualityJson;
    }

    public String getRawInput() {
        return rawInput;
    }

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public Long getRowVersion() {
        return rowVersion;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }
}
