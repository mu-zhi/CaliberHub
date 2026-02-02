package com.caliberhub.domain.scene.service;

import com.caliberhub.domain.scene.model.SceneVersion;
import com.caliberhub.domain.scene.service.LintResult.LintIssue;
import com.caliberhub.domain.scene.valueobject.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Lint 服务 - 领域服务
 * 实现 P0 规则清单中的 E001-E006, W001-W006
 */
@Slf4j
@Service
public class LintService {
    
    private static final Pattern SELECT_STAR_PATTERN = Pattern.compile(
            "SELECT\\s+\\*", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern PARTITION_DATE_PATTERN = Pattern.compile(
            "(WHERE|AND)\\s+.*?(date|dt|partition|p_date|biz_date)", Pattern.CASE_INSENSITIVE);
    
    /**
     * 对草稿版本执行 Lint 检查
     */
    public LintResult lint(SceneVersion version, LintContext context) {
        List<LintIssue> errors = new ArrayList<>();
        List<LintIssue> warnings = new ArrayList<>();
        
        SceneVersionContent content = version.getContent();
        
        // === Error 检查 ===
        
        // E001: 缺必填项
        checkRequiredFields(version, content, errors);
        
        // E002: SQL 抽取到的表名在元数据平台 NOT_FOUND
        checkTableNotFound(context.sourceTables(), errors);
        
        // E003: SQL 抽取到的表命中 BLACKLISTED
        checkTableBlacklisted(context.sourceTables(), errors);
        
        // E004: 已选择的敏感字段缺 mask_rule
        checkSensitiveFieldMaskRule(content.getSensitiveFields(), errors);
        
        // E005: 发布时缺 last_verified/verified_by（仅在发布检查时）
        if (context.isPublishCheck()) {
            checkLastVerified(context.lastVerifiedAt(), context.verifiedBy(), errors);
        }
        
        // E006: 发布时缺变更摘要
        if (context.isPublishCheck()) {
            checkChangeSummary(context.changeSummary(), errors);
        }
        
        // === Warning 检查 ===
        
        // W001: SQL 使用 SELECT *
        checkSelectStar(content.getSqlBlocks(), warnings);
        
        // W002: SQL 没有分区/日期条件
        checkPartitionCondition(content.getSqlBlocks(), warnings);
        
        // W003: 注意事项为空
        checkCaveatsEmpty(content.getCaveats(), warnings);
        
        // W004: last_verified 距今 > 365 天
        checkVerificationExpiry(version.getLastVerifiedAt(), warnings);
        
        // W005: 涉及敏感字段但无合规说明
        // (暂时跳过，需要更多上下文)
        
        // W006: 已选表中存在敏感字段，但敏感字段清单为空
        checkSensitiveTableNoFields(context.sourceTables(), content.getSensitiveFields(), 
                                     context.hasSensitiveTable(), warnings);
        
        return LintResult.of(errors, warnings);
    }
    
    // === Error 检查方法 ===
    
    private void checkRequiredFields(SceneVersion version, SceneVersionContent content, 
                                      List<LintIssue> errors) {
        if (version.getTitle() == null || version.getTitle().isBlank()) {
            errors.add(LintIssue.error("E001", "标题不能为空", "title"));
        }
        
        if (version.getOwnerUser() == null || version.getOwnerUser().isBlank()) {
            errors.add(LintIssue.error("E001", "负责人不能为空", "owner_user"));
        }
        
        if (content == null) {
            errors.add(LintIssue.error("E001", "内容不能为空", "content"));
            return;
        }
        
        if (content.getSceneDescription() == null || content.getSceneDescription().isBlank()) {
            errors.add(LintIssue.error("E001", "场景描述不能为空", "content.scene_description"));
        }
        
        if (content.getCaliberDefinition() == null || content.getCaliberDefinition().isBlank()) {
            errors.add(LintIssue.error("E001", "口径定义不能为空", "content.caliber_definition"));
        }
        
        if (content.getSqlBlocks() == null || content.getSqlBlocks().isEmpty()) {
            errors.add(LintIssue.error("E001", "至少需要一段 SQL", "content.sql_blocks"));
        }
    }
    
    private void checkTableNotFound(List<SourceTable> tables, List<LintIssue> errors) {
        if (tables == null) return;
        
        for (SourceTable table : tables) {
            if (table.getMatchStatus() == TableMatchStatus.NOT_FOUND) {
                errors.add(LintIssue.error("E002", 
                        "表 " + table.getTableFullname() + " 在元数据平台未找到",
                        "content.source_tables"));
            }
        }
    }
    
    private void checkTableBlacklisted(List<SourceTable> tables, List<LintIssue> errors) {
        if (tables == null) return;
        
        for (SourceTable table : tables) {
            if (table.getMatchStatus() == TableMatchStatus.BLACKLISTED) {
                errors.add(LintIssue.error("E003",
                        "表 " + table.getTableFullname() + " 命中黑名单",
                        "content.source_tables"));
            }
        }
    }
    
    private void checkSensitiveFieldMaskRule(List<SensitiveField> fields, List<LintIssue> errors) {
        if (fields == null) return;
        
        for (SensitiveField field : fields) {
            if (field.getMaskRule() == null) {
                errors.add(LintIssue.error("E004",
                        "敏感字段 " + field.getFieldFullname() + " 缺少脱敏规则",
                        "content.sensitive_fields"));
            }
        }
    }
    
    private void checkLastVerified(LocalDateTime lastVerifiedAt, String verifiedBy, 
                                    List<LintIssue> errors) {
        if (lastVerifiedAt == null) {
            errors.add(LintIssue.error("E005", "发布时必须填写最后验证日期", "last_verified_at"));
        }
        if (verifiedBy == null || verifiedBy.isBlank()) {
            errors.add(LintIssue.error("E005", "发布时必须填写验证人", "verified_by"));
        }
    }
    
    private void checkChangeSummary(String changeSummary, List<LintIssue> errors) {
        if (changeSummary == null || changeSummary.isBlank()) {
            errors.add(LintIssue.error("E006", "发布时必须填写变更摘要", "change_summary"));
        }
    }
    
    // === Warning 检查方法 ===
    
    private void checkSelectStar(List<SqlBlock> sqlBlocks, List<LintIssue> warnings) {
        if (sqlBlocks == null) return;
        
        for (SqlBlock block : sqlBlocks) {
            if (block.getSql() != null && SELECT_STAR_PATTERN.matcher(block.getSql()).find()) {
                warnings.add(LintIssue.warning("W001",
                        "SQL 使用了 SELECT *，建议明确指定字段",
                        "content.sql_blocks[" + block.getBlockId() + "]",
                        block.getBlockId()));
            }
        }
    }
    
    private void checkPartitionCondition(List<SqlBlock> sqlBlocks, List<LintIssue> warnings) {
        if (sqlBlocks == null) return;
        
        for (SqlBlock block : sqlBlocks) {
            if (block.getSql() != null && !PARTITION_DATE_PATTERN.matcher(block.getSql()).find()) {
                warnings.add(LintIssue.warning("W002",
                        "SQL 没有分区/日期条件，可能存在性能风险",
                        "content.sql_blocks[" + block.getBlockId() + "]",
                        block.getBlockId()));
            }
        }
    }
    
    private void checkCaveatsEmpty(List<Caveat> caveats, List<LintIssue> warnings) {
        if (caveats == null || caveats.isEmpty()) {
            warnings.add(LintIssue.warning("W003",
                    "注意事项为空，建议补充常见坑点",
                    "content.caveats"));
        }
    }
    
    private void checkVerificationExpiry(LocalDateTime lastVerifiedAt, List<LintIssue> warnings) {
        if (lastVerifiedAt == null) return;
        
        long daysSince = ChronoUnit.DAYS.between(lastVerifiedAt.toLocalDate(), LocalDate.now());
        if (daysSince > 365) {
            warnings.add(LintIssue.warning("W004",
                    "最后验证日期距今 " + daysSince + " 天，建议重新复核",
                    "last_verified_at"));
        }
    }
    
    private void checkSensitiveTableNoFields(List<SourceTable> tables, List<SensitiveField> sensitiveFields,
                                              boolean hasSensitiveTable, List<LintIssue> warnings) {
        if (hasSensitiveTable && (sensitiveFields == null || sensitiveFields.isEmpty())) {
            warnings.add(LintIssue.warning("W006",
                    "已选表中存在敏感字段，但敏感字段清单为空",
                    "content.sensitive_fields"));
        }
    }
    
    /**
     * Lint 上下文
     */
    public record LintContext(
        boolean isPublishCheck,
        LocalDateTime lastVerifiedAt,
        String verifiedBy,
        String changeSummary,
        List<SourceTable> sourceTables,
        boolean hasSensitiveTable
    ) {
        public static LintContext forDraft(List<SourceTable> sourceTables, boolean hasSensitiveTable) {
            return new LintContext(false, null, null, null, sourceTables, hasSensitiveTable);
        }
        
        public static LintContext forPublish(LocalDateTime lastVerifiedAt, String verifiedBy,
                                              String changeSummary, List<SourceTable> sourceTables,
                                              boolean hasSensitiveTable) {
            return new LintContext(true, lastVerifiedAt, verifiedBy, changeSummary, 
                                   sourceTables, hasSensitiveTable);
        }
    }
}
