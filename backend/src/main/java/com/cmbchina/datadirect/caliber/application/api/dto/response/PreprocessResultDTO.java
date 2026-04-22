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
                                  String materialId,
                                  JsonNode preprocessExperiment,
                                  List<String> referenceRefs,
                                  List<String> formalAssetWrites) {

    public PreprocessResultDTO {
        referenceRefs = referenceRefs == null ? List.of() : List.copyOf(referenceRefs);
        formalAssetWrites = formalAssetWrites == null ? List.of() : List.copyOf(formalAssetWrites);
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
                               CandidateGraphDTO candidateGraph,
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
                candidateGraph,
                stageTimings,
                sceneDrafts,
                importBatchId,
                materialId,
                null,
                List.of(),
                List.of());
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
                materialId,
                null,
                List.of(),
                List.of());
    }
}
