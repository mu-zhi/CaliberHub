package com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag;

public record UpsertOutputContractCmd(
        Long sceneId,
        String contractCode,
        String contractName,
        String summaryText,
        String fieldsJson,
        String maskingRulesJson,
        String usageConstraints,
        String timeCaliberNote,
        Long expectedVersion,
        String operator
) {
}
