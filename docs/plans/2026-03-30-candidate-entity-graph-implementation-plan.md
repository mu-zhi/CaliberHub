# Candidate Entity Graph Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add task-scoped `Candidate Entity Graph（候选实体图谱）` to the knowledge-production import flow so preprocess builds the adapted ontology, users can review graph nodes and edges, and accepted candidate results feed modeling-ready scene payloads without leaking into the published runtime graph.

**Architecture:** Keep the current `Import Task（导入任务）` + `Source Material（来源材料）` + `Scene Candidate（候选场景）` + `Evidence Candidate（候选证据）` chain as the front door. Add a new isolated candidate-graph storage layer keyed by `task_id（任务标识）` + `material_id（材料标识）`, expose a read/review API under `/api/import/tasks/{taskId}/candidate-graph`, and patch accepted identifier, field, time-semantic, source-table, source-column, and join-relation hints back into candidate scene payload JSON so the existing `confirmCandidate` and scene-edit flow can continue to work.

**Tech Stack:** Spring Boot, Spring Data JPA, Flyway, MockMvc, React, Vite, Vitest, D3

---

## File Map

- `backend/src/main/resources/db/migration/V15__add_candidate_entity_graph.sql`: candidate graph node / edge / review-event tables, unique keys, and task-level indexes.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/ImportCandidateGraphNodePO.java`: persisted candidate graph nodes.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/ImportCandidateGraphEdgePO.java`: persisted candidate graph edges.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/ImportCandidateReviewEventPO.java`: persisted review actions for audit and replay.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/ImportCandidateGraphNodeMapper.java`: node reads and writes by `task_id`, `graph_id`, and `node_code`.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/ImportCandidateGraphEdgeMapper.java`: edge reads and writes by `task_id`, `graph_id`, and `edge_code`.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/ImportCandidateReviewEventMapper.java`: review-event writes and task-level history reads.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportCandidateGraphDTO.java`: task-scoped graph read model returned to frontend.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportCandidateGraphNodeDTO.java`: node payload for frontend graph panel.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportCandidateGraphEdgeDTO.java`: edge payload for frontend graph panel.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportCandidateGraphSummaryDTO.java`: candidate graph counts and pending-review summary.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/request/ReviewImportCandidateGraphCmd.java`: generic review command supporting `ACCEPT / REJECT / MERGE / SPLIT`.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/support/ImportSceneNormalizationSupport.java`: canonicalize same-business candidate scenes so multiple retrieval plans hang from one shared scene candidate before persistence and graph build.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCandidateGraphBuildService.java`: adapts preprocess output into the 10-node candidate ontology.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCandidateGraphCommandAppService.java`: review actions, audit events, and candidate-payload patching.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/ImportCandidateGraphQueryAppService.java`: reads task-scoped candidate graph DTOs.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportTaskCommandAppService.java`: rebuild candidate graph when preprocess finishes and consume patched candidate payload on confirm.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ImportController.java`: `/candidate-graph` read/review endpoints.
- `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/support/ImportSceneNormalizationSupportTest.java`: focused red/green coverage for “same scene, multiple plans” canonicalization.
- `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportTaskApiIntegrationTest.java`: import-task API red/green coverage for candidate graph build and review.
- `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MvpKnowledgeGraphFlowIntegrationTest.java`: full import -> review -> confirm -> publish flow coverage.
- `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCandidateGraphBuildServiceTest.java`: focused builder unit tests for the adapted ontology.
- `frontend/src/api/contracts.ts`: templated API paths for candidate graph read/review endpoints.
- `frontend/src/types/openapi.d.ts`: regenerated frontend OpenAPI types after backend endpoints exist.
- `frontend/src/pages/knowledge-import-utils.js`: frontend candidate-graph normalization and summary helpers.
- `frontend/src/pages/knowledge-import-utils.test.js`: unit coverage for candidate-graph normalization helpers.
- `frontend/src/components/knowledge/CandidateEntityGraphPanel.jsx`: candidate-graph panel that adapts import graph data to the existing D3 graph view.
- `frontend/src/pages/KnowledgePage.jsx`: load candidate graph, render review panel in Step 2, and refresh forms after review actions.
- `frontend/src/pages/KnowledgePage.render.test.jsx`: SSR smoke test for the new candidate-graph section.
- `docs/testing/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation-test-report.md`: test-report skeleton and final verification record for this feature.
- `docs/engineering/current-delivery-status.md`: plan reference and next action after the plan lands.

### Task 1: Add the red tests and the parsing-stage test-report skeleton

**Files:**

- Create: `docs/testing/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation-test-report.md`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportTaskApiIntegrationTest.java`

- [ ] **Step 1: Create the test-report skeleton before touching production code**

Create `docs/testing/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation-test-report.md`:

```md
# 02-解析抽取与证据确认 测试报告

## 一、关联信息

- **对应特性文档：** `docs/architecture/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation.md`
- **对应实施计划：** `docs/plans/2026-03-30-candidate-entity-graph-implementation-plan.md`
- **测试环境：** 本地 MySQL 控制库 / Spring Boot 后端 / React 前端工作台
- **最近测试时间：** 2026-03-30

## 二、验收范围与边界

### 2.1 覆盖项
- [ ] 任务级隔离 `Candidate Entity Graph（候选实体图谱）` 落库。
- [ ] 候选节点 / 边查询接口。
- [ ] `ACCEPT / REJECT / MERGE / SPLIT` 人工确认动作。
- [ ] 已接受候选结果回写候选场景载荷并进入建模表单。

### 2.2 不覆盖项
- 已发布主图运行时消费。
- 跨任务候选图合并。
- 图数据库双写与投影性能压测。

## 三、测试案例与执行记录

### 3.1 TC-01：候选实体图谱构建
- **输入：** 导入代发/薪资域样板材料。
- **预期输出：** 返回任务级候选图谱，节点类型覆盖 `CANDIDATE_SCENE / CANDIDATE_PLAN / IDENTIFIER / FIELD_CONCEPT / TIME_SEMANTIC / SOURCE_TABLE / SOURCE_COLUMN / JOIN_RELATION / CANDIDATE_EVIDENCE_FRAGMENT`。
- **实际结果：** 待执行。

### 3.2 TC-02：高风险节点人工确认回写
- **输入：** 接受一个 `TIME_SEMANTIC` 节点和一个 `SOURCE_TABLE` 节点。
- **预期输出：** 任务预处理结果与候选场景载荷被回写，进入建模页面时可见已接受提示。
- **实际结果：** 待执行。

### 3.3 TC-03：导入 -> 复核 -> 确认 -> 发布闭环
- **输入：** 无草稿直建模式导入样板材料，先复核候选图，再确认候选场景并发布。
- **预期输出：** 形成正式 `Scene（业务场景）`，且确认后的时间语义和来源线索已进入正式载荷。
- **实际结果：** 待执行。
```

