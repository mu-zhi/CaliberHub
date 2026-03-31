package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.util.List;

public record GraphPlanCandidateDTO(
        Long sceneId,
        String sceneCode,
        String sceneTitle,
        Long planId,
        String planCode,
        String planName,
        String gateStatus,
        String decision,
        List<String> sourceTables,
        List<String> evidenceTitles,
        GraphScoreBreakdownDTO breakdown
) {
}
