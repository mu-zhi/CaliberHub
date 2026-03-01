package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.time.OffsetDateTime;

public record SceneDTO(
        Long id,
        String sceneCode,
        String sceneTitle,
        Long domainId,
        String domain,
        String domainName,
        String status,
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
        OffsetDateTime verifiedAt,
        String changeSummary,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String publishedBy,
        OffsetDateTime publishedAt
) {
}
