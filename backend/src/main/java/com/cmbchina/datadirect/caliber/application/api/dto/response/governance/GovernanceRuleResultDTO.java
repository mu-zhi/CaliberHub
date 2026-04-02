package com.cmbchina.datadirect.caliber.application.api.dto.response.governance;

public record GovernanceRuleResultDTO(
        String ruleCode,
        String stage,
        String name,
        String status,
        String blockingLevel,
        String message,
        boolean passed
) {
}
