package com.cmbchina.datadirect.caliber.domain.model;

public record SceneDraftUpdate(
        String sceneTitle,
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
        String rawInput
) {
}
