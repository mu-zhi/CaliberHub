package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.ReviewImportCandidateGraphCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphDTO;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportCandidateGraphEdgeMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportCandidateGraphNodeMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportCandidateReviewEventMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportSceneCandidateMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportTaskMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportCandidateGraphEdgePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportCandidateGraphNodePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportCandidateReviewEventPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportSceneCandidatePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportTaskPO;
import com.cmbchina.datadirect.caliber.application.service.query.ImportCandidateGraphQueryAppService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ImportCandidateGraphCommandAppService {

    private final ImportCandidateGraphNodeMapper nodeMapper;
    private final ImportCandidateGraphEdgeMapper edgeMapper;
    private final ImportCandidateReviewEventMapper reviewEventMapper;
    private final ImportSceneCandidateMapper importSceneCandidateMapper;
    private final ImportTaskMapper importTaskMapper;
    private final ImportCandidateGraphQueryAppService queryAppService;
    private final ObjectMapper objectMapper;

    public ImportCandidateGraphCommandAppService(ImportCandidateGraphNodeMapper nodeMapper,
                                                  ImportCandidateGraphEdgeMapper edgeMapper,
                                                  ImportCandidateReviewEventMapper reviewEventMapper,
                                                  ImportSceneCandidateMapper importSceneCandidateMapper,
                                                  ImportTaskMapper importTaskMapper,
                                                  ImportCandidateGraphQueryAppService queryAppService,
                                                  ObjectMapper objectMapper) {
        this.nodeMapper = nodeMapper;
        this.edgeMapper = edgeMapper;
        this.reviewEventMapper = reviewEventMapper;
        this.importSceneCandidateMapper = importSceneCandidateMapper;
        this.importTaskMapper = importTaskMapper;
        this.queryAppService = queryAppService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportCandidateGraphDTO review(String taskId, ReviewImportCandidateGraphCmd cmd) {
        String action = normalizeAction(cmd.action());
        if ("NODE".equalsIgnoreCase(cmd.targetType())) {
            ImportCandidateGraphNodePO node = requireNode(taskId, cmd.targetCode());
            String before = node.getReviewStatus();
            switch (action) {
                case "ACCEPT" -> node.setReviewStatus("ACCEPTED");
                case "REJECT" -> node.setReviewStatus("REJECTED");
                case "MERGE" -> {
                    requireNode(taskId, cmd.mergeIntoCode());
                    node.setReviewStatus("MERGED");
                    node.setCanonicalNodeCode(cmd.mergeIntoCode());
                }
                default -> throw new DomainValidationException("unsupported action: " + action);
            }
            node.setUpdatedAt(OffsetDateTime.now());
            nodeMapper.save(node);
            reviewEventMapper.save(buildReviewEvent(taskId, node.getMaterialId(), node.getGraphId(), "NODE", node.getNodeCode(), action, before, node.getReviewStatus(), cmd));
        } else {
            ImportCandidateGraphEdgePO edge = requireEdge(taskId, cmd.targetCode());
            String before = edge.getReviewStatus();
            edge.setReviewStatus("ACCEPT".equals(action) ? "ACCEPTED" : "REJECT".equals(action) ? "REJECTED" : before);
            edge.setUpdatedAt(OffsetDateTime.now());
            edgeMapper.save(edge);
            reviewEventMapper.save(buildReviewEvent(taskId, edge.getMaterialId(), edge.getGraphId(), "EDGE", edge.getEdgeCode(), action, before, edge.getReviewStatus(), cmd));
        }

        patchCandidatePayloads(taskId);
        return queryAppService.getByTaskId(taskId);
    }

    private void patchCandidatePayloads(String taskId) {
        ImportTaskPO task = importTaskMapper.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("import task not found: " + taskId));
        List<ImportSceneCandidatePO> sceneCandidates = importSceneCandidateMapper.findByTaskId(taskId);
        ObjectNode preprocessRoot = readObject(task.getPreprocessResultJson());

        for (ImportSceneCandidatePO sceneCandidate : sceneCandidates) {
            ObjectNode scenePayload = readObject(sceneCandidate.getCandidatePayloadJson());
            List<ImportCandidateGraphNodePO> acceptedNodes = nodeMapper.findByTaskIdAndSceneCandidateCodeAndReviewStatus(
                    taskId,
                    sceneCandidate.getCandidateCode(),
                    "ACCEPTED"
            );
            applyAcceptedNodes(scenePayload, acceptedNodes);
            sceneCandidate.setCandidatePayloadJson(write(scenePayload));
            importSceneCandidateMapper.save(sceneCandidate);
            replaceSceneInTaskPayload(preprocessRoot, sceneCandidate.getSceneIndex(), scenePayload);
        }

        task.setPreprocessResultJson(write(preprocessRoot));
        importTaskMapper.save(task);
    }

    private void applyAcceptedNodes(ObjectNode scenePayload, List<ImportCandidateGraphNodePO> acceptedNodes) {
        for (ImportCandidateGraphNodePO node : acceptedNodes) {
            ObjectNode payload = readObject(node.getPayloadJson());
            switch (node.getNodeType()) {
                case "IDENTIFIER" -> appendIdentifier(scenePayload, payload.path("slotName").asText(node.getNodeLabel()), node.getNodeLabel());
                case "FIELD_CONCEPT" -> appendOutputField(scenePayload, node.getNodeLabel());
                case "TIME_SEMANTIC" -> applyDefaultTimeSemantic(scenePayload, payload.path("defaultTimeSemantic").asText(node.getNodeLabel()));
                case "SOURCE_TABLE" -> appendSourceTable(scenePayload, payload.path("sourceTable").asText(node.getNodeLabel()));
                case "SOURCE_COLUMN" -> appendSourceColumn(scenePayload, payload.path("columnName").asText(node.getNodeLabel()));
                case "JOIN_RELATION" -> appendJoinRelationHint(scenePayload, node.getNodeLabel(), payload);
                default -> {
                    // no-op for scene/plan/evidence/business term
                }
            }
        }
    }

    private void appendIdentifier(ObjectNode scene, String slotName, String label) {
        ObjectNode inputs = ensureObject(scene, "inputs");
        ArrayNode params = ensureArray(inputs, "params");
        for (JsonNode param : params) {
            String name = param.path("name").asText(param.path("name_zh").asText(""));
            if (slotName.equals(name) || label.equals(name)) return;
        }
        ObjectNode param = objectMapper.createObjectNode();
        param.put("name", slotName);
        param.put("name_zh", label);
        param.put("type", "STRING");
        param.put("required", true);
        param.put("accepted_from_candidate_graph", true);
        params.add(param);
    }

    private void appendOutputField(ObjectNode scene, String fieldName) {
        ObjectNode outputs = ensureObject(scene, "outputs");
        ArrayNode fields = ensureArray(outputs, "fields");
        for (JsonNode field : fields) {
            if (fieldName.equals(field.path("name").asText(""))) return;
        }
        ObjectNode field = objectMapper.createObjectNode();
        field.put("name", fieldName);
        field.put("accepted_from_candidate_graph", true);
        fields.add(field);
    }

    private void applyDefaultTimeSemantic(ObjectNode scene, String timeSemantic) {
        JsonNode variants = scene.path("sql_variants");
        if (variants.isArray()) {
            for (JsonNode variant : variants) {
                if (variant.isObject()) {
                    ((ObjectNode) variant).put("default_time_semantic", timeSemantic);
                }
            }
        }
    }

    private void appendSourceTable(ObjectNode scene, String sourceTable) {
        JsonNode variants = scene.path("sql_variants");
        if (variants.isArray()) {
            for (JsonNode variant : variants) {
                if (variant.isObject()) {
                    ArrayNode tables = ensureArray((ObjectNode) variant, "source_tables");
                    boolean exists = false;
                    for (JsonNode t : tables) {
                        if (sourceTable.equals(t.asText(""))) { exists = true; break; }
                    }
                    if (!exists) tables.add(sourceTable);
                }
            }
        }
    }

    private void appendSourceColumn(ObjectNode scene, String columnName) {
        JsonNode variants = scene.path("sql_variants");
        if (variants.isArray()) {
            for (JsonNode variant : variants) {
                if (variant.isObject()) {
                    ArrayNode columns = ensureArray((ObjectNode) variant, "source_columns");
                    boolean exists = false;
                    for (JsonNode c : columns) {
                        if (columnName.equals(c.asText(""))) { exists = true; break; }
                    }
                    if (!exists) columns.add(columnName);
                }
            }
        }
    }

    private void appendJoinRelationHint(ObjectNode scene, String joinLabel, ObjectNode payload) {
        JsonNode variants = scene.path("sql_variants");
        if (variants.isArray()) {
            for (JsonNode variant : variants) {
                if (variant.isObject()) {
                    ArrayNode joins = ensureArray((ObjectNode) variant, "join_relations");
                    ObjectNode join = objectMapper.createObjectNode();
                    join.put("label", joinLabel);
                    join.put("accepted_from_candidate_graph", true);
                    joins.add(join);
                }
            }
        }
    }

    private void replaceSceneInTaskPayload(ObjectNode preprocessRoot, int sceneIndex, ObjectNode scenePayload) {
        JsonNode scenesNode = preprocessRoot.path("scenes");
        if (scenesNode.isArray() && sceneIndex < scenesNode.size()) {
            ((ArrayNode) scenesNode).set(sceneIndex, scenePayload);
        }
    }

    private ImportCandidateGraphNodePO requireNode(String taskId, String nodeCode) {
        return nodeMapper.findByTaskIdAndNodeCode(taskId, nodeCode)
                .orElseThrow(() -> new ResourceNotFoundException("candidate node not found: " + nodeCode));
    }

    private ImportCandidateGraphEdgePO requireEdge(String taskId, String edgeCode) {
        return edgeMapper.findByTaskIdAndEdgeCode(taskId, edgeCode)
                .orElseThrow(() -> new ResourceNotFoundException("candidate edge not found: " + edgeCode));
    }

    private String normalizeAction(String action) {
        return action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
    }

    private ImportCandidateReviewEventPO buildReviewEvent(String taskId, String materialId, String graphId,
                                                           String targetType, String targetCode,
                                                           String action, String before, String after,
                                                           ReviewImportCandidateGraphCmd cmd) {
        ImportCandidateReviewEventPO event = new ImportCandidateReviewEventPO();
        event.setTaskId(taskId);
        event.setMaterialId(materialId);
        event.setGraphId(graphId);
        event.setTargetType(targetType);
        event.setTargetCode(targetCode);
        event.setActionType(action);
        event.setBeforeStatus(before);
        event.setAfterStatus(after);
        event.setOperator(cmd.operator());
        event.setReasonText(cmd.reason());
        event.setCreatedAt(OffsetDateTime.now());
        return event;
    }

    private ObjectNode readObject(String json) {
        if (json == null || json.isBlank()) return objectMapper.createObjectNode();
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.isObject() ? (ObjectNode) node : objectMapper.createObjectNode();
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private ObjectNode ensureObject(ObjectNode parent, String field) {
        JsonNode existing = parent.path(field);
        if (existing.isObject()) return (ObjectNode) existing;
        ObjectNode created = objectMapper.createObjectNode();
        parent.set(field, created);
        return created;
    }

    private ArrayNode ensureArray(ObjectNode parent, String field) {
        JsonNode existing = parent.path(field);
        if (existing.isArray()) return (ArrayNode) existing;
        ArrayNode created = objectMapper.createArrayNode();
        parent.set(field, created);
        return created;
    }

    private String write(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
