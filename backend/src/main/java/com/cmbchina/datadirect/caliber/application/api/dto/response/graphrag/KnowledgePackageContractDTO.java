package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.util.List;

public record KnowledgePackageContractDTO(
        String contractCode,
        String viewCode,
        List<String> visibleFields,
        List<String> maskedFields,
        List<String> restrictedFields,
        List<String> forbiddenFields
) {
}
