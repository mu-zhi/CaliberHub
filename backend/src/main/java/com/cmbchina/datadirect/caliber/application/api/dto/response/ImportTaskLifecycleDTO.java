package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record ImportTaskLifecycleDTO(
        String taskId,
        String status,
        Integer currentStep,
        String sourceType,
        String sourceName,
        String operator,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime completedAt,
        List<Long> sceneIds,
        Integer draftTotal,
        Integer draftCount,
        Integer publishedCount,
        Integer discardedCount,
        Boolean resumable
) {
}
