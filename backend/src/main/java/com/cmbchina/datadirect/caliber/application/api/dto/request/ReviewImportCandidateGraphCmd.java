package com.cmbchina.datadirect.caliber.application.api.dto.request;

import java.util.List;

public record ReviewImportCandidateGraphCmd(
        String targetType,
        String targetCode,
        String action,
        String reason,
        String mergeIntoCode,
        List<String> splitLabels,
        String operator
) {
}
