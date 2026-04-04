# 断卡排查账户管控核验 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为断卡排查场景补正式运行链路，支持多入口标识收敛、按 `as_of_time（截止时点）` 回看最近管控记录，并对高敏明细维持审批边界。

**Architecture:** 后端补多入口标识收敛、截止时点回看和最小核验结果输出；前端补标识收敛链、截止时点解释和审批导出跳转。

**Tech Stack:** Spring Boot、MockMvc、React、Vitest、OpenAPI、Vite

---

## 前置依赖

- `docs/architecture/features/iteration-04-retail-and-high-risk/21-断卡排查账户管控核验.md`
- 现有标识谱系、推理结论和审批 / 审计页

## 任务拆分

### Task 1: 先补失败测试

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`

- [ ] **Step 1: 增加多入口标识收敛与截止时点回看用例**
- [ ] **Step 2: 增加“仅返回最小核验结果”前端用例**
- [ ] **Step 3: 运行失败测试**

Run:

- `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`
- `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx`

Expected: FAIL

### Task 2: 实现标识收敛和核验结果输出

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/KnowledgePackageQueryAppService.java`

- [ ] **Step 1: 实现客户号 / 户口号 / 证件号 / UID 收敛**
- [ ] **Step 2: 实现按 `as_of_time` 命中最近有效记录**
- [ ] **Step 3: 跑后端验证**

Run: `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`

Expected: PASS

### Task 3: 前端补收敛链与审批跳转

**Files:**

- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`

- [ ] **Step 1: 渲染标识收敛链和截止时点提示**
- [ ] **Step 2: 渲染高敏明细审批导出入口**
- [ ] **Step 3: 跑前端测试与构建**

Run:

- `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx`
- `cd frontend && npm run build`

Expected: PASS
