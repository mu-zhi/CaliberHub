# Metadata Alignment And Source Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn logic-level output requirements into formal `Source Contract（来源契约）` objects that can prove every required field has a stable physical landing location before publish.

**Architecture:** Extend the current control-store model with explicit field-level alignment records, availability checks, and blocking reasons. The frontend should consume structured alignment states instead of inferring them from free-form review text. Publish and review paths must depend on the structured alignment result.

**Tech Stack:** Spring Boot, Flyway, JPA, Maven, React, Vitest

---

## Target Scope

- Build the `知识生产台 -> 元数据对齐` sub-page and its API surface.
- Persist logical-to-physical field mappings and source availability state.
- Enforce "required outputs must align or block" as a hard backend rule.
- Add regression coverage for source-contract generation and publish blocking.

## Preconditions

- Feature doc: `docs/architecture/features/iteration-01-knowledge-production/05-metadata-alignment-and-source-contract.md`
- Reviewed inference outputs from `04`.
- Existing control assets for `Plan / Output Contract / Coverage Declaration`.

## Task 1: Create the verification scaffold

**Files:**

- Create: `docs/testing/features/iteration-01-knowledge-production/05-metadata-alignment-and-source-contract-test-report.md`

- [ ] **Step 1: Add the test-report skeleton**

Run:

- `test -f docs/testing/features/iteration-01-knowledge-production/05-metadata-alignment-and-source-contract-test-report.md || true`

Expected:

- The new test report lists field-alignment, blocking, and source-contract generation checks.

## Task 2: Add failing field-alignment backend tests

**Files:**

- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/sourcecontract/SourceContractAlignmentServiceTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportTaskApiIntegrationTest.java`

- [ ] **Step 1: Add failing alignment cases**

Run:

- `cd backend && mvn -q -Dtest=SourceContractAlignmentServiceTest,ImportTaskApiIntegrationTest test`

Expected:

- FAIL because required outputs without physical mappings are not yet blocked in one consistent rule.

- [ ] **Step 2: Lock the minimum expected outputs**

Expected outputs:

- aligned
- pending confirmation
- blocked
- gap task required

## Task 3: Persist source-contract alignment results

**Files:**

- Create: `backend/src/main/resources/db/migration/V__TODO_metadata_alignment_assets.sql`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/SourceFieldAlignmentPO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/sourcecontract/SourceContractAlignmentAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/imports/ImportWorkflowAppService.java`

- [ ] **Step 1: Add the migration using the next available Flyway version**

Run:

- `cd backend && mvn -q -DskipTests package`

Expected:

- PASS, and Flyway recognizes the new migration.

- [ ] **Step 2: Implement explicit alignment status writes**

Expected outputs:

- one row per logical field alignment
- source freshness and availability state
- generated `Source Contract` only when required fields are satisfied

- [ ] **Step 3: Re-run targeted backend tests**

Run:

- `cd backend && mvn -q -Dtest=SourceContractAlignmentServiceTest,ImportTaskApiIntegrationTest test`

Expected:

- PASS with deterministic blocking reasons.

## Task 4: Add the metadata-alignment frontend page

**Files:**

- Create: `frontend/src/pages/source-contract-alignment-adapter.js`
- Create: `frontend/src/pages/source-contract-alignment-adapter.test.js`
- Modify: `frontend/src/pages/KnowledgePage.jsx`

- [ ] **Step 1: Add failing UI tests**

Run:

- `cd frontend && npm test -- src/pages/source-contract-alignment-adapter.test.js`

Expected:

- FAIL because the page does not yet expose left-side logical demand and right-side physical landing states.

- [ ] **Step 2: Implement the page and status rendering**

Expected outputs:

- logical field list
- physical field candidates
- freshness and availability indicators
- structured block reasons

- [ ] **Step 3: Re-run frontend tests**

Run:

- `cd frontend && npm test -- src/pages/source-contract-alignment-adapter.test.js`

Expected:

- PASS, with clear aligned/pending/blocked states.

## Task 5: Prove publish blocking and gap-task handoff

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/M2M3ApiIntegrationTest.java`

- [ ] **Step 1: Add a failing publish gate case**

Run:

- `cd backend && mvn -q -Dtest=M2M3ApiIntegrationTest test`

Expected:

- FAIL because incomplete required field alignment can still reach publish preparation.

- [ ] **Step 2: Implement the minimal block**

Expected outputs:

- incomplete required outputs block publish
- missing mappings create or link a gap task

- [ ] **Step 3: Re-run the publish test**

Run:

- `cd backend && mvn -q -Dtest=M2M3ApiIntegrationTest test`

Expected:

- PASS, proving source-contract alignment is a formal publish dependency.
