package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.util.List;

public record GraphQueryResultDTO(
        String mode,
        String decision,
        String riskLevel,
        List<String> reasons,
        List<String> slotResolutions,
        List<String> outputContracts,
        List<GraphEntityLinkDTO> entityLinks,
        List<GraphSchemaLinkDTO> schemaLinks,
        List<GraphPlanCandidateDTO> candidates,
        GraphPathGraphDTO pathGraph
) {
}
