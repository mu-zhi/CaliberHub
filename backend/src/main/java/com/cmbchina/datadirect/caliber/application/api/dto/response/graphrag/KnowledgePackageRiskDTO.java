package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.util.List;

public record KnowledgePackageRiskDTO(
        String riskLevel,
        List<String> riskReasons
) {
}
