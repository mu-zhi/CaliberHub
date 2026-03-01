package com.cmbchina.datadirect.caliber.application.api.dto.request;

public record ExportServiceSpecCmd(
        Integer expectedVersion,
        String operator
) {
}

