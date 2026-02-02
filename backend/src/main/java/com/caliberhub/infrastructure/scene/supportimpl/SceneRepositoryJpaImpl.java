package com.caliberhub.infrastructure.scene.supportimpl;

import com.caliberhub.domain.scene.model.Scene;
import com.caliberhub.domain.scene.model.SceneVersion;
import com.caliberhub.domain.scene.support.SceneRepository;
import com.caliberhub.domain.scene.valueobject.SceneStatus;
import com.caliberhub.domain.scene.valueobject.VersionStatus;
import com.caliberhub.domain.scene.valueobject.SceneVersionContent;
import com.caliberhub.infrastructure.scene.dao.mapper.SceneMapper;
import com.caliberhub.infrastructure.scene.dao.mapper.SceneVersionMapper;
import com.caliberhub.infrastructure.scene.dao.po.ScenePO;
import com.caliberhub.infrastructure.scene.dao.po.SceneVersionPO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 基于 JPA 的场景仓储实现（落库 SQLite）
 */
@Slf4j
@Primary
@Repository
@RequiredArgsConstructor
public class SceneRepositoryJpaImpl implements SceneRepository {

    private final SceneMapper sceneMapper;
    private final SceneVersionMapper sceneVersionMapper;
    private final ObjectMapper objectMapper;

    // 支持 ISO 和 SQLite 默认格式
    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .appendLiteral(' ')
            .appendPattern("HH:mm:ss")
            .toFormatter();
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private LocalDateTime parseDateTime(String str) {
        if (str == null) return null;
        try {
            return LocalDateTime.parse(str, ISO_FORMATTER);
        } catch (Exception e) {
            return LocalDateTime.parse(str, FORMATTER);
        }
    }

    private String formatDateTime(LocalDateTime time) {
        return time != null ? time.format(ISO_FORMATTER) : null;
    }

