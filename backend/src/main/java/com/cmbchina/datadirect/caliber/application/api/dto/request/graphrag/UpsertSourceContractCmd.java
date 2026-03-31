package com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag;

public record UpsertSourceContractCmd(
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
        String startDate,
        String endDate,
        String materialSourceNote,
        String notes,
        Long expectedVersion,
        String operator
) {
}
