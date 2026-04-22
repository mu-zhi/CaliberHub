package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.response.CandidateGraphDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportGraphPatchDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.PreprocessImportCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.PreprocessResultDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.PreprocessSceneDraftDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.StageTimingDTO;
import com.cmbchina.datadirect.caliber.application.support.LlmPreprocessSupport;
import com.cmbchina.datadirect.caliber.application.support.PreprocessExperimentSupport;
import com.cmbchina.datadirect.caliber.infrastructure.module.supportimpl.application.LightRagPreprocessExperimentSupportImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
public class ImportCommandAppService {

    private static final int CHUNK_TARGET_SIZE = 6000;
    private static final int CHUNK_MAX_SIZE = 8000;
    private static final int CHUNK_PARALLELISM = 3;
    private static final int MAX_INPUT_LINES = 10_000;
    private static final String PREPROCESS_MODE_AUTO = "AUTO";
    private static final String PREPROCESS_MODE_RULE_ONLY = "RULE_ONLY";
    private static final String PREPROCESS_MODE_LLM_ONLY = "LLM_ONLY";

    private static final String STAGE_SPLIT = "split";
    private static final String STAGE_EXTRACT = "extract";
    private static final String STAGE_MERGE = "merge";
    private static final String STAGE_NORMALIZE = "normalize";
    private static final String STAGE_DRAFT_PERSIST = "draft_persist";
    private static final String STAGE_FINALIZE = "finalize";

    private final LlmPreprocessSupport llmPreprocessSupport;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final SceneCommandAppService sceneCommandAppService;
    private final ImportTaskCommandAppService importTaskCommandAppService;
    private final ImportCandidateGraphAssembler importCandidateGraphAssembler;
    private final PreprocessExperimentSupport preprocessExperimentSupport;

    public ImportCommandAppService(LlmPreprocessSupport llmPreprocessSupport,
                                   ObjectMapper objectMapper,
                                   MeterRegistry meterRegistry) {
        this(llmPreprocessSupport, objectMapper, meterRegistry, null, null, new ImportCandidateGraphAssembler(), new LightRagPreprocessExperimentSupportImpl(objectMapper));
    }

    public ImportCommandAppService(LlmPreprocessSupport llmPreprocessSupport,
                                   ObjectMapper objectMapper,
                                   MeterRegistry meterRegistry,
                                   SceneCommandAppService sceneCommandAppService) {
        this(llmPreprocessSupport, objectMapper, meterRegistry, sceneCommandAppService, null, new ImportCandidateGraphAssembler(), new LightRagPreprocessExperimentSupportImpl(objectMapper));
    }

    @Autowired
    public ImportCommandAppService(LlmPreprocessSupport llmPreprocessSupport,
                                   ObjectMapper objectMapper,
                                   MeterRegistry meterRegistry,
                                   SceneCommandAppService sceneCommandAppService,
                                   ImportTaskCommandAppService importTaskCommandAppService,
                                   ImportCandidateGraphAssembler importCandidateGraphAssembler,
                                   PreprocessExperimentSupport preprocessExperimentSupport) {
        this.llmPreprocessSupport = llmPreprocessSupport;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.sceneCommandAppService = sceneCommandAppService;
        this.importTaskCommandAppService = importTaskCommandAppService;
        this.importCandidateGraphAssembler = importCandidateGraphAssembler;
        this.preprocessExperimentSupport = preprocessExperimentSupport == null
                ? new LightRagPreprocessExperimentSupportImpl(objectMapper)
                : preprocessExperimentSupport;
    }

    public PreprocessResultDTO preprocess(PreprocessImportCmd cmd) {
        return preprocess(cmd, null);
    }

    public PreprocessResultDTO preprocess(PreprocessImportCmd cmd, String importBatchId) {
        return preprocessChunked(cmd, importBatchId, stage -> {
            // no-op for non-stream endpoint
        }, draft -> {
            // no-op for non-stream endpoint
        }, graphPatch -> {
            // no-op for non-stream endpoint
        });
    }

    public PreprocessResultDTO preprocessByLlm(PreprocessImportCmd cmd) {
        return preprocessByLlm(cmd, null);
    }

