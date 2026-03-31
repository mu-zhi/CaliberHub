package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

public record GraphEntityLinkDTO(
        String aliasText,
        String aliasType,
        Long sceneId,
        Long planId,
        Double score
) {
}
