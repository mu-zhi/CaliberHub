package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PreviewLlmPromptCmd(
        @NotBlank String rawText,
        String sourceType,
        String preprocessSystemPrompt,
        String preprocessUserPromptTemplate,
        String prepSchemaJson
) {
}
