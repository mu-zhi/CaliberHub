package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record SceneDiffDTO(
        Long sceneId,
        Integer fromVersion,
        Integer toVersion,
        List<String> changedFields,
        List<String> dictionaryChanges,
        List<String> identifierLineageChanges,
        List<String> timeSemanticSelectorChanges
) {

    public SceneDiffDTO(Long sceneId, Integer fromVersion, Integer toVersion, List<String> changedFields) {
        this(
                sceneId,
                fromVersion,
                toVersion,
                changedFields == null ? List.of() : List.copyOf(changedFields),
                selectAssetChanges(changedFields, "dictionaries"),
                selectAssetChanges(changedFields, "identifierLineages"),
                selectAssetChanges(changedFields, "timeSemanticSelectors")
        );
    }

    private static List<String> selectAssetChanges(List<String> changedFields, String blockKey) {
        if (changedFields == null || changedFields.isEmpty()) {
            return List.of();
        }
        return changedFields.stream()
                .filter(field -> field != null && (field.equals(blockKey) || field.startsWith(blockKey + ".")))
                .toList();
    }
}
