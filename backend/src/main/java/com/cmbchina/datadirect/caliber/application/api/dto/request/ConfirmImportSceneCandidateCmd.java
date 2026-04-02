package com.cmbchina.datadirect.caliber.application.api.dto.request;

public record ConfirmImportSceneCandidateCmd(
        Long domainId,
        String domain,
        String operator
) {
}
