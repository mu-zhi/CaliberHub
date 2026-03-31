package com.cmbchina.datadirect.caliber.adapter.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    void shouldPersistIndependentSourceMaterialForImportTask() throws Exception {
        String token = loginAndGetToken("support", "support123");
        String preprocessRequest = """
                {
                  "rawText":"### 场景标题：按协议号查询代发明细\\n- 场景描述：用于核对代发明细结果。\\n- 结果字段：代发批次号、交易金额",
                  "sourceType":"PASTE_MD",
                  "sourceName":"payroll-material-a.md",
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
                .andExpect(jsonPath("$.materialId").isString())
                .andReturn();

        JsonNode preprocessJson = objectMapper.readTree(preprocessResult.getResponse().getContentAsString());
        String taskId = preprocessJson.path("importBatchId").asText("");
        String materialId = preprocessJson.path("materialId").asText("");
        assertThat(taskId).isNotBlank();
        assertThat(materialId).isNotBlank();

        mockMvc.perform(get("/api/import/tasks/{taskId}", taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.materialId").value(materialId));

        String sourceName = jdbcTemplate.queryForObject(
                "SELECT source_name FROM caliber_source_material WHERE material_id = ?",
                String.class,
                materialId
        );
        String sourceType = jdbcTemplate.queryForObject(
                "SELECT source_type FROM caliber_source_material WHERE material_id = ?",
                String.class,
                materialId
        );
        String rawText = jdbcTemplate.queryForObject(
                "SELECT raw_text FROM caliber_source_material WHERE material_id = ?",
                String.class,
                materialId
        );
        String fingerprint = jdbcTemplate.queryForObject(
                "SELECT text_fingerprint FROM caliber_source_material WHERE material_id = ?",
                String.class,
                materialId
        );

        assertThat(sourceName).isEqualTo("payroll-material-a.md");
        assertThat(sourceType).isEqualTo("PASTE_MD");
        assertThat(rawText).contains("代发批次号");
        assertThat(fingerprint).isNotBlank();
    }

    @Test
    void shouldPersistCandidateScenesAndEvidenceAgainstMaterial() throws Exception {
        String token = loginAndGetToken("support", "support123");
        MvcResult preprocessResult = mockMvc.perform(post("/api/import/preprocess")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rawText":"### 场景标题：按协议号查询代发明细\\n- 场景描述：用于核对代发明细结果。\\n- 结果字段：代发批次号、交易金额\\n- SQL: SELECT AGN_BCH_SEQ, TRX_AMT FROM PDM_VHIS.T05_AGN_DTL WHERE MCH_AGR_NBR='${AGR_ID}';",
                                  "sourceType":"PASTE_MD",
                                  "sourceName":"payroll-scene-a.md",
                                  "preprocessMode":"RULE_ONLY",
                                  "autoCreateDrafts":true
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(preprocessResult.getResponse().getContentAsString());
        String taskId = root.path("importBatchId").asText();
        String materialId = root.path("materialId").asText();

        assertThat(taskId).isNotBlank();
        assertThat(materialId).isNotBlank();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM caliber_import_scene_candidate WHERE task_id = ? AND material_id = ?",
                Integer.class,
                taskId,
                materialId
        )).isGreaterThan(0);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM caliber_import_evidence_candidate WHERE task_id = ? AND material_id = ?",
                Integer.class,
                taskId,
                materialId
        )).isGreaterThan(0);
    }

    @Test
    void shouldExposeRestorableCandidateGraphInPreprocessResult() throws Exception {
        String token = loginAndGetToken("support", "support123");
        MvcResult preprocessResult = mockMvc.perform(post("/api/import/preprocess")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rawText":"### 场景标题：按协议号查询代发明细\\n- 场景描述：用于核对代发明细结果。\\n- 结果字段：代发批次号、交易金额\\n- SQL: SELECT AGN_BCH_SEQ, TRX_AMT FROM PDM_VHIS.T05_AGN_DTL WHERE MCH_AGR_NBR='${AGR_ID}';",
                                  "sourceType":"PASTE_MD",
                                  "sourceName":"candidate-graph-restore.md",
                                  "preprocessMode":"RULE_ONLY",
                                  "autoCreateDrafts":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importBatchId").isString())
                .andReturn();

        JsonNode root = objectMapper.readTree(preprocessResult.getResponse().getContentAsString());
        String taskId = root.path("importBatchId").asText("");
        assertThat(taskId).isNotBlank();

        mockMvc.perform(get("/api/import/tasks/{taskId}", taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preprocessResult.candidateGraph.nodes").isArray())
                .andExpect(jsonPath("$.preprocessResult.candidateGraph.edges").isArray())
                .andExpect(jsonPath("$.preprocessResult.candidateGraph.nodes[0].nodeType").isString());
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
