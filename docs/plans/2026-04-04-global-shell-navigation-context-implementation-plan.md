# 全局壳层、导航与跨工作台上下文跳转 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `12-全局壳层、导航与跨工作台上下文跳转.md` 从“已有部分实现”收口成正式的共享壳层与跨台上下文协议，实现导航可见性、锁定态校验和目标页结构化失败反馈的一致行为。

**Architecture:** 以前端为主，继续复用已存在的 `workbenchContext.ts` 与 `workbenchContextReceivers.ts`；围绕 `App.jsx`、`routes.js`、接收页和渲染测试把顶层导航纪律、置灰规则、锁定态错误提示与自动聚焦行为收口为统一契约。

**Tech Stack:** React Router、React、Vitest、TypeScript helper module、Vite

---

## 设计输入

- `docs/architecture/features/iteration-02-runtime-and-governance/12-全局壳层、导航与跨工作台上下文跳转.md`
- `docs/plans/2026-03-28-workbench-context-package-implementation-plan.md`
- `docs/plans/2026-04-04-feature-doc-coverage-and-gate-audit.md`

## File Map

- Modify: `frontend/src/App.jsx`
- Modify: `frontend/src/routes.js`
- Modify: `frontend/src/navigation/workbenchContext.ts`
- Modify: `frontend/src/navigation/workbenchContextReceivers.ts`
- Modify: `frontend/src/navigation/workbenchContext.test.js`
- Modify: `frontend/src/navigation/workbenchContextReceivers.test.js`
- Modify: `frontend/src/pages/WorkbenchContextPages.test.jsx`
- Modify: `frontend/src/App.access-control.test.jsx`
- Modify: `frontend/src/store/appStore.js`
- Create: `docs/testing/features/iteration-02-runtime-and-governance/12-全局壳层、导航与跨工作台上下文跳转-测试报告.md`
- Modify: `docs/engineering/current-delivery-status.md`

## 前置依赖

- 已有 `TOP_MODULES`、`findRoute`、`isTopModuleAccessible`
- 已有 `workbenchContext` 编解码与 replay / frozen 校验
- 已有 `ApprovalExportPage`、`KnowledgePackageWorkbenchPage`、`MonitoringAuditPage` 的上下文接收能力

### Task 1: 先补共享壳层与上下文协议失败测试

**Files:**

- Modify: `frontend/src/App.access-control.test.jsx`
- Modify: `frontend/src/navigation/workbenchContext.test.js`
- Modify: `frontend/src/navigation/workbenchContextReceivers.test.js`
- Modify: `frontend/src/pages/WorkbenchContextPages.test.jsx`

- [ ] **Step 1: 为顶栏“可见但置灰”补断言**

在 `App.access-control.test.jsx` 增加：

```jsx
it("keeps inaccessible top workbenches visible but disabled", async () => {
  renderAppAsRole("frontline", "/home");
  expect(await screen.findByText("监控与审计")).toHaveAttribute("aria-disabled", "true");
});
```

- [ ] **Step 2: 为上下文协议补锁定态错误测试**

在 `workbenchContext.test.js` 或 `workbenchContextReceivers.test.js` 追加：

```js
it("rejects replay or frozen targets when snapshot pair is missing", () => {
  const result = readValidatedWorkbenchContext("?ctx=...", "runtime");
  expect(result.ok).toBe(false);
  expect(result.message).toContain("snapshot_id");
});
```

- [ ] **Step 3: 为目标页结构化失败补渲染测试**

在 `WorkbenchContextPages.test.jsx` 增加：

```jsx
it("renders structured target-workbench mismatch error", () => {
  const entry = buildWorkbenchHref("/approval", {
    source_workbench: "runtime",
    target_workbench: "map",
    intent: "submit_approval",
    lock_mode: "latest",
  });
  const html = renderPage(entry, <ApprovalExportPage />);
  expect(html).toContain("target_workbench");
});
```

- [ ] **Step 4: 运行测试验证 Red**

Run:

