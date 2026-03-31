# Review And Gap Task Collaboration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the closing loop that lets business, technical, and compliance review states and `Gap Task（缺口任务）` states jointly decide whether a candidate asset set may enter publish checks.

**Architecture:** Model review tasks and gap tasks as first-class control objects with explicit links to assets, evidence, and blocking reasons. The knowledge-production frontend should present them as one coordinated workflow, while publish preparation consumes only structured statuses from this workflow.

**Tech Stack:** Spring Boot, JPA, Maven, React, Vitest

---

## Target Scope

- Add formal review-task and gap-task persistence if still missing or fragmented.
- Build the `知识生产台 -> 复核 / 缺口任务` coordinated UI.
- Enforce "all required reviews pass, all blocking gaps closed" before publish checks.

## Preconditions

- Feature doc: `docs/architecture/features/iteration-01-knowledge-production/06-review-and-gap-task-collaboration.md`
- Upstream outputs from `01`-`05`.
- Downstream consumer is publish preparation.

## Task 1: Add the acceptance scaffold

**Files:**

- Create: `docs/testing/features/iteration-01-knowledge-production/06-review-and-gap-task-collaboration-test-report.md`

- [ ] **Step 1: Create the test-report skeleton**

Run:

- `test -f docs/testing/features/iteration-01-knowledge-production/06-review-and-gap-task-collaboration-test-report.md || true`

Expected:

- The report covers review state transitions, blocking gaps, publish gating, and UI navigation.

## Task 2: Add failing backend coordination tests

**Files:**

- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/review/ReviewGapCoordinationServiceTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/M2M3ApiIntegrationTest.java`

- [ ] **Step 1: Add failing gate tests**

Run:

- `cd backend && mvn -q -Dtest=ReviewGapCoordinationServiceTest,M2M3ApiIntegrationTest test`

Expected:

- FAIL because publish checks are not yet blocked by a unified review/gap closure rule.

- [ ] **Step 2: Lock the required state model**

Expected outputs:

- review types: business, technical, compliance
- gap states: open, assigned, in_progress, resolved, exempted, verified
- one structured blocking summary for publish consumers

## Task 3: Implement review and gap orchestration

**Files:**

- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/review/ReviewGapCoordinationAppService.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/review/ReviewGateSummaryDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/imports/ImportWorkflowAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ImportTaskController.java`

- [ ] **Step 1: Add structured review/gap read and write APIs**

Expected outputs:

- grouped review list
- linked gap list
- reject reason capture
- publish readiness summary

- [ ] **Step 2: Re-run backend tests**

Run:

- `cd backend && mvn -q -Dtest=ReviewGapCoordinationServiceTest,M2M3ApiIntegrationTest test`

Expected:

- PASS, and blocked gaps or missing reviews stop publish entry.

## Task 4: Build the coordinated frontend surface

**Files:**

- Create: `frontend/src/pages/review-gap-adapter.js`
- Create: `frontend/src/pages/review-gap-adapter.test.js`
- Modify: `frontend/src/pages/KnowledgePage.jsx`

- [ ] **Step 1: Add failing page tests**

Run:

- `cd frontend && npm test -- src/pages/review-gap-adapter.test.js`

Expected:

- FAIL because review items and gap tasks are not yet rendered as one coordinated flow.

- [ ] **Step 2: Implement the UI**

Expected outputs:

- grouped review queues
- linked gap list
- structured reject reasons
- jump links between review detail and gap detail

- [ ] **Step 3: Re-run frontend tests**

Run:

- `cd frontend && npm test -- src/pages/review-gap-adapter.test.js`

Expected:

- PASS with deterministic Chinese status labels.

## Task 5: Verify publish handoff

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MvpKnowledgeGraphFlowIntegrationTest.java`

- [ ] **Step 1: Add the end-to-end failing handoff test**

Run:

- `cd backend && mvn -q -Dtest=MvpKnowledgeGraphFlowIntegrationTest test`

Expected:

- FAIL because the production chain does not yet enforce the new closeout contract.

- [ ] **Step 2: Implement the minimal handoff contract**

Expected outputs:

- assets enter publish checks only after three reviews pass and blocking gaps close

- [ ] **Step 3: Re-run the end-to-end test**

Run:

- `cd backend && mvn -q -Dtest=MvpKnowledgeGraphFlowIntegrationTest test`

Expected:

- PASS, proving the new closeout contract is on the main path.
