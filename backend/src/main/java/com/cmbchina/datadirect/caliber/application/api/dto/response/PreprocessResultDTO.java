package com.cmbchina.datadirect.caliber.application.api.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record PreprocessResultDTO(String caliberImportJson,
                                  String mode,
                                  JsonNode global,
                                  List<JsonNode> scenes,
                                  JsonNode quality,
                                  List<String> warnings,
                                  Double confidenceScore,
                                  String confidenceLevel,
                                  Boolean lowConfidence,
                                  Long totalElapsedMs,
                                  List<StageTimingDTO> stageTimings,
                                  List<PreprocessSceneDraftDTO> sceneDrafts,
                                  String importBatchId) {
}
