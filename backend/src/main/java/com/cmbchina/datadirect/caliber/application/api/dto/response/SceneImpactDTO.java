package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record SceneImpactDTO(
        Long sceneId,
        long referenceCount,
        long serviceSpecCount,
        List<String> affectedSpecCodes
) {
}
