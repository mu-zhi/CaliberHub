package com.cmbchina.datadirect.caliber.application.api.dto.response;

public record LineageEdgeDTO(
        String source,
        String target,
        String label
) {
}
