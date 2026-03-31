package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CoverageDeclarationDTO(
        Long id,
        Long planId,
        String coverageCode,
        String coverageTitle,
        String coverageType,
        String coverageStatus,
        String statementText,
        String applicablePeriod,
        String timeSemantic,
        String sourceSystem,
        String sourceTablesJson,
        String gapText,
        Boolean active,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long rowVersion
) {
}
