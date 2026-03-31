package com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag;

import java.util.List;

public record UpsertPolicyCmd(
        String policyCode,
        String policyName,
        String scopeType,
        Long scopeRefId,
        String effectType,
        String conditionText,
        String sourceType,
        String sensitivityLevel,
        String maskingRule,
        Long expectedVersion,
        String operator,
        List<Long> planIds
) {
}
