package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

public record GraphScoreBreakdownDTO(
        Double entityScore,
        Double schemaScore,
        Double pathScore,
        Double evidenceScore,
        Double vectorScore,
        Double finalScore
) {
}
