package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphNodeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphSummaryDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.PreprocessResultDTO;
import com.cmbchina.datadirect.caliber.application.service.support.ImportSceneNormalizationSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ImportCandidateGraphBuildService {

    private final ObjectMapper objectMapper;

    public ImportCandidateGraphBuildService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ImportCandidateGraphDTO build(String taskId, String materialId, PreprocessResultDTO result) {
        String graphId = taskId + ":" + materialId;
        List<ImportCandidateGraphNodeDTO> nodes = new ArrayList<>();
        List<ImportCandidateGraphEdgeDTO> edges = new ArrayList<>();

        List<JsonNode> scenes = ImportSceneNormalizationSupport.normalize(objectMapper, result.scenes());
        for (int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++) {
            JsonNode scene = scenes.get(sceneIndex);
            String sceneCandidateCode = ImportCandidateCodeSupport.sceneCandidateCode(taskId, sceneIndex);
            String sceneCode = code(taskId, "SC", sceneIndex + 1);
            nodes.add(node(graphId, sceneCode, sceneCandidateCode, "CANDIDATE_SCENE", text(scene.path("scene_title")), "PENDING_CONFIRMATION", "LOW", score(scene), payload(sceneIndex, null, Map.of())));
            appendFieldConceptNodes(nodes, graphId, taskId, sceneIndex, sceneCandidateCode, scene);

            JsonNode variants = arrayNode(scene.path("sql_variants"));
            for (int variantIndex = 0; variantIndex < variants.size(); variantIndex++) {
                JsonNode variant = variants.get(variantIndex);
                String planCode = code(taskId, "PLN", sceneIndex + 1, variantIndex + 1);
                nodes.add(node(graphId, planCode, sceneCandidateCode, "CANDIDATE_PLAN", text(variant.path("variant_name"), "默认方案"), "PENDING_CONFIRMATION", "LOW", score(scene), payload(sceneIndex, variantIndex, Map.of())));
                edges.add(edge(graphId, code(taskId, "EDGE", sceneIndex + 1, variantIndex + 1), sceneCandidateCode, "SCENE_HAS_PLAN", sceneCode, planCode, "候选方案", "PENDING_CONFIRMATION", "LOW", score(scene), payload(sceneIndex, variantIndex, Map.of())));

                for (String sourceTable : stringList(variant.path("source_tables"))) {
                    String tableCode = code(taskId, "SRC", sceneIndex + 1, variantIndex + 1, normalizeCode(sourceTable));
                    nodes.add(node(graphId, tableCode, sceneCandidateCode, "SOURCE_TABLE", sourceTable, "PENDING_CONFIRMATION", "HIGH", 0.82d, payload(sceneIndex, variantIndex, Map.of("sourceTable", sourceTable))));
                    edges.add(edge(graphId, code(taskId, "EDGE-SRC", sceneIndex + 1, variantIndex + 1, normalizeCode(sourceTable)), sceneCandidateCode, "PLAN_USES_SOURCE_TABLE", planCode, tableCode, "来源表", "PENDING_CONFIRMATION", "HIGH", 0.82d, payload(sceneIndex, variantIndex, Map.of())));
                }

                for (String columnName : stringList(variant.path("source_columns"))) {
                    String colCode = code(taskId, "COL", sceneIndex + 1, variantIndex + 1, normalizeCode(columnName));
                    nodes.add(node(graphId, colCode, sceneCandidateCode, "SOURCE_COLUMN", columnName, "PENDING_CONFIRMATION", "MEDIUM", 0.80d, payload(sceneIndex, variantIndex, Map.of("columnName", columnName))));
                    edges.add(edge(graphId, code(taskId, "EDGE-COL", sceneIndex + 1, variantIndex + 1, normalizeCode(columnName)), sceneCandidateCode, "PLAN_USES_SOURCE_COLUMN", planCode, colCode, "来源字段", "PENDING_CONFIRMATION", "MEDIUM", 0.80d, payload(sceneIndex, variantIndex, Map.of("columnName", columnName))));
                }

                String timeSemantic = text(variant.path("default_time_semantic"));
                if (timeSemantic == null || timeSemantic.isBlank()) {
                    timeSemantic = detectTimeSemanticFromSql(text(variant.path("sql_text")));
                }
                if (timeSemantic != null && !timeSemantic.isBlank()) {
                    String timeCode = code(taskId, "TIME", sceneIndex + 1, variantIndex + 1);
                    nodes.add(node(graphId, timeCode, sceneCandidateCode, "TIME_SEMANTIC", timeSemantic, "PENDING_CONFIRMATION", "HIGH", 0.78d, payload(sceneIndex, variantIndex, Map.of("defaultTimeSemantic", timeSemantic))));
                    edges.add(edge(graphId, code(taskId, "EDGE-TIME", sceneIndex + 1, variantIndex + 1), sceneCandidateCode, "PLAN_USES_TIME_SEMANTIC", planCode, timeCode, "时间语义", "PENDING_CONFIRMATION", "HIGH", 0.78d, payload(sceneIndex, variantIndex, Map.of())));
                }

                for (JsonNode joinRel : arrayNode(variant.path("join_relations"))) {
                    String joinLabel = text(joinRel.path("label"), text(joinRel.path("description")));
                    if (joinLabel != null && !joinLabel.isBlank()) {
                        String joinCode = code(taskId, "JOIN", sceneIndex + 1, variantIndex + 1, normalizeCode(joinLabel));
                        nodes.add(node(graphId, joinCode, sceneCandidateCode, "JOIN_RELATION", joinLabel, "PENDING_CONFIRMATION", "HIGH", 0.75d, payload(sceneIndex, variantIndex, Map.of("joinLabel", joinLabel))));
                        edges.add(edge(graphId, code(taskId, "EDGE-JOIN", sceneIndex + 1, variantIndex + 1, normalizeCode(joinLabel)), sceneCandidateCode, "PLAN_USES_JOIN_RELATION", planCode, joinCode, "表间关联", "PENDING_CONFIRMATION", "HIGH", 0.75d, payload(sceneIndex, variantIndex, Map.of("joinLabel", joinLabel))));
                    }
                }
            }

            for (JsonNode input : arrayNode(scene.path("inputs").path("params"))) {
                String label = text(input.path("name"), text(input.path("name_zh")));
                if (label != null && looksLikeIdentifier(label)) {
                    String nodeCode = code(taskId, "ID", sceneIndex + 1, normalizeCode(label));
                    nodes.add(node(graphId, nodeCode, sceneCandidateCode, "IDENTIFIER", label, "PENDING_CONFIRMATION", "HIGH", 0.86d, payload(sceneIndex, null, Map.of("slotName", label))));
                    edges.add(edge(graphId, code(taskId, "EDGE-ID", sceneIndex + 1, normalizeCode(label)), sceneCandidateCode, "SCENE_USES_IDENTIFIER", sceneCode, nodeCode, "标识对象", "PENDING_CONFIRMATION", "HIGH", 0.86d, payload(sceneIndex, null, Map.of())));
                }
            }

            // evidence fragments — always create one per scene from available evidence or raw text
            String evidenceCode = code(taskId, "EV", sceneIndex + 1);
            String sceneLabel = text(scene.path("scene_title"), "场景" + (sceneIndex + 1));
            String evidenceLabel = "证据片段: " + sceneLabel;
            JsonNode evidenceLines = scene.path("source_evidence_lines");
            List<String> evidenceLineList = evidenceLines.isArray() && evidenceLines.size() > 0 ? stringList(evidenceLines) : List.of();
            nodes.add(node(graphId, evidenceCode, sceneCandidateCode, "CANDIDATE_EVIDENCE_FRAGMENT", evidenceLabel, "PENDING_CONFIRMATION", "LOW", 0.90d, payload(sceneIndex, null, Map.of("evidenceLines", evidenceLineList))));
            edges.add(edge(graphId, code(taskId, "EDGE-EV", sceneIndex + 1), sceneCandidateCode, "NODE_SUPPORTED_BY_EVIDENCE", sceneCode, evidenceCode, "证据支撑", "PENDING_CONFIRMATION", "LOW", 0.90d, payload(sceneIndex, null, Map.of())));
        }

        int pendingReviewTotal = (int) nodes.stream().filter(item -> "PENDING_CONFIRMATION".equals(item.reviewStatus())).count();
        return new ImportCandidateGraphDTO(
                taskId,
                materialId,
                graphId,
                new ImportCandidateGraphSummaryDTO(nodes.size(), edges.size(), pendingReviewTotal),
                dedupeNodes(nodes),
                dedupeEdges(edges)
        );
    }

    private ImportCandidateGraphNodeDTO node(String graphId, String nodeCode, String sceneCandidateCode,
                                             String nodeType, String label, String reviewStatus,
                                             String riskLevel, Double confidenceScore, JsonNode payload) {
        return new ImportCandidateGraphNodeDTO(nodeCode, sceneCandidateCode, nodeType, label == null ? "" : label, reviewStatus, riskLevel, confidenceScore, payload, "");
    }

    private ImportCandidateGraphEdgeDTO edge(String graphId, String edgeCode, String sceneCandidateCode,
                                             String edgeType, String sourceNodeCode, String targetNodeCode,
                                             String label, String reviewStatus, String riskLevel,
                                             Double confidenceScore, JsonNode payload) {
        return new ImportCandidateGraphEdgeDTO(edgeCode, sceneCandidateCode, edgeType, sourceNodeCode, targetNodeCode, label, reviewStatus, riskLevel, confidenceScore, payload);
    }

    private JsonNode payload(Integer sceneIndex, Integer variantIndex, Map<String, Object> extras) {
        ObjectNode node = objectMapper.createObjectNode();
        if (sceneIndex != null) {
            node.put("sceneIndex", sceneIndex);
        }
        if (variantIndex != null) {
            node.put("variantIndex", variantIndex);
        }
        extras.forEach((key, value) -> node.set(key, objectMapper.valueToTree(value)));
        return node;
    }

    private void appendFieldConceptNodes(List<ImportCandidateGraphNodeDTO> nodes,
                                         String graphId,
                                         String taskId,
                                         int sceneIndex,
                                         String sceneCandidateCode,
                                         JsonNode scene) {
        LinkedHashSet<String> fieldNames = new LinkedHashSet<>();
        arrayNode(scene.path("outputs").path("fields")).forEach(field -> {
            String fieldName = firstNonBlank(
                    text(field.path("name")),
                    text(field.path("display_name")),
                    text(field.path("label")),
                    text(field.path("source_field"))
            );
            if (fieldName != null && !fieldName.isBlank()) {
                fieldNames.add(fieldName);
            }
        });
        arrayNode(scene.path("sql_variants")).forEach(variant ->
                arrayNode(variant.path("output_fields")).forEach(field -> {
                    String fieldName = firstNonBlank(text(field.path("name")), text(field.path("label")));
                    if (fieldName != null && !fieldName.isBlank()) {
                        fieldNames.add(fieldName);
                    }
                })
        );
        fieldNames.forEach(fieldName -> {
            String fieldCode = code(taskId, "FLD", sceneIndex + 1, normalizeCode(fieldName));
            nodes.add(node(graphId, fieldCode, sceneCandidateCode, "FIELD_CONCEPT", fieldName, "PENDING_CONFIRMATION", "MEDIUM", 0.85d, payload(sceneIndex, null, Map.of("fieldName", fieldName))));
        });
    }

    private ArrayNode arrayNode(JsonNode node) {
        return node != null && node.isArray() ? (ArrayNode) node : objectMapper.createArrayNode();
    }

    private String text(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText("");
    }

    private String text(JsonNode node, String fallback) {
        String current = text(node);
        return current == null || current.isBlank() ? fallback : current;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Double score(JsonNode scene) {
        return scene.path("quality").path("confidence").asDouble(0.8d);
    }

    private String code(String taskId, Object... parts) {
        String prefix = taskId == null ? "00000000" : taskId.replace("-", "");
        prefix = prefix.length() > 8 ? prefix.substring(0, 8) : prefix;
        return Stream.concat(Stream.of(prefix), Arrays.stream(parts).map(String::valueOf))
                .collect(Collectors.joining("-"));
    }

    private String normalizeCode(String value) {
        return value == null ? "UNK" : value.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "").toUpperCase(Locale.ROOT);
    }

    private List<String> stringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                String text = item.asText("").trim();
                if (!text.isBlank()) {
                    result.add(text);
                }
            });
        }
        return result;
    }

    private boolean looksLikeIdentifier(String label) {
        String normalized = label == null ? "" : label.toUpperCase(Locale.ROOT);
        return normalized.contains("协议号")
                || normalized.contains("户口号")
                || normalized.contains("客户号")
                || normalized.contains("证件号")
                || normalized.contains("批次号")
                || normalized.contains("ID")
                || normalized.contains("NBR")
                || normalized.contains("_NO")
                || normalized.contains("AGR")
                || normalized.contains("ACT")
                || normalized.contains("CST");
    }

    private String detectTimeSemanticFromSql(String sqlText) {
        if (sqlText == null || sqlText.isBlank()) return null;
        String upper = sqlText.toUpperCase(Locale.ROOT);
        String[] timeColumns = {"TRX_DT", "APPLY_DT", "VALUE_DT", "SETTLEMENT_DT", "BIZ_DT", "TRADE_DT", "FUND_DT", "SHARE_DT", "EFF_DT", "EXPIRY_DT"};
        for (String col : timeColumns) {
            if (upper.contains(col)) return col;
        }
        return null;
    }

    private List<ImportCandidateGraphNodeDTO> dedupeNodes(List<ImportCandidateGraphNodeDTO> nodes) {
        return new ArrayList<>(nodes.stream().collect(Collectors.toMap(
                ImportCandidateGraphNodeDTO::nodeCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        )).values());
    }

    private List<ImportCandidateGraphEdgeDTO> dedupeEdges(List<ImportCandidateGraphEdgeDTO> edges) {
        return new ArrayList<>(edges.stream().collect(Collectors.toMap(
                ImportCandidateGraphEdgeDTO::edgeCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        )).values());
    }
}
