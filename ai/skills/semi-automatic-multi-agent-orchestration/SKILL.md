---
name: semi-automatic-multi-agent-orchestration
description: "用于规则触发的半自动多智能体协作编排：按状态流转自动推荐或唤起设计、计划、实现、评审四类角色智能体，并在门禁下允许自动补文档、写计划与推进执行。"
---

# 半自动多智能体编排（semi-automatic-multi-agent-orchestration）

## Goal

将项目协作从“单智能体串行执行”提升为“按状态流转触发固定角色智能体”，同时保持以下边界：

- 状态真源仍以 `docs/engineering/current-delivery-status.md` 为准
- 自动化允许在门禁下直接补特性文档、写实施计划、推进开发计划，并更新当前工作项的事实字段
- 自动化不自动新建工作项，不自动重写 `来源设计`、`来源计划` 等来源链接
- 用户可以随时中断、覆盖或改用单智能体推进

## Role model

固定角色只分四类，不按细碎专题无限拆分：

1. `Design Agent（设计智能体）`
   - 负责：`brainstorming`、`feature-doc-coverage-mapping`、`feature-doc-authoring`
   - 适用状态：`proposed`、`brainstorming`、`planning（特性文档缺口补齐）`
2. `Plan Agent（计划智能体）`
   - 负责：`writing-plans`
   - 适用状态：`planning（实施计划编写）`
3. `Build Agent（实现智能体）`
   - 负责：`subagent-driven-development`、`executing-plans`、`test-driven-development`
   - 适用状态：`implementing`、`fixing`
4. `Review Agent（评审智能体）`
   - 负责：`requesting-code-review`、`code-reviewing`、`feature-test-report`、`change-review`
   - 适用状态：`reviewing`

## Trigger rules

按状态流转推荐或唤起角色，不做隐式无限扩张：

1. 工作项进入 `brainstorming` 时，优先切换到 `Design Agent`。
2. 工作项进入 `planning` 且特性文档缺失、映射不清或门禁未通过时，优先切换到 `Design Agent`。
3. 工作项进入 `planning` 且特性文档已通过门禁时，优先切换到 `Plan Agent`。
4. 工作项进入 `implementing` 时，优先切换到 `Build Agent`。
5. 工作项进入 `reviewing` 时，优先切换到 `Review Agent`。
6. 工作项因 `P0 / P1` 缺陷进入 `fixing` 时，回到 `Build Agent`；修复完成后再切回 `Review Agent`。

## Automatic write boundaries

自动化允许直接写入的正式产物固定为：

1. `docs/architecture/features/` 下目标特性文档
2. `docs/plans/` 下目标实施计划
3. `docs/engineering/current-delivery-status.md` 中当前工作项的以下字段：
   - `当前状态`
   - `最新完成`
   - `下一动作`
   - `最后更新时间`

自动化禁止直接做的事情：

1. 自动新建 `current-delivery-status.md` 工作项
2. 自动重写 `来源设计`、`来源计划` 与 `退出条件`
3. 绕过 `feature-doc-authoring` 门禁直接写实施计划
4. 绕过 `code-reviewing` 直接进入测试

## Semi-automatic boundaries

1. 半自动触发只在状态清晰、角色固定时生效。
2. 如果任务规模过小、上下文极强或用户明确要求单智能体推进，可以跳过多智能体编排。
3. 自动触发不替代门禁；门禁仍由对应 skill 与规则文档执行。
4. 若自动触发与用户即时指令冲突，以用户即时指令为准。
5. 若自动化发现主文档、特性文档、实施计划或状态真源之间存在冲突，先输出问题项并暂停写入，不自动覆盖冲突来源。

## Model routing

阶段级模型路由固定读取 `ai/project/agents/model-routing.json`，并区分两类运行时：

1. `runtime = codex`
   - 可直接用于当前 Codex 会话与 `spawn_agent`
2. `runtime = claude_code`
   - 必须通过本地脚本执行，例如：
     - `bash scripts/claude_review.sh`
     - `bash scripts/claude-coder.sh`
     - `bash scripts/claude_mux.sh <provider> <model>`

配置结构约定：

1. `workflow_routes`
   - 用于补特性文档、写实施计划、推进开发计划与代码检视
2. `build_subagent_routing`
   - 用于 `Build Agent` 在 `spawn_agent` 场景下的复杂度评分与 `Codex` 子代理路由

默认阶段路由如下：

1. `Design Agent`
   - 主模型：`gpt-5.4 + xhigh`
   - 备选：`claude-sonnet-4-6-20260218`
   - 文档复核：`claude-sonnet-4-6-20260218`
2. `Plan Agent`
   - 主模型：`gpt-5.4 + xhigh`
   - 备选：`claude-sonnet-4-6-20260218`
   - 计划复核：`claude-sonnet-4-6-20260218`
3. `Build Agent`
   - 主模型：`gpt-5.2-codex + high`
   - 高风险升级：`gpt-5.2-codex + xhigh`
   - 备选：`qwen3-coder-plus`
4. `Review Agent`
   - 主模型：`claude-sonnet-4-6-20260218`
   - 优先 skill：`review`
   - 备选：`gpt-5.4 + xhigh`

## Use when

- 用户明确提出“多智能体协作”“自动创建智能体”“半自动编排”
- 任务已经进入有明确状态和分工的执行阶段
- 需要把 `Codex` 与 `Claude Code` 的角色边界、自动写入边界和模型路由固定下来
