package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record ImportCandidateGraphDTO(
        String taskId,
        String materialId,
        String graphId,
        ImportCandidateGraphSummaryDTO summary,
        List<ImportCandidateGraphNodeDTO> nodes,
        List<ImportCandidateGraphEdgeDTO> edges) {
}
