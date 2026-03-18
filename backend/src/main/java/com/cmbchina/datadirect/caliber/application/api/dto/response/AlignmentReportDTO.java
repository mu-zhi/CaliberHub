package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.time.OffsetDateTime;

public record AlignmentReportDTO(
        Long id,
        Long sceneId,
        String status,
        String message,
        String reportJson,
        String checkedBy,
        OffsetDateTime checkedAt
) {
}