- [ ] **Step 2: Add the failing integration tests for graph build and readback**

Extend `ImportTaskApiIntegrationTest.java`:

```java
@Test
void shouldBuildTaskScopedCandidateEntityGraph() throws Exception {
    String token = loginAndGetToken("support", "support123");
    MvcResult preprocessResult = mockMvc.perform(post("/api/import/preprocess")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "rawText":"### 场景标题：按协议号查询代发明细\\n- 场景描述：用于核对代发明细结果。\\n- 结果字段：代发批次号、交易金额、交易日期\\n- SQL 语句\\nSELECT AGN_BCH_SEQ, TRX_AMT, TRX_DT FROM PDM_VHIS.T05_AGN_DTL WHERE MCH_AGR_NBR='${AGR_ID}';",
                              "sourceType":"PASTE_MD",
                              "sourceName":"candidate-graph-case.md",
                              "preprocessMode":"RULE_ONLY",
                              "autoCreateDrafts":false
                            }
                            """))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode preprocessJson = objectMapper.readTree(preprocessResult.getResponse().getContentAsString());
    String taskId = preprocessJson.path("importBatchId").asText();

    MvcResult graphResult = mockMvc.perform(get("/api/import/tasks/{taskId}/candidate-graph", taskId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode graph = objectMapper.readTree(graphResult.getResponse().getContentAsString());
    assertThat(nodeTypeCount(graph.path("nodes"), "CANDIDATE_SCENE")).isGreaterThan(0);
    assertThat(nodeTypeCount(graph.path("nodes"), "CANDIDATE_PLAN")).isGreaterThan(0);
    assertThat(nodeTypeCount(graph.path("nodes"), "IDENTIFIER")).isGreaterThan(0);
    assertThat(nodeTypeCount(graph.path("nodes"), "TIME_SEMANTIC")).isGreaterThan(0);
    assertThat(nodeTypeCount(graph.path("nodes"), "SOURCE_TABLE")).isGreaterThan(0);
    assertThat(nodeTypeCount(graph.path("nodes"), "CANDIDATE_EVIDENCE_FRAGMENT")).isGreaterThan(0);
    assertThat(edgeTypeCount(graph.path("edges"), "SCENE_HAS_PLAN")).isGreaterThan(0);
    assertThat(edgeTypeCount(graph.path("edges"), "NODE_SUPPORTED_BY_EVIDENCE")).isGreaterThan(0);
}

private int nodeTypeCount(JsonNode nodes, String nodeType) {
    int count = 0;
    for (JsonNode node : nodes) {
        if (nodeType.equals(node.path("nodeType").asText())) {
            count += 1;
        }
    }
    return count;
}

private int edgeTypeCount(JsonNode edges, String edgeType) {
    int count = 0;
    for (JsonNode edge : edges) {
        if (edgeType.equals(edge.path("edgeType").asText())) {
            count += 1;
        }
    }
    return count;
}
```

- [ ] **Step 3: Run the targeted backend integration test to verify red**

Run:

- `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest test`

Expected:

- FAIL because `/api/import/tasks/{taskId}/candidate-graph` does not exist yet, and no candidate graph tables are available.

### Task 1.5: Canonicalize “same scene, multiple plans” before persistence and graph build

**Files:**

- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/support/ImportSceneNormalizationSupportTest.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/support/ImportSceneNormalizationSupport.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCandidateGraphBuildService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/LlmPromptDefaults.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCandidateGraphBuildServiceTest.java`

- [ ] **Step 1: Write the failing normalization unit test**

Create `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/support/ImportSceneNormalizationSupportTest.java`:

```java
class ImportSceneNormalizationSupportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMergeSameBusinessSceneIntoOneSceneWithMultiplePlans() throws Exception {
        JsonNode first = objectMapper.readTree("""
                {
                  "scene_title":"方法1：按协议号查询代发明细",
                  "scene_description":"用于查询代发明细",
                  "applicability":"当前主表",
                  "boundaries":"仅查询明细",
                  "inputs":{"params":[{"name":"协议号"}]},
                  "outputs":{"fields":[{"name":"交易金额"},{"name":"交易日期"}]},
                  "sql_variants":[{"variant_name":"当前表方案","source_tables":["PDM.T05_AGN_DTL"],"sql_text":"SELECT TRX_AMT FROM PDM.T05_AGN_DTL"}]
                }
                """);
        JsonNode second = objectMapper.readTree("""
                {
                  "scene_title":"方法2：按协议号查询代发明细",
                  "scene_description":"用于查询代发明细",
                  "applicability":"历史表补录",
                  "boundaries":"仅查询明细",
                  "inputs":{"params":[{"name":"协议号"}]},
                  "outputs":{"fields":[{"name":"交易金额"},{"name":"交易日期"}]},
                  "sql_variants":[{"variant_name":"历史表方案","source_tables":["PDM_HIS.T05_AGN_DTL"],"sql_text":"SELECT TRX_AMT FROM PDM_HIS.T05_AGN_DTL"}]
                }
                """);

        List<JsonNode> normalized = ImportSceneNormalizationSupport.normalize(objectMapper, List.of(first, second));

        assertThat(normalized).hasSize(1);
        assertThat(normalized.get(0).path("sql_variants")).hasSize(2);
        assertThat(normalized.get(0).path("scene_title").asText()).isEqualTo("按协议号查询代发明细");
    }
}
```

- [ ] **Step 2: Run the targeted unit test to verify red**

Run:

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -q -Dtest=ImportSceneNormalizationSupportTest test`

Expected:

- FAIL because `ImportSceneNormalizationSupport` does not exist and preprocess results are not canonicalized.

- [ ] **Step 3: Implement normalization support and wire it into preprocess result normalization**

Add a focused helper that:

- strips `方法1/方法2/Step 1/Step 2/方案1/方案2` prefixes from candidate titles to derive a canonical scene key;
- keeps one shared `Candidate Scene（候选场景）` for the same business problem;
- merges `sql_variants` into the shared scene;
- preserves mild `applicability（适用范围）` / source-table differences on plan variants instead of splitting scenes;
- only keeps separate scenes when primary object, `applicability`, `inapplicable_scope`, or standard output commitments materially diverge.

- [ ] **Step 4: Add a builder regression test for shared-scene multi-plan graphs**

Extend `ImportCandidateGraphBuildServiceTest.java` with a case where two same-scene candidates normalize to one `CANDIDATE_SCENE` and two `CANDIDATE_PLAN` nodes connected by two `SCENE_HAS_PLAN` edges.

- [ ] **Step 5: Run the focused backend tests to verify green**

