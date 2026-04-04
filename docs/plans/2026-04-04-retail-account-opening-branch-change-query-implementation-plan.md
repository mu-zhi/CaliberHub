# 零售户口开户机构变更查询 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为开户机构变更轨迹场景补正式运行链路，支持轨迹查询、候选发布阻断和缺口任务联动。

**Architecture:** 后端补变更轨迹场景配置、缺口阻断与轨迹输出；前端运行决策台补“当前返回的是变更轨迹而非当前画像”的显式提示，并联动知识生产台缺口任务。

**Tech Stack:** Spring Boot、MockMvc、React、Vitest、OpenAPI、Vite

---

## 前置依赖

- `docs/architecture/features/iteration-04-retail-and-high-risk/19-零售户口开户机构变更查询.md`
- 现有缺口任务与影响分析相关能力

## 任务拆分

### Task 1: 先补失败测试

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`

- [ ] **Step 1: 增加字段不完整时阻断并转缺口任务用例**
- [ ] **Step 2: 增加“变更轨迹而非当前画像”前端提示用例**
- [ ] **Step 3: 运行失败测试**

Run:

- `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`
- `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx`

Expected: FAIL

### Task 2: 实现轨迹场景和缺口联动

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/KnowledgePackageQueryAppService.java`

- [ ] **Step 1: 输出变更轨迹字段和变更生效日期**
- [ ] **Step 2: 对缺字段场景输出阻断与缺口任务信息**
- [ ] **Step 3: 跑后端验证**

Run: `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`

Expected: PASS

### Task 3: 前端补轨迹解释和补缺入口

**Files:**

- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`

- [ ] **Step 1: 渲染轨迹说明与当前画像边界**
- [ ] **Step 2: 渲染跳转知识生产台缺口任务入口**
- [ ] **Step 3: 跑前端测试与构建**

Run:

- `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx`
- `cd frontend && npm run build`

Expected: PASS
