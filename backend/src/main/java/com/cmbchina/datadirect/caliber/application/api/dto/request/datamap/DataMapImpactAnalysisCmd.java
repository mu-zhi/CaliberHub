package com.cmbchina.datadirect.caliber.application.api.dto.request.datamap;

public record DataMapImpactAnalysisCmd(
        String assetRef,
        Long snapshotId
) {
}