Run:

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -q -Dtest=ImportSceneNormalizationSupportTest,ImportCandidateGraphBuildServiceTest test`

Expected:

- PASS with one canonical candidate scene and multiple candidate plans for the same business problem.

### Task 2: Persist the candidate entity graph and expose the read API

**Files:**

- Create: `backend/src/main/resources/db/migration/V15__add_candidate_entity_graph.sql`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/ImportCandidateGraphNodePO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/ImportCandidateGraphEdgePO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/ImportCandidateReviewEventPO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/ImportCandidateGraphNodeMapper.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/ImportCandidateGraphEdgeMapper.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/ImportCandidateReviewEventMapper.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportCandidateGraphDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportCandidateGraphNodeDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportCandidateGraphEdgeDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportCandidateGraphSummaryDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCandidateGraphBuildService.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/ImportCandidateGraphQueryAppService.java`
- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCandidateGraphBuildServiceTest.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportTaskCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ImportController.java`

- [ ] **Step 1: Add the failing builder unit test**

Create `ImportCandidateGraphBuildServiceTest.java`:

```java
@ExtendWith(MockitoExtension.class)
class ImportCandidateGraphBuildServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldBuildAdaptedOntologyFromPreprocessResult() throws Exception {
        ImportCandidateGraphBuildService service = new ImportCandidateGraphBuildService(objectMapper);
        JsonNode scene = objectMapper.readTree("""
                {
                  "scene_title":"按协议号查询代发明细",
                  "scene_description":"用于核对代发明细结果",
                  "source_evidence_lines":[1,2,3],
                  "inputs":{"params":[{"name":"协议号","type":"STRING","required":true,"identifiers":["协议号"]}]},
                  "outputs":{"fields":[{"name":"交易金额"},{"name":"交易日期"}]},
                  "sql_variants":[
                    {
                      "variant_name":"默认方案",
                      "default_time_semantic":"TRX_DT",
                      "source_tables":["PDM_VHIS.T05_AGN_DTL"],
                      "sql_text":"SELECT AGN_BCH_SEQ, TRX_AMT, TRX_DT FROM PDM_VHIS.T05_AGN_DTL WHERE MCH_AGR_NBR='${AGR_ID}'"
                    }
                  ]
                }
                """);
        PreprocessResultDTO result = new PreprocessResultDTO(
                "{}",
                "rule_generated",
                objectMapper.readTree("{\"domain_guess\":\"PAYROLL\"}"),
                List.of(scene),
                objectMapper.readTree("{\"confidence\":0.9}"),
                List.of(),
                0.9,
                "HIGH",
                false,
                120L,
                List.of(),
                List.of(),
                "task-1",
                "material-1"
        );

        ImportCandidateGraphDTO graph = service.build("task-1", "material-1", result);

