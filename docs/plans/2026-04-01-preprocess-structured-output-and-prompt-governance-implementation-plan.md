# 预处理结构化输出与 Prompt 治理实施计划

> **For agentic workers:** 默认按 `subagent-driven-development（子代理驱动开发技能）` 执行；进入实现前先跑 `test-driven-development（测试驱动开发技能）`，实现完成后必须走 `requesting-code-review（请求代码审查技能）`、`code-reviewing（代码检视技能）` 与 `feature-test-report（特性测试与验收技能）`。

**Feature Doc:** `docs/architecture/features/iteration-01-knowledge-production/02c-preprocess-structured-output-and-prompt-governance.md`

**Goal:** 把预处理 Prompt、结构化输出 Schema、人工审核标记、预览与测试回放正式收口成可审计治理链路。

**Scope:** 仅覆盖 Prompt / Schema / 审核与测试治理，不改 provider transport，不做 OpenAI endpoint 适配。

**Preconditions:**

1. 以 `02c-preprocess-structured-output-and-prompt-governance.md` 为唯一特性真源。
2. 当前默认 Prompt 已经对齐为结构化契约写法，本计划承接“治理闭环”，不是再做一轮 Prompt 文案试错。
3. 先补失败测试和审核字段，再做 UI 与接口收口。

---

## Task 1: 先写 Prompt / Schema 审核门禁的失败测试

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/LlmPreprocessConfigApiIntegrationTest.java`
- Create: `frontend/src/pages/__tests__/SystemPage.promptGovernance.test.jsx`

- [ ] **Step 1: 增加审核状态与结构化校验断言**

至少断言：

```java
jsonPath("$.requiresManualReview").isBoolean()
jsonPath("$.schemaValidationMessage").isString()
jsonPath("$.promptFingerprint").isString()
```

- [ ] **Step 2: 运行红灯测试**

Run:

- `cd backend && mvn -q -Dtest=LlmPreprocessConfigApiIntegrationTest test`
- `cd frontend && npm test -- src/pages/__tests__/SystemPage.promptGovernance.test.jsx`

Expected:

- 测试失败，提示审核字段、前端审核展示或结构化校验状态尚不存在。

## Task 2: 补齐后端 Prompt 治理字段与门禁

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/domain/model/LlmPreprocessConfig.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/LlmPreprocessConfigCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/LlmPreprocessConfigPO.java`
- Create: `backend/src/main/resources/db/migration/V21__llm_prompt_governance_flags.sql`

- [ ] **Step 1: 增加审核状态、审核摘要或等价字段**

- [ ] **Step 2: 在保存 / 重置 / 预览 / 测试链路中统一返回审核状态**

- [ ] **Step 3: 复跑后端测试**

Run:

- `cd backend && mvn -q -Dtest=LlmPreprocessConfigApiIntegrationTest test`

Expected:

- 后端配置接口已能返回 Prompt 审核状态与结构化校验结果。

## Task 3: 收口前端系统管理页的 Prompt 治理表达

**Files:**

- Modify: `frontend/src/pages/SystemPage.jsx`
- Create: `frontend/src/pages/systemPagePromptGovernance.js`

- [ ] **Step 1: 在系统管理页显式区分草稿、默认值、需人工审核状态**

- [ ] **Step 2: 显示结构化校验结果、Prompt 指纹和审核提示**

- [ ] **Step 3: 复跑前端测试与构建**

Run:

- `cd frontend && npm test -- src/pages/__tests__/SystemPage.promptGovernance.test.jsx`
- `cd frontend && npm run build`

Expected:

- 前端测试和构建通过，系统管理页具备最小治理表达。

## Task 4: 增加回放样例与交付状态同步

**Files:**

- Create: `docs/testing/features/iteration-01-knowledge-production/02c-preprocess-structured-output-and-prompt-governance-test-report.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 新建测试报告骨架，登记 Prompt 审核与 Schema 校验回放集**

- [ ] **Step 2: 记录交付状态**

- [ ] **Step 3: 跑最小回归**

Run:

- `cd backend && mvn -q -Dtest=LlmPreprocessConfigApiIntegrationTest test`
- `cd frontend && npm test -- src/pages/__tests__/SystemPage.promptGovernance.test.jsx && npm run build`

Expected:

- Prompt 治理相关后端、前端验证通过，测试报告可承接后续 reviewing 阶段。

