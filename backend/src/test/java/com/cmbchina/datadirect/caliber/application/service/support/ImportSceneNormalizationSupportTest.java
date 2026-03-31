package com.cmbchina.datadirect.caliber.application.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImportSceneNormalizationSupportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMergeSameBusinessSceneIntoOneSceneWithMultiplePlans() throws Exception {
        JsonNode first = objectMapper.readTree("""
                {
                  "scene_title":"方法1：按协议号查询代发明细",
                  "scene_description":"用于查询代发明细",
                  "applicability":"当前主表",
                  "inputs":{"params":[{"name":"协议号"}]},
                  "outputs":{"fields":[{"name":"交易金额"},{"name":"交易日期"}]},
                  "sql_variants":[{"variant_name":"默认方案","source_tables":["PDM.T05_AGN_DTL"],"sql_text":"SELECT TRX_AMT FROM PDM.T05_AGN_DTL"}]
                }
                """);
        JsonNode second = objectMapper.readTree("""
                {
                  "scene_title":"方法2：按协议号查询代发明细",
                  "scene_description":"用于查询代发明细",
                  "applicability":"历史表补录",
                  "inputs":{"params":[{"name":"协议号"}]},
                  "outputs":{"fields":[{"name":"交易金额"},{"name":"交易日期"}]},
                  "sql_variants":[{"variant_name":"默认方案","source_tables":["PDM_HIS.T05_AGN_DTL"],"sql_text":"SELECT TRX_AMT FROM PDM_HIS.T05_AGN_DTL"}]
                }
                """);

        List<JsonNode> normalized = ImportSceneNormalizationSupport.normalize(objectMapper, List.of(first, second));

        assertThat(normalized).hasSize(1);
        assertThat(normalized.get(0).path("scene_title").asText()).isEqualTo("按协议号查询代发明细");
        assertThat(normalized.get(0).path("sql_variants")).hasSize(2);
        assertThat(normalized.get(0).path("sql_variants").get(0).path("variant_name").asText()).isEqualTo("按协议号查询代发明细");
        assertThat(normalized.get(0).path("sql_variants").get(1).path("variant_name").asText()).isEqualTo("按协议号查询代发明细（历史表）");
    }

    @Test
    void shouldEnhanceDuplicateVariantNameBySourceAndPeriod() throws Exception {
        JsonNode first = objectMapper.readTree("""
                {
                  "scene_title":"方法1：代发明细查询",
                  "scene_description":"用于查询代发明细",
                  "applicability":"当前主表",
                  "inputs":{"params":[{"name":"协议号"}]},
                  "outputs":{"fields":[{"name":"交易金额"},{"name":"交易日期"}]},
                  "sql_variants":[
                    {
                      "variant_name":"默认方案",
                      "applicable_period":"当前表",
                      "source_tables":["PDM_VHIS.T05_AGN_DTL"],
                      "sql_text":"SELECT TRX_AMT FROM PDM_VHIS.T05_AGN_DTL"
                    }
                  ]
                }
                """);
        JsonNode second = objectMapper.readTree("""
                {
                  "scene_title":"方法2：代发明细查询",
                  "scene_description":"用于查询代发明细",
                  "applicability":"历史表补录",
                  "inputs":{"params":[{"name":"协议号"}]},
                  "outputs":{"fields":[{"name":"交易金额"},{"name":"交易日期"}]},
                  "sql_variants":[
                    {
                      "variant_name":"默认方案",
                      "applicable_period":"历史表",
                      "source_tables":["PDM_HIS.T05_AGN_DTL"],
                      "sql_text":"SELECT TRX_AMT FROM PDM_HIS.T05_AGN_DTL"
                    }
                  ]
                }
                """);

        List<JsonNode> normalized = ImportSceneNormalizationSupport.normalize(objectMapper, List.of(first, second));

        assertThat(normalized).hasSize(1);
        assertThat(normalized.get(0).path("sql_variants").get(0).path("variant_name").asText())
                .isEqualTo("代发明细查询");
        assertThat(normalized.get(0).path("sql_variants").get(1).path("variant_name").asText())
                .isEqualTo("代发明细查询（历史表）");
    }
}
