package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateLlmPromptConfigCmd(
        @NotBlank String preprocessSystemPrompt,
        @NotBlank String preprocessUserPromptTemplate,
        @NotBlank String prepSchemaJson,
        String operator
) {
}

