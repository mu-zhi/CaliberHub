package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TestLlmPreprocessConfigCmd(
        @NotBlank String rawText,
        String sourceType
) {
}
