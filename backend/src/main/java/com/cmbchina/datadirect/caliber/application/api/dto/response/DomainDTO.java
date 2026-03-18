package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.time.OffsetDateTime;

public record DomainDTO(
        Long id,
        String domainCode,
        String domainName,
        String domainOverview,
        String commonTables,
        String contacts,
        Integer sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
