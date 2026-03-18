package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record NlQueryCmd(
        @NotBlank String queryText,
        String operator
) {
}

