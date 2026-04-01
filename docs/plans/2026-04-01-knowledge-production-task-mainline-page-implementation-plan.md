# Knowledge Production Task Mainline Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor `知识生产台 / 材料接入与解析` from a four-step mixed page into a current-import-task workbench with a mainline task area and a secondary results area.

**Architecture:** Keep the existing backend import task, quality confirmation, compare confirmation, and draft publish flow unchanged. Reorganize the frontend around one derived `task mainline state`, move scene queue / compare / graph / import detail into a secondary results zone, and demote `01-04` to a status-summary layer with explicit impact warnings on backtracking.

**Tech Stack:** React, React Router, Vitest, existing `KnowledgePage` and `knowledge-import-utils`

---

## Target Scope

- Restructure `/production/ingest` to center `当前导入任务 -> 下一待办 -> 推荐处理对象`.
- Add a derived mainline-state adapter for the import workbench.
- Demote the current `01-04` cards into summary / details behavior instead of co-equal main content.
- Gate cross-workbench links behind real task context.
- Preserve current backend APIs and route semantics.

## Preconditions

- Design source: `docs/architecture/frontend-workbench-design.md` §4.3.1.
- Existing page: `frontend/src/pages/KnowledgePage.jsx`.
- Existing helpers/tests: `frontend/src/pages/knowledge-import-utils.js`, `frontend/src/pages/KnowledgePage.test.jsx`, `frontend/src/pages/KnowledgePage.render.test.jsx`.

## Task 1: Lock the frontend contract with failing tests

**Files:**

- Create: `frontend/src/pages/knowledge-task-mainline.test.js`
- Modify: `frontend/src/pages/KnowledgePage.render.test.jsx`
- Modify: `frontend/src/pages/KnowledgePage.test.jsx`

- [ ] **Step 1: Add the derived task-state test file**

```js
import { describe, expect, it } from "vitest";
import {
  deriveKnowledgeTaskMainlineState,
  shouldShowDataMapLink,
  shouldShowRuntimeLink,
} from "./knowledge-task-mainline";

describe("deriveKnowledgeTaskMainlineState", () => {
  it("returns waiting_import when no task and no queue exist", () => {
    const state = deriveKnowledgeTaskMainlineState({
      importTaskId: "",
      qualityConfirmed: false,
      compareConfirmed: false,
      queueStats: { draftCount: 0, publishedCount: 0, discardedCount: 0, lowConfidenceCount: 0 },
      currentDraft: {},
    });

    expect(state.kind).toBe("waiting_import");
    expect(state.primaryActionLabel).toBe("导入并生成草稿");
  });

  it("returns waiting_publish_prep when compare is confirmed and draft queue remains", () => {
    const state = deriveKnowledgeTaskMainlineState({
      importTaskId: "task-1",
      qualityConfirmed: true,
      compareConfirmed: true,
      queueStats: { draftCount: 3, publishedCount: 0, discardedCount: 0, lowConfidenceCount: 1 },
      currentDraft: { sceneTitle: "", sceneId: 12 },
    });

    expect(state.kind).toBe("waiting_publish_prep");
    expect(state.blockers).toContain("仍有场景待补齐业务名称或业务字段");
    expect(state.primaryActionLabel).toBe("继续处理当前场景");
  });
});

describe("cross workbench link gating", () => {
  it("shows data map only when a browseable scene exists", () => {
    expect(shouldShowDataMapLink({ queueStats: { publishedCount: 0 }, selectedScene: null })).toBe(false);
    expect(shouldShowDataMapLink({ queueStats: { publishedCount: 1 }, selectedScene: { scene_id: 9 } })).toBe(true);
  });

  it("shows runtime only when a verifiable scene exists", () => {
    expect(shouldShowRuntimeLink({ queueStats: { publishedCount: 0 }, currentDraft: {} })).toBe(false);
    expect(shouldShowRuntimeLink({ queueStats: { publishedCount: 1 }, currentDraft: { sceneId: 9 } })).toBe(true);
  });
});
```

- [ ] **Step 2: Run the new tests and confirm they fail**

Run: `cd frontend && npm test -- src/pages/knowledge-task-mainline.test.js`

Expected: FAIL because `knowledge-task-mainline.js` does not exist and the mainline-state API is not implemented.