    public PreprocessResultDTO preprocessByLlm(PreprocessImportCmd cmd, String importBatchIdInput) {
        enforceInputLineLimit(cmd.rawText());
        long totalStart = now();
        String importBatchId = normalizeImportBatchId(importBatchIdInput);
        String materialId = trackTaskStart(importBatchId, cmd);
        List<StageTimingDTO> stageTimings = new ArrayList<>();
        List<PreprocessSceneDraftDTO> sceneDrafts = new ArrayList<>();
        Consumer<StageTimingDTO> stageConsumer = stage -> {
            // no-op
        };
        Consumer<PreprocessSceneDraftDTO> draftConsumer = scene -> {
            // no-op
        };

        try {
            long splitStart = now();
            StageTimingDTO splitStage = stage(
                    STAGE_SPLIT,
                    "文档分块",
                    elapsed(splitStart),
                    5,
                    1,
                    1,
                    "文档分块完成（单文档）"
            );
            stageTimings.add(splitStage);
            stageConsumer.accept(splitStage);

            long extractStart = now();
            PreprocessResultDTO base = measured("llm_force", () -> llmPreprocessSupport.preprocessToCaliberImportV2ByLlm(
                    cmd.rawText(), cmd.sourceType(), cmd.sourceName()
            ));
            base = attachPreprocessExperiment(base, importBatchId, materialId);
            StageTimingDTO extractStage = stage(
                    STAGE_EXTRACT,
                    "抽取识别",
                    elapsed(extractStart),
                    70,
                    1,
                    1,
                    "大模型抽取完成"
            );
            stageTimings.add(extractStage);
            stageConsumer.accept(extractStage);

            long mergeStart = now();
            StageTimingDTO mergeStage = stage(
                    STAGE_MERGE,
                    "结果合并",
                    elapsed(mergeStart),
                    80,
                    1,
                    1,
                    "单文档无需合并"
            );
            stageTimings.add(mergeStage);
            stageConsumer.accept(mergeStage);

            long normalizeStart = now();
            StageTimingDTO normalizeStage = stage(
                    STAGE_NORMALIZE,
                    "结果归一",
                    elapsed(normalizeStart),
                    90,
                    1,
                    1,
                    "结果归一完成"
            );
            stageTimings.add(normalizeStage);
            stageConsumer.accept(normalizeStage);

            long draftStart = now();
            if (Boolean.TRUE.equals(cmd.autoCreateDrafts())) {
                sceneDrafts.addAll(persistSceneDrafts(base, cmd, draftConsumer));
            }
            StageTimingDTO draftStage = stage(
                    STAGE_DRAFT_PERSIST,
                    "草稿入库",
                    elapsed(draftStart),
                    95,
                    sceneDrafts.size(),
                    base.scenes() == null ? 0 : base.scenes().size(),
                    Boolean.TRUE.equals(cmd.autoCreateDrafts()) ? "草稿预创建完成" : "未启用草稿预创建"
            );
            stageTimings.add(draftStage);
            stageConsumer.accept(draftStage);

            StageTimingDTO finalizeStage = stage(
                    STAGE_FINALIZE,
                    "导入完成",
                    elapsed(totalStart),
                    100,
                    sceneDrafts.size(),
                    base.scenes() == null ? 0 : base.scenes().size(),
                    "导入流程完成"
            );
            stageTimings.add(finalizeStage);
            stageConsumer.accept(finalizeStage);

            PreprocessResultDTO result = decorateResult(base, stageTimings, sceneDrafts, importBatchId, materialId, elapsed(totalStart));
            trackTaskReady(importBatchId, result);
            return result;
        } catch (RuntimeException ex) {
            trackTaskFailed(importBatchId, ex.getMessage());
            throw ex;
        }
    }

    public PreprocessResultDTO preprocessChunked(PreprocessImportCmd cmd,
                                                 Consumer<StageTimingDTO> stageConsumer,
                                                 Consumer<PreprocessSceneDraftDTO> draftConsumer) {
        return preprocessChunked(cmd, null, stageConsumer, draftConsumer, graphPatch -> {
            // no-op
        });
    }

    public PreprocessResultDTO preprocessChunked(PreprocessImportCmd cmd,
                                                 String importBatchIdInput,
                                                 Consumer<StageTimingDTO> stageConsumer,
                                                 Consumer<PreprocessSceneDraftDTO> draftConsumer) {
        return preprocessChunked(cmd, importBatchIdInput, stageConsumer, draftConsumer, graphPatch -> {
            // no-op
        });
    }

