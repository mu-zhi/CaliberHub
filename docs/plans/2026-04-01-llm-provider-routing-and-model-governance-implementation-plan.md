# LLM Provider 路由与模型治理实施计划

> **For agentic workers:** 默认按 `subagent-driven-development（子代理驱动开发技能）` 执行；进入实现前先跑 `test-driven-development（测试驱动开发技能）`，实现完成后必须走 `requesting-code-review（请求代码审查技能）`、`code-reviewing（代码检视技能）` 与 `feature-test-report（特性测试与验收技能）`。

**Feature Doc:** `docs/architecture/features/iteration-02-runtime-and-governance/11c-llm-provider-routing-and-model-governance.md`

**Goal:** 为导入预处理链路补齐 provider 预设、模型能力矩阵、参数白名单、切换审计与灰度发布门禁。

**Scope:** 仅覆盖 provider / model 治理与灰度观测，不实现 OpenAI `Responses API` 具体适配，不改 Prompt 主文案。

**Preconditions:**

1. 以 `11c-llm-provider-routing-and-model-governance.md` 为唯一特性真源。
2. 现有系统管理页和配置持久化链路继续作为主入口。
3. 先固化能力矩阵与参数白名单测试，再接 UI 与灰度门禁。

---

## Task 1: 先写 provider 能力矩阵与参数白名单失败测试

**Files:**

- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/config/LlmProviderCapabilityRegistryTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/LlmPreprocessConfigApiIntegrationTest.java`

- [ ] **Step 1: 新增能力矩阵断言**

至少断言：

```java
assertThat(LlmProviderCapabilityRegistry.find("OPENAI", "gpt-5.4")).isPresent();
assertThat(LlmProviderCapabilityRegistry.find("OPENAI", "gpt-5.4").get().supportsResponsesApi()).isTrue();
assertThat(LlmProviderCapabilityRegistry.find("COMPATIBLE", "qwen3-max").get().supportsResponsesApi()).isFalse();
```

- [ ] **Step 2: 运行红灯测试**

Run:

- `cd backend && mvn -q -Dtest=LlmProviderCapabilityRegistryTest,LlmPreprocessConfigApiIntegrationTest test`

Expected:

- 测试失败，提示能力矩阵、参数白名单或配置接口还未表达 provider 差异。

## Task 2: 引入 provider 预设与能力矩阵

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/LlmPreprocessProperties.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/LlmProviderCapabilityRegistry.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/LlmPreprocessConfigCommandAppService.java`

- [ ] **Step 1: 增加 provider 枚举或等价字段**

- [ ] **Step 2: 建立 provider / model 能力矩阵与参数白名单**

- [ ] **Step 3: 在保存配置时阻断不合法组合**

- [ ] **Step 4: 复跑后端测试**

Run:

- `cd backend && mvn -q -Dtest=LlmProviderCapabilityRegistryTest,LlmPreprocessConfigApiIntegrationTest test`

Expected:

- 非法 provider / model / 参数组合被拦截，合法组合可保存。

## Task 3: 收口系统管理页 provider 选择、切换提示与灰度摘要

**Files:**

- Modify: `frontend/src/pages/SystemPage.jsx`
- Create: `frontend/src/pages/__tests__/SystemPage.providerCapability.test.jsx`

- [ ] **Step 1: 增加 provider / model 能力提示**

- [ ] **Step 2: 为 OpenAI 预设显示推荐模型摘要，例如 `gpt-5.4` / `gpt-5-mini`**

- [ ] **Step 3: 显示切换风险与需人工审核提示**

- [ ] **Step 4: 复跑前端测试与构建**

Run:

- `cd frontend && npm test -- src/pages/__tests__/SystemPage.providerCapability.test.jsx`
- `cd frontend && npm run build`

Expected:

- 系统管理页能表达 provider 能力差异，前端测试与构建通过。

## Task 4: 补齐切换审计、交付状态与最小灰度验证

**Files:**

- Modify: `docs/engineering/current-delivery-status.md`
- Create: `docs/testing/features/iteration-02-runtime-and-governance/11c-llm-provider-routing-and-model-governance-test-report.md`

- [ ] **Step 1: 在交付状态中登记 provider 治理实施项**

- [ ] **Step 2: 在测试报告中登记灰度检查项**

- [ ] **Step 3: 跑最小验证**

Run:

- `cd backend && mvn -q -Dtest=LlmProviderCapabilityRegistryTest,LlmPreprocessConfigApiIntegrationTest test`
- `cd frontend && npm test -- src/pages/__tests__/SystemPage.providerCapability.test.jsx && npm run build`

Expected:

- provider 治理的后端与前端基础门禁通过，后续可进入灰度实现。

