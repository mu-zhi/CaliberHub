package com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag;

import java.util.List;

public record KnowledgePackageExperimentDTO(
        String adapterName,
        String adapterVersion,
        String status,
        boolean fallbackToFormal,
        String summary,
        List<String> referenceRefs,
        List<ExperimentSceneCandidateDTO> candidateScenes,
        List<KnowledgePackageEvidenceDTO> candidateEvidence,
        List<ExperimentScoreDTO> scoreBreakdown
) {
    public record ExperimentSceneCandidateDTO(
            Long sceneId,
            String sceneCode,
            String sceneTitle,
            Long snapshotId,
            Double score,
            String source
    ) {
    }

    public record ExperimentScoreDTO(
            String label,
            Double score
    ) {
    }
}
