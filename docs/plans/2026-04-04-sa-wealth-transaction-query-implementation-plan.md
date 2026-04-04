# SA 理财交易查询 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 SA 理财交易场景补正式运行链路，支持 2020 年 8 月后正式覆盖、覆盖区间解释和与基金 / 受托理财的互斥判定。

**Architecture:** 后端补 SA 理财场景识别、2020 年 8 月覆盖边界和互斥约束；前端运行决策台补覆盖区间说明与互斥约束解释。

**Tech Stack:** Spring Boot、MockMvc、React、Vitest、OpenAPI、Vite

---

## 前置依赖

- `docs/architecture/features/iteration-03-payroll-and-wealth/17-SA理财交易查询.md`
- 现有运行检索链路和财富类测试夹具

## 任务拆分

### Task 1: 先补失败测试

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`

- [ ] **Step 1: 增加 2020 年 8 月前缺口解释用例**
- [ ] **Step 2: 增加基金 / SA 理财互斥判定用例**
- [ ] **Step 3: 运行失败测试**

Run:

- `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`
- `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx`

Expected: FAIL

### Task 2: 实现 SA 理财场景配置

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/KnowledgePackageQueryAppService.java`

- [ ] **Step 1: 实现 2020 年 8 月覆盖边界判断**
- [ ] **Step 2: 实现产品互斥约束与缺口说明**
- [ ] **Step 3: 跑后端验证**

Run: `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`

Expected: PASS

### Task 3: 前端补覆盖区间与互斥解释

**Files:**

- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`

- [ ] **Step 1: 渲染覆盖区间说明**
- [ ] **Step 2: 渲染互斥约束解释**
- [ ] **Step 3: 跑前端测试与构建**

Run:

- `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx`
- `cd frontend && npm run build`

Expected: PASS
