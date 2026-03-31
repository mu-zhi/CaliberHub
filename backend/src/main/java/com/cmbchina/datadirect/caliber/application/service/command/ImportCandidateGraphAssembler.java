package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.response.CandidateGraphDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportGraphEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportGraphNodeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.PreprocessResultDTO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportEvidenceCandidatePO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ImportCandidateGraphAssembler {

    private static final String STATUS_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";

    public CandidateGraphDTO buildSnapshotFromResult(String taskId, String materialId, PreprocessResultDTO result) {
        List<JsonNode> scenes = result == null || result.scenes() == null ? List.of() : result.scenes();
        if (scenes.isEmpty()) {
            return CandidateGraphDTO.empty();
        }

        Map<String, ImportGraphNodeDTO> nodes = new LinkedHashMap<>();
        Map<String, ImportGraphEdgeDTO> edges = new LinkedHashMap<>();
        double fallbackConfidence = result == null || result.confidenceScore() == null ? 0.0 : result.confidenceScore();

        for (int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++) {
            JsonNode scene = scenes.get(sceneIndex);
            String sceneId = buildSceneNodeId(taskId, sceneIndex);
            double confidence = readConfidence(scene, fallbackConfidence);
            String sceneTitle = text(
                    scene.path("scene_title"),
                    text(scene.path("scene_code_guess"), "未命名场景" + (sceneIndex + 1))
            );
            nodes.put(sceneId, new ImportGraphNodeDTO(
                    sceneId,
                    "CANDIDATE_SCENE",
                    sceneTitle,
                    STATUS_PENDING_CONFIRMATION,
                    confidence,
                    List.of()
            ));

            JsonNode inputs = scene.path("inputs").path("params");
            if (inputs.isArray()) {
                for (JsonNode item : inputs) {
                    String inputName = firstNonBlank(
                            text(item.path("name")),
                            text(item.path("name_zh")),
                            text(item.path("name_en")),
                            text(item.path("label"))
                    );
                    if (inputName == null) {
                        continue;
                    }
                    String inputId = "input-field:" + inputName;
                    nodes.putIfAbsent(inputId, new ImportGraphNodeDTO(
                            inputId,
                            "INPUT_FIELD",
                            inputName,
                            STATUS_PENDING_CONFIRMATION,
                            confidence,
                            List.of()
                    ));
                    edges.putIfAbsent(sceneId + "->" + inputId, new ImportGraphEdgeDTO(
                            sceneId + "->" + inputId,
                            sceneId,
                            inputId,
                            "DECLARES_INPUT",
                            STATUS_PENDING_CONFIRMATION,
                            confidence,
                            List.of()
                    ));
                }
            }

            JsonNode outputs = scene.path("outputs").path("fields");
            if (outputs.isArray()) {
                for (JsonNode item : outputs) {
                    String outputName = firstNonBlank(
                            text(item.path("display_name")),
                            text(item.path("field_name")),
                            text(item.path("name")),
                            text(item.path("label"))
                    );
                    if (outputName == null) {
                        continue;
                    }
                    String outputId = "output-field:" + outputName;
                    nodes.putIfAbsent(outputId, new ImportGraphNodeDTO(
                            outputId,
                            "OUTPUT_FIELD",
                            outputName,
                            STATUS_PENDING_CONFIRMATION,
                            confidence,
                            List.of()
                    ));
                    edges.putIfAbsent(sceneId + "=>" + outputId, new ImportGraphEdgeDTO(
                            sceneId + "=>" + outputId,
                            sceneId,
                            outputId,
                            "DECLARES_OUTPUT",
                            STATUS_PENDING_CONFIRMATION,
                            confidence,
                            List.of()
                    ));
                }
            }

            JsonNode sqlVariants = scene.path("sql_variants");
            if (sqlVariants.isArray()) {
                for (JsonNode variant : sqlVariants) {
                    JsonNode sourceTables = variant.path("source_tables");
                    if (!sourceTables.isArray()) {
                        continue;
                    }
                    for (JsonNode table : sourceTables) {
                        String tableName = text(table, null);
                        if (tableName == null) {
                            continue;
                        }
                        String tableId = "source-table:" + tableName;
                        nodes.putIfAbsent(tableId, new ImportGraphNodeDTO(
                                tableId,
                                "SOURCE_TABLE",
                                tableName,
                                STATUS_PENDING_CONFIRMATION,
                                confidence,
                                List.of()
                        ));
                        edges.putIfAbsent(sceneId + "::" + tableId, new ImportGraphEdgeDTO(
                                sceneId + "::" + tableId,
                                sceneId,
                                tableId,
                                "USES_SOURCE_TABLE",
                                STATUS_PENDING_CONFIRMATION,
                                confidence,
                                List.of()
                        ));
                    }
                }
            }
        }

        return new CandidateGraphDTO(new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()));
    }

    public CandidateGraphDTO enrichWithPersistedEvidence(CandidateGraphDTO base, List<ImportEvidenceCandidatePO> evidences) {
        CandidateGraphDTO snapshot = base == null ? CandidateGraphDTO.empty() : base;
        Map<String, ImportGraphNodeDTO> nodes = new LinkedHashMap<>();
        snapshot.nodes().forEach(node -> nodes.put(node.id(), node));
        Map<String, ImportGraphEdgeDTO> edges = new LinkedHashMap<>();
        snapshot.edges().forEach(edge -> edges.put(edge.id(), edge));

        List<ImportEvidenceCandidatePO> safeEvidences = evidences == null ? List.of() : evidences;
        for (ImportEvidenceCandidatePO evidence : safeEvidences) {
            String evidenceId = "evidence:" + evidence.getCandidateCode();
            nodes.put(evidenceId, new ImportGraphNodeDTO(
                    evidenceId,
                    "CANDIDATE_EVIDENCE_FRAGMENT",
                    text(evidence.getAnchorLabel(), evidence.getCandidateCode()),
                    text(evidence.getConfirmationStatus(), STATUS_PENDING_CONFIRMATION),
                    1.0,
                    List.of(evidence.getCandidateCode())
            ));
            String sceneId = text(evidence.getSceneCandidateCode(), null);
            if (sceneId == null) {
                continue;
            }
            edges.put(sceneId + "~~" + evidenceId, new ImportGraphEdgeDTO(
                    sceneId + "~~" + evidenceId,
                    sceneId,
                    evidenceId,
                    "SUPPORTED_BY_EVIDENCE",
                    text(evidence.getConfirmationStatus(), STATUS_PENDING_CONFIRMATION),
                    1.0,
                    List.of(evidence.getCandidateCode())
            ));
        }

        return new CandidateGraphDTO(new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()));
    }

    private String buildSceneNodeId(String taskId, int sceneIndex) {
        String safeTaskId = taskId == null ? "IMPORT" : taskId.replace("-", "").trim().toUpperCase(Locale.ROOT);
        if (safeTaskId.isBlank()) {
            safeTaskId = "IMPORT";
        }
        if (safeTaskId.length() > 16) {
            safeTaskId = safeTaskId.substring(0, 16);
        }
        return "SCN-" + safeTaskId + "-" + String.format(Locale.ROOT, "%02d", sceneIndex + 1);
    }

    private double readConfidence(JsonNode scene, double fallback) {
        JsonNode confidenceNode = scene.path("quality").path("confidence");
        if (confidenceNode.isNumber()) {
            return confidenceNode.asDouble(fallback);
        }
        return fallback;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = text(value, null);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String text(JsonNode node, String fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        return text(node.asText(fallback), fallback);
    }

    private String text(JsonNode node) {
        return text(node, null);
    }

    private String text(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
