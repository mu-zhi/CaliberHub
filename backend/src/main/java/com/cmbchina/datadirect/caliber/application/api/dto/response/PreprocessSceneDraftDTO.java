package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record PreprocessSceneDraftDTO(Integer sceneIndex,
                                      String sceneTitle,
                                      Long sceneId,
                                      String status,
                                      Double confidenceScore,
                                      Boolean lowConfidence,
                                      List<String> warnings) {
}
