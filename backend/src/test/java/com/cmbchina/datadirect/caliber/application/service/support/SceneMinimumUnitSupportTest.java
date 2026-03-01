package com.cmbchina.datadirect.caliber.application.service.support;

import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneMinimumUnitCheckDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneMinimumUnitCheckItemDTO;
import com.cmbchina.datadirect.caliber.domain.model.Scene;
import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SceneMinimumUnitSupportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldFailWhenRequiredUnitMissing() {
        Scene scene = Scene.builder()
                .id(1L)
                .sceneTitle("代发明细查询")
                .status(SceneStatus.DRAFT)
                .sceneDescription("")
                .sqlVariantsJson("[]")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        SceneMinimumUnitCheckDTO result = SceneMinimumUnitSupport.check(scene, objectMapper);

        assertThat(result.publishReady()).isFalse();
        Map<String, SceneMinimumUnitCheckItemDTO> itemMap = result.items().stream()
                .collect(Collectors.toMap(SceneMinimumUnitCheckItemDTO::key, item -> item));
        assertThat(itemMap.get("domain_id").passed()).isFalse();
        assertThat(itemMap.get("scene_description").passed()).isFalse();
        assertThat(itemMap.get("sql_variant").passed()).isFalse();
    }

    @Test
    void shouldPassWhenUnitIsComplete() {
        String sqlVariantsJson = """
                [
                  {
                    "variant_name":"查询方案1",
                    "sql_text":"SELECT * FROM PDM_VHIS.T05_AGN_DTL WHERE AGN_BCH_SEQ = '1'"
                  }
                ]
                """;
        Scene scene = Scene.builder()
                .id(2L)
                .sceneTitle("代发明细查询")
                .domainId(10L)
                .status(SceneStatus.DRAFT)
                .sceneDescription("根据代发协议号查询代发明细并统计交易金额。")
                .sqlVariantsJson(sqlVariantsJson)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        SceneMinimumUnitCheckDTO result = SceneMinimumUnitSupport.check(scene, objectMapper);

        assertThat(result.publishReady()).isTrue();
        assertThat(result.items()).allMatch(item -> Boolean.TRUE.equals(item.passed()));
    }
}
