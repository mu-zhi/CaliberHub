# Inference Validation And Chain Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first formal `Inference Rule（推理规则） / Inference Assertion（推理结论） / Inference Chain（推理链）` review loop so knowledge production can block untrusted inference assets before metadata alignment and publishing.

**Architecture:** Keep inference assets in the relational control store as the formal source of truth, project replay-friendly chain detail into graph/read models, and make the knowledge-production frontend consume one explicit review API set. The implementation should enforce "conflict blocks publish, low-confidence stays review-only" as executable gates rather than page-only hints.

**Tech Stack:** Spring Boot, Maven, JUnit 5, React, Vite, Vitest, Markdown test report skeleton

---

## Target Scope

- Formalize the `知识生产台 -> 推理校验` sub-page.
- Persist review actions and block-state transitions for inference assets.
- Expose replayable chain detail and confidence/conflict summaries to frontend consumers.
- Add gate tests that prove unreviewed or conflicting inference assets cannot advance to metadata alignment or publish.

## Preconditions

- Feature doc: `docs/architecture/features/iteration-01-knowledge-production/04-inference-validation-and-chain-review.md`
- Upstream assets already exist from `01`-`03` chain outputs.
- Downstream handoff target is `05-metadata-alignment-and-source-contract.md`.

## Task 1: Freeze the acceptance surface

**Files:**

- Create: `docs/testing/features/iteration-01-knowledge-production/04-inference-validation-and-chain-review-test-report.md`
- Modify: `docs/plans/README.md`

- [ ] **Step 1: Create the test-report skeleton**

Run:

- `test -f docs/testing/features/iteration-01-knowledge-production/04-inference-validation-and-chain-review-test-report.md || true`

Expected:

- The new skeleton documents backend gate tests, frontend page tests, and one end-to-end review path.

- [ ] **Step 2: Verify the feature/plan/test links**

Run:

- `rg -n "04-inference-validation-and-chain-review" docs/architecture/features docs/testing/features docs/plans`

Expected:

- The feature doc, test report, and this plan all resolve with the same topic string.

## Task 2: Add failing backend gate coverage

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportTaskApiIntegrationTest.java`
- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/inference/InferenceReviewGateTest.java`

- [ ] **Step 1: Add failing tests for conflict and low-confidence review states**

Run:

- `cd backend && mvn -q -Dtest=InferenceReviewGateTest,ImportTaskApiIntegrationTest test`

Expected:

- FAIL because conflicting inference assets are not yet blocked by an executable review gate.

- [ ] **Step 2: Lock the expected gate outputs**

Expected outputs to assert:

- conflict assets cannot move to metadata alignment
- low-confidence assets remain reviewable but unpublished
- review actions always capture operator, reason, and result

## Task 3: Implement the review gate and replay query

**Files:**

- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/inference/InferenceReviewAppService.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/inference/InferenceReviewDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ImportTaskController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/imports/ImportWorkflowAppService.java`

- [ ] **Step 1: Add explicit review commands**

Expected outputs:

- confirm
- reject
- hold-for-review
- fetch replay chain detail

- [ ] **Step 2: Re-run backend tests**

Run:

- `cd backend && mvn -q -Dtest=InferenceReviewGateTest,ImportTaskApiIntegrationTest test`

Expected:

- PASS, and no inference asset bypasses the new gate.

## Task 4: Add the frontend review surface

**Files:**

- Create: `frontend/src/pages/inference-review-adapter.js`
- Create: `frontend/src/pages/inference-review-adapter.test.js`
- Modify: `frontend/src/pages/KnowledgePage.jsx`
- Modify: `frontend/src/pages/WorkbenchPages.render.test.jsx`

- [ ] **Step 1: Add failing page tests**

Run:

- `cd frontend && npm test -- src/pages/inference-review-adapter.test.js src/pages/WorkbenchPages.render.test.jsx`

Expected:

- FAIL because the knowledge-production flow does not yet expose an inference-review page or review actions.

- [ ] **Step 2: Implement page wiring**

Expected outputs:

- grouped inference result list
- replay chain panel
- conflict badge
- review action buttons with reason capture

- [ ] **Step 3: Re-run frontend tests**

Run:

- `cd frontend && npm test -- src/pages/inference-review-adapter.test.js src/pages/WorkbenchPages.render.test.jsx`

Expected:

- PASS, and the page renders review states in Chinese labels.

## Task 5: Prove downstream handoff behavior

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MvpKnowledgeGraphFlowIntegrationTest.java`

- [ ] **Step 1: Add a failing handoff test**

Run:

- `cd backend && mvn -q -Dtest=MvpKnowledgeGraphFlowIntegrationTest test`

Expected:

- FAIL because unreviewed inference assets can still flow downstream.

- [ ] **Step 2: Implement the minimal handoff rule**

Expected outputs:

- only reviewed inference assets reach metadata alignment
- blocked assets produce structured reasons

- [ ] **Step 3: Re-run the integration test**

Run:

- `cd backend && mvn -q -Dtest=MvpKnowledgeGraphFlowIntegrationTest test`

Expected:

- PASS, proving the new gate is part of the main production chain.