        assertThat(graph.summary().nodeTotal()).isGreaterThanOrEqualTo(6);
        assertThat(graph.nodes().stream().map(ImportCandidateGraphNodeDTO::nodeType))
                .contains("CANDIDATE_SCENE", "CANDIDATE_PLAN", "IDENTIFIER", "TIME_SEMANTIC", "SOURCE_TABLE");
    }
}
```

- [ ] **Step 2: Run the focused builder test to verify red**

Run:

- `cd backend && mvn -q -Dtest=ImportCandidateGraphBuildServiceTest test`

Expected:

- FAIL because the builder service and graph DTOs do not exist yet.

- [ ] **Step 3: Add the candidate-graph schema, DTOs, and builder**

Create `V15__add_candidate_entity_graph.sql`:

```sql
CREATE TABLE caliber_import_candidate_graph_node (
    node_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    graph_id VARCHAR(128) NOT NULL,
    node_code VARCHAR(96) NOT NULL,
    scene_candidate_code VARCHAR(64),
    node_type VARCHAR(48) NOT NULL,
    node_label VARCHAR(255) NOT NULL,
    canonical_node_code VARCHAR(96),
    review_status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    confidence_score DOUBLE,
    payload_json LONGTEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_candidate_graph_node_code
    ON caliber_import_candidate_graph_node (node_code);

CREATE INDEX idx_candidate_graph_node_task_graph
    ON caliber_import_candidate_graph_node (task_id, graph_id, review_status, updated_at);

CREATE TABLE caliber_import_candidate_graph_edge (
    edge_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    graph_id VARCHAR(128) NOT NULL,
    edge_code VARCHAR(96) NOT NULL,
    scene_candidate_code VARCHAR(64),
    edge_type VARCHAR(48) NOT NULL,
    source_node_code VARCHAR(96) NOT NULL,
    target_node_code VARCHAR(96) NOT NULL,
    edge_label VARCHAR(255),
    review_status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    confidence_score DOUBLE,
    payload_json LONGTEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_candidate_graph_edge_code
    ON caliber_import_candidate_graph_edge (edge_code);

CREATE INDEX idx_candidate_graph_edge_task_graph
    ON caliber_import_candidate_graph_edge (task_id, graph_id, review_status, updated_at);

CREATE TABLE caliber_import_candidate_review_event (
    event_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    graph_id VARCHAR(128) NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_code VARCHAR(96) NOT NULL,
    action_type VARCHAR(16) NOT NULL,
    before_status VARCHAR(32),
    after_status VARCHAR(32),
    operator VARCHAR(64),
    reason_text VARCHAR(255),
    payload_json LONGTEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_candidate_review_event_task_graph
    ON caliber_import_candidate_review_event (task_id, graph_id, created_at);
```

Create `ImportCandidateGraphBuildService.java`:

```java
@Service
public class ImportCandidateGraphBuildService {

    private final ObjectMapper objectMapper;

    public ImportCandidateGraphBuildService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ImportCandidateGraphDTO build(String taskId, String materialId, PreprocessResultDTO result) {
        String graphId = taskId + ":" + materialId;
        List<ImportCandidateGraphNodeDTO> nodes = new ArrayList<>();
        List<ImportCandidateGraphEdgeDTO> edges = new ArrayList<>();

        for (int sceneIndex = 0; sceneIndex < result.scenes().size(); sceneIndex++) {
            JsonNode scene = result.scenes().get(sceneIndex);
            String sceneCode = code(taskId, "SC", sceneIndex + 1);
            nodes.add(node(graphId, sceneCode, null, "CANDIDATE_SCENE", text(scene.path("scene_title")), "PENDING_CONFIRMATION", "LOW", score(scene), payload(sceneIndex, null, Map.of())));

            JsonNode variants = arrayNode(scene.path("sql_variants"));
            for (int variantIndex = 0; variantIndex < variants.size(); variantIndex++) {
                JsonNode variant = variants.get(variantIndex);
                String planCode = code(taskId, "PLN", sceneIndex + 1, variantIndex + 1);
                nodes.add(node(graphId, planCode, sceneCode, "CANDIDATE_PLAN", text(variant.path("variant_name"), "默认方案"), "PENDING_CONFIRMATION", "LOW", score(scene), payload(sceneIndex, variantIndex, Map.of())));
                edges.add(edge(graphId, code(taskId, "EDGE", sceneIndex + 1, variantIndex + 1), sceneCode, "SCENE_HAS_PLAN", sceneCode, planCode, "候选方案", "PENDING_CONFIRMATION", "LOW", score(scene), payload(sceneIndex, variantIndex, Map.of())));

                for (String sourceTable : stringList(variant.path("source_tables"))) {
                    String tableCode = code(taskId, "SRC", sceneIndex + 1, variantIndex + 1, normalizeCode(sourceTable));
                    nodes.add(node(graphId, tableCode, sceneCode, "SOURCE_TABLE", sourceTable, "PENDING_CONFIRMATION", "HIGH", 0.82d, payload(sceneIndex, variantIndex, Map.of("sourceTable", sourceTable))));
                    edges.add(edge(graphId, code(taskId, "EDGE-SRC", sceneIndex + 1, variantIndex + 1, normalizeCode(sourceTable)), sceneCode, "PLAN_USES_SOURCE_TABLE", planCode, tableCode, "来源表", "PENDING_CONFIRMATION", "HIGH", 0.82d, payload(sceneIndex, variantIndex, Map.of())));
                }

                String timeSemantic = text(variant.path("default_time_semantic"));
                if (!timeSemantic.isBlank()) {
                    String timeCode = code(taskId, "TIME", sceneIndex + 1, variantIndex + 1);
                    nodes.add(node(graphId, timeCode, sceneCode, "TIME_SEMANTIC", timeSemantic, "PENDING_CONFIRMATION", "HIGH", 0.78d, payload(sceneIndex, variantIndex, Map.of("defaultTimeSemantic", timeSemantic))));
                    edges.add(edge(graphId, code(taskId, "EDGE-TIME", sceneIndex + 1, variantIndex + 1), sceneCode, "PLAN_USES_TIME_SEMANTIC", planCode, timeCode, "时间语义", "PENDING_CONFIRMATION", "HIGH", 0.78d, payload(sceneIndex, variantIndex, Map.of())));
                }
            }

            for (JsonNode input : arrayNode(scene.path("inputs").path("params"))) {
                String label = text(input.path("name"), text(input.path("name_zh")));
                if (looksLikeIdentifier(label)) {
                    String nodeCode = code(taskId, "ID", sceneIndex + 1, normalizeCode(label));
                    nodes.add(node(graphId, nodeCode, sceneCode, "IDENTIFIER", label, "PENDING_CONFIRMATION", "HIGH", 0.86d, payload(sceneIndex, null, Map.of("slotName", label))));
                    edges.add(edge(graphId, code(taskId, "EDGE-ID", sceneIndex + 1, normalizeCode(label)), sceneCode, "SCENE_USES_IDENTIFIER", sceneCode, nodeCode, "标识对象", "PENDING_CONFIRMATION", "HIGH", 0.86d, payload(sceneIndex, null, Map.of())));
                }
            }
        }

        int pendingReviewTotal = (int) nodes.stream().filter(item -> "PENDING_CONFIRMATION".equals(item.reviewStatus())).count();
        return new ImportCandidateGraphDTO(
                taskId,
                materialId,
                graphId,
                new ImportCandidateGraphSummaryDTO(nodes.size(), edges.size(), pendingReviewTotal),
                dedupeNodes(nodes),
                dedupeEdges(edges)
        );
    }

    private ImportCandidateGraphNodeDTO node(String graphId, String nodeCode, String sceneCandidateCode,
                                             String nodeType, String label, String reviewStatus,
                                             String riskLevel, Double confidenceScore, JsonNode payload) {
        return new ImportCandidateGraphNodeDTO(nodeCode, sceneCandidateCode, nodeType, label, reviewStatus, riskLevel, confidenceScore, payload, "");
    }

    private ImportCandidateGraphEdgeDTO edge(String graphId, String edgeCode, String sceneCandidateCode,
                                             String edgeType, String sourceNodeCode, String targetNodeCode,
                                             String label, String reviewStatus, String riskLevel,
                                             Double confidenceScore, JsonNode payload) {
        return new ImportCandidateGraphEdgeDTO(edgeCode, sceneCandidateCode, edgeType, sourceNodeCode, targetNodeCode, label, reviewStatus, riskLevel, confidenceScore, payload);
    }

    private JsonNode payload(Integer sceneIndex, Integer variantIndex, Map<String, Object> extras) {
        ObjectNode node = objectMapper.createObjectNode();
        if (sceneIndex != null) {
            node.put("sceneIndex", sceneIndex);
        }
        if (variantIndex != null) {
            node.put("variantIndex", variantIndex);
        }
        extras.forEach((key, value) -> node.set(key, objectMapper.valueToTree(value)));
        return node;
    }

    private ArrayNode arrayNode(JsonNode node) {
        return node != null && node.isArray() ? (ArrayNode) node : objectMapper.createArrayNode();
    }

    private String text(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? "" : node.asText("");
    }

    private String text(JsonNode node, String fallback) {
        String current = text(node);
        return current.isBlank() ? fallback : current;
    }

    private Double score(JsonNode scene) {
        return scene.path("quality").path("confidence").asDouble(0.8d);
    }

    private String code(String taskId, Object... parts) {
        return Stream.concat(Stream.of(taskId.replace("-", "").substring(0, 8)), Arrays.stream(parts).map(String::valueOf))
                .collect(Collectors.joining("-"));
    }

    private String normalizeCode(String value) {
        return value == null ? "UNK" : value.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "").toUpperCase(Locale.ROOT);
    }

    private List<String> stringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                String text = item.asText("").trim();
                if (!text.isBlank()) {
                    result.add(text);
                }
            });
        }
        return result;
    }

    private boolean looksLikeIdentifier(String label) {
        String normalized = label == null ? "" : label.toUpperCase(Locale.ROOT);
        return normalized.contains("协议号")
                || normalized.contains("户口号")
                || normalized.contains("客户号")
                || normalized.contains("ID");
    }

    private List<ImportCandidateGraphNodeDTO> dedupeNodes(List<ImportCandidateGraphNodeDTO> nodes) {
        return new ArrayList<>(nodes.stream().collect(Collectors.toMap(
                ImportCandidateGraphNodeDTO::nodeCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        )).values());
    }

    private List<ImportCandidateGraphEdgeDTO> dedupeEdges(List<ImportCandidateGraphEdgeDTO> edges) {
        return new ArrayList<>(edges.stream().collect(Collectors.toMap(
                ImportCandidateGraphEdgeDTO::edgeCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        )).values());
    }
}
```

- [ ] **Step 4: Rebuild the graph when preprocess finishes and add the read endpoint**

In `ImportTaskCommandAppService.java`, rebuild candidate graph inside `markQualityReviewReady`:

```java
private final ImportCandidateGraphBuildService importCandidateGraphBuildService;
private final ImportCandidateGraphNodeMapper importCandidateGraphNodeMapper;
private final ImportCandidateGraphEdgeMapper importCandidateGraphEdgeMapper;

