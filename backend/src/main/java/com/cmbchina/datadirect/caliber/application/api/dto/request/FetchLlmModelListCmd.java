package com.cmbchina.datadirect.caliber.application.api.dto.request;

public record FetchLlmModelListCmd(
        String endpoint,
        String apiKey,
        Integer timeoutSeconds
) {
}

