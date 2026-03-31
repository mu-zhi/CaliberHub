package com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag;

import java.util.List;

public record UpsertEvidenceFragmentCmd(
        Long sceneId,
        String evidenceCode,
        String title,
        String fragmentText,
        String sourceAnchor,
        String sourceType,
        String sourceRef,
        Double confidenceScore,
        Long expectedVersion,
        String operator,
        List<Long> planIds
) {
}