    @Override
    public Optional<Scene> findById(String id) {
        return sceneMapper.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Scene> findBySceneCode(String sceneCode) {
        return sceneMapper.findBySceneCode(sceneCode).map(this::toDomain);
    }

    @Override
    public List<Scene> findByDomainId(String domainId) {
        return sceneMapper.findByDomainId(domainId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Scene> findAllActive() {
        return sceneMapper.findByLifecycleStatus(SceneStatus.ACTIVE.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void save(Scene scene) {
        ScenePO po = toPO(scene);
        sceneMapper.save(po);

        // 处理版本：先保存草稿，再保存发布版本（如有）
        SceneVersion draft = scene.getCurrentDraft();
        SceneVersion published = scene.getCurrentPublished();

        if (draft != null) {
            sceneVersionMapper.save(toVersionPO(draft, po.getSceneCode()));
        } else {
            // 若没有草稿，删除当前草稿记录
            sceneVersionMapper.deleteCurrentDraft(scene.getId());
        }

        if (published != null) {
            // 先清除旧的 current 标记
            sceneVersionMapper.markAllPublishedNotCurrent(scene.getId());
            sceneVersionMapper.save(toVersionPO(published, po.getSceneCode()));
        }
    }

    @Override
    public void delete(String id) {
        sceneMapper.deleteById(id);
    }

    @Override
    public boolean existsBySceneCode(String sceneCode) {
        return sceneMapper.existsBySceneCode(sceneCode);
    }

    // ========== 映射 ==========

    private Scene toDomain(ScenePO po) {
        // 加载当前草稿、已发布版本
        Optional<SceneVersionPO> draftPO = sceneVersionMapper.findCurrentDraft(po.getId());
        Optional<SceneVersionPO> publishedPO = sceneVersionMapper.findCurrentPublished(po.getId());

        SceneVersion draft = draftPO.map(v -> toVersion(v, po.getSceneCode())).orElse(null);
        SceneVersion published = publishedPO.map(v -> toVersion(v, po.getSceneCode())).orElse(null);

        return Scene.builder()
                .id(po.getId())
                .sceneCode(po.getSceneCode())
                .title(po.getTitle())
                .domainId(po.getDomainId())
                .lifecycleStatus(SceneStatus.valueOf(po.getLifecycleStatus()))
                .deprecatedAt(parseDateTime(po.getDeprecatedAt()))
                .deprecatedBy(po.getDeprecatedBy())
                .deprecateReason(po.getDeprecateReason())
                .currentDraft(draft)
                .currentPublished(published)
                .createdBy(po.getCreatedBy())
                .createdAt(parseDateTime(po.getCreatedAt()))
                .updatedBy(po.getUpdatedBy())
                .updatedAt(parseDateTime(po.getUpdatedAt()))
                .build();
    }

    private ScenePO toPO(Scene scene) {
        return ScenePO.builder()
                .id(scene.getId())
                .sceneCode(scene.getSceneCode())
                .title(scene.getTitle())
                .domainId(scene.getDomainId())
                .lifecycleStatus(scene.getLifecycleStatus().name())
                .deprecatedAt(formatDateTime(scene.getDeprecatedAt()))
                .deprecatedBy(scene.getDeprecatedBy())
                .deprecateReason(scene.getDeprecateReason())
                .createdBy(scene.getCreatedBy())
                .createdAt(formatDateTime(scene.getCreatedAt()))
                .updatedBy(scene.getUpdatedBy())
                .updatedAt(formatDateTime(scene.getUpdatedAt()))
                .build();
    }

    private SceneVersion toVersion(SceneVersionPO po, String sceneCode) {
        List<String> tags = readList(po.getTagsJson());
        List<String> contributors = readList(po.getContributorsJson());

        SceneVersionContent content = readContent(po.getContentJson());
        var lintMeta = readLintMeta(po.getLintJson());

        return SceneVersion.builder()
                .id(po.getId())
                .sceneId(po.getSceneId())
                .sceneCode(sceneCode)
                .domainId(po.getDomainId())
                .status(VersionStatus.valueOf(po.getStatus()))
                .isCurrent(po.getIsCurrent() != null && po.getIsCurrent() == 1)
                .versionSeq(po.getVersionSeq())
                .versionLabel(po.getVersionLabel())
                .title(po.getTitle())
                .tags(tags)
                .ownerUser(po.getOwnerUser())
                .contributors(contributors)
                .hasSensitive(po.getHasSensitive() != null && po.getHasSensitive() == 1)
                .lastVerifiedAt(parseDateTime(po.getLastVerifiedAt()))
                .verifiedBy(po.getVerifiedBy())
                .verifyEvidence(po.getVerifyEvidence())
                .changeSummary(po.getChangeSummary())
                .publishedBy(po.getPublishedBy())
                .publishedAt(parseDateTime(po.getPublishedAt()))
                .content(content)
                .lintPassed(lintMeta.passed())
                .errorCount(lintMeta.errorCount())
                .warningCount(lintMeta.warningCount())
                .createdBy(po.getCreatedBy())
                .createdAt(parseDateTime(po.getCreatedAt()))
                .updatedBy(po.getUpdatedBy())
                .updatedAt(parseDateTime(po.getUpdatedAt()))
                .build();
    }

    private SceneVersionPO toVersionPO(SceneVersion version, String sceneCode) {
        String tagsJson = writeValue(defaultList(version.getTags()));
        String contributorsJson = writeValue(defaultList(version.getContributors()));
        String contentJson = writeValue(version.getContent() != null ? version.getContent() : SceneVersionContent.empty());
        String lintJson = writeValue(LintMeta.of(version.isLintPassed(), version.getErrorCount(), version.getWarningCount()));

        return SceneVersionPO.builder()
                .id(version.getId())
                .sceneId(version.getSceneId())
                .domainId(version.getDomainId())
                .status(version.getStatus().name())
                .isCurrent(version.isCurrent() ? 1 : 0)
                .versionSeq(version.getVersionSeq())
                .versionLabel(version.getVersionLabel())
                .title(version.getTitle())
                .tagsJson(tagsJson)
                .ownerUser(version.getOwnerUser())
                .contributorsJson(contributorsJson)
                .hasSensitive(version.isHasSensitive() ? 1 : 0)
                .lastVerifiedAt(formatDateTime(version.getLastVerifiedAt()))
                .verifiedBy(version.getVerifiedBy())
                .verifyEvidence(version.getVerifyEvidence())
                .changeSummary(version.getChangeSummary())
                .publishedBy(version.getPublishedBy())
                .publishedAt(formatDateTime(version.getPublishedAt()))
                .contentJson(contentJson)
                .lintJson(lintJson)
                .createdBy(version.getCreatedBy())
                .createdAt(formatDateTime(version.getCreatedAt()))
                .updatedBy(version.getUpdatedBy())
                .updatedAt(formatDateTime(version.getUpdatedAt()))
                .build();
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析列表失败，返回空列表: {}", e.getMessage());
            return List.of();
        }
    }

    private SceneVersionContent readContent(String json) {
        if (json == null || json.isBlank()) {
            return SceneVersionContent.empty();
        }
        try {
            return objectMapper.readValue(json, SceneVersionContent.class);
        } catch (Exception e) {
            log.warn("解析 SceneVersionContent 失败，返回空内容: {}", e.getMessage());
            return SceneVersionContent.empty();
        }
    }

    private String writeValue(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON 序列化失败: {}", e.getMessage());
            return "{}";
        }
    }

    private record LintMeta(boolean passed, int errorCount, int warningCount) {
        static LintMeta of(boolean passed, int errorCount, int warningCount) {
            return new LintMeta(passed, errorCount, warningCount);
        }
    }

    private LintMeta readLintMeta(String json) {
        if (json == null || json.isBlank()) {
            return LintMeta.of(false, 0, 0);
        }
        try {
            var map = objectMapper.readValue(json, new TypeReference<java.util.Map<String, Object>>() {});
            boolean passed = Boolean.TRUE.equals(map.get("passed"));
            int errors = map.get("errorCount") instanceof Number n ? n.intValue() : 0;
            int warnings = map.get("warningCount") instanceof Number n ? n.intValue() : 0;
            return LintMeta.of(passed, errors, warnings);
        } catch (Exception e) {
            return LintMeta.of(false, 0, 0);
        }
    }

    private List<String> defaultList(List<String> list) {
        return list != null ? list : new ArrayList<>();
    }
}
