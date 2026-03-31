# Import Live Graph Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a real-time candidate graph to document import so the graph grows during extraction through `graph_patch（图谱增量补丁）` events and can be restored later from the final preprocess result.

**Architecture:** Keep the existing `/api/import/preprocess-stream` pipeline and extend it instead of introducing a new task system. Backend emits typed `graph_patch` batches during chunked preprocess, persists a final `candidateGraph` snapshot into `PreprocessResultDTO`, and frontend consumes patches through a pure reducer plus a dedicated `ImportLiveGraphCanvas` inside `KnowledgePage`.

**Tech Stack:** Spring Boot, Jackson, MockMvc, SpringDoc OpenAPI, React, D3, Vitest, Vite

---

## File Structure

- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/CandidateGraphDTO.java`
  Responsibility: final restorable candidate graph snapshot returned in `done` and stored inside `ImportTaskDTO.preprocessResult`.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportGraphNodeDTO.java`
  Responsibility: stable graph node contract for both final snapshot and streaming patches.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportGraphEdgeDTO.java`
  Responsibility: stable graph edge contract for both final snapshot and streaming patches.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportGraphPatchDTO.java`
  Responsibility: `graph_patch` event payload with `patchSeq`, stage metadata, added/updated nodes and edges, focus nodes, and summary.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/PreprocessResultDTO.java`
  Responsibility: carry `candidateGraph` in the final preprocess result and persisted task payload.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCandidateGraphAssembler.java`
  Responsibility: assemble minimal stable graph snapshots from preprocess scenes, source tables, outputs, and persisted evidence candidates.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCommandAppService.java`
  Responsibility: emit `graph_patch` events during chunked preprocess and attach `candidateGraph` to final `PreprocessResultDTO`.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportTaskCommandAppService.java`
  Responsibility: enrich final task payload with a restorable `candidateGraph` after candidate scenes/evidence are persisted.
- `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ImportController.java`
  Responsibility: stream `graph_patch` alongside existing `start` / `stage` / `draft` / `done` events.
- `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportTaskApiIntegrationTest.java`
  Responsibility: assert persisted task result exposes restorable `candidateGraph`.
- `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportPreprocessStreamApiIntegrationTest.java`
  Responsibility: assert `/api/import/preprocess-stream` emits `graph_patch` before `done`.
- `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiDocumentationIntegrationTest.java`
  Responsibility: assert OpenAPI exposes `/api/import/preprocess-stream` and `PreprocessResultDTO.candidateGraph`.
- `frontend/src/components/knowledge/importLiveGraphState.js`
  Responsibility: pure reducer and snapshot helpers for patch application, dedupe, recent additions, and restore.
- `frontend/src/components/knowledge/importLiveGraphState.test.js`
  Responsibility: reducer-level TDD for patch application, focus retention, dedupe, and snapshot restore.
- `frontend/src/components/knowledge/ImportLiveGraphCanvas.jsx`
  Responsibility: patch-friendly D3 graph canvas and the default inspector shell for the import page.
- `frontend/src/pages/KnowledgePage.jsx`
  Responsibility: receive `graph_patch`, reset and restore graph state, and render the live graph workbench.
- `frontend/src/pages/KnowledgePage.render.test.jsx`
  Responsibility: ensure the import preset renders the live graph shell without runtime regressions.
- `frontend/src/styles.css`
  Responsibility: styles for the live graph shell, empty state, inspector cards, and patch highlights.
- `frontend/src/types/openapi.d.ts`
  Responsibility: regenerated frontend contract snapshot after backend OpenAPI changes.
- `docs/testing/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation-test-report.md`
  Responsibility: feature-level test report and acceptance log for this work item.
- `docs/engineering/current-delivery-status.md`
  Responsibility: sync implementation status, verification state, and next handoff action after code lands.

### Task 1: Create the test/report scaffolds and make the backend contract fail

**Files:**
- Create: `docs/testing/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation-test-report.md`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportTaskApiIntegrationTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiDocumentationIntegrationTest.java`

- [ ] **Step 1: Create the test report skeleton**

Create `docs/testing/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation-test-report.md` with this initial content:

```md
# 解析抽取与证据确认测试文档

> 对应特性文档：`docs/architecture/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation.md`
> 当前阶段：计划中，等待导入中活图谱实现完成后回填真实结果

## 1. 测试目标

验证“导入中活图谱”首轮最小闭环是否成立，重点覆盖：

1. `/api/import/preprocess-stream` 是否在 `done` 前发出 `graph_patch`
2. `PreprocessResultDTO` 与 `ImportTaskDTO.preprocessResult` 是否携带可恢复的 `candidateGraph`
3. `KnowledgePage` 是否能在导入态渲染活图谱空态、增量补丁和默认 Inspector
4. 恢复导入任务时是否能从最终快照重新还原图谱

## 2. 测试范围

本轮只覆盖“导入 -> 流式补丁 -> 完成态快照 -> 页面恢复”这一条最小闭环。

## 3. 测试环境

1. 前端：`frontend`，React / D3 / Vitest
2. 后端：`backend`，Spring Boot / MockMvc / Maven
3. 联调口径：前端 `5174`，后端 `8082`

## 4. 预设测试案例

