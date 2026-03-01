package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.time.OffsetDateTime;

public record SceneVersionDTO(
        Long id,
        Long sceneId,
        Integer versionNo,
        String snapshotJson,
        String changeSummary,
        String createdBy,
        OffsetDateTime createdAt
) {
}

