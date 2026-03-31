package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.util.List;

public record KnowledgePackageCoverageDTO(
        String status,
        String matchedSegment,
        List<String> matchedSourceContracts,
        String coverageExplanation
) {
}
