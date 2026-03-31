# Governance Asset Modeling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the first production-grade implementation of `03-asset-modeling-and-governance-editing.md` so formal governance assets can be created, validated, diffed, and persisted from the knowledge-production workbench.

**Architecture:** Reuse the current `Scene / Plan / Output Contract / Contract View / Coverage / Policy` persistence path, then add the still-missing first-class governance assets around dictionary, identifier lineage, and time semantic selector. Keep relational tables as the source of truth, keep graph projection read-only, and make the frontend asset-editing workbench consume structured backend DTOs rather than free-form scene JSON.

**Tech Stack:** Spring Boot, MyBatis/JPA-style mappers already in repo, React, Vitest, Maven, MySQL-compatible Flyway migrations

---

## Design Inputs

- `docs/architecture/features/iteration-01-knowledge-production/03-asset-modeling-and-governance-editing.md`
- `docs/testing/features/iteration-01-knowledge-production/03-asset-modeling-and-governance-editing-test-report.md`
- `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/graphrag/`
- `frontend/src/pages/KnowledgePage.jsx`

## Scope

- Extend the formal governance-asset model to cover the feature doc’s missing first-class objects.
- Add backend validation so draft scenes cannot be marked runnable when required governance objects are absent.
- Add frontend modeling interactions and regression coverage for structured asset editing and diff review.

## Preconditions

- Real MySQL-compatible control-store baseline remains the only write source.
- `docs/testing/features/iteration-01-knowledge-production/03-asset-modeling-and-governance-editing-test-report.md` must be updated before production code.

## File Map

- Create: `backend/src/main/resources/db/migration/V__add_dictionary_identifier_time_semantic_assets.sql`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/graphrag/DictionaryPO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/graphrag/IdentifierLineagePO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/graphrag/TimeSemanticSelectorPO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/graphrag/DictionaryMapper.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/graphrag/IdentifierLineageMapper.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/graphrag/TimeSemanticSelectorMapper.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/SceneCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/SceneDiffDTO.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/SceneCommandAppServiceTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/SceneApiIntegrationTest.java`
- Modify: `frontend/src/pages/KnowledgePage.jsx`
- Modify: `frontend/src/pages/KnowledgePage.test.jsx`
- Modify: `frontend/src/pages/KnowledgePage.render.test.jsx`
- Modify: `docs/testing/features/iteration-01-knowledge-production/03-asset-modeling-and-governance-editing-test-report.md`

### Task 1: Lock the acceptance boundary in the test report

**Files:**

- Modify: `docs/testing/features/iteration-01-knowledge-production/03-asset-modeling-and-governance-editing-test-report.md`

- [ ] Add explicit acceptance rows for `Dictionary`, `Identifier Lineage`, and `Time Semantic Selector`.
- [ ] Record the red-phase commands that should fail before implementation.

Run: `rg -n "Dictionary|Identifier Lineage|Time Semantic Selector" docs/testing/features/iteration-01-knowledge-production/03-asset-modeling-and-governance-editing-test-report.md`

Expected: The report lists all three new first-class assets and their gate conditions.

### Task 2: Add failing backend coverage for missing governance assets

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/SceneCommandAppServiceTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/SceneApiIntegrationTest.java`

- [ ] Add a service-level test that rejects runnable/reviewable drafts when required dictionary, identifier-lineage, or time-semantic assets are absent.
- [ ] Add an API-level test that verifies scene diff output exposes the new governance asset blocks.

Run: `cd backend && mvn -q -Dtest=SceneCommandAppServiceTest,SceneApiIntegrationTest test`

Expected: FAIL with assertions showing the new asset gates and diff fields do not exist yet.

### Task 3: Add relational persistence for the new governance assets

**Files:**

- Create: `backend/src/main/resources/db/migration/V__add_dictionary_identifier_time_semantic_assets.sql`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/graphrag/DictionaryPO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/graphrag/IdentifierLineagePO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/graphrag/TimeSemanticSelectorPO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/graphrag/DictionaryMapper.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/graphrag/IdentifierLineageMapper.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/graphrag/TimeSemanticSelectorMapper.java`

- [ ] Add MySQL-compatible tables and mapper bindings for the three missing assets.
- [ ] Keep all records keyed by scene draft or plan context so later diff and publish checks can reuse them.

Run: `cd backend && mvn -q -Dtest=SceneCommandAppServiceTest test`

Expected: Tests still fail, but failures move from persistence-missing to service/diff behavior gaps.

### Task 4: Wire backend validation and diff output

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/SceneCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/SceneDiffDTO.java`

- [ ] Add minimum-unit validation that enforces the new governance assets before review/publish transitions.
- [ ] Extend diff output so current-draft vs published-version comparisons include the three new asset groups.

Run: `cd backend && mvn -q -Dtest=SceneCommandAppServiceTest,SceneApiIntegrationTest test`

Expected: PASS.

### Task 5: Add frontend modeling and diff coverage

**Files:**

- Modify: `frontend/src/pages/KnowledgePage.jsx`
- Modify: `frontend/src/pages/KnowledgePage.test.jsx`
- Modify: `frontend/src/pages/KnowledgePage.render.test.jsx`

- [ ] Add structured modeling sections for dictionary, identifier-lineage, and time-semantic-selector editing.
- [ ] Surface backend diff blocks in the asset-review area rather than burying them in raw JSON.

Run: `cd frontend && npm test -- src/pages/KnowledgePage.test.jsx src/pages/KnowledgePage.render.test.jsx`

Expected: PASS.

### Task 6: Run the full feature gate

**Files:**

- No additional file changes.

- [ ] Re-run backend and frontend feature checks.
- [ ] Update the test report with final evidence and remaining known gaps.

Run:

- `cd backend && mvn -q -Dtest=SceneCommandAppServiceTest,SceneApiIntegrationTest test`
- `cd frontend && npm test -- src/pages/KnowledgePage.test.jsx src/pages/KnowledgePage.render.test.jsx`

Expected:

- Backend targeted tests PASS.
- Frontend targeted tests PASS.
- Test report reflects the final gate status.
