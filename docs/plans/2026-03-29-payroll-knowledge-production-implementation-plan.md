# Payroll Knowledge Production Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn payroll-domain document upload into a formal three-stage production chain: `Source Intake（材料接入）` -> `Parsing & Evidence Confirmation（解析与证据确认）` -> `Governance Asset Modeling（治理资产建模）`.

**Architecture:** Build on top of the new `Source Material + Import Task` baseline. The import APIs remain the front door, but each stage must now write explicit control assets instead of relying on scene JSON blobs as the only intermediate state. Candidate review stays human-in-the-loop, and formal `Scene / Plan / Source Intake Contract / Source Contract / Coverage / Policy` drafts become the only publishable output.

**Tech Stack:** Spring Boot, Spring Data JPA, Flyway, MockMvc, React, Vite, Vitest

---

### Task 1: Make intake response and UI treat material/task as first-class objects

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportTaskApiIntegrationTest.java`
- Modify: `frontend/src/pages/KnowledgePage.jsx`
- Modify: `frontend/src/pages/WorkbenchPages.render.test.jsx`

- [ ] **Step 1: Add the failing frontend render assertion**

Extend `WorkbenchPages.render.test.jsx` so the ingest page shows both `taskId` and `materialId` after preprocess:

```jsx
it("shows persisted material and task identity after preprocess", async () => {
  render(<KnowledgePage />);
  await userEvent.type(screen.getByLabelText("来源名称"), "payroll-source-a.md");
  await userEvent.type(screen.getByLabelText("材料正文"), "### 场景标题：按协议号查询代发明细");
  await userEvent.click(screen.getByRole("button", { name: "开始预处理" }));

  expect(await screen.findByText(/taskId/i)).toBeInTheDocument();
  expect(await screen.findByText(/materialId/i)).toBeInTheDocument();
});
```

- [ ] **Step 2: Run the targeted frontend test to verify red**

Run:

- `cd frontend && npm test -- src/pages/WorkbenchPages.render.test.jsx`

Expected:

- FAIL because the ingest page still renders preprocess output as a transient result without material/task identity.

- [ ] **Step 3: Add the minimal page state wiring**

In `KnowledgePage.jsx`, keep the existing fetch call but persist response metadata:

```jsx
const [importMeta, setImportMeta] = useState(null);

async function handlePreprocessSubmit(payload) {
  const result = await apiRequest("/import/preprocess", { method: "POST", body: payload });
  setImportMeta({
    taskId: result.importBatchId,
    materialId: result.materialId,
    sourceName: payload.sourceName,
  });
  setPreprocessResult(result);
}
```

- [ ] **Step 4: Re-run the targeted test**

Run:

- `cd frontend && npm test -- src/pages/WorkbenchPages.render.test.jsx`

Expected:

- PASS.

### Task 2: Persist candidate review objects instead of relying only on preprocess JSON

**Files:**

- Create: `backend/src/main/resources/db/migration/V14__add_import_review_assets.sql`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/ImportSceneCandidatePO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/ImportEvidenceCandidatePO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/ImportSceneCandidateMapper.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/ImportEvidenceCandidateMapper.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCommandAppService.java`

- [ ] **Step 1: Add the failing persistence test**

Add a new integration test in `ImportTaskApiIntegrationTest.java`:

```java
@Test
void shouldPersistCandidateScenesAndEvidenceAgainstMaterial() throws Exception {
    String token = loginAndGetToken("support", "support123");
    MvcResult preprocessResult = mockMvc.perform(post("/api/import/preprocess")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "rawText":"### 场景标题：按协议号查询代发明细\\n- SQL: SELECT ...",
                              "sourceType":"PASTE_MD",
                              "sourceName":"payroll-scene-a.md",
                              "preprocessMode":"RULE_ONLY",
                              "autoCreateDrafts":true
                            }
                            """))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode root = objectMapper.readTree(preprocessResult.getResponse().getContentAsString());
    String taskId = root.path("importBatchId").asText();

    assertThat(importSceneCandidateMapper.findByTaskId(taskId)).isNotEmpty();
    assertThat(importEvidenceCandidateMapper.findByTaskId(taskId)).isNotEmpty();
}
```

- [ ] **Step 2: Run the test to verify red**

Run:

- `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest test`

Expected:

- FAIL because no candidate tables or repositories exist yet.

- [ ] **Step 3: Add review-asset schema**

Create `V14__add_import_review_assets.sql`:

```sql
CREATE TABLE caliber_import_scene_candidate (
    candidate_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    candidate_code VARCHAR(64) NOT NULL,
    scene_title VARCHAR(255) NOT NULL,
    scene_description TEXT,
    confidence_score DECIMAL(5,4),
    confirmation_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE caliber_import_evidence_candidate (
    evidence_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    candidate_code VARCHAR(64) NOT NULL,
    anchor_label VARCHAR(255) NOT NULL,
    quote_text TEXT,
    line_start INT,
    line_end INT,
    confirmation_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

- [ ] **Step 4: Save candidates in `ImportCommandAppService`**

After parsing completes, persist scene and evidence candidates keyed by `taskId` and `materialId`:

```java
private void persistReviewCandidates(String taskId, String materialId, PreprocessResultDTO base) {
    List<ImportSceneCandidatePO> sceneCandidates = mapSceneCandidates(taskId, materialId, base.scenes());
    importSceneCandidateMapper.saveAll(sceneCandidates);

    List<ImportEvidenceCandidatePO> evidenceCandidates = mapEvidenceCandidates(taskId, materialId, base);
    importEvidenceCandidateMapper.saveAll(evidenceCandidates);
}
```

- [ ] **Step 5: Re-run the targeted test**

Run:

- `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest test`

Expected:

- PASS, proving candidate scenes and evidence are persisted independently of scene drafts.

### Task 3: Add formal modeling commands for payroll multi-scene drafts

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/GraphAssetController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/GraphAssetAppService.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/M2M3ApiIntegrationTest.java`

- [ ] **Step 1: Add the failing multi-scene modeling test**

Extend `M2M3ApiIntegrationTest.java` to require at least two payroll scenes with distinct modeling differences:

```java
@Test
void shouldCreateFormalPayrollAssetsForMultiplePublishedCandidateScenes() throws Exception {
    // create scene A: payroll detail by agreement
    // create scene B: payroll batch result by company account
    // upsert plan/source intake/source contract/coverage/policy for each
    // assert both scenes have distinct identifierType or timeSemantic
}
```

- [ ] **Step 2: Run the targeted test to verify red**

Run:

- `cd backend && mvn -q -Dtest=M2M3ApiIntegrationTest test`

Expected:

- FAIL because the current happy path does not enforce payroll multi-scene formalization.

- [ ] **Step 3: Implement modeling commands with explicit formal assets**

In `GraphAssetAppService`, refuse to publish a scene that has no formal `Plan / Source Intake Contract / Source Contract / Coverage / Policy` set:

```java
private void requireFormalPayrollAssetSet(Long sceneId, Long planId) {
    requireSourceIntakeContract(sceneId);
    requireSourceContract(sceneId, planId);
    requireCoverageDeclaration(sceneId, planId);
    requirePolicy(sceneId, planId);
}
```

- [ ] **Step 4: Re-run the targeted test**

Run:

- `cd backend && mvn -q -Dtest=M2M3ApiIntegrationTest test`

Expected:

- PASS, with at least two payroll-domain scenes modeled through explicit governance assets.

### Task 4: Verify the full production chain for payroll domain

**Files:**

- Modify: `docs/engineering/current-delivery-status.md`
- Modify: `docs/architecture/features/iteration-01-knowledge-production/02-解析抽取与证据确认.md`
- Modify: `docs/architecture/features/iteration-01-knowledge-production/03-资产建模与治理对象编辑.md`

- [ ] **Step 1: Run backend production-chain verification**

Run:

- `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest,M2M3ApiIntegrationTest test`

Expected:

- PASS, covering intake persistence, candidate persistence, and formal modeling.

- [ ] **Step 2: Run frontend production-page verification**

Run:

- `cd frontend && npm test -- src/pages/WorkbenchPages.render.test.jsx src/pages/KnowledgePage.render.test.jsx`

Expected:

- PASS, with ingest UI surfacing material/task identity and current production stage.

- [ ] **Step 3: Sync delivery status**

Record completion as “知识生产链路已落到材料接入 / 解析确认 / 资产建模三段，并具备代发 / 薪资域多场景正式资产出口”.
