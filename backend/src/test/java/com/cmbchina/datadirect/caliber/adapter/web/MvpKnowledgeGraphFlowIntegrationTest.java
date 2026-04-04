package com.cmbchina.datadirect.caliber.adapter.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.cmbchina.datadirect.caliber.application.service.command.graphrag.CanonicalEntityResolutionService;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityMembershipMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityRelationMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalSnapshotMembershipMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.DictionaryMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.IdentifierLineageMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PolicyMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.TimeSemanticSelectorMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.AbstractSnapshotGraphAuditablePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.DictionaryPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EvidenceFragmentPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.IdentifierLineagePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.OutputContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PolicyPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.TimeSemanticSelectorPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MvpKnowledgeGraphFlowIntegrationTest {

    private static final String PAYROLL_SAMPLE = """
            ### 场景标题：根据代发协议号查询代发明细
            - 场景描述：按代发协议号查询代发明细，供工单核验和样板回放使用。
            - 口径提供人：张三/80000001
            - 结果字段：协议号、交易日期、金额、收款账号
            - 注意事项：2014 年至今优先使用当前主表。
            - SQL 语句
            -- Step 1：查询代发明细
            SELECT MCH_AGR_NBR AS PROTOCOL_NBR, TRX_DT, TRX_AMT, EAC_NBR
            FROM PDM_VHIS.T05_AGN_DTL
            WHERE MCH_AGR_NBR = '${PROTOCOL_NBR}'
              AND TRX_DT BETWEEN DATE '${DATE_FROM}' AND DATE '${DATE_TO}';
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlanMapper planMapper;

    @Autowired
    private DictionaryMapper dictionaryMapper;

    @Autowired
    private IdentifierLineageMapper identifierLineageMapper;

    @Autowired
    private TimeSemanticSelectorMapper timeSemanticSelectorMapper;

    @Autowired
    private OutputContractMapper outputContractMapper;

    @Autowired
    private CanonicalEntityResolutionService canonicalEntityResolutionService;

    @Autowired
    private CanonicalEntityMembershipMapper canonicalEntityMembershipMapper;

    @Autowired
    private CanonicalEntityRelationMapper canonicalEntityRelationMapper;

    @Autowired
    private CanonicalSnapshotMembershipMapper canonicalSnapshotMembershipMapper;

    @Autowired
    private PolicyMapper policyMapper;

    @Autowired
    private EvidenceFragmentMapper evidenceFragmentMapper;

    @Test
    void shouldFinishImportPublishGraphAndRetrievalMvpFlow() throws Exception {
        String token = loginAndGetToken("support", "support123");
        long domainId = createDomain(token);

        JsonNode preprocessPayload = importPayrollSample(token);
        String taskId = preprocessPayload.path("importBatchId").asText("");
        assertThat(taskId).isNotBlank();

        JsonNode candidateGraph = fetchCandidateGraph(token, taskId);
        String candidateCode = firstSceneCandidateCode(candidateGraph.path("nodes"));
        String timeSemanticNodeCode = firstNodeCode(candidateGraph.path("nodes"), "TIME_SEMANTIC");
        String sourceTableNodeCode = firstNodeCode(candidateGraph.path("nodes"), "SOURCE_TABLE");

        mockMvc.perform(post("/api/import/tasks/{taskId}/candidate-graph/review", taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType":"NODE",
                                  "targetCode":"%s",
                                  "action":"ACCEPT",
                                  "reason":"候选时间语义进入正式载荷",
                                  "operator":"support"
                                }
                                """.formatted(timeSemanticNodeCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.pendingReviewTotal").isNumber());

        mockMvc.perform(post("/api/import/tasks/{taskId}/candidate-graph/review", taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType":"NODE",
                                  "targetCode":"%s",
                                  "action":"ACCEPT",
                                  "reason":"候选来源表进入正式载荷",
                                  "operator":"support"
                                }
                                """.formatted(sourceTableNodeCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.pendingReviewTotal").isNumber());

        MvcResult confirmResult = mockMvc.perform(post("/api/import/candidates/{candidateCode}/confirm", candidateCode)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domainId": %d,
                                  "operator": "support"
                                }
                                """.formatted(domainId)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode confirmedPayload = objectMapper.readTree(confirmResult.getResponse().getContentAsString());
        JsonNode confirmedScene = confirmedPayload.path("scene");
        long sceneId = confirmedScene.path("id").asLong();
        assertThat(sceneId).isPositive();
        assertThat(confirmedScene.path("sqlVariantsJson").asText()).contains("default_time_semantic");
        assertThat(confirmedScene.path("sqlVariantsJson").asText()).contains("TRX_DT");
        assertThat(confirmedScene.path("sqlVariantsJson").asText()).contains("PDM_VHIS.T05_AGN_DTL");
        assertThat(confirmedPayload.path("governanceSummary").path("publishReady").asBoolean()).isFalse();
        assertThat(confirmedPayload.path("governanceSummary").path("failedRules")).hasSize(3);
        assertThat(confirmedPayload.path("governanceSummary").path("openBlockingGaps")).hasSize(3);

        mockMvc.perform(get("/api/publish-checks/{sceneId}", sceneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishReady").value(false))
                .andExpect(jsonPath("$.failedRules.length()").value(3))
                .andExpect(jsonPath("$.openBlockingGaps.length()").value(3));

        mockMvc.perform(post("/api/import/tasks/{taskId}/quality-confirm", taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualityConfirmed").value(true));

        mockMvc.perform(post("/api/import/tasks/{taskId}/compare-confirm", taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compareConfirmed").value(true));

        JsonNode currentScene = fetchScene(token, sceneId);
        ObjectNode updateRequest = objectMapper.createObjectNode();
        updateRequest.put("sceneTitle", currentScene.path("sceneTitle").asText());
        updateRequest.put("domainId", domainId);
        updateRequest.put("domain", "");
        updateRequest.put("sceneType", currentScene.path("sceneType").asText("FACT_DETAIL"));
        updateRequest.put("sceneDescription", currentScene.path("sceneDescription").asText(""));
        updateRequest.put("caliberDefinition", currentScene.path("caliberDefinition").asText(""));
        updateRequest.put("applicability", currentScene.path("applicability").asText(""));
        updateRequest.put("boundaries", currentScene.path("boundaries").asText(""));
        updateRequest.put("inputsJson", currentScene.path("inputsJson").asText("{}"));
        updateRequest.put("outputsJson", currentScene.path("outputsJson").asText("{}"));
        updateRequest.put("sqlVariantsJson", currentScene.path("sqlVariantsJson").asText("[]"));
        updateRequest.put("codeMappingsJson", currentScene.path("codeMappingsJson").asText("[]"));
        updateRequest.put("contributors", currentScene.path("contributors").asText(""));
        updateRequest.put("sqlBlocksJson", currentScene.path("sqlBlocksJson").asText("[]"));
        updateRequest.put("sourceTablesJson", currentScene.path("sourceTablesJson").asText("[]"));
        updateRequest.put("caveatsJson", currentScene.path("caveatsJson").asText("[]"));
        updateRequest.put("unmappedText", currentScene.path("unmappedText").asText(""));
        updateRequest.put("qualityJson", currentScene.path("qualityJson").asText("{}"));
        updateRequest.put("rawInput", currentScene.path("rawInput").asText(PAYROLL_SAMPLE));
        updateRequest.put("expectedVersion", currentScene.path("rowVersion").asLong());
        updateRequest.put("operator", "support");

        mockMvc.perform(put("/api/scenes/{id}", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domainId").value(domainId));

        ensureGovernanceAssets(sceneId);

        MvcResult publishResult = mockMvc.perform(post("/api/scenes/{id}/publish", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verifiedAt": "2026-03-27T10:00:00+08:00",
                                  "changeSummary": "MVP 导入样例发布",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.snapshotId").isNumber())
                .andReturn();
        JsonNode publishPayload = objectMapper.readTree(publishResult.getResponse().getContentAsString());
        long snapshotId = publishPayload.path("snapshotId").asLong();
        assertThat(snapshotId).as("publish response must contain a positive snapshotId").isPositive();

        mockMvc.perform(post("/api/import/tasks/{taskId}/complete", taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/publish-checks/{sceneId}", sceneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishReady").value(true));

        MvcResult projectionResult = mockMvc.perform(get("/api/graphrag/projection/{sceneId}", sceneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode projectionPayload = objectMapper.readTree(projectionResult.getResponse().getContentAsString());
        assertThat(projectionPayload.path("status").asText("")).isNotBlank();
        assertThat(projectionPayload.path("status").asText("")).isNotEqualTo("FAILED");
        assertThat(projectionPayload.path("snapshotId").asLong())
                .as("projection status must reference the same snapshotId as publish")
                .isEqualTo(snapshotId);

        mockMvc.perform(get("/api/assets/lineage/{sceneId}", sceneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.edges").isArray());

        MvcResult dataMapGraphResult = mockMvc.perform(get("/api/datamap/graph")
                        .header("Authorization", "Bearer " + token)
                        .param("root_type", "SCENE")
                        .param("root_id", String.valueOf(sceneId))
                        .param("snapshot_id", String.valueOf(snapshotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootRef").isString())
                .andExpect(jsonPath("$.sceneId").value(sceneId))
                .andExpect(jsonPath("$.readSource").value("RELATIONAL"))
                .andExpect(jsonPath("$.projectionVerificationStatus").value("SKIPPED"))
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.edges").isArray())
                .andReturn();
        JsonNode dataMapGraph = objectMapper.readTree(dataMapGraphResult.getResponse().getContentAsString());
        assertThat(dataMapGraph.path("rootRef").asText("")).isNotBlank();
        assertThat(dataMapGraph.path("nodes").isArray()).isTrue();
        assertThat(dataMapGraph.path("nodes")).isNotEmpty();
        assertThat(dataMapGraph.path("projectionVerifiedAt").isNull()).isTrue();

        for (JsonNode node : dataMapGraph.path("nodes")) {
            String nodeType = node.path("objectType").asText("");
            if (!java.util.Set.of("SCENE", "DOMAIN", "VERSION_SNAPSHOT").contains(nodeType)) {
                assertThat(node.path("snapshotId").asLong())
                        .as("data-map node %s (%s) must belong to the published snapshot",
                                node.path("id").asText(), nodeType)
                        .isEqualTo(snapshotId);
            }
        }

        String assetRef = findNodeIdByType(dataMapGraph.path("nodes"), "PLAN", "SCENE");
        assertThat(assetRef).isNotBlank();

        mockMvc.perform(get("/api/datamap/node/{id}/detail", assetRef)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetRef").value(assetRef))
                .andExpect(jsonPath("$.node.id").value(assetRef))
                .andExpect(jsonPath("$.node.objectType").isString())
                .andExpect(jsonPath("$.attributes.object_type").isString());

        mockMvc.perform(post("/api/datamap/impact-analysis")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetRef": "%s",
                                  "snapshotId": %d
                                }
                                """.formatted(assetRef, snapshotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetRef").value(assetRef))
                .andExpect(jsonPath("$.riskLevel").isString())
                .andExpect(jsonPath("$.recommendedActions").isArray())
                .andExpect(jsonPath("$.graph.readSource").value("RELATIONAL"))
                .andExpect(jsonPath("$.graph.projectionVerificationStatus").value("SKIPPED"))
                .andExpect(jsonPath("$.graph.nodes").isArray())
                .andExpect(jsonPath("$.graph.edges").isArray());

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
                                  "purpose": "MVP 样例回放",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scene.sceneTitle").value(currentScene.path("sceneTitle").asText()))
                .andExpect(jsonPath("$.plan.planName").isString())
                .andExpect(jsonPath("$.trace.traceId").isString())
                .andExpect(jsonPath("$.decision").isString());
    }

    @Test
    void shouldExposeDomainGraphUsingPublishedSnapshotScopedMemberships() throws Exception {
        String token = loginAndGetToken("support", "support123");
        long domainId = createDomain(token);

        JsonNode preprocessPayload = importPayrollSample(token);
        String taskId = preprocessPayload.path("importBatchId").asText("");
        assertThat(taskId).isNotBlank();

        JsonNode candidateGraph = fetchCandidateGraph(token, taskId);
        String candidateCode = firstSceneCandidateCode(candidateGraph.path("nodes"));
        String timeSemanticNodeCode = firstNodeCode(candidateGraph.path("nodes"), "TIME_SEMANTIC");
        String sourceTableNodeCode = firstNodeCode(candidateGraph.path("nodes"), "SOURCE_TABLE");

        mockMvc.perform(post("/api/import/tasks/{taskId}/candidate-graph/review", taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType":"NODE",
                                  "targetCode":"%s",
                                  "action":"ACCEPT",
                                  "reason":"候选时间语义进入正式载荷",
                                  "operator":"support"
                                }
                                """.formatted(timeSemanticNodeCode)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/import/tasks/{taskId}/candidate-graph/review", taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType":"NODE",
                                  "targetCode":"%s",
                                  "action":"ACCEPT",
                                  "reason":"候选来源表进入正式载荷",
                                  "operator":"support"
                                }
                                """.formatted(sourceTableNodeCode)))
                .andExpect(status().isOk());

        MvcResult confirmResult = mockMvc.perform(post("/api/import/candidates/{candidateCode}/confirm", candidateCode)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domainId": %d,
                                  "operator": "support"
                                }
                                """.formatted(domainId)))
                .andExpect(status().isOk())
                .andReturn();
        long sceneId = objectMapper.readTree(confirmResult.getResponse().getContentAsString()).path("scene").path("id").asLong();
        ensureGovernanceAssets(sceneId);

        JsonNode currentScene = fetchScene(token, sceneId);
        ObjectNode updateRequest = objectMapper.createObjectNode();
        updateRequest.put("sceneTitle", currentScene.path("sceneTitle").asText());
        updateRequest.put("domainId", domainId);
        updateRequest.put("domain", "");
        updateRequest.put("sceneType", currentScene.path("sceneType").asText("FACT_DETAIL"));
        updateRequest.put("sceneDescription", currentScene.path("sceneDescription").asText(""));
        updateRequest.put("caliberDefinition", currentScene.path("caliberDefinition").asText(""));
        updateRequest.put("applicability", currentScene.path("applicability").asText(""));
        updateRequest.put("boundaries", currentScene.path("boundaries").asText(""));
        updateRequest.put("inputsJson", currentScene.path("inputsJson").asText("{}"));
        updateRequest.put("outputsJson", currentScene.path("outputsJson").asText("{}"));
        updateRequest.put("sqlVariantsJson", currentScene.path("sqlVariantsJson").asText("[]"));
        updateRequest.put("codeMappingsJson", currentScene.path("codeMappingsJson").asText("[]"));
        updateRequest.put("contributors", currentScene.path("contributors").asText(""));
        updateRequest.put("sqlBlocksJson", currentScene.path("sqlBlocksJson").asText("[]"));
        updateRequest.put("sourceTablesJson", currentScene.path("sourceTablesJson").asText("[]"));
        updateRequest.put("caveatsJson", currentScene.path("caveatsJson").asText("[]"));
        updateRequest.put("unmappedText", currentScene.path("unmappedText").asText(""));
        updateRequest.put("qualityJson", currentScene.path("qualityJson").asText("{}"));
        updateRequest.put("rawInput", currentScene.path("rawInput").asText(PAYROLL_SAMPLE));
        updateRequest.put("expectedVersion", currentScene.path("rowVersion").asLong());
        updateRequest.put("operator", "support");

        mockMvc.perform(put("/api/scenes/{id}", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        ensureCanonicalRelationAssets(sceneId);

        MvcResult publishResult = mockMvc.perform(post("/api/scenes/{id}/publish", sceneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verifiedAt": "2026-03-27T10:00:00+08:00",
                                  "changeSummary": "DOMAIN 图读红测发布",
                                  "operator": "support"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long snapshotId = objectMapper.readTree(publishResult.getResponse().getContentAsString()).path("snapshotId").asLong();
        assertThat(snapshotId).isPositive();
        assertThat(canonicalSnapshotMembershipMapper.findBySnapshotIdAndSceneIdOrderByUpdatedAtDesc(snapshotId, sceneId))
                .as("发布后的 canonical snapshot membership 必须包含 EVIDENCE，才能暴露 SUPPORTED_BY")
                .anyMatch(item -> "EVIDENCE".equals(item.getSceneAssetType()) && item.getCanonicalEntityId() != null);

        MvcResult domainGraphResult = mockMvc.perform(get("/api/datamap/graph")
                        .header("Authorization", "Bearer " + token)
                        .param("root_type", "DOMAIN")
                        .param("root_id", String.valueOf(domainId))
                        .param("snapshot_id", String.valueOf(snapshotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootRef").value("domain:" + domainId))
                .andExpect(jsonPath("$.readSource").value("RELATIONAL"))
                .andExpect(jsonPath("$.edges").isArray())
                .andReturn();

        JsonNode domainGraph = objectMapper.readTree(domainGraphResult.getResponse().getContentAsString());
        assertThat(domainGraph.path("edges"))
                .as("DOMAIN 图必须包含来自已发布 scene snapshot 的 SCENE_MEMBERSHIP 边")
                .anyMatch(edge -> "SCENE_MEMBERSHIP".equals(edge.path("relationType").asText()));
        assertThat(domainGraph.path("edges"))
                .as("DOMAIN 图中的 SCENE_MEMBERSHIP 边必须显式返回 relationGroup=control")
                .anyMatch(edge -> "SCENE_MEMBERSHIP".equals(edge.path("relationType").asText())
                        && "control".equals(edge.path("relationGroup").asText()));
        assertThat(domainGraph.path("edges"))
                .as("DOMAIN 图必须包含 scene asset -> canonical entity 的 INSTANCE_OF 边")
                .anyMatch(edge -> "INSTANCE_OF".equals(edge.path("relationType").asText()));
        assertThat(domainGraph.path("edges"))
                .as("DOMAIN 图中的 INSTANCE_OF 边必须显式返回 relationGroup=control")
                .anyMatch(edge -> "INSTANCE_OF".equals(edge.path("relationType").asText())
                        && "control".equals(edge.path("relationGroup").asText()));
        assertThat(domainGraph.path("edges"))
                .as("DOMAIN 图必须包含 canonical entity -> canonical entity 的 MAPS_TO_SOURCE 边")
                .anyMatch(edge -> "MAPS_TO_SOURCE".equals(edge.path("relationType").asText()));
        assertThat(domainGraph.path("edges"))
                .as("DOMAIN 图中的 MAPS_TO_SOURCE 边必须显式返回 relationGroup=metadata")
                .anyMatch(edge -> "MAPS_TO_SOURCE".equals(edge.path("relationType").asText())
                        && "metadata".equals(edge.path("relationGroup").asText()));
        assertThat(domainGraph.path("edges"))
                .as("DOMAIN 图必须包含 canonical entity -> canonical entity 的 APPLIES_POLICY 边")
                .anyMatch(edge -> "APPLIES_POLICY".equals(edge.path("relationType").asText()));
        assertThat(domainGraph.path("edges"))
                .as("DOMAIN 图中的 APPLIES_POLICY 边必须显式返回 relationGroup=control")
                .anyMatch(edge -> "APPLIES_POLICY".equals(edge.path("relationType").asText())
                        && "control".equals(edge.path("relationGroup").asText()));
        assertThat(domainGraph.path("edges"))
                .as("DOMAIN 图必须包含 canonical entity -> canonical entity 的 SUPPORTED_BY 边")
                .anyMatch(edge -> "SUPPORTED_BY".equals(edge.path("relationType").asText()));
        assertThat(domainGraph.path("edges"))
                .as("DOMAIN 图中的 SUPPORTED_BY 边必须显式返回 relationGroup=evidence")
                .anyMatch(edge -> "SUPPORTED_BY".equals(edge.path("relationType").asText())
                        && "evidence".equals(edge.path("relationGroup").asText()));
    }

    private JsonNode importPayrollSample(String token) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("rawText", PAYROLL_SAMPLE);
        body.put("sourceType", "PASTE_MD");
        body.put("sourceName", "mvp-payroll-sample.md");
        body.put("preprocessMode", "RULE_ONLY");
        body.put("autoCreateDrafts", false);
        MvcResult result = mockMvc.perform(post("/api/import/preprocess")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importBatchId").isString())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode fetchScene(String token, long sceneId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/scenes/{id}", sceneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode fetchCandidateGraph(String token, String taskId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/import/tasks/{taskId}/candidate-graph", taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private long createDomain(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/domains")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domainCode": "PAYROLL_MVP_FLOW",
                                  "domainName": "代发样板域",
                                  "domainOverview": "用于 MVP 闭环验证的代发明细样板域",
                                  "operator": "support"
                                }
                                """))
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

    private String findNodeIdByType(JsonNode nodes, String... objectTypes) {
        if (!nodes.isArray()) {
            return "";
        }
        for (String objectType : objectTypes) {
            for (JsonNode node : nodes) {
                if (objectType.equalsIgnoreCase(node.path("objectType").asText())) {
                    return node.path("id").asText("");
                }
            }
        }
        return "";
    }

    private String firstNodeCode(JsonNode nodes, String nodeType) {
        if (!nodes.isArray()) {
            throw new IllegalStateException("candidate graph nodes missing");
        }
        for (JsonNode node : nodes) {
            if (nodeType.equals(node.path("nodeType").asText())) {
                return node.path("nodeCode").asText("");
            }
        }
        throw new IllegalStateException("node not found: " + nodeType);
    }

    private String firstSceneCandidateCode(JsonNode nodes) {
        if (!nodes.isArray()) {
            throw new IllegalStateException("candidate graph nodes missing");
        }
        for (JsonNode node : nodes) {
            if ("CANDIDATE_SCENE".equals(node.path("nodeType").asText())) {
                return node.path("sceneCandidateCode").asText("");
            }
        }
        throw new IllegalStateException("candidate scene not found");
    }

    private void ensureGovernanceAssets(long sceneId) {
        OffsetDateTime now = OffsetDateTime.now();
        PlanPO primaryPlan = planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("plan missing for scene " + sceneId));

        if (dictionaryMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty()) {
            DictionaryPO dictionary = new DictionaryPO();
            dictionary.setSceneId(sceneId);
            dictionary.setPlanId(primaryPlan.getId());
            dictionary.setDictCode("DICT-" + sceneId + "-MAIN");
            dictionary.setDictName("代发协议状态字典");
            dictionary.setDictCategory("ENUM");
            dictionary.setDictVersion("v1");
            dictionary.setReleaseStatus("PUBLISHED");
            dictionary.setEntriesJson("[{\"code\":\"DEFAULT\",\"name\":\"默认值\"}]");
            dictionary.setReferencedByJson("[\"scene:" + sceneId + "\"]");
            stamp(dictionary, now);
            dictionaryMapper.save(dictionary);
        }

        if (identifierLineageMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty()) {
            IdentifierLineagePO lineage = new IdentifierLineagePO();
            lineage.setSceneId(sceneId);
            lineage.setPlanId(primaryPlan.getId());
            lineage.setLineageCode("LIN-" + sceneId + "-MAIN");
            lineage.setLineageName("代发协议主标识链");
            lineage.setIdentifierType("PROTOCOL_NBR");
            lineage.setSourceIdentifierType("MCH_AGR_NBR");
            lineage.setTargetIdentifierType("PROTOCOL_NBR");
            lineage.setMappingRulesJson("[{\"rule\":\"direct-pass-through\"}]");
            lineage.setEvidenceRefsJson("[\"scene:" + sceneId + "\"]");
            lineage.setConfirmationStatus("CONFIRMED");
            stamp(lineage, now);
            identifierLineageMapper.save(lineage);
        }

        if (timeSemanticSelectorMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty()) {
            TimeSemanticSelectorPO selector = new TimeSemanticSelectorPO();
            selector.setSceneId(sceneId);
            selector.setPlanId(primaryPlan.getId());
            selector.setSelectorCode("TIME-" + sceneId + "-MAIN");
            selector.setSelectorName("代发交易日期时间语义");
            selector.setDefaultSemantic("TRX_DT");
            selector.setCandidateSemanticsJson("[\"TRX_DT\",\"交易日期\"]");
            selector.setClarificationTermsJson("[\"交易日\",\"时间范围\"]");
            selector.setPriorityRulesJson("[{\"priority\":1,\"semantic\":\"TRX_DT\"}]");
            selector.setMustClarifyFlag(false);
            stamp(selector, now);
            timeSemanticSelectorMapper.save(selector);
        }
    }

    private void ensureCanonicalRelationAssets(long sceneId) {
        OffsetDateTime now = OffsetDateTime.now();
        if (outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty()) {
            OutputContractPO contract = new OutputContractPO();
            contract.setSceneId(sceneId);
            contract.setContractCode("OUT-" + sceneId + "-MAIN");
            contract.setContractName("代发明细输出契约");
            contract.setContractSemanticKey("payroll.detail.list");
            contract.setSummaryText("代发明细统一输出");
            contract.setFieldsJson("[\"PROTOCOL_NBR\",\"TRX_DT\",\"TRX_AMT\",\"EAC_NBR\"]");
            contract.setMaskingRulesJson("[]");
            contract.setUsageConstraints("仅用于工单核验与样板回放");
            contract.setTimeCaliberNote("TRX_DT");
            stamp(contract, now);
            outputContractMapper.save(contract);
        }
        if (policyMapper.findByFilter(null, sceneId, null).isEmpty()) {
            PolicyPO policy = new PolicyPO();
            policy.setPolicyCode("PLC-" + sceneId + "-MAIN");
            policy.setPolicyName("代发明细脱敏策略");
            policy.setPolicySemanticKey("payroll.detail.masking");
            policy.setScopeType("SCENE");
            policy.setScopeRefId(sceneId);
            policy.setEffectType("MASK");
            policy.setConditionText("收款账号默认脱敏显示");
            policy.setSourceType("RULE");
            policy.setSensitivityLevel("L2");
            policy.setMaskingRule("EAC_NBR -> tail4");
            stamp(policy, now);
            policyMapper.save(policy);
        }
        boolean hasManagedEvidence = evidenceFragmentMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream()
                .anyMatch(item -> ("EVD-" + sceneId + "-MAIN").equals(item.getEvidenceCode()));
        if (!hasManagedEvidence) {
            EvidenceFragmentPO evidence = new EvidenceFragmentPO();
            evidence.setSceneId(sceneId);
            evidence.setEvidenceCode("EVD-" + sceneId + "-MAIN");
            evidence.setTitle("代发明细样板 SQL 片段");
            evidence.setFragmentText("SELECT MCH_AGR_NBR AS PROTOCOL_NBR, TRX_DT, TRX_AMT, EAC_NBR FROM PDM_VHIS.T05_AGN_DTL");
            evidence.setSourceAnchor("raw-input#sql");
            evidence.setSourceType("RAW_INPUT_DOC");
            evidence.setSourceRef("mvp-payroll-sample.md");
            evidence.setOriginType("DOC");
            evidence.setOriginRef("mvp-payroll-sample.md");
            evidence.setOriginLocator("raw-input#sql");
            evidence.setConfidenceScore(1.0d);
            stamp(evidence, now);
            evidenceFragmentMapper.save(evidence);
        }
        canonicalEntityResolutionService.resolveScene(sceneId, "tester");
        assertThat(canonicalEntityMembershipMapper.findBySceneIdAndSceneAssetTypeOrderByUpdatedAtDesc(sceneId, "EVIDENCE"))
                .as("发布前必须已生成 active EVIDENCE canonical membership")
                .anyMatch(item -> item.isActiveFlag() && item.getCanonicalEntityId() != null);
        assertThat(canonicalEntityRelationMapper.findAll())
                .as("resolveScene 后必须已生成 SUPPORTED_BY canonical relation")
                .anyMatch(item -> "SUPPORTED_BY".equals(item.getRelationType()));
    }

    private void stamp(AbstractSnapshotGraphAuditablePO po, OffsetDateTime now) {
        po.setStatus("ACTIVE");
        po.setSnapshotId(null);
        po.setCreatedBy("tester");
        po.setCreatedAt(now);
        po.setUpdatedBy("tester");
        po.setUpdatedAt(now);
    }
}
