# OpenAI 集成最小迁移计划

## 目标

- 基于最新官方推荐，盘点当前仓库中的 `OpenAI（开放人工智能接口）` / 兼容型 `LLM（大语言模型，Large Language Model）` 集成假设。
- 在尽量不改变现有行为的前提下，给出最小迁移路径。
- 先升级默认 Prompt 与模型/API 选择建议，不直接重写当前运行时调用链。
- 显式标记所有需要人工审核的变更点。

## 官方依据

- [Models](https://developers.openai.com/api/docs/models)：官方当前建议，复杂专业任务优先 `gpt-5.4`；延迟/成本更敏感时优先 `gpt-5-mini`。
- [GPT-5.4](https://developers.openai.com/api/docs/models/gpt-5.4)：`gpt-5.4` 为当前复杂推理与专业工作旗舰模型。
- [Latest model migration guidance](https://developers.openai.com/api/docs/guides/latest-model/#migrating-from-other-models-to-gpt-54)：迁移到 `gpt-5.4` 时，建议先做最小参数变更；从旧模型迁移可先保守关闭额外推理参数。
- [Prompt guidance for GPT-5.4](https://developers.openai.com/api/docs/guides/prompt-guidance/)：Prompt 应明确输出契约、完成标准、工具使用边界和冲突规则。
- [Responses API create](https://developers.openai.com/api/reference/resources/responses/methods/create/)：`Responses API（响应式接口）` 是 GPT-5 系列推荐入口，支持 `instructions`、结构化文本格式、工具调用、`previous_response_id`、`reasoning` 等能力。

## 当前仓库盘点

### 1. 模型假设

- 后端默认模型位于 [application.yml](/Users/rlc/Code/CaliberHub/backend/src/main/resources/application.yml) 与 [LlmPreprocessProperties.java](/Users/rlc/Code/CaliberHub/backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/LlmPreprocessProperties.java)。
- 当前默认模型是 `qwen3-max`，不是 OpenAI 原生模型。
- 当前仓库里没有“面向 OpenAI 单独预设一套默认模型”的分支逻辑。

### 2. Endpoint 假设

- 当前默认 endpoint 是兼容型 `chat/completions` 路径：
  - `https://coding.dashscope.aliyuncs.com/v1/chat/completions`
  - `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`
- 调用实现位于 [LlmPreprocessSupportImpl.java](/Users/rlc/Code/CaliberHub/backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/supportimpl/application/LlmPreprocessSupportImpl.java)。
- 代码当前构造的是 `Chat Completions（聊天补全接口）` 风格请求体，并按相同响应结构解析结果。

### 3. Prompt 假设

- 默认 Prompt 位于：
  - [LlmPromptDefaults.java](/Users/rlc/Code/CaliberHub/backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/LlmPromptDefaults.java)
  - [SystemPage.jsx](/Users/rlc/Code/CaliberHub/frontend/src/pages/SystemPage.jsx)
- 原实现存在前后端口径漂移：
  - 后端要求：同一业务问题下的 Step/时段/历史表切换尽量保持单一 `scene_candidate`。
  - 前端要求：Step/时段分层必须拆成多个 `scene_candidates`。
- 这会直接影响模型输出的一致性与人工配置可解释性。

### 4. 工具假设

- 当前预处理链路未使用 `tool calling（工具调用）` / `function calling（函数调用）`。
- 当前能力假设是“单轮纯 JSON 输出”，依赖 `response_format = json_object` 与文本 Prompt 约束。
- 当前不存在 `previous_response_id`、多阶段工具编排、`phase` 或推理上下文复用能力。

## 最小迁移策略

### 阶段 1：当前轮建议直接落地

- 保持现有兼容型 `chat/completions` 运行时代码不变。
- 升级默认 Prompt，使其符合 GPT-5.4 官方建议：
  - 明确输出契约。
  - 明确完成标准。
  - 明确冲突规则与自检项。
  - 去掉前后端对场景拆分的自相矛盾指令。
- 在仓库内记录 OpenAI 推荐模型与后续 API 迁移方向。

### 阶段 2：人工审核后再做

- 若决定切换到 OpenAI 原生接口，复杂任务默认模型建议设置为 `gpt-5.4`。
- 若后续存在对成本或延迟更敏感的轻量场景，可补充 `gpt-5-mini` 预设。
- 真正迁移到 `Responses API` 时，再为 OpenAI 路径单独增加请求/响应分支，不要直接拿现有兼容型 `chat/completions` 代码硬改 endpoint。

## 特性拆分与执行顺序

本次迁移正式拆成以下三个专题特性，不再按单一大特性推进：

1. [LLM Provider 路由与模型治理](/Users/rlc/Code/CaliberHub/docs/architecture/features/iteration-02-runtime-and-governance/11c-llm-provider-routing-and-model-governance.md)
对应计划：[2026-04-01-llm-provider-routing-and-model-governance-implementation-plan.md](/Users/rlc/Code/CaliberHub/docs/plans/2026-04-01-llm-provider-routing-and-model-governance-implementation-plan.md)
2. [OpenAI Responses 预处理适配](/Users/rlc/Code/CaliberHub/docs/architecture/features/iteration-01-knowledge-production/02b-openai-responses-preprocess-adapter.md)
对应计划：[2026-04-01-openai-responses-preprocess-adapter-implementation-plan.md](/Users/rlc/Code/CaliberHub/docs/plans/2026-04-01-openai-responses-preprocess-adapter-implementation-plan.md)
3. [预处理结构化输出与 Prompt 治理](/Users/rlc/Code/CaliberHub/docs/architecture/features/iteration-01-knowledge-production/02c-preprocess-structured-output-and-prompt-governance.md)
对应计划：[2026-04-01-preprocess-structured-output-and-prompt-governance-implementation-plan.md](/Users/rlc/Code/CaliberHub/docs/plans/2026-04-01-preprocess-structured-output-and-prompt-governance-implementation-plan.md)

推荐实施顺序：

1. 先做 provider 路由与模型治理，先把能力矩阵、参数白名单和切换门禁钉住。
2. 再做 OpenAI `Responses API` 适配，把 transport / parse 路径单独接入。
3. 最后做 Prompt / Schema 治理，把人工审核、结构化校验和默认配置闭环收口。

## 需要人工审核的变更

### A. 是否引入 OpenAI 原生 endpoint

- 当前代码与 `Responses API` 不兼容，不能只改 URL。
- 需要人工审核是否接受以下开发范围：
  - 请求体从 `messages`/`response_format` 改为 `input`/`instructions`/`text.format`。
  - 响应解析从 `choices[0].message.content` 改为 `response output` 结构。
  - 新增 OpenAI 专属参数分支，避免破坏现有兼容型供应商。

### B. 是否切到 Structured Outputs

- 当前使用 `json_object` 风格约束。
- 若切到 OpenAI 原生能力，建议人工评估是否升级为 `json_schema` 级结构化输出。
- 这是接口层与错误处理层面的变更，不应与本轮 Prompt 升级混在一起。

### C. 是否新增 OpenAI 专属模型预设

- 当前默认模型仍是 `qwen3-max`。
- 若要支持“一键切换到 OpenAI 推荐模型”，需要人工决定：
  - 是全局替换默认模型，还是为 OpenAI 增加独立 provider preset。
  - 是否允许不同 provider 继续共享同一套请求字段。

### D. 是否启用 GPT-5 特有参数

- 包括但不限于 `reasoning`、`previous_response_id`、`phase`、工具选择策略。
- 本轮不建议直接启用。
- 需要人工审核哪些能力真的能提升当前“口径文档预处理”场景，而不是增加不可控变量。

## 本轮已执行的最小落地

- 对齐并升级前后端默认 Prompt。
- 保持当前运行时 `chat/completions` 行为不变。
- 将“OpenAI 最新模型与 API 推荐路径”写入仓库内计划文档。
- 用后端集成测试锁定新的 Prompt 契约。
- 已把迁移方案拆成三个正式专题特性，并同步产出对应实施计划。

## 后续建议执行顺序

1. 先观察更新后的 Prompt 是否在现有兼容型模型上保持输出稳定。
2. 若要接入 OpenAI 原生模型，先新增 provider 级配置分支，而不是直接替换现有默认 endpoint。
3. 单独开一个实施任务，把 `Responses API` 请求构造、响应解析、错误映射和回归测试一起迁移。
