# Automation Model Routing Design

## 背景

当前项目已经明确了不同环节应采用不同模型：

1. 补充特性文档与实施计划，优先使用强模型
2. 推进开发计划，优先使用日常编码模型
3. 代码检视，优先使用 `Claude Sonnet 4.6`

但仓库内现状仍有两个问题：

1. 自动化规则主要停留在“推荐角色、触发 skill（技能）”，尚未把“允许自动补文档 / 自动写计划 / 自动推进任务”的行为边界正式写清。
2. 现有 `ai/project/agents/model-routing.json` 只覆盖 `Build Agent（实现智能体）` 的复杂度路由，没有覆盖“特性文档补写 / 实施计划生成 / 计划推进 / 代码检视”这些阶段级自动化。

本轮目标是在不破坏现有协作门禁的前提下，把自动化从“只提醒”升级为“可闭环产出”，并把每个阶段的主模型、备选模型、推理强度与回退方式写成正式规则。

## 目标

1. 允许自动化直接补充或新建目标特性文档。
2. 允许自动化直接生成 `docs/plans/` 下的实施计划。
3. 允许自动化按计划推进开发任务，并保留现有 `TDD（测试驱动开发，Test-Driven Development）`、任务间审查和代码检视门禁。
4. 允许自动化更新当前工作项在 `docs/engineering/current-delivery-status.md` 中的事实字段，但不允许自动新建工作项或重写来源链接。
5. 明确区分两类执行运行时：
   - `Codex runtime（当前 Codex / 子代理运行时）`
   - `Claude Code runtime（通过本地脚本调用的 Claude Code 运行时）`

## 设计

### 1. 两层运行时

自动化模型路由统一区分为两层：

1. `Codex runtime`
   - 直接用于当前 Codex 会话与 `spawn_agent`
   - 可显式传递 `model` 与 `reasoning_effort`
   - 适合承担补特性文档、写实施计划与推进开发计划的主路径
2. `Claude Code runtime`
   - 通过仓库内脚本触发：
     - `bash scripts/claude_review.sh`
     - `bash scripts/claude-coder.sh`
     - `bash scripts/claude_mux.sh <provider> <model>`
   - 适合作为本地补位链路，尤其是 `Claude Sonnet 4.6` 代码检视与 `Qwen3 Coder Plus` 编码备选

约束：

1. 只有 `runtime = codex` 的路由项可以直接映射到 `spawn_agent`
2. `runtime = claude_code` 的路由项必须通过本地脚本执行
3. 所有共享规则必须显式标注运行时，避免把“Codex 子代理可用模型”和“本地 Claude Code 可用模型”混为一谈

### 2. 阶段级路由

#### 2.1 补特性文档

自动链路固定为：

`feature-doc-coverage-mapping -> feature-doc-authoring（authoring mode） -> feature-doc-authoring（gate mode）`

模型约定：

- 主模型：`gpt-5.4 + xhigh`
- 备选模型：`claude-sonnet-4-6-20260218`
- 校验主模型：`claude-sonnet-4-6-20260218`
- 校验备选：`gpt-5.4 + xhigh`

行为边界：

1. `feature-doc-coverage-mapping` 继续负责映射、归属与缺口判断，不直接补正文。
2. `feature-doc-authoring` 在 `authoring mode` 下允许直接新建或更新目标特性文档。
3. 自动补文档后必须立刻再跑一次门禁检查，不允许“写完即视为通过”。

#### 2.2 写实施计划

自动链路固定为：

`feature-doc-authoring（passed） -> writing-plans`

模型约定：

- 主模型：`gpt-5.4 + xhigh`
- 备选模型：`claude-sonnet-4-6-20260218`
- 计划复核主模型：`claude-sonnet-4-6-20260218`
- 计划复核备选：`gpt-5.4 + xhigh`

行为边界：

1. 实施计划仍必须引用对应特性文档。
2. 如果特性文档门禁未通过，不允许自动写计划。
3. 计划生成完成后允许自动同步当前工作项的 `来源计划`、`下一动作` 与 `最后更新时间`。

#### 2.3 推进开发计划

自动链路固定为：

`subagent-driven-development` 或 `executing-plans`

模型约定：

- 主模型：`gpt-5.2-codex + high`
- 升级主模型：`gpt-5.2-codex + xhigh`
- 备选模型：`qwen3-coder-plus`

升级条件：

1. 跨 `5` 个以上文件
2. 同一问题两轮修复仍失败
3. 涉及公共接口、数据契约或核心状态机
4. 需要先读大量上下文再改动

行为边界：

1. 仍强制 `TDD`
2. 仍强制任务间审查
3. 允许自动推进到下一个未关闭任务，但不允许并行推进多个未关闭问题的任务
4. 自动化只允许更新当前工作项的事实字段，不自动改 `来源设计`、`来源计划`、`退出条件`

#### 2.4 代码检视

自动链路固定为：

`requesting-code-review -> code-reviewing`

模型约定：

- 主模型：`claude-sonnet-4-6-20260218`
- 主入口：`bash scripts/claude_review.sh`
- 主 skill：`review`
- 备选模型：`gpt-5.4 + xhigh`

行为边界：

1. 任务间快速门禁与整体代码检视都优先使用 `Claude Sonnet 4.6`
2. 若问问 provider 不可用、超时或额度不足，才切回 `Codex gpt-5.4 + xhigh`
3. 代码检视阶段仍禁止直接修改生产代码

### 3. 自动化写入边界

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
2. 自动重写 `来源设计`、`来源计划` 链接
3. 绕过 `feature-doc-authoring` 门禁直接写计划
4. 绕过 `code-reviewing` 直接进入测试

### 4. 配置与文档落点

本轮调整的正式落点如下：

1. `ai/project/agents/model-routing.json`
2. `ai/skills/semi-automatic-multi-agent-orchestration/SKILL.md`
3. `ai/skills/feature-doc-coverage-mapping/SKILL.md`
4. `ai/skills/feature-doc-authoring/SKILL.md`
5. `ai/skills/writing-plans/SKILL.md`
6. `ai/skills/subagent-driven-development/SKILL.md`
7. `ai/skills/executing-plans/SKILL.md`
8. `ai/skills/requesting-code-review/SKILL.md`
9. `ai/skills/code-reviewing/SKILL.md`
10. `docs/engineering/collaboration-workflow.md`
11. `tooling/claude-providers/README.md`
12. `docs/engineering/current-delivery-status.md`

## 验收标准

1. 阶段级自动化路由已正式写入仓库文档与配置，而不是只存在于聊天记录。
2. 自动化边界已从“只推荐角色”升级为“可按门禁直接补特性文档、写实施计划、推进计划执行”。
3. `Design Agent / Plan Agent / Build Agent / Review Agent` 均有明确主模型、备选模型和回退方式。
4. `Codex runtime` 与 `Claude Code runtime` 的职责边界清晰，不再混淆可直接 `spawn_agent` 的模型和只能通过本地脚本调用的模型。
