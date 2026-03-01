package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSemanticViewCmd(
        @NotBlank @Size(max = 64) String viewCode,
        @NotBlank @Size(max = 200) String viewName,
        Long domainId,
        String description,
        String fieldDefinitionsJson,
        String operator
) {
}

