# 首页总览首屏文案减重 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 删除首页总览 Hero 左侧的长标题与说明文字，只保留 `首页总览 / 指挥页` 轻量标识，同时保证状态条、动作卡和首页主结构保持不变。

**Architecture:** 本次改动只收口 `HomePage` 的 Hero 文案结构，不调整 CSS 和首页其他模块。验证采用现有服务端渲染测试文件补精确断言，再以一份对应特性测试文档承接后续 `reviewing（评审验证中）` 留痕与交付状态同步。

**Tech Stack:** React 18, React Router, Vite, Vitest, Markdown 文档体系（`docs/plans/`、`docs/testing/features/`、`docs/engineering/`）

---

## Design Inputs

- `docs/architecture/frontend-workbench-design.md`
- `docs/architecture/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发.md`
- `docs/plans/2026-03-30-home-overview-hero-copy-trim-design.md`

## File Map

- Create: `docs/testing/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发-测试报告.md`
- Modify: `docs/engineering/current-delivery-status.md`
- Modify: `frontend/src/pages/HomePage.jsx`
- Modify: `frontend/src/pages/WorkbenchPages.render.test.jsx`

## Scope

- 只删除首页 Hero 左侧的长标题与说明文。
- 保留 `首页总览 / 指挥页` 与 `home-overview-statusbar`。
- 不修改动作卡、指标卡、主区模块、跳转关系和样式文件。
- 不新增新的视觉替代元素。

### Task 1: 先落测试与验收骨架

**Files:**

- Create: `docs/testing/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发-测试报告.md`
- Modify: `frontend/src/pages/WorkbenchPages.render.test.jsx`

- [ ] **Step 1: 创建首页总览对应测试文档骨架**

在 `docs/testing/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发-测试报告.md` 中写入以下内容：

```md
# 首页总览与状态分发测试文档

> 对应特性文档：`docs/architecture/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发.md`
> 当前阶段：待进入 `reviewing（评审验证中）`

## 1. 测试目标

验证首页总览 Hero 文案减重后，页面仍保持“状态优先、任务分发次之”的首页定位。

本轮重点覆盖：

1. Hero 左侧只保留 `首页总览 / 指挥页`
2. 已移除长标题“先看到当前版本、风险和待办，再进入图谱工作台”
3. 已移除首页说明文“首页不再以搜索为第一动作，而是把发布阻断、审批积压、运行健康和近期变更放到同一视野里。”
4. `home-overview-statusbar` 仍随首页一起正常渲染

## 2. 测试范围

本轮覆盖：

1. 首页 Hero 文案结构
2. 首页服务端渲染断言
3. 前端构建可用性

本轮不覆盖：

1. 首页动作卡顺序或跳转
2. 状态条文案与数据来源
3. 首页样式重排与响应式细调

## 3. 测试环境

1. 前端：`frontend`
2. 构建工具：Vite
3. 测试工具：Vitest
4. 页面入口：`frontend/src/pages/HomePage.jsx`

## 4. 测试案例

| 编号 | 用例 | 输入 | 预期输出 | 实际结果 |
| --- | --- | --- | --- | --- |
| TC-01 | 保留轻量页头标识 | 渲染 `HomePage` | HTML 中包含 `首页总览 / 指挥页` | 待进入 `reviewing` 后回填 |
| TC-02 | 删除长标题 | 渲染 `HomePage` | HTML 中不包含 `先看到当前版本、风险和待办，再进入图谱工作台` | 待进入 `reviewing` 后回填 |
| TC-03 | 删除说明文字 | 渲染 `HomePage` | HTML 中不包含首页说明文 | 待进入 `reviewing` 后回填 |
| TC-04 | 状态条保留 | 渲染 `HomePage` | HTML 中包含 `当前全局状态` 与 `稳定版本` | 待进入 `reviewing` 后回填 |

## 5. TDD 与测试命令引用

待在实现完成并进入 `reviewing（评审验证中）` 后回填以下命令结果：

1. `cd frontend && npm test -- src/pages/WorkbenchPages.render.test.jsx`
2. `cd frontend && npm run build`

## 6. 缺陷清单

当前阶段暂无已确认缺陷；进入 `reviewing` 后如发现问题，按 `P0 / P1 / P2` 分级回填。

## 7. 放行结论

当前阶段尚未进入 `reviewing（评审验证中）`，暂不提供放行结论。
```

- [ ] **Step 2: 给首页 Hero 补失败断言**

在 `frontend/src/pages/WorkbenchPages.render.test.jsx` 的 `formal workbench pages render smoke` 后追加下面测试：

```jsx
describe("home overview hero copy", () => {
  it("keeps only the lightweight overview marker before the status bar", () => {
    const html = renderPage(<HomePage />);

    expect(html).toContain("首页总览 / 指挥页");
    expect(html).toContain("当前全局状态");
    expect(html).toContain("稳定版本");
    expect(html).not.toContain("先看到当前版本、风险和待办，再进入图谱工作台");
    expect(html).not.toContain("首页不再以搜索为第一动作，而是把发布阻断、审批积压、运行健康和近期变更放到同一视野里。");
  });
});
```

- [ ] **Step 3: 运行测试验证 Red**

Run: `cd frontend && npm test -- src/pages/WorkbenchPages.render.test.jsx`

Expected:

