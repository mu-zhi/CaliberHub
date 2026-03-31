package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.time.OffsetDateTime;

public record InputSlotSchemaDTO(
        Long id,
        Long sceneId,
        String slotCode,
        String slotName,
        String slotType,
        Boolean requiredFlag,
        String identifierCandidatesJson,
        String normalizationRule,
        String clarificationHint,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long rowVersion
) {
}
