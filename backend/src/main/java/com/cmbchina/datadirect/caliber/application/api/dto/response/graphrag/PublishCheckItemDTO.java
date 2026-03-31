package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

public record PublishCheckItemDTO(
        String key,
        String name,
        Boolean passed,
        String level,
        String message
) {
}
