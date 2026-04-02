# Knowledge Production Real-Service E2E Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为知识生产台补齐两条最小必要的真实服务 `Playwright（浏览器自动化框架）` 专项用例，作为当前方案收口证据。

**Architecture:** 复用现有真实服务回归基线与测试数据准备方式，不扩展第三条用例，不新增以测试为目的的前端交互改造。优先使用现有标题、按钮文案和路由断言；仅当仓库里已有可复用选择器时才直接引用。按 `Red（先失败） -> Green（最小通过） -> Refactor（保持绿色）` 补齐空态门禁和完成态跳转两条专项。

**Tech Stack:** Playwright、真实后端服务、现有 `frontend/e2e/real-service-smoke.spec.jsx`

---

## 目标范围

- 新增空态门禁真实服务专项用例。
- 新增完成态 `CTA` 跳转 `/map/scenes` 的真实服务专项用例。
- 保持测试过程中不使用 `page.route` / `route.fulfill`。

## 前置文档

- 设计来源：`docs/architecture/frontend-workbench-design.md` §4.3.1
- 特性文档：`docs/architecture/features/iteration-01-knowledge-production/02e-knowledge-production-real-service-e2e-hardening.md`
- 现有真实服务回归：`frontend/e2e/real-service-smoke.spec.jsx`

### Task 1: 先写空态门禁失败用例

**Files:**
- Modify: `frontend/e2e/real-service-smoke.spec.jsx`
- Test: `frontend/e2e/real-service-smoke.spec.jsx`

- [ ] **Step 1: 补空态任务门禁用例**

```jsx
test("empty import state stays on step 01 only", async ({ page }) => {
  await page.goto("/#/production/ingest");

  await expect(page.getByRole("heading", { name: "当前还没有有效导入任务" })).toBeVisible();
  await expect(page.getByRole("button", { name: "导入并生成草稿" })).toBeVisible();
  await expect(page.getByRole("button", { name: "回到此阶段" })).toHaveCount(1);
  await expect(page.getByRole("button", { name: "确认质检，进入对照" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "确认对照，进入发布" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "保存当前场景" })).toHaveCount(0);
});
```

- [ ] **Step 2: 运行单用例并确认先失败**

Run: `cd frontend && npx playwright test e2e/real-service-smoke.spec.jsx --grep "empty import state stays on step 01 only"`

Expected: FAIL，直到真实服务空态数据准备和页面断言完全对齐。

### Task 2: 补完成态跳转失败用例

**Files:**
- Modify: `frontend/e2e/real-service-smoke.spec.jsx`

- [ ] **Step 1: 补完成态主线 CTA 跳转用例**

```jsx
test("completed import task jumps to map scenes with real data", async ({ page }) => {
  await page.goto("/#/production/ingest");

  const restoreButton = page.getByRole("button", { name: "恢复处理" }).first();
  await expect(restoreButton).toBeVisible({ timeout: 15000 });
  await restoreButton.click();

  await expect(page.getByRole("heading", { name: "当前导入任务已处理完成" })).toBeVisible({
    timeout: 15000,
  });
  await expect(page.getByRole("button", { name: "查看已处理场景" })).toBeVisible();
  await page.getByRole("button", { name: "查看已处理场景" }).click();

  await expect(page).toHaveURL(/#\/map\/scenes(?:\?|$)/);
  await expect(page.getByRole("heading", { name: "业务场景" })).toBeVisible();
  await expect(page.getByText("代发协议查询")).toBeVisible({ timeout: 15000 });
});
```

- [ ] **Step 2: 运行单用例并确认先失败**

Run: `cd frontend && npx playwright test e2e/real-service-smoke.spec.jsx --grep "completed import task jumps to map scenes with real data"`

Expected: FAIL，直到真实服务数据准备、恢复处理和 `/map/scenes` 落页完全打通。

### Task 3: 最小修正数据准备与回归集合

**Files:**
- Modify: `frontend/e2e/real-service-smoke.spec.jsx`
- Modify: `scripts/run_system_test_flow.sh`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 对齐真实服务前置数据准备**

```text
空态：无有效导入任务、draftCount=0、无 preprocess、无 candidateGraph、无 scenes
完成态：存在 1 个可恢复已完成任务、publishedCount>=1、/map/scenes 返回“代发协议查询”
```

- [ ] **Step 2: 运行两条专项与原 smoke 集合**

Run: `cd frontend && npx playwright test e2e/real-service-smoke.spec.jsx`

Expected: PASS，且日志中不出现 `page.route` / `route.fulfill` 注入。

- [ ] **Step 3: 回写交付状态与测试文档**

```text
docs/engineering/current-delivery-status.md
docs/testing/features/...
```

Expected: 当前工作项的“最新完成 / 下一动作 / 退出条件”更新为“两条真实服务专项用例已通过”。
