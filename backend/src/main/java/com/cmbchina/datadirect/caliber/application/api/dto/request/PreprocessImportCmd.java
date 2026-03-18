package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PreprocessImportCmd(
        @NotBlank String rawText,
        @NotBlank String sourceType,
        String sourceName,
        String preprocessMode,
        Boolean autoCreateDrafts,
        String operator
) {
}
