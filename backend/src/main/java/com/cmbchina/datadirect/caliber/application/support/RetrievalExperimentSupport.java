package com.cmbchina.datadirect.caliber.application.support;

import java.util.List;
import java.util.Map;

public interface RetrievalExperimentSupport {

    RetrievalExperimentResult retrieve(RetrievalExperimentRequest request);

    record RetrievalExperimentRequest(
            String traceId,
            String queryText,
            Map<String, Object> structuredSlots,
            Long domainId,
            Long selectedSceneId,
            Long requestedSnapshotId,
            List<String> requestedFields,
            String allowedEvidenceScope,
            String operator
    ) {
        public RetrievalExperimentRequest {
            structuredSlots = structuredSlots == null ? Map.of() : Map.copyOf(structuredSlots);
            requestedFields = requestedFields == null ? List.of() : List.copyOf(requestedFields);
            allowedEvidenceScope = normalizeText(allowedEvidenceScope, "published_evidence_only");
            operator = normalizeText(operator, "system");
        }
    }

    record SceneCandidate(
            Long sceneId,
            String sceneCode,
            String sceneTitle,
            Long snapshotId,
            Double score,
            String source
    ) {
        public SceneCandidate {
            sceneCode = normalizeText(sceneCode, "");
            sceneTitle = normalizeText(sceneTitle, "");
            score = score == null ? 0.0d : score;
            source = normalizeText(source, "LightRAG");
        }
    }

    record EntityCandidate(
            String entityCode,
            String entityType,
            String label,
            Double score,
            String source
    ) {
        public EntityCandidate {
            entityCode = normalizeText(entityCode, "");
            entityType = normalizeText(entityType, "");
            label = normalizeText(label, "");
            score = score == null ? 0.0d : score;
            source = normalizeText(source, "LightRAG");
        }
    }

    record EvidenceCandidate(
            String evidenceCode,
            String title,
            String sourceAnchor,
            String referenceRef,
            Double score
    ) {
        public EvidenceCandidate {
            evidenceCode = normalizeText(evidenceCode, "");
            title = normalizeText(title, "");
            sourceAnchor = normalizeText(sourceAnchor, "");
            referenceRef = normalizeText(referenceRef, "");
            score = score == null ? 0.0d : score;
        }
    }

    record ScoreBreakdownItem(
            String label,
            Double score
    ) {
        public ScoreBreakdownItem {
            label = normalizeText(label, "");
            score = score == null ? 0.0d : score;
        }
    }

    record RetrievalExperimentResult(
            String adapterName,
            String adapterVersion,
            String status,
            boolean fallbackToFormal,
            String decision,
            String summary,
            List<SceneCandidate> candidateScenes,
            List<EntityCandidate> candidateEntities,
            List<EvidenceCandidate> candidateEvidence,
            List<String> referenceRefs,
            List<ScoreBreakdownItem> scoreBreakdown,
            Map<String, String> adapterMetadata
    ) {
        public RetrievalExperimentResult {
            adapterName = normalizeText(adapterName, "LightRAG");
            adapterVersion = normalizeText(adapterVersion, "heuristic-sidecar/v1");
            status = normalizeText(status, fallbackToFormal ? "FALLBACK_FORMAL" : "COMPLETED");
            summary = normalizeText(summary, fallbackToFormal ? "实验侧车不可用，已回退正式链路。" : "实验侧车补充候选场景与证据引用。");
            candidateScenes = candidateScenes == null ? List.of() : List.copyOf(candidateScenes);
            candidateEntities = candidateEntities == null ? List.of() : List.copyOf(candidateEntities);
            candidateEvidence = candidateEvidence == null ? List.of() : List.copyOf(candidateEvidence);
            referenceRefs = referenceRefs == null ? List.of() : List.copyOf(referenceRefs);
            scoreBreakdown = scoreBreakdown == null ? List.of() : List.copyOf(scoreBreakdown);
            adapterMetadata = adapterMetadata == null ? Map.of() : Map.copyOf(adapterMetadata);
        }
    }

    private static String normalizeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
