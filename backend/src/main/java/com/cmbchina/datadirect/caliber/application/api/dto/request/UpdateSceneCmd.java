package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateSceneCmd(
        @NotBlank String sceneTitle,
        Long domainId,
        String domain,
        String sceneType,
        String sceneDescription,
        String caliberDefinition,
        String applicability,
        String boundaries,
        String inputsJson,
        String outputsJson,
        String sqlVariantsJson,
        String codeMappingsJson,
        String contributors,
        String sqlBlocksJson,
        String sourceTablesJson,
        String caveatsJson,
        String unmappedText,
        String qualityJson,
        String rawInput,
        Long expectedVersion,
        String operator
) {
}
