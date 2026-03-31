# Operational Readiness And Release Assurance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make launch readiness, replay-based acceptance, rollback triggers, and post-rollback verification executable release gates instead of scattered release notes.

**Architecture:** Keep publish execution inside the publish center, but move "is this version releasable?" into one structured gate pipeline backed by replay sets, NFR checks, alert rules, and rollback criteria. Monitoring and audit pages consume the same structured gate data for runbook and rollback verification.

**Tech Stack:** Spring Boot, Maven, shell gate scripts, Markdown runbook/test docs

---

## Target Scope

- Feature doc: `docs/architecture/features/iteration-02-runtime-and-governance/11b-operational-readiness-and-release-assurance.md`
- Create the missing formal implementation plan for this passed feature doc.
- Focus on release qualification and rollback verification, not publish-button UI polish.

## Task 1: Freeze the readiness artifacts

**Files:**

- Create: `docs/testing/features/iteration-02-runtime-and-governance/11b-operational-readiness-and-release-assurance-test-report.md`
- Create: `scripts/run_release_readiness_gate.sh`

- [ ] **Step 1: Create the test-report skeleton**

Run:

- `test -f docs/testing/features/iteration-02-runtime-and-governance/11b-operational-readiness-and-release-assurance-test-report.md || true`

Expected:

- The report captures NFR, replay, rollback, and audit-completeness checks.

- [ ] **Step 2: Add the gate script skeleton**

Expected outputs:

- one script that can run the release-readiness bundle end to end

## Task 2: Add failing readiness tests

**Files:**

- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ReleaseReadinessGateTest.java`

- [ ] **Step 1: Add failing qualification assertions**

Run:

- `cd backend && mvn -q -Dtest=ReleaseReadinessGateTest test`

Expected:

- FAIL because readiness is not yet represented as one executable gate summary.

- [ ] **Step 2: Lock the minimum expected outputs**

Expected outputs:

- NFR pass/fail summary
- replay pass/fail summary
- rollback trigger summary
- post-rollback verification summary

## Task 3: Implement the gate summary service

**Files:**

- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/release/ReleaseReadinessAppService.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/release/ReleaseReadinessSummaryDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/PublishController.java`

- [ ] **Step 1: Implement readiness aggregation**

Expected outputs:

- candidate snapshot pair input
- structured blockers
- rollback action requirements

- [ ] **Step 2: Re-run readiness tests**

Run:

- `cd backend && mvn -q -Dtest=ReleaseReadinessGateTest test`

Expected:

- PASS.

## Task 4: Wire replay and rollback checks into scripts

**Files:**

- Modify: `scripts/run_release_readiness_gate.sh`
- Modify: `scripts/start_backend.sh`

- [ ] **Step 1: Add replay and smoke-check commands**

Run:

- `bash scripts/run_release_readiness_gate.sh`

Expected:

- Script exits non-zero when replay, NFR, or rollback prerequisites fail.

- [ ] **Step 2: Verify live service probes**

Run:

- `curl -sSf http://127.0.0.1:8082/v3/api-docs >/dev/null`

Expected:

- exit code `0` while backend is healthy.

## Task 5: Sync monitoring/audit handoff and status

**Files:**

- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: Record the new gate**

Expected output:

- delivery status names the release-readiness gate, test report, and rollback verification path.
