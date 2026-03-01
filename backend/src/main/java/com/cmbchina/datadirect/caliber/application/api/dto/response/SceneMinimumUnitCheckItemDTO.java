package com.cmbchina.datadirect.caliber.application.api.dto.response;

public record SceneMinimumUnitCheckItemDTO(
        String key,
        String name,
        Boolean passed,
        String message
) {
}
