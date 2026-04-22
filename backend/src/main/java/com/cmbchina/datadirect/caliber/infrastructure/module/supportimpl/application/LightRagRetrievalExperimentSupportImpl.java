package com.cmbchina.datadirect.caliber.infrastructure.module.supportimpl.application;

import com.cmbchina.datadirect.caliber.application.support.RetrievalExperimentSupport;
import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneVersionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.InputSlotSchemaMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneVersionPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EvidenceFragmentPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.InputSlotSchemaPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class LightRagRetrievalExperimentSupportImpl implements RetrievalExperimentSupport {

    private final SceneMapper sceneMapper;
    private final SceneVersionMapper sceneVersionMapper;
    private final InputSlotSchemaMapper inputSlotSchemaMapper;
    private final EvidenceFragmentMapper evidenceFragmentMapper;
    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;

    public LightRagRetrievalExperimentSupportImpl(SceneMapper sceneMapper,
                                                  SceneVersionMapper sceneVersionMapper,
                                                  InputSlotSchemaMapper inputSlotSchemaMapper,
                                                  EvidenceFragmentMapper evidenceFragmentMapper,
                                                  ObjectMapper objectMapper) {
        this.sceneMapper = sceneMapper;
        this.sceneVersionMapper = sceneVersionMapper;
        this.inputSlotSchemaMapper = inputSlotSchemaMapper;
        this.evidenceFragmentMapper = evidenceFragmentMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public RetrievalExperimentResult retrieve(RetrievalExperimentRequest request) {
        try {
            String normalizedQuery = normalize(request.queryText());
            List<SceneCandidate> candidateScenes = new ArrayList<>();
            List<EntityCandidate> entityCandidates = new ArrayList<>();
            List<EvidenceCandidate> evidenceCandidates = new ArrayList<>();
            LinkedHashSet<String> referenceRefs = new LinkedHashSet<>();
            List<ScoreBreakdownItem> scoreBreakdown = new ArrayList<>();

            sceneMapper.findAll().stream()
                    .filter(scene -> scene.getStatus() == SceneStatus.PUBLISHED)
                    .filter(scene -> request.domainId() == null || request.domainId().equals(scene.getDomainId()))
                    .map(scene -> toSceneCandidate(scene, normalizedQuery, request.requestedSnapshotId()))
                    .filter(item -> item.score() > 0d)
                    .sorted(Comparator.comparing(SceneCandidate::score).reversed())
                    .limit(5)
                    .forEach(candidateScenes::add);

            inputSlotSchemaMapper.findAll().stream()
                    .filter(slot -> candidateScenes.stream().anyMatch(candidate -> candidate.sceneId().equals(slot.getSceneId())))
                    .limit(5)
                    .forEach(slot -> entityCandidates.add(new EntityCandidate(
                            slot.getSlotCode(),
                            "INPUT_SLOT",
                            slot.getSlotName(),
                            slotScore(request, slot),
                            "LightRAG"
                    )));

            evidenceFragmentMapper.findAll().stream()
                    .filter(evidence -> candidateScenes.stream().anyMatch(candidate -> candidate.sceneId().equals(evidence.getSceneId())))
                    .sorted(Comparator.comparing(EvidenceFragmentPO::getConfidenceScore, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(10)
                    .forEach(evidence -> {
                        evidenceCandidates.add(new EvidenceCandidate(
                                evidence.getEvidenceCode(),
                                evidence.getTitle(),
                                safe(evidence.getSourceAnchor()),
                                safe(evidence.getSourceAnchor()),
                                evidence.getConfidenceScore() == null ? 0.80d : evidence.getConfidenceScore()
                        ));
                        if (evidence.getSourceAnchor() != null && !evidence.getSourceAnchor().isBlank()) {
                            referenceRefs.add(evidence.getSourceAnchor());
                        }
                    });

            scoreBreakdown.add(new ScoreBreakdownItem("scene.lexical", candidateScenes.isEmpty() ? 0.0d : candidateScenes.get(0).score()));
            scoreBreakdown.add(new ScoreBreakdownItem("slot.identifier", entityCandidates.isEmpty() ? 0.0d : entityCandidates.get(0).score()));
            scoreBreakdown.add(new ScoreBreakdownItem("evidence.anchor", evidenceCandidates.isEmpty() ? 0.0d : evidenceCandidates.get(0).score()));

            return new RetrievalExperimentResult(
                    "LightRAG",
                    "heuristic-sidecar/v1",
                    "COMPLETED",
                    false,
                    null,
                    "实验侧车补充了候选场景与证据引用，正式决策仍由原链路给出。",
                    candidateScenes,
                    entityCandidates,
                    evidenceCandidates,
                    List.copyOf(referenceRefs),
                    scoreBreakdown,
                    Map.of(
                            "requestedSnapshotId", request.requestedSnapshotId() == null ? "" : String.valueOf(request.requestedSnapshotId()),
                            "allowedEvidenceScope", request.allowedEvidenceScope()
                    )
            );
        } catch (Exception ex) {
            return new RetrievalExperimentResult(
                    "LightRAG",
                    "heuristic-sidecar/v1",
                    "FALLBACK_FORMAL",
                    true,
                    null,
                    ex.getMessage(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    Map.of("error", ex.getMessage())
            );
        }
    }

    private SceneCandidate toSceneCandidate(ScenePO scene, String normalizedQuery, Long requestedSnapshotId) {
        double lexicalScore = 0d;
        String corpus = normalize(scene.getSceneCode()) + " " + normalize(scene.getSceneTitle()) + " " + normalize(scene.getSceneDescription());
        if (normalizedQuery.isBlank()) {
            lexicalScore = 0.66d;
        } else {
            if (corpus.contains(normalizedQuery)) {
                lexicalScore = 0.88d;
            } else if (corpus.contains("代发") && normalizedQuery.contains("代发")) {
                lexicalScore = 0.74d;
            }
        }
        if (lexicalScore <= 0d) {
            return new SceneCandidate(scene.getId(), scene.getSceneCode(), scene.getSceneTitle(), null, 0.0d, "LightRAG");
        }
        SceneVersionPO snapshot = sceneVersionMapper.findTopBySceneIdOrderByVersionNoDesc(scene.getId()).orElse(null);
        Long snapshotId = requestedSnapshotId != null ? requestedSnapshotId : snapshot == null ? null : snapshot.getId();
        return new SceneCandidate(scene.getId(), scene.getSceneCode(), scene.getSceneTitle(), snapshotId, lexicalScore, "LightRAG");
    }

    private double slotScore(RetrievalExperimentRequest request, InputSlotSchemaPO slot) {
        String identifierType = safe(request.structuredSlots().get("identifierType"));
        String slotCode = safe(slot.getSlotCode()).toUpperCase(Locale.ROOT);
        if (!identifierType.isBlank() && slotCode.contains(identifierType.toUpperCase(Locale.ROOT))) {
            return 0.22d;
        }
        return 0.12d;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
