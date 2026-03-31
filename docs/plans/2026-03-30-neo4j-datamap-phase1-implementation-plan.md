# Neo4j Datamap Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep `MySQL（关系型数据库，MySQL）` as the source of truth while bringing `Neo4j（图数据库产品，Neo4j）` forward as the published-snapshot graph read model for `Data Map Graph API（数据地图图谱接口）` and `Impact Analysis API（影响分析接口）`, with publish-time verification and relational fallback.

**Architecture:** Publishing already creates `Version Snapshot（版本快照）`; Phase 1 extends that path so the same `snapshotId` drives graph projection, publish-time verification, and the read gate. Query-side routing stays conservative: only when the latest projection record for `sceneId + snapshotId` is `PASSED` and graph read is enabled do data-map queries read `Neo4j`; otherwise they fall back to the snapshot-scoped relational bundle. API JSON keeps the repo’s existing camelCase style, so the design terms `read_source / projection_verification_status / projection_verified_at` land as `readSource / projectionVerificationStatus / projectionVerifiedAt`.

**Tech Stack:** Spring Boot, Flyway, JPA, Neo4j Java Driver, JUnit 5, MockMvc, Vitest, React

---

## File Map

- Modify: `backend/src/main/resources/db/migration/V16__snapshot_projection_tracking.sql`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/graphrag/SnapshotProjectionPO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/graphrag/SnapshotProjectionMapper.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/ProjectionValidationService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/ReadSourceRouter.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/Neo4jGraphReadService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/graphrag/Neo4jDriverConfig.java`
- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/ProjectionValidationServiceTest.java`
- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/GraphQueryServiceTest.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/GraphProjectionStatusDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/GraphProjectionAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/SceneCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/GraphReadService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/GraphQueryService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/ImpactAnalysisService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/datamap/DataMapGraphResponseDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/graphrag/GraphRuntimeProperties.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MvpKnowledgeGraphFlowIntegrationTest.java`
- Modify: `frontend/src/pages/datamap-adapter.js`
- Modify: `frontend/src/pages/datamap-adapter.test.js`
- Modify: `frontend/src/types/dataMap.ts`
- Create: `docs/testing/features/iteration-02-runtime-and-governance/10-data-map-neo4j-phase1-test-report.md`
- Modify: `docs/engineering/current-delivery-status.md`

---

### Task 1: Lock the external contract before touching projection logic

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MvpKnowledgeGraphFlowIntegrationTest.java`
- Modify: `frontend/src/pages/datamap-adapter.test.js`

- [ ] **Step 1: Add failing backend assertions for graph read metadata**

Add the new expectations to the existing end-to-end flow immediately after `/api/datamap/graph` and `/api/datamap/impact-analysis`:

```java
.andExpect(jsonPath("$.readSource").value("RELATIONAL"))
.andExpect(jsonPath("$.projectionVerificationStatus").value("SKIPPED"))
.andExpect(jsonPath("$.projectionVerifiedAt").isNotEmpty())
```

```java
.andExpect(jsonPath("$.graph.readSource").value("RELATIONAL"))
.andExpect(jsonPath("$.graph.projectionVerificationStatus").value("SKIPPED"))
```

Use the existing projection-disabled test profile as the initial fallback baseline. The purpose of this red test is to freeze the outward contract before backend refactoring starts.

- [ ] **Step 2: Add failing frontend adapter assertions for the same fields**

Extend the `normalizes graph dto and forwards filters` and `normalizes impact analysis payload` cases:

```javascript
expect(graph.readSource).toBe("RELATIONAL");
expect(graph.projectionVerificationStatus).toBe("SKIPPED");
expect(graph.projectionVerifiedAt).toBe("2026-03-30T10:15:00+08:00");
```

```javascript
expect(impact.graph?.readSource).toBe("RELATIONAL");
expect(impact.graph?.projectionVerificationStatus).toBe("SKIPPED");
```

Update the mocked payloads in the same test file so they now contain:

```javascript
readSource: "RELATIONAL",
projectionVerificationStatus: "SKIPPED",
projectionVerifiedAt: "2026-03-30T10:15:00+08:00",
```

- [ ] **Step 3: Run the targeted tests to verify red**

