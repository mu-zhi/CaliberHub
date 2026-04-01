# OpenAI Responses 预处理适配实施计划

> **For agentic workers:** 默认按 `subagent-driven-development（子代理驱动开发技能）` 执行；进入实现前先跑 `test-driven-development（测试驱动开发技能）`，实现完成后必须走 `requesting-code-review（请求代码审查技能）`、`code-reviewing（代码检视技能）` 与 `feature-test-report（特性测试与验收技能）`。

**Feature Doc:** `docs/architecture/features/iteration-01-knowledge-production/02b-openai-responses-preprocess-adapter.md`

**Goal:** 在不破坏现有兼容型 `chat/completions` provider 的前提下，为导入预处理链路补 OpenAI `Responses API（响应式接口）` 适配器、统一结果解析器与错误映射。

**Scope:** 仅覆盖 transport / parse 适配，不改 Prompt 业务语义、不改系统管理页视觉结构、不做 provider 发布门禁。

**Preconditions:**

1. 以 `02b-openai-responses-preprocess-adapter.md` 为唯一特性真源。
2. 现有 `LlmPreprocessSupportImpl` 与系统管理测试接口保持为主消费面。
3. 先补失败测试和回放样例，再写 OpenAI 适配实现。

---

## Task 1: 先写 OpenAI 路径的失败契约测试

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/LlmPreprocessConfigApiIntegrationTest.java`
- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/infrastructure/module/supportimpl/application/OpenAiResponsesPreprocessSupportTest.java`

- [ ] **Step 1: 为 OpenAI provider 增加失败测试**

补以下断言：

```java
assertThat(requestBody).contains("\"input\"");
assertThat(requestBody).contains("\"instructions\"");
assertThat(requestBody).doesNotContain("\"messages\"");
```

- [ ] **Step 2: 运行红灯测试**

Run:

- `cd backend && mvn -q -Dtest=LlmPreprocessConfigApiIntegrationTest,OpenAiResponsesPreprocessSupportTest test`

Expected:

- 测试失败，提示 OpenAI provider 路径仍在使用旧的 `chat/completions` 请求结构或缺少专用适配器。

## Task 2: 引入 provider 感知的预处理适配器

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/support/LlmPreprocessSupport.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/supportimpl/application/LlmPreprocessSupportImpl.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/supportimpl/application/OpenAiResponsesRequestMapper.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/supportimpl/application/OpenAiResponsesResponseMapper.java`

- [ ] **Step 1: 抽出统一域请求和 provider 分支**

要求 `LlmPreprocessSupportImpl` 先判断 provider capability，再路由到 OpenAI 或兼容型适配器。

- [ ] **Step 2: 实现 OpenAI Responses 请求映射**

最小要求：

```java
instructions
input
text.format / 等价结构化输出配置
model
```

- [ ] **Step 3: 实现统一响应解析**

把 OpenAI 响应映射回当前预处理测试和导入主链路消费的统一结果对象。

- [ ] **Step 4: 复跑测试**

Run:

- `cd backend && mvn -q -Dtest=OpenAiResponsesPreprocessSupportTest test`

Expected:

- OpenAI 请求与响应映射断言通过；集成测试如仍失败，只剩错误码或配置门禁问题。

## Task 3: 补齐错误映射与回退语义

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/LlmPreprocessConfigCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/LlmPreprocessProperties.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/SystemController.java`

- [ ] **Step 1: 增加 provider 不兼容与结构化返回失败的受控错误码**

- [ ] **Step 2: 确保系统管理测试接口能区分 OpenAI 路径与兼容 provider 路径**

- [ ] **Step 3: 复跑定向集成测试**

Run:

- `cd backend && mvn -q -Dtest=LlmPreprocessConfigApiIntegrationTest test`

Expected:

- 系统管理测试接口能返回受控结果，不泄露 OpenAI 原始错误体。

## Task 4: 记录迁移状态并做最小回归

**Files:**

- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 把本特性实施状态写回交付状态**

- [ ] **Step 2: 运行最小回归**

Run:

- `cd backend && mvn -q -Dtest=LlmPreprocessConfigApiIntegrationTest,OpenAiResponsesPreprocessSupportTest test`

Expected:

- OpenAI 路径和兼容型路径定向测试均通过。

