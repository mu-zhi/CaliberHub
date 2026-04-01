package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ContractViewMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.DictionaryMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.IdentifierLineageMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.InputSlotSchemaMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceIntakeContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.TimeSemanticSelectorMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.AbstractSnapshotGraphAuditablePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.ContractViewPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.DictionaryPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.IdentifierLineagePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.InputSlotSchemaPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.OutputContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceIntakeContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.TimeSemanticSelectorPO;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SceneApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlanMapper planMapper;

    @Autowired
    private OutputContractMapper outputContractMapper;

    @Autowired
    private InputSlotSchemaMapper inputSlotSchemaMapper;

    @Autowired
    private SourceIntakeContractMapper sourceIntakeContractMapper;

    @Autowired
    private ContractViewMapper contractViewMapper;

    @Autowired
    private SourceContractMapper sourceContractMapper;

    @Autowired
    private DictionaryMapper dictionaryMapper;

    @Autowired
    private IdentifierLineageMapper identifierLineageMapper;

    @Autowired
    private TimeSemanticSelectorMapper timeSemanticSelectorMapper;

    @Test
    void shouldCompleteDraftToPublishFlow() throws Exception {
        String token = loginAndGetToken("support", "support123");
        String domainRequest = """
                {
                  "domainCode": "RETAIL_PUBLISH_FLOW",
                  "domainName": "零售发布域",
                  "domainOverview": "发布流测试域",
                  "operator": "admin"
                }
                """;
        MvcResult domainResult = mockMvc.perform(post("/api/domains")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(domainRequest))
                .andExpect(status().isCreated())
                .andReturn();
        long domainId = objectMapper.readTree(domainResult.getResponse().getContentAsString()).path("id").asLong();

        String createRequest = """
                {
                  "sceneTitle": "零售客户信息查询",
                  "domain": "零售金融",
                  "rawInput": "初始口径文档",
                  "operator": "tester"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/scenes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        JsonNode createdScene = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long id = createdScene.path("id").asLong();
        assertThat(id).isPositive();

        String updateRequest = """
                {
                  "sceneTitle": "零售客户信息查询（修订）",
                  "domainId": %d,
                  "sceneDescription": "查询客户基础信息",
                  "inputsJson": "[{\\"name\\":\\"cust_id\\"}]",
                  "outputsJson": "[{\\"name\\":\\"cust_name\\"}]",
                  "sqlBlocksJson": "[{\\"name\\":\\"SQL块1\\",\\"sql\\":\\"select cust_id from dm_customer_info;\\"}]",
                  "operator": "editor"
                }
                """.formatted(domainId);

        mockMvc.perform(put("/api/scenes/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sceneTitle").value("零售客户信息查询（修订）"));

        prepareMinimumPublishAssets(id);

        String publishRequest = """
                {
                  "verifiedAt": "2026-02-10T10:00:00+08:00",
                  "changeSummary": "修订并发布",
                  "operator": "reviewer"
                }
                """;

        mockMvc.perform(post("/api/scenes/{id}/publish", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedBy").value("support"));

        mockMvc.perform(get("/api/scenes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(delete("/api/scenes/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_STATE"));
    }

    @Test
    void shouldReturnCaliberImportV2FromPreprocess() throws Exception {
        String token = loginAndGetToken("support", "support123");
        String request = """
                {
                  "rawText": "# 零售客户信息查询\\n```sql\\nselect * from dm_customer_info;\\n```",
                  "sourceType": "FILE_SQL",
                  "sourceName": "03-口径文档现状-零售客户信息查询.sql"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/import/preprocess")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caliberImportJson").exists())
                .andExpect(jsonPath("$.scenes").isArray())
                .andExpect(jsonPath("$.mode").exists())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String caliberImportJson = response.path("caliberImportJson").asText();
        JsonNode parsed = objectMapper.readTree(caliberImportJson);
        assertThat(parsed.path("doc_type").asText()).isEqualTo("CALIBER_IMPORT_V2");
        assertThat(parsed.path("scenes").get(0).path("sql_variants")).isNotNull();
        assertThat(response.path("scenes").isArray()).isTrue();
    }

    @Test
    void shouldReturnLowConfidenceWhenLlmFallbackHappens() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        String supportToken = loginAndGetToken("support", "support123");
        String updateConfigRequest = """
                {
                  "enabled": true,
                  "endpoint": "http://127.0.0.1:1/v1/chat/completions",
                  "model": "qwen3-max",
                  "timeoutSeconds": 15,
                  "temperature": 0.0,
                  "maxTokens": 2048,
                  "fallbackToRule": true,
                  "operator": "admin"
                }
                """;
        mockMvc.perform(put("/api/system/llm-preprocess-config")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateConfigRequest))
                .andExpect(status().isOk());

        String preprocessRequest = """
                {
                  "rawText": "# 回退验证\\n```sql\\nselect * from dm_customer_info;\\n```",
                  "sourceType": "FILE_SQL",
                  "sourceName": "fallback-check.sql"
                }
                """;
        mockMvc.perform(post("/api/import/preprocess")
                        .header("Authorization", "Bearer " + supportToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(preprocessRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("rule_generated"))
                .andExpect(jsonPath("$.lowConfidence").value(true))
                .andExpect(jsonPath("$.warnings").isArray());
    }

    @Test
    void shouldSupportDomainIdFilter() throws Exception {
        String token = loginAndGetToken("support", "support123");
        String createDomainRequest = """
                {
                  "domainCode": "RETAIL_FILTER",
                  "domainName": "零售业务",
                  "domainOverview": "零售客户经营场景",
                  "operator": "admin"
                }
                """;
        MvcResult domainResult = mockMvc.perform(post("/api/domains")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDomainRequest))
                .andExpect(status().isCreated())
                .andReturn();
        long domainId = objectMapper.readTree(domainResult.getResponse().getContentAsString()).path("id").asLong();

        String createSceneRequest = """
                {
                  "sceneTitle": "按域过滤验证",
                  "domainId": %d,
                  "rawInput": "raw",
                  "operator": "tester"
                }
                """.formatted(domainId);
        mockMvc.perform(post("/api/scenes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSceneRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.domainId").value(domainId));

        mockMvc.perform(get("/api/scenes").param("domainId", String.valueOf(domainId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].domainId").value(domainId))
                .andExpect(jsonPath("$[0].domainName").value("零售业务"));
    }

    @Test
    void shouldSupportLegacyDomainFilterParam() throws Exception {
        String token = loginAndGetToken("support", "support123");
        String createSceneRequest = """
                {
                  "sceneTitle": "旧参数域过滤验证",
                  "domain": "零售金融-旧口径",
                  "rawInput": "raw",
                  "operator": "tester"
                }
                """;
        mockMvc.perform(post("/api/scenes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSceneRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.domain").value("零售金融-旧口径"));

        mockMvc.perform(get("/api/scenes").param("domain", "零售金融-旧口径"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].domain").value("零售金融-旧口径"));
    }

    @Test
    void shouldReturnCalSc409WhenExpectedVersionMismatched() throws Exception {
        String token = loginAndGetToken("support", "support123");
        String createRequest = """
                {
                  "sceneTitle": "并发冲突校验",
                  "rawInput": "raw",
                  "operator": "tester"
                }
                """;
        MvcResult createResult = mockMvc.perform(post("/api/scenes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andReturn();
        long sceneId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        String updateRequest = """
                {
                  "sceneTitle": "并发冲突校验-更新",
                  "expectedVersion": 999,
                  "operator": "editor"
                }
                """;
        mockMvc.perform(put("/api/scenes/{id}", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAL-SC-409"));
    }

    @Test
    void shouldKeepSqlBlocksCompatibilityWhenOnlyLegacyFieldProvided() throws Exception {
        String token = loginAndGetToken("support", "support123");
        String createRequest = """
                {
                  "sceneTitle": "SQL块兼容验证",
                  "rawInput": "raw",
                  "operator": "tester"
                }
                """;
        MvcResult createResult = mockMvc.perform(post("/api/scenes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andReturn();
        long sceneId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        String updateRequest = """
                {
                  "sceneTitle": "SQL块兼容验证-更新",
                  "sqlBlocksJson": "[{\\"name\\":\\"SQL块1\\",\\"sql\\":\\"select cust_id from dm_customer_info;\\"}]",
                  "operator": "editor"
                }
                """;
        mockMvc.perform(put("/api/scenes/{id}", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sqlBlocksJson").value(org.hamcrest.Matchers.containsString("dm_customer_info")))
                .andExpect(jsonPath("$.sqlVariantsJson").value(org.hamcrest.Matchers.containsString("dm_customer_info")));

        mockMvc.perform(get("/api/scenes").param("keyword", "dm_customer_info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sceneId));
    }

    @Test
    void shouldRejectWriteWhenUnauthenticated() throws Exception {
        String createRequest = """
                {
                  "sceneTitle": "未登录写入",
                  "rawInput": "raw"
                }
                """;
        mockMvc.perform(post("/api/scenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectSceneWriteForFrontlineRole() throws Exception {
        String token = loginAndGetToken("frontline", "frontline123");
        String createRequest = """
                {
                  "sceneTitle": "前台只读角色写入",
                  "rawInput": "raw"
                }
                """;
        mockMvc.perform(post("/api/scenes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldDiscardDraftAndHideFromDefaultList() throws Exception {
        String token = loginAndGetToken("support", "support123");
        String title = "弃用草稿-过滤验证";
        String createRequest = """
                {
                  "sceneTitle": "%s",
                  "rawInput": "raw",
                  "operator": "tester"
                }
                """.formatted(title);
        MvcResult createResult = mockMvc.perform(post("/api/scenes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andReturn();
        long sceneId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(post("/api/scenes/{id}/discard", sceneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISCARDED"));

        mockMvc.perform(get("/api/scenes").param("keyword", title))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/scenes")
                        .param("status", "DISCARDED")
                        .param("keyword", title))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sceneId))
                .andExpect(jsonPath("$[0].status").value("DISCARDED"));
    }

    @Test
    void shouldExposeMinimumUnitDefinitionAndCheck() throws Exception {
        String token = loginAndGetToken("support", "support123");
        MvcResult createResult = mockMvc.perform(post("/api/scenes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sceneTitle": "最小单元校验场景",
                                  "rawInput": "raw",
                                  "operator": "tester"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long sceneId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(get("/api/scenes/minimum-unit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitType").value("CALIBER_SCENE_UNIT_V1"))
                .andExpect(jsonPath("$.requiredFields").isArray());

        mockMvc.perform(get("/api/scenes/{id}/minimum-unit-check", sceneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sceneId").value(sceneId))
                .andExpect(jsonPath("$.publishReady").value(false))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void shouldExposeDictionaryIdentifierLineageAndTimeSemanticDiffBlocks() throws Exception {
        String token = loginAndGetToken("support", "support123");
        MvcResult createResult = mockMvc.perform(post("/api/scenes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sceneTitle": "差异块输出校验场景",
                                  "rawInput": "raw",
                                  "operator": "tester"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long sceneId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(post("/api/scenes/{id}/versions", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changeSummary": "baseline",
                                  "operator": "tester"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/scenes/{id}", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sceneTitle": "差异块输出校验场景-更新",
                                  "sceneDescription": "触发版本差异",
                                  "operator": "editor"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/scenes/{id}/versions", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changeSummary": "after-edit",
                                  "operator": "editor"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/scenes/{id}/diff", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .param("from", "1")
                        .param("to", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changedFields").isArray())
                .andExpect(jsonPath("$.dictionaryChanges").isArray())
                .andExpect(jsonPath("$.identifierLineageChanges").isArray())
                .andExpect(jsonPath("$.timeSemanticSelectorChanges").isArray());
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

    private void prepareMinimumPublishAssets(long sceneId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<PlanPO> plans = planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        assertThat(plans).isNotEmpty();

        for (PlanPO plan : plans) {
            if (plan.getDefaultTimeSemantic() == null || plan.getDefaultTimeSemantic().isBlank()) {
                plan.setDefaultTimeSemantic("交易日期");
                plan.setUpdatedBy("tester");
                plan.setUpdatedAt(now);
                planMapper.save(plan);
            }
        }

        OutputContractPO outputContract = outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream()
                .findFirst()
                .orElseThrow();
        SourceIntakeContractPO intakeContract = sourceIntakeContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream()
                .findFirst()
                .orElseThrow();
        PlanPO primaryPlan = plans.get(0);

        if (inputSlotSchemaMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty()) {
            InputSlotSchemaPO slot = new InputSlotSchemaPO();
            slot.setSceneId(sceneId);
            slot.setSlotCode("SLOT-" + sceneId + "-CUST");
            slot.setSlotName("cust_id");
            slot.setSlotType("STRING");
            slot.setRequiredFlag(true);
            slot.setIdentifierCandidatesJson("[\"CUST_ID\"]");
            slot.setNormalizationRule("trim");
            slot.setClarificationHint("请输入客户号");
            stamp(slot, now);
            inputSlotSchemaMapper.save(slot);
        }

        if (contractViewMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty()) {
            ContractViewPO view = new ContractViewPO();
            view.setSceneId(sceneId);
            view.setPlanId(primaryPlan.getId());
            view.setOutputContractId(outputContract.getId());
            view.setViewCode("CV-" + sceneId + "-DEFAULT");
            view.setViewName("默认普通角色视图");
            view.setRoleScope("SUPPORT");
            view.setVisibleFieldsJson("[\"cust_id\",\"cust_name\"]");
            view.setMaskedFieldsJson("[]");
            view.setRestrictedFieldsJson("[]");
            view.setForbiddenFieldsJson("[]");
            view.setApprovalTemplate("");
            stamp(view, now);
            contractViewMapper.save(view);
        }

        if (sourceContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty()) {
            SourceContractPO sourceContract = new SourceContractPO();
            sourceContract.setSceneId(sceneId);
            sourceContract.setPlanId(primaryPlan.getId());
            sourceContract.setIntakeContractId(intakeContract.getId());
            sourceContract.setSourceContractCode("SRC-" + sceneId + "-MAIN");
            sourceContract.setSourceName("零售客户信息主来源");
            sourceContract.setPhysicalTable("DM_CUSTOMER_INFO");
            sourceContract.setSourceRole("DETAIL_MAIN");
            sourceContract.setIdentifierType("CUST_ID");
            sourceContract.setOutputIdentifierType("CUST_ID");
            sourceContract.setSourceSystem("零售发布域");
            sourceContract.setTimeSemantic("交易日期");
            sourceContract.setCompletenessLevel("FULL");
            sourceContract.setSensitivityLevel("S1");
            sourceContract.setStartDate(LocalDate.of(2014, 1, 1));
            sourceContract.setMaterialSourceNote("集成测试补齐最小发布门禁");
            sourceContract.setNotes("用于验证发布快照绑定");
            stamp(sourceContract, now);
            sourceContractMapper.save(sourceContract);
        }

        if (dictionaryMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty()) {
            DictionaryPO dictionary = new DictionaryPO();
            dictionary.setSceneId(sceneId);
            dictionary.setPlanId(primaryPlan.getId());
            dictionary.setDictCode("DICT-" + sceneId + "-MAIN");
            dictionary.setDictName("客户状态字典");
            dictionary.setDictCategory("ENUM");
            dictionary.setDictVersion("v1");
            dictionary.setReleaseStatus("PUBLISHED");
            dictionary.setEntriesJson("[{\"code\":\"01\",\"name\":\"正常\"}]");
            dictionary.setReferencedByJson("[\"output_contract:main\"]");
            stamp(dictionary, now);
            dictionaryMapper.save(dictionary);
        }

        if (identifierLineageMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty()) {
            IdentifierLineagePO lineage = new IdentifierLineagePO();
            lineage.setSceneId(sceneId);
            lineage.setPlanId(primaryPlan.getId());
            lineage.setLineageCode("LIN-" + sceneId + "-MAIN");
            lineage.setLineageName("客户号映射链");
            lineage.setIdentifierType("CUST_ID");
            lineage.setSourceIdentifierType("SRC_CUST_ID");
            lineage.setTargetIdentifierType("CUST_ID");
            lineage.setMappingRulesJson("[{\"rule\":\"trim\"}]");
            lineage.setEvidenceRefsJson("[\"evidence:integration-test\"]");
            lineage.setConfirmationStatus("CONFIRMED");
            stamp(lineage, now);
            identifierLineageMapper.save(lineage);
        }

        if (timeSemanticSelectorMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty()) {
            TimeSemanticSelectorPO selector = new TimeSemanticSelectorPO();
            selector.setSceneId(sceneId);
            selector.setPlanId(primaryPlan.getId());
            selector.setSelectorCode("TIME-" + sceneId + "-MAIN");
            selector.setSelectorName("时间语义选择器");
            selector.setDefaultSemantic("交易日期");
            selector.setCandidateSemanticsJson("[\"交易日期\",\"记账日期\"]");
            selector.setClarificationTermsJson("[\"当日\",\"最近\"]");
            selector.setPriorityRulesJson("[{\"priority\":1,\"semantic\":\"交易日期\"}]");
            selector.setMustClarifyFlag(false);
            stamp(selector, now);
            timeSemanticSelectorMapper.save(selector);
        }
    }

    private void stamp(AbstractSnapshotGraphAuditablePO po, OffsetDateTime now) {
        po.setStatus("DRAFT");
        po.setCreatedBy("tester");
        po.setCreatedAt(now);
        po.setUpdatedBy("tester");
        po.setUpdatedAt(now);
    }
}
