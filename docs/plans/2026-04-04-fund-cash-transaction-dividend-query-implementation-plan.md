# 基金资金交易与分红记录查询 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为基金资金事件场景补正式运行链路，支持按资金日期返回资金交易与分红记录，并与申请记录场景严格切分。

**Architecture:** 后端补资金类时间语义选择、资金类输出契约和分红事件承接；前端运行决策台补资金日期解释卡与回到申请记录场景的反向切换提示。

**Tech Stack:** Spring Boot、MockMvc、React、Vitest、OpenAPI、Vite

---

## 目标范围

- 覆盖资金交易日期、资金清算日期、净值日期三类解释。
- 输出资金交易、分红、净值等资金类字段。
- 不实现基金申请级事件。

## 前置依赖

- `docs/architecture/features/iteration-03-payroll-and-wealth/16-基金资金交易与分红记录查询.md`
- 现有运行检索链路和 `Knowledge Package API（知识包接口）`

## 任务拆分

### Task 1: 先补失败测试

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`

- [ ] **Step 1: 后端增加资金日期口径与分红记录用例**
- [ ] **Step 2: 前端增加资金日期提示卡用例**
- [ ] **Step 3: 运行失败测试**

Run:

- `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`
- `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx`

Expected: FAIL

### Task 2: 实现资金类场景与输出契约

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/KnowledgePackageQueryAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackageDTO.java`

- [ ] **Step 1: 增加资金事件主对象判定**
- [ ] **Step 2: 输出资金日期口径、分红事件和覆盖说明**
- [ ] **Step 3: 跑后端验证**

Run: `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`

Expected: PASS

### Task 3: 前端渲染资金口径说明

**Files:**

- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`

- [ ] **Step 1: 渲染资金日期解释卡**
- [ ] **Step 2: 渲染回到申请记录场景的提示入口**
- [ ] **Step 3: 跑前端测试与构建**

Run:

- `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx`
- `cd frontend && npm run build`

Expected: PASS