@Transactional
public ImportTaskDTO markQualityReviewReady(String taskId, PreprocessResultDTO result) {
    ImportTaskPO po = requireTask(taskId);
    persistReviewCandidates(po, result);
    rebuildCandidateGraph(po, result);
    po.setStatus(STATUS_QUALITY_REVIEWING);
    po.setCurrentStep(Math.max(2, defaultStep(po.getCurrentStep())));
    po.setPreprocessResultJson(writeResultJson(result));
    po.setQualityConfirmed(false);
    po.setCompareConfirmed(false);
    po.setErrorMessage(null);
    po.setUpdatedAt(OffsetDateTime.now());
    return toDTO(importTaskMapper.save(po));
}

private void rebuildCandidateGraph(ImportTaskPO task, PreprocessResultDTO result) {
    importCandidateGraphNodeMapper.deleteByTaskId(task.getTaskId());
    importCandidateGraphEdgeMapper.deleteByTaskId(task.getTaskId());
    ImportCandidateGraphDTO graph = importCandidateGraphBuildService.build(task.getTaskId(), task.getMaterialId(), result);
    graph.nodes().forEach(node -> importCandidateGraphNodeMapper.save(toNodePO(graph.graphId(), task, node)));
    graph.edges().forEach(edge -> importCandidateGraphEdgeMapper.save(toEdgePO(graph.graphId(), task, edge)));
}
```

In `ImportController.java`, add the read endpoint:

```java
@GetMapping("/tasks/{taskId}/candidate-graph")
public ResponseEntity<ImportCandidateGraphDTO> getCandidateGraph(@PathVariable String taskId) {
    return ResponseEntity.ok(importCandidateGraphQueryAppService.getByTaskId(taskId));
}
```

- [ ] **Step 5: Re-run the focused builder and API tests**

Run:

- `cd backend && mvn -q -Dtest=ImportCandidateGraphBuildServiceTest,ImportTaskApiIntegrationTest test`

Expected:

- PASS for the builder unit test and the candidate-graph readback integration test.

### Task 3: Implement review actions and patch accepted hints back into candidate scene payloads

**Files:**

- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/request/ReviewImportCandidateGraphCmd.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCandidateGraphCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ImportController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportTaskCommandAppService.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportTaskApiIntegrationTest.java`

- [ ] **Step 1: Add the failing review-and-patch integration test**

Extend `ImportTaskApiIntegrationTest.java`:

```java
@Test
void shouldPatchCandidateScenePayloadAfterAcceptingTimeSemanticNode() throws Exception {
    String token = loginAndGetToken("support", "support123");
    long domainId = createDomain(token);

    MvcResult preprocessResult = mockMvc.perform(post("/api/import/preprocess")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "rawText":"### 场景标题：按协议号查询代发明细\\n- 场景描述：用于核对代发明细结果。\\n- 结果字段：交易金额、交易日期\\n- SQL 语句\\nSELECT TRX_AMT, TRX_DT FROM PDM_VHIS.T05_AGN_DTL WHERE MCH_AGR_NBR='${AGR_ID}';",
                              "sourceType":"PASTE_MD",
                              "sourceName":"candidate-review-case.md",
                              "preprocessMode":"RULE_ONLY",
                              "autoCreateDrafts":false
                            }
                            """))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode preprocessJson = objectMapper.readTree(preprocessResult.getResponse().getContentAsString());
    String taskId = preprocessJson.path("importBatchId").asText();

    JsonNode graph = objectMapper.readTree(mockMvc.perform(get("/api/import/tasks/{taskId}/candidate-graph", taskId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
    String timeNodeCode = firstNodeCode(graph.path("nodes"), "TIME_SEMANTIC");
    String candidateCode = jdbcTemplate.queryForObject(
            "SELECT candidate_code FROM caliber_import_scene_candidate WHERE task_id = ? ORDER BY scene_index ASC LIMIT 1",
            String.class,
            taskId
    );

    mockMvc.perform(post("/api/import/tasks/{taskId}/candidate-graph/review", taskId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "targetType":"NODE",
                              "targetCode":"%s",
                              "action":"ACCEPT",
                              "reason":"交易日期作为默认时间语义",
                              "operator":"support"
                            }
                            """.formatted(timeNodeCode)))
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

    JsonNode scene = objectMapper.readTree(confirmResult.getResponse().getContentAsString());
    assertThat(scene.path("sqlVariantsJson").asText()).contains("default_time_semantic");
    assertThat(scene.path("sqlVariantsJson").asText()).contains("TRX_DT");
}

private String firstNodeCode(JsonNode nodes, String nodeType) {
    for (JsonNode node : nodes) {
        if (nodeType.equals(node.path("nodeType").asText())) {
            return node.path("nodeCode").asText();
        }
    }
    throw new IllegalStateException("node not found: " + nodeType);
}

private long createDomain(String token) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/domains")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "domainCode": "PAYROLL_IMPORT_TEST",
                              "domainName": "代发导入测试域",
                              "domainOverview": "用于候选实体图谱导入测试",
                              "operator": "support"
                            }
                            """))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
}
```

- [ ] **Step 2: Run the targeted review test to verify red**

Run:

- `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest test`

Expected:

- FAIL because `/api/import/tasks/{taskId}/candidate-graph/review` does not exist yet and accepted nodes are not patched back into candidate scene payloads.

- [ ] **Step 3: Implement the generic review command, audit event, and payload patcher**

Create `ReviewImportCandidateGraphCmd.java`:

```java
public record ReviewImportCandidateGraphCmd(
        String targetType,
        String targetCode,
        String action,
        String reason,
        String mergeIntoCode,
        List<String> splitLabels,
        String operator
) {
}
```

Create `ImportCandidateGraphCommandAppService.java`:

```java
@Service
public class ImportCandidateGraphCommandAppService {

    @Transactional
    public ImportCandidateGraphDTO review(String taskId, ReviewImportCandidateGraphCmd cmd) {
        String action = normalizeAction(cmd.action());
        if ("NODE".equalsIgnoreCase(cmd.targetType())) {
            ImportCandidateGraphNodePO node = requireNode(taskId, cmd.targetCode());
            String before = node.getReviewStatus();
            switch (action) {
                case "ACCEPT" -> node.setReviewStatus("ACCEPTED");
                case "REJECT" -> node.setReviewStatus("REJECTED");
                case "MERGE" -> {
                    requireNode(taskId, cmd.mergeIntoCode());
                    node.setReviewStatus("MERGED");
                    node.setCanonicalNodeCode(cmd.mergeIntoCode());
                }
                case "SPLIT" -> splitNode(taskId, node, cmd.splitLabels(), normalizeOperator(cmd.operator()));
                default -> throw new DomainValidationException("unsupported action: " + action);
            }
            node.setUpdatedAt(OffsetDateTime.now());
            nodeMapper.save(node);
            reviewEventMapper.save(reviewEvent(taskId, node.getMaterialId(), node.getGraphId(), "NODE", node.getNodeCode(), action, before, node.getReviewStatus(), cmd)));
        } else {
            ImportCandidateGraphEdgePO edge = requireEdge(taskId, cmd.targetCode());
            String before = edge.getReviewStatus();
            edge.setReviewStatus("ACCEPT".equals(action) ? "ACCEPTED" : "REJECT".equals(action) ? "REJECTED" : before);
            edge.setUpdatedAt(OffsetDateTime.now());
            edgeMapper.save(edge);
            reviewEventMapper.save(reviewEvent(taskId, edge.getMaterialId(), edge.getGraphId(), "EDGE", edge.getEdgeCode(), action, before, edge.getReviewStatus(), cmd)));
        }

        patchCandidatePayloads(taskId);
        return queryAppService.getByTaskId(taskId);
    }

    private void patchCandidatePayloads(String taskId) {
        ImportTaskPO task = importTaskMapper.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("import task not found: " + taskId));
        List<ImportSceneCandidatePO> sceneCandidates = importSceneCandidateMapper.findByTaskId(taskId);
        ObjectNode preprocessRoot = readObject(task.getPreprocessResultJson());

        for (ImportSceneCandidatePO sceneCandidate : sceneCandidates) {
            ObjectNode scenePayload = readObject(sceneCandidate.getCandidatePayloadJson());
            List<ImportCandidateGraphNodePO> acceptedNodes = nodeMapper.findByTaskIdAndSceneCandidateCodeAndReviewStatus(
                    taskId,
                    sceneCandidate.getCandidateCode(),
                    "ACCEPTED"
            );
            applyAcceptedNodes(scenePayload, acceptedNodes);
            sceneCandidate.setCandidatePayloadJson(write(scenePayload));
            importSceneCandidateMapper.save(sceneCandidate);
            replaceSceneInTaskPayload(preprocessRoot, sceneCandidate.getSceneIndex(), scenePayload);
        }

        task.setPreprocessResultJson(write(preprocessRoot));
        importTaskMapper.save(task);
    }

    private void applyAcceptedNodes(ObjectNode scenePayload, List<ImportCandidateGraphNodePO> acceptedNodes) {
        for (ImportCandidateGraphNodePO node : acceptedNodes) {
            ObjectNode payload = readObject(node.getPayloadJson());
            switch (node.getNodeType()) {
                case "IDENTIFIER" -> appendIdentifier(scenePayload, payload.path("slotName").asText(node.getNodeLabel()), node.getNodeLabel());
                case "FIELD_CONCEPT" -> appendOutputField(scenePayload, node.getNodeLabel());
                case "TIME_SEMANTIC" -> applyDefaultTimeSemantic(scenePayload, payload.path("defaultTimeSemantic").asText(node.getNodeLabel()));
                case "SOURCE_TABLE" -> appendSourceTable(scenePayload, payload.path("sourceTable").asText(node.getNodeLabel()));
                case "SOURCE_COLUMN" -> appendSourceColumn(scenePayload, payload.path("columnName").asText(node.getNodeLabel()));
                case "JOIN_RELATION" -> appendJoinRelationHint(scenePayload, node.getNodeLabel(), payload);
                default -> {
                    // no-op for scene/plan/evidence/business term
                }
            }
        }
    }
}
```

In `ImportController.java`, add the review endpoint:

```java
@PostMapping("/tasks/{taskId}/candidate-graph/review")
public ResponseEntity<ImportCandidateGraphDTO> reviewCandidateGraph(@PathVariable String taskId,
                                                                    @RequestBody @Valid ReviewImportCandidateGraphCmd cmd) {
    return ResponseEntity.ok(importCandidateGraphCommandAppService.review(taskId, withOperator(cmd)));
}
```

- [ ] **Step 4: Re-run the targeted review integration test**

Run:

- `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest test`

Expected:

- PASS, and the confirmed `Scene（业务场景）` payload includes accepted time-semantic hints.

### Task 4: Render and review the candidate graph from the import workbench

**Files:**

- Create: `frontend/src/components/knowledge/CandidateEntityGraphPanel.jsx`
- Modify: `frontend/src/api/contracts.ts`
- Modify: `frontend/src/pages/knowledge-import-utils.js`
- Modify: `frontend/src/pages/knowledge-import-utils.test.js`
- Modify: `frontend/src/pages/KnowledgePage.jsx`
- Modify: `frontend/src/pages/KnowledgePage.render.test.jsx`
- Modify: `frontend/src/types/openapi.d.ts`

- [ ] **Step 1: Add the failing frontend utility and render tests**

Extend `knowledge-import-utils.test.js`:

```js
import {
  buildStep1Summary,
  buildStep2Summary,
  buildStep3Summary,
  normalizeCandidateGraph,
  summarizeCandidateGraph,
  resolveAccordionStepState,
  toConfidenceLevelZh,
} from "./knowledge-import-utils";

it("normalizes candidate graph payload into graph view data", () => {
  const graph = normalizeCandidateGraph({
    graphId: "task-a:material-a",
    nodes: [
      { nodeCode: "SC-001", nodeType: "CANDIDATE_SCENE", label: "代发明细查询", reviewStatus: "PENDING_CONFIRMATION" },
      { nodeCode: "ID-001", nodeType: "IDENTIFIER", label: "协议号", reviewStatus: "ACCEPTED" },
    ],
    edges: [
      { edgeCode: "EDGE-001", edgeType: "SCENE_USES_IDENTIFIER", sourceNodeCode: "SC-001", targetNodeCode: "ID-001", label: "标识对象" },
    ],
  });

  expect(graph.nodes).toHaveLength(2);
  expect(graph.edges).toHaveLength(1);
  expect(graph.nodes[0].objectType).toBe("CANDIDATE_SCENE");
});

it("summarizes pending review counts for candidate graph", () => {
  expect(summarizeCandidateGraph({
    nodes: [
      { reviewStatus: "PENDING_CONFIRMATION" },
      { reviewStatus: "ACCEPTED" },
      { reviewStatus: "PENDING_CONFIRMATION" },
    ],
    edges: [{ reviewStatus: "REJECTED" }],
  })).toEqual({
    pendingNodes: 2,
    acceptedNodes: 1,
    pendingEdges: 0,
  });
});
```