| 编号 | 用例 | 输入 | 预期输出 | 实际结果 |
| --- | --- | --- | --- | --- |
| TC-01 | 流式补丁先于完成态 | 导入一份代发样例材料 | `graph_patch` 在 `done` 前出现 | 未执行 |
| TC-02 | 完成态可恢复候选图 | 查询任务详情 | `preprocessResult.candidateGraph.nodes/edges` 存在 | 未执行 |
| TC-03 | 页面渲染活图谱空态 | 打开 `KnowledgePage` 导入预设 | 出现“候选实体图谱”“正在等待首批实体” | 未执行 |
| TC-04 | 恢复任务还原图谱 | 恢复刚完成的导入任务 | 画布节点与关系恢复 | 未执行 |
```

- [ ] **Step 2: Add the failing persisted snapshot test**

Extend `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportTaskApiIntegrationTest.java` with this test:

```java
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
```

- [ ] **Step 3: Extend the OpenAPI contract test**

Update `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiDocumentationIntegrationTest.java` to assert the stream path and final snapshot schema:

```java
@Test
void shouldExposeOpenApiDocumentWithCorePaths() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.openapi").exists())
            .andExpect(jsonPath("$.paths['/api/scenes']").exists())
            .andExpect(jsonPath("$.paths['/api/graphrag/query']").exists())
            .andExpect(jsonPath("$.paths['/api/import/preprocess-stream']").exists())
            .andExpect(jsonPath("$.components.schemas.PreprocessResultDTO.properties.candidateGraph").exists());
}
```

- [ ] **Step 4: Run the targeted backend tests and verify red**

Run:

- `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest,OpenApiDocumentationIntegrationTest test`

Expected:

- FAIL because `PreprocessResultDTO` does not yet contain `candidateGraph`, so task restore and OpenAPI schema assertions both fail.

### Task 2: Add backend graph snapshot DTOs and persist a restorable final graph

**Files:**
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/CandidateGraphDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportGraphNodeDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportGraphEdgeDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCandidateGraphAssembler.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/PreprocessResultDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportTaskCommandAppService.java`

- [ ] **Step 1: Add the DTO records**

Create `CandidateGraphDTO.java`, `ImportGraphNodeDTO.java`, and `ImportGraphEdgeDTO.java`:

```java
package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record CandidateGraphDTO(List<ImportGraphNodeDTO> nodes,
                                List<ImportGraphEdgeDTO> edges) {

    public static CandidateGraphDTO empty() {
        return new CandidateGraphDTO(List.of(), List.of());
    }
}
```

```java
package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record ImportGraphNodeDTO(String id,
                                 String nodeType,
                                 String label,
                                 String status,
                                 Double confidenceScore,
                                 List<String> evidenceRefs) {
}
```

```java
package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record ImportGraphEdgeDTO(String id,
                                 String sourceId,
                                 String targetId,
                                 String relationType,
                                 String status,
                                 Double confidenceScore,
                                 List<String> evidenceRefs) {
}
```

- [ ] **Step 2: Add the candidate graph assembler**

Create `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCandidateGraphAssembler.java`:

```java
package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.response.CandidateGraphDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportGraphEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportGraphNodeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.PreprocessResultDTO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportEvidenceCandidatePO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ImportCandidateGraphAssembler {

    public CandidateGraphDTO buildSnapshotFromResult(String taskId, String materialId, PreprocessResultDTO result) {
        List<JsonNode> scenes = result == null || result.scenes() == null ? List.of() : result.scenes();
        Map<String, ImportGraphNodeDTO> nodes = new LinkedHashMap<>();
        Map<String, ImportGraphEdgeDTO> edges = new LinkedHashMap<>();

        for (int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++) {
            JsonNode scene = scenes.get(sceneIndex);
            String sceneId = "candidate-scene:" + sceneIndex;
            double confidence = scene.path("quality").path("confidence").asDouble(result.confidenceScore() == null ? 0.0 : result.confidenceScore());
            String sceneTitle = text(scene.path("scene_title"), "未命名场景" + (sceneIndex + 1));
            nodes.put(sceneId, new ImportGraphNodeDTO(sceneId, "CandidateScene", sceneTitle, "PENDING_CONFIRMATION", confidence, List.of()));

            JsonNode inputs = scene.path("inputs").path("params");
            if (inputs.isArray()) {
                for (JsonNode item : inputs) {
                    String rawName = text(item.path("name"), text(item.path("label"), ""));
                    if (rawName.isBlank()) {
                        continue;
                    }
                    String nodeId = "field-input:" + rawName;
                    nodes.putIfAbsent(nodeId, new ImportGraphNodeDTO(nodeId, "FieldConcept", rawName, "PENDING_CONFIRMATION", confidence, List.of()));
                    edges.putIfAbsent(sceneId + "->" + nodeId, new ImportGraphEdgeDTO(sceneId + "->" + nodeId, sceneId, nodeId, "DECLARES_INPUT", "PENDING_CONFIRMATION", confidence, List.of()));
                }
            }

            JsonNode outputs = scene.path("outputs").path("fields");
            if (outputs.isArray()) {
                for (JsonNode item : outputs) {
                    String rawName = text(item.path("field_name"), text(item.path("name"), text(item.path("label"), "")));
                    if (rawName.isBlank()) {
                        continue;
                    }
                    String nodeId = "field-output:" + rawName;
                    nodes.putIfAbsent(nodeId, new ImportGraphNodeDTO(nodeId, "FieldConcept", rawName, "PENDING_CONFIRMATION", confidence, List.of()));
                    edges.putIfAbsent(sceneId + "=>"+ nodeId, new ImportGraphEdgeDTO(sceneId + "=>"+ nodeId, sceneId, nodeId, "DECLARES_OUTPUT", "PENDING_CONFIRMATION", confidence, List.of()));
                }
            }

            JsonNode variants = scene.path("sql_variants");
            if (variants.isArray()) {
                for (JsonNode variant : variants) {
                    JsonNode tables = variant.path("source_tables");
                    if (!tables.isArray()) {
                        continue;
                    }
                    for (JsonNode table : tables) {
                        String tableName = text(table, "");
                        if (tableName.isBlank()) {
                            continue;
                        }
                        String nodeId = "source-table:" + tableName;
                        nodes.putIfAbsent(nodeId, new ImportGraphNodeDTO(nodeId, "SourceTable", tableName, "PENDING_CONFIRMATION", confidence, List.of()));
                        edges.putIfAbsent(sceneId + "::" + nodeId, new ImportGraphEdgeDTO(sceneId + "::" + nodeId, sceneId, nodeId, "MAPS_TO_SOURCE", "PENDING_CONFIRMATION", confidence, List.of()));
                    }
                }
            }
        }
        return new CandidateGraphDTO(new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()));
    }

    public CandidateGraphDTO enrichWithPersistedEvidence(CandidateGraphDTO base,
                                                         List<ImportEvidenceCandidatePO> evidences) {
        if (base == null) {
            base = CandidateGraphDTO.empty();
        }
        Map<String, ImportGraphNodeDTO> nodes = new LinkedHashMap<>();
        base.nodes().forEach(node -> nodes.put(node.id(), node));
        Map<String, ImportGraphEdgeDTO> edges = new LinkedHashMap<>();
        base.edges().forEach(edge -> edges.put(edge.id(), edge));

        for (ImportEvidenceCandidatePO evidence : evidences) {
            String evidenceId = "evidence:" + evidence.getCandidateCode();
            nodes.put(evidenceId, new ImportGraphNodeDTO(
                    evidenceId,
                    "CandidateEvidenceFragment",
                    evidence.getAnchorLabel(),
                    evidence.getConfirmationStatus(),
                    1.0,
                    List.of(evidence.getCandidateCode())
            ));
            if (evidence.getSceneCandidateCode() != null && !evidence.getSceneCandidateCode().isBlank()) {
                String sceneId = "candidate-scene-code:" + evidence.getSceneCandidateCode();
                edges.put(sceneId + "~~" + evidenceId, new ImportGraphEdgeDTO(
                        sceneId + "~~" + evidenceId,
                        sceneId,
                        evidenceId,
                        "SUPPORTED_BY",
                        evidence.getConfirmationStatus(),
                        1.0,
                        List.of(evidence.getCandidateCode())
                ));
            }
        }
        return new CandidateGraphDTO(new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()));
    }

    private String text(JsonNode node, String fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        String value = node.asText("");
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
```

