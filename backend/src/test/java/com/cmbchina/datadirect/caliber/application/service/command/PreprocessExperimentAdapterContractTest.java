package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.response.PreprocessResultDTO;
import com.cmbchina.datadirect.caliber.application.support.PreprocessExperimentSupport;
import com.cmbchina.datadirect.caliber.infrastructure.module.supportimpl.application.LightRagPreprocessExperimentSupportImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PreprocessExperimentAdapterContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnCandidateOnlyExperimentPayload() throws Exception {
        PreprocessExperimentSupport support = new LightRagPreprocessExperimentSupportImpl(objectMapper);
        JsonNode scene = objectMapper.readTree("""
                {
                  "scene_title":"按协议号查询代发明细",
                  "source_evidence_lines":[11,12,13],
                  "inputs":{"params":[{"name":"协议号","type":"STRING","required":true}]},
                  "outputs":{"fields":[{"name":"交易金额"},{"name":"交易日期"}]},
                  "sql_variants":[
                    {
                      "variant_name":"当前表方案",
                      "source_tables":["PDM_VHIS.T05_AGN_DTL"],
                      "source_columns":["TRX_AMT","TRX_DT"],
                      "join_relations":[{"label":"T01_PROTOCOL.ID = T05_AGN_DTL.PROTOCOL_ID"}],
                      "sql_text":"SELECT TRX_AMT, TRX_DT FROM PDM_VHIS.T05_AGN_DTL WHERE MCH_AGR_NBR='${AGR_ID}'"
                    }
                  ],
                  "quality":{"confidence":0.92}
                }
                """);
        PreprocessResultDTO preprocessResult = new PreprocessResultDTO(
                "{}",
                "rule_generated",
                objectMapper.readTree("{\"domain_guess\":\"PAYROLL\"}"),
                List.of(scene),
                objectMapper.readTree("{\"confidence\":0.92}"),
                List.of(),
                0.92,
                "HIGH",
                false,
                180L,
                List.of(),
                List.of(),
                "task-02f",
                "material-02f"
        );

        PreprocessExperimentSupport.PreprocessExperimentResult result = support.run(
                new PreprocessExperimentSupport.PreprocessExperimentRequest(
                        "task-02f",
                        "material-02f",
                        List.of("按协议号查询代发明细", "PDM_VHIS.T05_AGN_DTL"),
                        List.of("payroll-detail.pdf"),
                        List.of("TEXT", "PDF"),
                        "trace-02f",
                        preprocessResult
                )
        );

        assertThat(result.candidateEntities()).isNotEmpty();
        assertThat(result.candidateRelations()).isNotEmpty();
        assertThat(result.candidateEvidence()).isNotEmpty();
        assertThat(result.referenceRefs()).isNotEmpty();
        assertThat(result.formalAssetWrites()).isEmpty();
        assertThat(result.adapterName()).isEqualTo("LightRAG");
        assertThat(result.adapterVersion()).isNotBlank();
    }
}
