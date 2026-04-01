package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ContractViewMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.DictionaryMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.IdentifierLineageMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.InputSlotSchemaMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanSchemaLinkMapper;
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
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanSchemaLinkPO;
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

    @Autowired
    private PlanMapper planMapper;

    @Autowired
    private OutputContractMapper outputContractMapper;

    @Autowired
    private InputSlotSchemaMapper inputSlotSchemaMapper;

    @Autowired
    private PlanSchemaLinkMapper planSchemaLinkMapper;

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
    void shouldCompleteM2M3CoreFlow() throws Exception {
        String token = loginAndGetToken("support", "support123");
        long domainId = createDomain(token);
        long sceneId = createScene(token, "M2M3-场景初始");
        updateSceneForPublish(token, sceneId, domainId, "M2M3-场景初始", "M2M3 初始描述");
        prepareMinimumPublishAssets(sceneId);

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
        prepareMinimumPublishAssets(sceneId);
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

        mockMvc.perform(get("/api/service-specs")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sceneId").value(sceneId))
                .andExpect(jsonPath("$[0].specCode").value(specCode))
                .andExpect(jsonPath("$[0].specVersion").value(1));

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

    @Test
    void shouldCreatePublishedSnapshotBeforeGraphProjection() throws Exception {
        String token = loginAndGetToken("support", "support123");
        long domainId = createDomain(token);
        long sceneId = createScene(token, "代发明细查询");
        updateSceneForPublish(token, sceneId, domainId, "代发明细查询", "按协议号查询代发明细");
        prepareMinimumPublishAssets(sceneId);

        MvcResult publishResult = mockMvc.perform(post("/api/scenes/{id}/publish", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verifiedAt": "2026-03-29T10:00:00+08:00",
                                  "changeSummary": "发布代发明细样板",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotId").isNumber())
                .andReturn();

        long snapshotId = objectMapper.readTree(publishResult.getResponse().getContentAsString()).path("snapshotId").asLong();
        assertThat(snapshotId).isPositive();

        mockMvc.perform(get("/api/graphrag/projection/{sceneId}", sceneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sceneId").value(sceneId))
                .andExpect(jsonPath("$.snapshotId").value(snapshotId));

        mockMvc.perform(post("/api/scenes/{id}/versions", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changeSummary": "发布后草稿快照",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNo").value(2));

        mockMvc.perform(get("/api/graphrag/projection/{sceneId}", sceneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sceneId").value(sceneId))
                .andExpect(jsonPath("$.snapshotId").value(snapshotId));
    }

    private long createDomain(String token) throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        MvcResult result = mockMvc.perform(post("/api/domains")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domainCode": "M2M3_DOMAIN_%s",
                                  "domainName": "M2M3 测试域 %s",
                                  "domainOverview": "用于 M2/M3 集成测试",
                                  "operator": "support"
                                }
                                """.formatted(suffix, suffix)))
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

    private void prepareMinimumPublishAssets(long sceneId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<PlanPO> plans = planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        assertThat(plans).isNotEmpty();

        for (PlanPO plan : plans) {
            if (plan.getDefaultTimeSemantic() == null || plan.getDefaultTimeSemantic().isBlank()) {
                plan.setDefaultTimeSemantic("交易日期");
                plan.setUpdatedBy("support");
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
            view.setVisibleFieldsJson("[\"cust_id\"]");
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
            sourceContract.setSourceName("M2M3 主来源");
            sourceContract.setPhysicalTable("DM_CUSTOMER_INFO");
            sourceContract.setSourceRole("DETAIL_MAIN");
            sourceContract.setIdentifierType("CUST_ID");
            sourceContract.setOutputIdentifierType("CUST_ID");
            sourceContract.setSourceSystem("M2M3 测试域");
            sourceContract.setTimeSemantic("交易日期");
            sourceContract.setCompletenessLevel("FULL");
            sourceContract.setSensitivityLevel("S1");
            sourceContract.setStartDate(LocalDate.of(2014, 1, 1));
            sourceContract.setMaterialSourceNote("M2M3 集成测试补齐最小发布门禁");
            sourceContract.setNotes("用于验证发布门禁与快照");
            stamp(sourceContract, now);
            sourceContractMapper.save(sourceContract);
        }

        if (dictionaryMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty()) {
            DictionaryPO dictionary = new DictionaryPO();
            dictionary.setSceneId(sceneId);
            dictionary.setPlanId(primaryPlan.getId());
            dictionary.setDictCode("DICT-" + sceneId + "-DEFAULT");
            dictionary.setDictName("M2M3 默认术语字典");
            dictionary.setDictCategory("BUSINESS_TERM");
            dictionary.setDictVersion("v1");
            dictionary.setReleaseStatus("ACTIVE");
            dictionary.setEntriesJson("[{\"term\":\"cust_id\",\"meaning\":\"客户号\"}]");
            dictionary.setReferencedByJson("[\"PLAN:" + primaryPlan.getPlanCode() + "\"]");
            stamp(dictionary, now);
            dictionaryMapper.save(dictionary);
        }

        if (identifierLineageMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty()) {
            IdentifierLineagePO lineage = new IdentifierLineagePO();
            lineage.setSceneId(sceneId);
            lineage.setPlanId(primaryPlan.getId());
            lineage.setLineageCode("LIN-" + sceneId + "-CUST");
            lineage.setLineageName("客户号到客户号");
            lineage.setIdentifierType("CUST_ID");
            lineage.setSourceIdentifierType("CUST_ID");
            lineage.setTargetIdentifierType("CUST_ID");
            lineage.setMappingRulesJson("[{\"rule\":\"identity\"}]");
            lineage.setEvidenceRefsJson("[]");
            lineage.setConfirmationStatus("CONFIRMED");
            stamp(lineage, now);
            identifierLineageMapper.save(lineage);
        }

        if (timeSemanticSelectorMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty()) {
            TimeSemanticSelectorPO selector = new TimeSemanticSelectorPO();
            selector.setSceneId(sceneId);
            selector.setPlanId(primaryPlan.getId());
            selector.setSelectorCode("TS-" + sceneId + "-DEFAULT");
            selector.setSelectorName("默认时间语义");
            selector.setDefaultSemantic("交易日期");
            selector.setCandidateSemanticsJson("[\"交易日期\"]");
            selector.setClarificationTermsJson("[\"交易日期\"]");
            selector.setPriorityRulesJson("[{\"priority\":1,\"semantic\":\"交易日期\"}]");
            selector.setMustClarifyFlag(false);
            stamp(selector, now);
            timeSemanticSelectorMapper.save(selector);
        }

        for (PlanPO plan : plans) {
            boolean hasSchemaLink = !planSchemaLinkMapper.findByPlanIdAndStatus(plan.getId(), "DRAFT").isEmpty()
                    || !planSchemaLinkMapper.findByPlanIdAndStatus(plan.getId(), "PUBLISHED").isEmpty();
            if (!hasSchemaLink) {
                PlanSchemaLinkPO schemaLink = new PlanSchemaLinkPO();
                schemaLink.setPlanId(plan.getId());
                schemaLink.setTableName("DM_CUSTOMER_INFO");
                schemaLink.setColumnName("CUST_ID");
                schemaLink.setLinkRole("OUTPUT");
                schemaLink.setEvidenceId(null);
                schemaLink.setConfidenceScore(0.9d);
                schemaLink.setStatus("DRAFT");
                schemaLink.setCreatedBy("support");
                schemaLink.setCreatedAt(now);
                schemaLink.setUpdatedBy("support");
                schemaLink.setUpdatedAt(now);
                planSchemaLinkMapper.save(schemaLink);
            }
        }
    }

    private void stamp(AbstractSnapshotGraphAuditablePO po, OffsetDateTime now) {
        po.setStatus("DRAFT");
        po.setCreatedBy("support");
        po.setCreatedAt(now);
        po.setUpdatedBy("support");
        po.setUpdatedAt(now);
    }
}
