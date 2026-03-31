# Cross-scene Canonical Entity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the documented `10b-cross-scene-canonical-entity-and-domain-graph.md` path from canonical-entity persistence to domain graph read-model consumption and runtime reuse.

**Architecture:** Continue to treat MySQL control tables as the canonical source. Extend the existing canonical entity and snapshot-binding services so publish freezes memberships, domain graph queries expose shared canonical nodes, and runtime retrieval consumes canonical narrowing without bypassing published scene boundaries.

**Tech Stack:** Spring Boot, MyBatis mappers, Maven, React, Vitest, repository gate scripts

---

## Design Inputs

- `docs/architecture/features/iteration-02-runtime-and-governance/10b-cross-scene-canonical-entity-and-domain-graph.md`
- `docs/testing/features/iteration-02-runtime-and-governance/10b-cross-scene-canonical-entity-and-domain-graph-test-report.md`
- `scripts/run_canonical_entity_gate.sh`

## File Map

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/GraphReadService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/DataMapGraphDtoAdapter.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/KnowledgePackageQueryAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/CanonicalSnapshotBindingService.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/CanonicalSnapshotBindingServiceTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/CanonicalEntityResolutionServiceTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MvpKnowledgeGraphFlowIntegrationTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
- Modify: `frontend/src/components/datamap/DataMapContainer.jsx`
- Modify: `frontend/src/pages/datamap-adapter.js`
- Modify: `frontend/src/pages/datamap-adapter.test.js`
- Modify: `frontend/src/pages/WorkbenchContextPages.test.jsx`
- Modify: `docs/testing/features/iteration-02-runtime-and-governance/10b-cross-scene-canonical-entity-and-domain-graph-test-report.md`

### Task 1: Update the test report and gate script expectations

**Files:**

- Modify: `docs/testing/features/iteration-02-runtime-and-governance/10b-cross-scene-canonical-entity-and-domain-graph-test-report.md`

- [ ] Add explicit Task 4-7 acceptance checks for `DOMAIN` graph rendering, impact reuse, and runtime reuse.
- [ ] Record the canonical gate command as required evidence.

Run: `rg -n "DOMAIN|runtime reuse|impact" docs/testing/features/iteration-02-runtime-and-governance/10b-cross-scene-canonical-entity-and-domain-graph-test-report.md`

Expected: The report includes the remaining Task 4-7 checks.

### Task 2: Add failing tests for domain graph aggregation and runtime narrowing

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MvpKnowledgeGraphFlowIntegrationTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/CanonicalSnapshotBindingServiceTest.java`

- [ ] Add a graph integration assertion for `root_type=DOMAIN` returning canonical nodes plus scene memberships under a single snapshot.
- [ ] Add a runtime assertion proving canonical narrowing helps recall while final selection still resolves to published `scene_id + snapshot_id + plan_id`.

Run: `cd backend && mvn -q -Dtest=CanonicalSnapshotBindingServiceTest,MvpKnowledgeGraphFlowIntegrationTest,KnowledgePackageApiIntegrationTest test`

Expected: FAIL because domain aggregation or runtime reuse is not fully wired yet.

### Task 3: Complete publish-time canonical freeze behavior

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/CanonicalSnapshotBindingService.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/CanonicalEntityResolutionServiceTest.java`

- [ ] Ensure publish freezes canonical memberships and visible canonical relations without cloning canonical entities.
- [ ] Keep ambiguous objects in explicit review-required states rather than force-merging them.

Run: `cd backend && mvn -q -Dtest=CanonicalEntityResolutionServiceTest,CanonicalSnapshotBindingServiceTest test`

Expected: PASS.

### Task 4: Expose canonical-aware domain graph reads

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/GraphReadService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/DataMapGraphDtoAdapter.java`

- [ ] Add `DOMAIN` graph aggregation that emits canonical nodes, scene members, and snapshot-scoped relations.
- [ ] Preserve explicit `readSource` and snapshot metadata in the response.

Run: `cd backend && mvn -q -Dtest=MvpKnowledgeGraphFlowIntegrationTest test`

Expected: PASS.

### Task 5: Reuse canonical narrowing in runtime query flow

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/KnowledgePackageQueryAppService.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`

- [ ] Add canonical-assisted narrowing before final scene selection.
- [ ] Keep final execution pinned to published scene instances and plans.

Run: `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`

Expected: PASS.

### Task 6: Render canonical nodes in the frontend data map

**Files:**

- Modify: `frontend/src/components/datamap/DataMapContainer.jsx`
- Modify: `frontend/src/pages/datamap-adapter.js`
- Modify: `frontend/src/pages/datamap-adapter.test.js`
- Modify: `frontend/src/pages/WorkbenchContextPages.test.jsx`

- [ ] Normalize the new canonical-node and membership-edge shapes in the adapter.
- [ ] Render shared canonical identity and scene-member context without breaking snapshot-scoped highlighting.

Run: `cd frontend && npm test -- src/pages/datamap-adapter.test.js src/pages/WorkbenchContextPages.test.jsx`

Expected: PASS.

### Task 7: Run the canonical gate end-to-end

**Files:**

- Modify: `docs/testing/features/iteration-02-runtime-and-governance/10b-cross-scene-canonical-entity-and-domain-graph-test-report.md`

- [ ] Re-run the scripted gate, targeted backend tests, and frontend tests.
- [ ] Record final evidence and unresolved follow-up items.

Run:

- `bash scripts/run_canonical_entity_gate.sh`
- `cd backend && mvn -q -Dtest=CanonicalEntityPersistenceTest,SceneGraphAssetSyncCanonicalTest,CanonicalEntityResolutionServiceTest,CanonicalSnapshotBindingServiceTest,MvpKnowledgeGraphFlowIntegrationTest,KnowledgePackageApiIntegrationTest test`
- `cd frontend && npm test -- src/pages/datamap-adapter.test.js src/pages/WorkbenchContextPages.test.jsx`

Expected:

- Gate script PASS.
- Backend targeted tests PASS.
- Frontend targeted tests PASS.
