package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.util.List;

public record GraphPathGraphDTO(
        List<GraphPathNodeDTO> nodes,
        List<GraphPathEdgeDTO> edges
) {
}
