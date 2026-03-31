package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record ConfirmCandidateCmd(
        @NotNull(message = "domainId 不能为空") Long domainId,
        String operator
) {
}
