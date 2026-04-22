package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneVersionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ContractViewMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CoverageDeclarationMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.InputSlotSchemaMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanEvidenceRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanPolicyRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PolicyMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneVersionPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.AbstractGraphAuditablePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.AbstractSnapshotGraphAuditablePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.ContractViewPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CoverageDeclarationPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EvidenceFragmentPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.InputSlotSchemaPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.OutputContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanEvidenceRefPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPolicyRefPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PolicyPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceContractPO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KnowledgePackageApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SceneMapper sceneMapper;

    @Autowired
    private SceneVersionMapper sceneVersionMapper;

    @Autowired
    private PlanMapper planMapper;

    @Autowired
    private OutputContractMapper outputContractMapper;

    @Autowired
    private InputSlotSchemaMapper inputSlotSchemaMapper;

    @Autowired
    private ContractViewMapper contractViewMapper;

    @Autowired
    private SourceContractMapper sourceContractMapper;

    @Autowired
    private CoverageDeclarationMapper coverageDeclarationMapper;

    @Autowired
    private PolicyMapper policyMapper;

    @Autowired
    private PlanPolicyRefMapper planPolicyRefMapper;

    @Autowired
    private EvidenceFragmentMapper evidenceFragmentMapper;

    @Autowired
    private PlanEvidenceRefMapper planEvidenceRefMapper;

    @Test
    void shouldReturnKnowledgePackageForPayrollSample() throws Exception {
        String token = loginAndGetToken("support", "support123");
        Fixture fixture = seedPayrollFixture();

        mockMvc.perform(post("/api/graphrag/query")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifierType": "PROTOCOL_NBR",
                                  "identifierValue": "AGR-2021-0001",
                                  "dateFrom": "2021-01-01",
                                  "dateTo": "2021-12-31",
                                  "requestedFields": ["协议号", "交易日期", "金额"],
                                  "purpose": "代发样板回放",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("allow"))
                .andExpect(jsonPath("$.runtimeMode").value("FULL_MATCH"))
                .andExpect(jsonPath("$.degradeReasonCodes").isArray())
                .andExpect(jsonPath("$.degradeReasonCodes[0]").value("ALLOW"))
                .andExpect(jsonPath("$.scene.sceneCode").value(fixture.sceneCode))
                .andExpect(jsonPath("$.plan.planCode").value(fixture.currentPlanCode))
                .andExpect(jsonPath("$.coverage.status").value("FULL"))
                .andExpect(jsonPath("$.trace.snapshotId").isNumber())
                .andExpect(jsonPath("$.trace.inferenceSnapshotId").isNumber())
                .andExpect(jsonPath("$.trace.versionTag").value(fixture.versionTag))
                .andExpect(jsonPath("$.trace.retrievalAdapter").value("LightRAG"))
                .andExpect(jsonPath("$.trace.retrievalStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.trace.fallbackToFormal").value(false))
                .andExpect(jsonPath("$.experiment.adapterName").value("LightRAG"))
                .andExpect(jsonPath("$.experiment.status").value("COMPLETED"))
                .andExpect(jsonPath("$.experiment.candidateScenes[0].sceneCode").value(fixture.sceneCode))
                .andExpect(jsonPath("$.experiment.referenceRefs[0]").isString())
                .andExpect(jsonPath("$.experiment.candidateEvidence[0].evidenceCode").isString());

        mockMvc.perform(post("/api/graphrag/query")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifierType": "CUST_ID",
                                  "identifierValue": "CUST-2008-0009",
                                  "dateFrom": "2011-01-01",
                                  "dateTo": "2011-12-31",
                                  "requestedFields": ["协议号", "身份证号"],
                                  "purpose": "代发样板回放",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("need_approval"))
                .andExpect(jsonPath("$.runtimeMode").value("PARTIAL_WITH_APPROVAL"))
                .andExpect(jsonPath("$.degradeReasonCodes").isArray())
                .andExpect(jsonPath("$.degradeReasonCodes[0]").value("PARTIAL_COVERAGE_APPROVAL"))
                .andExpect(jsonPath("$.reasonCode").value("PARTIAL_COVERAGE_APPROVAL"))
                .andExpect(jsonPath("$.plan.planCode").value(fixture.historyPlanCode))
                .andExpect(jsonPath("$.plan.resolvedIdentifierType").value("PROTOCOL_NBR"))
                .andExpect(jsonPath("$.coverage.status").value("PARTIAL"))
                .andExpect(jsonPath("$.policy.approvalRequired").value(true))
                .andExpect(jsonPath("$.trace.retrievalAdapter").value("LightRAG"))
                .andExpect(jsonPath("$.experiment.adapterName").value("LightRAG"))
                .andExpect(jsonPath("$.experiment.fallbackToFormal").value(false));

        MvcResult gapResult = mockMvc.perform(post("/api/graphrag/query")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifierType": "PROTOCOL_NBR",
                                  "identifierValue": "AGR-2003-0001",
                                  "dateFrom": "2003-01-01",
                                  "dateTo": "2003-12-31",
                                  "requestedFields": ["协议号", "金额"],
                                  "purpose": "代发样板回放",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("deny"))
                .andExpect(jsonPath("$.runtimeMode").value("DENIED"))
                .andExpect(jsonPath("$.degradeReasonCodes").isArray())
                .andExpect(jsonPath("$.degradeReasonCodes[0]").value("COVERAGE_GAP"))
                .andExpect(jsonPath("$.reasonCode").value("COVERAGE_GAP"))
                .andExpect(jsonPath("$.coverage.status").value("GAP"))
                .andExpect(jsonPath("$.coverage.coverageExplanation").value("2004年前暂无代发明细覆盖"))
                .andExpect(jsonPath("$.trace.inferenceSnapshotId").isNumber())
                .andReturn();

        JsonNode gapPayload = objectMapper.readTree(gapResult.getResponse().getContentAsString());
        assertThat(gapPayload.path("trace").path("traceId").asText()).startsWith("KP-");

        mockMvc.perform(post("/api/graphrag/query")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifierType": "PROTOCOL_NBR",
                                  "identifierValue": "AGR-2012-2021",
                                  "dateFrom": "2012-01-01",
                                  "dateTo": "2021-12-31",
                                  "requestedFields": ["协议号", "金额"],
                                  "purpose": "代发样板回放",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("deny"))
                .andExpect(jsonPath("$.reasonCode").value("CROSS_PLAN_RANGE_UNSUPPORTED"))
                .andExpect(jsonPath("$.coverage.status").value("PARTIAL"));

        mockMvc.perform(post("/api/graphrag/query")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifierType": "PROTOCOL_NBR",
                                  "identifierValue": "",
                                  "queryText": "按协议号查询代发明细",
                                  "dateFrom": "2021-01-01",
                                  "dateTo": "2021-12-31",
                                  "requestedFields": ["协议号", "金额"],
                                  "purpose": "代发样板回放",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("deny"))
                .andExpect(jsonPath("$.runtimeMode").value("INPUT_INVALID"))
                .andExpect(jsonPath("$.reasonCode").value("IDENTIFIER_REQUIRED"))
                .andExpect(jsonPath("$.degradeReasonCodes").isArray())
                .andExpect(jsonPath("$.degradeReasonCodes[0]").value("IDENTIFIER_REQUIRED"))
                .andExpect(jsonPath("$.trace.inferenceSnapshotId").isNumber());
    }

    @Test
    void shouldResolvePayrollQuestionToCorrectSceneAndPlan() throws Exception {
        String token = loginAndGetToken("support", "support123");
        MultiSceneFixture fixture = seedMultiScenePayrollFixture();

        mockMvc.perform(post("/api/graphrag/query")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "queryText": "按协议号查询代发明细",
                                  "identifierType": "PROTOCOL_NBR",
                                  "identifierValue": "AGR-2024-0001",
                                  "dateFrom": "2024-01-01",
                                  "dateTo": "2024-12-31",
                                  "requestedFields": ["协议号", "交易日期", "金额"],
                                  "purpose": "代发多场景回放",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("allow"))
                .andExpect(jsonPath("$.reasonCode").value("ALLOW"))
                .andExpect(jsonPath("$.scene.sceneCode").value(fixture.detailSceneCode))
                .andExpect(jsonPath("$.plan.planCode").value(fixture.detailPlanCode))
                .andExpect(jsonPath("$.clarification").doesNotExist())
                .andExpect(jsonPath("$.trace.snapshotId").isNumber())
                .andExpect(jsonPath("$.trace.versionTag").value(fixture.detailVersionTag));
    }

    @Test
    void shouldReturnClarificationForCrossScenePayrollQuestion() throws Exception {
        String token = loginAndGetToken("support", "support123");
        MultiSceneFixture fixture = seedMultiScenePayrollFixture();

        mockMvc.perform(post("/api/graphrag/query")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "queryText": "查询公司户最近一年代发批次和协议号明细",
                                  "requestedFields": ["公司户", "协议号", "金额"],
                                  "purpose": "代发多场景回放",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("clarification_only"))
                .andExpect(jsonPath("$.reasonCode").value("MULTI_SCENE_AMBIGUOUS"))
                .andExpect(jsonPath("$.scene").doesNotExist())
                .andExpect(jsonPath("$.plan").doesNotExist())
                .andExpect(jsonPath("$.clarification.summary").value("当前问题同时命中代发明细查询和代发批次结果查询，请拆分后分别检索"))
                .andExpect(jsonPath("$.clarification.sceneCandidates[0].sceneCode").value(fixture.detailSceneCode))
                .andExpect(jsonPath("$.clarification.sceneCandidates[1].sceneCode").value(fixture.batchSceneCode))
                .andExpect(jsonPath("$.clarification.planCandidates[0].planCode").value(fixture.detailPlanCode))
                .andExpect(jsonPath("$.clarification.planCandidates[1].planCode").value(fixture.batchPlanCode))
                .andExpect(jsonPath("$.clarification.subQuestions[0]").value("按协议号查询代发明细"))
                .andExpect(jsonPath("$.clarification.subQuestions[1]").value("按公司户查询代发批次结果"))
                .andExpect(jsonPath("$.clarification.mergeHints[0]").value("请先选择「代发明细查询」或「代发批次结果查询」，再分别提交运行请求"))
                .andExpect(jsonPath("$.trace.traceId").exists());
    }

    @Test
    void shouldKeepProjectionStatusBoundToPublishedSnapshotAfterCreatingNewDraftSnapshot() throws Exception {
        String token = loginAndGetToken("support", "support123");
        Fixture fixture = seedPayrollFixture();

        mockMvc.perform(post("/api/graphrag/rebuild/{sceneId}", fixture.sceneId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sceneId").value(fixture.sceneId()))
                .andExpect(jsonPath("$.snapshotId").value(fixture.snapshotId()));

        mockMvc.perform(post("/api/scenes/{id}/versions", fixture.sceneId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changeSummary": "发布后新增草稿快照",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNo").value(2));

        mockMvc.perform(get("/api/graphrag/projection/{sceneId}", fixture.sceneId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sceneId").value(fixture.sceneId()))
                .andExpect(jsonPath("$.snapshotId").value(fixture.snapshotId()));
    }

    private Fixture seedPayrollFixture() {
        purgeAllPublishedScenes();
        return seedPayrollFixture("KP-" + System.nanoTime());
    }

    private void purgeAllPublishedScenes() {
        sceneMapper.findAll().stream()
                .filter(s -> s.getStatus() == SceneStatus.PUBLISHED)
                .forEach(s -> {
                    s.setStatus(SceneStatus.DRAFT);
                    sceneMapper.save(s);
                });
    }

    private Fixture seedPayrollFixture(String suffix) {
        OffsetDateTime now = OffsetDateTime.now();
        String sceneCode = "SCN-KP-" + suffix;
        String versionTag = sceneCode + "-V001";

        ScenePO scene = new ScenePO();
        scene.setSceneCode(sceneCode);
        scene.setSceneTitle("代发明细查询-" + suffix);
        scene.setSceneType("FACT_DETAIL");
        scene.setStatus(SceneStatus.PUBLISHED);
        scene.setSceneDescription("代发明细样板场景");
        scene.setCreatedBy("support");
        scene.setCreatedAt(now);
        scene.setUpdatedAt(now);
        scene.setPublishedBy("support");
        scene.setPublishedAt(now);
        scene = sceneMapper.save(scene);

        SceneVersionPO version = new SceneVersionPO();
        version.setSceneId(scene.getId());
        version.setVersionNo(1);
        version.setVersionTag(versionTag);
        version.setSnapshotJson("{\"scene\":\"payroll\"}");
        version.setSnapshotSummaryJson("{\"planCount\":2}");
        version.setChangeSummary("首刀样板发布");
        version.setPublishStatus("PUBLISHED");
        version.setPublishedBy("support");
        version.setPublishedAt(now);
        version.setCreatedBy("support");
        version.setCreatedAt(now);
        version = sceneVersionMapper.save(version);

        OutputContractPO outputContract = new OutputContractPO();
        outputContract.setSceneId(scene.getId());
        outputContract.setContractCode("OUT-" + suffix);
        outputContract.setContractName("代发明细输出契约");
        outputContract.setFieldsJson("[\"协议号\",\"交易日期\",\"金额\",\"收款账号\",\"身份证号\",\"银行卡号\"]");
        stamp(outputContract, "PUBLISHED", version.getId(), versionTag, now);
        outputContract = outputContractMapper.save(outputContract);

        InputSlotSchemaPO protocolSlot = new InputSlotSchemaPO();
        protocolSlot.setSceneId(scene.getId());
        protocolSlot.setSlotCode("PROTOCOL_NBR-" + suffix);
        protocolSlot.setSlotName("协议号");
        protocolSlot.setSlotType("STRING");
        protocolSlot.setRequiredFlag(true);
        protocolSlot.setIdentifierCandidatesJson("[\"PROTOCOL_NBR\"]");
        protocolSlot.setClarificationHint("请输入协议号");
        stamp(protocolSlot, "PUBLISHED", version.getId(), versionTag, now);
        inputSlotSchemaMapper.save(protocolSlot);

        InputSlotSchemaPO customerSlot = new InputSlotSchemaPO();
        customerSlot.setSceneId(scene.getId());
        customerSlot.setSlotCode("CUST_ID-" + suffix);
        customerSlot.setSlotName("客户号");
        customerSlot.setSlotType("STRING");
        customerSlot.setRequiredFlag(true);
        customerSlot.setIdentifierCandidatesJson("[\"CUST_ID\"]");
        customerSlot.setClarificationHint("请输入客户号");
        stamp(customerSlot, "PUBLISHED", version.getId(), versionTag, now);
        inputSlotSchemaMapper.save(customerSlot);

        PlanPO currentPlan = new PlanPO();
        currentPlan.setSceneId(scene.getId());
        currentPlan.setPlanCode("PLN-CUR-" + suffix);
        currentPlan.setPlanName("当前明细 2014+");
        currentPlan.setApplicablePeriod("2014+");
        currentPlan.setDefaultTimeSemantic("交易日期");
        currentPlan.setSourceTablesJson("[\"PDM_VHIS.T05_AGN_DTL\"]");
        currentPlan.setRetrievalText("代发当前明细");
        stamp(currentPlan, "PUBLISHED", version.getId(), versionTag, now);
        currentPlan = planMapper.save(currentPlan);

        PlanPO historyPlan = new PlanPO();
        historyPlan.setSceneId(scene.getId());
        historyPlan.setPlanCode("PLN-HIS-" + suffix);
        historyPlan.setPlanName("历史明细 2004-2013");
        historyPlan.setApplicablePeriod("2004-2013");
        historyPlan.setDefaultTimeSemantic("交易日期");
        historyPlan.setSourceTablesJson("[\"LGC_EAM.EPHISTRXP1\",\"LGC_EAM.UNICORE_EPHISTRXP_YEAR\",\"LGC_EDW.ODS_C3A3_A2DPADTLP_YEAR\"]");
        historyPlan.setRetrievalText("代发历史明细");
        stamp(historyPlan, "PUBLISHED", version.getId(), versionTag, now);
        historyPlan = planMapper.save(historyPlan);

        CoverageDeclarationPO currentCoverage = new CoverageDeclarationPO();
        currentCoverage.setPlanId(currentPlan.getId());
        currentCoverage.setCoverageCode("COV-CUR-" + suffix);
        currentCoverage.setCoverageTitle("当前明细覆盖");
        currentCoverage.setCoverageType("PERIOD_TABLE");
        currentCoverage.setCoverageStatus("FULL");
        currentCoverage.setStatementText("2014年及以后当前明细可完整覆盖");
        currentCoverage.setApplicablePeriod("2014+");
        currentCoverage.setActive(true);
        currentCoverage.setStartDate(LocalDate.of(2014, 1, 1));
        stamp(currentCoverage, "PUBLISHED", version.getId(), versionTag, now);
        coverageDeclarationMapper.save(currentCoverage);

        CoverageDeclarationPO historyCoverage = new CoverageDeclarationPO();
        historyCoverage.setPlanId(historyPlan.getId());
        historyCoverage.setCoverageCode("COV-HIS-" + suffix);
        historyCoverage.setCoverageTitle("历史明细覆盖");
        historyCoverage.setCoverageType("LEGACY");
        historyCoverage.setCoverageStatus("PARTIAL");
        historyCoverage.setStatementText("2004-2013历史明细可查询，但需人工确认完整性");
        historyCoverage.setApplicablePeriod("2004-2013");
        historyCoverage.setGapText("2004-2013历史明细存在部分覆盖，需要审批");
        historyCoverage.setActive(true);
        historyCoverage.setStartDate(LocalDate.of(2004, 1, 1));
        historyCoverage.setEndDate(LocalDate.of(2013, 12, 31));
        stamp(historyCoverage, "PUBLISHED", version.getId(), versionTag, now);
        coverageDeclarationMapper.save(historyCoverage);

        CoverageDeclarationPO gapCoverage = new CoverageDeclarationPO();
        gapCoverage.setPlanId(historyPlan.getId());
        gapCoverage.setCoverageCode("COV-GAP-" + suffix);
        gapCoverage.setCoverageTitle("历史缺口说明");
        gapCoverage.setCoverageType("LEGACY");
        gapCoverage.setCoverageStatus("GAP");
        gapCoverage.setStatementText("2004年前暂无代发明细覆盖");
        gapCoverage.setGapText("2004年前暂无代发明细覆盖");
        gapCoverage.setApplicablePeriod("2004前");
        gapCoverage.setActive(true);
        gapCoverage.setEndDate(LocalDate.of(2003, 12, 31));
        stamp(gapCoverage, "PUBLISHED", version.getId(), versionTag, now);
        coverageDeclarationMapper.save(gapCoverage);

        PolicyPO historyPolicy = new PolicyPO();
        historyPolicy.setPolicyCode("PLC-HIS-" + suffix);
        historyPolicy.setPolicyName("历史明细审批策略");
        historyPolicy.setScopeType("PLAN");
        historyPolicy.setScopeRefId(historyPlan.getId());
        historyPolicy.setEffectType("REQUIRE_APPROVAL");
        historyPolicy.setSensitivityLevel("S3");
        historyPolicy.setMaskingRule("按历史审批链路执行");
        stamp(historyPolicy, "ACTIVE", version.getId(), versionTag, now);
        historyPolicy = policyMapper.save(historyPolicy);

        PlanPolicyRefPO historyPolicyRef = new PlanPolicyRefPO();
        historyPolicyRef.setPlanId(historyPlan.getId());
        historyPolicyRef.setPolicyId(historyPolicy.getId());
        historyPolicyRef.setRelationType("DIRECT");
        historyPolicyRef.setCreatedBy("support");
        historyPolicyRef.setCreatedAt(now);
        planPolicyRefMapper.save(historyPolicyRef);

        EvidenceFragmentPO currentEvidence = new EvidenceFragmentPO();
        currentEvidence.setSceneId(scene.getId());
        currentEvidence.setEvidenceCode("EVD-CUR-" + suffix);
        currentEvidence.setTitle("当前明细 SQL 证据");
        currentEvidence.setFragmentText("PDM_VHIS.T05_AGN_DTL 覆盖 2014+ 当前明细");
        currentEvidence.setSourceAnchor("05-口径文档现状-代发明细查询.sql#current");
        currentEvidence.setSourceType("SQL");
        currentEvidence.setConfidenceScore(0.95d);
        stamp(currentEvidence, "PUBLISHED", version.getId(), versionTag, now);
        currentEvidence = evidenceFragmentMapper.save(currentEvidence);

        EvidenceFragmentPO historyEvidence = new EvidenceFragmentPO();
        historyEvidence.setSceneId(scene.getId());
        historyEvidence.setEvidenceCode("EVD-HIS-" + suffix);
        historyEvidence.setTitle("历史明细 SQL 证据");
        historyEvidence.setFragmentText("LGC_EAM / LGC_EDW 历史明细覆盖 2004-2013");
        historyEvidence.setSourceAnchor("05-口径文档现状-代发明细查询.sql#history");
        historyEvidence.setSourceType("SQL");
        historyEvidence.setConfidenceScore(0.92d);
        stamp(historyEvidence, "PUBLISHED", version.getId(), versionTag, now);
        historyEvidence = evidenceFragmentMapper.save(historyEvidence);

        PlanEvidenceRefPO currentEvidenceRef = new PlanEvidenceRefPO();
        currentEvidenceRef.setPlanId(currentPlan.getId());
        currentEvidenceRef.setEvidenceId(currentEvidence.getId());
        currentEvidenceRef.setRelationType("PRIMARY");
        currentEvidenceRef.setCreatedBy("support");
        currentEvidenceRef.setCreatedAt(now);
        planEvidenceRefMapper.save(currentEvidenceRef);

        PlanEvidenceRefPO historyEvidenceRef = new PlanEvidenceRefPO();
        historyEvidenceRef.setPlanId(historyPlan.getId());
        historyEvidenceRef.setEvidenceId(historyEvidence.getId());
        historyEvidenceRef.setRelationType("PRIMARY");
        historyEvidenceRef.setCreatedBy("support");
        historyEvidenceRef.setCreatedAt(now);
        planEvidenceRefMapper.save(historyEvidenceRef);

        ContractViewPO contractView = new ContractViewPO();
        contractView.setSceneId(scene.getId());
        contractView.setOutputContractId(outputContract.getId());
        contractView.setViewCode("CV-" + suffix);
        contractView.setViewName("普通角色契约视图");
        contractView.setRoleScope("GENERAL");
        contractView.setVisibleFieldsJson("[\"协议号\",\"交易日期\",\"金额\",\"收款账号\"]");
        contractView.setRestrictedFieldsJson("[\"身份证号\"]");
        contractView.setForbiddenFieldsJson("[\"银行卡号\"]");
        contractView.setMaskedFieldsJson("[\"收款账号\"]");
        stamp(contractView, "PUBLISHED", version.getId(), versionTag, now);
        contractViewMapper.save(contractView);

        SourceContractPO customerSource = new SourceContractPO();
        customerSource.setSceneId(scene.getId());
        customerSource.setPlanId(historyPlan.getId());
        customerSource.setSourceContractCode("SRC-CUST-" + suffix);
        customerSource.setSourceName("客户号转协议号");
        customerSource.setPhysicalTable("PDM_VHIS.T03_SF_COOP_AGR_INF_S");
        customerSource.setSourceRole("IDENTIFIER");
        customerSource.setIdentifierType("CUST_ID");
        customerSource.setOutputIdentifierType("PROTOCOL_NBR");
        customerSource.setCompletenessLevel("FULL");
        customerSource.setSensitivityLevel("S1");
        stamp(customerSource, "PUBLISHED", version.getId(), versionTag, now);
        sourceContractMapper.save(customerSource);

        SourceContractPO currentSource = new SourceContractPO();
        currentSource.setSceneId(scene.getId());
        currentSource.setPlanId(currentPlan.getId());
        currentSource.setSourceContractCode("SRC-CUR-" + suffix);
        currentSource.setSourceName("当前明细来源");
        currentSource.setPhysicalTable("PDM_VHIS.T05_AGN_DTL");
        currentSource.setSourceRole("DETAIL");
        currentSource.setIdentifierType("PROTOCOL_NBR");
        currentSource.setCompletenessLevel("FULL");
        currentSource.setSensitivityLevel("S1");
        currentSource.setStartDate(LocalDate.of(2014, 1, 1));
        stamp(currentSource, "PUBLISHED", version.getId(), versionTag, now);
        sourceContractMapper.save(currentSource);

        SourceContractPO historySource = new SourceContractPO();
        historySource.setSceneId(scene.getId());
        historySource.setPlanId(historyPlan.getId());
        historySource.setSourceContractCode("SRC-HIS-" + suffix);
        historySource.setSourceName("历史明细来源");
        historySource.setPhysicalTable("LGC_EAM.EPHISTRXP1");
        historySource.setSourceRole("DETAIL");
        historySource.setIdentifierType("PROTOCOL_NBR");
        historySource.setCompletenessLevel("PARTIAL");
        historySource.setSensitivityLevel("S2");
        historySource.setStartDate(LocalDate.of(2004, 1, 1));
        historySource.setEndDate(LocalDate.of(2013, 12, 31));
        stamp(historySource, "PUBLISHED", version.getId(), versionTag, now);
        sourceContractMapper.save(historySource);

        return new Fixture(scene.getId(), version.getId(), sceneCode, currentPlan.getPlanCode(), historyPlan.getPlanCode(), versionTag);
    }

    private MultiSceneFixture seedMultiScenePayrollFixture() {
        purgeAllPublishedScenes();
        String suffix = "MS-" + System.nanoTime();
        Fixture detail = seedPayrollFixture(suffix + "-DETAIL");

        OffsetDateTime now = OffsetDateTime.now();
        String batchSceneCode = "SCN-KP-" + suffix + "-BATCH";
        String batchVersionTag = batchSceneCode + "-V001";

        ScenePO batchScene = new ScenePO();
        batchScene.setSceneCode(batchSceneCode);
        batchScene.setSceneTitle("代发批次结果查询-" + suffix);
        batchScene.setSceneType("FACT_AGGREGATION");
        batchScene.setStatus(SceneStatus.PUBLISHED);
        batchScene.setSceneDescription("按公司户查询代发批次结果");
        batchScene.setCreatedBy("support");
        batchScene.setCreatedAt(now);
        batchScene.setUpdatedAt(now);
        batchScene.setPublishedBy("support");
        batchScene.setPublishedAt(now);
        batchScene = sceneMapper.save(batchScene);

        SceneVersionPO batchVersion = new SceneVersionPO();
        batchVersion.setSceneId(batchScene.getId());
        batchVersion.setVersionNo(1);
        batchVersion.setVersionTag(batchVersionTag);
        batchVersion.setSnapshotJson("{\"scene\":\"payroll-batch\"}");
        batchVersion.setSnapshotSummaryJson("{\"planCount\":1}");
        batchVersion.setChangeSummary("批次结果样板发布");
        batchVersion.setPublishStatus("PUBLISHED");
        batchVersion.setPublishedBy("support");
        batchVersion.setPublishedAt(now);
        batchVersion.setCreatedBy("support");
        batchVersion.setCreatedAt(now);
        batchVersion = sceneVersionMapper.save(batchVersion);

        OutputContractPO batchOutputContract = new OutputContractPO();
        batchOutputContract.setSceneId(batchScene.getId());
        batchOutputContract.setContractCode("OUT-" + suffix + "-BATCH");
        batchOutputContract.setContractName("代发批次结果输出契约");
        batchOutputContract.setFieldsJson("[\"公司户\",\"批次号\",\"批次日期\",\"成功笔数\",\"金额\"]");
        stamp(batchOutputContract, "PUBLISHED", batchVersion.getId(), batchVersionTag, now);
        batchOutputContract = outputContractMapper.save(batchOutputContract);

        InputSlotSchemaPO orgAccountSlot = new InputSlotSchemaPO();
        orgAccountSlot.setSceneId(batchScene.getId());
        orgAccountSlot.setSlotCode("ORG_ACCOUNT-" + suffix);
        orgAccountSlot.setSlotName("公司户");
        orgAccountSlot.setSlotType("STRING");
        orgAccountSlot.setRequiredFlag(true);
        orgAccountSlot.setIdentifierCandidatesJson("[\"ORG_ACCOUNT\"]");
        orgAccountSlot.setClarificationHint("请输入公司户");
        stamp(orgAccountSlot, "PUBLISHED", batchVersion.getId(), batchVersionTag, now);
        inputSlotSchemaMapper.save(orgAccountSlot);

        PlanPO batchPlan = new PlanPO();
        batchPlan.setSceneId(batchScene.getId());
        batchPlan.setPlanCode("PLN-BATCH-" + suffix);
        batchPlan.setPlanName("批次结果 2019+");
        batchPlan.setApplicablePeriod("2019+");
        batchPlan.setDefaultTimeSemantic("批次日期");
        batchPlan.setSourceTablesJson("[\"PDM_VHIS.T06_AGN_BATCH_SUMMARY\"]");
        batchPlan.setRetrievalText("代发批次结果");
        stamp(batchPlan, "PUBLISHED", batchVersion.getId(), batchVersionTag, now);
        batchPlan = planMapper.save(batchPlan);

        CoverageDeclarationPO batchCoverage = new CoverageDeclarationPO();
        batchCoverage.setPlanId(batchPlan.getId());
        batchCoverage.setCoverageCode("COV-BATCH-" + suffix);
        batchCoverage.setCoverageTitle("批次结果覆盖");
        batchCoverage.setCoverageType("PERIOD_TABLE");
        batchCoverage.setCoverageStatus("FULL");
        batchCoverage.setStatementText("2019年及以后批次结果可完整覆盖");
        batchCoverage.setApplicablePeriod("2019+");
        batchCoverage.setActive(true);
        batchCoverage.setStartDate(LocalDate.of(2019, 1, 1));
        stamp(batchCoverage, "PUBLISHED", batchVersion.getId(), batchVersionTag, now);
        coverageDeclarationMapper.save(batchCoverage);

        ContractViewPO batchContractView = new ContractViewPO();
        batchContractView.setSceneId(batchScene.getId());
        batchContractView.setPlanId(batchPlan.getId());
        batchContractView.setOutputContractId(batchOutputContract.getId());
        batchContractView.setViewCode("CV-BATCH-" + suffix);
        batchContractView.setViewName("批次结果契约视图");
        batchContractView.setRoleScope("GENERAL");
        batchContractView.setVisibleFieldsJson("[\"公司户\",\"批次号\",\"批次日期\",\"成功笔数\",\"金额\"]");
        batchContractView.setRestrictedFieldsJson("[]");
        batchContractView.setForbiddenFieldsJson("[]");
        batchContractView.setMaskedFieldsJson("[]");
        stamp(batchContractView, "PUBLISHED", batchVersion.getId(), batchVersionTag, now);
        contractViewMapper.save(batchContractView);

        EvidenceFragmentPO batchEvidence = new EvidenceFragmentPO();
        batchEvidence.setSceneId(batchScene.getId());
        batchEvidence.setEvidenceCode("EVD-BATCH-" + suffix);
        batchEvidence.setTitle("批次结果 SQL 证据");
        batchEvidence.setFragmentText("PDM_VHIS.T06_AGN_BATCH_SUMMARY 覆盖代发批次结果");
        batchEvidence.setSourceAnchor("05-口径文档现状-代发批次结果.sql#batch");
        batchEvidence.setSourceType("SQL");
        batchEvidence.setConfidenceScore(0.94d);
        stamp(batchEvidence, "PUBLISHED", batchVersion.getId(), batchVersionTag, now);
        batchEvidence = evidenceFragmentMapper.save(batchEvidence);

        PlanEvidenceRefPO batchEvidenceRef = new PlanEvidenceRefPO();
        batchEvidenceRef.setPlanId(batchPlan.getId());
        batchEvidenceRef.setEvidenceId(batchEvidence.getId());
        batchEvidenceRef.setRelationType("PRIMARY");
        batchEvidenceRef.setCreatedBy("support");
        batchEvidenceRef.setCreatedAt(now);
        planEvidenceRefMapper.save(batchEvidenceRef);

        SourceContractPO batchSource = new SourceContractPO();
        batchSource.setSceneId(batchScene.getId());
        batchSource.setPlanId(batchPlan.getId());
        batchSource.setSourceContractCode("SRC-BATCH-" + suffix);
        batchSource.setSourceName("批次结果来源");
        batchSource.setPhysicalTable("PDM_VHIS.T06_AGN_BATCH_SUMMARY");
        batchSource.setSourceRole("AGGREGATION");
        batchSource.setIdentifierType("ORG_ACCOUNT");
        batchSource.setCompletenessLevel("FULL");
        batchSource.setSensitivityLevel("S1");
        batchSource.setStartDate(LocalDate.of(2019, 1, 1));
        stamp(batchSource, "PUBLISHED", batchVersion.getId(), batchVersionTag, now);
        sourceContractMapper.save(batchSource);

        return new MultiSceneFixture(
                detail.sceneCode(),
                detail.currentPlanCode(),
                detail.versionTag(),
                batchSceneCode,
                batchPlan.getPlanCode(),
                batchVersionTag
        );
    }

    private void stamp(AbstractGraphAuditablePO po, String status, OffsetDateTime now) {
        po.setStatus(status);
        po.setCreatedBy("support");
        po.setCreatedAt(now);
        po.setUpdatedBy("support");
        po.setUpdatedAt(now);
    }

    private void stamp(AbstractSnapshotGraphAuditablePO po, String status, Long snapshotId, String versionTag, OffsetDateTime now) {
        stamp((AbstractGraphAuditablePO) po, status, now);
        po.setSnapshotId(snapshotId);
        po.setVersionTag(versionTag);
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

    private record Fixture(Long sceneId,
                           Long snapshotId,
                           String sceneCode,
                           String currentPlanCode,
                           String historyPlanCode,
                           String versionTag) {
    }

    private record MultiSceneFixture(String detailSceneCode,
                                     String detailPlanCode,
                                     String detailVersionTag,
                                     String batchSceneCode,
                                     String batchPlanCode,
                                     String batchVersionTag) {
    }
}
