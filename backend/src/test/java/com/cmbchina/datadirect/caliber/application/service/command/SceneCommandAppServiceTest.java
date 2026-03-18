package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.PublishSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.assembler.SceneAssembler;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.domain.model.Scene;
import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.domain.support.CaliberDomainSupport;
import com.cmbchina.datadirect.caliber.domain.support.SceneDomainSupport;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneAuditLogMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneAuditLogPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class SceneCommandAppServiceTest {

    @Mock
    private SceneDomainSupport sceneDomainSupport;

    @Mock
    private CaliberDomainSupport caliberDomainSupport;

    @Mock
    private SceneAssembler sceneAssembler;

    @Mock
    private AlignmentReportAppService alignmentReportAppService;

    @Mock
    private SceneAuditLogMapper sceneAuditLogMapper;

    private SceneCommandAppService sceneCommandAppService;

    @BeforeEach
    void setUp() {
        CaliberDictSyncService noopDictSyncService = new CaliberDictSyncService(null, null) {
            @Override
            public void syncFromScene(Long sceneId, Long domainId, String codeMappingsJson) {
                // no-op for unit test
            }
        };
        sceneCommandAppService = new SceneCommandAppService(
                sceneDomainSupport,
                caliberDomainSupport,
                sceneAssembler,
                noopDictSyncService,
                alignmentReportAppService,
                new SimpleMeterRegistry(),
                new ObjectMapper(),
                sceneAuditLogMapper
        );
    }

    @Test
    void shouldCreateDraftScene() {
        CreateSceneCmd cmd = new CreateSceneCmd("客户查询", null, "零售金融", "raw", "tester");
        OffsetDateTime now = OffsetDateTime.now();
        Scene savedScene = Scene.builder()
                .id(1L)
                .sceneCode("SCN-TEST000001")
                .sceneTitle("客户查询")
                .domain("零售金融")
                .status(SceneStatus.DRAFT)
                .createdBy("tester")
                .createdAt(now)
                .updatedAt(now)
                .build();
        SceneDTO dto = new SceneDTO(
                1L,
                "SCN-TEST000001",
                "客户查询",
                null,
                "零售金融",
                "零售金融",
                "DRAFT",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "raw",
                null,
                null,
                "tester",
                now,
                now,
                null,
                null,
                0L
        );

        when(sceneDomainSupport.save(any(Scene.class))).thenReturn(savedScene);
        when(sceneAssembler.toDTO(savedScene)).thenReturn(dto);

        SceneDTO result = sceneCommandAppService.create(cmd);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo("DRAFT");
        verify(sceneDomainSupport).save(any(Scene.class));
    }

    @Test
    void shouldThrowWhenPublishingMissingScene() {
        when(sceneDomainSupport.findById(99L)).thenReturn(Optional.empty());
        PublishSceneCmd cmd = new PublishSceneCmd(OffsetDateTime.now(), "发布说明", "reviewer");

        assertThatThrownBy(() -> sceneCommandAppService.publish(99L, cmd))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("scene not found");
    }

    @Test
    void shouldPublishWithSoftGateWarningWhenIdMappingNotesMissing() {
        OffsetDateTime now = OffsetDateTime.now();
        Scene scene = Scene.builder()
                .id(7L)
                .sceneCode("SCN-WARN000001")
                .sceneTitle("发布软门禁测试")
                .domainId(1L)
                .domain("零售金融")
                .status(SceneStatus.DRAFT)
                .sceneDescription("用于测试发布")
                .sqlVariantsJson("[{\"sql_text\":\"select 1\"}]")
                .codeMappingsJson("[{\"code\":\"AGN_STS_CD\",\"mappings\":[{\"value_code\":\"01\",\"value_name\":\"成功\"}]}]")
                .createdBy("tester")
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(sceneDomainSupport.findById(7L)).thenReturn(Optional.of(scene));
        when(sceneDomainSupport.save(any(Scene.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sceneAssembler.toDTO(any(Scene.class))).thenReturn(new SceneDTO(
                7L, "SCN-WARN000001", "发布软门禁测试",
                1L, "零售金融", "零售金融", "PUBLISHED",
                "用于测试发布", null, null, null,
                null, null, "[{\"sql_text\":\"select 1\"}]", "[{\"code\":\"AGN_STS_CD\"}]",
                null, null, null, null, null, null, null,
                now, "发布", "tester", now, now, "reviewer", now, 1L
        ));
        doNothing().when(alignmentReportAppService).assertPublishAllowed(eq(7L));

        sceneCommandAppService.publish(7L, new PublishSceneCmd(now, "发布", "reviewer"));

        ArgumentCaptor<SceneAuditLogPO> captor = ArgumentCaptor.forClass(SceneAuditLogPO.class);
        verify(sceneAuditLogMapper).save(captor.capture());
        assertThat(captor.getValue().getDetailJson()).contains("QG-102:id_mapping_notes_missing");
    }
}
