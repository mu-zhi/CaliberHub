package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.time.OffsetDateTime;

public record OutputContractDTO(
        Long id,
        Long sceneId,
        String contractCode,
        String contractName,
        String summaryText,
        String fieldsJson,
        String maskingRulesJson,
        String usageConstraints,
        String timeCaliberNote,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long rowVersion
) {
}
