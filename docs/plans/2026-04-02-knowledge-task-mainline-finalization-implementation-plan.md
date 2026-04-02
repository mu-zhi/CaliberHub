# Knowledge Task Mainline Finalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 收口知识生产台任务主线页的历史摘要折叠态字段与后置规则，让 `/production/ingest` 的最终页面口径与最终稿一致。

**Architecture:** 保持当前 `KnowledgePage（知识生产页）`、`knowledge-task-mainline.js` 和 `AccordionStepCard（可折叠步骤卡）` 结构不变，只在前端摘要字段映射、折叠态呈现和目标测试上做最小调整。先用失败测试锁住 4 字段模板与后置项，再做最小实现，最后回归现有任务主线页测试。

**Tech Stack:** React、Vitest、现有 `KnowledgePage.jsx` / `knowledge-task-mainline.js` / `AccordionStepCard.jsx`

---

## 目标范围

- 把历史摘要区折叠态固定为 `Step 01-04` 四字段模板。
- 删除折叠态中的 `CTA`、多行说明和次级列表，只保留最小结果指标。
- 保持任务主线区、次级结果区和跨工作台门禁逻辑不变。

## 前置文档

- 设计来源：`docs/architecture/frontend-workbench-design.md` §4.3.1
- 特性文档：`docs/architecture/features/iteration-01-knowledge-production/02d-knowledge-task-mainline-finalization.md`
- 现有计划：`docs/plans/2026-04-01-knowledge-production-task-mainline-page-implementation-plan.md`

### Task 1: 用失败测试锁住折叠态四字段模板

**Files:**
- Modify: `frontend/src/pages/KnowledgePage.render.test.jsx`
- Modify: `frontend/src/pages/KnowledgePage.test.jsx`

- [ ] **Step 1: 在渲染测试中补四字段模板断言**

```jsx
it("renders collapsed history summaries with four final fields only", () => {
  const html = renderKnowledgePage("import");

  expect(html).toContain("来源类型");
  expect(html).toContain("导入规模");
  expect(html).toContain("识别场景数");
  expect(html).toContain("已确认场景数");
  expect(html).toContain("待发布场景数");
  expect(html).not.toContain("导入耗时");
  expect(html).not.toContain("推荐动作");
});
```

- [ ] **Step 2: 在交互测试中补“折叠态不保留 CTA”断言**

```jsx
it("does not render repeated CTA content inside collapsed history summaries", async () => {
  render(<KnowledgePage preset="import" />);

  expect(screen.queryByText("推荐动作")).not.toBeInTheDocument();
  expect(screen.queryAllByRole("button", { name: "查看已处理场景" })).toHaveLength(1);
});
```

- [ ] **Step 3: 运行测试并确认先失败**

Run: `cd frontend && npm test -- src/pages/KnowledgePage.render.test.jsx src/pages/KnowledgePage.test.jsx`

Expected: FAIL，因为当前折叠态字段和重复动作表达尚未完全符合最终稿。

### Task 2: 最小实现四字段映射与后置规则

**Files:**
- Modify: `frontend/src/pages/KnowledgePage.jsx`
- Modify: `frontend/src/components/knowledge/AccordionStepCard.jsx`

- [ ] **Step 1: 在 `KnowledgePage.jsx` 收口每个历史步骤的折叠态字段**

```jsx
const historySummaryItems = {
  step01: ["步骤标题", "处理状态", "来源类型", "导入规模（字符数）"],
  step02: ["步骤标题", "处理状态", "识别场景数", "置信度"],
  step03: ["步骤标题", "处理状态", "已确认场景数", "待处理场景数"],
  step04: ["步骤标题", "处理状态", "待发布场景数", "当前场景状态"],
};
```

- [ ] **Step 2: 在 `AccordionStepCard.jsx` 保证折叠态只渲染摘要字段，不渲染次级动作**

```jsx
{collapsed ? (
  <SummaryFields fields={summaryFields.slice(0, 4)} />
) : (
  <>{children}</>
)}
```

- [ ] **Step 3: 重新运行目标测试确认转绿**

Run: `cd frontend && npm test -- src/pages/KnowledgePage.render.test.jsx src/pages/KnowledgePage.test.jsx`

Expected: PASS

### Task 3: 回归任务主线页专项测试与构建

**Files:**
- Test: `frontend/src/pages/knowledge-task-mainline.test.js`

- [ ] **Step 1: 运行任务主线页专项测试**

Run: `cd frontend && npm test -- src/pages/knowledge-task-mainline.test.js`

Expected: PASS

- [ ] **Step 2: 运行前端构建**

Run: `cd frontend && npm run build`

Expected: build 成功，无新的折叠态渲染错误。

- [ ] **Step 3: 回写测试文档与交付状态**

```text
docs/engineering/current-delivery-status.md
docs/testing/features/...
```

Expected: 最新完成与退出条件更新为“历史摘要区四字段模板已按最终稿收口”。
