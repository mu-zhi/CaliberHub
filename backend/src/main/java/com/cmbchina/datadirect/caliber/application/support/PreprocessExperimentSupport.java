package com.cmbchina.datadirect.caliber.application.support;

import com.cmbchina.datadirect.caliber.application.api.dto.response.PreprocessResultDTO;

import java.util.List;
import java.util.Map;

public interface PreprocessExperimentSupport {

    PreprocessExperimentResult run(PreprocessExperimentRequest request);

    record PreprocessExperimentRequest(
            String importTaskId,
            String materialId,
            List<String> normalizedChunks,
            List<String> attachmentRefs,
            List<String> allowedModalityScope,
            String traceId,
            PreprocessResultDTO preprocessResult
    ) {
        public PreprocessExperimentRequest {
            normalizedChunks = normalizedChunks == null ? List.of() : List.copyOf(normalizedChunks);
            attachmentRefs = attachmentRefs == null ? List.of() : List.copyOf(attachmentRefs);
            allowedModalityScope = allowedModalityScope == null ? List.of() : List.copyOf(allowedModalityScope);
        }
    }

    record CandidateEntity(
            String entityCode,
            String entityType,
            String label,
            Double confidenceScore,
            List<String> referenceRefs,
            Map<String, Object> payload
    ) {
        public CandidateEntity {
            referenceRefs = referenceRefs == null ? List.of() : List.copyOf(referenceRefs);
            payload = payload == null ? Map.of() : Map.copyOf(payload);
        }
    }

    record CandidateRelation(
            String relationCode,
            String relationType,
            String sourceCode,
            String targetCode,
            Double confidenceScore,
            List<String> referenceRefs,
            Map<String, Object> payload
    ) {
        public CandidateRelation {
            referenceRefs = referenceRefs == null ? List.of() : List.copyOf(referenceRefs);
            payload = payload == null ? Map.of() : Map.copyOf(payload);
        }
    }

    record CandidateEvidence(
            String evidenceCode,
            String title,
            String sourceAnchor,
            Double confidenceScore,
            List<String> referenceRefs,
            Map<String, Object> payload
    ) {
        public CandidateEvidence {
            referenceRefs = referenceRefs == null ? List.of() : List.copyOf(referenceRefs);
            payload = payload == null ? Map.of() : Map.copyOf(payload);
        }
    }

    record PreprocessExperimentResult(
            String adapterName,
            String adapterVersion,
            String status,
            List<CandidateEntity> candidateEntities,
            List<CandidateRelation> candidateRelations,
            List<CandidateEvidence> candidateEvidence,
            List<String> referenceRefs,
            List<String> warnings,
            List<String> formalAssetWrites
    ) {
        public PreprocessExperimentResult {
            adapterName = normalizeText(adapterName, "LightRAG");
            adapterVersion = normalizeText(adapterVersion, "heuristic-preprocess/v1");
            status = normalizeText(status, "COMPLETED");
            candidateEntities = candidateEntities == null ? List.of() : List.copyOf(candidateEntities);
            candidateRelations = candidateRelations == null ? List.of() : List.copyOf(candidateRelations);
            candidateEvidence = candidateEvidence == null ? List.of() : List.copyOf(candidateEvidence);
            referenceRefs = referenceRefs == null ? List.of() : List.copyOf(referenceRefs);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
            formalAssetWrites = formalAssetWrites == null ? List.of() : List.copyOf(formalAssetWrites);
        }
    }

    private static String normalizeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
