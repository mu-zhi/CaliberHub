package com.cmbchina.datadirect.caliber.adapter.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportTaskApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldListTaskLifecycleAndTaskScenes() throws Exception {
        String token = loginAndGetToken("support", "support123");
        String preprocessRequest = """
                {
                  "rawText":"### 场景标题：按协议号查询代发明细\\n- 场景描述：用于核对代发明细结果。\\n- 口径提供人：张三/80000001\\n- 结果字段：代发批次号、交易金额\\n- 注意事项：跨年查询需拆分。\\n- SQL 语句\\n-- Step 1\\nSELECT AGN_BCH_SEQ, TRX_AMT FROM PDM_VHIS.T05_AGN_DTL WHERE MCH_AGR_NBR='${AGR_ID}';",
                  "sourceType":"PASTE_MD",
                  "sourceName":"task-lifecycle-case.md",
                  "preprocessMode":"RULE_ONLY",
                  "autoCreateDrafts":true
                }
                """;

        MvcResult preprocessResult = mockMvc.perform(post("/api/import/preprocess")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(preprocessRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importBatchId").isString())
                .andReturn();
        JsonNode preprocessJson = objectMapper.readTree(preprocessResult.getResponse().getContentAsString());
        String taskId = preprocessJson.path("importBatchId").asText("");
        assertThat(taskId).isNotBlank();

        mockMvc.perform(get("/api/import/tasks")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").isString())
                .andExpect(jsonPath("$[0].draftTotal").isNumber());

        mockMvc.perform(get("/api/import/tasks/{taskId}/scenes", taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String loginBody = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
        MvcResult result = mockMvc.perform(post("/api/system/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tokenNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return tokenNode.path("accessToken").asText();
    }
}