Run:

- `cd backend && mvn -q -Dtest=MvpKnowledgeGraphFlowIntegrationTest test`
- `cd frontend && npm test -- src/pages/datamap-adapter.test.js`

Expected:

- Backend fails because `DataMapGraphResponseDTO` does not yet expose `readSource`, `projectionVerificationStatus`, or `projectionVerifiedAt`.
- Frontend fails because `normalizeLineageGraph()` drops the new fields.

### Task 2: Reuse the existing snapshot-projection tracking model and harden verification

**Files:**

- Modify: `backend/src/main/resources/db/migration/V16__snapshot_projection_tracking.sql`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/graphrag/SnapshotProjectionPO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/graphrag/SnapshotProjectionMapper.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/ProjectionValidationService.java`
- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/ProjectionValidationServiceTest.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/GraphProjectionStatusDTO.java`

- [ ] **Step 1: Write the failing verification comparison test**

Create `ProjectionValidationServiceTest.java` and lock three cases: exact match passes, missing edge fails, extra edge also fails. This avoids the current “只看数量不看集合”的验证漏洞。

```java
@Test
void shouldPassWhenNodeAndEdgeSetsMatch() {
    DataMapGraphResponseDTO relational = graph(
            List.of(node("scene:12", "SCENE"), node("plan:3", "PLAN")),
            List.of(edge("scene:12>USES_PLAN>plan:3", "scene:12", "plan:3", "USES_PLAN"))
    );
    DataMapGraphResponseDTO neo4j = graph(
            List.of(node("scene:12", "SCENE"), node("plan:3", "PLAN")),
            List.of(edge("scene:12>USES_PLAN>plan:3", "scene:12", "plan:3", "USES_PLAN"))
    );

    SnapshotProjectionPO result = service.recordProjectionAndValidate(
            12L,
            88L,
            Set.of("scene:12", "plan:3"),
            Set.of("scene:12|USES_PLAN|plan:3"),
            Set.of("scene:12", "plan:3"),
            Set.of("scene:12|USES_PLAN|plan:3")
    );

    assertThat(result.getVerificationStatus()).isEqualTo("PASSED");
    assertThat(result.getVerificationMessage()).contains("2 nodes").contains("1 edges");
    assertThat(result.getVerifiedAt()).isNotNull();
}

@Test
void shouldFailWhenNeo4jGraphMissesExpectedRelation() {
    SnapshotProjectionPO result = service.recordProjectionAndValidate(
            12L,
            88L,
            Set.of("scene:12", "plan:3"),
            Set.of("scene:12|USES_PLAN|plan:3"),
            Set.of("scene:12", "plan:3"),
            Set.of()
    );

    assertThat(result.getVerificationStatus()).isEqualTo("FAILED");
    assertThat(result.getVerificationMessage()).contains("missingEdges");
}

@Test
void shouldFailWhenNeo4jGraphContainsExtraEdge() {
    SnapshotProjectionPO result = service.recordProjectionAndValidate(
            12L,
            88L,
            Set.of("scene:12", "plan:3"),
            Set.of("scene:12|USES_PLAN|plan:3"),
            Set.of("scene:12", "plan:3", "policy:8"),
            Set.of("scene:12|USES_PLAN|plan:3", "plan:3|GOVERNED_BY_POLICY|policy:8")
    );

    assertThat(result.getVerificationStatus()).isEqualTo("FAILED");
    assertThat(result.getVerificationMessage()).contains("extraEdges");
}
```

This test intentionally avoids `Neo4j` network I/O. It drives the validation service with explicit expected / actual key sets so the comparison rule stays deterministic.

- [ ] **Step 2: Run the new backend unit test to verify red**

Run:

- `cd backend && mvn -q -Dtest=ProjectionValidationServiceTest test`

Expected:

- FAIL because `ProjectionValidationService` does not yet accept explicit expected / actual node-edge key sets.

- [ ] **Step 3: Reuse the existing snapshot projection table and harden the validation service**

Do not create a new `V15__...` migration. The repo already contains `V16__snapshot_projection_tracking.sql` and `SnapshotProjectionPO / SnapshotProjectionMapper`. First diff the SQL against the entity; only if there is a real schema gap after rebasing should you create the next available migration version.

