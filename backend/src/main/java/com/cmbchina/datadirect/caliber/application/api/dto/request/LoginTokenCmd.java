package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginTokenCmd(
        @NotBlank String username,
        @NotBlank String password
) {
}
