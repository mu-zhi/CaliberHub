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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class M2M3ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCompleteM2M3CoreFlow() throws Exception {
        String token = loginAndGetToken("support", "support123");
        long domainId = createDomain(token);
        long sceneId = createScene(token, "M2M3-场景初始");
        updateSceneForPublish(token, sceneId, domainId, "M2M3-场景初始", "M2M3 初始描述");

        long semanticViewId = createSemanticView(token, domainId);
        mockMvc.perform(post("/api/scenes/{id}/references", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refType": "SEMANTIC_VIEW",
                                  "refId": %d,
                                  "strategy": "LOCKED",
                                  "operator": "support"
                                }
                                """.formatted(semanticViewId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refType").value("SEMANTIC_VIEW"))
                .andExpect(jsonPath("$.strategy").value("LOCKED"));

        mockMvc.perform(post("/api/scenes/{id}/versions", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changeSummary": "创建版本V1",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNo").value(1));

        updateSceneForPublish(token, sceneId, domainId, "M2M3-场景修订", "M2M3 二次描述");
        mockMvc.perform(post("/api/scenes/{id}/versions", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changeSummary": "创建版本V2",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNo").value(2));

        mockMvc.perform(get("/api/scenes/{id}/versions", sceneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].versionNo").value(2))
                .andExpect(jsonPath("$[1].versionNo").value(1));

        mockMvc.perform(get("/api/scenes/{id}/diff", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .param("from", "1")
                        .param("to", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sceneId").value(sceneId))
                .andExpect(jsonPath("$.fromVersion").value(1))
                .andExpect(jsonPath("$.toVersion").value(2))
                .andExpect(jsonPath("$.changedFields").isArray())
                .andExpect(jsonPath("$.changedFields").isNotEmpty());

        mockMvc.perform(post("/api/alignment/reports/{sceneId}", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "FAIL",
                                  "message": "缺少脱敏策略",
                                  "operator": "support",
                                  "tables": ["dm_customer_info"],
                                  "columns": ["mobile_no"],
                                  "policies": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAIL"));

        mockMvc.perform(post("/api/scenes/{id}/publish", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verifiedAt": "2026-02-27T10:00:00+08:00",
                                  "changeSummary": "尝试发布",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOMAIN_VALIDATION_ERROR"));

        mockMvc.perform(post("/api/alignment/reports/{sceneId}", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PASS",
                                  "message": "对齐通过",
                                  "operator": "support",
                                  "tables": ["dm_customer_info"],
                                  "columns": ["cust_id","cust_name"],
                                  "policies": ["MASK_PHONE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASS"));

        mockMvc.perform(post("/api/scenes/{id}/publish", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verifiedAt": "2026-02-27T11:00:00+08:00",
                                  "changeSummary": "发布通过",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        MvcResult exportResult = mockMvc.perform(post("/api/service-specs/export/{sceneId}", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": 0,
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sceneId").value(sceneId))
                .andExpect(jsonPath("$.specVersion").value(1))
                .andReturn();
        JsonNode exportPayload = objectMapper.readTree(exportResult.getResponse().getContentAsString());
        String specCode = exportPayload.path("specCode").asText();
        assertThat(specCode).isNotBlank();

        mockMvc.perform(post("/api/service-specs/export/{sceneId}", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": 0,
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAL-SS-409"));

        mockMvc.perform(get("/api/service-specs/{specCode}", specCode)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specCode").value(specCode))
                .andExpect(jsonPath("$.specVersion").value(1));

        MvcResult planResult = mockMvc.perform(post("/api/nl/query")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "queryText": "查询 M2M3 场景修订 的客户信息",
                                  "operator": "frontline"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").isString())
                .andExpect(jsonPath("$.planAuditId").isNumber())
                .andExpect(jsonPath("$.evidence").isArray())
                .andReturn();
        long planAuditId = objectMapper.readTree(planResult.getResponse().getContentAsString()).path("planAuditId").asLong();
        assertThat(planAuditId).isPositive();

        mockMvc.perform(post("/api/nl/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planAuditId": %d,
                                  "success": true,
                                  "reason": "命中场景",
                                  "selectedPlan": "DEFAULT"
                                }
                                """.formatted(planAuditId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.sceneWeight").isNumber());

        mockMvc.perform(get("/api/impact/scenes/{sceneId}", sceneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sceneId").value(sceneId))
                .andExpect(jsonPath("$.referenceCount").value(1))
                .andExpect(jsonPath("$.serviceSpecCount").value(1));
    }

    private long createDomain(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/domains")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domainCode": "M2M3_DOMAIN",
                                  "domainName": "M2M3 测试域",
                                  "domainOverview": "用于 M2/M3 集成测试",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private long createScene(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/scenes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sceneTitle": "%s",
                                  "rawInput": "raw",
                                  "operator": "support"
                                }
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private void updateSceneForPublish(String token, long sceneId, long domainId, String title, String description) throws Exception {
        mockMvc.perform(put("/api/scenes/{id}", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sceneTitle": "%s",
                                  "domainId": %d,
                                  "sceneDescription": "%s",
                                  "sqlVariantsJson": "[{\\"variant_name\\":\\"默认方案\\",\\"sql_text\\":\\"select cust_id from dm_customer_info\\"}]",
                                  "operator": "support"
                                }
                                """.formatted(title, domainId, description)))
                .andExpect(status().isOk());
    }

    private long createSemanticView(String token, long domainId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/semantic-views")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "viewCode": "SV_M2M3",
                                  "viewName": "M2M3 语义视图",
                                  "domainId": %d,
                                  "description": "语义视图描述",
                                  "fieldDefinitionsJson": "[{\\"name\\":\\"cust_id\\",\\"meaning\\":\\"客户号\\"}]",
                                  "operator": "support"
                                }
                                """.formatted(domainId)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
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
