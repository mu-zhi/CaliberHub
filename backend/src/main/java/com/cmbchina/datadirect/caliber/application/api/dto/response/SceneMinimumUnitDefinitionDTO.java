package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record SceneMinimumUnitDefinitionDTO(
        String unitType,
        String schemaVersion,
        List<String> requiredFields,
        String description
) {
}