Change `ProjectionValidationService` to accept explicit graph-shape sets and to persist them into the existing `SnapshotProjectionPO`:

```java
public SnapshotProjectionPO recordProjectionAndValidate(Long sceneId,
                                                        Long snapshotId,
                                                        Set<String> expectedNodeKeys,
                                                        Set<String> expectedEdgeKeys,
                                                        Set<String> actualNodeKeys,
                                                        Set<String> actualEdgeKeys) {
    OffsetDateTime now = OffsetDateTime.now();
    SnapshotProjectionPO po = snapshotProjectionMapper.findBySceneIdAndSnapshotId(sceneId, snapshotId)
            .orElseGet(() -> freshProjection(sceneId, snapshotId, now));

    po.setProjectionStatus("SUCCEEDED");
    po.setNodeCount(actualNodeKeys.size());
    po.setEdgeCount(actualEdgeKeys.size());
    po.setProjectedAt(now);
    po.setUpdatedAt(now);

    Set<String> missingNodes = new LinkedHashSet<>(expectedNodeKeys);
    missingNodes.removeAll(actualNodeKeys);
    Set<String> extraNodes = new LinkedHashSet<>(actualNodeKeys);
    extraNodes.removeAll(expectedNodeKeys);
    Set<String> missingEdges = new LinkedHashSet<>(expectedEdgeKeys);
    missingEdges.removeAll(actualEdgeKeys);
    Set<String> extraEdges = new LinkedHashSet<>(actualEdgeKeys);
    extraEdges.removeAll(expectedEdgeKeys);

    if (missingNodes.isEmpty() && extraNodes.isEmpty() && missingEdges.isEmpty() && extraEdges.isEmpty()) {
        po.setVerificationStatus("PASSED");
        po.setVerificationMessage("Projection verified: nodes=%d, edges=%d"
                .formatted(actualNodeKeys.size(), actualEdgeKeys.size()));
    } else {
        po.setVerificationStatus("FAILED");
        po.setVerificationMessage("missingNodes=%s, extraNodes=%s, missingEdges=%s, extraEdges=%s"
                .formatted(missingNodes, extraNodes, missingEdges, extraEdges));
    }
    po.setVerifiedAt(now);
    return snapshotProjectionMapper.save(po);
}
```

Keep `GraphProjectionStatusDTO` as the scene-level projection status DTO, but source `verificationStatus / verificationMessage / verifiedAt` from `SnapshotProjectionPO` whenever a snapshot row exists.

- [ ] **Step 4: Re-run the verification unit test**

Run:

- `cd backend && mvn -q -Dtest=ProjectionValidationServiceTest test`

Expected:

- PASS, proving the comparison logic rejects both missing and extra graph data before it is wired into publish flow.

