package com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag;

public record UpsertCoverageDeclarationCmd(
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
        String startDate,
        String endDate,
        Long expectedVersion,
        String operator
) {
}
