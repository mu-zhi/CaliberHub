package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record NlFeedbackCmd(
        @NotNull Long planAuditId,
        @NotNull Boolean success,
        String reason,
        String selectedPlan
) {
}
