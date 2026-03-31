package com.cmbchina.datadirect.caliber.application.api.dto.response.datamap;

import java.time.OffsetDateTime;
import java.util.List;

public record DataMapImpactAnalysisDTO(
        String assetRef,
        String riskLevel,
        ReadSource readSource,
        ProjectionVerificationStatus projectionVerificationStatus,
        OffsetDateTime projectionVerifiedAt,
        List<String> recommendedActions,
        List<DataMapImpactAssetDTO> affectedAssets,
        DataMapGraphResponseDTO graph
) {
}
