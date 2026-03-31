# Workbench Context Package Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first production implementation of `Workbench Context Package（工作台上下文包）` so formal cross-workbench jumps carry validated context instead of dropping the current focus object.

**Architecture:** Introduce one shared frontend context-package helper that serializes, parses, and validates route-carried workbench context through a single `ctx` query payload. Receiver pages consume the parsed package through small pure bootstrap helpers so lock-mode banners, structured errors, readonly behavior, and target-page prefills stay testable without pushing context through global store or `localStorage`.

**Tech Stack:** React Router, React, Vitest, existing UI components, query-string transport, TypeScript helper modules, Vite quality checks

---

## File Map

- Create: `frontend/src/navigation/workbenchContext.ts`
- Create: `frontend/src/navigation/workbenchContext.test.js`
- Create: `frontend/src/navigation/workbenchContextReceivers.ts`
- Create: `frontend/src/navigation/workbenchContextReceivers.test.js`
- Modify: `frontend/src/components/datamap/DataMapContainer.jsx`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/ApprovalExportPage.jsx`
- Modify: `frontend/src/pages/MonitoringAuditPage.jsx`
- Modify: `frontend/src/pages/PublishCenterPage.jsx`
- Modify: `frontend/src/pages/WorkbenchPages.render.test.jsx`
- Modify: `docs/architecture/frontend-workbench-design.md`

## Scope

- This batch implements the documented chains that already have clear sender or receiver surfaces in the current UI:
  - `数据地图 -> 运行决策台`
  - `运行决策台 -> 审批与导出`
  - `运行决策台 -> 数据地图`
  - `发布中心 -> 数据地图`
  - `监控与审计 -> 运行决策台`
  - `监控与审计 -> 数据地图`
- This batch does not add global-store transport and does not keep full objects in memory.
- This batch leaves `plan_code` / `edge_id` / `candidate_path_id` / `relation_type` auto-focus as follow-up if existing page data is insufficient; the first implementation must still validate them and preserve them in the package.

### Task 1: Add failing tests for context serialization and validation

**Files:**

- Create: `frontend/src/navigation/workbenchContext.test.js`
- Create: `frontend/src/navigation/workbenchContext.ts`

- [ ] **Step 1: Write the failing codec and validation tests**

Create `frontend/src/navigation/workbenchContext.test.js`:

```js
import { describe, expect, it } from "vitest";
import {
  buildWorkbenchHref,
  parseWorkbenchContextFromSearch,
  validateWorkbenchContext,
} from "./workbenchContext";

describe("workbench context package", () => {
  it("serializes context into one ctx query payload and parses it back", () => {
    const href = buildWorkbenchHref("/runtime", {
      source_workbench: "map",
      target_workbench: "runtime",
      intent: "run_query",
      scene_code: "SCN_PAYROLL_DETAIL",
      asset_ref: "plan:payroll_detail",
      lock_mode: "latest",
    });

    expect(href).toMatch(/^\\/runtime\\?ctx=/);

    const parsed = parseWorkbenchContextFromSearch(href.split("?")[1]);
    expect(parsed).toMatchObject({
      source_workbench: "map",
      target_workbench: "runtime",
      intent: "run_query",
      scene_code: "SCN_PAYROLL_DETAIL",
      asset_ref: "plan:payroll_detail",
      lock_mode: "latest",
    });
  });

  it("rejects replay or frozen packages when snapshot pair is incomplete", () => {
    const result = validateWorkbenchContext({
      source_workbench: "monitoring",
      target_workbench: "runtime",
      intent: "replay_trace",
      trace_id: "trace_runtime_20260327_07",
      snapshot_id: "snapshot-20260327-01",
      lock_mode: "replay",
    });

    expect(result.ok).toBe(false);
    expect(result.message).toContain("snapshot_id");
    expect(result.message).toContain("inference_snapshot_id");
  });
});
```

- [ ] **Step 2: Run the test to verify red**

Run: `cd frontend && npm test -- src/navigation/workbenchContext.test.js`

Expected: FAIL because `./workbenchContext` does not exist yet.

### Task 2: Add failing tests for receiver-page bootstrap behavior

**Files:**

- Create: `frontend/src/navigation/workbenchContextReceivers.test.js`
- Create: `frontend/src/navigation/workbenchContextReceivers.ts`

- [ ] **Step 1: Write the failing receiver bootstrap tests**

Create `frontend/src/navigation/workbenchContextReceivers.test.js`:

```js
import { describe, expect, it } from "vitest";
import {
  resolveApprovalContextState,
  resolveDataMapContextState,
  resolveRuntimeContextState,
} from "./workbenchContextReceivers";

