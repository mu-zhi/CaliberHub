package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneVersionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ExperimentalRetrievalIndexManifestMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneVersionPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.ExperimentalRetrievalIndexManifestPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        CanonicalEntityTestApplication.class,
        ExperimentalRetrievalIndexSyncServiceTest.TestConfiguration.class
})
@ActiveProfiles("test")
@Transactional
class ExperimentalRetrievalIndexSyncServiceTest {

    @Autowired
    private ExperimentalRetrievalIndexSyncService experimentalRetrievalIndexSyncService;

    @Autowired
    private ExperimentalRetrievalIndexManifestMapper experimentalRetrievalIndexManifestMapper;

    @Autowired
    private SceneVersionMapper sceneVersionMapper;

    @Test
    void shouldRejectNonPublishedSnapshotManifest() {
        assertThatThrownBy(() -> experimentalRetrievalIndexSyncService.syncSnapshotManifest(
                11L,
                101L,
                "SCN-RAG-LOCK-001",
                "SCN-RAG-LOCK-001-V001",
                "DRAFT",
                """
                        {"sceneStatus":"DRAFT","planCount":1}
                        """,
                "tester"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUBLISHED");
    }

    @Test
    void shouldLockUniqueIndexVersionPerSnapshotId() {
        ExperimentalRetrievalIndexManifestPO firstManifest = experimentalRetrievalIndexSyncService.syncPublishedSnapshot(
                21L,
                201L,
                "SCN-RAG-LOCK-021",
                "SCN-RAG-LOCK-021-V001",
                """
                        {"sceneStatus":"PUBLISHED","planCount":1,"evidenceCount":2}
                        """,
                "tester"
        );
        ExperimentalRetrievalIndexManifestPO secondManifest = experimentalRetrievalIndexSyncService.syncPublishedSnapshot(
                21L,
                202L,
                "SCN-RAG-LOCK-021",
                "SCN-RAG-LOCK-021-V002",
                """
                        {"sceneStatus":"PUBLISHED","planCount":2,"evidenceCount":3}
                        """,
                "tester"
        );

        assertThat(firstManifest.getSnapshotId()).isEqualTo(201L);
        assertThat(firstManifest.getSourceStatus()).isEqualTo("PUBLISHED");
        assertThat(firstManifest.getDraftLeakCount()).isZero();

        assertThat(secondManifest.getSnapshotId()).isEqualTo(202L);
        assertThat(secondManifest.getSourceStatus()).isEqualTo("PUBLISHED");
        assertThat(secondManifest.getDraftLeakCount()).isZero();
        assertThat(secondManifest.getFallbackIndexVersion()).isEqualTo(firstManifest.getIndexVersion());

        assertThat(experimentalRetrievalIndexSyncService.resolveLockedIndexVersion(21L, 201L))
                .contains(firstManifest.getIndexVersion());
        assertThat(experimentalRetrievalIndexSyncService.resolveLockedIndexVersion(21L, 202L))
                .contains(secondManifest.getIndexVersion());
        assertThat(experimentalRetrievalIndexSyncService.resolveLockedIndexVersion(21L, 999L)).isEmpty();

        assertThat(experimentalRetrievalIndexManifestMapper.findBySceneIdAndSnapshotId(21L, 201L))
                .get()
                .extracting(ExperimentalRetrievalIndexManifestPO::getManifestStatus)
                .isEqualTo("ACTIVE");
        assertThat(experimentalRetrievalIndexManifestMapper.findBySceneIdAndSnapshotId(21L, 202L))
                .get()
                .extracting(ExperimentalRetrievalIndexManifestPO::getManifestStatus)
                .isEqualTo("ACTIVE");
    }

    @Test
    void shouldBackfillLockedManifestFromPublishedSnapshotRecord() {
        SceneVersionPO snapshot = new SceneVersionPO();
        snapshot.setSceneId(31L);
        snapshot.setVersionNo(1);
        snapshot.setVersionTag("SCN-RAG-LOCK-031-V001");
        snapshot.setSnapshotJson("{}");
        snapshot.setSnapshotSummaryJson("""
                {"sceneStatus":"PUBLISHED","planCount":1,"evidenceCount":1}
                """);
        snapshot.setChangeSummary("首次发布");
        snapshot.setPublishStatus("PUBLISHED");
        snapshot.setPublishedBy("tester");
        snapshot.setPublishedAt(OffsetDateTime.now());
        snapshot.setCreatedBy("tester");
        snapshot.setCreatedAt(OffsetDateTime.now());
        SceneVersionPO saved = sceneVersionMapper.save(snapshot);

        ExperimentalRetrievalIndexManifestPO manifest = experimentalRetrievalIndexSyncService.ensureSnapshotLock(
                31L,
                saved.getId(),
                "SCN-RAG-LOCK-031",
                "tester"
        );

        assertThat(manifest.getSnapshotId()).isEqualTo(saved.getId());
        assertThat(manifest.getSourceStatus()).isEqualTo("PUBLISHED");
        assertThat(manifest.getDraftLeakCount()).isZero();
        assertThat(experimentalRetrievalIndexSyncService.resolveLockedIndexVersion(31L, saved.getId()))
                .contains(manifest.getIndexVersion());
    }

    static class TestConfiguration {

        @Bean
        @Primary
        ExperimentalRetrievalIndexSyncService experimentalRetrievalIndexSyncService(
                ExperimentalRetrievalIndexManifestMapper experimentalRetrievalIndexManifestMapper,
                SceneVersionMapper sceneVersionMapper,
                ObjectMapper objectMapper) {
            return new ExperimentalRetrievalIndexSyncService(
                    experimentalRetrievalIndexManifestMapper,
                    sceneVersionMapper,
                    objectMapper
            );
        }
    }
}
