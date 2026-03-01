package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record TestLlmPreprocessConfigResultDTO(
        boolean success,
        String mode,
        boolean llmEnabled,
        boolean llmEffective,
        boolean fallbackUsed,
        String statusLabel,
        String statusReason,
        String promptFingerprint,
        long latencyMs,
        int sceneCount,
        int sqlCount,
        List<String> warnings,
        String message
) {
}