describe("workbench context receiver state", () => {
  it("marks replay runtime context as readonly and exposes a banner", () => {
    const state = resolveRuntimeContextState({
      source_workbench: "monitoring",
      target_workbench: "runtime",
      intent: "replay_trace",
      trace_id: "trace_runtime_20260327_07",
      snapshot_id: "snapshot-20260327-01",
      inference_snapshot_id: "inference-20260327-01",
      lock_mode: "replay",
    });

    expect(state.readOnly).toBe(true);
    expect(state.banner?.title).toBe("历史回放态");
  });

  it("builds frozen approval context summary from runtime output", () => {
    const state = resolveApprovalContextState({
      source_workbench: "runtime",
      target_workbench: "approval",
      intent: "submit_approval",
      trace_id: "trace_runtime_20260327_07",
      scene_code: "SCN_PAYROLL_DETAIL",
      plan_code: "PLAN_PAYROLL_DETAIL",
      snapshot_id: "snapshot-20260327-01",
      inference_snapshot_id: "inference-20260327-01",
      requested_fields: ["协议号", "交易日期", "金额"],
      purpose: "工单核验",
      lock_mode: "frozen",
    });

    expect(state.banner?.title).toBe("版本冻结态");
    expect(state.summary.traceId).toBe("trace_runtime_20260327_07");
    expect(state.summary.requestedFields).toEqual(["协议号", "交易日期", "金额"]);
  });

  it("treats replay map context as locked and pre-fills snapshot filters", () => {
    const state = resolveDataMapContextState({
      source_workbench: "monitoring",
      target_workbench: "map",
      intent: "view_node",
      asset_ref: "plan:payroll_detail",
      snapshot_id: "42",
      inference_snapshot_id: "108",
      lock_mode: "replay",
    });

    expect(state.banner?.title).toBe("历史回放态");
    expect(state.snapshotId).toBe("42");
    expect(state.readOnly).toBe(true);
  });
});
```

- [ ] **Step 2: Run the tests to verify red**

Run: `cd frontend && npm test -- src/navigation/workbenchContextReceivers.test.js`

Expected: FAIL because `./workbenchContextReceivers` does not exist yet.

### Task 3: Implement the shared context-package helpers

**Files:**

- Create: `frontend/src/navigation/workbenchContext.ts`
- Create: `frontend/src/navigation/workbenchContextReceivers.ts`

- [ ] **Step 1: Implement the transport and validation helper**

Create `frontend/src/navigation/workbenchContext.ts` with a single transport shape:

```ts
export const WORKBENCH_CONTEXT_QUERY_KEY = "ctx";

export function buildWorkbenchHref(targetPath: string, context: WorkbenchContextInput) {
  const payload = encodeURIComponent(JSON.stringify(context));
  return `${targetPath}?${WORKBENCH_CONTEXT_QUERY_KEY}=${payload}`;
}

export function parseWorkbenchContextFromSearch(search: string) {
  const params = new URLSearchParams(search.startsWith("?") ? search.slice(1) : search);
  const encoded = params.get(WORKBENCH_CONTEXT_QUERY_KEY);
  if (!encoded) {
    return null;
  }
  return JSON.parse(decodeURIComponent(encoded));
}

export function validateWorkbenchContext(input: unknown) {
  // validate source_workbench, target_workbench, intent, lock_mode,
  // and enforce snapshot pair for replay/frozen.
}
```

- [ ] **Step 2: Implement receiver bootstrap helpers as pure functions**

Create `frontend/src/navigation/workbenchContextReceivers.ts` with three exported resolvers:

```ts
export function resolveRuntimeContextState(context: WorkbenchContext | null) {
  if (!context) {
    return { readOnly: false, banner: null, sceneCode: "", planCode: "", requestedFields: [], purpose: "" };
  }
  return {
    readOnly: context.lock_mode === "replay" || context.lock_mode === "frozen",
    banner: buildLockBanner(context.lock_mode),
    sceneCode: context.scene_code || "",
    planCode: context.plan_code || "",
    requestedFields: Array.isArray(context.requested_fields) ? context.requested_fields : [],
    purpose: context.purpose || "",
    traceId: context.trace_id || "",
  };
}
```

Use the same module to expose `resolveApprovalContextState` and `resolveDataMapContextState`, keeping page logic out of component bodies.

- [ ] **Step 3: Run the focused tests to verify green**

Run:

- `cd frontend && npm test -- src/navigation/workbenchContext.test.js src/navigation/workbenchContextReceivers.test.js`
- `cd frontend && npm run type-check`

Expected: PASS.

### Task 4: Wire sender links and receiver pages

**Files:**

- Modify: `frontend/src/components/datamap/DataMapContainer.jsx`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/ApprovalExportPage.jsx`
- Modify: `frontend/src/pages/MonitoringAuditPage.jsx`
- Modify: `frontend/src/pages/PublishCenterPage.jsx`

- [ ] **Step 1: Replace sender-side bare links with context-package hrefs**

Update the cross-workbench CTAs so they no longer link directly without context:

```jsx
<UiButton
  as={Link}
  to={buildWorkbenchHref("/approval", {
    source_workbench: "runtime",
    target_workbench: "approval",
    intent: "submit_approval",
    trace_id: result.trace?.traceId,
    scene_code: result.scene?.sceneCode,
    plan_code: result.plan?.planCode,
    snapshot_id: result.trace?.snapshotId,
    inference_snapshot_id: result.trace?.inferenceSnapshotId,
    requested_fields: result.contract?.visibleFields || [],
    purpose: form.purpose.trim(),
    lock_mode: "frozen",
  })}
>
  提交审批与导出
</UiButton>
```