- [ ] **Step 3: Add render expectations for the new page skeleton**

```jsx
it("renders the import page as a current task workbench", () => {
  const html = renderKnowledgePage("import");

  expect(html).toContain("当前导入任务");
  expect(html).toContain("当前待办");
  expect(html).toContain("场景队列");
  expect(html).toContain("导入明细");
});
```

- [ ] **Step 4: Run the render tests and confirm they fail**

Run: `cd frontend && npm test -- src/pages/KnowledgePage.render.test.jsx src/pages/KnowledgePage.test.jsx`

Expected: FAIL because the page still renders the four-step mixed layout and lacks the new current-task labels.

## Task 2: Implement the mainline-state adapter

**Files:**

- Create: `frontend/src/pages/knowledge-task-mainline.js`
- Modify: `frontend/src/pages/knowledge-import-utils.js`

- [ ] **Step 1: Add the minimal adapter implementation**

```js
export function deriveKnowledgeTaskMainlineState({
  importTaskId,
  qualityConfirmed,
  compareConfirmed,
  queueStats,
  currentDraft,
}) {
  if (!importTaskId && Number(queueStats?.draftCount || 0) === 0) {
    return {
      kind: "waiting_import",
      title: "当前还没有有效导入任务",
      primaryActionLabel: "导入并生成草稿",
      blockers: [],
    };
  }

  if (!qualityConfirmed) {
    return {
      kind: "waiting_quality",
      title: "当前任务停留在抽取质量判断",
      primaryActionLabel: "确认质检，进入对照",
      blockers: Number(queueStats?.lowConfidenceCount || 0) > 0 ? ["仍有低置信度场景待确认"] : [],
    };
  }

  if (!compareConfirmed) {
    return {
      kind: "waiting_compare",
      title: "当前任务停留在原文对照",
      primaryActionLabel: "确认对照，进入发布整理",
      blockers: [],
    };
  }

  return {
    kind: "waiting_publish_prep",
    title: "当前任务停留在场景整理与发布",
    primaryActionLabel: "继续处理当前场景",
    blockers: !currentDraft?.sceneTitle ? ["仍有场景待补齐业务名称或业务字段"] : [],
  };
}

export function shouldShowDataMapLink({ queueStats, selectedScene }) {
  return Boolean(selectedScene?.scene_id || Number(queueStats?.publishedCount || 0) > 0);
}

export function shouldShowRuntimeLink({ queueStats, currentDraft }) {
  return Boolean(currentDraft?.sceneId || Number(queueStats?.publishedCount || 0) > 0);
}
```

- [ ] **Step 2: Re-run the adapter tests**

Run: `cd frontend && npm test -- src/pages/knowledge-task-mainline.test.js`

Expected: PASS, proving the page can derive `waiting_import / waiting_quality / waiting_compare / waiting_publish_prep`.

## Task 3: Recompose `KnowledgePage` into mainline + secondary results

**Files:**

- Modify: `frontend/src/pages/KnowledgePage.jsx`
- Modify: `frontend/src/pages/KnowledgePage.render.test.jsx`
- Modify: `frontend/src/pages/KnowledgePage.test.jsx`

- [ ] **Step 1: Import and wire the mainline adapter**

```js
import {
  deriveKnowledgeTaskMainlineState,
  shouldShowDataMapLink,
  shouldShowRuntimeLink,
} from "./knowledge-task-mainline";
```

```js
const mainlineState = deriveKnowledgeTaskMainlineState({
  importTaskId,
  qualityConfirmed,
  compareConfirmed,
  queueStats,
  currentDraft,
});

const showDataMapLink = shouldShowDataMapLink({ queueStats, selectedScene });
const showRuntimeLink = shouldShowRuntimeLink({ queueStats, currentDraft });
```

- [ ] **Step 2: Replace the top mixed hero with the current-task summary**

```jsx
<section className="task-summary-band">
  <div>
    <p className="eyebrow">当前导入任务</p>
    <h1>{currentTaskTitle}</h1>
    <p className="subtle-note">
      {sourceTypeLabel} · 最近更新 {formatDateTimeLabel(importTaskUpdatedAt)} · 当前阶段 {mainlineState.title}
    </p>
  </div>
  <div className="summary-actions">
    <button className="btn btn-ghost" type="button" onClick={loadImportBestPracticeSample}>查看样例</button>
    {showDataMapLink ? <button className="btn btn-ghost" type="button" onClick={goDataMap}>查看数据地图</button> : null}
    {showRuntimeLink ? <button className="btn btn-ghost" type="button" onClick={goRuntimeWorkbench}>查看运行决策台</button> : null}
  </div>
</section>
```

