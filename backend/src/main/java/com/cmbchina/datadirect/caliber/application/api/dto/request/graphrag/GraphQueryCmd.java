package com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag;

public record GraphQueryCmd(
        String queryText,
        String mode,
        Long domainId,
        Long sceneId,
        String slotHintsJson,
        String operator
) {
}