Extend `KnowledgePage.render.test.jsx`:

```jsx
it("renders candidate graph review marker in import preset", () => {
  const html = renderKnowledgePage("import");
  expect(html).toContain("候选实体图谱");
  expect(html).toContain("接受");
  expect(html).toContain("驳回");
});
```

- [ ] **Step 2: Run the targeted frontend tests to verify red**

Run:

- `cd frontend && npm test -- src/pages/knowledge-import-utils.test.js src/pages/KnowledgePage.render.test.jsx`

Expected:

- FAIL because candidate-graph helpers and the graph panel do not exist yet.

- [ ] **Step 3: Add frontend API paths, graph normalization helpers, and the review panel**

In `frontend/src/api/contracts.ts`, add the new templated paths:

```ts
interface ContractPathParams {
  importTaskCandidateGraph: { taskId: PathParamValue };
  importTaskCandidateGraphReview: { taskId: PathParamValue };
}

const TEMPLATED_OPENAPI_PATHS = {
  importTaskCandidateGraph: "/api/import/tasks/{taskId}/candidate-graph",
  importTaskCandidateGraphReview: "/api/import/tasks/{taskId}/candidate-graph/review",
} as const;
```

In `knowledge-import-utils.js`, add the graph helpers:

```js
export function normalizeCandidateGraph(graph) {
  const nodes = Array.isArray(graph?.nodes) ? graph.nodes : [];
  const edges = Array.isArray(graph?.edges) ? graph.edges : [];
  return {
    rootRef: graph?.graphId || "",
    nodes: nodes.map((node) => ({
      id: node.nodeCode,
      objectType: node.nodeType,
      objectCode: node.nodeCode,
      objectName: node.label || node.nodeCode,
      status: node.reviewStatus || "",
      summaryText: node.summaryText || "",
      meta: {
        riskLevel: node.riskLevel || "",
        sceneCandidateCode: node.sceneCandidateCode || "",
      },
    })),
    edges: edges.map((edge) => ({
      id: edge.edgeCode,
      relationType: edge.edgeType,
      source: edge.sourceNodeCode,
      target: edge.targetNodeCode,
      label: edge.label || edge.edgeType,
      confidence: edge.confidenceScore || 0,
      meta: {
        reviewStatus: edge.reviewStatus || "",
      },
    })),
  };
}

export function summarizeCandidateGraph(graph) {
  const nodes = Array.isArray(graph?.nodes) ? graph.nodes : [];
  const edges = Array.isArray(graph?.edges) ? graph.edges : [];
  return {
    pendingNodes: nodes.filter((item) => item.reviewStatus === "PENDING_CONFIRMATION").length,
    acceptedNodes: nodes.filter((item) => item.reviewStatus === "ACCEPTED").length,
    pendingEdges: edges.filter((item) => item.reviewStatus === "PENDING_CONFIRMATION").length,
  };
}
```

Create `CandidateEntityGraphPanel.jsx`:

```jsx
import { LineageGraphView } from "../datamap/LineageGraphView";
import { UiButton, UiInput } from "../ui";
import { normalizeCandidateGraph, summarizeCandidateGraph } from "../../pages/knowledge-import-utils";

export function CandidateEntityGraphPanel({
  graph,
  selectedNodeId,
  selectedEdgeId,
  mergeTargetCode,
  splitLabelsText,
  onMergeTargetChange,
  onSplitLabelsChange,
  onNodeSelect,
  onEdgeSelect,
  onReview,
}) {
  const normalized = normalizeCandidateGraph(graph);
  const summary = summarizeCandidateGraph(graph);
  const selectedNode = (graph?.nodes || []).find((item) => item.nodeCode === selectedNodeId) || null;
  const selectedEdge = (graph?.edges || []).find((item) => item.edgeCode === selectedEdgeId) || null;

  return (
    <section className="knowledge-card">
      <div className="knowledge-card__header">
        <div>
          <p className="knowledge-card__eyebrow">Step 2 / 候选实体图谱</p>
          <h3>候选实体图谱</h3>
        </div>
        <p className="knowledge-card__meta">
          待确认节点 {summary.pendingNodes} 个，已接受节点 {summary.acceptedNodes} 个，待确认关系 {summary.pendingEdges} 条
        </p>
      </div>
      <div className="knowledge-graph-panel">
        <div className="knowledge-graph-panel__canvas">
          <LineageGraphView
            lineageData={normalized}
            selectedNodeId={selectedNodeId}
            selectedEdgeId={selectedEdgeId}
            onNodeSelect={(node) => onNodeSelect(node?.id || "")}
            onEdgeSelect={(edge) => onEdgeSelect(edge?.id || "")}
          />
        </div>
        <div className="knowledge-graph-panel__detail">
          <h4>{selectedNode?.label || selectedEdge?.label || "选择节点或关系"}</h4>
          <div className="knowledge-graph-panel__actions">
            <UiButton onClick={() => onReview("NODE", selectedNodeId, "ACCEPT")} disabled={!selectedNodeId}>接受</UiButton>
            <UiButton onClick={() => onReview("NODE", selectedNodeId, "REJECT")} disabled={!selectedNodeId}>驳回</UiButton>
            <UiInput value={mergeTargetCode} onChange={(event) => onMergeTargetChange(event.target.value)} placeholder="合并到 nodeCode" />
            <UiButton onClick={() => onReview("NODE", selectedNodeId, "MERGE")} disabled={!selectedNodeId || !mergeTargetCode}>合并</UiButton>
            <UiInput value={splitLabelsText} onChange={(event) => onSplitLabelsChange(event.target.value)} placeholder="拆分标签，逗号分隔" />
            <UiButton onClick={() => onReview("NODE", selectedNodeId, "SPLIT")} disabled={!selectedNodeId || !splitLabelsText.trim()}>拆分</UiButton>
          </div>
        </div>
      </div>
    </section>
  );
}
```

In `KnowledgePage.jsx`, load and refresh candidate graph:

