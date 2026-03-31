package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

public record KnowledgePackagePlanDTO(
        Long planId,
        String planCode,
        String planName,
        String resolvedIdentifierType,
        String resolvedIdentifierValue
) {
}
