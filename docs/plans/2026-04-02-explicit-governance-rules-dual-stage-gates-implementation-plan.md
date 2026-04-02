# Explicit Governance Rules Dual-Stage Gates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把候选场景确认后的治理完整性从“`Import Confirm（导入确认）` 隐式补最小种子”改成“显式规则评估 + 缺口任务 + 双阶段发布门禁”的正式闭环。

**Architecture:** 复用现有 `Gap Task（缺口任务）` 表和发布检查入口，在后端新增统一的治理规则评估服务与读模型；`Import Confirm` 只创建草稿并产出治理缺口，`Publish Gate` 只消费结构化评估摘要。前端先在知识生产台和发布检查区展示最小必要的治理规则状态与阻断摘要，不扩成独立治理工作台。

**Tech Stack:** Spring Boot、JPA、Maven、React、Vitest

---

## 目标范围

- 去掉 `Import Confirm` 里的隐式治理种子补齐。
- 落地三条显式治理规则与两阶段评估：`IMPORT_CONFIRM`、`PRE_PUBLISH`。
- 复用 `Gap Task` 持久化治理缺口，并暴露场景治理摘要读接口。
- 在知识生产台与发布检查区展示治理缺口摘要。

## 前置文档

- 特性文档：`docs/architecture/features/iteration-01-knowledge-production/06a-explicit-governance-rules-and-dual-stage-gates.md`
- 关联文档：`docs/architecture/features/iteration-01-knowledge-production/06-复核与缺口任务协同.md`
- 主文档：`docs/architecture/system-design.md` §5.4、§5.5、§6.1

## Task 1: 先补失败测试与读模型骨架

**Files:**

- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/governance/SceneGovernanceGateAppServiceTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MvpKnowledgeGraphFlowIntegrationTest.java`

- [ ] **Step 1: 先写规则评估失败测试**

Run:

- `cd backend && mvn -q -Dtest=SceneGovernanceGateAppServiceTest test`

Expected:

- FAIL，因为当前还没有统一治理规则评估服务，也没有结构化的治理摘要对象。

- [ ] **Step 2: 锁定第一版规则集**

Expected outputs:

- `GR-DICT-001`
- `GR-IDL-001`
- `GR-TIME-001`
- 阶段：`IMPORT_CONFIRM`、`PRE_PUBLISH`
- 结果：`PASSED / FAILED / WAIVED`
- 阻断等级：`BLOCKING / WARNING`

## Task 2: 重构后端导入确认与治理摘要

**Files:**

- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/governance/GovernanceGapDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/governance/GovernanceRuleResultDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/governance/SceneGovernanceSummaryDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ConfirmImportSceneCandidateResultDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/governance/SceneGovernanceGateAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportTaskCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ImportController.java`

- [ ] **Step 1: 让 confirm 返回 `scene + governanceSummary`**

Expected outputs:

- 候选场景确认后返回正式草稿场景
- 同时返回三条治理规则的当前结果
- 同时返回治理缺口列表

- [ ] **Step 2: 删除隐式补种子逻辑**

Run:

- `cd backend && mvn -q -Dtest=SceneGovernanceGateAppServiceTest,MvpKnowledgeGraphFlowIntegrationTest test`

Expected:

- 先 FAIL，直到 `Import Confirm` 不再自动补字典、标识链、时间语义对象，而是改为缺口任务输出。

## Task 3: 重构发布门禁只消费结构化摘要

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/SceneCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/ScenePublishGateAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/GraphAssetController.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/SceneGovernanceController.java`

- [ ] **Step 1: 发布门禁接入 `PRE_PUBLISH` 规则评估**

Expected outputs:

- 发布命令不再直接查三张治理对象表再拼错误字符串
- `publish-checks` 返回失败规则、未关闭阻断缺口和可读摘要
- 新增场景治理缺口查询接口

- [ ] **Step 2: 跑后端主链路回归**

Run:

- `cd backend && mvn -q -Dtest=SceneGovernanceGateAppServiceTest,MvpKnowledgeGraphFlowIntegrationTest test`
- `cd backend && mvn -q test`

Expected:

- PASS，且 `MvpKnowledgeGraphFlowIntegrationTest` 明确要求显式补齐治理对象后才能发布。

## Task 4: 补前端治理摘要可见性

**Files:**

- Modify: `frontend/src/pages/KnowledgePage.jsx`
- Modify: `frontend/src/pages/KnowledgePage.test.jsx`
- Modify: `frontend/src/pages/PublishCenterPage.test.jsx`
- Modify: `frontend/src/types/openapi.d.ts`

- [ ] **Step 1: 先写失败页面断言**

Run:

- `cd frontend && npm test -- src/pages/KnowledgePage.test.jsx src/pages/PublishCenterPage.test.jsx`

Expected:

- FAIL，因为当前页面还没有显式展示治理规则失败与缺口摘要。

- [ ] **Step 2: 对齐前端治理摘要展示**

Expected outputs:

- 知识生产台出现治理缺口摘要区
- 发布检查区能展示失败规则和阻断缺口
- 中文口径稳定，不引入额外工作台

- [ ] **Step 3: 跑前端回归**

Run:

- `cd frontend && npm test -- src/pages/KnowledgePage.test.jsx src/pages/PublishCenterPage.test.jsx`
- `cd frontend && npm run lint`
- `cd frontend && npm run build`

Expected:

- PASS，且页面显示“失败规则 / 阻断级缺口 / 需补对象”三类信息。

## Task 5: 回写交付状态与验证证据

**Files:**

- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 回写当前工作项状态**

Expected outputs:

- 最新完成：显式治理规则与双阶段门禁实现完成
- 下一动作：如无新增阻塞，进入代码检视与测试报告补齐

- [ ] **Step 2: 汇总最终验证命令**

Run:

- `cd backend && mvn -q test`
- `cd frontend && npm run lint`
- `cd frontend && npm run test`
- `cd frontend && npm run build`

Expected:

- PASS，形成本轮实现、回归与交接证据。
