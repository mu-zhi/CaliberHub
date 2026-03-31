package com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag;

import java.util.List;

public record UpsertPlanCmd(
        Long sceneId,
        String planCode,
        String planName,
        String applicablePeriod,
        String defaultTimeSemantic,
        String sourceTablesJson,
        String notes,
        String sqlText,
        Double confidenceScore,
        Long expectedVersion,
        String operator,
        List<Long> evidenceIds,
        List<Long> policyIds
) {
}
