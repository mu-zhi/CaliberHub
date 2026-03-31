package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record CandidateGraphDTO(List<ImportGraphNodeDTO> nodes,
                                List<ImportGraphEdgeDTO> edges) {

    public static CandidateGraphDTO empty() {
        return new CandidateGraphDTO(List.of(), List.of());
    }
}
