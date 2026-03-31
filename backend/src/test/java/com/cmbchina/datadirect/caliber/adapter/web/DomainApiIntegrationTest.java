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
class DomainApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateUpdateAndQueryDomain() throws Exception {
        String token = loginAndGetToken("support", "support123");
        String createRequest = """
                {
                  "domainCode": "RETAIL_API",
                  "domainName": "零售业务",
                  "domainOverview": "零售客户相关主题",
                  "commonTables": "dm_customer_info,dm_account",
                  "contacts": "数据支持组",
                  "sortOrder": 10,
                  "operator": "admin"
                }
                """;
        MvcResult createResult = mockMvc.perform(post("/api/domains")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.domainCode").value("RETAIL_API"))
                .andExpect(jsonPath("$.domainName").value("零售业务"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long id = created.path("id").asLong();
        assertThat(id).isPositive();

        String updateRequest = """
                {
                  "domainCode": "RETAIL_API",
                  "domainName": "零售业务-更新",
                  "domainOverview": "更新后的概述",
                  "commonTables": "dm_customer_info",
                  "contacts": "治理专员",
                  "sortOrder": 8,
                  "operator": "admin"
                }
                """;
        mockMvc.perform(put("/api/domains/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domainName").value("零售业务-更新"));

        mockMvc.perform(get("/api/domains/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domainOverview").value("更新后的概述"));

        mockMvc.perform(get("/api/domains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.domainCode == 'RETAIL_API')]").isArray());
    }

    @Test
    void shouldBootstrapDomainsFromBusinessCategories() throws Exception {
        String token = loginAndGetToken("support", "support123");

        mockMvc.perform(post("/api/domains/bootstrap-from-categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.domains[?(@.domainName == '零售基础业务')]").isArray())
                .andExpect(jsonPath("$.domains[?(@.domainName == '公司业务')]").isArray())
                .andExpect(jsonPath("$.domains[?(@.domainName == '贷款及信用卡业务')]").isArray())
                .andExpect(jsonPath("$.domains[?(@.domainName == '外汇及境外机构')]").isArray())
                .andExpect(jsonPath("$.domains[?(@.domainName == '财富管理')]").isArray());
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