### Task 3: Make publish flow write one projection gate per `sceneId + snapshotId`

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/SceneCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/GraphProjectionAppService.java`

- [ ] **Step 1: Pass the just-created snapshot into graph projection**

Update publish flow in `SceneCommandAppService.publish()`:

```java
SceneVersionDTO snapshot = sceneVersionAppService.createPublishedSnapshot(saved.getId(), cmd.changeSummary(), cmd.operator());
graphProjectionAppService.refreshProjection(
        saved.getId(),
        snapshot.id(),
        saved.getSceneCode(),
        cmd.operator()
);
```

Keep the rest of the publish transaction unchanged. The only new invariant is that projection no longer guesses the latest snapshot; it must use the snapshot produced by the same publish call.

- [ ] **Step 2: Make `GraphProjectionAppService` snapshot-scoped and verification-aware**

Change the service signature and event lifecycle:

```java
@Transactional
public GraphProjectionStatusDTO refreshProjection(Long sceneId,
                                                  Long snapshotId,
                                                  String sceneCode,
                                                  String operator) {
    ProjectionEventPO event = projectionEventMapper.findBySceneId(sceneId).orElseGet(ProjectionEventPO::new);
    OffsetDateTime now = OffsetDateTime.now();
    if (event.getId() == null) {
        event.setSceneId(sceneId);
        event.setSceneCode(sceneCode);
        event.setCreatedAt(now);
    }
    event.setSceneCode(sceneCode);
    event.setStage("PREPARE");
    event.setStatus("PENDING");
    event.setMessage("正在准备图投影");
    event.setPayloadJson(buildPayloadJson(sceneId));
    event.setUpdatedAt(now);
    projectionEventMapper.save(event);

    if (!graphRuntimeProperties.isProjectionEnabled()) {
        event.setStage("RELATIONAL_ONLY");
        event.setStatus("SKIPPED");
        event.setMessage("图投影关闭，数据地图将回退关系库");
        event.setLastProjectedAt(now);
        event.setUpdatedAt(now);
        projectionValidationService.recordSkipped(sceneId, snapshotId, "图投影已关闭");
        return toDTO(projectionEventMapper.save(event));
    }

    projectScene(sceneId, snapshotId);
    DataMapGraphResponseDTO relational = buildRelationalSnapshotGraph(sceneId, snapshotId);
    Neo4jGraphResult neo4j = neo4jGraphReadService.readGraph(
            sceneId,
            snapshotId,
            DataMapGraphQueryOptions.of(snapshotId, null, null, null, null)
    );
    SnapshotProjectionPO validation = projectionValidationService.recordProjectionAndValidate(
            sceneId,
            snapshotId,
            nodeKeys(relational.nodes()),
            edgeKeys(relational.edges()),
            nodeKeys(neo4j.nodes()),
            edgeKeys(neo4j.edges())
    );
    event.setStage("VERIFIED");
    event.setStatus("SUCCEEDED");
    event.setMessage(validation.getVerificationMessage());
    event.setLastProjectedAt(now);
    event.setUpdatedAt(now);
    return toDTO(projectionEventMapper.save(event));
}
```

Inside the same file, change `projectScene()` so every `MERGE` and cleanup query is snapshot-scoped. Use `sceneId + snapshotId` as the stable boundary:

```java
tx.run("""
    MATCH (s:Scene {sceneId:$sceneId, snapshotId:$snapshotId})-[r]->(n)
    DETACH DELETE n
    """, Values.parameters("sceneId", sceneId, "snapshotId", snapshotId));
```

```java
tx.run("""
    MERGE (s:Scene {sceneId:$sceneId, snapshotId:$snapshotId})
    SET s.sceneCode=$sceneCode, s.sceneTitle=$sceneTitle, s.status=$status
    """, Values.parameters(
            "sceneId", sceneId,
            "snapshotId", snapshotId,
            "sceneCode", scene.getSceneCode(),
            "sceneTitle", scene.getSceneTitle(),
            "status", scene.getStatus() == null ? "DRAFT" : scene.getStatus().name()
    ));
```

Add the two private helpers in the same service so verification compares the same DTO shape that the API will later emit:

```java
private DataMapGraphResponseDTO buildRelationalSnapshotGraph(Long sceneId, Long snapshotId) {
    GraphSceneBundle bundle = graphReadService.loadBundle("SCENE", sceneId, snapshotId);
    DataMapGraphResponseDTO graph = dataMapGraphDtoAdapter.buildGraph(
            "scene:" + sceneId,
            bundle,
            DataMapGraphQueryOptions.of(snapshotId, null, null, null, null)
    );
    return new DataMapGraphResponseDTO(
            graph.rootRef(),
            graph.sceneId(),
            graph.sceneName(),
            snapshotId,
            ReadSource.RELATIONAL,
            ProjectionVerificationStatus.PENDING,
            null,
            graph.nodes(),
            graph.edges()
    );
}

private Set<String> nodeKeys(List<DataMapGraphNodeDTO> nodes) {
    return nodes.stream()
            .map(DataMapGraphNodeDTO::id)
            .collect(Collectors.toCollection(LinkedHashSet::new));
}

