package com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag;

public record UpsertInputSlotSchemaCmd(
        Long sceneId,
        String slotCode,
        String slotName,
        String slotType,
        Boolean requiredFlag,
        String identifierCandidatesJson,
        String normalizationRule,
        String clarificationHint,
        Long expectedVersion,
        String operator
) {
}
