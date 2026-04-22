package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneVersionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ExperimentalRetrievalIndexManifestMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneVersionPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.ExperimentalRetrievalIndexManifestPO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ExperimentalRetrievalIndexSyncService {

    private final ExperimentalRetrievalIndexManifestMapper experimentalRetrievalIndexManifestMapper;
    private final SceneVersionMapper sceneVersionMapper;
    private final ObjectMapper objectMapper;

    public ExperimentalRetrievalIndexSyncService(ExperimentalRetrievalIndexManifestMapper experimentalRetrievalIndexManifestMapper,
                                                 SceneVersionMapper sceneVersionMapper,
                                                 ObjectMapper objectMapper) {
        this.experimentalRetrievalIndexManifestMapper = experimentalRetrievalIndexManifestMapper;
        this.sceneVersionMapper = sceneVersionMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ExperimentalRetrievalIndexManifestPO syncPublishedSnapshot(Long sceneId,
                                                                      Long snapshotId,
                                                                      String sceneCode,
                                                                      String versionTag,
                                                                      String snapshotSummaryJson,
                                                                      String operator) {
        return syncSnapshotManifest(sceneId, snapshotId, sceneCode, versionTag, "PUBLISHED", snapshotSummaryJson, operator);
    }

    @Transactional
    public ExperimentalRetrievalIndexManifestPO syncSnapshotManifest(Long sceneId,
                                                                     Long snapshotId,
                                                                     String sceneCode,
                                                                     String versionTag,
                                                                     String sourceStatus,
                                                                     String snapshotSummaryJson,
                                                                     String operator) {
        String normalizedStatus = sourceStatus == null ? "" : sourceStatus.trim().toUpperCase(Locale.ROOT);
        if (!"PUBLISHED".equals(normalizedStatus)) {
            throw new IllegalStateException("experimental retrieval index only accepts PUBLISHED snapshots");
        }
        OffsetDateTime now = OffsetDateTime.now();
        ExperimentalRetrievalIndexManifestPO manifest = experimentalRetrievalIndexManifestMapper
                .findBySceneIdAndSnapshotId(sceneId, snapshotId)
                .orElseGet(ExperimentalRetrievalIndexManifestPO::new);
        if (manifest.getId() == null) {
            manifest.setCreatedAt(now);
        }
        String fallbackIndexVersion = experimentalRetrievalIndexManifestMapper.findBySceneIdOrderByCreatedAtDesc(sceneId).stream()
                .filter(item -> item.getSnapshotId() != null && !item.getSnapshotId().equals(snapshotId))
                .map(ExperimentalRetrievalIndexManifestPO::getIndexVersion)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);

        manifest.setSceneId(sceneId);
        manifest.setSnapshotId(snapshotId);
        manifest.setSceneCode(sceneCode == null || sceneCode.isBlank() ? "SCENE-" + sceneId : sceneCode.trim());
        manifest.setVersionTag(versionTag);
        manifest.setIndexVersion(buildIndexVersion(manifest.getSceneCode(), versionTag, snapshotId));
        manifest.setFallbackIndexVersion(fallbackIndexVersion);
        manifest.setSourceStatus("PUBLISHED");
        manifest.setManifestStatus("ACTIVE");
        manifest.setDraftLeakCount(draftLeakCount(snapshotSummaryJson));
        manifest.setSummaryJson(snapshotSummaryJson);
        manifest.setFailureReason(null);
        manifest.setCreatedBy(operator == null || operator.isBlank() ? "system" : operator.trim());
        manifest.setUpdatedAt(now);
        return experimentalRetrievalIndexManifestMapper.save(manifest);
    }

    @Transactional
    public ExperimentalRetrievalIndexManifestPO ensureSnapshotLock(Long sceneId,
                                                                   Long snapshotId,
                                                                   String sceneCode,
                                                                   String operator) {
        return experimentalRetrievalIndexManifestMapper.findBySceneIdAndSnapshotId(sceneId, snapshotId)
                .orElseGet(() -> {
                    SceneVersionPO snapshot = sceneVersionMapper.findById(snapshotId)
                            .orElseThrow(() -> new IllegalStateException("snapshot not found: " + snapshotId));
                    return syncSnapshotManifest(
                            sceneId,
                            snapshotId,
                            sceneCode,
                            snapshot.getVersionTag(),
                            snapshot.getPublishStatus(),
                            snapshot.getSnapshotSummaryJson(),
                            operator
                    );
                });
    }

    @Transactional(readOnly = true)
    public Optional<String> resolveLockedIndexVersion(Long sceneId, Long snapshotId) {
        return experimentalRetrievalIndexManifestMapper.findBySceneIdAndSnapshotId(sceneId, snapshotId)
                .filter(item -> "ACTIVE".equalsIgnoreCase(item.getManifestStatus()))
                .map(ExperimentalRetrievalIndexManifestPO::getIndexVersion);
    }

    @Transactional(readOnly = true)
    public List<ExperimentalRetrievalIndexManifestPO> listSceneManifests(Long sceneId) {
        return experimentalRetrievalIndexManifestMapper.findBySceneIdOrderByCreatedAtDesc(sceneId);
    }

    private String buildIndexVersion(String sceneCode, String versionTag, Long snapshotId) {
        String normalizedSceneCode = sceneCode == null || sceneCode.isBlank() ? "SCENE" : sceneCode.trim();
        String normalizedVersionTag = versionTag == null || versionTag.isBlank() ? "SNAPSHOT" + snapshotId : versionTag.trim();
        return normalizedSceneCode + "::" + normalizedVersionTag + "::" + snapshotId;
    }

    private int draftLeakCount(String summaryJson) {
        if (summaryJson == null || summaryJson.isBlank()) {
            return 0;
        }
        try {
            JsonNode root = objectMapper.readTree(summaryJson);
            String sceneStatus = root.path("sceneStatus").asText("");
            return "PUBLISHED".equalsIgnoreCase(sceneStatus) ? 0 : 1;
        } catch (Exception ex) {
            return 0;
        }
    }
}
