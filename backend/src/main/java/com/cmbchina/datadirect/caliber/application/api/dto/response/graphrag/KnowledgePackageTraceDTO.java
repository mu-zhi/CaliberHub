package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

public record KnowledgePackageTraceDTO(
        String traceId,
        Long snapshotId,
        Long inferenceSnapshotId,
        String versionTag,
        String retrievalAdapter,
        String retrievalStatus,
        boolean fallbackToFormal
) {
}
