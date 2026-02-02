package com.caliberhub.domain.scene.model;

import com.caliberhub.domain.scene.valueobject.SceneStatus;
import com.caliberhub.domain.scene.valueobject.SceneVersionContent;
import com.caliberhub.infrastructure.common.exception.BusinessException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 场景 - 聚合根
 * 口径知识资产的最小治理单元
 * 
 * 设计原则（遵循行内规范）：
 * - 使用 @Builder 创建，禁止 public setter
 * - 所有状态变更通过业务语义化方法
 * - 实体创建后必须是业务完整的
 */
@Getter
@Builder
public class Scene {

    private final String id;
    private final String sceneCode;
    private String title;
    private final String domainId;
    
    private SceneStatus lifecycleStatus;
    
    private LocalDateTime deprecatedAt;
    private String deprecatedBy;
    private String deprecateReason;
    
    private SceneVersion currentDraft;
    private SceneVersion currentPublished;
    
    private final String createdBy;
    private final LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    
    // ===== 静态工厂方法 =====
    
    /**
     * 创建新场景（含初始草稿）
     */
    public static Scene create(String title, String domainId, String ownerUser, String operator) {
        return createInternal(null, title, domainId, ownerUser, operator);
    }

    /**
     * 创建新场景（允许指定场景编码，导入/迁移用）
     */
    public static Scene createWithCode(String sceneCode, String title, String domainId, String ownerUser, String operator) {
        if (sceneCode == null || sceneCode.isBlank()) {
            throw new BusinessException("场景编码不能为空");
        }
        return createInternal(sceneCode, title, domainId, ownerUser, operator);
    }

    private static String generateSceneCode() {
        // 格式：SCE-YYYYMMDD-XXXX
        String datePart = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", (int)(Math.random() * 10000));
        return "SCE-" + datePart + "-" + randomPart;
    }

    private static Scene createInternal(String sceneCode, String title, String domainId, String ownerUser, String operator) {
        if (title == null || title.isBlank()) {
            throw new BusinessException("场景标题不能为空");
        }
        if (domainId == null || domainId.isBlank()) {
            throw new BusinessException("必须选择所属领域");
        }

        LocalDateTime now = LocalDateTime.now();
        String sceneId = UUID.randomUUID().toString();
        String finalSceneCode = sceneCode != null && !sceneCode.isBlank() ? sceneCode : generateSceneCode();

        SceneVersion draft = SceneVersion.createDraft(sceneId, finalSceneCode, domainId, title, ownerUser, operator);

        return Scene.builder()
                .id(sceneId)
                .sceneCode(finalSceneCode)
                .title(title)
                .domainId(domainId)
                .lifecycleStatus(SceneStatus.ACTIVE)
                .currentDraft(draft)
                .createdBy(operator)
                .createdAt(now)
                .updatedBy(operator)
                .updatedAt(now)
                .build();
    }
    
    // ===== 业务行为 =====
    
    /**
     * 废弃场景
     */
    public void deprecate(String operator) {
        deprecate(null, operator);
    }
    
    /**
     * 废弃场景（带原因）
     */
    public void deprecate(String reason, String operator) {
        if (this.lifecycleStatus == SceneStatus.DEPRECATED) {
            throw new BusinessException("场景已废弃，不可重复操作");
        }
        this.lifecycleStatus = SceneStatus.DEPRECATED;
        this.deprecatedAt = LocalDateTime.now();
        this.deprecatedBy = operator;
        this.deprecateReason = reason;
        this.updatedBy = operator;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 重新激活
     */
    public void activate(String operator) {
        if (this.lifecycleStatus == SceneStatus.ACTIVE) {
            throw new BusinessException("场景已处于活跃状态");
        }
        this.lifecycleStatus = SceneStatus.ACTIVE;
        this.deprecatedAt = null;
        this.deprecatedBy = null;
        this.deprecateReason = null;
        this.updatedBy = operator;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 保存/更新草稿
     */
    public void saveDraft(String title, SceneVersionContent content, String operator) {
        if (this.lifecycleStatus == SceneStatus.DEPRECATED) {
            throw new BusinessException("已废弃的场景不能编辑");
        }
        
        if (this.currentDraft == null) {
            // 基于当前发布版本创建新草稿
            this.currentDraft = SceneVersion.createDraft(this.id, this.sceneCode, this.domainId, 
                    title != null ? title : this.title, null, operator);
        }
        
        if (title != null) {
            this.currentDraft.setTitle(title);
        }
        this.currentDraft.updateContent(content, operator);
        this.updatedBy = operator;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 发布（简化版，验证信息已设置到草稿中）
     */
    public SceneVersion publish(String operator) {
        if (this.lifecycleStatus == SceneStatus.DEPRECATED) {
            throw new BusinessException("已废弃的场景不能发布");
        }
        if (this.currentDraft == null) {
            throw new BusinessException("没有可发布的草稿");
        }
        
        // 计算下一个版本号
        int nextSeq = this.currentPublished == null ? 1 : this.currentPublished.getVersionSeq() + 1;
        
        // 发布草稿
        this.currentDraft.publishVersion(nextSeq, operator);
        
        // 将当前发布版本标记为非当前
        if (this.currentPublished != null) {
            this.currentPublished.markNotCurrent();
        }
        
        // 切换版本状态
        this.currentPublished = this.currentDraft;
        this.currentDraft = null;
        this.title = this.currentPublished.getTitle();
        this.updatedBy = operator;
        this.updatedAt = LocalDateTime.now();
        
        return this.currentPublished;
    }
    
    /**
     * 发布（完整版）
     */
    public SceneVersion publish(String changeSummary, LocalDateTime lastVerifiedAt, 
                                  String verifiedBy, String verifyEvidence, String operator) {
        if (this.lifecycleStatus == SceneStatus.DEPRECATED) {
            throw new BusinessException("已废弃的场景不能发布");
        }
        if (this.currentDraft == null) {
            throw new BusinessException("没有可发布的草稿");
        }
        
        // 设置验证信息
        this.currentDraft.setChangeSummary(changeSummary);
        this.currentDraft.setLastVerifiedAt(lastVerifiedAt);
        this.currentDraft.setVerifiedBy(verifiedBy);
        this.currentDraft.setVerifyEvidence(verifyEvidence);
        
        return publish(operator);
    }
    
    /**
     * 更新草稿Lint结果
     */
    public void updateDraftLintResult(boolean passed, int errors, int warnings) {
        if (this.currentDraft == null) {
            throw new BusinessException("没有草稿可更新Lint结果");
        }
        this.currentDraft.updateLintResult(passed, errors, warnings);
    }
    
    // ===== 查询方法 =====
    
    /**
     * 是否可发布
     */
    public boolean canPublish() {
        return this.lifecycleStatus == SceneStatus.ACTIVE 
                && this.currentDraft != null 
                && this.currentDraft.isLintPassed();
    }
    
    /**
     * 是否有未发布的草稿
     */
    public boolean hasDraft() {
        return this.currentDraft != null;
    }
    
    /**
     * 获取当前版本（草稿或已发布）
     */
    public SceneVersion getCurrentVersion() {
        return this.currentDraft != null ? this.currentDraft : this.currentPublished;
    }
    
    /**
     * 获取版本历史
     */
    public java.util.List<SceneVersion> getVersionHistory() {
        java.util.List<SceneVersion> history = new java.util.ArrayList<>();
        if (this.currentDraft != null) {
            history.add(this.currentDraft);
        }
        if (this.currentPublished != null) {
            history.add(this.currentPublished);
        }
        // TODO: 从数据库加载历史版本
        return history;
    }
}
