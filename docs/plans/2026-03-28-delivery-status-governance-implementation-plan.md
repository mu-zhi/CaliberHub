# Delivery Status Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish a repository-level `Delivery Status（交付状态）` governance baseline so current progress and next-stage work have a single canonical entry and mandatory update gates.

**Architecture:** Add the governance principle to `docs/engineering/standards/overall-principles.md`, define workflow gates and field contracts in `docs/engineering/collaboration-workflow.md` and a dedicated standards file, create `docs/engineering/current-delivery-status.md` as the only rolling status source, and expose the entry from repository navigation docs without polluting architecture design documents.

**Tech Stack:** Markdown, repository documentation structure, ripgrep

---

## Task 1: Write the Implementation Plan File

**Files:**

- Create: `docs/plans/2026-03-28-delivery-status-governance-implementation-plan.md`

- [ ] **Step 1: Write the plan file**

Create this file with the approved layering model, target files, update-gate rules, and verification commands for the delivery-status governance change.

- [ ] **Step 2: Verify the plan file exists**

Run: `test -f 'docs/plans/2026-03-28-delivery-status-governance-implementation-plan.md'`

Expected: exit code `0`

## Task 2: Define the Principle Layer and Terminology

**Files:**

- Modify: `docs/glossary.md`
- Modify: `docs/engineering/standards/overall-principles.md`
- Create: `docs/engineering/standards/delivery-status-contract.md`

- [ ] **Step 1: Add the required glossary terms**

Add `Delivery Status（交付状态）`, `Single Source of Truth（唯一真源）`, and `Handoff Contract（任务接力契约）` to the glossary so the new governance text does not introduce undefined English terms.

- [ ] **Step 2: Update the highest-principles document**

Rewrite `docs/engineering/standards/overall-principles.md` so the first principle declares `docs/engineering/current-delivery-status.md` as the only rolling source for current progress and next-stage work, while keeping detailed field rules delegated to lower-level governance docs.

- [ ] **Step 3: Write the delivery-status contract**

Create `docs/engineering/standards/delivery-status-contract.md` with the scope boundary, required fields, status enum, allowed state transitions, update timing, and prohibited practices.

- [ ] **Step 4: Verify the principle-layer references**

Run:

```bash
rg -n "current-delivery-status|delivery-status-contract|交付状态|唯一真源" \
  docs/glossary.md \
  docs/engineering/standards/overall-principles.md \
  docs/engineering/standards/delivery-status-contract.md
```

Expected: all three files contain the new governance terms and paths

## Task 3: Define the Workflow Gate and Status Source

**Files:**

- Modify: `docs/engineering/collaboration-workflow.md`
- Create: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: Add workflow update gates**

Update `docs/engineering/collaboration-workflow.md` so design confirmation, plan landing, implementation progress, blocking events, and task handoff all require synchronizing `docs/engineering/current-delivery-status.md`.

- [ ] **Step 2: Create the canonical status document**

Create `docs/engineering/current-delivery-status.md` with fixed sections for repository overview, in-progress work, next-stage work, near-term acceptance, and major risks, plus an initial bootstrap entry for this governance rollout.

- [ ] **Step 3: Verify the workflow layer**

Run:

```bash
test -f 'docs/engineering/current-delivery-status.md' && \
rg -n "current-delivery-status|交付状态真源|任务交接" docs/engineering/collaboration-workflow.md docs/engineering/current-delivery-status.md
```

Expected: exit code `0`, and both files show the new status-governance language

## Task 4: Sync Navigation and Shared Collaboration Entry Docs

**Files:**

- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/README.md`
- Modify: `docs/plans/README.md`

- [ ] **Step 1: Update repository navigation**

Add `docs/engineering/current-delivery-status.md` to `README.md` and `docs/README.md` so the status entry is visible from both repository and docs landing pages.

- [ ] **Step 2: Update shared collaboration rules**

Update `AGENTS.md` so the root collaboration contract states that current progress and next-stage work live only in `docs/engineering/current-delivery-status.md`, and that certain workflow milestones require synchronization.

- [ ] **Step 3: Clarify the boundary of `docs/plans/`**

Update `docs/plans/README.md` so implementation plans remain process artifacts and do not become a parallel place to maintain rolling project status.

- [ ] **Step 4: Run final consistency verification**

Run:

```bash
rg -n "current-delivery-status|delivery-status-contract" \
  README.md AGENTS.md docs/README.md docs/plans/README.md \
  docs/engineering/collaboration-workflow.md \
  docs/engineering/standards/overall-principles.md \
  docs/engineering/standards/delivery-status-contract.md \
  docs/engineering/current-delivery-status.md
```

Expected: all required entry points and governance docs reference the same canonical status path
