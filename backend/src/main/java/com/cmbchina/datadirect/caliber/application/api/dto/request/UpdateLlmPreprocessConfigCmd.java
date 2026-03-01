package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateLlmPreprocessConfigCmd(
        @NotNull Boolean enabled,
        String endpoint,
        String model,
        @NotNull Integer timeoutSeconds,
        @NotNull Double temperature,
        @NotNull Integer maxTokens,
        Boolean enableThinking,
        @NotNull Boolean fallbackToRule,
        String apiKey,
        Boolean clearApiKey,
        String operator
) {
}
