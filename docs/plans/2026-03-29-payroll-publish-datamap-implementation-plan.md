# Payroll Publish Datamap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish payroll-domain formal assets into `Version Snapshot（版本快照）` and project only the required objects into graph/data-map views for browsing, coverage tracing, and runtime jump-in.

**Architecture:** Treat relational control assets as the source of truth. Publishing creates or refreshes a bounded snapshot; graph projection reads from that snapshot and never reads raw scene drafts. Data map pages consume projected snapshot data plus relational metadata for node detail and coverage tracing.

**Tech Stack:** Spring Boot, Flyway, JPA, graph runtime module, React, Vitest

---

### Task 1: Make publish gate assert snapshot-first behavior

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/M2M3ApiIntegrationTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`

- [ ] **Step 1: Add the failing snapshot gate test**

Add a publish assertion:

```java
@Test
void shouldCreatePublishedSnapshotBeforeGraphProjection() throws Exception {
    // arrange a payroll scene with full formal assets
    // publish the scene
    // assert publish response contains snapshotId
    // assert graph projection status is tied to the same snapshotId
}
```

- [ ] **Step 2: Run the targeted tests to verify red**

Run:

- `cd backend && mvn -q -Dtest=M2M3ApiIntegrationTest,KnowledgePackageApiIntegrationTest test`

Expected:

- FAIL because publish flow and projection status are not yet locked to the same explicit snapshot contract.

### Task 2: Persist and expose snapshot-linked projection metadata

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/GraphProjectionAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/GraphRagQueryAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/GraphProjectionStatusDTO.java`

- [ ] **Step 1: Add `snapshotId` as a required projection field**

Update the projection status DTO:

```java
public record GraphProjectionStatusDTO(
        Long sceneId,
        Long snapshotId,
        String projectionStatus,
        OffsetDateTime projectedAt
) {
}
```

- [ ] **Step 2: Enforce snapshot input in projection service**

Update `GraphProjectionAppService`:

```java
public GraphProjectionStatusDTO projectPublishedScene(Long sceneId, Long snapshotId, String operator) {
    SceneVersionPO snapshot = requirePublishedSnapshot(sceneId, snapshotId);
    rebuildProjection(snapshot);
    return new GraphProjectionStatusDTO(sceneId, snapshotId, "READY", OffsetDateTime.now());
}
```

- [ ] **Step 3: Re-run the targeted tests**

Run:

- `cd backend && mvn -q -Dtest=M2M3ApiIntegrationTest,KnowledgePackageApiIntegrationTest test`

Expected:

- Snapshot-related assertions move from missing contract failures to any remaining data-map/runtime behavior gaps.

### Task 3: Make data-map browsing strictly snapshot-scoped

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DataMapGraphController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/DataMapQueryAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/DataMapGraphDtoAdapter.java`
- Modify: `frontend/src/components/datamap/DataMapContainer.jsx`
- Modify: `frontend/src/components/datamap/LineageGraphView.jsx`

- [ ] **Step 1: Add the failing data-map render test**

Extend `WorkbenchContextPages.test.jsx` so data map requires snapshot context:

```jsx
it("loads datamap by snapshot and highlights runtime focus object", async () => {
  render(<LineageGraphView snapshotId={2026032901} focusRef="scene:payroll_detail" />);
  expect(await screen.findByText(/snapshotId/i)).toBeInTheDocument();
  expect(await screen.findByText(/payroll_detail/i)).toBeInTheDocument();
});
```

- [ ] **Step 2: Run the frontend test to verify red**

Run:

- `cd frontend && npm test -- src/pages/WorkbenchContextPages.test.jsx`

Expected:

- FAIL because current data-map page does not require or render explicit snapshot scope.

- [ ] **Step 3: Wire snapshot-scoped query options**

Update backend and frontend to require `snapshotId` in graph/data-map requests:

```java
public record DataMapGraphQueryOptions(
        Long snapshotId,
        String focusRef,
        boolean includeCoverage,
        boolean includeEvidence
) {
}
```

```jsx
apiRequest(`/datamap/graph?snapshotId=${snapshotId}&focusRef=${encodeURIComponent(focusRef)}`)
```

- [ ] **Step 4: Re-run the targeted frontend test**

Run:

- `cd frontend && npm test -- src/pages/WorkbenchContextPages.test.jsx`

Expected:

- PASS, with page state driven by `snapshotId` and focus object.

### Task 4: Verify publish-to-datamap path end to end

**Files:**

- Modify: `docs/engineering/current-delivery-status.md`
- Modify: `docs/architecture/features/iteration-02-runtime-and-governance/10-数据地图浏览与覆盖追踪.md`

- [ ] **Step 1: Run the publish + datamap backend verification**

Run:

- `cd backend && mvn -q -Dtest=M2M3ApiIntegrationTest,KnowledgePackageApiIntegrationTest test`

Expected:

- PASS, with publish responses, projection status, and graph reads all bound to the same snapshot ID.

- [ ] **Step 2: Run the data-map frontend verification**

Run:

- `cd frontend && npm test -- src/pages/WorkbenchContextPages.test.jsx src/pages/WorkbenchPages.render.test.jsx`

Expected:

- PASS, and runtime jump-ins focus the correct node inside the same snapshot-scoped map.

- [ ] **Step 3: Sync delivery status**

Record completion as “发布动作已产出快照，数据地图已收口为快照投影浏览、覆盖追踪和运行定位三类职责”.
