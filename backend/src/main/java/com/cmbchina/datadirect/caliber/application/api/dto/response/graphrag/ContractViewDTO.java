package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.time.OffsetDateTime;

public record ContractViewDTO(
        Long id,
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
        Long snapshotId,
        String versionTag,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long rowVersion
) {
}
