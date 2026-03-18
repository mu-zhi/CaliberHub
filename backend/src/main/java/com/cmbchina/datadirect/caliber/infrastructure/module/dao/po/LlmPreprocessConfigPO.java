package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "caliber_llm_preprocess_config")
public class LlmPreprocessConfigPO {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "endpoint", length = 500)
    private String endpoint;

    @Lob
    @Column(name = "api_key_ciphertext")
    private String apiKeyCiphertext;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "enable_thinking", nullable = false)
    private Boolean enableThinking;

    @Column(name = "fallback_to_rule", nullable = false)
    private Boolean fallbackToRule;

    @Lob
    @Column(name = "preprocess_system_prompt")
    private String preprocessSystemPrompt;

    @Lob
    @Column(name = "preprocess_user_prompt_template")
    private String preprocessUserPromptTemplate;

    @Lob
    @Column(name = "prep_schema_json")
    private String prepSchemaJson;

    @Column(name = "prompt_version")
    private Long promptVersion;

    @Column(name = "prompt_hash", length = 64)
    private String promptHash;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKeyCiphertext() {
        return apiKeyCiphertext;
    }

    public void setApiKeyCiphertext(String apiKeyCiphertext) {
        this.apiKeyCiphertext = apiKeyCiphertext;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Boolean getEnableThinking() {
        return enableThinking;
    }

    public void setEnableThinking(Boolean enableThinking) {
        this.enableThinking = enableThinking;
    }

    public Boolean getFallbackToRule() {
        return fallbackToRule;
    }

    public void setFallbackToRule(Boolean fallbackToRule) {
        this.fallbackToRule = fallbackToRule;
    }

    public String getPreprocessSystemPrompt() {
        return preprocessSystemPrompt;
    }

    public void setPreprocessSystemPrompt(String preprocessSystemPrompt) {
        this.preprocessSystemPrompt = preprocessSystemPrompt;
    }

    public String getPreprocessUserPromptTemplate() {
        return preprocessUserPromptTemplate;
    }

    public void setPreprocessUserPromptTemplate(String preprocessUserPromptTemplate) {
        this.preprocessUserPromptTemplate = preprocessUserPromptTemplate;
    }

    public String getPrepSchemaJson() {
        return prepSchemaJson;
    }

    public void setPrepSchemaJson(String prepSchemaJson) {
        this.prepSchemaJson = prepSchemaJson;
    }

    public Long getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(Long promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getPromptHash() {
        return promptHash;
    }

    public void setPromptHash(String promptHash) {
        this.promptHash = promptHash;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
