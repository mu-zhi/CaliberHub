package com.cmbchina.datadirect.caliber.application.api.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

public record ImportCandidateGraphEdgeDTO(
        String edgeCode,
        String sceneCandidateCode,
        String edgeType,
        String sourceNodeCode,
        String targetNodeCode,
        String label,
        String reviewStatus,
        String riskLevel,
        Double confidenceScore,
        JsonNode payload) {
}
