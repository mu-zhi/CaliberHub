package com.caliberhub.domain.scene.model;

import com.caliberhub.domain.scene.valueobject.SceneVersionContent;
import com.caliberhub.domain.scene.valueobject.VersionStatus;
import com.caliberhub.infrastructure.common.exception.BusinessException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 场景版本 - 实体
 * 表示场景的一个内容快照（草稿或已发布）
 */
@Getter
@Builder
public class SceneVersion {

    private final String id;
    private final String sceneId;
    private String sceneCode;
    private final String domainId;

    private VersionStatus status;
    private boolean isCurrent;

    private int versionSeq;
    private String versionLabel;

    private String title;
    private List<String> tags;
    private String ownerUser;
    private List<String> contributors;

    private boolean hasSensitive;
    private LocalDateTime lastVerifiedAt;
    private String verifiedBy;
    private String verifyEvidence;
    private String changeSummary;

    private String publishedBy;
    private LocalDateTime publishedAt;

    private SceneVersionContent content;

    // Lint 结果
    private boolean lintPassed;
    private int errorCount;
    private int warningCount;

    // 导入元信息
    private String sourceType;
    private String sourceRaw;
    private String parseReportJson;
    private String importedBy;
    private LocalDateTime importedAt;

    private final String createdBy;
    private final LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    /**
     * 创建草稿
     */
    public static SceneVersion createDraft(String sceneId, String sceneCode, String domainId,
            String title, String ownerUser, String operator) {
        LocalDateTime now = LocalDateTime.now();
        return SceneVersion.builder()
                .id(UUID.randomUUID().toString())
                .sceneId(sceneId)
                .sceneCode(sceneCode)
                .domainId(domainId)
                .status(VersionStatus.DRAFT)
                .isCurrent(true)
                .versionSeq(0)
                .versionLabel("draft")
                .title(title)
                .tags(List.of())
                .ownerUser(ownerUser != null ? ownerUser : operator)
                .contributors(List.of())
                .hasSensitive(false)
                .content(SceneVersionContent.empty())
                .lintPassed(false)
                .errorCount(0)
                .warningCount(0)
                .createdBy(operator)
                .createdAt(now)
                .updatedBy(operator)
                .updatedAt(now)
                .build();
    }

    /**
     * 更新内容
     */
    public void updateContent(SceneVersionContent newContent, String operator) {
        if (this.status == VersionStatus.PUBLISHED) {
            throw new BusinessException("已发布版本不可修改");
        }
        this.content = newContent;
        this.hasSensitive = newContent.hasSensitiveFields();
        this.updatedBy = operator;
        this.updatedAt = LocalDateTime.now();
        // 内容变更后清除Lint状态
        this.lintPassed = false;
        this.errorCount = 0;
        this.warningCount = 0;
    }

    /**
     * 更新Lint结果
     */
    public void updateLintResult(boolean passed, int errors, int warnings) {
        this.lintPassed = passed;
        this.errorCount = errors;
        this.warningCount = warnings;
    }

    /**
     * 发布版本
     */
    public SceneVersion publish(int nextVersionSeq, String changeSummary,
            LocalDateTime lastVerifiedAt, String verifiedBy,
            String verifyEvidence, String operator) {
        if (this.status == VersionStatus.PUBLISHED) {
            throw new BusinessException("版本已发布");
        }
        if (!this.lintPassed) {
            throw new BusinessException("Lint检查未通过，不能发布");
        }
        if (lastVerifiedAt == null) {
            throw new BusinessException("必须填写最后验证日期");
        }

        LocalDateTime now = LocalDateTime.now();

        // 创建已发布版本（基于当前草稿的内容）
        return SceneVersion.builder()
                .id(UUID.randomUUID().toString())
                .sceneId(this.sceneId)
                .domainId(this.domainId)
                .status(VersionStatus.PUBLISHED)
                .isCurrent(true)
                .versionSeq(nextVersionSeq)
                .versionLabel("v" + nextVersionSeq + ".0")
                .title(this.title)
                .tags(this.tags)
                .ownerUser(this.ownerUser)
                .contributors(this.contributors)
                .hasSensitive(this.hasSensitive)
                .lastVerifiedAt(lastVerifiedAt)
                .verifiedBy(verifiedBy)
                .verifyEvidence(verifyEvidence)
                .changeSummary(changeSummary)
                .publishedBy(operator)
                .publishedAt(now)
                .content(this.content)
                .lintPassed(this.lintPassed)
                .errorCount(this.errorCount)
                .warningCount(this.warningCount)
                .createdBy(operator)
                .createdAt(now)
                .updatedBy(operator)
                .updatedAt(now)
                .build();
    }

    /**
     * 标记为非当前版本
     */
    public void markNotCurrent() {
        this.isCurrent = false;
    }

    /**
     * 发布版本（简化版，验证信息已通过 setter 设置）
     */
    public void publishVersion(int nextVersionSeq, String operator) {
        if (this.status == VersionStatus.PUBLISHED) {
            throw new BusinessException("版本已发布");
        }

        LocalDateTime now = LocalDateTime.now();
        this.status = VersionStatus.PUBLISHED;
        this.versionSeq = nextVersionSeq;
        this.versionLabel = "v" + nextVersionSeq + ".0";
        this.publishedBy = operator;
        this.publishedAt = now;
        this.isCurrent = true;
        this.updatedBy = operator;
        this.updatedAt = now;
    }

    // === Setter 方法 ===

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setOwnerUser(String ownerUser) {
        this.ownerUser = ownerUser;
    }

    public void setContributors(List<String> contributors) {
        this.contributors = contributors;
    }

    public void setContent(SceneVersionContent content) {
        this.content = content;
        if (content != null) {
            this.hasSensitive = content.hasSensitiveFields();
        }
    }

    public void setHasSensitive(boolean hasSensitive) {
        this.hasSensitive = hasSensitive;
    }

    public void setLastVerifiedAt(LocalDateTime lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public void setVerifyEvidence(String verifyEvidence) {
        this.verifyEvidence = verifyEvidence;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public void setExportDocJson(String docJson) {
        // 存储导出结果
    }

    public void setExportChunksJson(String chunksJson) {
        // 存储导出结果
    }

    // === 导入元信息 Setter ===

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public void setSourceRaw(String sourceRaw) {
        this.sourceRaw = sourceRaw;
    }

    public void setParseReportJson(String parseReportJson) {
        this.parseReportJson = parseReportJson;
    }

    public void setImportedBy(String importedBy) {
        this.importedBy = importedBy;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }
}
