package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record LlmPromptConfigDTO(
        String preprocessSystemPrompt,
        String preprocessUserPromptTemplate,
        String prepSchemaJson,
        Long promptVersion,
        String promptHash,
        String promptFingerprint,
        Boolean schemaValid,
        String schemaValidationMessage,
        Boolean templateHasRequiredTokens,
        List<String> templateMissingTokens,
        String updatedBy,
        OffsetDateTime updatedAt
) {
}