- [ ] **Step 3: Add `candidateGraph` to `PreprocessResultDTO` and persist it**

Update `PreprocessResultDTO.java` and the task write path:

```java
public record PreprocessResultDTO(String caliberImportJson,
                                  String mode,
                                  JsonNode global,
                                  List<JsonNode> scenes,
                                  JsonNode quality,
                                  List<String> warnings,
                                  Double confidenceScore,
                                  String confidenceLevel,
                                  Boolean lowConfidence,
                                  Long totalElapsedMs,
                                  CandidateGraphDTO candidateGraph,
                                  List<StageTimingDTO> stageTimings,
                                  List<PreprocessSceneDraftDTO> sceneDrafts,
                                  String importBatchId,
                                  String materialId) {
}
```

```java
@Transactional
public ImportTaskDTO markQualityReviewReady(String taskId, PreprocessResultDTO result) {
    ImportTaskPO po = requireTask(taskId);
    PersistedReviewCandidates persisted = persistReviewCandidates(po, result);
    CandidateGraphDTO graph = importCandidateGraphAssembler.enrichWithPersistedEvidence(
            importCandidateGraphAssembler.buildSnapshotFromResult(po.getTaskId(), po.getMaterialId(), result),
            persisted.evidences()
    );
    PreprocessResultDTO persistedResult = new PreprocessResultDTO(
            result.caliberImportJson(),
            result.mode(),
            result.global(),
            result.scenes(),
            result.quality(),
            result.warnings(),
            result.confidenceScore(),
            result.confidenceLevel(),
            result.lowConfidence(),
            result.totalElapsedMs(),
            graph,
            result.stageTimings(),
            result.sceneDrafts(),
            result.importBatchId(),
            result.materialId()
    );
    po.setStatus(STATUS_QUALITY_REVIEWING);
    po.setCurrentStep(Math.max(2, defaultStep(po.getCurrentStep())));
    po.setPreprocessResultJson(writeResultJson(persistedResult));
    po.setQualityConfirmed(false);
    po.setCompareConfirmed(false);
    po.setErrorMessage(null);
    po.setUpdatedAt(OffsetDateTime.now());
    return toDTO(importTaskMapper.save(po));
}

private record PersistedReviewCandidates(List<ImportSceneCandidatePO> scenes,
                                         List<ImportEvidenceCandidatePO> evidences) {
}
```

Also update every `new PreprocessResultDTO(...)` call in `ImportCommandAppService.java` so the new `candidateGraph` argument is present, using `CandidateGraphDTO.empty()` before the final persisted snapshot exists.

- [ ] **Step 4: Re-run the targeted backend tests**

Run:

- `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest,OpenApiDocumentationIntegrationTest test`

Expected:

- PASS. `ImportTaskApiIntegrationTest` now sees `preprocessResult.candidateGraph`, and the OpenAPI schema exposes the new `candidateGraph` field on `PreprocessResultDTO`.

- [ ] **Step 5: Commit the snapshot contract**

Run:

```bash
git add \
  backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/CandidateGraphDTO.java \
  backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportGraphNodeDTO.java \
  backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportGraphEdgeDTO.java \
  backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/PreprocessResultDTO.java \
  backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCandidateGraphAssembler.java \
  backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCommandAppService.java \
  backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportTaskCommandAppService.java \
  backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportTaskApiIntegrationTest.java \
  backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiDocumentationIntegrationTest.java \
  docs/testing/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation-test-report.md
git commit -m "feat: persist restorable import candidate graph"
```

### Task 3: Stream `graph_patch` events during preprocess

