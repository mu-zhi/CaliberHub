package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.time.OffsetDateTime;

public record SourceIntakeContractDTO(
        Long id,
        Long sceneId,
        String intakeCode,
        String intakeName,
        String sourceType,
        String requiredFieldsJson,
        String completenessRule,
        String gapTaskHint,
        String sourceTableHintsJson,
        String knownCoverageJson,
        String sensitivityLevel,
        String defaultTimeSemantic,
        String materialSourceNote,
        Long snapshotId,
        String versionTag,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long rowVersion
) {
}
