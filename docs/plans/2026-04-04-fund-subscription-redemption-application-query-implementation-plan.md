# 基金申购、赎回申请记录查询 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为基金申请类场景补正式运行链路，支持按申请日期返回基金申购、赎回申请记录，并与资金交易场景严格切分。

**Architecture:** 在后端沿用现有运行检索主线，新增基金申请场景配置、申请日期口径解释和申请类输出契约；前端运行决策台补申请日期提示和跳转到资金交易场景的澄清入口。

**Tech Stack:** Spring Boot、MockMvc、React、Vitest、OpenAPI、Vite

---

## 目标范围

- 覆盖基金申购、赎回申请记录的场景检索、方案选择、知识包输出与前端解释。
- 输出申请日期、产品、金额、份额、渠道等申请类字段。
- 不实现资金清算结果与分红流水，它们归属 `16-基金资金交易与分红记录查询.md`。

## 前置依赖

- `docs/architecture/features/iteration-03-payroll-and-wealth/15-基金申购、赎回申请记录查询.md`
- 现有运行决策台与 `Knowledge Package API（知识包接口）`
- 现有 `KnowledgePackageApiIntegrationTest.java` 与前端运行页测试

## 任务拆分

### Task 1: 为基金申请场景补失败测试

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`

- [ ] **Step 1: 写后端失败测试**

Run: `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`

Expected: 新增“申请日期口径命中”和“跳转到资金交易场景澄清”用例先失败。

- [ ] **Step 2: 写前端失败测试**

Run: `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx`

Expected: 新增“显示申请日期口径说明”用例先失败。

### Task 2: 实现场景配置与知识包输出

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/KnowledgePackageQueryAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackageDTO.java`
- Modify: `backend/src/main/resources/static/samples/ingest/` 相关样例或测试夹具

- [ ] **Step 1: 增加基金申请场景夹具与主对象判定**
- [ ] **Step 2: 输出申请日期口径、申请类核心字段和跳转提示**
- [ ] **Step 3: 运行后端测试验证 Green**

Run: `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`

Expected: PASS

### Task 3: 前端补申请日期解释和跨场景切换提示

**Files:**

- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`

- [ ] **Step 1: 展示“当前按申请日期口径返回”提示**
- [ ] **Step 2: 当用户请求资金日期时展示切换到资金交易场景的入口**
- [ ] **Step 3: 跑前端测试与构建**

Run:

- `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx`
- `cd frontend && npm run build`

Expected: PASS
