package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.util.List;

public record KnowledgePackageClarificationDTO(
        String summary,
        List<SceneCandidateDTO> sceneCandidates,
        List<PlanCandidateDTO> planCandidates,
        List<String> subQuestions,
        List<String> mergeHints,
        List<String> clarificationQuestions
) {
    public record SceneCandidateDTO(
            Long sceneId,
            String sceneCode,
            String sceneTitle,
            Long snapshotId
    ) {
    }

    public record PlanCandidateDTO(
            String sceneCode,
            Long planId,
            String planCode,
            String planName
    ) {
    }
}
