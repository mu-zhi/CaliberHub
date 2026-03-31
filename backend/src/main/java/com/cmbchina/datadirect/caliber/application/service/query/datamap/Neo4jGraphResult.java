package com.cmbchina.datadirect.caliber.application.service.query.datamap;

import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphNodeDTO;

import java.util.List;

public record Neo4jGraphResult(
        String rootRef,
        Long sceneId,
        String sceneName,
        List<DataMapGraphNodeDTO> nodes,
        List<DataMapGraphEdgeDTO> edges
) {
}
