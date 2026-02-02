package com.caliberhub.application.importdoc.service;

import com.caliberhub.application.importdoc.dto.*;
import com.caliberhub.domain.importdoc.service.JsonImportParser;
import com.caliberhub.domain.importdoc.service.MarkdownParser;
import com.caliberhub.domain.scene.model.Scene;
import com.caliberhub.domain.scene.model.SceneVersion;
import com.caliberhub.domain.scene.support.DomainRepository;
import com.caliberhub.domain.scene.support.SceneRepository;
import com.caliberhub.domain.scene.support.SqlParserSupport;
import com.caliberhub.domain.scene.valueobject.*;
import com.caliberhub.infrastructure.common.context.UserContextHolder;
import com.caliberhub.infrastructure.scene.service.SceneVersionDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档导入应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportAppService {

    private final MarkdownParser markdownParser;
    private final JsonImportParser jsonImportParser;
    private final SceneRepository sceneRepository;
    private final DomainRepository domainRepository;
    private final SqlParserSupport sqlParserSupport;
    private final SceneVersionDataService sceneVersionDataService;
    private final ObjectMapper objectMapper;

    /**
     * 解析预览
     */
    public ParseResponse parse(ParseRequest request) {
        log.info("解析导入文档, sourceType={}, mode={}, rawTextLength={}",
                request.getSourceType(), request.getMode(),
                request.getRawText() != null ? request.getRawText().length() : 0);

        return parseInternal(request.getRawText(), request.getMode());
    }

    /**
     * 确认导入
     */
    @Transactional
    public CommitResponse commit(CommitRequest request) {
        String actor = UserContextHolder.getCurrentUser();
        log.info("提交导入, sourceType={}, mode={}, selectedCount={}, actor={}",
                request.getSourceType(), request.getMode(),
                request.getSelectedTempIds() != null ? request.getSelectedTempIds().size() : 0,
                actor);

        // 1. 重新解析获取候选（支持 JSON + Markdown）
        ParseResponse parseResponse = parseInternal(request.getRawText(), request.getMode());

        // 2. 过滤选中的候选
        Set<String> selectedIds = new HashSet<>(request.getSelectedTempIds());
        List<ParseResponse.SceneCandidate> selectedCandidates = parseResponse.getSceneCandidates().stream()
                .filter(c -> selectedIds.contains(c.getTempId()))
                .collect(Collectors.toList());

        if (selectedCandidates.isEmpty()) {
            return CommitResponse.builder()
                    .createdScenes(List.of())
                    .build();
        }

        // 3. 获取默认领域
        String defaultDomainId = request.getDefaultDomainId();
        if (defaultDomainId == null || defaultDomainId.isBlank()) {
            // 使用第一个领域作为默认
            var domains = domainRepository.findAll();
            if (!domains.isEmpty()) {
                defaultDomainId = domains.get(0).getId();
            }
        }

        // 4. 为每个候选创建场景
        List<CommitResponse.CreatedScene> createdScenes = new ArrayList<>();
        String parseReportJson;
        try {
            parseReportJson = objectMapper.writeValueAsString(parseResponse.getParseReport());
        } catch (Exception e) {
            parseReportJson = "{}";
        }

        for (ParseResponse.SceneCandidate candidate : selectedCandidates) {
            try {
                CommitResponse.CreatedScene created = createSceneFromCandidate(
                        candidate,
                        defaultDomainId,
                        request.getSourceType(),
                        request.getRawText(),
                        parseReportJson,
                        actor);
                createdScenes.add(created);
            } catch (Exception e) {
                log.error("创建场景失败: tempId={}", candidate.getTempId(), e);
            }
        }

        return CommitResponse.builder()
                .createdScenes(createdScenes)
                .build();
    }

    /**
     * 从候选创建场景
     */
    private CommitResponse.CreatedScene createSceneFromCandidate(
            ParseResponse.SceneCandidate candidate,
            String domainId,
            String sourceType,
            String sourceRaw,
            String parseReportJson,
            String actor) {

        ParseResponse.DraftContent dc = candidate.getDraftContent();

        // 生成场景编码
        String sceneCode = "SCE-IMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 构建版本内容
        List<SqlBlock> sqlBlocks = dc.getSqlBlocks() != null ? dc.getSqlBlocks().stream()
                .map(sb -> SqlBlock.of(sb.getBlockId(), sb.getName(), sb.getCondition(), sb.getSql(), sb.getNotes()))
                .collect(Collectors.toList()) : List.of();

        List<Caveat> caveats = dc.getCaveats() != null ? dc.getCaveats().stream()
                .map(c -> Caveat.of(
                        "cav-" + UUID.randomUUID().toString().substring(0, 6),
                        c.getTitle(),
                        c.getRisk(),
                        c.getText()))
                .collect(Collectors.toList()) : List.of();

        SceneVersionContent content = SceneVersionContent.builder()
                .sceneDescription(dc.getSceneDescription())
                .caliberDefinition(dc.getCaliberDefinition())
                .applicability(dc.getApplicability())
                .boundaries(dc.getBoundaries())
                .entities(dc.getEntities())
                .inputParams(List.of())
                .constraintsDescription("")
                .outputSummary(dc.getOutputs() != null ? dc.getOutputs().getSummary() : "")
                .sqlBlocks(sqlBlocks)
                .caveats(caveats)
                .sourceTables(List.of())
                .sensitiveFields(List.of())
                .build();

        // 创建场景
        String title = dc.getTitle() != null && !dc.getTitle().isBlank() ? dc.getTitle() : candidate.getTitleGuess();
        if (title == null || title.isBlank()) {
            title = "导入场景-" + sceneCode;
        }

        // 保留导入生成的场景编码
        Scene scene = Scene.createWithCode(sceneCode, title, domainId, actor, actor);
        scene.saveDraft(title, content, actor);

        SceneVersion draft = scene.getCurrentVersion();

        // 设置导入元信息
        draft.setSourceType(sourceType);
        draft.setSourceRaw(truncateIfNeeded(sourceRaw, 1_000_000));
        draft.setParseReportJson(parseReportJson);
        draft.setImportedBy(actor);
        draft.setImportedAt(LocalDateTime.now());

        // 设置标签和贡献者
        if (dc.getTags() != null) {
            draft.setTags(dc.getTags());
        }
        if (dc.getContributors() != null) {
            draft.setContributors(dc.getContributors());
        }

        // 保存场景
        sceneRepository.save(scene);

        // 抽取表名并匹配
        try {
            List<SourceTable> sourceTables = extractAndMatchTables(sqlBlocks);
            if (!sourceTables.isEmpty()) {
                content = content.withSourceTables(sourceTables);
                draft.setContent(content);
                sceneVersionDataService.saveSourceTables(draft.getId(), sourceTables);
            }
        } catch (Exception e) {
            log.warn("表名抽取失败: sceneCode={}", sceneCode, e);
        }

        log.info("导入创建场景成功: sceneCode={}, draftVersionId={}", scene.getSceneCode(), draft.getId());

        return CommitResponse.CreatedScene.builder()
                .sceneCode(scene.getSceneCode())
                .draftVersionId(draft.getId())
                .build();
    }

    /**
     * 抽取并匹配表名
     */
    private List<SourceTable> extractAndMatchTables(List<SqlBlock> sqlBlocks) {
        if (sqlBlocks == null || sqlBlocks.isEmpty()) {
            return List.of();
        }

        // 转换为SqlBlockInput
        List<SqlParserSupport.SqlBlockInput> inputs = sqlBlocks.stream()
                .map(b -> new SqlParserSupport.SqlBlockInput(b.getBlockId(), b.getSql()))
                .collect(Collectors.toList());

        // 调用SQL解析服务
        SqlParserSupport.SqlParseResult result = sqlParserSupport.extractTablesFromBlocks(inputs);

        // 构建SourceTable列表
        return result.tables().stream()
                .map(t -> SourceTable.builder()
                        .tableFullname(t.tableFullname())
                        .matchStatus(TableMatchStatus.NOT_FOUND)
                        .keyTable(false)
                        .source("EXTRACTED")
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 截断超长文本
     */
    private String truncateIfNeeded(String text, int maxLength) {
        if (text == null)
            return null;
        if (text.length() <= maxLength)
            return text;
        return text.substring(0, maxLength);
    }

    /**
     * 根据输入选择解析器：优先 JSON，失败再回退 Markdown。
     */
    private ParseResponse parseInternal(String rawText, String mode) {
        try {
            // 优先尝试 JSON 导入格式
            ParseResponse jsonParsed = jsonImportParser.tryParse(rawText);
            if (jsonParsed != null) {
                return jsonParsed;
            }
            // 回退 Markdown / 自由文本解析
            return markdownParser.parse(rawText, mode);
        } catch (Exception e) {
            log.error("导入解析异常", e);
            return ParseResponse.builder()
                    .mode(mode)
                    .sceneCandidates(List.of())
                    .parseReport(ParseResponse.ParseReport.builder()
                            .parser("error")
                            .mode(mode)
                            .global_errors(List.of(e.getMessage()))
                            .global_warnings(List.of())
                            .scenes(List.of())
                            .build())
                    .build();
        }
    }
}
