package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record ImportGraphEdgeDTO(String id,
                                 String sourceId,
                                 String targetId,
                                 String relationType,
                                 String status,
                                 Double confidenceScore,
                                 List<String> evidenceRefs) {
}
