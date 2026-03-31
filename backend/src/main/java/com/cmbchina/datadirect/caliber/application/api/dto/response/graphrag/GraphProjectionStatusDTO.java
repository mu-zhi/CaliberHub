package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.time.OffsetDateTime;

public record GraphProjectionStatusDTO(
        Long sceneId,
        Long snapshotId,
        String sceneCode,
        String status,
        String stage,
        String message,
        String payloadJson,
        OffsetDateTime lastProjectedAt,
        OffsetDateTime updatedAt
) {
}