private Set<String> edgeKeys(List<DataMapGraphEdgeDTO> edges) {
    return edges.stream()
            .map(edge -> edge.source() + "|" + edge.relationType() + "|" + edge.target())
            .collect(Collectors.toCollection(LinkedHashSet::new));
}
```

- [ ] **Step 3: Re-run the current end-to-end backend test**

Run:

- `cd backend && mvn -q -Dtest=MvpKnowledgeGraphFlowIntegrationTest test`

Expected:

- The test still fails, but the failure moves from “projection metadata missing everywhere” to “query-side DTO does not yet surface the stored verification state”.

### Task 4: Route `DataMap Graph API` through Neo4j only when the gate is `PASSED`

**Files:**

- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/GraphQueryServiceTest.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/ReadSourceRouter.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/Neo4jGraphReadService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/GraphReadService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/GraphQueryService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/datamap/DataMapGraphResponseDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/graphrag/Neo4jDriverConfig.java`

- [ ] **Step 1: Write the failing query routing test**

Create `GraphQueryServiceTest.java` with mocked collaborators:

```java
@Test
void shouldUseNeo4jWhenProjectionVerificationPassed() {
    when(graphReadService.resolveSceneId("SCENE", 12L)).thenReturn(12L);
    when(readSourceRouter.decide(12L, 88L)).thenReturn(
            new ReadSourceRouter.ReadSourceDecision(
                    ReadSource.NEO4J,
                    88L,
                    ProjectionVerificationStatus.PASSED,
                    OffsetDateTime.parse("2026-03-30T10:15:00+08:00")
            )
    );
    when(neo4jGraphReadService.readGraph(eq(12L), eq(88L), any(DataMapGraphQueryOptions.class)))
            .thenReturn(new Neo4jGraphResult("scene:12", 12L, "代发样板场景", List.of(), List.of()));

    DataMapGraphResponseDTO result = service.queryGraph("SCENE", 12L, 88L, null, null, null, null);

    assertThat(result.readSource()).isEqualTo("NEO4J");
}

@Test
void shouldFallbackToRelationalWhenNeo4jThrowsAfterPassedDecision() {
    when(graphReadService.resolveSceneId("SCENE", 12L)).thenReturn(12L);
    when(readSourceRouter.decide(12L, 88L)).thenReturn(
            new ReadSourceRouter.ReadSourceDecision(
                    ReadSource.NEO4J,
                    88L,
                    ProjectionVerificationStatus.PASSED,
                    OffsetDateTime.parse("2026-03-30T10:15:00+08:00")
            )
    );
    when(neo4jGraphReadService.readGraph(eq(12L), eq(88L), any(DataMapGraphQueryOptions.class)))
            .thenThrow(new IllegalStateException("Neo4j unavailable"));
    when(graphReadService.loadBundle("SCENE", 12L, 88L)).thenReturn(relationalBundle());

    DataMapGraphResponseDTO result = service.queryGraph("SCENE", 12L, 88L, null, null, null, null);

    assertThat(result.readSource()).isEqualTo("RELATIONAL");
    assertThat(result.projectionVerificationStatus()).isEqualTo(ProjectionVerificationStatus.PASSED);
}
```

Add helper methods in the same test file so the routing expectations are fully concrete:

```java
private GraphSceneBundle relationalBundle() {
    SceneDTO scene = new SceneDTO(
            12L,
            "SCN_PAYROLL_SAMPLE",
            "代发样板场景",
            3L,
            "代发域",
            "FACT_DETAIL",
            "PUBLISHED",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
    );
    return new GraphSceneBundle(scene, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
}
```

- [ ] **Step 2: Run the routing test to verify red**

Run:

- `cd backend && mvn -q -Dtest=GraphQueryServiceTest test`

Expected:

- FAIL because `GraphQueryService` still loads relational bundles without `decision.snapshotId()` and the fallback branch is not yet covered by a dedicated unit test.

- [ ] **Step 3: Implement the routing seam and the response metadata**

Do not add a second read-toggle property. Reuse `caliber.graph-runtime.read-enabled` and the existing `ReadSourceRouter`. The implementation focus is to make the current partial read-routing code snapshot-correct and failure-safe.

Keep the response DTO shape aligned with the existing enums:

```java
public record DataMapGraphResponseDTO(
        String rootRef,
        Long sceneId,
        String sceneName,
        Long snapshotId,
        ReadSource readSource,
        ProjectionVerificationStatus projectionVerificationStatus,
        OffsetDateTime projectionVerifiedAt,
        List<DataMapGraphNodeDTO> nodes,
        List<DataMapGraphEdgeDTO> edges
) {
}
```

