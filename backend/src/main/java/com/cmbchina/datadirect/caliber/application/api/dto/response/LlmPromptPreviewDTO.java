package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record LlmPromptPreviewDTO(
        String systemPrompt,
        String userPrompt,
        String promptFingerprint,
        String normalizedSourceType,
        int lineCount,
        List<String> warnings
) {
}
