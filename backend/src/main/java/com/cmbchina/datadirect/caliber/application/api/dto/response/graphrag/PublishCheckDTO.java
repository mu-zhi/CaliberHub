package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import com.cmbchina.datadirect.caliber.application.api.dto.response.governance.GovernanceGapDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.governance.GovernanceRuleResultDTO;

import java.util.List;

public record PublishCheckDTO(
        Long sceneId,
        Boolean publishReady,
        List<PublishCheckItemDTO> items,
        List<GovernanceRuleResultDTO> failedRules,
        List<GovernanceGapDTO> openBlockingGaps,
        String summary
) {
}
