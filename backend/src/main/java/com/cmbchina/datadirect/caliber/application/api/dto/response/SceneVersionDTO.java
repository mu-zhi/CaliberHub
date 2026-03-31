package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.time.OffsetDateTime;

public record SceneVersionDTO(
        Long id,
        Long sceneId,
        Integer versionNo,
        String versionTag,
        String snapshotJson,
        String snapshotSummaryJson,
        String changeSummary,
        String publishStatus,
        String publishedBy,
        OffsetDateTime publishedAt,
        String createdBy,
        OffsetDateTime createdAt
) {
}
