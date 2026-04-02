package com.cmbchina.datadirect.caliber.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "caliber.security.require-write-auth=false")
class SceneApiWriteAuthOptionalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowAnonymousSceneWriteWhenWriteAuthDisabled() throws Exception {
        String createRequest = """
                {
                  "sceneTitle": "匿名写入场景",
                  "rawInput": "raw"
                }
                """;
        mockMvc.perform(post("/api/scenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdBy").value("system"));
    }

    @Test
    void shouldAllowAnonymousImportWhenWriteAuthDisabled() throws Exception {
        String request = """
                {
                  "rawText": "# 测试\\n```sql\\nselect 1;\\n```",
                  "sourceType": "PASTE_MD"
                }
                """;
        mockMvc.perform(post("/api/import/preprocess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caliberImportJson").exists());
    }

    @Test
    void shouldKeepSystemApiProtectedWhenWriteAuthDisabled() throws Exception {
        mockMvc.perform(get("/api/system/llm-preprocess-config"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAdminSystemApiWhenWriteAuthDisabled() throws Exception {
        String token = loginAndGetToken("admin", "admin123");

        mockMvc.perform(get("/api/system/llm-preprocess-config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").exists());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String loginBody = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
        return mockMvc.perform(post("/api/system/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }
}
