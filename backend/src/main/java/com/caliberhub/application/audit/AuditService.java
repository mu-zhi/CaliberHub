package com.caliberhub.application.audit;

import com.caliberhub.domain.audit.AuditAction;
import com.caliberhub.domain.audit.AuditLog;
import com.caliberhub.domain.audit.AuditLogRepository;
import com.caliberhub.infrastructure.common.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 审计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    
    /**
     * 记录审计日志
     */
    public void log(String sceneId, String versionId, AuditAction action, String summary) {
        String actor = UserContextHolder.getCurrentUser();
        AuditLog auditLog = AuditLog.create(sceneId, versionId, action, actor, summary);
        auditLogRepository.save(auditLog);
        log.info("审计日志: {} - {} - {} - {}", actor, action, sceneId, summary);
    }
    
    /**
     * 记录带 diff 的审计日志
     */
    public void logWithDiff(String sceneId, String versionId, AuditAction action, 
                            String summary, Map<String, Object> diff) {
        String actor = UserContextHolder.getCurrentUser();
        AuditLog auditLog = AuditLog.createWithDiff(sceneId, versionId, action, actor, summary, diff);
        auditLogRepository.save(auditLog);
        log.info("审计日志: {} - {} - {} - {}", actor, action, sceneId, summary);
    }
    
    /**
     * 记录场景创建
     */
    public void logCreateScene(String sceneId, String versionId, String title) {
        log(sceneId, versionId, AuditAction.CREATE_SCENE, "创建场景: " + title);
    }
    
    /**
     * 记录草稿保存
     */
    public void logSaveDraft(String sceneId, String versionId) {
        log(sceneId, versionId, AuditAction.SAVE_DRAFT, "保存草稿");
    }
    
    /**
     * 记录 Lint 运行
     */
    public void logRunLint(String sceneId, String versionId, boolean passed, int errorCount, int warningCount) {
        String summary = String.format("运行校验: %s (错误: %d, 警告: %d)", 
                passed ? "通过" : "未通过", errorCount, warningCount);
        log(sceneId, versionId, AuditAction.RUN_LINT, summary);
    }
    
    /**
     * 记录发布
     */
    public void logPublish(String sceneId, String versionId, String versionLabel, String changeSummary) {
        String summary = String.format("发布版本 %s: %s", versionLabel, changeSummary);
        log(sceneId, versionId, AuditAction.PUBLISH, summary);
    }
    
    /**
     * 记录废弃
     */
    public void logDeprecate(String sceneId, String reason) {
        log(sceneId, null, AuditAction.DEPRECATE, "废弃场景: " + reason);
    }
    
    /**
     * 记录导出
     */
    public void logExport(String sceneId, String versionId, String exportType) {
        log(sceneId, versionId, AuditAction.EXPORT, "导出: " + exportType);
    }
}
