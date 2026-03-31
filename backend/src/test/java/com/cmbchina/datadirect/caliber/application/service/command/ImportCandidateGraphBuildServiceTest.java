package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphNodeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.PreprocessResultDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ImportCandidateGraphBuildServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldBuildAdaptedOntologyFromPreprocessResult() throws Exception {
        ImportCandidateGraphBuildService service = new ImportCandidateGraphBuildService(objectMapper);
        JsonNode scene = objectMapper.readTree("""
                {
                  "scene_title":"按协议号查询代发明细",
                  "scene_description":"用于核对代发明细结果",
                  "source_evidence_lines":[1,2,3],
                  "inputs":{"params":[{"name":"协议号","type":"STRING","required":true,"identifiers":["协议号"]}]},
                  "outputs":{"fields":[{"name":"交易金额"},{"name":"交易日期"}]},
                  "sql_variants":[
                    {
                      "variant_name":"默认方案",
                      "default_time_semantic":"TRX_DT",
                      "source_tables":["PDM_VHIS.T05_AGN_DTL"],
                      "source_columns":["TRX_DT","TRX_AMT"],
                      "join_relations":[{"label":"T01_PROTOCOL.ID = T05_AGN_DTL.PROTOCOL_ID"}],
                      "sql_text":"SELECT AGN_BCH_SEQ, TRX_AMT, TRX_DT FROM PDM_VHIS.T05_AGN_DTL WHERE MCH_AGR_NBR='${AGR_ID}'"
                    }
                  ]
                }
                """);
        PreprocessResultDTO result = new PreprocessResultDTO(
                "{}",
                "rule_generated",
                objectMapper.readTree("{\"domain_guess\":\"PAYROLL\"}"),
                List.of(scene),
                objectMapper.readTree("{\"confidence\":0.9}"),
                List.of(),
                0.9,
                "HIGH",
                false,
                120L,
                List.of(),
                List.of(),
                "task-1",
                "material-1"
        );

        ImportCandidateGraphDTO graph = service.build("task-1", "material-1", result);

        assertThat(graph.summary().nodeTotal()).isGreaterThanOrEqualTo(6);
        assertThat(graph.nodes().stream().map(ImportCandidateGraphNodeDTO::nodeType))
                .contains(
                        "CANDIDATE_SCENE",
                        "CANDIDATE_PLAN",
                        "IDENTIFIER",
                        "TIME_SEMANTIC",
                        "SOURCE_TABLE",
                        "SOURCE_COLUMN",
                        "JOIN_RELATION",
                        "FIELD_CONCEPT"
                );
        assertThat(graph.nodes().stream()
                .map(ImportCandidateGraphNodeDTO::sceneCandidateCode))
                .doesNotContainNull()
                .contains("SC-task1-001");
        assertThat(graph.nodes().stream()
                .filter(node -> !"CANDIDATE_SCENE".equals(node.nodeType()))
                .map(ImportCandidateGraphNodeDTO::sceneCandidateCode))
                .containsOnly("SC-task1-001");
        assertThat(graph.edges().stream()
                .map(ImportCandidateGraphEdgeDTO::sceneCandidateCode))
                .containsOnly("SC-task1-001");
        assertThat(graph.nodes().stream()
                .filter(node -> "SOURCE_COLUMN".equals(node.nodeType()))
                .count())
                .isEqualTo(2);
        assertThat(graph.nodes().stream()
                .filter(node -> "JOIN_RELATION".equals(node.nodeType()))
                .count())
                .isEqualTo(1);
        assertThat(graph.edges().stream()
                .filter(edge -> "PLAN_USES_SOURCE_COLUMN".equals(edge.edgeType()))
                .count())
                .isEqualTo(2);
        assertThat(graph.edges().stream()
                .filter(edge -> "PLAN_USES_JOIN_RELATION".equals(edge.edgeType()))
                .count())
                .isEqualTo(1);
    }

    @Test
    void shouldMergeSameBusinessSceneVariantsUnderOneCandidateScene() throws Exception {
        ImportCandidateGraphBuildService service = new ImportCandidateGraphBuildService(objectMapper);
        JsonNode currentScene = objectMapper.readTree("""
                {
                  "scene_title":"方法1：按协议号查询代发明细",
                  "scene_description":"用于核对代发明细结果",
                  "applicability":"当前主表",
                  "inputs":{"params":[{"name":"协议号","type":"STRING","required":true}]},
                  "outputs":{"fields":[{"name":"交易金额"},{"name":"交易日期"}]},
                  "sql_variants":[
                    {
                      "variant_name":"当前表方案",
                      "source_tables":["PDM_VHIS.T05_AGN_DTL"],
                      "sql_text":"SELECT TRX_AMT, TRX_DT FROM PDM_VHIS.T05_AGN_DTL WHERE MCH_AGR_NBR='${AGR_ID}'"
                    }
                  ]
                }
                """);
        JsonNode historyScene = objectMapper.readTree("""
                {
                  "scene_title":"方法2：按协议号查询代发明细",
                  "scene_description":"用于核对代发明细结果",
                  "applicability":"历史表补录",
                  "inputs":{"params":[{"name":"协议号","type":"STRING","required":true}]},
                  "outputs":{"fields":[{"name":"交易金额"},{"name":"交易日期"}]},
                  "sql_variants":[
                    {
                      "variant_name":"历史表方案",
                      "source_tables":["PDM_VHIS_HIS.T05_AGN_DTL"],
                      "sql_text":"SELECT TRX_AMT, TRX_DT FROM PDM_VHIS_HIS.T05_AGN_DTL WHERE MCH_AGR_NBR='${AGR_ID}'"
                    }
                  ]
                }
                """);
        PreprocessResultDTO result = new PreprocessResultDTO(
                "{}",
                "rule_generated",
                objectMapper.readTree("{\"domain_guess\":\"PAYROLL\"}"),
                List.of(currentScene, historyScene),
                objectMapper.readTree("{\"confidence\":0.9}"),
                List.of(),
                0.9,
                "HIGH",
                false,
                120L,
                List.of(),
                List.of(),
                "task-merge",
                "material-merge"
        );

        ImportCandidateGraphDTO graph = service.build("task-merge", "material-merge", result);

        assertThat(graph.nodes().stream()
                .filter(node -> "CANDIDATE_SCENE".equals(node.nodeType())))
                .hasSize(1);
        assertThat(graph.nodes().stream()
                .filter(node -> "CANDIDATE_PLAN".equals(node.nodeType())))
                .hasSize(2);
        assertThat(graph.edges().stream()
                .filter(edge -> "SCENE_HAS_PLAN".equals(edge.edgeType())))
                .hasSize(2);
    }
}