**Files:**
- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportPreprocessStreamApiIntegrationTest.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportGraphPatchDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ImportController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCommandAppService.java`

- [ ] **Step 1: Add the failing stream test**

Create `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportPreprocessStreamApiIntegrationTest.java`:

```java
package com.cmbchina.datadirect.caliber.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportPreprocessStreamApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldStreamGraphPatchEventsBeforeDone() throws Exception {
        String token = loginAndGetToken("support", "support123");

        MvcResult result = mockMvc.perform(post("/api/import/preprocess-stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {
                                  "rawText":"### 场景标题：按协议号查询代发明细\\n- 场景描述：用于核对代发明细结果。\\n- 结果字段：代发批次号、交易金额\\n- SQL: SELECT AGN_BCH_SEQ, TRX_AMT FROM PDM_VHIS.T05_AGN_DTL WHERE MCH_AGR_NBR='${AGR_ID}';",
                                  "sourceType":"PASTE_MD",
                                  "sourceName":"stream-graph-patch.md",
                                  "preprocessMode":"RULE_ONLY",
                                  "autoCreateDrafts":true
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andReturn();

        result.getAsyncResult(10000);
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("event:start");
        assertThat(body).contains("event:stage");
        assertThat(body).contains("event:graph_patch");
        assertThat(body).contains("event:done");
        assertThat(body.indexOf("event:graph_patch")).isLessThan(body.indexOf("event:done"));
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/system/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }
}
```

- [ ] **Step 2: Run the stream test to verify red**

Run:

- `cd backend && mvn -q -Dtest=ImportPreprocessStreamApiIntegrationTest test`

Expected:

- FAIL because `/api/import/preprocess-stream` still emits only `start` / `stage` / `draft` / `done`.

- [ ] **Step 3: Implement `ImportGraphPatchDTO` and emit patches**

Create `ImportGraphPatchDTO.java`:

```java
package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record ImportGraphPatchDTO(Integer patchSeq,
                                  String stageKey,
                                  Integer chunkIndex,
                                  Integer chunkTotal,
                                  List<ImportGraphNodeDTO> addedNodes,
                                  List<ImportGraphNodeDTO> updatedNodes,
                                  List<ImportGraphEdgeDTO> addedEdges,
                                  List<ImportGraphEdgeDTO> updatedEdges,
                                  List<String> focusNodeIds,
                                  String summary) {
}
```

Then update `ImportCommandAppService.java` and `ImportController.java`:

```java
public PreprocessResultDTO preprocessChunked(PreprocessImportCmd cmd,
                                             String importBatchIdInput,
                                             Consumer<StageTimingDTO> stageConsumer,
                                             Consumer<ImportGraphPatchDTO> graphPatchConsumer,
                                             Consumer<PreprocessSceneDraftDTO> draftConsumer) {
    enforceInputLineLimit(cmd.rawText());
    long totalStart = now();
    String importBatchId = normalizeImportBatchId(importBatchIdInput);
    String materialId = trackTaskStart(importBatchId, cmd);
    List<StageTimingDTO> stageTimings = new ArrayList<>();
    List<PreprocessSceneDraftDTO> sceneDrafts = new ArrayList<>();
    Set<String> emittedNodeIds = new LinkedHashSet<>();
    Set<String> emittedEdgeIds = new LinkedHashSet<>();
    AtomicInteger patchSeq = new AtomicInteger(0);
    ...
    if (chunks.size() == 1) {
        base = preprocessDirect(cmd);
        CandidateGraphDTO snapshot = importCandidateGraphAssembler.buildSnapshotFromResult(importBatchId, materialId, base);
        graphPatchConsumer.accept(toPatch(snapshot, patchSeq.incrementAndGet(), STAGE_EXTRACT, 1, 1, emittedNodeIds, emittedEdgeIds, "抽取完成"));
        ...
    } else {
        ...
        ChunkTaskResult completedResult = future.get();
        CandidateGraphDTO snapshot = importCandidateGraphAssembler.buildSnapshotFromResult(importBatchId, materialId, completedResult.result());
        graphPatchConsumer.accept(toPatch(snapshot, patchSeq.incrementAndGet(), STAGE_EXTRACT, completedResult.chunkIndex(), chunks.size(), emittedNodeIds, emittedEdgeIds, "分块抽取完成 " + completed + "/" + chunks.size()));
        ...
    }
    ...
}

private ImportGraphPatchDTO toPatch(CandidateGraphDTO snapshot,
                                    int patchSeq,
                                    String stageKey,
                                    int chunkIndex,
                                    int chunkTotal,
                                    Set<String> emittedNodeIds,
                                    Set<String> emittedEdgeIds,
                                    String summary) {
    List<ImportGraphNodeDTO> addedNodes = new ArrayList<>();
    List<ImportGraphNodeDTO> updatedNodes = new ArrayList<>();
    for (ImportGraphNodeDTO node : snapshot.nodes()) {
        if (emittedNodeIds.add(node.id())) {
            addedNodes.add(node);
        } else {
            updatedNodes.add(node);
        }
    }
    List<ImportGraphEdgeDTO> addedEdges = new ArrayList<>();
    List<ImportGraphEdgeDTO> updatedEdges = new ArrayList<>();
    for (ImportGraphEdgeDTO edge : snapshot.edges()) {
        if (emittedEdgeIds.add(edge.id())) {
            addedEdges.add(edge);
        } else {
            updatedEdges.add(edge);
        }
    }
    List<String> focusNodeIds = snapshot.nodes().stream().limit(3).map(ImportGraphNodeDTO::id).toList();
    return new ImportGraphPatchDTO(patchSeq, stageKey, chunkIndex, chunkTotal, addedNodes, updatedNodes, addedEdges, updatedEdges, focusNodeIds, summary);
}
```

```java
PreprocessResultDTO result = importCommandAppService.preprocessChunked(
        payload,
        taskId,
        stage -> sendEventUnchecked(emitter, "stage", stage),
        patch -> sendEventUnchecked(emitter, "graph_patch", patch),
        draft -> sendEventUnchecked(emitter, "draft", draft)
);
```

- [ ] **Step 4: Re-run the stream test**

Run:

- `cd backend && mvn -q -Dtest=ImportPreprocessStreamApiIntegrationTest test`

Expected:

- PASS. The response body now contains `event:graph_patch` before `event:done`.

- [ ] **Step 5: Commit the stream contract**

Run:

```bash
git add \
  backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportGraphPatchDTO.java \
  backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ImportController.java \
  backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCommandAppService.java \
  backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportPreprocessStreamApiIntegrationTest.java
git commit -m "feat: stream import graph patches"
```

### Task 4: Build the frontend patch reducer with pure tests

**Files:**
- Create: `frontend/src/components/knowledge/importLiveGraphState.js`
- Create: `frontend/src/components/knowledge/importLiveGraphState.test.js`

- [ ] **Step 1: Add the failing reducer tests**

Create `frontend/src/components/knowledge/importLiveGraphState.test.js`:

```js
import { describe, expect, it } from "vitest";
import {
  applyGraphPatch,
  createEmptyImportLiveGraphState,
  createImportLiveGraphStateFromSnapshot,
} from "./importLiveGraphState";

describe("importLiveGraphState", () => {
  it("applies a graph patch incrementally and dedupes by id", () => {
    let state = createEmptyImportLiveGraphState();

    state = applyGraphPatch(state, {
      patchSeq: 1,
      stageKey: "extract",
      chunkIndex: 1,
      chunkTotal: 1,
      addedNodes: [{ id: "candidate-scene:0", nodeType: "CandidateScene", label: "代发明细查询", status: "PENDING_CONFIRMATION", confidenceScore: 0.92, evidenceRefs: [] }],
      updatedNodes: [],
      addedEdges: [],
      updatedEdges: [],
      focusNodeIds: ["candidate-scene:0"],
      summary: "首批场景节点已加入",
    });

    state = applyGraphPatch(state, {
      patchSeq: 1,
      stageKey: "extract",
      chunkIndex: 1,
      chunkTotal: 1,
      addedNodes: [{ id: "candidate-scene:0", nodeType: "CandidateScene", label: "代发明细查询", status: "PENDING_CONFIRMATION", confidenceScore: 0.92, evidenceRefs: [] }],
      updatedNodes: [],
      addedEdges: [],
      updatedEdges: [],
      focusNodeIds: ["candidate-scene:0"],
      summary: "重复补丁不应重复加节点",
    });

    expect(state.graph.nodes).toHaveLength(1);
    expect(state.lastPatchSeq).toBe(1);
    expect(state.recentSummary).toBe("首批场景节点已加入");
  });

  it("keeps selected focus when later patches arrive", () => {
    const seed = {
      graph: {
        nodes: [{ id: "candidate-scene:0", nodeType: "CandidateScene", label: "代发明细查询", status: "PENDING_CONFIRMATION", confidenceScore: 0.92, evidenceRefs: [] }],
        edges: [],
      },
      selectedNodeId: "candidate-scene:0",
      selectedEdgeId: "",
      recentSummary: "",
      lastPatchSeq: 0,
      recentAddedNodeIds: [],
      recentAddedEdgeIds: [],
    };

    const next = applyGraphPatch(seed, {
      patchSeq: 2,
      stageKey: "merge",
      chunkIndex: 1,
      chunkTotal: 1,
      addedNodes: [{ id: "field-output:交易金额", nodeType: "FieldConcept", label: "交易金额", status: "PENDING_CONFIRMATION", confidenceScore: 0.92, evidenceRefs: [] }],
      updatedNodes: [],
      addedEdges: [],
      updatedEdges: [],
      focusNodeIds: ["field-output:交易金额"],
      summary: "补充输出字段",
    });

    expect(next.selectedNodeId).toBe("candidate-scene:0");
    expect(next.graph.nodes.map((item) => item.id)).toContain("field-output:交易金额");
  });

  it("restores state from final candidate graph snapshot", () => {
    const state = createImportLiveGraphStateFromSnapshot({
      nodes: [{ id: "candidate-scene:0", nodeType: "CandidateScene", label: "代发明细查询", status: "PENDING_CONFIRMATION", confidenceScore: 0.92, evidenceRefs: [] }],
      edges: [{ id: "candidate-scene:0=>field-output:交易金额", sourceId: "candidate-scene:0", targetId: "field-output:交易金额", relationType: "DECLARES_OUTPUT", status: "PENDING_CONFIRMATION", confidenceScore: 0.92, evidenceRefs: [] }],
    });

    expect(state.graph.nodes).toHaveLength(1);
    expect(state.graph.edges).toHaveLength(1);
    expect(state.lastPatchSeq).toBe(0);
  });
});
```

- [ ] **Step 2: Run the reducer test to verify red**

Run:

- `cd frontend && npm test -- src/components/knowledge/importLiveGraphState.test.js`

Expected:

- FAIL because `importLiveGraphState.js` does not exist yet.

- [ ] **Step 3: Implement the reducer helpers**

Create `frontend/src/components/knowledge/importLiveGraphState.js`:

```js
function normalizeArray(value) {
  return Array.isArray(value) ? value : [];
}

function upsertById(items, incoming) {
  const map = new Map(normalizeArray(items).map((item) => [item.id, item]));
  normalizeArray(incoming).forEach((item) => {
    if (!item?.id) {
      return;
    }
    map.set(item.id, {
      ...(map.get(item.id) || {}),
      ...item,
    });
  });
  return [...map.values()];
}

export function createEmptyImportLiveGraphState() {
  return {
    graph: { nodes: [], edges: [] },
    selectedNodeId: "",
    selectedEdgeId: "",
    recentSummary: "",
    lastPatchSeq: 0,
    recentAddedNodeIds: [],
    recentAddedEdgeIds: [],
  };
}

export function createImportLiveGraphStateFromSnapshot(snapshot) {
  return {
    ...createEmptyImportLiveGraphState(),
    graph: {
      nodes: normalizeArray(snapshot?.nodes),
      edges: normalizeArray(snapshot?.edges),
    },
  };
}

export function applyGraphPatch(state, patch) {
  const current = state || createEmptyImportLiveGraphState();
  const patchSeq = Number(patch?.patchSeq || 0);
  if (patchSeq > 0 && patchSeq <= Number(current.lastPatchSeq || 0)) {
    return current;
  }

  const addedNodes = normalizeArray(patch?.addedNodes);
  const updatedNodes = normalizeArray(patch?.updatedNodes);
  const addedEdges = normalizeArray(patch?.addedEdges);
  const updatedEdges = normalizeArray(patch?.updatedEdges);

  return {
    ...current,
    graph: {
      nodes: upsertById(current.graph?.nodes, [...addedNodes, ...updatedNodes]),
      edges: upsertById(current.graph?.edges, [...addedEdges, ...updatedEdges]),
    },
    recentSummary: `${patch?.summary || ""}`,
    lastPatchSeq: patchSeq,
    recentAddedNodeIds: addedNodes.map((item) => item.id).filter(Boolean),
    recentAddedEdgeIds: addedEdges.map((item) => item.id).filter(Boolean),
    selectedNodeId: current.selectedNodeId || "",
    selectedEdgeId: current.selectedEdgeId || "",
  };
}
```

- [ ] **Step 4: Re-run the reducer test**

Run:

- `cd frontend && npm test -- src/components/knowledge/importLiveGraphState.test.js`

Expected:

- PASS.

- [ ] **Step 5: Commit the reducer**

Run:

```bash
git add \
  frontend/src/components/knowledge/importLiveGraphState.js \
  frontend/src/components/knowledge/importLiveGraphState.test.js
git commit -m "feat: add import live graph state reducer"
```

### Task 5: Render the live graph workbench inside `KnowledgePage`

**Files:**
- Create: `frontend/src/components/knowledge/ImportLiveGraphCanvas.jsx`
- Modify: `frontend/src/pages/KnowledgePage.jsx`
- Modify: `frontend/src/pages/KnowledgePage.render.test.jsx`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Add the failing render assertion**

Extend `frontend/src/pages/KnowledgePage.render.test.jsx`:

```jsx
it("renders the import live graph shell for the import preset", () => {
  const html = renderKnowledgePage("import");

  expect(html).toContain("候选实体图谱");
  expect(html).toContain("正在等待首批实体");
  expect(html).toContain("当前抽取进度");
});
```

- [ ] **Step 2: Run the render and reducer tests to verify red**

Run:

- `cd frontend && npm test -- src/components/knowledge/importLiveGraphState.test.js src/pages/KnowledgePage.render.test.jsx`

Expected:

- FAIL because `KnowledgePage` still renders only the old import summary and has no live graph shell.

- [ ] **Step 3: Add the live graph canvas component**

Create `frontend/src/components/knowledge/ImportLiveGraphCanvas.jsx`:

```jsx
import { useEffect, useMemo, useRef } from "react";
import * as d3 from "d3";

function nodeTone(nodeType, status) {
  if (status === "REJECTED") return "#d97706";
  if (nodeType === "CandidateScene") return "#335c9f";
  if (nodeType === "SourceTable") return "#8a817c";
  if (nodeType === "CandidateEvidenceFragment") return "#8b5cf6";
  return "#2f7f73";
}

export function ImportLiveGraphCanvas({
  graph,
  stageText,
  patchSummary,
  selectedNodeId,
  onNodeSelect,
  emptyLabel = "正在等待首批实体",
}) {
  const svgRef = useRef(null);
  const shellRef = useRef(null);
  const nodes = useMemo(() => Array.isArray(graph?.nodes) ? graph.nodes.map((item) => ({ ...item })) : [], [graph]);
  const edges = useMemo(() => Array.isArray(graph?.edges) ? graph.edges.map((item) => ({ ...item })) : [], [graph]);

  useEffect(() => {
    if (!shellRef.current || !svgRef.current || nodes.length === 0) {
      return;
    }
    const width = shellRef.current.clientWidth || 900;
    const height = shellRef.current.clientHeight || 520;
    const svg = d3.select(svgRef.current);
    svg.selectAll("*").remove();
    svg.attr("width", width).attr("height", height);

    const simulation = d3.forceSimulation(nodes)
      .force("link", d3.forceLink(edges).id((d) => d.id).distance(120))
      .force("charge", d3.forceManyBody().strength(-340))
      .force("center", d3.forceCenter(width / 2, height / 2))
      .force("collide", d3.forceCollide(36));

    const link = svg.append("g").selectAll("line")
      .data(edges)
      .enter()
      .append("line")
      .attr("stroke", "#b7c1d6")
      .attr("stroke-width", 1.4)
      .attr("stroke-dasharray", (d) => d.status === "MERGED" ? "4 3" : null);

    const node = svg.append("g").selectAll("circle")
      .data(nodes)
      .enter()
      .append("circle")
      .attr("r", (d) => d.id === selectedNodeId ? 13 : 10)
      .attr("fill", (d) => nodeTone(d.nodeType, d.status))
      .attr("stroke", (d) => d.id === selectedNodeId ? "#0f172a" : "#ffffff")
      .attr("stroke-width", 2.5)
      .style("cursor", "pointer")
      .on("click", (_event, d) => onNodeSelect?.(d));

    const label = svg.append("g").selectAll("text")
      .data(nodes)
      .enter()
      .append("text")
      .text((d) => d.label)
      .attr("font-size", 11)
      .attr("fill", "#334155")
      .attr("text-anchor", "middle");

    simulation.on("tick", () => {
      link
        .attr("x1", (d) => d.source.x)
        .attr("y1", (d) => d.source.y)
        .attr("x2", (d) => d.target.x)
        .attr("y2", (d) => d.target.y);
      node
        .attr("cx", (d) => d.x)
        .attr("cy", (d) => d.y);
      label
        .attr("x", (d) => d.x)
        .attr("y", (d) => d.y + 24);
    });

    return () => simulation.stop();
  }, [edges, nodes, onNodeSelect, selectedNodeId]);

  return (
    <section className="import-live-graph-card">
      <div className="import-live-graph-head">
        <div>
          <h3>候选实体图谱</h3>
          <p className="subtle-note">导入中的候选实体与关系会按批次补入图谱，点击节点后右侧切换为对象详情。</p>
        </div>
        <div className="import-live-graph-meta">
          <strong>{stageText || "待导入"}</strong>
          <span>{patchSummary || emptyLabel}</span>
        </div>
      </div>
      <div ref={shellRef} className="import-live-graph-shell">
        {nodes.length === 0 ? (
          <div className="import-live-graph-empty">{emptyLabel}</div>
        ) : (
          <svg ref={svgRef} className="import-live-graph-svg" aria-label="候选实体图谱画布" />
        )}
      </div>
    </section>
  );
}
```

- [ ] **Step 4: Wire reducer state into `KnowledgePage`**

Update `frontend/src/pages/KnowledgePage.jsx`:

```jsx
import { ImportLiveGraphCanvas } from "../components/knowledge/ImportLiveGraphCanvas";
import {
  applyGraphPatch,
  createEmptyImportLiveGraphState,
  createImportLiveGraphStateFromSnapshot,
} from "../components/knowledge/importLiveGraphState";

const [liveGraphState, setLiveGraphState] = useState(() => createEmptyImportLiveGraphState());
const [liveGraphFocus, setLiveGraphFocus] = useState(null);

function resetImportRuntimeState() {
  setImportStageTimings([]);
  setImportPercent(0);
  setImportStageText("导入任务已提交");
  setImportElapsedMs(0);
  setLiveGraphState(createEmptyImportLiveGraphState());
  setLiveGraphFocus(null);
}

// inside handlePreprocessSubmit
resetImportRuntimeState();

// inside SSE onEvent
if (event.event === "graph_patch") {
  const detail = event.data || {};
  setLiveGraphState((prev) => applyGraphPatch(prev, detail));
  return;
}

// inside applyPreprocessPayload
if (response?.candidateGraph) {
  setLiveGraphState(createImportLiveGraphStateFromSnapshot(response.candidateGraph));
}

const selectedGraphNode = useMemo(() => {
  return liveGraphState.graph.nodes.find((item) => item.id === liveGraphState.selectedNodeId) || null;
}, [liveGraphState]);
```

Render the shell above the existing import task board:

```jsx
<div className="import-live-graph-layout">
  <ImportLiveGraphCanvas
    graph={liveGraphState.graph}
    stageText={importStageText}
    patchSummary={liveGraphState.recentSummary}
    selectedNodeId={liveGraphState.selectedNodeId}
    onNodeSelect={(node) => {
      setLiveGraphState((prev) => ({ ...prev, selectedNodeId: node?.id || "", selectedEdgeId: "" }));
      setLiveGraphFocus(node || null);
    }}
  />
  <aside className="import-live-inspector">
    {liveGraphFocus ? (
      <>
        <h3>{liveGraphFocus.label}</h3>
        <p className="subtle-note">{liveGraphFocus.nodeType} · 置信度 {Math.round(Number(liveGraphFocus.confidenceScore || 0) * 100)}%</p>
        <p className="subtle-note">状态：{liveGraphFocus.status || "PENDING_CONFIRMATION"}</p>
      </>
    ) : (
      <>
        <h3>当前抽取进度</h3>
        <p className="subtle-note">{importStageText || "待导入"}</p>
        <p className="subtle-note">{liveGraphState.recentSummary || "点击节点查看详情"}</p>
      </>
    )}
  </aside>
</div>
```

- [ ] **Step 5: Add the styles**

Append to `frontend/src/styles.css`:

```css
.import-live-graph-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 0.9fr);
  gap: 16px;
  margin-top: 16px;
}