```jsx
const [candidateGraph, setCandidateGraph] = useState(null);
const [candidateGraphLoading, setCandidateGraphLoading] = useState(false);
const [selectedCandidateNodeId, setSelectedCandidateNodeId] = useState("");
const [selectedCandidateEdgeId, setSelectedCandidateEdgeId] = useState("");
const [mergeTargetCode, setMergeTargetCode] = useState("");
const [splitLabelsText, setSplitLabelsText] = useState("");

const loadCandidateGraph = useCallback(async (taskIdInput) => {
  const nextTaskId = `${taskIdInput || ""}`.trim();
  if (!nextTaskId) {
    setCandidateGraph(null);
    return null;
  }
  setCandidateGraphLoading(true);
  try {
    const graph = await apiRequest(buildApiPath("importTaskCandidateGraph", { taskId: nextTaskId }), { token });
    setCandidateGraph(graph || null);
    return graph;
  } finally {
    setCandidateGraphLoading(false);
  }
}, [token]);

async function reviewCandidateGraph(targetType, targetCode, action) {
  if (!importBatchId || !targetCode) {
    return;
  }
  const splitLabels = splitLabelsText.split(/[，,]/).map((item) => item.trim()).filter(Boolean);
  await apiRequest(buildApiPath("importTaskCandidateGraphReview", { taskId: importBatchId }), {
    method: "POST",
    token,
    body: {
      targetType,
      targetCode,
      action,
      mergeIntoCode: mergeTargetCode || null,
      splitLabels,
      reason: "知识生产台人工确认",
      operator: "",
    },
  });
  const refreshedTask = await apiRequest(buildApiPath("importTaskById", { taskId: importBatchId }), { token });
  if (refreshedTask?.preprocessResult) {
    applyPreprocessPayload(refreshedTask.preprocessResult, { keepActiveStep: true });
  }
  await loadCandidateGraph(importBatchId);
}
```

- [ ] **Step 4: Re-generate OpenAPI types and re-run the frontend tests**

Run:

- `cd frontend && OPENAPI_SCHEMA_URL=http://127.0.0.1:8082/v3/api-docs npm run generate:openapi`
- `cd frontend && npm test -- src/pages/knowledge-import-utils.test.js src/pages/KnowledgePage.render.test.jsx`

Expected:

- `generate:openapi` succeeds and updates `src/types/openapi.d.ts`.
- Frontend tests PASS.

### Task 5: Verify the full import -> review -> confirm -> publish flow and sync status docs

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MvpKnowledgeGraphFlowIntegrationTest.java`
- Modify: `docs/testing/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation-test-report.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: Extend the MVP integration test to include candidate-graph review before confirmation**

Extend `MvpKnowledgeGraphFlowIntegrationTest.java`:

```java
@Test
void shouldFinishImportReviewConfirmPublishFlowWithCandidateEntityGraph() throws Exception {
    String token = loginAndGetToken("support", "support123");
    long domainId = createDomain(token);

    ObjectNode body = objectMapper.createObjectNode();
    body.put("rawText", PAYROLL_SAMPLE);
    body.put("sourceType", "PASTE_MD");
    body.put("sourceName", "mvp-candidate-graph-sample.md");
    body.put("preprocessMode", "RULE_ONLY");
    body.put("autoCreateDrafts", false);

    JsonNode preprocessPayload = objectMapper.readTree(mockMvc.perform(post("/api/import/preprocess")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());

    String taskId = preprocessPayload.path("importBatchId").asText();
    JsonNode graph = objectMapper.readTree(mockMvc.perform(get("/api/import/tasks/{taskId}/candidate-graph", taskId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());

    String identifierNodeCode = findNodeCodeByType(graph.path("nodes"), "IDENTIFIER");
    mockMvc.perform(post("/api/import/tasks/{taskId}/candidate-graph/review", taskId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "targetType":"NODE",
                              "targetCode":"%s",
                              "action":"ACCEPT",
                              "reason":"协议号作为主标识",
                              "operator":"support"
                            }
                            """.formatted(identifierNodeCode)))
            .andExpect(status().isOk());

    String candidateCode = findSceneCandidateCode(taskId);
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

    JsonNode scene = objectMapper.readTree(confirmResult.getResponse().getContentAsString());
    long sceneId = scene.path("id").asLong();
    assertThat(scene.path("inputsJson").asText()).contains("协议号");

    prepareMinimumPublishAssets(sceneId);
    mockMvc.perform(post("/api/scenes/{id}/publish", sceneId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "verifiedAt": "2026-03-30T11:00:00+08:00",
                              "changeSummary": "候选图谱复核后发布",
                              "operator": "support"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"));
}
```

- [ ] **Step 2: Run the end-to-end verification suite**

Run:

- `cd backend && mvn -q -Dtest=ImportCandidateGraphBuildServiceTest,ImportTaskApiIntegrationTest,MvpKnowledgeGraphFlowIntegrationTest test`
- `cd frontend && npm test -- src/pages/knowledge-import-utils.test.js src/pages/KnowledgePage.render.test.jsx`
- `cd frontend && npm run build`
- `cd backend && mvn -q -DskipTests package`

Expected:

- All targeted backend tests PASS.
- All targeted frontend tests PASS.
- Frontend build PASS.
- Backend package PASS.

- [ ] **Step 3: Update the parsing-stage test report with real execution results**

Replace the placeholder sections in `02-parsing-and-evidence-confirmation-test-report.md` with the real results:

```md
### 3.1 TC-01：候选实体图谱构建
- **输入：** 导入代发/薪资域样板材料，`autoCreateDrafts=false`。
- **预期输出：** 任务级 `Candidate Entity Graph` 返回候选场景、候选方案、标识对象、时间语义、来源表和证据节点。
- **实际结果：** 已通过，`ImportTaskApiIntegrationTest.shouldBuildTaskScopedCandidateEntityGraph` 验证通过。**（通过）**

### 3.2 TC-02：高风险节点人工确认回写
- **输入：** 接受一个 `TIME_SEMANTIC` 节点。
- **预期输出：** 候选场景载荷写回 `default_time_semantic`，后续 `confirmCandidate` 生成的正式 `Scene` 载荷可见。
- **实际结果：** 已通过，确认后的 `sqlVariantsJson` 包含 `TRX_DT`。**（通过）**

### 3.3 TC-03：导入 -> 复核 -> 确认 -> 发布闭环
- **输入：** 先复核候选图，再确认候选场景并发布。
- **预期输出：** 获得 `PUBLISHED` 场景，且复核结果进入正式载荷。
- **实际结果：** 已通过，`MvpKnowledgeGraphFlowIntegrationTest` 验证通过。**（通过）**
```

- [ ] **Step 4: Sync the delivery-status file to point at execution instead of planning**

Update `docs/engineering/current-delivery-status.md`:

```md
| 知识生产链路实施 | 候选资产表与导入质检确认已落地，且“导入中活图谱 + `graph_patch（图谱增量补丁）` 事件契约”与候选实体图谱实施计划已回写主文档 | [知识生产链路实施计划](../plans/2026-03-29-payroll-knowledge-production-implementation-plan.md)、[候选实体图谱实施计划](../plans/2026-03-30-candidate-entity-graph-implementation-plan.md)、[知识图谱与数据地图方案](../architecture/system-design.md)、[解析抽取与证据确认](../architecture/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation.md)、[前端工作台设计](../architecture/frontend-workbench-design.md) | 选择执行方式后，先从后端候选图谱红测开始，依次完成候选图构建、确认动作、`graph_patch` 回写和前端工作台接线 | Codex（实现） |
```
