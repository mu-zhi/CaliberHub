package com.cmbchina.datadirect.caliber.application.api.dto.response;

import com.cmbchina.datadirect.caliber.application.api.dto.response.governance.SceneGovernanceSummaryDTO;

public record ConfirmImportSceneCandidateResultDTO(
        SceneDTO scene,
        SceneGovernanceSummaryDTO governanceSummary
) {
}
