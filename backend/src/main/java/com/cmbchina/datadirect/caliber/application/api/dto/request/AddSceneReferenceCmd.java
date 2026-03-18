package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddSceneReferenceCmd(
        @NotBlank String refType,
        @NotNull Long refId,
        @NotBlank String strategy,
        String operator
) {
}

