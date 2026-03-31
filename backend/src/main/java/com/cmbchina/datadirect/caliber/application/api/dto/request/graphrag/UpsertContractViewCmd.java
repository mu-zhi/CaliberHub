package com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag;

public record UpsertContractViewCmd(
        Long sceneId,
        Long planId,
        Long outputContractId,
        String viewCode,
        String viewName,
        String roleScope,
        String visibleFieldsJson,
        String maskedFieldsJson,
        String restrictedFieldsJson,
        String forbiddenFieldsJson,
        String approvalTemplate,
        Long expectedVersion,
        String operator
) {
}
