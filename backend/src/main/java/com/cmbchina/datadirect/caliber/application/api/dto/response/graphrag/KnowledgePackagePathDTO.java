package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.util.List;

public record KnowledgePackagePathDTO(
        List<String> resolutionSteps,
        List<String> sourceContractCodes
) {
}
