package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateAlignmentReportCmd(
        @NotBlank String status,
        String message,
        List<String> tables,
        List<String> columns,
        List<String> policies,
        String operator
) {
}