.import-live-graph-card,
.import-live-inspector {
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.96), rgba(240, 246, 255, 0.9));
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.05);
}

.import-live-graph-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px 0;
}

.import-live-graph-shell {
  position: relative;
  min-height: 520px;
  padding: 12px 16px 18px;
}

.import-live-graph-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 460px;
  border: 1px dashed rgba(148, 163, 184, 0.45);
  border-radius: 18px;
  color: #64748b;
  background: radial-gradient(circle at top, rgba(255, 255, 255, 0.9), rgba(237, 243, 255, 0.72));
}

.import-live-graph-svg {
  width: 100%;
  height: 100%;
  display: block;
}

.import-live-inspector {
  padding: 20px;
}
```

- [ ] **Step 6: Re-run the targeted frontend tests**

Run:

- `cd frontend && npm test -- src/components/knowledge/importLiveGraphState.test.js src/pages/KnowledgePage.render.test.jsx`

Expected:

- PASS. The reducer is green and the import preset SSR now includes the live graph shell.

- [ ] **Step 7: Commit the frontend workbench**

Run:

```bash
git add \
  frontend/src/components/knowledge/ImportLiveGraphCanvas.jsx \
  frontend/src/components/knowledge/importLiveGraphState.js \
  frontend/src/components/knowledge/importLiveGraphState.test.js \
  frontend/src/pages/KnowledgePage.jsx \
  frontend/src/pages/KnowledgePage.render.test.jsx \
  frontend/src/styles.css
