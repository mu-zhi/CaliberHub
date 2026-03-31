package com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag;

public record UpsertSourceIntakeContractCmd(
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
        Long expectedVersion,
        String operator
) {
}
