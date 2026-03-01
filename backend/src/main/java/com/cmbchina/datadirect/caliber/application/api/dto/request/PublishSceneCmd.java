package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record PublishSceneCmd(
        @NotNull OffsetDateTime verifiedAt,
        @NotBlank String changeSummary,
        String operator
) {
}
