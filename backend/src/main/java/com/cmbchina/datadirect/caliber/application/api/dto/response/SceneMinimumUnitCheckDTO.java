package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record SceneMinimumUnitCheckDTO(
        Long sceneId,
        String unitType,
        String schemaVersion,
        Boolean publishReady,
        List<SceneMinimumUnitCheckItemDTO> items
) {
}
