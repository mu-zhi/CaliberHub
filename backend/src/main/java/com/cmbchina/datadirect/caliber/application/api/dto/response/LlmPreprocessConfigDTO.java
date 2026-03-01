package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.time.OffsetDateTime;

public record LlmPreprocessConfigDTO(
        Boolean enabled,
        String endpoint,
        String model,
        Integer timeoutSeconds,
        Double temperature,
        Integer maxTokens,
        Boolean enableThinking,
        Boolean fallbackToRule,
        Boolean hasApiKey,
        String maskedApiKey,
        String configSource,
        String endpointHost,
        String fallbackStrategy,
        String updatedBy,
        OffsetDateTime updatedAt
) {
}