git commit -m "feat: add live import graph workbench"
```

### Task 6: Regenerate contracts, finish the test report, and run end-to-end verification

**Files:**
- Modify: `frontend/src/types/openapi.d.ts`
- Modify: `docs/testing/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation-test-report.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: Regenerate frontend OpenAPI types from the real backend port**

Run:

- `cd frontend && OPENAPI_SCHEMA_URL=http://127.0.0.1:8082/v3/api-docs npm run generate:openapi`

Expected:

- PASS with output similar to `generated src/types/openapi.d.ts from http://127.0.0.1:8082/v3/api-docs`.

- [ ] **Step 2: Run the backend target verification**

Run:

- `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest,ImportPreprocessStreamApiIntegrationTest,OpenApiDocumentationIntegrationTest test`

Expected:

- PASS. Backend verifies restorable `candidateGraph`, streaming `graph_patch`, and OpenAPI contract output.

- [ ] **Step 3: Run the frontend target verification**

Run:

- `cd frontend && npm test -- src/components/knowledge/importLiveGraphState.test.js src/pages/KnowledgePage.render.test.jsx`

Expected:

- PASS.

- [ ] **Step 4: Run the lightweight build verification**

Run:

- `cd frontend && npm run build`
- `cd backend && mvn -q -DskipTests package`
- `bash scripts/start_backend.sh`
- `curl -sSf http://127.0.0.1:8082/v3/api-docs | jq -r '.openapi, .info.title, (.paths | length)'`

