package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.time.OffsetDateTime;
import java.util.List;

public record EvidenceFragmentDTO(
        Long id,
        Long sceneId,
        String evidenceCode,
        String title,
        String fragmentText,
        String sourceAnchor,
        String sourceType,
        String sourceRef,
        Double confidenceScore,
        String status,
        List<Long> planIds,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long rowVersion
) {
}
