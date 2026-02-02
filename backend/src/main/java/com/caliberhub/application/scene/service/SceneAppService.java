package com.caliberhub.application.scene.service;

import com.caliberhub.application.audit.AuditService;
import com.caliberhub.application.scene.api.dto.request.CreateSceneCmd;
import com.caliberhub.application.scene.api.dto.request.PublishSceneCmd;
import com.caliberhub.application.scene.api.dto.request.SaveDraftCmd;
import com.caliberhub.application.scene.api.dto.response.SceneDTO;
import com.caliberhub.application.scene.api.dto.response.SceneVersionDTO;
import com.caliberhub.application.support.MetadataSupport;
import com.caliberhub.application.support.MetadataSupport.MatchResult;
import com.caliberhub.domain.audit.AuditAction;
import com.caliberhub.domain.scene.model.Domain;
import com.caliberhub.domain.scene.model.Scene;
import com.caliberhub.domain.scene.model.SceneVersion;
import com.caliberhub.domain.scene.service.LintResult;
import com.caliberhub.domain.scene.service.LintService;
import com.caliberhub.domain.scene.service.LintService.LintContext;
import com.caliberhub.domain.scene.support.DomainRepository;
import com.caliberhub.domain.scene.support.SceneRepository;
import com.caliberhub.domain.scene.support.SqlParserSupport;
import com.caliberhub.domain.scene.valueobject.*;
import com.caliberhub.infrastructure.common.context.UserContextHolder;
import com.caliberhub.infrastructure.common.exception.BusinessException;
import com.caliberhub.infrastructure.common.exception.ResourceNotFoundException;
import com.caliberhub.infrastructure.export.RagExportService;
import com.caliberhub.infrastructure.export.RagExportService.ExportResult;
import com.caliberhub.infrastructure.scene.service.SceneVersionDataService;
import com.caliberhub.infrastructure.scene.dao.mapper.SceneVersionExportMapper;
import com.caliberhub.infrastructure.scene.dao.po.SceneVersionExportPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 场景应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneAppService {

        private final SceneRepository sceneRepository;
        private final DomainRepository domainRepository;
        private final SqlParserSupport sqlParserSupport;
        private final MetadataSupport metadataSupport;
        private final LintService lintService;
        private final RagExportService ragExportService;
        private final AuditService auditService;
        private final SceneVersionDataService sceneVersionDataService;
        private final SceneVersionExportMapper sceneVersionExportMapper;

        /**
         * 创建场景
         */
        @Transactional
        public SceneDTO createScene(CreateSceneCmd cmd) {
                String actor = UserContextHolder.getCurrentUser();

                // 验证领域
                Domain domain;
                if (cmd.getDomainId() == null || cmd.getDomainId().isBlank()) {
                        // 默认取第一个领域
                        List<Domain> domains = domainRepository.findAll();
                        if (domains.isEmpty()) {
                                throw new BusinessException("系统未初始化领域，请先创建领域");
                        }
                        domain = domains.get(0);
                } else {
                        domain = domainRepository.findById(cmd.getDomainId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "领域不存在: " + cmd.getDomainId()));
                }

                // 创建场景
                Scene scene = Scene.create(cmd.getTitle(), domain.getId(), cmd.getOwnerUser(), actor);
                if (cmd.getTags() != null) {
                        scene.getCurrentVersion().setTags(cmd.getTags());
                }

                // 设置基础内容
                SceneVersionContent content = SceneVersionContent.builder()
                                .sceneDescription(cmd.getSceneDescription())
                                .caliberDefinition(cmd.getCaliberDefinition())
                                .build();
                scene.getCurrentVersion().setContent(content);

                // 保存
                sceneRepository.save(scene);

                // 审计
                auditService.logCreateScene(scene.getId(), scene.getCurrentVersion().getId(),
                                scene.getCurrentVersion().getTitle());

                return toSceneDTO(scene, domain);
        }

        /**
         * 获取场景详情
         */
        public SceneDTO getScene(String sceneCode) {
                Scene scene = sceneRepository.findBySceneCode(sceneCode)
                                .orElseThrow(() -> new ResourceNotFoundException("场景不存在: " + sceneCode));

                Domain domain = domainRepository.findById(scene.getDomainId())
                                .orElseThrow(() -> new ResourceNotFoundException("领域不存在"));

                return toSceneDTO(scene, domain);
        }

        /**
         * 获取草稿
         */
        public SceneVersionDTO getDraft(String sceneCode) {
                Scene scene = sceneRepository.findBySceneCode(sceneCode)
                                .orElseThrow(() -> new ResourceNotFoundException("场景不存在: " + sceneCode));

                SceneVersion draft = scene.getCurrentVersion();
                if (draft == null) {
                        throw new ResourceNotFoundException("草稿不存在");
                }

                return toSceneVersionDTO(draft);
        }

        /**
         * 保存草稿
         */
        @Transactional
        public SceneVersionDTO saveDraft(String sceneCode, SaveDraftCmd cmd) {
                String actor = UserContextHolder.getCurrentUser();

                Scene scene = sceneRepository.findBySceneCode(sceneCode)
                                .orElseThrow(() -> new ResourceNotFoundException("场景不存在: " + sceneCode));

                // 构建内容
                SceneVersionContent content = buildContent(cmd);

                // 保存草稿
                scene.saveDraft(cmd.getTitle(), content, actor);

                SceneVersion draft = scene.getCurrentVersion();
                if (cmd.getOwnerUser() != null) {
                        draft.setOwnerUser(cmd.getOwnerUser());
                }
                if (cmd.getTags() != null) {
                        draft.setTags(cmd.getTags());
                }
                if (cmd.getContributors() != null) {
                        draft.setContributors(cmd.getContributors());
                }

                // 抽取表名并匹配
                List<SourceTable> sourceTables = extractAndMatchTables(content.getSqlBlocks());
                content = content.withSourceTables(sourceTables);
                draft.setContent(content);

                // 更新敏感标记
                boolean hasSensitive = sourceTables.stream()
                                .anyMatch(t -> {
                                        var detail = metadataSupport.getTableDetail(t.getTableFullname());
                                        return detail != null
                                                        && detail.fields().stream().anyMatch(f -> f.isSensitive());
                                });
                draft.setHasSensitive(hasSensitive);

                // 保存
                sceneRepository.save(scene);

                // 写入数据来源表和敏感字段到数据库
                sceneVersionDataService.saveSourceTables(draft.getId(), sourceTables);
                if (content.getSensitiveFields() != null) {
                        sceneVersionDataService.saveSensitiveFields(draft.getId(), content.getSensitiveFields());
                }

                // 审计
                auditService.logSaveDraft(scene.getId(), draft.getId());

                return toSceneVersionDTO(draft);
        }

        /**
         * 运行 Lint
         */
        public LintResult lint(String sceneCode) {
                Scene scene = sceneRepository.findBySceneCode(sceneCode)
                                .orElseThrow(() -> new ResourceNotFoundException("场景不存在: " + sceneCode));

                SceneVersion draft = scene.getCurrentVersion();
                if (draft == null) {
                        throw new BusinessException("草稿不存在");
                }

                List<SourceTable> sourceTables = draft.getContent() != null
                                ? draft.getContent().getSourceTables()
                                : List.of();

                boolean hasSensitiveTable = draft.isHasSensitive();

                LintContext context = LintContext.forDraft(sourceTables, hasSensitiveTable);
                return lintService.lint(draft, context);
        }

        /**
         * 发布场景
         */
        @Transactional
        public SceneVersionDTO publish(String sceneCode, PublishSceneCmd cmd) {
                String actor = UserContextHolder.getCurrentUser();

                Scene scene = sceneRepository.findBySceneCode(sceneCode)
                                .orElseThrow(() -> new ResourceNotFoundException("场景不存在: " + sceneCode));

                SceneVersion draft = scene.getCurrentVersion();
                if (draft == null) {
                        throw new BusinessException("草稿不存在");
                }

                // 发布前 Lint 检查
                List<SourceTable> sourceTables = draft.getContent() != null
                                ? draft.getContent().getSourceTables()
                                : List.of();
                List<SensitiveField> sensitiveFields = draft.getContent() != null
                                ? draft.getContent().getSensitiveFields()
                                : List.of();

                LintContext publishContext = LintContext.forPublish(
                                cmd.getLastVerifiedAt().atStartOfDay(),
                                cmd.getVerifiedBy(),
                                cmd.getChangeSummary(),
                                sourceTables,
                                draft.isHasSensitive());

                LintResult lintResult = lintService.lint(draft, publishContext);
                if (!lintResult.isPassed()) {
                        throw new BusinessException("存在校验错误，无法发布");
                }

                // 设置验证信息
                draft.setLastVerifiedAt(cmd.getLastVerifiedAt().atStartOfDay());
                draft.setVerifiedBy(cmd.getVerifiedBy());
                draft.setVerifyEvidence(cmd.getVerifyEvidence());
                draft.setChangeSummary(cmd.getChangeSummary());

                // 发布
                scene.publish(actor);

                // 生成导出
                Domain domain = domainRepository.findById(scene.getDomainId())
                                .orElseThrow(() -> new ResourceNotFoundException("领域不存在"));

                var exportResult = ragExportService.generate(
                                draft, domain, sourceTables, sensitiveFields, lintResult);

                if (exportResult.success()) {
                        draft.setExportDocJson(exportResult.docJson());
                        draft.setExportChunksJson(exportResult.chunksJson());
                        persistExport(draft.getId(), exportResult, actor);
                }

                // 保存
                sceneRepository.save(scene);

                // 写入数据来源表和敏感字段到数据库
                sceneVersionDataService.saveSourceTables(draft.getId(), sourceTables);
                sceneVersionDataService.saveSensitiveFields(draft.getId(), sensitiveFields);

                // 审计
                auditService.logPublish(scene.getId(), draft.getId(), draft.getVersionLabel(),
                                draft.getChangeSummary());

                return toSceneVersionDTO(draft);
        }

        /**
         * 废弃场景
         */
        @Transactional
        public void deprecate(String sceneCode, String reason) {
                String actor = UserContextHolder.getCurrentUser();

                Scene scene = sceneRepository.findBySceneCode(sceneCode)
                                .orElseThrow(() -> new ResourceNotFoundException("场景不存在: " + sceneCode));

                scene.deprecate(actor);
                sceneRepository.save(scene);

                auditService.logDeprecate(scene.getId(), reason);
        }

        /**
         * 获取版本列表
         */
        public List<SceneVersionDTO> getVersions(String sceneCode) {
                Scene scene = sceneRepository.findBySceneCode(sceneCode)
                                .orElseThrow(() -> new ResourceNotFoundException("场景不存在: " + sceneCode));

                return scene.getVersionHistory().stream()
                                .map(this::toSceneVersionDTO)
                                .collect(Collectors.toList());
        }

        /**
         * 创建新版本（基于已发布版本）
         */
        @Transactional
        public SceneVersionDTO createVersion(String sceneCode, String baseVersion) {
                String actor = UserContextHolder.getCurrentUser();

                Scene scene = sceneRepository.findBySceneCode(sceneCode)
                                .orElseThrow(() -> new ResourceNotFoundException("场景不存在: " + sceneCode));

                if (scene.hasDraft()) {
                        throw new BusinessException("当前已存在草稿版本，请勿重复创建");
                }

                // 简单的基于当前发布版本创建草稿逻辑
                // 这里调用 saveDraft 也可以，但 saveDraft 需要传 content
                // 我们调用 Scene 的内部方法来创建草稿
                // 注意：Scene.saveDraft 会自动基于 Published 创建 Draft 如果 draft 为 null
                // 这里我们构造一个空的 updateContent 调用即可触发 createDraft 逻辑
                // TODO: 应该从当前发布版本复制内容，此处暂时创建空内容
                scene.saveDraft(null, SceneVersionContent.empty(), actor);

                sceneRepository.save(scene);

                return toSceneVersionDTO(scene.getCurrentVersion());
        }

        // === 私有方法 ===

        private SceneVersionContent buildContent(SaveDraftCmd cmd) {
                List<InputParam> inputParams = cmd.getInputParams() != null
                                ? cmd.getInputParams().stream()
                                                .map(p -> InputParam.of(p.getName(), p.getDisplayName(), p.getType(),
                                                                p.isRequired(), p.getExample(), p.getDescription()))
                                                .collect(Collectors.toList())
                                : List.of();

                List<SqlBlock> sqlBlocks = cmd.getSqlBlocks() != null
                                ? cmd.getSqlBlocks().stream()
                                                .map(b -> SqlBlock.of(b.getBlockId(), b.getName(), b.getCondition(),
                                                                b.getSql(), b.getNotes()))
                                                .collect(Collectors.toList())
                                : List.of();

                List<Caveat> caveats = cmd.getCaveats() != null
                                ? cmd.getCaveats().stream()
                                                .map(c -> Caveat.of(c.getId(), c.getTitle(), c.getRisk(), c.getText()))
                                                .collect(Collectors.toList())
                                : List.of();

                return SceneVersionContent.builder()
                                .sceneDescription(cmd.getSceneDescription())
                                .caliberDefinition(cmd.getCaliberDefinition())
                                .inputParams(inputParams)
                                .constraintsDescription(cmd.getConstraintsDescription())
                                .outputSummary(cmd.getOutputSummary())
                                .sqlBlocks(sqlBlocks)
                                .caveats(caveats)
                                .build();
        }

        private List<SourceTable> extractAndMatchTables(List<SqlBlock> sqlBlocks) {
                if (sqlBlocks == null || sqlBlocks.isEmpty()) {
                        return List.of();
                }

                // 转换为 SqlBlockInput
                List<SqlParserSupport.SqlBlockInput> inputs = sqlBlocks.stream()
                                .map(b -> new SqlParserSupport.SqlBlockInput(b.getBlockId(), b.getSql()))
                                .collect(Collectors.toList());

                SqlParserSupport.SqlParseResult parseResult = sqlParserSupport.extractTablesFromBlocks(inputs);
                List<SourceTable> result = new ArrayList<>();

                for (SqlParserSupport.TableInfo tableInfo : parseResult.tables()) {
                        MetadataSupport.MatchResult matchResult = metadataSupport.matchTable(tableInfo.tableFullname());

                        TableMatchStatus status;
                        switch (matchResult.status()) {
                                case MATCHED -> status = TableMatchStatus.MATCHED;
                                case NOT_FOUND -> status = TableMatchStatus.NOT_FOUND;
                                case BLACKLISTED -> status = TableMatchStatus.BLACKLISTED;
                                default -> status = TableMatchStatus.VERIFY_FAILED;
                        }

                        result.add(SourceTable.builder()
                                        .tableFullname(tableInfo.tableFullname())
                                        .metadataTableId(matchResult.metadataTableId())
                                        .matchStatus(status)
                                        .keyTable(false)
                                        .source("EXTRACTED")
                                        .build());
                }

                return result;
        }

        /**
         * 导出场景
         */
        public ExportResult export(String sceneCode) {
                // Find scene
                Scene scene = sceneRepository.findBySceneCode(sceneCode)
                                .orElseThrow(() -> new ResourceNotFoundException("场景不存在: " + sceneCode));

                SceneVersion version = scene.getCurrentVersion();
                if (version == null) {
                        throw new BusinessException("场景没有有效版本");
                }

                Domain domain = domainRepository.findById(scene.getDomainId())
                                .orElseThrow(() -> new ResourceNotFoundException("Domain not found"));

                SceneVersionContent content = version.getContent();
                List<SourceTable> sourceTables = content != null && content.getSourceTables() != null
                                ? content.getSourceTables()
                                : List.of();
                List<SensitiveField> sensitiveFields = content != null && content.getSensitiveFields() != null
                                ? content.getSensitiveFields()
                                : List.of();

                // 若已有导出记录，直接返回
                var existing = sceneVersionExportMapper.findById(version.getId());
                if (existing.isPresent()) {
                        var po = existing.get();
                        return ExportResult.success(po.getDocJson(), po.getChunksJson(), po.getChunkCount());
                }

                // Context for Lint
                LintContext context = LintContext.forDraft(sourceTables, version.isHasSensitive());
                LintResult lintResult = lintService.lint(version, context);

                ExportResult result = ragExportService.generate(version, domain, sourceTables, sensitiveFields, lintResult);
                if (result.success()) {
                        persistExport(version.getId(), result, UserContextHolder.getCurrentUser());
                }
                return result;
        }

        private SceneDTO toSceneDTO(Scene scene, Domain domain) {
                SceneVersion current = scene.getCurrentVersion();

                return SceneDTO.builder()
                                .id(scene.getId())
                                .sceneCode(scene.getSceneCode())
                                .title(current != null ? current.getTitle() : null)
                                .domainId(scene.getDomainId())
                                .domainKey(domain.getDomainKey())
                                .domainName(domain.getName())
                                .lifecycleStatus(scene.getLifecycleStatus().name())
                                .currentVersionId(current != null ? current.getId() : null)
                                .currentVersionLabel(current != null ? current.getVersionLabel() : null)
                                .versionStatus(current != null ? current.getStatus().name() : null)
                                .hasSensitive(current != null && current.isHasSensitive())
                                .lastVerifiedAt(current != null ? current.getLastVerifiedAt() : null)
                                .verifiedBy(current != null ? current.getVerifiedBy() : null)
                                .ownerUser(current != null ? current.getOwnerUser() : null)
                                .tags(current != null ? current.getTags() : List.of())
                                .createdAt(scene.getCreatedAt())
                                .createdBy(scene.getCreatedBy())
                                .updatedAt(scene.getUpdatedAt())
                                .updatedBy(scene.getUpdatedBy())
                                .build();
        }

        private SceneVersionDTO toSceneVersionDTO(SceneVersion version) {
                return SceneVersionDTO.builder()
                                .id(version.getId())
                                .sceneId(version.getSceneId())
                                .sceneCode(version.getSceneCode())
                                .status(version.getStatus().name())
                                .isCurrent(version.isCurrent())
                                .versionSeq(version.getVersionSeq())
                                .versionLabel(version.getVersionLabel())
                                .title(version.getTitle())
                                .tags(version.getTags())
                                .ownerUser(version.getOwnerUser())
                                .contributors(version.getContributors())
                                .hasSensitive(version.isHasSensitive())
                                .lastVerifiedAt(version.getLastVerifiedAt())
                                .verifiedBy(version.getVerifiedBy())
                                .verifyEvidence(version.getVerifyEvidence())
                                .changeSummary(version.getChangeSummary())
                                .publishedAt(version.getPublishedAt())
                                .publishedBy(version.getPublishedBy())
                                .createdAt(version.getCreatedAt())
                                .createdBy(version.getCreatedBy())
                                .updatedAt(version.getUpdatedAt())
                                .updatedBy(version.getUpdatedBy())
                                .build();
        }

        private void persistExport(String versionId, ExportResult result, String actor) {
                SceneVersionExportPO po = SceneVersionExportPO.builder()
                                .versionId(versionId)
                                .docJson(result.docJson())
                                .chunksJson(result.chunksJson())
                                .chunkCount(result.chunkCount())
                                .generatedAt(LocalDateTime.now().toString())
                                .generatedBy(actor)
                                .hash(null)
                                .build();
                sceneVersionExportMapper.save(po);
        }
}
