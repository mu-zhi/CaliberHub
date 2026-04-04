# 客户密码修改日志查询 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为密码修改日志高敏场景补正式运行链路，确保默认拒绝、专审申请、审计回放和受控导出完整闭环。

**Architecture:** 后端补密码日志场景配置、最小可见说明字段、审批模板绑定和导出审计；前端补默认拒绝提示、专审入口和完整追踪回放链接。

**Tech Stack:** Spring Boot、MockMvc、React、Vitest、OpenAPI、Vite

---

## 前置依赖

- `docs/architecture/features/iteration-04-retail-and-high-risk/20-客户密码修改日志查询.md`
- 现有审批与导出页、监控与审计页

## 任务拆分

### Task 1: 先补失败测试

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
- Modify: `frontend/src/pages/ApprovalExportPage.test.jsx`
- Modify: `frontend/src/pages/MonitoringAuditPage.test.jsx`

- [ ] **Step 1: 增加“未经专审暴露次数为 0”用例**
- [ ] **Step 2: 增加专审入口与审计追踪可见性用例**
- [ ] **Step 3: 运行失败测试**

Run:

- `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`
- `cd frontend && npm test -- src/pages/ApprovalExportPage.test.jsx src/pages/MonitoringAuditPage.test.jsx`

Expected: FAIL

### Task 2: 实现高敏默认拒绝与审批导出闭环

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/KnowledgePackageQueryAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/GraphAuditEventAppService.java`

- [ ] **Step 1: 返回最小可见说明字段**
- [ ] **Step 2: 绑定审批模板、导出任务和审计事件链**
- [ ] **Step 3: 跑后端验证**

Run: `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`

Expected: PASS

### Task 3: 前端补默认拒绝和专审链路

**Files:**

- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/ApprovalExportPage.jsx`
- Modify: `frontend/src/pages/MonitoringAuditPage.jsx`

- [ ] **Step 1: 渲染高敏默认拒绝提示**
- [ ] **Step 2: 渲染专审跳转与追踪编号**
- [ ] **Step 3: 跑前端测试与构建**

Run:

- `cd frontend && npm test -- src/pages/ApprovalExportPage.test.jsx src/pages/MonitoringAuditPage.test.jsx`
- `cd frontend && npm run build`

Expected: PASS
