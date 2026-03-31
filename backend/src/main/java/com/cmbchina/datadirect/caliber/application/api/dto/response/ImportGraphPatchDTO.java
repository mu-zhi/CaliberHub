package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record ImportGraphPatchDTO(Integer patchSeq,
                                  String stageKey,
                                  String stageName,
                                  List<ImportGraphNodeDTO> addedNodes,
                                  List<ImportGraphNodeDTO> updatedNodes,
                                  List<ImportGraphEdgeDTO> addedEdges,
                                  List<ImportGraphEdgeDTO> updatedEdges,
                                  List<String> focusNodeIds,
                                  Integer totalNodeCount,
                                  Integer totalEdgeCount,
                                  String message) {
}
