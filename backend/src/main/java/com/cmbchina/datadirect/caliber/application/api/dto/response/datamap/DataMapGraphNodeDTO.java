package com.cmbchina.datadirect.caliber.application.api.dto.response.datamap;

import java.time.OffsetDateTime;
import java.util.Map;

public record DataMapGraphNodeDTO(
        String id,
        String label,
        String objectType,
        String objectCode,
        String objectName,
        String status,
        Long snapshotId,
        String domainCode,
        String owner,
        String sensitivityScope,
        String timeSemantic,
        Integer evidenceCount,
        OffsetDateTime lastReviewedAt,
        String summaryText,
        Map<String, Object> meta
) {
}
