package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.time.OffsetDateTime;

public record SceneReferenceDTO(
        Long id,
        Long sceneId,
        String refType,
        Long refId,
        String strategy,
        String createdBy,
        OffsetDateTime createdAt
) {
}