Expected:

- Frontend build PASS.
- Backend package PASS.
- `/v3/api-docs` returns `3.1.0`, `数据直通车 API`, and a positive path count.

- [ ] **Step 5: Replace the test report skeleton with real results**

Update `docs/testing/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation-test-report.md` to record the executed commands and outcomes:

```md
## 5. TDD 与测试命令引用

当前已执行：

1. `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest,ImportPreprocessStreamApiIntegrationTest,OpenApiDocumentationIntegrationTest test`
2. `cd frontend && npm test -- src/components/knowledge/importLiveGraphState.test.js src/pages/KnowledgePage.render.test.jsx`
3. `cd frontend && OPENAPI_SCHEMA_URL=http://127.0.0.1:8082/v3/api-docs npm run generate:openapi`
4. `cd frontend && npm run build`
5. `cd backend && mvn -q -DskipTests package`
6. `bash scripts/start_backend.sh`
7. `curl -sSf http://127.0.0.1:8082/v3/api-docs | jq -r '.openapi, .info.title, (.paths | length)'`

执行结果：

1. 后端目标测试：通过；已验证 `candidateGraph` 持久化与 `graph_patch` 事件顺序。
2. 前端目标测试：通过；已验证 reducer 去重、恢复和页面活图谱壳层渲染。
3. 契约生成：通过；`src/types/openapi.d.ts` 已按 `8082` 环境重新生成。
4. 构建与服务探活：通过；前端构建成功，后端打包成功，`/v3/api-docs` 返回 `200 OK`。
```

- [ ] **Step 6: Sync `current-delivery-status` after implementation**

Update `docs/engineering/current-delivery-status.md` so the “知识生产链路实施” entry reflects the new state:

```md
| 知识生产链路实施 | 候选资产表与导入质检确认已落地，且“导入中活图谱 + `graph_patch（图谱增量补丁）` 事件契约”已回写主文档 | ... | `reviewing（测试与评审中）` | 已完成 `/api/import/preprocess-stream` 的 `graph_patch` 事件流、`candidateGraph` 完成态快照、导入页活图谱 reducer 与导入工作台左图右检视接入 | 根据代码检视结论与测试文档进入收尾或继续修复 | 后端目标测试、前端目标测试、OpenAPI 生成、构建和 `/v3/api-docs` 探活均通过 | 无 | Codex（实现） | 2026-03-30 |
```

- [ ] **Step 7: Commit the verification and docs sync**

Run:

```bash
git add \
  frontend/src/types/openapi.d.ts \
  docs/testing/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation-test-report.md \
  docs/engineering/current-delivery-status.md
git commit -m "docs: record import live graph verification"
```

## Self-Review

### Spec coverage

1. “导入中活图谱 + `graph_patch` 事件契约”由 Task 2 和 Task 3 覆盖。
2. “最终可恢复的 `candidateGraph` 快照”由 Task 2 覆盖。
3. “前端局部补丁 reducer + 左图右检视工作台”由 Task 4 和 Task 5 覆盖。
4. “测试文档、状态同步、OpenAPI 生成和探活”由 Task 1 和 Task 6 覆盖。

### Placeholder scan

1. 本计划未使用 `TBD`、`TODO`、`implement later`、`add appropriate error handling` 之类占位语。
2. 每个代码步骤都给出具体文件、代码骨架、命令和预期结果。

### Type consistency

1. 后端统一使用 `CandidateGraphDTO`、`ImportGraphNodeDTO`、`ImportGraphEdgeDTO`、`ImportGraphPatchDTO` 四个名字，不再混用 `snapshot` / `graphPayload` / `streamPatch` 等临时称呼。
2. 前端统一使用 `createEmptyImportLiveGraphState`、`createImportLiveGraphStateFromSnapshot`、`applyGraphPatch` 三个入口，避免页面层和测试层命名漂移。

