package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.PreprocessImportCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.CandidateGraphDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphNodeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportTaskDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.PreprocessResultDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportCandidateGraphEdgeMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportCandidateGraphNodeMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportEvidenceCandidateMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportSceneCandidateMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportTaskMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SourceMaterialMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.DictionaryMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.IdentifierLineageMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.TimeSemanticSelectorMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportCandidateGraphEdgePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportCandidateGraphNodePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportEvidenceCandidatePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportSceneCandidatePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportTaskPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SourceMaterialPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.AbstractSnapshotGraphAuditablePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.DictionaryPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.IdentifierLineagePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.TimeSemanticSelectorPO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImportTaskCommandAppService {

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_QUALITY_REVIEWING = "QUALITY_REVIEWING";
    private static final String STATUS_SCENE_REVIEWING = "SCENE_REVIEWING";
    private static final String STATUS_PUBLISHING = "PUBLISHING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    private final ImportTaskMapper importTaskMapper;
    private final SourceMaterialMapper sourceMaterialMapper;
    private final ImportSceneCandidateMapper importSceneCandidateMapper;
    private final ImportEvidenceCandidateMapper importEvidenceCandidateMapper;
    private final ImportCandidateGraphNodeMapper importCandidateGraphNodeMapper;
    private final ImportCandidateGraphEdgeMapper importCandidateGraphEdgeMapper;
    private final ImportCandidateGraphAssembler importCandidateGraphAssembler;
    private final ImportCandidateGraphBuildService importCandidateGraphBuildService;
    private final SceneCommandAppService sceneCommandAppService;
    private final DictionaryMapper dictionaryMapper;
    private final IdentifierLineageMapper identifierLineageMapper;
    private final TimeSemanticSelectorMapper timeSemanticSelectorMapper;
    private final ObjectMapper objectMapper;

    public ImportTaskCommandAppService(ImportTaskMapper importTaskMapper,
                                       SourceMaterialMapper sourceMaterialMapper,
                                       ImportSceneCandidateMapper importSceneCandidateMapper,
                                       ImportEvidenceCandidateMapper importEvidenceCandidateMapper,
                                       ImportCandidateGraphNodeMapper importCandidateGraphNodeMapper,
                                       ImportCandidateGraphEdgeMapper importCandidateGraphEdgeMapper,
                                       ImportCandidateGraphAssembler importCandidateGraphAssembler,
                                       ImportCandidateGraphBuildService importCandidateGraphBuildService,
                                       SceneCommandAppService sceneCommandAppService,
                                       DictionaryMapper dictionaryMapper,
                                       IdentifierLineageMapper identifierLineageMapper,
                                       TimeSemanticSelectorMapper timeSemanticSelectorMapper,
                                       ObjectMapper objectMapper) {
        this.importTaskMapper = importTaskMapper;
        this.sourceMaterialMapper = sourceMaterialMapper;
        this.importSceneCandidateMapper = importSceneCandidateMapper;
        this.importEvidenceCandidateMapper = importEvidenceCandidateMapper;
        this.importCandidateGraphNodeMapper = importCandidateGraphNodeMapper;
        this.importCandidateGraphEdgeMapper = importCandidateGraphEdgeMapper;
        this.importCandidateGraphAssembler = importCandidateGraphAssembler;
        this.importCandidateGraphBuildService = importCandidateGraphBuildService;
        this.sceneCommandAppService = sceneCommandAppService;
        this.dictionaryMapper = dictionaryMapper;
        this.identifierLineageMapper = identifierLineageMapper;
        this.timeSemanticSelectorMapper = timeSemanticSelectorMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportTaskDTO start(String taskId, PreprocessImportCmd cmd) {
        String normalizedTaskId = normalizeTaskId(taskId);
        OffsetDateTime now = OffsetDateTime.now();
        ImportTaskPO po = importTaskMapper.findById(normalizedTaskId).orElseGet(ImportTaskPO::new);
        if (po.getTaskId() == null || po.getTaskId().isBlank()) {
            po.setTaskId(normalizedTaskId);
            po.setCreatedAt(now);
        }
        SourceMaterialPO material = resolveMaterial(po, now);
        material.setSourceType(trimToNull(cmd.sourceType()));
        material.setSourceName(trimToNull(cmd.sourceName()));
        material.setRawText(cmd.rawText());
        material.setOperator(trimToNull(cmd.operator()));
        material.setTextFingerprint(buildFingerprint(cmd));
        material.setUpdatedAt(now);
        sourceMaterialMapper.save(material);

        po.setStatus(STATUS_RUNNING);
        po.setCurrentStep(1);
        po.setSourceType(trimToNull(cmd.sourceType()));
        po.setSourceName(trimToNull(cmd.sourceName()));
        po.setMaterialId(material.getMaterialId());
        po.setOperator(trimToNull(cmd.operator()));
        po.setRawText(cmd.rawText());
        po.setPreprocessResultJson(null);
        po.setQualityConfirmed(false);
        po.setCompareConfirmed(false);
        po.setErrorMessage(null);
        po.setCompletedAt(null);
        po.setUpdatedAt(now);
        return toDTO(importTaskMapper.save(po));
    }

    @Transactional
    public ImportTaskDTO markQualityReviewReady(String taskId, PreprocessResultDTO result) {
        ImportTaskPO po = requireTask(taskId);
        PersistedReviewCandidates persisted = persistReviewCandidates(po, result);
        persistCandidateGraph(po, result, persisted.scenes());
        CandidateGraphDTO baseGraph = result == null ? CandidateGraphDTO.empty() : result.candidateGraph();
        if ((baseGraph == null || (baseGraph.nodes().isEmpty() && baseGraph.edges().isEmpty())) && result != null) {
            baseGraph = importCandidateGraphAssembler.buildSnapshotFromResult(po.getTaskId(), po.getMaterialId(), result);
        }
        CandidateGraphDTO candidateGraph = importCandidateGraphAssembler.enrichWithPersistedEvidence(
                baseGraph,
                persisted.evidences()
        );
        PreprocessResultDTO persistedResult = new PreprocessResultDTO(
                result.caliberImportJson(),
                result.mode(),
                result.global(),
                result.scenes(),
                result.quality(),
                result.warnings(),
                result.confidenceScore(),
                result.confidenceLevel(),
                result.lowConfidence(),
                result.totalElapsedMs(),
                candidateGraph,
                result.stageTimings(),
                result.sceneDrafts(),
                result.importBatchId(),
                result.materialId()
        );
        po.setStatus(STATUS_QUALITY_REVIEWING);
        po.setCurrentStep(Math.max(2, defaultStep(po.getCurrentStep())));
        po.setPreprocessResultJson(writeResultJson(persistedResult));
        po.setQualityConfirmed(false);
        po.setCompareConfirmed(false);
        po.setErrorMessage(null);
        po.setUpdatedAt(OffsetDateTime.now());
        return toDTO(importTaskMapper.save(po));
    }

    @Transactional
    public ImportTaskDTO confirmQuality(String taskId, String operator) {
        ImportTaskPO po = requireTask(taskId);
        po.setQualityConfirmed(true);
        po.setStatus(STATUS_SCENE_REVIEWING);
        po.setCurrentStep(Math.max(3, defaultStep(po.getCurrentStep())));
        po.setOperator(mergeOperator(po.getOperator(), operator));
        po.setUpdatedAt(OffsetDateTime.now());
        return toDTO(importTaskMapper.save(po));
    }

    @Transactional
    public ImportTaskDTO confirmCompare(String taskId, String operator) {
        ImportTaskPO po = requireTask(taskId);
        po.setCompareConfirmed(true);
        po.setStatus(STATUS_PUBLISHING);
        po.setCurrentStep(Math.max(4, defaultStep(po.getCurrentStep())));
        po.setOperator(mergeOperator(po.getOperator(), operator));
        po.setUpdatedAt(OffsetDateTime.now());
        return toDTO(importTaskMapper.save(po));
    }

    @Transactional
    public ImportTaskDTO rewindToStep(String taskId, int step, String operator) {
        if (step < 1 || step > 4) {
            throw new DomainValidationException("step must be between 1 and 4");
        }
        ImportTaskPO po = requireTask(taskId);
        po.setOperator(mergeOperator(po.getOperator(), operator));
        po.setUpdatedAt(OffsetDateTime.now());
        if (step <= 1) {
            po.setStatus(STATUS_RUNNING);
            po.setCurrentStep(1);
            po.setPreprocessResultJson(null);
            po.setQualityConfirmed(false);
            po.setCompareConfirmed(false);
            po.setErrorMessage(null);
            po.setCompletedAt(null);
        } else if (step == 2) {
            po.setStatus(STATUS_QUALITY_REVIEWING);
            po.setCurrentStep(2);
            po.setQualityConfirmed(false);
            po.setCompareConfirmed(false);
            po.setCompletedAt(null);
        } else if (step == 3) {
            po.setStatus(STATUS_SCENE_REVIEWING);
            po.setCurrentStep(3);
            po.setQualityConfirmed(true);
            po.setCompareConfirmed(false);
            po.setCompletedAt(null);
        } else {
            po.setStatus(STATUS_PUBLISHING);
            po.setCurrentStep(4);
            po.setQualityConfirmed(true);
            po.setCompareConfirmed(true);
            po.setCompletedAt(null);
        }
        return toDTO(importTaskMapper.save(po));
    }

    @Transactional
    public ImportTaskDTO complete(String taskId, String operator) {
        ImportTaskPO po = requireTask(taskId);
        po.setStatus(STATUS_COMPLETED);
        po.setCurrentStep(4);
        po.setQualityConfirmed(true);
        po.setCompareConfirmed(true);
        po.setOperator(mergeOperator(po.getOperator(), operator));
        OffsetDateTime now = OffsetDateTime.now();
        po.setUpdatedAt(now);
        po.setCompletedAt(now);
        return toDTO(importTaskMapper.save(po));
    }

    @Transactional
    public SceneDTO confirmSceneCandidate(String candidateCode, Long domainId, String domain, String operator) {
        ImportSceneCandidatePO candidate = importSceneCandidateMapper.findByCandidateCode(candidateCode)
                .orElseThrow(() -> new ResourceNotFoundException("scene candidate not found: " + candidateCode));
        JsonNode scene = readJson(candidate.getCandidatePayloadJson());
        String sceneTitle = firstNonBlank(
                text(scene.path("scene_title")),
                text(scene.path("scene_code_guess")),
                candidate.getSceneTitle(),
                "未命名场景"
        );
        String resolvedOperator = mergeOperator(null, operator);
        SceneDTO created = sceneCommandAppService.create(new CreateSceneCmd(
                sceneTitle,
                domainId,
                trimToEmpty(domain),
                "FACT_DETAIL",
                resolveRawInput(candidate.getTaskId(), scene),
                resolvedOperator
        ));

        String sqlVariantsJson = writeJson(scene.path("sql_variants"), "[]");
        SceneDTO updated = sceneCommandAppService.update(created.id(), new UpdateSceneCmd(
                sceneTitle,
                domainId,
                trimToEmpty(domain),
                firstNonBlank(text(scene.path("scene_type")), "FACT_DETAIL"),
                trimToEmpty(text(scene.path("scene_description"))),
                trimToEmpty(text(scene.path("caliber_definition"))),
                trimToEmpty(text(scene.path("applicability"))),
                trimToEmpty(text(scene.path("boundaries"))),
                writeJson(scene.path("inputs"), "{}"),
                writeJson(scene.path("outputs"), "{}"),
                sqlVariantsJson,
                writeJson(scene.path("code_mappings"), "[]"),
                joinTextArray(scene.path("contributors")),
                sqlVariantsJson,
                writeJson(extractSourceTables(scene.path("sql_variants")), "[]"),
                writeJson(scene.path("caveats"), "[]"),
                trimToEmpty(text(scene.path("unmapped_text"))),
                writeJson(scene.path("quality"), "{}"),
                resolveRawInput(candidate.getTaskId(), scene),
                null,
                resolvedOperator
        ));

        candidate.setConfirmationStatus("CONFIRMED");
        candidate.setUpdatedAt(OffsetDateTime.now());
        importSceneCandidateMapper.save(candidate);
        seedRequiredGovernanceAssets(updated);
        return updated;
    }

    @Transactional
    public void markFailed(String taskId, String errorMessage) {
        if (taskId == null || taskId.isBlank()) {
            return;
        }
        Optional<ImportTaskPO> optional = importTaskMapper.findById(taskId.trim());
        if (optional.isEmpty()) {
            return;
        }
        ImportTaskPO po = optional.get();
        po.setStatus(STATUS_FAILED);
        po.setErrorMessage(trimToNull(errorMessage));
        po.setUpdatedAt(OffsetDateTime.now());
        po.setCurrentStep(Math.max(1, defaultStep(po.getCurrentStep())));
        importTaskMapper.save(po);
    }

    private ImportTaskPO requireTask(String taskId) {
        String normalizedTaskId = normalizeTaskId(taskId);
        return importTaskMapper.findById(normalizedTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("import task not found: " + normalizedTaskId));
    }

    private int defaultStep(Integer step) {
        return step == null || step <= 0 ? 1 : step;
    }

    private SourceMaterialPO resolveMaterial(ImportTaskPO po, OffsetDateTime now) {
        String existingMaterialId = trimToNull(po.getMaterialId());
        if (existingMaterialId != null) {
            Optional<SourceMaterialPO> existing = sourceMaterialMapper.findById(existingMaterialId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        SourceMaterialPO material = new SourceMaterialPO();
        material.setMaterialId(existingMaterialId == null ? UUID.randomUUID().toString() : existingMaterialId);
        material.setCreatedAt(now);
        return material;
    }

    private String writeResultJson(PreprocessResultDTO result) {
        if (result == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception ex) {
            return null;
        }
    }

    private PersistedReviewCandidates persistReviewCandidates(ImportTaskPO task, PreprocessResultDTO result) {
        String materialId = trimToNull(task.getMaterialId());
        if (result == null || materialId == null) {
            return new PersistedReviewCandidates(List.of(), List.of());
        }
        importEvidenceCandidateMapper.deleteByTaskId(task.getTaskId());
        importSceneCandidateMapper.deleteByTaskId(task.getTaskId());

        OffsetDateTime now = OffsetDateTime.now();
        List<JsonNode> scenes = result.scenes() == null ? List.of() : result.scenes();
        List<ImportSceneCandidatePO> sceneCandidates = new ArrayList<>();
        List<ImportEvidenceCandidatePO> evidenceCandidates = new ArrayList<>();
        for (int i = 0; i < scenes.size(); i++) {
            JsonNode scene = scenes.get(i);
            String sceneTitle = firstNonBlank(
                    text(scene.path("scene_title")),
                    text(scene.path("scene_code_guess")),
                    "未命名场景" + (i + 1)
            );
            String sceneCode = buildSceneCandidateCode(task.getTaskId(), i);

            ImportSceneCandidatePO sceneCandidate = new ImportSceneCandidatePO();
            sceneCandidate.setTaskId(task.getTaskId());
            sceneCandidate.setMaterialId(materialId);
            sceneCandidate.setCandidateCode(sceneCode);
            sceneCandidate.setSceneIndex(i);
            sceneCandidate.setSceneTitle(sceneTitle);
            sceneCandidate.setSceneDescription(firstNonBlank(
                    text(scene.path("scene_description")),
                    text(scene.path("caliber_definition")),
                    text(scene.path("applicability"))
            ));
            sceneCandidate.setCandidatePayloadJson(writeSceneJson(scene));
            sceneCandidate.setConfidenceScore(readConfidence(scene, result));
            sceneCandidate.setConfirmationStatus("PENDING_CONFIRMATION");
            sceneCandidate.setCreatedAt(now);
            sceneCandidate.setUpdatedAt(now);
            sceneCandidates.add(sceneCandidate);

            evidenceCandidates.add(buildEvidenceCandidate(task.getTaskId(), materialId, sceneCode, i, sceneTitle, scene, now));
        }

        List<ImportSceneCandidatePO> persistedScenes = importSceneCandidateMapper.saveAll(sceneCandidates);
        List<ImportEvidenceCandidatePO> persistedEvidences = importEvidenceCandidateMapper.saveAll(evidenceCandidates);
        return new PersistedReviewCandidates(persistedScenes, persistedEvidences);
    }

    private void persistCandidateGraph(ImportTaskPO task,
                                       PreprocessResultDTO result,
                                       List<ImportSceneCandidatePO> persistedScenes) {
        importCandidateGraphNodeMapper.deleteByTaskId(task.getTaskId());
        importCandidateGraphEdgeMapper.deleteByTaskId(task.getTaskId());
        if (result == null || task.getMaterialId() == null || task.getMaterialId().isBlank()) {
            return;
        }

        ImportCandidateGraphDTO candidateGraph = importCandidateGraphBuildService.build(
                task.getTaskId(),
                task.getMaterialId(),
                result
        );
        OffsetDateTime now = OffsetDateTime.now();
        List<ImportCandidateGraphNodePO> nodePOs = candidateGraph.nodes().stream()
                .map(node -> toNodePO(task, persistedScenes, candidateGraph.graphId(), node, now))
                .toList();
        List<ImportCandidateGraphEdgePO> edgePOs = candidateGraph.edges().stream()
                .map(edge -> toEdgePO(task, persistedScenes, candidateGraph.graphId(), edge, now))
                .toList();
        importCandidateGraphNodeMapper.saveAll(nodePOs);
        importCandidateGraphEdgeMapper.saveAll(edgePOs);
    }

    private ImportCandidateGraphNodePO toNodePO(ImportTaskPO task,
                                                List<ImportSceneCandidatePO> persistedScenes,
                                                String graphId,
                                                ImportCandidateGraphNodeDTO node,
                                                OffsetDateTime now) {
        ImportCandidateGraphNodePO po = new ImportCandidateGraphNodePO();
        po.setTaskId(task.getTaskId());
        po.setMaterialId(task.getMaterialId());
        po.setGraphId(graphId);
        po.setNodeCode(node.nodeCode());
        po.setSceneCandidateCode(resolveSceneCandidateCode(node.payload(), persistedScenes, node.sceneCandidateCode()));
        po.setNodeType(node.nodeType());
        po.setNodeLabel(node.label());
        po.setCanonicalNodeCode(null);
        po.setReviewStatus(node.reviewStatus());
        po.setRiskLevel(node.riskLevel());
        po.setConfidenceScore(node.confidenceScore());
        po.setPayloadJson(writeJson(node.payload(), "{}"));
        po.setCreatedAt(now);
        po.setUpdatedAt(now);
        return po;
    }

    private ImportCandidateGraphEdgePO toEdgePO(ImportTaskPO task,
                                                List<ImportSceneCandidatePO> persistedScenes,
                                                String graphId,
                                                ImportCandidateGraphEdgeDTO edge,
                                                OffsetDateTime now) {
        ImportCandidateGraphEdgePO po = new ImportCandidateGraphEdgePO();
        po.setTaskId(task.getTaskId());
        po.setMaterialId(task.getMaterialId());
        po.setGraphId(graphId);
        po.setEdgeCode(edge.edgeCode());
        po.setSceneCandidateCode(resolveSceneCandidateCode(edge.payload(), persistedScenes, edge.sceneCandidateCode()));
        po.setEdgeType(edge.edgeType());
        po.setSourceNodeCode(edge.sourceNodeCode());
        po.setTargetNodeCode(edge.targetNodeCode());
        po.setEdgeLabel(edge.label());
        po.setReviewStatus(edge.reviewStatus());
        po.setRiskLevel(edge.riskLevel());
        po.setConfidenceScore(edge.confidenceScore());
        po.setPayloadJson(writeJson(edge.payload(), "{}"));
        po.setCreatedAt(now);
        po.setUpdatedAt(now);
        return po;
    }

    private String resolveSceneCandidateCode(JsonNode payload,
                                             List<ImportSceneCandidatePO> persistedScenes,
                                             String fallback) {
        if (payload != null && payload.has("sceneIndex")) {
            int sceneIndex = payload.path("sceneIndex").asInt(-1);
            if (sceneIndex >= 0 && sceneIndex < persistedScenes.size()) {
                return persistedScenes.get(sceneIndex).getCandidateCode();
            }
        }
        return fallback;
    }

    private ImportEvidenceCandidatePO buildEvidenceCandidate(String taskId,
                                                             String materialId,
                                                             String sceneCandidateCode,
                                                             int sceneIndex,
                                                             String sceneTitle,
                                                             JsonNode scene,
                                                             OffsetDateTime now) {
        ImportEvidenceCandidatePO evidence = new ImportEvidenceCandidatePO();
        evidence.setTaskId(taskId);
        evidence.setMaterialId(materialId);
        evidence.setCandidateCode(buildEvidenceCandidateCode(taskId, sceneIndex));
        evidence.setSceneCandidateCode(sceneCandidateCode);
        evidence.setEvidenceType(resolveEvidenceType(scene));
        evidence.setAnchorLabel(resolveAnchorLabel(sceneTitle, scene));
        evidence.setQuoteText(resolveQuoteText(scene));
        evidence.setLineStart(readFirstLine(scene.path("source_evidence_lines")));
        evidence.setLineEnd(readLastLine(scene.path("source_evidence_lines")));
        evidence.setConfirmationStatus("PENDING_CONFIRMATION");
        evidence.setCreatedAt(now);
        evidence.setUpdatedAt(now);
        return evidence;
    }

    private String resolveEvidenceType(JsonNode scene) {
        if (scene.path("sql_variants").isArray() && !scene.path("sql_variants").isEmpty()) {
            return "SQL_VARIANT";
        }
        if (scene.path("outputs").path("fields").isArray() && !scene.path("outputs").path("fields").isEmpty()) {
            return "OUTPUT_FIELD";
        }
        return "TEXT_FRAGMENT";
    }

    private String resolveAnchorLabel(String sceneTitle, JsonNode scene) {
        Integer firstLine = readFirstLine(scene.path("source_evidence_lines"));
        Integer lastLine = readLastLine(scene.path("source_evidence_lines"));
        if (firstLine != null && lastLine != null) {
            return sceneTitle + " 原文行 " + firstLine + "-" + lastLine;
        }
        String variantName = text(scene.path("sql_variants").path(0).path("variant_name"));
        if (variantName != null) {
            return sceneTitle + " / " + variantName;
        }
        return sceneTitle + " 解析证据";
    }

    private String resolveQuoteText(JsonNode scene) {
        return firstNonBlank(
                text(scene.path("sql_variants").path(0).path("sql_text")),
                text(scene.path("scene_description")),
                text(scene.path("caliber_definition")),
                text(scene.path("outputs").path("summary")),
                text(scene.path("unmapped_text"))
        );
    }

    private Double readConfidence(JsonNode scene, PreprocessResultDTO result) {
        if (scene != null && scene.path("quality").hasNonNull("confidence")) {
            return scene.path("quality").path("confidence").asDouble();
        }
        return result.confidenceScore();
    }

    private Integer readFirstLine(JsonNode lines) {
        if (!lines.isArray() || lines.isEmpty()) {
            return null;
        }
        return lines.get(0).canConvertToInt() ? lines.get(0).asInt() : null;
    }

    private Integer readLastLine(JsonNode lines) {
        if (!lines.isArray() || lines.isEmpty()) {
            return null;
        }
        JsonNode last = lines.get(lines.size() - 1);
        return last.canConvertToInt() ? last.asInt() : null;
    }

    private String writeSceneJson(JsonNode scene) {
        try {
            return objectMapper.writeValueAsString(scene);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String writeJson(JsonNode node, String fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String buildSceneCandidateCode(String taskId, int sceneIndex) {
        return abbreviate("SCN-" + compactTaskId(taskId) + "-" + String.format(Locale.ROOT, "%02d", sceneIndex + 1), 64);
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private String resolveRawInput(String taskId, JsonNode scene) {
        ImportTaskPO task = requireTask(taskId);
        String rawInput = trimToNull(task.getRawText());
        if (rawInput != null) {
            return rawInput;
        }
        return trimToEmpty(text(scene.path("unmapped_text")));
    }

    private String joinTextArray(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return "";
        }
        List<String> items = new ArrayList<>();
        node.forEach(item -> {
            String value = trimToNull(item.asText());
            if (value != null) {
                items.add(value);
            }
        });
        return String.join(",", items);
    }

    private JsonNode extractSourceTables(JsonNode sqlVariants) {
        if (sqlVariants == null || !sqlVariants.isArray()) {
            return objectMapper.createArrayNode();
        }
        var result = objectMapper.createArrayNode();
        for (JsonNode variant : sqlVariants) {
            JsonNode sourceTables = variant.path("source_tables");
            if (!sourceTables.isArray()) {
                continue;
            }
            sourceTables.forEach(table -> {
                String value = trimToNull(table.asText());
                if (value != null && !containsText(result, value)) {
                    result.add(value);
                }
            });
        }
        return result;
    }

    private boolean containsText(JsonNode array, String expected) {
        if (array == null || !array.isArray()) {
            return false;
        }
        for (JsonNode item : array) {
            if (expected.equals(item.asText())) {
                return true;
            }
        }
        return false;
    }

    private void seedRequiredGovernanceAssets(SceneDTO scene) {
        if (scene == null || scene.id() == null) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();

        if (dictionaryMapper.findBySceneIdOrderByUpdatedAtDesc(scene.id()).isEmpty()) {
            DictionaryPO dictionary = new DictionaryPO();
            dictionary.setSceneId(scene.id());
            dictionary.setDictCode("DICT-" + scene.id() + "-MAIN");
            dictionary.setDictName(scene.sceneTitle() + "字典");
            dictionary.setDictCategory("ENUM");
            dictionary.setDictVersion("v1");
            dictionary.setReleaseStatus("PUBLISHED");
            dictionary.setEntriesJson("[{\"code\":\"DEFAULT\",\"name\":\"默认值\"}]");
            dictionary.setReferencedByJson("[\"scene:" + scene.id() + "\"]");
            stamp(dictionary, now);
            dictionaryMapper.save(dictionary);
        }

        if (identifierLineageMapper.findBySceneIdOrderByUpdatedAtDesc(scene.id()).isEmpty()) {
            IdentifierLineagePO lineage = new IdentifierLineagePO();
            lineage.setSceneId(scene.id());
            lineage.setLineageCode("LIN-" + scene.id() + "-MAIN");
            lineage.setLineageName(scene.sceneTitle() + "标识链");
            lineage.setIdentifierType("SCENE_IDENTIFIER");
            lineage.setSourceIdentifierType("SRC_IDENTIFIER");
            lineage.setTargetIdentifierType("SCENE_IDENTIFIER");
            lineage.setMappingRulesJson("[{\"rule\":\"direct-pass-through\"}]");
            lineage.setEvidenceRefsJson("[\"scene:" + scene.id() + "\"]");
            lineage.setConfirmationStatus("CONFIRMED");
            stamp(lineage, now);
            identifierLineageMapper.save(lineage);
        }

        if (timeSemanticSelectorMapper.findBySceneIdOrderByUpdatedAtDesc(scene.id()).isEmpty()) {
            TimeSemanticSelectorPO selector = new TimeSemanticSelectorPO();
            selector.setSceneId(scene.id());
            selector.setSelectorCode("TIME-" + scene.id() + "-MAIN");
            selector.setSelectorName(scene.sceneTitle() + "时间语义");
            selector.setDefaultSemantic(extractDefaultTimeSemantic(scene.sqlVariantsJson()));
            selector.setCandidateSemanticsJson("[\"TRX_DT\",\"交易日期\"]");
            selector.setClarificationTermsJson("[\"当日\",\"时间范围\"]");
            selector.setPriorityRulesJson("[{\"priority\":1,\"semantic\":\"TRX_DT\"}]");
            selector.setMustClarifyFlag(false);
            stamp(selector, now);
            timeSemanticSelectorMapper.save(selector);
        }
    }

    private void stamp(AbstractSnapshotGraphAuditablePO po, OffsetDateTime now) {
        po.setStatus("ACTIVE");
        po.setSnapshotId(null);
        po.setCreatedBy("system");
        po.setCreatedAt(now);
        po.setUpdatedBy("system");
        po.setUpdatedAt(now);
    }

    private String extractDefaultTimeSemantic(String sqlVariantsJson) {
        JsonNode variants = readJson(sqlVariantsJson);
        if (variants.isArray()) {
            for (JsonNode variant : variants) {
                String semantic = trimToNull(variant.path("default_time_semantic").asText());
                if (semantic != null) {
                    return semantic;
                }
            }
        }
        return "TRX_DT";
    }

    private String buildEvidenceCandidateCode(String taskId, int sceneIndex) {
        return abbreviate("EVD-" + compactTaskId(taskId) + "-" + String.format(Locale.ROOT, "%02d", sceneIndex + 1), 64);
    }

    private String compactTaskId(String taskId) {
        String normalized = taskId == null ? "" : taskId.replace("-", "").trim().toUpperCase(Locale.ROOT);
        return normalized.length() <= 16 ? normalized : normalized.substring(0, 16);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String buildFingerprint(PreprocessImportCmd cmd) {
        String payload = String.join("|",
                trimToEmpty(cmd.sourceType()),
                trimToEmpty(cmd.sourceName()),
                trimToEmpty(cmd.rawText()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to build source material fingerprint", ex);
        }
    }

    private String normalizeTaskId(String taskId) {
        String normalized = taskId == null ? "" : taskId.trim();
        if (normalized.isBlank()) {
            throw new DomainValidationException("taskId must not be blank");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return trimToNull(node.asText());
    }

    private String trimToEmpty(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "" : normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String mergeOperator(String currentOperator, String nextOperator) {
        String normalizedNext = trimToNull(nextOperator);
        if (normalizedNext != null) {
            return normalizedNext;
        }
        return trimToNull(currentOperator);
    }

    private ImportTaskDTO toDTO(ImportTaskPO po) {
        JsonNode preprocessResult = null;
        if (po.getPreprocessResultJson() != null && !po.getPreprocessResultJson().isBlank()) {
            try {
                preprocessResult = objectMapper.readTree(po.getPreprocessResultJson());
            } catch (Exception ignore) {
                // ignore parse failure
            }
        }
        String status = po.getStatus() == null ? null : po.getStatus().toUpperCase(Locale.ROOT);
        return new ImportTaskDTO(
                po.getTaskId(),
                po.getMaterialId(),
                status,
                po.getCurrentStep(),
                po.getSourceType(),
                po.getSourceName(),
                po.getOperator(),
                po.getRawText(),
                Boolean.TRUE.equals(po.getQualityConfirmed()),
                Boolean.TRUE.equals(po.getCompareConfirmed()),
                preprocessResult,
                po.getErrorMessage(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getCompletedAt()
        );
    }

    private record PersistedReviewCandidates(List<ImportSceneCandidatePO> scenes,
                                             List<ImportEvidenceCandidatePO> evidences) {
    }
}
