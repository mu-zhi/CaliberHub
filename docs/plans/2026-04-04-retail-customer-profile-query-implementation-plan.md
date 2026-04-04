# 零售客户基础信息查询 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为零售客户基础画像场景补正式运行链路，支持三类入口收敛、快照日期提示和 `S2（敏感级别二）` 字段默认脱敏。

**Architecture:** 后端补客户画像场景配置、快照日期输出和字段级脱敏结果；前端运行决策台补快照日期说明与字段裁剪解释。

**Tech Stack:** Spring Boot、MockMvc、React、Vitest、OpenAPI、Vite

---

## 前置依赖

- `docs/architecture/features/iteration-04-retail-and-high-risk/18-零售客户基础信息查询.md`
- 现有运行检索链路和审批 / 审计页

## 任务拆分

### Task 1: 先补失败测试

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`

- [ ] **Step 1: 增加三类入口收敛与快照提示用例**
- [ ] **Step 2: 增加 `S2` 字段脱敏展示用例**
- [ ] **Step 3: 运行失败测试**

Run:

- `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`
- `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx`

Expected: FAIL

### Task 2: 实现客户画像场景输出

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/KnowledgePackageQueryAppService.java`

- [ ] **Step 1: 实现客户号 / 户口号 / 证件号三类入口收敛**
- [ ] **Step 2: 返回快照日期和字段脱敏结果**
- [ ] **Step 3: 跑后端验证**

Run: `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`

Expected: PASS

### Task 3: 前端补快照与字段级解释

**Files:**

- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`

- [ ] **Step 1: 渲染快照日期说明**
- [ ] **Step 2: 渲染字段裁剪与审批提示**
- [ ] **Step 3: 跑前端测试与构建**

Run:

- `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx`
- `cd frontend && npm run build`

Expected: PASS
