package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.time.OffsetDateTime;
import java.util.List;

public record PlanDTO(
        Long id,
        Long sceneId,
        String planCode,
        String planName,
        String applicablePeriod,
        String defaultTimeSemantic,
        String sourceTablesJson,
        String notes,
        String retrievalText,
        String sqlText,
        Double confidenceScore,
        String status,
        List<Long> evidenceIds,
        List<Long> policyIds,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long rowVersion
) {
}
