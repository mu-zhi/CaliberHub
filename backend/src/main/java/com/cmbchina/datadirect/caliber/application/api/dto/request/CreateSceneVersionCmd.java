package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateSceneVersionCmd(
        String changeSummary,
        @NotBlank String operator
) {
}

