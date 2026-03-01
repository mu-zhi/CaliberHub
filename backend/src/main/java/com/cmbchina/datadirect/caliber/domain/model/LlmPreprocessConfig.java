package com.cmbchina.datadirect.caliber.domain.model;

import java.time.OffsetDateTime;

public class LlmPreprocessConfig {

    private Long id;
    private Boolean enabled;
    private String endpoint;
    private String apiKeyCiphertext;
    private String model;
    private Integer timeoutSeconds;
    private Double temperature;
    private Integer maxTokens;
    private Boolean enableThinking;
    private Boolean fallbackToRule;
    private String preprocessSystemPrompt;
    private String preprocessUserPromptTemplate;
    private String prepSchemaJson;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    private LlmPreprocessConfig() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final LlmPreprocessConfig instance = new LlmPreprocessConfig();

        public Builder id(Long id) {
            instance.id = id;
            return this;
        }

        public Builder enabled(Boolean enabled) {
            instance.enabled = enabled;
            return this;
        }

        public Builder endpoint(String endpoint) {
            instance.endpoint = endpoint;
            return this;
        }

        public Builder apiKeyCiphertext(String apiKeyCiphertext) {
            instance.apiKeyCiphertext = apiKeyCiphertext;
            return this;
        }

        public Builder model(String model) {
            instance.model = model;
            return this;
        }

        public Builder timeoutSeconds(Integer timeoutSeconds) {
            instance.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder temperature(Double temperature) {
            instance.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            instance.maxTokens = maxTokens;
            return this;
        }

        public Builder enableThinking(Boolean enableThinking) {
            instance.enableThinking = enableThinking;
            return this;
        }

        public Builder fallbackToRule(Boolean fallbackToRule) {
            instance.fallbackToRule = fallbackToRule;
            return this;
        }

        public Builder preprocessSystemPrompt(String preprocessSystemPrompt) {
            instance.preprocessSystemPrompt = preprocessSystemPrompt;
            return this;
        }

        public Builder preprocessUserPromptTemplate(String preprocessUserPromptTemplate) {
            instance.preprocessUserPromptTemplate = preprocessUserPromptTemplate;
            return this;
        }

        public Builder prepSchemaJson(String prepSchemaJson) {
            instance.prepSchemaJson = prepSchemaJson;
            return this;
        }

        public Builder updatedBy(String updatedBy) {
            instance.updatedBy = updatedBy;
            return this;
        }

        public Builder updatedAt(OffsetDateTime updatedAt) {
            instance.updatedAt = updatedAt;
            return this;
        }

        public LlmPreprocessConfig build() {
            return instance;
        }
    }

    public Long getId() {
        return id;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getApiKeyCiphertext() {
        return apiKeyCiphertext;
    }

    public String getModel() {
        return model;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public Boolean getEnableThinking() {
        return enableThinking;
    }

    public Boolean getFallbackToRule() {
        return fallbackToRule;
    }

    public String getPreprocessSystemPrompt() {
        return preprocessSystemPrompt;
    }

    public String getPreprocessUserPromptTemplate() {
        return preprocessUserPromptTemplate;
    }

    public String getPrepSchemaJson() {
        return prepSchemaJson;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
