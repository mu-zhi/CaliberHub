package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.time.OffsetDateTime;

public record ServiceSpecDTO(
        Long id,
        Long sceneId,
        String specCode,
        Integer specVersion,
        String specJson,
        String exportedBy,
        OffsetDateTime exportedAt
) {
}

