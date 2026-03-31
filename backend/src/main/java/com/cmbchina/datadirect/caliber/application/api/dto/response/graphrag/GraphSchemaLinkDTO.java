package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

public record GraphSchemaLinkDTO(
        Long planId,
        String tableName,
        String columnName,
        String linkRole,
        Double score
) {
}