Make `GraphReadService` snapshot-aware for fallback reads by threading `snapshotId` into `loadBundle()` and adding an explicit resolver for the effective snapshot:

```java
@Transactional(readOnly = true)
public Long resolveSnapshotId(Long sceneId, Long requestedSnapshotId) {
    if (requestedSnapshotId != null) {
        return requestedSnapshotId;
    }
    return sceneVersionMapper.findTopBySceneIdOrderByVersionNoDesc(sceneId)
            .map(SceneVersionPO::getId)
            .orElse(null);
}

@Transactional(readOnly = true)
public GraphSceneBundle loadBundle(String rootType, Long rootId, Long snapshotId) {
    Long sceneId = resolveSceneId(rootType, rootId);
    Long effectiveSnapshotId = resolveSnapshotId(sceneId, snapshotId);
    SceneDTO scene = sceneQueryAppService.getById(sceneId);
    return new GraphSceneBundle(
            scene,
            publishedPlans(sceneId, effectiveSnapshotId),
            publishedOutputContracts(sceneId, effectiveSnapshotId),
            publishedContractViews(sceneId, effectiveSnapshotId),
            publishedCoverageDeclarations(sceneId, effectiveSnapshotId),
            publishedPolicies(sceneId, effectiveSnapshotId),
            publishedEvidenceFragments(sceneId, effectiveSnapshotId),
            publishedSourceContracts(sceneId, effectiveSnapshotId),
            publishedSourceIntakeContracts(sceneId, effectiveSnapshotId)
    );
}
```

Add the snapshot filters directly in the same class so fallback reads stay aligned with current `GraphAssetAppService` DTOs:

```java
private List<PlanDTO> publishedPlans(Long sceneId, Long snapshotId) {
    return graphAssetAppService.listPlans(sceneId, null, "PUBLISHED").stream()
            .filter(plan -> Objects.equals(plan.snapshotId(), snapshotId))
            .toList();
}

private List<OutputContractDTO> publishedOutputContracts(Long sceneId, Long snapshotId) {
    return graphAssetAppService.listOutputContracts(sceneId, null, "PUBLISHED").stream()
            .filter(contract -> Objects.equals(contract.snapshotId(), snapshotId))
            .toList();
}

private List<ContractViewDTO> publishedContractViews(Long sceneId, Long snapshotId) {
    return graphAssetAppService.listContractViews(sceneId, null, "ACTIVE").stream()
            .filter(view -> Objects.equals(view.snapshotId(), snapshotId))
            .toList();
}

private List<CoverageDeclarationDTO> publishedCoverageDeclarations(Long sceneId, Long snapshotId) {
    return graphAssetAppService.listCoverageDeclarations(sceneId, null, "ACTIVE").stream()
            .filter(coverage -> Objects.equals(coverage.snapshotId(), snapshotId))
            .toList();
}

private List<PolicyDTO> publishedPolicies(Long sceneId, Long snapshotId) {
    return graphAssetAppService.listPolicies(sceneId, null, "ACTIVE").stream()
            .filter(policy -> Objects.equals(policy.snapshotId(), snapshotId))
            .toList();
}

private List<EvidenceFragmentDTO> publishedEvidenceFragments(Long sceneId, Long snapshotId) {
    return graphAssetAppService.listEvidenceFragments(sceneId, null, "PUBLISHED").stream()
            .filter(evidence -> Objects.equals(evidence.snapshotId(), snapshotId))
            .toList();
}

private List<SourceContractDTO> publishedSourceContracts(Long sceneId, Long snapshotId) {
    return graphAssetAppService.listSourceContracts(sceneId, null, "ACTIVE").stream()
            .filter(contract -> Objects.equals(contract.snapshotId(), snapshotId))
            .toList();
}

private List<SourceIntakeContractDTO> publishedSourceIntakeContracts(Long sceneId, Long snapshotId) {
    return graphAssetAppService.listSourceIntakeContracts(sceneId, null, "ACTIVE").stream()
            .filter(contract -> Objects.equals(contract.snapshotId(), snapshotId))
            .toList();
}
```

Use the router decision as the single source for query-side read selection; do not query projection status tables directly from `GraphQueryService`.

