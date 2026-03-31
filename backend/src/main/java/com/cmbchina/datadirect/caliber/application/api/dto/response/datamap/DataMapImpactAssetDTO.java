package com.cmbchina.datadirect.caliber.application.api.dto.response.datamap;

public record DataMapImpactAssetDTO(
        String assetRef,
        String objectType,
        String objectName,
        String relationType,
        String impactReason
) {
}
