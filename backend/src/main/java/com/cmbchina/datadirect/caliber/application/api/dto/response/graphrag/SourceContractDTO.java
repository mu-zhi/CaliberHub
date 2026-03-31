package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SourceContractDTO(
        Long id,
        Long sceneId,
        Long planId,
        Long intakeContractId,
        String sourceContractCode,
        String sourceName,
        String physicalTable,
        String sourceRole,
        String identifierType,
        String outputIdentifierType,
        String sourceSystem,
        String timeSemantic,
        String completenessLevel,
        String sensitivityLevel,
        LocalDate startDate,
        LocalDate endDate,
        String materialSourceNote,
        String notes,
        Long snapshotId,
        String versionTag,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long rowVersion
) {
}
