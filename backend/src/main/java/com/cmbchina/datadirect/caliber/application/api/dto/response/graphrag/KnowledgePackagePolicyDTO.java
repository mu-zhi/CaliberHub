package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

public record KnowledgePackagePolicyDTO(
        String decision,
        String sensitivityLevel,
        Boolean approvalRequired,
        String maskingPlan
) {
}
