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
class LlmPreprocessConfigApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldUpdateAndQueryConfig() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        String updateRequest = """
                {
                  "enabled": false,
                  "endpoint": "https://example.com/v1/chat/completions",
                  "model": "gpt-4.1-mini",
                  "timeoutSeconds": 45,
                  "temperature": 0.1,
                  "maxTokens": 4096,
                  "enableThinking": true,
                  "fallbackToRule": true,
                  "apiKey": "test-key-123456",
                  "operator": "sys-admin"
                }
                """;

        mockMvc.perform(put("/api/system/llm-preprocess-config")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoint").value("https://example.com/v1/chat/completions"))
                .andExpect(jsonPath("$.enableThinking").value(true))
                .andExpect(jsonPath("$.hasApiKey").value(true))
                .andExpect(jsonPath("$.providerCode").value("COMPATIBLE"))
                .andExpect(jsonPath("$.supportsResponsesApi").value(false))
                .andExpect(jsonPath("$.supportsThinkingToggle").value(true));

        MvcResult queryResult = mockMvc.perform(get("/api/system/llm-preprocess-config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode config = objectMapper.readTree(queryResult.getResponse().getContentAsString());
        assertThat(config.path("maskedApiKey").asText()).isNotBlank();
        assertThat(config.path("maskedApiKey").asText()).isNotEqualTo("test-key-123456");
        assertThat(config.path("timeoutSeconds").asInt()).isEqualTo(45);
        assertThat(config.path("enableThinking").asBoolean()).isTrue();
        assertThat(config.path("configSource").asText()).isEqualTo("PERSISTED");
        assertThat(config.path("endpointHost").asText()).isEqualTo("example.com");
        assertThat(config.path("fallbackStrategy").asText()).isEqualTo("LLM -> RULE");
        assertThat(config.path("providerCode").asText()).isEqualTo("COMPATIBLE");
        assertThat(config.path("providerLabel").asText()).isEqualTo("兼容模式");
        assertThat(config.path("supportsResponsesApi").asBoolean()).isFalse();
        assertThat(config.path("supportsStructuredOutputs").asBoolean()).isFalse();
        assertThat(config.path("supportsThinkingToggle").asBoolean()).isTrue();
    }

    @Test
    void shouldRejectIncompatibleThinkingToggleForOpenAiResponses() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        String updateRequest = """
                {
                  "enabled": true,
                  "endpoint": "https://api.openai.com/v1/responses",
                  "model": "gpt-5.4",
                  "timeoutSeconds": 45,
                  "temperature": 0.1,
                  "maxTokens": 4096,
                  "enableThinking": true,
                  "fallbackToRule": true,
                  "operator": "sys-admin"
                }
                """;

        mockMvc.perform(put("/api/system/llm-preprocess-config")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOMAIN_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("enableThinking")));
    }

    @Test
    void shouldTestCurrentConfigWithRawText() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        String enableLlmRequest = """
                {
                  "enabled": true,
                  "endpoint": "http://127.0.0.1:1/v1/chat/completions",
                  "model": "gpt-4.1-mini",
                  "timeoutSeconds": 15,
                  "temperature": 0.0,
                  "maxTokens": 2048,
                  "enableThinking": false,
                  "fallbackToRule": true,
                  "operator": "sys-admin"
                }
                """;
        mockMvc.perform(put("/api/system/llm-preprocess-config")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(enableLlmRequest))
                .andExpect(status().isOk());

        String testRequest = """
                {
                  "rawText": "# 零售客户信息查询\\n```sql\\nselect * from dm_customer_info;\\n```",
                  "sourceType": "PASTE_MD"
                }
                """;

        mockMvc.perform(post("/api/system/llm-preprocess-config/test")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.llmEnabled").value(true))
                .andExpect(jsonPath("$.llmEffective").value(false))
                .andExpect(jsonPath("$.fallbackUsed").value(true))
                .andExpect(jsonPath("$.statusLabel").value("已回退规则"))
                .andExpect(jsonPath("$.statusReason").isString())
                .andExpect(jsonPath("$.promptFingerprint").isString())
                .andExpect(jsonPath("$.sceneCount").isNumber())
                .andExpect(jsonPath("$.sqlCount").isNumber());
    }

    @Test
    void shouldGetUpdateAndResetPrompts() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        MvcResult getResult = mockMvc.perform(get("/api/system/llm-preprocess-config/prompts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preprocessSystemPrompt").isString())
                .andExpect(jsonPath("$.preprocessUserPromptTemplate").isString())
                .andExpect(jsonPath("$.prepSchemaJson").isString())
                .andExpect(jsonPath("$.promptVersion").isNumber())
                .andExpect(jsonPath("$.promptHash").isString())
                .andExpect(jsonPath("$.promptFingerprint").isString())
                .andExpect(jsonPath("$.schemaValid").isBoolean())
                .andExpect(jsonPath("$.schemaValidationMessage").isString())
                .andExpect(jsonPath("$.templateHasRequiredTokens").isBoolean())
                .andExpect(jsonPath("$.templateMissingTokens").isArray())
                .andExpect(jsonPath("$.requiresManualReview").isBoolean())
                .andExpect(jsonPath("$.manualReviewReasons").isArray())
                .andReturn();
        JsonNode oldPromptPayload = objectMapper.readTree(getResult.getResponse().getContentAsString());
        String oldFingerprint = oldPromptPayload.path("promptFingerprint").asText();
        String oldPromptHash = oldPromptPayload.path("promptHash").asText();
        long oldPromptVersion = oldPromptPayload.path("promptVersion").asLong();

        String updateBody = """
                {
                  "preprocessSystemPrompt": "系统提示词-测试",
                  "preprocessUserPromptTemplate": "schema={{PREP_SCHEMA}}\\nraw={{RAW_DOC}}\\nsource={{SOURCE_TYPE}}",
                  "prepSchemaJson": "{\\"prep_type\\":\\"CALIBER_PREP_V1\\",\\"quality\\":{\\"warnings\\":[],\\"errors\\":[]}}"
                }
                """;
        MvcResult updateResult = mockMvc.perform(put("/api/system/llm-preprocess-config/prompts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promptVersion").isNumber())
                .andExpect(jsonPath("$.promptHash").isString())
                .andExpect(jsonPath("$.promptFingerprint").isString())
                .andExpect(jsonPath("$.schemaValid").value(true))
                .andExpect(jsonPath("$.templateHasRequiredTokens").value(true))
                .andExpect(jsonPath("$.requiresManualReview").value(true))
                .andExpect(jsonPath("$.manualReviewReasons[0]").isString())
                .andReturn();
        JsonNode updatedPromptPayload = objectMapper.readTree(updateResult.getResponse().getContentAsString());
        String newFingerprint = updatedPromptPayload.path("promptFingerprint").asText();
        String newPromptHash = updatedPromptPayload.path("promptHash").asText();
        long newPromptVersion = updatedPromptPayload.path("promptVersion").asLong();
        assertThat(newFingerprint).isNotBlank();
        assertThat(newFingerprint).isNotEqualTo(oldFingerprint);
        assertThat(newPromptHash).isNotBlank();
        assertThat(newPromptHash).isNotEqualTo(oldPromptHash);
        assertThat(newPromptVersion).isGreaterThan(oldPromptVersion);

        MvcResult resetResult = mockMvc.perform(post("/api/system/llm-preprocess-config/prompts/reset")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preprocessSystemPrompt").isString())
                .andExpect(jsonPath("$.preprocessUserPromptTemplate").isString())
                .andExpect(jsonPath("$.prepSchemaJson").isString())
                .andExpect(jsonPath("$.promptVersion").isNumber())
                .andExpect(jsonPath("$.promptHash").isString())
                .andExpect(jsonPath("$.promptFingerprint").isString())
                .andExpect(jsonPath("$.schemaValid").isBoolean())
                .andExpect(jsonPath("$.templateHasRequiredTokens").isBoolean())
                .andExpect(jsonPath("$.requiresManualReview").value(false))
                .andExpect(jsonPath("$.manualReviewReasons").isArray())
                .andReturn();
        JsonNode resetPayload = objectMapper.readTree(resetResult.getResponse().getContentAsString());
        assertThat(resetPayload.path("preprocessUserPromptTemplate").asText()).contains("{{DYNAMIC_JSON_SCHEMA}}");
        assertThat(resetPayload.path("preprocessUserPromptTemplate").asText()).contains("<task>");
        assertThat(resetPayload.path("preprocessSystemPrompt").asText()).contains("<output_contract>");
        assertThat(resetPayload.path("promptVersion").asLong()).isGreaterThan(newPromptVersion);
        assertThat(resetPayload.path("promptHash").asText()).isNotBlank();
    }

    @Test
    void shouldPreviewPromptRenderWithDraftOverrides() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        String previewBody = """
                {
                  "rawText": "select * from dm_customer_info;",
                  "sourceType": "FILE_SQL",
                  "preprocessSystemPrompt": "系统草稿提示词",
                  "preprocessUserPromptTemplate": "schema={{DYNAMIC_JSON_SCHEMA}}\\nraw={{RAW_DOC}}\\nsource={{SOURCE_TYPE}}",
                  "prepSchemaJson": "{\\"prep_type\\":\\"CALIBER_PREP_V1\\"}"
                }
                """;
        mockMvc.perform(post("/api/system/llm-preprocess-config/prompts/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(previewBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemPrompt").value("系统草稿提示词"))
                .andExpect(jsonPath("$.promptFingerprint").isString())
                .andExpect(jsonPath("$.normalizedSourceType").value("FILE_SQL"))
                .andExpect(jsonPath("$.lineCount").value(1))
                .andExpect(jsonPath("$.userPrompt").value(org.hamcrest.Matchers.containsString("\"prep_type\"")))
                .andExpect(jsonPath("$.userPrompt").value(org.hamcrest.Matchers.containsString("[001] select * from dm_customer_info;")))
                .andExpect(jsonPath("$.warnings").isArray());
    }

    @Test
    void shouldRejectConfigWriteForNonAdminRole() throws Exception {
        String token = loginAndGetToken("support", "support123");
        String updateRequest = """
                {
                  "enabled": false,
                  "endpoint": "https://example.com/v1/chat/completions",
                  "model": "gpt-4.1-mini",
                  "timeoutSeconds": 35,
                  "temperature": 0,
                  "maxTokens": 4096,
                  "enableThinking": false,
                  "fallbackToRule": true,
                  "operator": "support"
                }
                """;
        mockMvc.perform(put("/api/system/llm-preprocess-config")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldRejectPromptWriteForNonAdminRole() throws Exception {
        String token = loginAndGetToken("support", "support123");
        String body = """
                {
                  "preprocessSystemPrompt": "x",
                  "preprocessUserPromptTemplate": "{{PREP_SCHEMA}}{{RAW_DOC}}{{SOURCE_TYPE}}",
                  "prepSchemaJson": "{}"
                }
                """;
        mockMvc.perform(put("/api/system/llm-preprocess-config/prompts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
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