- FAIL
- 失败点命中 `.not.toContain("先看到当前版本、风险和待办，再进入图谱工作台")`
- 或失败点命中 `.not.toContain("首页不再以搜索为第一动作，而是把发布阻断、审批积压、运行健康和近期变更放到同一视野里。")`

### Task 2: 只改 `HomePage` Hero 文案结构

**Files:**

- Modify: `frontend/src/pages/HomePage.jsx`
- Test: `frontend/src/pages/WorkbenchPages.render.test.jsx`

- [ ] **Step 1: 用最小实现删除长标题与说明文**

把 `frontend/src/pages/HomePage.jsx` 中的 `home-overview-copy` 改成下面这段，除此之外不改 Hero 其他结构：

```jsx
<div className="home-overview-copy">
  <p className="home-overview-kicker">首页总览 / 指挥页</p>
</div>
```

- [ ] **Step 2: 运行目标测试验证 Green**

Run: `cd frontend && npm test -- src/pages/WorkbenchPages.render.test.jsx`

Expected:

- PASS
- 新增的 `keeps only the lightweight overview marker before the status bar` 用例通过
- 现有首页、审批、发布、运行、监控渲染冒烟用例仍通过

- [ ] **Step 3: 运行前端构建验证未引入结构性回归**

Run: `cd frontend && npm run build`

Expected:

- PASS
- 产出新的 `dist/index.html` 与静态资源
- 无 JSX / import / route 相关构建错误

### Task 3: 收口评审、测试留痕与状态真源

**Files:**

- Modify: `docs/testing/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发-测试报告.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 按工作流执行代码检视链路**

按顺序执行：

1. `requesting-code-review（请求代码审查技能）`
2. `code-reviewing（代码检视技能）`

Expected:

1. 若发现 `P0 / P1`，先回到 Task 2 修复后再重新送检。
2. 无阻塞问题后，才允许更新测试文档并进入最终验证。

- [ ] **Step 2: 回填测试文档执行结果**

将 `docs/testing/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发-测试报告.md` 更新为至少包含以下结论：

```md
> 当前阶段：已完成本轮验收，可进入分支收尾

## 4. 测试案例

| 编号 | 用例 | 输入 | 预期输出 | 实际结果 |
| --- | --- | --- | --- | --- |
| TC-01 | 保留轻量页头标识 | 渲染 `HomePage` | HTML 中包含 `首页总览 / 指挥页` | 已通过：目标测试命中轻量标识 |
| TC-02 | 删除长标题 | 渲染 `HomePage` | HTML 中不包含 `先看到当前版本、风险和待办，再进入图谱工作台` | 已通过：目标测试验证长标题已删除 |
| TC-03 | 删除说明文字 | 渲染 `HomePage` | HTML 中不包含首页说明文 | 已通过：目标测试验证说明文已删除 |
| TC-04 | 状态条保留 | 渲染 `HomePage` | HTML 中包含 `当前全局状态` 与 `稳定版本` | 已通过：目标测试验证状态条仍正常渲染 |

## 5. TDD 与测试命令引用

当前已执行：

1. `cd frontend && npm test -- src/pages/WorkbenchPages.render.test.jsx`
2. `cd frontend && npm run build`

执行结果：

1. 首页渲染测试：通过
2. 前端构建：通过

## 6. 缺陷清单

当前无已确认 `P0 / P1 / P2` 缺陷。

## 7. 放行结论

当前结论：通过，可进入 `finishing-a-development-branch（开发分支收尾技能）`。
```

- [ ] **Step 3: 更新交付状态真源**

把 `docs/engineering/current-delivery-status.md` 中“首页总览首屏文案减重”这一行至少更新为以下口径：

```md
| 首页总览首屏文案减重 | 前端工作台细部收口 | [前端工作台设计](../architecture/frontend-workbench-design.md)、[首页总览与状态分发特性文档](../architecture/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发.md)、[2026-03-30-首页总览首屏文案减重设计稿](../plans/2026-03-30-home-overview-hero-copy-trim-design.md) | [2026-03-30-home-overview-hero-copy-trim-implementation-plan.md](../plans/2026-03-30-home-overview-hero-copy-trim-implementation-plan.md) | [12a-首页总览与状态分发-测试报告.md](../testing/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发-测试报告.md) | `done（完成）` | 已完成首页 Hero 文案减重实现：保留 `首页总览 / 指挥页` 与状态条，删除长标题和说明文字；前端目标测试与构建验证通过 | 进入 `finishing-a-development-branch（开发分支收尾技能）`，等待分支处理选择 | 首页 Hero 仅保留轻量页头标识，且无未关闭 `P0/P1` 缺陷 | 无 | Codex（设计 / 实现） | 2026-03-30 |
```

- [ ] **Step 4: 运行最终验证**

Run: `cd frontend && npm test -- src/pages/WorkbenchPages.render.test.jsx`

Expected:

- PASS

Run: `cd frontend && npm run build`

Expected:

- PASS

- [ ] **Step 5: 提交本批次改动**

Run:

```bash
git add docs/plans/2026-03-30-home-overview-hero-copy-trim-implementation-plan.md \
  docs/testing/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发-测试报告.md \
  docs/engineering/current-delivery-status.md \
  frontend/src/pages/HomePage.jsx \
  frontend/src/pages/WorkbenchPages.render.test.jsx
git commit -m "feat: trim home overview hero copy"
```

Expected:

- 生成单一提交，范围只包含首页 Hero 文案减重、对应测试与交付文档同步
