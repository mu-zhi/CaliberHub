package com.cmbchina.datadirect.caliber.infrastructure.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "caliber.llm.preprocess")
public class LlmPreprocessProperties {

    private boolean enabled = true;
    private String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private String apiKey = "";
    private String model = "qwen3-max";
    private int timeoutSeconds = 35;
    private double temperature = 0.0;
    private int maxTokens = 4096;
    private boolean enableThinking = false;
    private boolean fallbackToRule = true;
    private String secretKey = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public boolean isEnableThinking() {
        return enableThinking;
    }

    public void setEnableThinking(boolean enableThinking) {
        this.enableThinking = enableThinking;
    }

    public boolean isFallbackToRule() {
        return fallbackToRule;
    }

    public void setFallbackToRule(boolean fallbackToRule) {
        this.fallbackToRule = fallbackToRule;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @PostConstruct
    public void validate() {
        boolean requiresSecret = apiKey != null && !apiKey.isBlank();
        if (requiresSecret && (secretKey == null || secretKey.isBlank())) {
            throw new IllegalStateException("caliber.llm.preprocess.secret-key must be provided when caliber.llm.preprocess.api-key is configured");
        }
    }
}
