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
                                  CandidateGraphDTO candidateGraph,
                                  List<StageTimingDTO> stageTimings,
                                  List<PreprocessSceneDraftDTO> sceneDrafts,
                                  String importBatchId,
                                  String materialId) {

    public PreprocessResultDTO {
    }

    public PreprocessResultDTO(String caliberImportJson,
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
                               String importBatchId,
                               String materialId) {
        this(caliberImportJson,
                mode,
                global,
                scenes,
                quality,
                warnings,
                confidenceScore,
                confidenceLevel,
                lowConfidence,
                totalElapsedMs,
                CandidateGraphDTO.empty(),
                stageTimings,
                sceneDrafts,
                importBatchId,
                materialId);
    }
}
