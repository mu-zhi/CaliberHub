package com.cmbchina.datadirect.caliber.application.api.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

public record ImportCandidateGraphNodeDTO(
        String nodeCode,
        String sceneCandidateCode,
        String nodeType,
        String label,
        String reviewStatus,
        String riskLevel,
        Double confidenceScore,
        JsonNode payload,
        String summaryText) {
}
