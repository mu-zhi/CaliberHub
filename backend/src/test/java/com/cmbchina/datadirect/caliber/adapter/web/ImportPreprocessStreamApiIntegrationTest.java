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

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportPreprocessStreamApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldStreamGraphPatchEventsBeforeDone() throws Exception {
        String token = loginAndGetToken("support", "support123");

        MvcResult result = mockMvc.perform(post("/api/import/preprocess-stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {
                                  "rawText":"### 场景标题：按协议号查询代发明细\\n- 场景描述：用于核对代发明细结果。\\n- 结果字段：代发批次号、交易金额\\n- SQL: SELECT AGN_BCH_SEQ, TRX_AMT FROM PDM_VHIS.T05_AGN_DTL WHERE MCH_AGR_NBR='${AGR_ID}';",
                                  "sourceType":"PASTE_MD",
                                  "sourceName":"stream-graph-patch.md",
                                  "preprocessMode":"RULE_ONLY",
                                  "autoCreateDrafts":true
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andReturn();

        result.getAsyncResult(10000);
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("event:start");
        assertThat(body).contains("event:graph_patch");
        assertThat(body).contains("event:done");
        assertThat(body.indexOf("event:graph_patch")).isLessThan(body.indexOf("event:done"));
        assertThat(body).contains("\"addedNodes\"");
        assertThat(body).contains("\"addedEdges\"");
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
