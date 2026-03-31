package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.util.List;

public record PublishCheckDTO(
        Long sceneId,
        Boolean publishReady,
        List<PublishCheckItemDTO> items
) {
}
