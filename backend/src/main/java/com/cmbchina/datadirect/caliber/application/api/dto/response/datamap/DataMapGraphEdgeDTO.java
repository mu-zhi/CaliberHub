package com.cmbchina.datadirect.caliber.application.api.dto.response.datamap;

import java.util.Map;

public record DataMapGraphEdgeDTO(
        String id,
        String relationType,
        String source,
        String target,
        String label,
        Double confidence,
        String traceId,
        String sourceRef,
        String effectiveFrom,
        String effectiveTo,
        Boolean policyHit,
        String coverageExplanation,
        Map<String, Object> meta
) {
}
