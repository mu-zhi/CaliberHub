package com.cmbchina.datadirect.caliber.application.service.query;

import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportTaskDTO;
import com.cmbchina.datadirect.caliber.application.service.command.ImportCandidateGraphAssembler;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportTaskMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportTaskPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportTaskQueryAppServiceTest {

    @Mock
    private ImportTaskMapper importTaskMapper;

    @Mock
    private SceneQueryAppService sceneQueryAppService;

    @Test
    void shouldBackfillCandidateGraphForLegacyTaskPayload() {
        ImportTaskPO task = new ImportTaskPO();
        task.setTaskId("task-1");
        task.setMaterialId("material-1");
        task.setStatus("QUALITY_REVIEWING");
        task.setCurrentStep(2);
        task.setSourceType("PASTE_MD");
        task.setSourceName("legacy.md");
        task.setRawText("### 场景标题：按协议号查询代发明细");
        task.setQualityConfirmed(false);
        task.setCompareConfirmed(false);
        task.setCreatedAt(OffsetDateTime.now());
        task.setUpdatedAt(OffsetDateTime.now());
        task.setPreprocessResultJson("""
                {
                  "mode": "rule_generated",
                  "scenes": [
                    {
                      "scene_title": "按协议号查询代发明细",
                      "quality": {"confidence": 0.85},
                      "inputs": {
                        "params": [
                          {"name_zh": "协议号"}
                        ]
                      },
                      "outputs": {
                        "fields": [
                          {"display_name": "交易金额"}
                        ]
                      },
                      "sql_variants": [
                        {
                          "source_tables": ["PDM_VHIS.T05_AGN_DTL"]
                        }
                      ]
                    }
                  ],
                  "warnings": []
                }
                """);
        when(importTaskMapper.findById("task-1")).thenReturn(Optional.of(task));

        ImportTaskQueryAppService service = new ImportTaskQueryAppService(
                importTaskMapper,
                new ObjectMapper(),
                sceneQueryAppService,
                new ImportCandidateGraphAssembler()
        );

        ImportTaskDTO dto = service.getByTaskId("task-1");

        assertThat(dto.preprocessResult()).isNotNull();
        assertThat(dto.preprocessResult().path("candidateGraph").path("nodes").isArray()).isTrue();
        assertThat(dto.preprocessResult().path("candidateGraph").path("nodes")).hasSize(4);
        assertThat(dto.preprocessResult().path("candidateGraph").path("edges")).hasSize(3);
    }

    @Test
    void shouldBackfillCandidateGraphWhenLegacyGraphShellIsEmpty() {
        ImportTaskPO task = new ImportTaskPO();
        task.setTaskId("task-2");
        task.setMaterialId("material-2");
        task.setStatus("QUALITY_REVIEWING");
        task.setCurrentStep(2);
        task.setSourceType("PASTE_MD");
        task.setSourceName("legacy-shell.md");
        task.setRawText("### 场景标题：按协议号查询代发明细");
        task.setCreatedAt(OffsetDateTime.now());
        task.setUpdatedAt(OffsetDateTime.now());
        task.setPreprocessResultJson("""
                {
                  "mode": "rule_generated",
                  "candidateGraph": {
                    "nodes": [],
                    "edges": []
                  },
                  "scenes": [
                    {
                      "scene_title": "按协议号查询代发明细",
                      "quality": {"confidence": 0.85},
                      "inputs": {
                        "params": [
                          {"name_zh": "协议号"}
                        ]
                      },
                      "outputs": {
                        "fields": [
                          {"display_name": "交易金额"}
                        ]
                      },
                      "sql_variants": [
                        {
                          "source_tables": ["PDM_VHIS.T05_AGN_DTL"]
                        }
                      ]
                    }
                  ],
                  "warnings": []
                }
                """);
        when(importTaskMapper.findById("task-2")).thenReturn(Optional.of(task));

        ImportTaskQueryAppService service = new ImportTaskQueryAppService(
                importTaskMapper,
                new ObjectMapper(),
                sceneQueryAppService,
                new ImportCandidateGraphAssembler()
        );

        ImportTaskDTO dto = service.getByTaskId("task-2");

        assertThat(dto.preprocessResult()).isNotNull();
        assertThat(dto.preprocessResult().path("candidateGraph").path("nodes")).hasSize(4);
        assertThat(dto.preprocessResult().path("candidateGraph").path("edges")).hasSize(3);
    }
}
