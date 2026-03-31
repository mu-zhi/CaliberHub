package com.cmbchina.datadirect.caliber.application.api.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;

public record ImportTaskDTO(
        String taskId,
        String materialId,
        String status,
        Integer currentStep,
        String sourceType,
        String sourceName,
        String operator,
        String rawText,
        Boolean qualityConfirmed,
        Boolean compareConfirmed,
        JsonNode preprocessResult,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime completedAt
) {
}
