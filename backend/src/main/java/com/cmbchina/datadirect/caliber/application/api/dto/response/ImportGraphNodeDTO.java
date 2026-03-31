package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record ImportGraphNodeDTO(String id,
                                 String nodeType,
                                 String label,
                                 String status,
                                 Double confidenceScore,
                                 List<String> evidenceRefs) {
}
