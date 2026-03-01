package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.time.OffsetDateTime;

public record SemanticViewDTO(
        Long id,
        String viewCode,
        String viewName,
        Long domainId,
        String description,
        String fieldDefinitionsJson,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

