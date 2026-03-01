package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record LlmModelListResultDTO(
        Boolean success,
        String endpoint,
        List<String> models,
        String selectedModel,
        String message
) {
}

