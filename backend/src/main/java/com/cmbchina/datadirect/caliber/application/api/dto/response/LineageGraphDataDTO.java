package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record LineageGraphDataDTO(
        List<LineageNodeDTO> nodes,
        List<LineageEdgeDTO> edges,
        boolean truncated,
        int hiddenNodeCount
) {
}
