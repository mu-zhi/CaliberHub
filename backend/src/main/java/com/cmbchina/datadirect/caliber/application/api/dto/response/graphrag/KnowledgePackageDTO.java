package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.util.List;

public record KnowledgePackageDTO(
        String decision,
        String reasonCode,
        String runtimeMode,
        List<String> degradeReasonCodes,
        KnowledgePackageSceneDTO scene,
        KnowledgePackagePlanDTO plan,
        KnowledgePackageContractDTO contract,
        KnowledgePackageCoverageDTO coverage,
        KnowledgePackagePolicyDTO policy,
        KnowledgePackagePathDTO path,
        List<KnowledgePackageEvidenceDTO> evidence,
        KnowledgePackageRiskDTO risk,
        KnowledgePackageTraceDTO trace,
        KnowledgePackageExperimentDTO experiment,
        KnowledgePackageClarificationDTO clarification
) {
}