Implement the same pattern for:

- `DataMapContainer.jsx`: `map -> runtime`
- `KnowledgePackageWorkbenchPage.jsx`: `runtime -> approval`, `runtime -> map`
- `PublishCenterPage.jsx`: `publish -> map`
- `MonitoringAuditPage.jsx`: `monitoring -> runtime`, `monitoring -> map`

- [ ] **Step 2: Consume runtime context and enforce readonly replay mode**

In `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`:

- parse the package from `useLocation().search`
- validate target `runtime`
- when `scene_code` matches a loaded scene, auto-select it
- when `requested_fields` / `purpose` exist, prefill the form
- when `lock_mode` is `replay`, render a top banner and disable submit/refresh actions that would mutate or recompute

The visible banner shape should be simple and testable:

```jsx
{contextBanner ? (
  <div className={`workbench-context-banner tone-${contextBanner.tone}`} role="status">
    <strong>{contextBanner.title}</strong>
    <p>{contextBanner.message}</p>
  </div>
) : null}
```

- [ ] **Step 3: Consume frozen approval context and render structured error states**

In `frontend/src/pages/ApprovalExportPage.jsx`:

- parse and validate target `approval`
- when validation fails, render `UiInlineError` with a concrete message such as `缺少 snapshot_id / inference_snapshot_id，无法进入版本冻结审批态`
- when validation passes, render the frozen banner plus a summary block showing `trace_id`, `scene_code`, `plan_code`, `requested_fields`, and `purpose`

- [ ] **Step 4: Consume map context and lock snapshot filters for replay/frozen**

In `frontend/src/components/datamap/DataMapContainer.jsx`:

- parse and validate target `map`
- prefill `graphFilters.snapshotId` from context when present
- surface a replay/frozen banner above the workbench content
- render a structured error instead of silently dropping to latest mode if the package is invalid
- keep existing graph refresh available only when `lock_mode === "latest"`

- [ ] **Step 5: Add monitoring sender actions**

In `frontend/src/pages/MonitoringAuditPage.jsx`, add explicit CTA buttons using hardcoded sample trace context:

```jsx
<UiButton
  as={Link}
  to={buildWorkbenchHref("/runtime", {
    source_workbench: "monitoring",
    target_workbench: "runtime",
    intent: "replay_trace",
    trace_id: "trace_runtime_20260327_07",
    snapshot_id: "snapshot-20260327-01",
    inference_snapshot_id: "inference-20260327-01",
    lock_mode: "replay",
  })}
>
  回放到运行决策台
</UiButton>
```

Mirror the same sample context for the `monitoring -> map` button with `asset_ref`.

### Task 5: Verify page rendering and sync implementation notes

**Files:**

- Modify: `frontend/src/pages/WorkbenchPages.render.test.jsx`
- Modify: `docs/architecture/frontend-workbench-design.md`

- [ ] **Step 1: Extend render smoke around the new receivers**

Add a new test block in `frontend/src/pages/WorkbenchPages.render.test.jsx` that mounts receiver pages through `MemoryRouter initialEntries` with a `ctx` query and confirms render does not throw:

```jsx
it("renders runtime and approval pages with workbench context query", () => {
  expect(() => renderToString(
    <MemoryRouter initialEntries={["/runtime?ctx=%7B%22source_workbench%22%3A%22monitoring%22%7D"]}>
      <KnowledgePackageWorkbenchPage />
    </MemoryRouter>,
  )).not.toThrow();
});
```

Use at least one valid runtime context and one valid approval context.

- [ ] **Step 2: Sync one implementation note back to the frontend main doc**

Append a short implementation note to `docs/architecture/frontend-workbench-design.md` stating:

- `ctx` query transport is the current first-phase carrier
- receiver pages must validate `replay` / `frozen` snapshot pairs before loading
- invalid context packages render structured error UI instead of silently degrading

- [ ] **Step 3: Run full verification**

Run:

- `cd frontend && npm test -- src/navigation/workbenchContext.test.js src/navigation/workbenchContextReceivers.test.js src/pages/WorkbenchPages.render.test.jsx`
- `cd frontend && npm run quality`
- `cd backend && mvn -q clean test`
- `curl -I -s http://127.0.0.1:5173/ | sed -n '1,5p'`
- `curl -I -s http://127.0.0.1:8080/v3/api-docs | sed -n '1,5p'`

Expected:

- targeted frontend tests PASS
- frontend quality PASS
- backend tests PASS
- frontend returns `HTTP 200`
- backend OpenAPI endpoint returns `HTTP 200`

- [ ] **Step 4: Run Claude Code review**

Run `./claude-1 -p --dangerously-skip-permissions --tools "Read,Bash,Grep,Glob"` against the modified files in this batch and require either concrete findings or the exact conclusion `无异议`.
