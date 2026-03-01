package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record NlQueryResultDTO(
        Long planAuditId,
        String decision,
        String riskLevel,
        Long sceneId,
        String sceneCode,
        String sceneTitle,
        List<String> evidence
) {
}

