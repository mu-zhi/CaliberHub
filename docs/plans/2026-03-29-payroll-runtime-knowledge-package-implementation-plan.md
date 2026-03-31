# Payroll Runtime Knowledge Package Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make payroll-domain runtime retrieval resolve the correct `Scene（业务场景）` and `Plan（方案资产）` across multiple published scenes, then return a structured `Knowledge Package（知识包）` or an explicit clarification result when the question is ambiguous.

**Architecture:** Keep runtime as a read-only orchestration layer over published snapshots. Break the flow into explicit query decomposition, slot completion, scene recall, plan selection, coverage decision, policy decision, path resolution, and knowledge-package assembly. Ambiguity is handled as a first-class output, not a hidden fallback.

**Tech Stack:** Spring Boot, MockMvc, graph runtime query services, React, Vitest

---

### Task 1: Add failing multi-scene runtime and clarification tests

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/WorkbenchContextPages.test.jsx`

- [ ] **Step 1: Add the failing backend runtime tests**

Extend `KnowledgePackageApiIntegrationTest.java` with two cases:

```java
@Test
void shouldResolvePayrollQuestionToCorrectSceneAndPlan() throws Exception {
    // published scene A: 按协议号查询代发明细
    // published scene B: 按公司户查询批次结果
    // ask a protocol-number question
    // assert knowledge package sceneCode == PAYROLL_DETAIL_BY_AGREEMENT
    // assert selectedPlanCode matches the published plan
}

@Test
void shouldReturnClarificationForCrossScenePayrollQuestion() throws Exception {
    // ask a question mixing 公司户 and 协议号 without enough time context
    // assert response contains decomposition/clarification result instead of mixed package
}
```

- [ ] **Step 2: Add the failing frontend assertion**

Extend `WorkbenchContextPages.test.jsx`:

```jsx
it("renders clarification instead of fake mixed knowledge package for ambiguous payroll question", async () => {
  render(<KnowledgePackageWorkbenchPage />);
  await userEvent.type(screen.getByLabelText("问题输入"), "查询公司户最近一年代发批次和协议号明细");
  await userEvent.click(screen.getByRole("button", { name: "开始检索" }));

  expect(await screen.findByText(/需要补充条件/)).toBeInTheDocument();
});
```

- [ ] **Step 3: Run the targeted tests to verify red**

Run:

- `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`
- `cd frontend && npm test -- src/pages/WorkbenchContextPages.test.jsx`

Expected:

- Both FAIL because runtime still follows a single-sample path and does not surface clarification as a first-class result.

### Task 2: Split runtime orchestration into explicit scene recall and plan selection

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/KnowledgePackageQueryAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/GraphRagQueryAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/GraphRagController.java`

- [ ] **Step 1: Add explicit scene-search and plan-select methods**

Refactor `KnowledgePackageQueryAppService` into smaller stages:

```java
SceneRecallResult sceneRecall = recallScene(cmd.queryText(), cmd.slotHints(), cmd.snapshotId());
if (sceneRecall.requiresClarification()) {
    return KnowledgePackageDTO.clarification(sceneRecall.toClarificationResult());
}

PlanSelectionResult planSelection = selectPlan(sceneRecall.sceneId(), cmd.slotHints(), cmd.snapshotId());
CoverageDecision coverageDecision = decideCoverage(sceneRecall, planSelection, cmd.slotHints());
```

- [ ] **Step 2: Keep `/api/graphrag/query` as the final assembly endpoint**

The controller keeps one final call, but it must accept explicit runtime context:

```java
public record KnowledgePackageQueryCmd(
        String queryText,
        Long snapshotId,
        Long selectedSceneId,
        Long selectedPlanId,
        JsonNode slotHints
) {
}
```

- [ ] **Step 3: Re-run the backend runtime test**

Run:

- `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`

Expected:

- Scene/plan assertions should now fail deeper in package assembly if any block is still missing, rather than at recall ambiguity handling.

### Task 3: Return full knowledge-package blocks and clarification payloads

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackageDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackageSceneDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackagePlanDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackageCoverageDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackagePolicyDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackagePathDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackageEvidenceDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackageRiskDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackageTraceDTO.java`

- [ ] **Step 1: Add clarification/decomposition fields**

Update `KnowledgePackageDTO` to support both successful package and clarification result:

```java
public record KnowledgePackageDTO(
        KnowledgePackageSceneDTO scene,
        KnowledgePackagePlanDTO plan,
        KnowledgePackageContractDTO contract,
        KnowledgePackageCoverageDTO coverage,
        KnowledgePackagePolicyDTO policy,
        KnowledgePackagePathDTO path,
        List<KnowledgePackageEvidenceDTO> evidence,
        List<KnowledgePackageRiskDTO> risks,
        KnowledgePackageTraceDTO trace,
        JsonNode clarification
) {
}
```

- [ ] **Step 2: Populate all blocks in the query service**

Assemble:

```java
return new KnowledgePackageDTO(
        buildSceneBlock(sceneRecall),
        buildPlanBlock(planSelection),
        buildContractBlock(planSelection),
        buildCoverageBlock(coverageDecision),
        buildPolicyBlock(policyDecision),
        buildPathBlock(pathResolution),
        buildEvidenceBlock(pathResolution),
        buildRiskBlock(coverageDecision, policyDecision),
        buildTraceBlock(sceneRecall, planSelection, snapshotId),
        null
);
```

- [ ] **Step 3: Re-run the backend test**

Run:

- `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`

Expected:

- PASS for both “correct scene/plan hit” and “clarification instead of fake mix” cases.

### Task 4: Render clarification and package blocks in the runtime workbench

**Files:**

- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/KnowledgePage.render.test.jsx`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: Add conditional rendering for clarification**

In `KnowledgePackageWorkbenchPage.jsx`:

```jsx
if (result?.clarification) {
  return (
    <section>
      <h3>需要补充条件</h3>
      <p>{result.clarification.summary}</p>
    </section>
  );
}
```

- [ ] **Step 2: Render the required nine blocks for successful result**

Keep a fixed block order:

```jsx
<KnowledgePackageSection title="场景" data={result.scene} />
<KnowledgePackageSection title="方案" data={result.plan} />
<KnowledgePackageSection title="契约视图" data={result.contract} />
<KnowledgePackageSection title="覆盖" data={result.coverage} />
<KnowledgePackageSection title="策略结果" data={result.policy} />
<KnowledgePackageSection title="路径" data={result.path} />
<KnowledgePackageSection title="证据" data={result.evidence} />
<KnowledgePackageSection title="风险说明" data={result.risks} />
<KnowledgePackageSection title="追踪信息" data={result.trace} />
```

- [ ] **Step 3: Run the frontend runtime verification**

Run:

- `cd frontend && npm test -- src/pages/WorkbenchContextPages.test.jsx src/pages/KnowledgePage.render.test.jsx`

Expected:

- PASS, with runtime workbench showing either the nine-block knowledge package or a clarification card.

- [ ] **Step 4: Run full runtime verification**

Run:

- `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest,M2M3ApiIntegrationTest test`
- `cd frontend && npm test -- src/pages/WorkbenchContextPages.test.jsx src/pages/KnowledgePage.render.test.jsx`

Expected:

- PASS, proving payroll multi-scene runtime recall and structured knowledge package output are stable.
