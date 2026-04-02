package com.cmbchina.datadirect.caliber.application.api.dto.response.governance;

import java.util.List;

public record SceneGovernanceSummaryDTO(
        Long sceneId,
        String stage,
        boolean publishReady,
        List<GovernanceRuleResultDTO> rules,
        List<GovernanceRuleResultDTO> failedRules,
        List<GovernanceGapDTO> openBlockingGaps,
        String summary
) {
}
