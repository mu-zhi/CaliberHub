# LLM Provider 路由与模型治理特性文档

> 迭代归属：迭代二运行与治理工作台
> 来源主文档：`system-design.md` §4.4.7、§11.5、§12.3；`frontend-workbench-design.md` 当前无独立系统管理总纲，前端承载以系统管理页为准
> 对应实施计划：`docs/plans/2026-04-01-llm-provider-routing-and-model-governance-implementation-plan.md`

# 一、特性概述

## 1.1 背景

当前系统管理中的 LLM 配置仍以单一 endpoint、单一模型、单一请求语义为中心设计。这种配置方式适合早期兼容型 provider，但不适合同时承接 OpenAI 原生 `Responses API` 与现有兼容 provider，也无法表达“哪个模型支持哪些结构化输出能力、哪些参数只允许某 provider 使用、哪些切换需要灰度观察”。

## 1.2 目标

本特性用于把导入预处理链路相关的 provider 预设、模型能力矩阵、参数白名单、切换门禁、灰度验证和运行观测收口成一个正式治理场景，确保 OpenAI provider 的引入不等于“把现有配置页改成多几个输入框”。

# 二、指标

1. provider 预设合法性达到 100%。统计口径：分母为系统管理中可保存的 provider / model 组合数，分子为具备能力矩阵、参数白名单和 endpoint 校验的组合数，统计窗口为迁移首轮支持的全部 provider。
2. 不兼容参数误发次数为 0。统计口径：分母为所有 provider 测试调用与正式调用次数，分子为把不支持参数发往 provider 的次数，统计窗口为迁移灰度期。
3. provider 切换可追溯率达到 100%。统计口径：分母为 provider 或模型切换次数，分子为带有操作者、变更摘要、灰度结果和回退指向的次数，统计窗口为迁移实施周期。

# 三、特性全景

## 3.1 特性全景描述

本特性只覆盖“LLM provider 路由与模型治理”一个场景。它位于系统管理和运行治理层，负责 provider 预设、模型能力矩阵、切换门禁、灰度验证和观测指标，不负责具体 Prompt 内容，也不负责 OpenAI 响应解析细节。

## 3.2 特性图示

结构固定为“系统管理页 provider 预设 -> 能力矩阵校验 -> 参数白名单 -> 测试调用 / 灰度调用 -> 观测与回退”。

# 四、特性说明

## 4.1 原型（非必要）

前端主承载为系统管理页中的大模型配置区域；运行观测与灰度结果沉淀在监控与审计工作台，不新增业务工作台页面。

## 4.2 功能说明

### 4.2.1 特性场景1

通用补充项：
- 主对象：provider 预设、模型能力矩阵、参数白名单、切换记录、灰度结果、回退指针。
- 默认时间语义：不适用；本场景只治理 provider 与模型，不解释业务时间。
- 覆盖范围和缺口：覆盖 provider/model 配置、能力矩阵、参数门禁、灰度与回退；不覆盖 Prompt 语义、不覆盖具体 `Responses API` 字段映射。
- 策略或审批边界：高影响切换必须先走测试或灰度，不允许未验证直接成为默认 provider；管理员是唯一可变更角色。
- 前端入口、详情页和跳转链路：入口为“系统管理 -> 大模型配置”；灰度结果与告警在“监控审计与影响分析”查看；不新增业务详情页。
- 接口或数据边界：输入为 provider / model / capability 配置、测试请求和切换动作，输出为合法性校验、测试结果、灰度状态和审计记录；不改变导入业务请求协议。
- 存储落位或持久化边界：provider 预设、能力矩阵、切换记录与灰度状态落关系型控制库与审计表；不新增业务知识资产。

1. 系统必须显式区分“provider 预设”和“Prompt 配置”，不得把 endpoint、模型名、Schema 能力和 Prompt 文本混在同一组无语义字段中。
2. 每个 provider / model 组合都必须声明能力矩阵，例如是否支持 `Responses API`、是否支持结构化输出、是否支持推理参数、是否支持兼容型 `chat/completions`。
3. 参数白名单必须按 provider / model 生效，任何不兼容参数都必须在发请求前阻断并返回明确错误。
4. OpenAI provider 的默认复杂任务模型建议为 `gpt-5.4`，延迟 / 成本敏感场景可另配 `gpt-5-mini`；但是否成为默认值，必须经过灰度验证。
5. provider 或模型切换必须留下变更人、变更时间、变更摘要、测试结论、灰度结论和回退目标。
6. 本场景与 [02b-openai-responses-preprocess-adapter.md](../iteration-01-knowledge-production/02b-openai-responses-preprocess-adapter.md) 的边界是：`02b` 负责 transport 适配，本场景负责 provider 治理与切换门禁。
7. 本场景与 [02c-preprocess-structured-output-and-prompt-governance.md](../iteration-01-knowledge-production/02c-preprocess-structured-output-and-prompt-governance.md) 的边界是：本场景不定义 Prompt 主语义和 Schema 内容，只校验 provider 是否支持对应能力。
8. 本场景结束后，系统得到的是“可切换、可观测、可回退的 provider 治理闭环”，而不是业务输出质量本身。