- [ ] **Step 3: Add the task-mainline work area ahead of the old step cards**

```jsx
<section className="task-mainline-grid">
  <article className="task-mainline-card">
    <p className="eyebrow">当前待办</p>
    <h2>{mainlineState.title}</h2>
    <p>{mainlineState.primaryActionLabel}</p>
    {mainlineState.blockers.length > 0 ? (
      <ul className="blocker-list">
        {mainlineState.blockers.map((item) => <li key={item}>{item}</li>)}
      </ul>
    ) : null}
    <button className="btn btn-primary" type="button" onClick={focusCurrentWorkItem}>
      {mainlineState.primaryActionLabel}
    </button>
  </article>

  <aside className="task-risk-card">
    <h2>风险与状态</h2>
    <ul className="risk-metric-list">
      <li>低置信度 {queueStats.lowConfidenceCount}</li>
      <li>待处理场景 {queueStats.draftCount}</li>
      <li>已发布 {queueStats.publishedCount}</li>
      <li>已弃用 {queueStats.discardedCount}</li>
    </ul>
  </aside>
</section>
```

- [ ] **Step 4: Move queue / compare / graph / import detail into a secondary results section**

```jsx
<section className="secondary-results-zone" aria-label="次级结果区">
  <nav className="secondary-result-tabs" aria-label="结果标签">
    <button type="button">场景队列</button>
    <button type="button">原文对照</button>
    <button type="button">候选图谱</button>
    <button type="button">导入明细</button>
  </nav>
</section>
```

- [ ] **Step 5: Demote `01-04` to summary / detail behavior**

Expected outputs:

- keep `AccordionStepCard` for history and drill-down
- remove the current “four equal hero cards” reading order
- show impact warning before allowing edit on upstream completed steps

- [ ] **Step 6: Re-run the page tests**

Run: `cd frontend && npm test -- src/pages/KnowledgePage.render.test.jsx src/pages/KnowledgePage.test.jsx src/pages/knowledge-task-mainline.test.js`

Expected: PASS, proving the page now exposes `当前导入任务`, `当前待办`, and the secondary results labels.

## Task 4: Add a regression test report and sync docs

**Files:**

- Create: `docs/testing/features/iteration-01-knowledge-production/knowledge-production-task-mainline-page-test-report.md`
- Modify: `docs/architecture/frontend-workbench-design.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: Create the test-report skeleton**

```md
# 知识生产台任务主线页重构测试报告

- 目标：验证知识生产台从四步混合页切换为任务主线页后，主线入口、次级结果区和跨台跳转门禁稳定。
- 覆盖：导入空态、待质检、待对照、待发布整理、上下文驱动的数据地图/运行决策台入口。
- 结果：待执行。
```

- [ ] **Step 2: Run the focused frontend verification**

Run: `cd frontend && npm test -- src/pages/knowledge-task-mainline.test.js src/pages/KnowledgePage.render.test.jsx src/pages/KnowledgePage.test.jsx && npm run build`

Expected: PASS, and the production ingest page still builds.

- [ ] **Step 3: Sync the delivery status entry**

Expected outputs:

- one new doc-sync work item in `docs/engineering/current-delivery-status.md`
- latest completion mentions the mainline-page design and plan landing together

## Self-Review

- Spec coverage: the plan covers task-summary band, task-mainline card, risk/status card, secondary results zone, step demotion, and cross-workbench gating.
- Placeholder scan: no `TODO`, `TBD`, or unnamed files remain.
- Type consistency: adapter names are reused consistently as `deriveKnowledgeTaskMainlineState`, `shouldShowDataMapLink`, and `shouldShowRuntimeLink`.

## Execution Handoff

Plan complete and saved to `docs/plans/2026-04-01-knowledge-production-task-mainline-page-implementation-plan.md`. Two execution options:

1. Subagent-Driven (recommended) - I dispatch a fresh subagent per task, review between tasks, fast iteration
2. Inline Execution - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
