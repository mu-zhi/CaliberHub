package com.cmbchina.datadirect.caliber.application.api.dto.response;

public record NlFeedbackResultDTO(
        Long planAuditId,
        Boolean success,
        Double sceneWeight
) {
}

