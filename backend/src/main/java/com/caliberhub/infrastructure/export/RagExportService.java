package com.caliberhub.infrastructure.export;

import com.caliberhub.domain.scene.model.Domain;
import com.caliberhub.domain.scene.model.SceneVersion;
import com.caliberhub.domain.scene.service.LintResult;
import com.caliberhub.domain.scene.valueobject.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * RAG 导出服务
 * 生成 doc.json 和 chunks.json
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagExportService {

    private static final String DOC_SCHEMA = "caliberhub.doc.v0.1";
    private static final String CHUNKS_SCHEMA = "caliberhub.chunks.v0.1";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ObjectMapper objectMapper;

    /**
     * 生成导出结果
     */
    public ExportResult generate(SceneVersion version, Domain domain,
            List<SourceTable> sourceTables,
            List<SensitiveField> sensitiveFields,
            LintResult lintResult) {
        try {
            Map<String, Object> docJson = generateDocJson(version, domain, sourceTables, sensitiveFields, lintResult);
            Map<String, Object> chunksJson = generateChunksJson(version, domain, sourceTables, sensitiveFields);

            String docJsonStr = objectMapper.writeValueAsString(docJson);
            String chunksJsonStr = objectMapper.writeValueAsString(chunksJson);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> chunks = (List<Map<String, Object>>) chunksJson.get("chunks");
            int chunkCount = chunks != null ? chunks.size() : 0;

            return ExportResult.success(docJsonStr, chunksJsonStr, chunkCount);
        } catch (Exception e) {
            log.error("导出生成失败", e);
            return ExportResult.failed(e.getMessage());
        }
    }

    /**
     * 生成 doc.json
     */
    private Map<String, Object> generateDocJson(SceneVersion version, Domain domain,
            List<SourceTable> sourceTables,
            List<SensitiveField> sensitiveFields,
            LintResult lintResult) {
        Map<String, Object> doc = new LinkedHashMap<>();

        doc.put("$schema", DOC_SCHEMA);
        doc.put("doc_id", version.getSceneId());

        // scene
        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("title", version.getTitle());
        scene.put("domain", Map.of(
                "key", domain.getDomainKey(),
                "name", domain.getName()));
        scene.put("tags", version.getTags() != null ? version.getTags() : List.of());
        doc.put("scene", scene);

        // version
        Map<String, Object> versionInfo = new LinkedHashMap<>();
        versionInfo.put("version_id", version.getId());
        versionInfo.put("version_seq", version.getVersionSeq());
        versionInfo.put("version_label", version.getVersionLabel());
        versionInfo.put("published_at", formatDateTime(version.getPublishedAt()));
        versionInfo.put("published_by", version.getPublishedBy());
        versionInfo.put("change_summary", version.getChangeSummary());
        doc.put("version", versionInfo);

        // governance
        Map<String, Object> governance = new LinkedHashMap<>();
        governance.put("owner", version.getOwnerUser());
        governance.put("contributors", version.getContributors() != null ? version.getContributors() : List.of());
        governance.put("last_verified", Map.of(
                "date", formatDateTime(version.getLastVerifiedAt()),
                "by", version.getVerifiedBy() != null ? version.getVerifiedBy() : "",
                "evidence", version.getVerifyEvidence() != null ? version.getVerifyEvidence() : ""));
        governance.put("has_sensitive", version.isHasSensitive());
        governance.put("sensitive_fields",
                sensitiveFields != null ? sensitiveFields.stream().map(this::mapSensitiveField).toList() : List.of());
        doc.put("governance", governance);

        // data_sources
        Map<String, Object> dataSources = new LinkedHashMap<>();
        dataSources.put("tables",
                sourceTables != null ? sourceTables.stream().map(this::mapSourceTable).toList() : List.of());
        doc.put("data_sources", dataSources);

        // content
        SceneVersionContent content = version.getContent();
        if (content != null) {
            Map<String, Object> contentMap = new LinkedHashMap<>();
            contentMap.put("scene_description", content.getSceneDescription());
            contentMap.put("caliber_definition", content.getCaliberDefinition());
            contentMap.put("inputs", Map.of(
                    "params",
                    content.getInputParams() != null
                            ? content.getInputParams().stream().map(this::mapInputParam).toList()
                            : List.of(),
                    "constraints",
                    content.getConstraintsDescription() != null ? content.getConstraintsDescription() : ""));
            contentMap.put("outputs", Map.of(
                    "summary", content.getOutputSummary() != null ? content.getOutputSummary() : ""));
            contentMap.put("sql_blocks",
                    content.getSqlBlocks() != null ? content.getSqlBlocks().stream().map(this::mapSqlBlock).toList()
                            : List.of());
            contentMap.put("caveats",
                    content.getCaveats() != null ? content.getCaveats().stream().map(this::mapCaveat).toList()
                            : List.of());
            doc.put("content", contentMap);
        }

        // lint
        if (lintResult != null) {
            Map<String, Object> lint = new LinkedHashMap<>();
            lint.put("passed", lintResult.isPassed());
            lint.put("errors", lintResult.getErrors().stream().map(this::mapLintIssue).toList());
            lint.put("warnings", lintResult.getWarnings().stream().map(this::mapLintIssue).toList());
            doc.put("lint", lint);
        }

        return doc;
    }

    /**
     * 生成 chunks.json
     */
    private Map<String, Object> generateChunksJson(SceneVersion version, Domain domain,
            List<SourceTable> sourceTables,
            List<SensitiveField> sensitiveFields) {
        Map<String, Object> chunksDoc = new LinkedHashMap<>();
        chunksDoc.put("$schema", CHUNKS_SCHEMA);
        chunksDoc.put("doc_id", version.getSceneId());
        chunksDoc.put("version_label", version.getVersionLabel());
        chunksDoc.put("version_id", version.getId());
        chunksDoc.put("generated_at", formatDateTime(LocalDateTime.now()));

        List<Map<String, Object>> chunks = new ArrayList<>();
        SceneVersionContent content = version.getContent();

        // 基础 metadata
        Map<String, Object> baseMetadata = new LinkedHashMap<>();
        baseMetadata.put("domain_key", domain.getDomainKey());
        baseMetadata.put("tags", version.getTags() != null ? version.getTags() : List.of());
        baseMetadata.put("tables",
                sourceTables != null ? sourceTables.stream().map(SourceTable::getTableFullname).toList() : List.of());
        baseMetadata.put("has_sensitive", version.isHasSensitive());
        baseMetadata.put("last_verified", formatDateTime(version.getLastVerifiedAt()));

        int seq = 0;

        // 口径定义 chunk
        if (content != null && content.getCaliberDefinition() != null && !content.getCaliberDefinition().isBlank()) {
            chunks.add(createChunk(version, "caliber_definition", seq++, "text",
                    "【口径定义】" + content.getCaliberDefinition(),
                    "content.caliber_definition", null, baseMetadata));
        }

        // 场景描述 chunk
        if (content != null && content.getSceneDescription() != null && !content.getSceneDescription().isBlank()) {
            chunks.add(createChunk(version, "scene_description", seq++, "text",
                    "【场景描述】" + content.getSceneDescription(),
                    "content.scene_description", null, baseMetadata));
        }

        // 输入参数 chunk
        if (content != null && content.getInputParams() != null && !content.getInputParams().isEmpty()) {
            StringBuilder inputText = new StringBuilder("【输入参数】\n");
            for (InputParam param : content.getInputParams()) {
                inputText.append("- ").append(param.getDisplayName())
                        .append("(").append(param.getName()).append(")")
                        .append(": ").append(param.getType())
                        .append(param.isRequired() ? " [必填]" : " [可选]")
                        .append("\n");
            }
            chunks.add(createChunk(version, "inputs", seq++, "text",
                    inputText.toString(), "content.inputs", null, baseMetadata));
        }

        // SQL 块 chunks
        if (content != null && content.getSqlBlocks() != null) {
            for (SqlBlock block : content.getSqlBlocks()) {
                String text = "【SQL方案 " + block.getName() + "】\n```sql\n" + block.getSql() + "\n```";
                if (block.getCondition() != null && !block.getCondition().isBlank()) {
                    text = "【适用条件】" + block.getCondition() + "\n" + text;
                }
                chunks.add(createChunk(version, "sql_block", seq++, "sql",
                        text, "content.sql_blocks[" + block.getBlockId() + "]",
                        block.getBlockId(), baseMetadata));
            }
        }

        // 注意事项 chunks
        if (content != null && content.getCaveats() != null) {
            for (Caveat caveat : content.getCaveats()) {
                String text = "【注意事项】" + caveat.getTitle() + "\n" + caveat.getText();
                chunks.add(createChunk(version, "caveat", seq++, "text",
                        text, "content.caveats[" + caveat.getId() + "]",
                        null, baseMetadata));
            }
        }

        chunksDoc.put("chunk_count", chunks.size());
        chunksDoc.put("chunks", chunks);

        return chunksDoc;
    }

    private Map<String, Object> createChunk(SceneVersion version, String section, int seq,
            String contentType, String text, String sourcePath,
            String blockId, Map<String, Object> baseMetadata) {
        Map<String, Object> chunk = new LinkedHashMap<>();
        chunk.put("chunk_id", version.getSceneId() + "::" + version.getVersionLabel() + "::" + section + "::" + seq);
        chunk.put("section", section);
        chunk.put("content_type", contentType);
        chunk.put("text", text);

        Map<String, Object> metadata = new LinkedHashMap<>(baseMetadata);
        metadata.put("source_path", sourcePath);
        if (blockId != null) {
            metadata.put("block_id", blockId);
        }
        chunk.put("metadata", metadata);

        return chunk;
    }

    // === 映射方法 ===

    private Map<String, Object> mapSourceTable(SourceTable table) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("table_fullname", table.getTableFullname());
        map.put("metadata_table_id", table.getMetadataTableId());
        map.put("match_status", table.getMatchStatus().name());
        map.put("is_key", table.isKeyTable());
        map.put("usage_type", table.getUsageType());
        map.put("partition_field", table.getPartitionField());
        map.put("source", table.getSource());
        return map;
    }

    private Map<String, Object> mapSensitiveField(SensitiveField field) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("field_fullname", field.getFieldFullname());
        map.put("sensitivity_level", field.getSensitivityLevel());
        map.put("mask_rule", field.getMaskRule());
        map.put("remarks", field.getRemarks());
        return map;
    }

    private Map<String, Object> mapInputParam(InputParam param) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", param.getName());
        map.put("display_name", param.getDisplayName());
        map.put("type", param.getType());
        map.put("required", param.isRequired());
        map.put("example", param.getExample());
        map.put("description", param.getDescription());
        return map;
    }

    private Map<String, Object> mapSqlBlock(SqlBlock block) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("block_id", block.getBlockId());
        map.put("name", block.getName());
        map.put("condition", block.getCondition());
        map.put("sql", block.getSql());
        map.put("notes", block.getNotes());
        return map;
    }

    private Map<String, Object> mapCaveat(Caveat caveat) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", caveat.getId());
        map.put("title", caveat.getTitle());
        map.put("risk", caveat.getRisk());
        map.put("text", caveat.getText());
        return map;
    }

    private Map<String, Object> mapLintIssue(LintResult.LintIssue issue) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", issue.getId());
        map.put("message", issue.getMessage());
        map.put("path", issue.getPath());
        if (issue.getBlockId() != null) {
            map.put("block_id", issue.getBlockId());
        }
        return map;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(ISO_FORMATTER) : null;
    }

    /**
     * 导出结果
     */
    public record ExportResult(
            boolean success,
            String docJson,
            String chunksJson,
            int chunkCount,
            String errorMessage) {
        public static ExportResult success(String docJson, String chunksJson, int chunkCount) {
            return new ExportResult(true, docJson, chunksJson, chunkCount, null);
        }

        public static ExportResult failed(String errorMessage) {
            return new ExportResult(false, null, null, 0, errorMessage);
        }
    }
}
