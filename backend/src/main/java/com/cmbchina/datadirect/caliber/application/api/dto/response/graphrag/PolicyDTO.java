package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.time.OffsetDateTime;
import java.util.List;

public record PolicyDTO(
        Long id,
        String policyCode,
        String policyName,
        String scopeType,
        Long scopeRefId,
        String effectType,
        String conditionText,
        String sourceType,
        String sensitivityLevel,
        String maskingRule,
        String status,
        List<Long> planIds,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long rowVersion
) {
}