    public PreprocessResultDTO preprocessChunked(PreprocessImportCmd cmd,
                                                 String importBatchIdInput,
                                                 Consumer<StageTimingDTO> stageConsumer,
                                                 Consumer<PreprocessSceneDraftDTO> draftConsumer,
                                                 Consumer<ImportGraphPatchDTO> graphPatchConsumer) {
        enforceInputLineLimit(cmd.rawText());
        long totalStart = now();
        String importBatchId = normalizeImportBatchId(importBatchIdInput);
        String materialId = trackTaskStart(importBatchId, cmd);
        List<StageTimingDTO> stageTimings = new ArrayList<>();
        List<PreprocessSceneDraftDTO> sceneDrafts = new ArrayList<>();
        int graphPatchSeq = 0;
        CandidateGraphDTO previousGraph = null;
        try {

            long splitStart = now();
            List<String> chunks = splitRawText(cmd.rawText());
            StageTimingDTO splitStage = stage(
                    STAGE_SPLIT,
                    "文档分块",
                    elapsed(splitStart),
                    5,
                    0,
                    chunks.size(),
                    "文档分块完成"
            );
            stageTimings.add(splitStage);
            stageConsumer.accept(splitStage);

            PreprocessResultDTO base;
            long extractStart = now();
            if (chunks.size() == 1) {
                base = preprocessDirect(cmd);
                base = attachPreprocessExperiment(base, importBatchId, materialId);
                StageTimingDTO extractStage = stage(
                        STAGE_EXTRACT,
                        "抽取识别",
                        elapsed(extractStart),
                        70,
                        1,
                        1,
                        "抽取完成"
                );
                stageTimings.add(extractStage);
                stageConsumer.accept(extractStage);
            } else {
                ExecutorService executorService = Executors.newFixedThreadPool(Math.max(1, Math.min(CHUNK_PARALLELISM, chunks.size())));
                CompletionService<ChunkTaskResult> completionService = new ExecutorCompletionService<>(executorService);
                List<PreprocessResultDTO> chunkResults = new ArrayList<>(Collections.nCopies(chunks.size(), null));
                int submittedCount = 0;
                try {
                    for (int i = 0; i < chunks.size(); i++) {
                        int chunkIndex = i + 1;
                        String chunkSourceName = appendChunkSuffix(cmd.sourceName(), chunkIndex, chunks.size());
                        PreprocessImportCmd chunkCmd = new PreprocessImportCmd(
                                chunks.get(i),
                                cmd.sourceType(),
                                chunkSourceName,
                                cmd.preprocessMode(),
                                false,
                                cmd.operator()
                        );
                        completionService.submit(() -> new ChunkTaskResult(chunkIndex, preprocessDirect(chunkCmd)));
                        submittedCount += 1;
                    }

                    for (int completed = 1; completed <= submittedCount; completed++) {
                        Future<ChunkTaskResult> future = completionService.take();
                        ChunkTaskResult completedResult = future.get();
                        chunkResults.set(completedResult.chunkIndex() - 1, completedResult.result());
                        int percent = Math.min(75, Math.max(10, completed * 65 / chunks.size() + 10));
                        stageConsumer.accept(stage(
                                STAGE_EXTRACT,
                                "抽取识别",
                                elapsed(extractStart),
                                percent,
                                completed,
                                chunks.size(),
                                "分块抽取完成 " + completed + "/" + chunks.size()
                        ));
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("分块预处理被中断", ex);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    throw new IllegalStateException("分块预处理失败", cause);
                } finally {
                    executorService.shutdownNow();
                }
                StageTimingDTO extractStage = stage(
                        STAGE_EXTRACT,
                        "抽取识别",
                        elapsed(extractStart),
                        75,
                        chunks.size(),
                        chunks.size(),
                        "分块抽取完成"
                );
                stageTimings.add(extractStage);
                stageConsumer.accept(extractStage);

                long mergeStart = now();
                base = mergeChunkResults(chunkResults);
                base = attachPreprocessExperiment(base, importBatchId, materialId);
                StageTimingDTO mergeStage = stage(
                        STAGE_MERGE,
                        "结果合并",
                        elapsed(mergeStart),
                        85,
                        chunks.size(),
                        chunks.size(),
                        "分块结果合并完成"
                );
                stageTimings.add(mergeStage);
                stageConsumer.accept(mergeStage);
                previousGraph = emitGraphPatchIfEnabled(
                        graphPatchConsumer,
                        importBatchId,
                        materialId,
                        base,
                        previousGraph,
                        STAGE_MERGE,
                        ++graphPatchSeq,
                        chunks.size(),
                        chunkResults.size(),
                        "merge阶段已输出候选实体图谱增量"
                );
            }

            if (chunks.size() == 1) {
                long mergeStart = now();
                StageTimingDTO mergeStage = stage(
                        STAGE_MERGE,
                        "结果合并",
                        elapsed(mergeStart),
                        85,
                        1,
                        1,
                        "单文档无需合并"
                );
                stageTimings.add(mergeStage);
                stageConsumer.accept(mergeStage);
            }

            long normalizeStart = now();
            StageTimingDTO normalizeStage = stage(
                    STAGE_NORMALIZE,
                    "结果归一",
                    elapsed(normalizeStart),
                    90,
                    chunks.size(),
                    chunks.size(),
                    "结果归一完成"
            );
            stageTimings.add(normalizeStage);
            stageConsumer.accept(normalizeStage);

            CandidateGraphDTO liveGraph = importCandidateGraphAssembler.buildSnapshotFromResult(importBatchId, materialId, base);
            if (graphPatchConsumer != null && (!liveGraph.nodes().isEmpty() || !liveGraph.edges().isEmpty())) {
                graphPatchConsumer.accept(buildGraphPatch(liveGraph, ++graphPatchSeq, normalizeStage, "候选实体图谱已更新"));
                previousGraph = liveGraph;
            }

            long draftStart = now();
            if (Boolean.TRUE.equals(cmd.autoCreateDrafts())) {
                sceneDrafts.addAll(persistSceneDrafts(base, cmd, draftConsumer));
            }
            StageTimingDTO draftStage = stage(
                    STAGE_DRAFT_PERSIST,
                    "草稿入库",
                    elapsed(draftStart),
                    95,
                    sceneDrafts.size(),
                    base.scenes() == null ? 0 : base.scenes().size(),
                    Boolean.TRUE.equals(cmd.autoCreateDrafts()) ? "草稿预创建完成" : "未启用草稿预创建"
            );
            stageTimings.add(draftStage);
            stageConsumer.accept(draftStage);

            StageTimingDTO finalizeStage = stage(
                    STAGE_FINALIZE,
                    "导入完成",
                    elapsed(totalStart),
                    100,
                    sceneDrafts.size(),
                    base.scenes() == null ? 0 : base.scenes().size(),
                    "导入流程完成"
            );
            stageTimings.add(finalizeStage);
            stageConsumer.accept(finalizeStage);

            PreprocessResultDTO result = decorateResult(base, stageTimings, sceneDrafts, importBatchId, materialId, elapsed(totalStart));
            trackTaskReady(importBatchId, result);
            previousGraph = emitGraphPatchIfEnabled(
                    graphPatchConsumer,
                    importBatchId,
                    materialId,
                    result,
                    previousGraph,
                    STAGE_FINALIZE,
                    ++graphPatchSeq,
                    1,
                    1,
                    "导入完成时输出候选实体图谱快照"
            );
            return result;
        } catch (RuntimeException ex) {
            trackTaskFailed(importBatchId, ex.getMessage());
            throw ex;
        }
    }

    private CandidateGraphDTO emitGraphPatchIfEnabled(Consumer<ImportGraphPatchDTO> graphPatchConsumer,
                                                      String taskId,
                                                      String materialId,
                                                      PreprocessResultDTO base,
                                                      CandidateGraphDTO previousGraph,
                                                      String stageKey,
                                                      int patchSeq,
                                                      int chunkIndex,
                                                      int chunkTotal,
                                                      String summary) {
        if (graphPatchConsumer == null || base == null) {
            return previousGraph;
        }
        CandidateGraphDTO currentGraph = importCandidateGraphAssembler.buildSnapshotFromResult(taskId, materialId, base);
        if (currentGraph == null) {
            return previousGraph;
        }
        graphPatchConsumer.accept(buildGraphPatch(
                currentGraph,
                patchSeq,
                stageKey,
                resolveStageName(stageKey),
                summary
        ));
        return currentGraph;
    }

    private PreprocessResultDTO measured(String mode, Supplier<String> supplier) {
        Timer.Sample sample = Timer.start(meterRegistry);
        boolean success = false;
        try {
            String json = supplier.get();
            PreprocessResultDTO result = buildResponse(json);
            success = true;
            return result;
        } finally {
            Counter.builder("caliber.import.preprocess.total")
                    .tag("mode", mode)
                    .tag("success", String.valueOf(success))
                    .register(meterRegistry)
                    .increment();
            sample.stop(Timer.builder("caliber.import.preprocess.latency")
                    .tag("mode", mode)
                    .tag("success", String.valueOf(success))
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    private PreprocessResultDTO buildResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode global = root.path("global");
            List<JsonNode> scenes = new ArrayList<>();
            JsonNode sceneNode = root.path("scenes");
            if (sceneNode.isArray()) {
                sceneNode.forEach(scenes::add);
            }
            JsonNode quality = scenes.isEmpty() ? null : scenes.get(0).path("quality");
            if ((quality == null || quality.isMissingNode() || quality.isNull()) && root.path("quality").isObject()) {
                quality = root.path("quality");
            }
            List<String> warnings = new ArrayList<>();
            JsonNode parseWarnings = root.path("parse_report").path("warnings");
            if (parseWarnings.isArray()) {
                parseWarnings.forEach(item -> warnings.add(item.asText("")));
                warnings.removeIf(String::isBlank);
            }
            String mode = root.path("_meta").path("mode").asText(null);
            double confidence = quality != null && !quality.isMissingNode() && !quality.isNull()
                    ? quality.path("confidence").asDouble(0.0)
                    : 0.0;
            String confidenceLevel = resolveConfidenceLevel(confidence);
            if (confidence < 0.70 && warnings.stream().noneMatch(item -> "confidence_below_threshold".equalsIgnoreCase(item))) {
                warnings.add("confidence_below_threshold");
            }
            return new PreprocessResultDTO(
                    json,
                    mode,
                    global.isMissingNode() ? null : global,
                    scenes,
                    quality == null || quality.isMissingNode() ? null : quality,
                    warnings,
                    confidence,
                    confidenceLevel,
                    confidence < 0.70,
                    null,
                    CandidateGraphDTO.empty(),
                    List.of(),
                    List.of(),
                    null,
                    null
            );
        } catch (Exception ex) {
            return new PreprocessResultDTO(
                    json,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of("preprocess_json_parse_failed"),
                    0.0,
                    "LOW",
                    true,
                    null,
                    CandidateGraphDTO.empty(),
                    List.of(),
                    List.of(),
                    null,
                    null
            );
        }
    }

    private List<String> splitRawText(String rawText) {
        String safe = rawText == null ? "" : rawText;
        if (safe.length() <= CHUNK_MAX_SIZE) {
            return List.of(safe);
        }
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String[] lines = safe.split("\\R", -1);
        for (String line : lines) {
            String normalized = line == null ? "" : line.trim();
            boolean isHeading = normalized.startsWith("##") || normalized.startsWith("###") || normalized.startsWith("####");
            int appendLength = (line == null ? 0 : line.length()) + 1;
            boolean exceedMax = current.length() + appendLength > CHUNK_MAX_SIZE;
            boolean cutAtHeading = isHeading && current.length() >= CHUNK_TARGET_SIZE;
            if (current.length() > 0 && (exceedMax || cutAtHeading)) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }
            if (line != null) {
                current.append(line);
            }
            current.append('\n');
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }

        List<String> normalizedChunks = new ArrayList<>();
        for (String chunk : chunks) {
            if (chunk == null || chunk.isBlank()) {
                continue;
            }
            if (chunk.length() <= CHUNK_MAX_SIZE) {
                normalizedChunks.add(chunk);
                continue;
            }
            int start = 0;
            while (start < chunk.length()) {
                int end = Math.min(chunk.length(), start + CHUNK_MAX_SIZE);
                String piece = chunk.substring(start, end).trim();
                if (!piece.isBlank()) {
                    normalizedChunks.add(piece);
                }
                start = end;
            }
        }
        if (normalizedChunks.isEmpty()) {
            return List.of(safe);
        }
        return normalizedChunks;
    }

    private String appendChunkSuffix(String sourceName, int chunkIndex, int chunkTotal) {
        String safe = sourceName == null ? "" : sourceName.trim();
        if (safe.isEmpty()) {
            return "chunk-" + chunkIndex + "-" + chunkTotal;
        }
        return safe + " [chunk " + chunkIndex + "/" + chunkTotal + "]";
    }

    private void enforceInputLineLimit(String rawText) {
        String safe = rawText == null ? "" : rawText;
        if (safe.isBlank()) {
            return;
        }
        int lineCount = safe.split("\\R", -1).length;
        if (lineCount > MAX_INPUT_LINES) {
            throw new IllegalStateException("文本过长，请分段导入（当前行数：" + lineCount + "，上限：" + MAX_INPUT_LINES + "）");
        }
    }

    private PreprocessResultDTO mergeChunkResults(List<PreprocessResultDTO> chunkResults) {
        if (chunkResults == null || chunkResults.isEmpty()) {
            return new PreprocessResultDTO(
                    "{}",
                    "rule_generated_chunked",
                    null,
                    List.of(),
                    null,
                    List.of("chunk_merge_empty"),
                    0.0,
                    "LOW",
                    true,
                    null,
                    CandidateGraphDTO.empty(),
                    List.of(),
                    List.of(),
                    null,
                    null
            );
        }
        if (chunkResults.size() == 1) {
            return chunkResults.get(0);
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("doc_type", "CALIBER_IMPORT_V2");
        root.put("schema_version", "2.0.0");
        root.put("source_type", "PASTE_MD");

        ObjectNode global = null;
        ArrayNode scenes = objectMapper.createArrayNode();
        Set<String> warnings = new LinkedHashSet<>();
        Set<String> errors = new LinkedHashSet<>();
        double confidenceSum = 0.0;
        int confidenceCount = 0;
        String baseMode = "rule_generated";

        for (PreprocessResultDTO part : chunkResults) {
            if (part == null) {
                continue;
            }
            JsonNode partRoot = readJsonSafely(part.caliberImportJson());
            if (partRoot != null) {
                String sourceType = partRoot.path("source_type").asText("");
                if (!sourceType.isBlank()) {
                    root.put("source_type", sourceType);
                }
                JsonNode globalNode = partRoot.path("global");
                if (global == null && globalNode != null && globalNode.isObject()) {
                    global = ((ObjectNode) globalNode).deepCopy();
                }
                JsonNode scenesNode = partRoot.path("scenes");
                if (scenesNode != null && scenesNode.isArray()) {
                    scenesNode.forEach(node -> scenes.add(node.deepCopy()));
                }
                JsonNode parseWarnings = partRoot.path("parse_report").path("warnings");
                if (parseWarnings != null && parseWarnings.isArray()) {
                    parseWarnings.forEach(item -> warnings.add(item.asText("")));
                }
                JsonNode parseErrors = partRoot.path("parse_report").path("errors");
                if (parseErrors != null && parseErrors.isArray()) {
                    parseErrors.forEach(item -> errors.add(item.asText("")));
                }
            }
            if (part.warnings() != null) {
                warnings.addAll(part.warnings());
            }
            if (part.confidenceScore() != null) {
                confidenceSum += part.confidenceScore();
                confidenceCount += 1;
            }
            if (part.mode() != null && !part.mode().isBlank()) {
                baseMode = part.mode();
            }
        }

        double confidence = confidenceCount == 0 ? 0.0 : confidenceSum / confidenceCount;
        ObjectNode quality = objectMapper.createObjectNode();
        quality.put("confidence", Math.max(0.0, Math.min(1.0, confidence)));
        ArrayNode qualityWarnings = objectMapper.createArrayNode();
        warnings.stream().filter(item -> item != null && !item.isBlank()).forEach(qualityWarnings::add);
        quality.set("warnings", qualityWarnings);
        ArrayNode qualityErrors = objectMapper.createArrayNode();
        errors.stream().filter(item -> item != null && !item.isBlank()).forEach(qualityErrors::add);
        quality.set("errors", qualityErrors);

        ObjectNode parseReport = objectMapper.createObjectNode();
        parseReport.put("parser", "rule-chunked-v1");
        parseReport.set("warnings", qualityWarnings.deepCopy());
        parseReport.set("errors", qualityErrors.deepCopy());

        ObjectNode meta = objectMapper.createObjectNode();
        meta.put("schema_id", "caliber.import.v2");
        meta.put("schema_version", "2.0.0");
        meta.put("mode", baseMode);
        meta.put("lang", "zh");

        root.set("global", global == null ? objectMapper.createObjectNode() : global);
        root.set("scenes", scenes);
        root.set("quality", quality);
        root.set("parse_report", parseReport);
        root.set("_meta", meta);

        try {
            return buildResponse(objectMapper.writeValueAsString(root));
        } catch (Exception ex) {
            return new PreprocessResultDTO(
                    "{}",
                    "rule_generated_chunked",
                    null,
                    List.of(),
                    null,
                    List.of("chunk_merge_json_build_failed"),
                    0.0,
                    "LOW",
                    true,
                    null,
                    CandidateGraphDTO.empty(),
                    List.of(),
                    List.of(),
                    null,
                    null
            );
        }
    }

    private JsonNode readJsonSafely(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(text);
        } catch (Exception ex) {
            return null;
        }
    }

    private PreprocessResultDTO preprocessDirect(PreprocessImportCmd cmd) {
        String preprocessMode = normalizePreprocessMode(cmd.preprocessMode());
        if (PREPROCESS_MODE_RULE_ONLY.equals(preprocessMode)) {
            return measured("rule_only", () -> llmPreprocessSupport.preprocessToCaliberImportV2(
                    cmd.rawText(), cmd.sourceType(), cmd.sourceName()
            ));
        }
        return measured("llm_default", () -> llmPreprocessSupport.preprocessToCaliberImportV2ByLlm(
                cmd.rawText(), cmd.sourceType(), cmd.sourceName()
        ));
    }

    private String resolveConfidenceLevel(double confidence) {
        if (confidence >= 0.85) {
            return "HIGH";
        }
        if (confidence >= 0.70) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private List<PreprocessSceneDraftDTO> persistSceneDrafts(PreprocessResultDTO base,
                                                             PreprocessImportCmd cmd,
                                                             Consumer<PreprocessSceneDraftDTO> draftConsumer) {
        if (sceneCommandAppService == null) {
            return List.of();
        }
        List<PreprocessSceneDraftDTO> drafts = new ArrayList<>();
        List<JsonNode> scenes = base.scenes() == null ? List.of() : base.scenes();
        for (int i = 0; i < scenes.size(); i++) {
            JsonNode scene = scenes.get(i);
            String sceneTitle = safeText(scene.path("scene_title"), "未命名场景" + (i + 1));
            SceneDTO created = sceneCommandAppService.create(new CreateSceneCmd(
                    sceneTitle,
                    null,
                    "",
                    "FACT_DETAIL",
                    cmd.rawText(),
                    cmd.operator()
            ));

            String sqlVariantsJson = writeJson(scene.path("sql_variants"), "[]");
            String sourceTablesJson = writeJson(extractSourceTables(scene.path("sql_variants")), "[]");
            SceneDTO updated = sceneCommandAppService.update(created.id(), new UpdateSceneCmd(
                    sceneTitle,
                    null,
                    safeText(scene.path("domain_guess"), ""),
                    safeText(scene.path("scene_type"), "FACT_DETAIL"),
                    safeText(scene.path("scene_description"), ""),
                    safeText(scene.path("caliber_definition"), ""),
                    safeText(scene.path("applicability"), ""),
                    safeText(scene.path("boundaries"), ""),
                    writeJson(scene.path("inputs"), "{}"),
                    writeJson(scene.path("outputs"), "{}"),
                    sqlVariantsJson,
                    writeJson(scene.path("code_mappings"), "[]"),
                    joinTextArray(scene.path("contributors")),
                    sqlVariantsJson,
                    sourceTablesJson,
                    writeJson(scene.path("caveats"), "[]"),
                    safeText(scene.path("unmapped_text"), ""),
                    writeJson(scene.path("quality"), "{}"),
                    cmd.rawText(),
                    null,
                    cmd.operator()
            ));

            double confidence = scene.path("quality").path("confidence").asDouble(base.confidenceScore() == null ? 0.0 : base.confidenceScore());
            List<String> warnings = readTextList(scene.path("quality").path("warnings"));
            if (warnings.isEmpty() && base.warnings() != null) {
                warnings = base.warnings();
            }
            PreprocessSceneDraftDTO draft = new PreprocessSceneDraftDTO(
                    i,
                    sceneTitle,
                    updated.id(),
                    updated.status(),
                    confidence,
                    confidence < 0.70,
                    warnings
            );
            drafts.add(draft);
            draftConsumer.accept(draft);
        }
        return drafts;
    }

    private PreprocessResultDTO decorateResult(PreprocessResultDTO base,
                                               List<StageTimingDTO> stageTimings,
                                               List<PreprocessSceneDraftDTO> sceneDrafts,
                                               String importBatchId,
                                               String materialId,
                                               long totalElapsedMs) {
        CandidateGraphDTO candidateGraph = base.candidateGraph();
        if (candidateGraph == null
                || candidateGraph.nodes() == null
                || candidateGraph.edges() == null
                || (candidateGraph.nodes().isEmpty() && candidateGraph.edges().isEmpty() && base.scenes() != null && !base.scenes().isEmpty())) {
            candidateGraph = importCandidateGraphAssembler.buildSnapshotFromResult(importBatchId, materialId, base);
        }
        return new PreprocessResultDTO(
                base.caliberImportJson(),
                base.mode(),
                enrichGlobalWithExperiment(base.global(), base.preprocessExperiment()),
                base.scenes(),
                base.quality(),
                base.warnings(),
                base.confidenceScore(),
                base.confidenceLevel(),
                base.lowConfidence(),
                totalElapsedMs,
                candidateGraph,
                stageTimings,
                sceneDrafts,
                importBatchId,
                materialId == null ? base.materialId() : materialId,
                base.preprocessExperiment(),
                base.referenceRefs(),
                base.formalAssetWrites()
        );
    }

    private JsonNode enrichGlobalWithExperiment(JsonNode global, JsonNode preprocessExperiment) {
        ObjectNode result = global != null && global.isObject()
                ? ((ObjectNode) global).deepCopy()
                : objectMapper.createObjectNode();
        if (preprocessExperiment != null && !preprocessExperiment.isMissingNode() && !preprocessExperiment.isNull()) {
            result.set("preprocess_experiment", preprocessExperiment.deepCopy());
        }
        return result;
    }

    private PreprocessResultDTO attachPreprocessExperiment(PreprocessResultDTO base, String importTaskId, String materialId) {
        if (base == null || preprocessExperimentSupport == null) {
            return base;
        }
        PreprocessExperimentSupport.PreprocessExperimentResult experiment = preprocessExperimentSupport.run(
                new PreprocessExperimentSupport.PreprocessExperimentRequest(
                        importTaskId,
                        materialId == null ? base.materialId() : materialId,
                        collectNormalizedChunks(base),
                        List.of(),
                        List.of("TEXT", "PDF"),
                        importTaskId == null ? "trace-import" : "trace-" + importTaskId,
                        base
                )
        );
        return new PreprocessResultDTO(
                base.caliberImportJson(),
                base.mode(),
                base.global(),
                base.scenes(),
                base.quality(),
                base.warnings(),
                base.confidenceScore(),
                base.confidenceLevel(),
                base.lowConfidence(),
                base.totalElapsedMs(),
                base.candidateGraph(),
                base.stageTimings(),
                base.sceneDrafts(),
                base.importBatchId(),
                materialId == null ? base.materialId() : materialId,
                objectMapper.valueToTree(experiment),
                experiment.referenceRefs(),
                experiment.formalAssetWrites()
        );
    }

    private List<String> collectNormalizedChunks(PreprocessResultDTO base) {
        List<String> chunks = new ArrayList<>();
        if (base == null) {
            return chunks;
        }
        if (base.scenes() != null) {
            for (JsonNode scene : base.scenes()) {
                String title = safeText(scene.path("scene_title"), null);
                if (title != null) {
                    chunks.add(title);
                }
                JsonNode variants = scene.path("sql_variants");
                if (variants.isArray()) {
                    variants.forEach(variant -> {
                        String sqlText = safeText(variant.path("sql_text"), null);
                        if (sqlText != null) {
                            chunks.add(sqlText);
                        }
                    });
                }
            }
        }
        if (chunks.isEmpty() && base.global() != null) {
            chunks.add(base.global().toString());
        }
        return chunks;
    }

    private ImportGraphPatchDTO buildGraphPatch(CandidateGraphDTO graph,
                                                int patchSeq,
                                                StageTimingDTO stage,
                                                String message) {
        return buildGraphPatch(graph, patchSeq, stage.stageKey(), stage.stageName(), message);
    }

    private ImportGraphPatchDTO buildGraphPatch(CandidateGraphDTO graph,
                                                int patchSeq,
                                                String stageKey,
                                                String stageName,
                                                String message) {
        List<String> focusNodeIds = new ArrayList<>();
        for (int i = 0; i < graph.nodes().size() && i < 3; i++) {
            focusNodeIds.add(graph.nodes().get(i).id());
        }
        return new ImportGraphPatchDTO(
                patchSeq,
                stageKey,
                stageName,
                graph.nodes(),
                List.of(),
                graph.edges(),
                List.of(),
                focusNodeIds,
                graph.nodes().size(),
                graph.edges().size(),
                message
        );
    }

    private String resolveStageName(String stageKey) {
        if (STAGE_SPLIT.equals(stageKey)) {
            return "文档分块";
        }
        if (STAGE_EXTRACT.equals(stageKey)) {
            return "抽取识别";
        }
        if (STAGE_MERGE.equals(stageKey)) {
            return "结果合并";
        }
        if (STAGE_NORMALIZE.equals(stageKey)) {
            return "结果归一";
        }
        if (STAGE_DRAFT_PERSIST.equals(stageKey)) {
            return "草稿入库";
        }
        if (STAGE_FINALIZE.equals(stageKey)) {
            return "导入完成";
        }
        return "";
    }

    private String normalizeImportBatchId(String importBatchIdInput) {
        if (importBatchIdInput == null || importBatchIdInput.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return importBatchIdInput.trim();
    }

    private String trackTaskStart(String importBatchId, PreprocessImportCmd cmd) {
        if (importTaskCommandAppService == null) {
            return null;
        }
        return importTaskCommandAppService.start(importBatchId, cmd).materialId();
    }

    private void trackTaskReady(String importBatchId, PreprocessResultDTO result) {
        if (importTaskCommandAppService == null) {
            return;
        }
        importTaskCommandAppService.markQualityReviewReady(importBatchId, result);
    }

    private void trackTaskFailed(String importBatchId, String message) {
        if (importTaskCommandAppService == null) {
            return;
        }
        try {
            importTaskCommandAppService.markFailed(importBatchId, message);
        } catch (Exception ignore) {
            // ignore tracking failure
        }
    }

    private ArrayNode extractSourceTables(JsonNode sqlVariants) {
        ArrayNode tables = objectMapper.createArrayNode();
        Set<String> dedup = new LinkedHashSet<>();
        if (sqlVariants != null && sqlVariants.isArray()) {
            for (JsonNode variant : sqlVariants) {
                JsonNode sourceTables = variant.path("source_tables");
                if (sourceTables.isArray()) {
                    for (JsonNode table : sourceTables) {
                        String text = safeText(table, "");
                        if (!text.isBlank()) {
                            dedup.add(text);
                        }
                    }
                }
            }
        }
        dedup.forEach(tables::add);
        return tables;
    }

    private String safeText(JsonNode node, String fallback) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return fallback;
        }
        String text = node.asText(fallback);
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text;
    }

    private String safeText(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text;
    }

    private List<String> readTextList(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        arrayNode.forEach(item -> {
            String text = safeText(item, "");
            if (!text.isBlank()) {
                result.add(text);
            }
        });
        return result;
    }

    private String joinTextArray(JsonNode arrayNode) {
        List<String> texts = readTextList(arrayNode);
        if (texts.isEmpty()) {
            return "";
        }
        return String.join("，", texts);
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

    private StageTimingDTO stage(String stageKey,
                                 String stageName,
                                 long elapsedMs,
                                 int percent,
                                 int chunkIndex,
                                 int chunkTotal,
                                 String message) {
        return new StageTimingDTO(
                stageKey,
                stageName,
                elapsedMs,
                percent,
                chunkIndex,
                chunkTotal,
                message
        );
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private long elapsed(long start) {
        return Math.max(0L, now() - start);
    }

    private String normalizePreprocessMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toUpperCase();
        if (normalized.isBlank() || PREPROCESS_MODE_AUTO.equals(normalized)) {
            return PREPROCESS_MODE_AUTO;
        }
        if (PREPROCESS_MODE_RULE_ONLY.equals(normalized)) {
            return PREPROCESS_MODE_RULE_ONLY;
        }
        if (PREPROCESS_MODE_LLM_ONLY.equals(normalized)) {
            return PREPROCESS_MODE_LLM_ONLY;
        }
        return PREPROCESS_MODE_AUTO;
    }

    private record ChunkTaskResult(
            int chunkIndex,
            PreprocessResultDTO result
    ) {
    }
}
