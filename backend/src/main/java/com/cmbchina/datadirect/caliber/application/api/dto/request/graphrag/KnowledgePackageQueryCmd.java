package com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag;

import java.util.List;

public record KnowledgePackageQueryCmd(
        String queryText,
        Long snapshotId,
        Long selectedSceneId,
        Long selectedPlanId,
        String slotHintsJson,
        String identifierType,
        String identifierValue,
        String dateFrom,
        String dateTo,
        List<String> requestedFields,
        String purpose,
        String operator
) {
}