Patch `GraphQueryService` to use the router decision directly:

```java
ReadSourceRouter.ReadSourceDecision decision = readSourceRouter.decide(sceneId, snapshotId);
DataMapGraphQueryOptions options = DataMapGraphQueryOptions.of(
        decision.snapshotId(),
        objectTypes,
        statuses,
        relationTypes,
        sensitivityScopes
);

if (decision.readSource() == ReadSource.NEO4J) {
    try {
        Neo4jGraphResult neo4jResult = neo4jGraphReadService.readGraph(sceneId, decision.snapshotId(), options);
        return new DataMapGraphResponseDTO(
                neo4jResult.rootRef(),
                neo4jResult.sceneId(),
                neo4jResult.sceneName(),
                decision.snapshotId(),
                ReadSource.NEO4J,
                decision.verificationStatus(),
                decision.verifiedAt(),
                neo4jResult.nodes(),
                neo4jResult.edges()
        );
    } catch (Exception ex) {
        log.warn("Neo4j read failed for scene={}, snapshot={}, falling back: {}", sceneId, decision.snapshotId(), ex.getMessage());
    }
}
```

Then update `queryFromRelational()` so fallback uses the same snapshot decision:

```java
GraphSceneBundle bundle = graphReadService.loadBundle(rootType, rootId, decision.snapshotId());
DataMapGraphResponseDTO relational = dataMapGraphDtoAdapter.buildGraph(normalizeRootRef(rootType, rootId), bundle, options);
return new DataMapGraphResponseDTO(
        relational.rootRef(),
        relational.sceneId(),
        relational.sceneName(),
        decision.snapshotId(),
        ReadSource.RELATIONAL,
        decision.verificationStatus(),
        decision.verifiedAt(),
        relational.nodes(),
        relational.edges()
);
```

Finally, keep `Neo4jGraphReadService` on the shared `Driver` bean from `Neo4jDriverConfig`; do not create a new driver per request, and ensure the Cypher query keys on both `sceneId` and `snapshotId`.

- [ ] **Step 4: Re-run routing and end-to-end tests**

Run:

- `cd backend && mvn -q -Dtest=GraphQueryServiceTest,MvpKnowledgeGraphFlowIntegrationTest test`

Expected:

- `GraphQueryServiceTest` passes.
- `MvpKnowledgeGraphFlowIntegrationTest` now passes the new top-level graph metadata assertions on the fallback path.

### Task 5: Carry the same graph-read contract through impact analysis and the frontend adapter

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/ImpactAnalysisService.java`
- Modify: `frontend/src/pages/datamap-adapter.js`
- Modify: `frontend/src/pages/datamap-adapter.test.js`
- Modify: `frontend/src/types/dataMap.ts`

- [ ] **Step 1: Preserve graph metadata in impact analysis responses**

Update the `ImpactAnalysisService` graph construction so it does not discard the read metadata:

```java
return new DataMapImpactAnalysisDTO(
        assetRef,
        riskLevel(rootNode, affectedAssets.size()),
        recommendedActions(rootNode),
        affectedAssets,
        new DataMapGraphResponseDTO(
                assetRef,
                fullGraph.sceneId(),
                fullGraph.sceneName(),
                fullGraph.snapshotId(),
                fullGraph.readSource(),
                fullGraph.projectionVerificationStatus(),
                fullGraph.projectionVerifiedAt(),
                impactedNodes,
                impactedEdges
        )
);
```

- [ ] **Step 2: Normalize the new fields on the frontend**

Extend `normalizeLineageGraph()` by inserting these bindings above the existing returned object construction:

```javascript
const readSource = `${payload?.readSource || "RELATIONAL"}`.trim().toUpperCase();
const projectionVerificationStatus = `${payload?.projectionVerificationStatus || "PENDING"}`
  .trim()
  .toUpperCase();
