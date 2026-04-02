package com.cmbchina.datadirect.caliber.application.api.dto.response.governance;

public record GovernanceGapDTO(
        Long id,
        String taskCode,
        String taskTitle,
        String taskType,
        String status,
        String severity,
        String detailText,
        String sourceRef
) {
}
