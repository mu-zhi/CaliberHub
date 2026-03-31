package com.cmbchina.datadirect.caliber.application.api.dto.response.datamap;

import java.util.Map;

public record DataMapNodeDetailDTO(
        String assetRef,
        DataMapGraphNodeDTO node,
        Map<String, Object> attributes
) {
}
