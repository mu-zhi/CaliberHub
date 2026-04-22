package com.cmbchina.datadirect.caliber.infrastructure.module.supportimpl.application;

import com.cmbchina.datadirect.caliber.application.api.dto.response.PreprocessResultDTO;
import com.cmbchina.datadirect.caliber.application.support.PreprocessExperimentSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class LightRagPreprocessExperimentSupportImpl implements PreprocessExperimentSupport {

    private final ObjectMapper objectMapper;

    public LightRagPreprocessExperimentSupportImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public PreprocessExperimentResult run(PreprocessExperimentRequest request) {
        PreprocessResultDTO preprocessResult = request.preprocessResult();
        if (preprocessResult == null || preprocessResult.scenes() == null || preprocessResult.scenes().isEmpty()) {
            return new PreprocessExperimentResult(
                    "LightRAG",
                    "heuristic-preprocess/v1",
                    "EMPTY",
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of("no_candidate_scene"),
                    List.of()
            );
        }

        List<CandidateEntity> entities = new ArrayList<>();
        List<CandidateRelation> relations = new ArrayList<>();
        List<CandidateEvidence> evidences = new ArrayList<>();
        LinkedHashSet<String> referenceRefs = new LinkedHashSet<>();

        for (int sceneIndex = 0; sceneIndex < preprocessResult.scenes().size(); sceneIndex++) {
            final int currentSceneIndex = sceneIndex;
            JsonNode scene = preprocessResult.scenes().get(sceneIndex);
            String sceneTitle = text(scene.path("scene_title"), "未命名场景");
            String sceneCode = "EXP-SC-" + (sceneIndex + 1);
            List<String> sceneRefs = toEvidenceRefs(scene.path("source_evidence_lines"), currentSceneIndex);
            referenceRefs.addAll(sceneRefs);

            entities.add(new CandidateEntity(
                    sceneCode,
                    "CANDIDATE_SCENE",
                    sceneTitle,
                    confidence(scene.path("quality").path("confidence"), preprocessResult.confidenceScore()),
                    sceneRefs,
                    Map.of("sceneIndex", currentSceneIndex)
            ));

            JsonNode outputs = scene.path("outputs").path("fields");
            if (outputs.isArray()) {
                outputs.forEach(field -> {
                    String fieldName = text(field.path("name"), text(field.path("label"), null));
                    if (fieldName != null && !fieldName.isBlank()) {
                        String fieldCode = "EXP-FLD-" + normalizeCode(fieldName);
                        entities.add(new CandidateEntity(
                                fieldCode,
                                "FIELD_CONCEPT",
                                fieldName,
                                0.84d,
                                sceneRefs,
                                Map.of("fieldName", fieldName)
                        ));
                        relations.add(new CandidateRelation(
                                sceneCode + "->" + fieldCode,
                                "DECLARES_OUTPUT_FIELD",
                                sceneCode,
                                fieldCode,
                                0.84d,
                                sceneRefs,
                                Map.of("sceneIndex", currentSceneIndex)
                        ));
                    }
                });
            }

            JsonNode variants = scene.path("sql_variants");
            if (variants.isArray()) {
                variants.forEach(variant -> {
                    variant.path("source_tables").forEach(table -> {
                        String tableName = table.asText("").trim();
                        if (!tableName.isBlank()) {
                            String tableCode = "EXP-TBL-" + normalizeCode(tableName);
                            entities.add(new CandidateEntity(
                                    tableCode,
                                    "SOURCE_TABLE",
                                    tableName,
                                    0.82d,
                                    sceneRefs,
                                    Map.of("sourceTable", tableName)
                            ));
                            relations.add(new CandidateRelation(
                                    sceneCode + "->" + tableCode,
                                    "USES_SOURCE_TABLE",
                                    sceneCode,
                                    tableCode,
                                    0.82d,
                                    sceneRefs,
                                    Map.of()
                            ));
                        }
                    });
                });
            }

            evidences.add(new CandidateEvidence(
                    "EXP-EV-" + (sceneIndex + 1),
                    "证据片段: " + sceneTitle,
                    sceneRefs.isEmpty() ? "scene:" + sceneIndex : sceneRefs.get(0),
                    0.90d,
                    sceneRefs,
                    Map.of("sceneIndex", currentSceneIndex)
            ));
        }

        return new PreprocessExperimentResult(
                "LightRAG",
                "heuristic-preprocess/v1",
                "COMPLETED",
                entities,
                relations,
                evidences,
                List.copyOf(referenceRefs),
                List.of("experimental_candidates_only"),
                List.of()
        );
    }

    private List<String> toEvidenceRefs(JsonNode linesNode, int sceneIndex) {
        List<String> refs = new ArrayList<>();
        if (linesNode != null && linesNode.isArray()) {
            linesNode.forEach(line -> refs.add("scene:" + sceneIndex + ":line:" + line.asText("")));
        }
        return refs;
    }

    private double confidence(JsonNode node, Double fallback) {
        if (node != null && !node.isMissingNode() && node.isNumber()) {
            return node.asDouble();
        }
        return fallback == null ? 0.80d : fallback;
    }

    private String text(JsonNode node, String fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        String value = node.asText("").trim();
        return value.isBlank() ? fallback : value;
    }

    private String normalizeCode(String value) {
        return value == null ? "UNK" : value.replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase(Locale.ROOT);
    }
}