const projectionVerifiedAt = `${payload?.projectionVerifiedAt || ""}`.trim() || undefined;
```

Then insert these three properties into the existing returned object immediately before `nodes:`:

```javascript
readSource,
projectionVerificationStatus,
projectionVerifiedAt,
```

Update the shared type in `frontend/src/types/dataMap.ts`:

```ts
export interface LineageGraphData {
  rootNodeId?: string;
  sceneId?: number;
  sceneName?: string;
  readSource?: "NEO4J" | "RELATIONAL" | string;
  projectionVerificationStatus?: "PASSED" | "FAILED" | "PENDING" | "SKIPPED" | string;
  projectionVerifiedAt?: string;
  nodes: DataMapGraphNode[];
  edges: DataMapGraphEdge[];
  truncated?: boolean;
  hiddenNodeCount?: number;
}
```

- [ ] **Step 3: Re-run the frontend adapter tests**

Run:

- `cd frontend && npm test -- src/pages/datamap-adapter.test.js`

Expected:

- PASS, with graph metadata normalized both for direct graph fetches and impact-analysis graph payloads.

### Task 6: Record the verification evidence and hand implementation over cleanly

**Files:**

- Create: `docs/testing/features/iteration-02-runtime-and-governance/10-data-map-neo4j-phase1-test-report.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: Run the focused backend regression suite**

Run:

- `cd backend && mvn -q -Dtest=ProjectionValidationServiceTest,ReadSourceRouterTest,GraphQueryServiceTest,MvpKnowledgeGraphFlowIntegrationTest test`

Expected:

- PASS, proving:
  - publish writes `sceneId + snapshotId` projection state,
  - projection verification produces `PASSED / FAILED / SKIPPED`,
  - data-map reads fall back cleanly when the gate is not `PASSED`,
  - impact analysis reuses the same graph metadata contract.

- [ ] **Step 2: Run the focused frontend regression suite**

Run:

- `cd frontend && npm test -- src/pages/datamap-adapter.test.js src/pages/WorkbenchContextPages.test.jsx`

Expected:

- PASS, with no regression to the existing data-map route rendering.

- [ ] **Step 3: Write the feature test report skeleton and update delivery status**

Create `docs/testing/features/iteration-02-runtime-and-governance/10-data-map-neo4j-phase1-test-report.md` with this starting structure:

```markdown
# 数据地图 Neo4j Phase 1 测试与验收报告

## 范围

- `Data Map Graph API` 在 `PASSED` 快照上可读 `Neo4j`
- `Data Map Graph API` 在 `FAILED / PENDING / SKIPPED` 快照上回退关系库
- `Impact Analysis API` 继承相同的图读元数据
- `/api/graphrag/query` 与 `Knowledge Package` 主路径未切换

## 执行记录

- `cd backend && mvn -q -Dtest=ProjectionValidationServiceTest,ReadSourceRouterTest,GraphQueryServiceTest,MvpKnowledgeGraphFlowIntegrationTest test`
- `cd frontend && npm test -- src/pages/datamap-adapter.test.js src/pages/WorkbenchContextPages.test.jsx`

## 结果

- Backend：记录 `ProjectionValidationServiceTest / ReadSourceRouterTest / GraphQueryServiceTest / MvpKnowledgeGraphFlowIntegrationTest` 的最终结果与时间戳
- Frontend：记录 `datamap-adapter.test.js / WorkbenchContextPages.test.jsx` 的最终结果与时间戳
- 风险说明：如果 `PASSED` 快照仍触发回退，在此记录对应 `sceneId + snapshotId`
```

Then update the `Neo4j` row in `docs/engineering/current-delivery-status.md`:

```markdown
- 当前状态：维持 `planning（计划中）`
- 最新完成：更新为“已完成实施计划编写，并经终端 `Claude Code` 复核通过”
- 下一动作：更新为“按计划执行 Task 1，从契约红测进入 `TDD（测试驱动开发，Test-Driven Development）`”
- 最后更新时间：更新为 `2026-03-30`
```

This closes the planning loop and leaves the repo in the state expected by `AGENTS.md`.

---

## Self-Review

- 设计范围已完整映射到任务：只改 `Data Map Graph API` 与 `Impact Analysis API`，不碰 `/api/graphrag/query` 与 `Knowledge Package`。
- 所有任务都以测试或校验先行，没有跳过 `TDD（测试驱动开发，Test-Driven Development）`。
- API 字段名统一采用现有 camelCase 契约，避免与现有 `rootRef / sceneId / sceneName` 风格冲突。
