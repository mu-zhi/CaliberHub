package com.cmbchina.datadirect.caliber.application.api.dto.response;

public record StageTimingDTO(String stageKey,
                             String stageName,
                             Long elapsedMs,
                             Integer percent,
                             Integer chunkIndex,
                             Integer chunkTotal,
                             String message) {
}