- `cd frontend && npm test -- src/App.access-control.test.jsx src/navigation/workbenchContext.test.js src/navigation/workbenchContextReceivers.test.js src/pages/WorkbenchContextPages.test.jsx`

Expected:

- FAIL，说明当前顶栏显隐、上下文错误提示或目标页兜底仍不完整

### Task 2: 收口全局壳层与导航纪律

**Files:**

- Modify: `frontend/src/App.jsx`
- Modify: `frontend/src/routes.js`
- Modify: `frontend/src/store/appStore.js`

- [ ] **Step 1: 固化一级导航可见性和置灰行为**

在 `App.jsx` 中保持顶栏始终渲染 7 个工作台，并对无权限项统一输出：

```jsx
<span className="mini-link top-link is-disabled" aria-disabled="true" title="当前角色无权限进入该工作台">
  {item.label}
</span>
```

- [ ] **Step 2: 固化左侧二级导航折叠态**

在 `appStore.js` 中保留统一键：

```js
const NAV_COLLAPSED_KEY = "dd_nav_collapsed";
```

并确保 `App.jsx` 只通过 `navCollapsed` 管理二级导航折叠，不在页面内引入新的局部存储键。

- [ ] **Step 3: 运行导航回归测试**

Run:

- `cd frontend && npm test -- src/App.access-control.test.jsx`

Expected:

- PASS
- 顶部 7 个工作台可见性与置灰逻辑稳定

### Task 3: 收口上下文包协议和接收页兜底

**Files:**

- Modify: `frontend/src/navigation/workbenchContext.ts`
- Modify: `frontend/src/navigation/workbenchContextReceivers.ts`
- Modify: `frontend/src/navigation/workbenchContext.test.js`
- Modify: `frontend/src/navigation/workbenchContextReceivers.test.js`
- Modify: `frontend/src/pages/WorkbenchContextPages.test.jsx`

- [ ] **Step 1: 强化目标工作台校验**

在 `readValidatedWorkbenchContext` 保持目标页强校验：

```ts
if (targetWorkbench && validated.context.target_workbench !== targetWorkbench) {
  return { ok: false, context: null, message: `target_workbench 必须为 ${targetWorkbench}` };
}
```

- [ ] **Step 2: 统一 replay / frozen 结构化错误**

在 receiver resolver 中统一返回：

```ts
{
  banner: { title: "历史回放态", tone: "warn" },
  readOnly: true,
  error: "snapshot_id 与 inference_snapshot_id 必须成对出现"
}
```

- [ ] **Step 3: 运行上下文链路回归**

Run:

- `cd frontend && npm test -- src/navigation/workbenchContext.test.js src/navigation/workbenchContextReceivers.test.js src/pages/WorkbenchContextPages.test.jsx`
- `cd frontend && npm run build`

Expected:

- PASS
- `/runtime`、`/approval`、`/map`、`/monitoring` 的 `ctx` 链路渲染稳定

### Task 4: 收口测试报告与状态真源

**Files:**

- Create: `docs/testing/features/iteration-02-runtime-and-governance/12-全局壳层、导航与跨工作台上下文跳转-测试报告.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 新增测试报告骨架**

测试文档至少记录：

```md
- 对应特性文档：`docs/architecture/features/iteration-02-runtime-and-governance/12-全局壳层、导航与跨工作台上下文跳转.md`
- 对应实施计划：`docs/plans/2026-04-04-global-shell-navigation-context-implementation-plan.md`
- 验证命令：
  - `cd frontend && npm test -- src/App.access-control.test.jsx src/navigation/workbenchContext.test.js src/navigation/workbenchContextReceivers.test.js src/pages/WorkbenchContextPages.test.jsx`
  - `cd frontend && npm run build`
```

- [ ] **Step 2: 更新交付状态**

在 `docs/engineering/current-delivery-status.md` 中记录本场景已从“旧计划局部实现”切换为“正式壳层与上下文专项计划”，下一动作聚焦导航纪律、锁定态错误和跨台自动聚焦。
