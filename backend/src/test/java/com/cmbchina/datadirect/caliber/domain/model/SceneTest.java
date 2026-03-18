package com.cmbchina.datadirect.caliber.domain.model;

import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SceneTest {

    @Test
    void shouldCreateDraftWithDefaultStatus() {
        Scene scene = Scene.createDraft("零售客户信息查询", "零售金融", "raw-text", "tester");

        assertThat(scene.getSceneCode()).startsWith("SCN-");
        assertThat(scene.getStatus()).isEqualTo(SceneStatus.DRAFT);
        assertThat(scene.getSceneTitle()).isEqualTo("零售客户信息查询");
        assertThat(scene.getDomain()).isEqualTo("零售金融");
        assertThat(scene.getCreatedBy()).isEqualTo("tester");
        assertThat(scene.getCreatedAt()).isNotNull();
        assertThat(scene.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRejectPublishWhenMandatoryFieldsMissing() {
        Scene scene = Scene.createDraft("零售客户信息查询", "零售金融", "raw-text", "tester");

        assertThatThrownBy(() -> scene.publish(null, "变更说明", "tester"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("verifiedAt");

        assertThatThrownBy(() -> scene.publish(OffsetDateTime.now(), " ", "tester"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("changeSummary");
    }

    @Test
    void shouldPublishDraftSuccessfully() {
        Scene scene = Scene.createDraft("零售客户信息查询", "零售金融", "raw-text", "tester");
        OffsetDateTime verifiedAt = OffsetDateTime.now().minusDays(1);

        scene.publish(verifiedAt, "首次发布", "reviewer");

        assertThat(scene.getStatus()).isEqualTo(SceneStatus.PUBLISHED);
        assertThat(scene.getVerifiedAt()).isEqualTo(verifiedAt);
        assertThat(scene.getChangeSummary()).isEqualTo("首次发布");
        assertThat(scene.getPublishedBy()).isEqualTo("reviewer");
        assertThat(scene.getPublishedAt()).isNotNull();
    }

    @Test
    void shouldRejectUpdateWhenSceneAlreadyPublished() {
        Scene scene = Scene.createDraft("零售客户信息查询", "零售金融", "raw-text", "tester");
        scene.publish(OffsetDateTime.now(), "首次发布", "reviewer");

        SceneDraftUpdate update = new SceneDraftUpdate(
                "零售客户信息查询-更新",
                null,
                "零售金融",
                "desc",
                "definition",
                "",
                "",
                "[]",
                "[]",
                "[]",
                "[]",
                "tester",
                "[]",
                "[]",
                "[]",
                "",
                "{}",
                "raw");

        assertThatThrownBy(() -> scene.updateDraft(update, "editor"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("read-only");
    }
}
