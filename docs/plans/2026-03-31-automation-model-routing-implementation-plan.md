# Automation Model Routing Implementation Plan

> **For agentic workers:** 默认按 `subagent-driven-development（子代理驱动开发技能）` 推进；这是一次协作治理与自动化规则变更，不涉及业务服务实现，但仍要求“设计 -> 计划 -> 配置 / 文档落地 -> 校验”闭环。

**Goal:** 把“补特性文档、写实施计划、推进开发计划、代码检视”的模型分工正式落到仓库内自动化规则、阶段路由配置与协作文档中。

**Architecture:** 采用“一个阶段路由配置文件 + 多个 skill（技能）约束 + 协作工作流总纲 + 本地 Claude Code 使用说明”的结构。`ai/project/agents/model-routing.json` 承载阶段级主模型 / 备选模型与运行时边界；各 skill 明确可自动写入哪些正式产物；协作工作流文档负责把这些规则收口成共享协议。

**Tech Stack:** `JSON（结构化配置格式）`、`Markdown（轻量标记语言）`、项目级 `skill（技能）` 文档、`Claude Code（代码智能体）` 本地入口脚本

---

### Task 1: 写正式设计与计划真源

**Files:**
- Create: `docs/plans/2026-03-31-automation-model-routing-design.md`
- Create: `docs/plans/2026-03-31-automation-model-routing-implementation-plan.md`

- [ ] **Step 1: 写设计稿**

写明以下内容：

1. 两层运行时：`Codex runtime` 与 `Claude Code runtime`
2. 四个阶段：补特性文档、写实施计划、推进开发计划、代码检视
3. 每个阶段的主模型、备选模型、推理强度与回退方式
4. 自动化允许写入与禁止写入的正式产物边界

- [ ] **Step 2: 写实施计划**

计划至少覆盖：

1. 更新 `ai/project/agents/model-routing.json`
2. 更新自动化相关 skill
3. 更新协作工作流文档
4. 更新本地 Claude provider 使用说明
5. 更新交付状态

### Task 2: 更新阶段路由配置

**Files:**
- Modify: `ai/project/agents/model-routing.json`

- [ ] **Step 1: 将配置升级为阶段级路由 + 构建子任务路由**

配置至少包含：

1. `workflow_routes`
2. `build_subagent_routing`
3. `notes`

并显式写出：

```json
"feature_doc_authoring": {
  "primary": { "runtime": "codex", "model": "gpt-5.4", "reasoning_effort": "xhigh" },
  "fallback": { "runtime": "claude_code", "model": "claude-sonnet-4-6-20260218" }
}
```

- [ ] **Step 2: 为推进开发计划写清升级条件**

在 `plan_execution` 或 `build_subagent_routing` 中写明：

1. 默认 `gpt-5.2-codex + high`
2. 高风险时升级到 `gpt-5.2-codex + xhigh`
3. 备选为 `qwen3-coder-plus`

- [ ] **Step 3: 验证 JSON 结构**

Run: `python3 -m json.tool ai/project/agents/model-routing.json >/dev/null`

Expected: 命令退出码 `0`

### Task 3: 更新自动化 skill 规则

**Files:**
- Modify: `ai/skills/semi-automatic-multi-agent-orchestration/SKILL.md`
- Modify: `ai/skills/feature-doc-coverage-mapping/SKILL.md`
- Modify: `ai/skills/feature-doc-authoring/SKILL.md`
- Modify: `ai/skills/writing-plans/SKILL.md`
- Modify: `ai/skills/subagent-driven-development/SKILL.md`
- Modify: `ai/skills/executing-plans/SKILL.md`
- Modify: `ai/skills/requesting-code-review/SKILL.md`
- Modify: `ai/skills/code-reviewing/SKILL.md`

- [ ] **Step 1: 让半自动编排 skill 承认自动写入边界**

至少写明：

1. 自动化允许补特性文档、写实施计划、推进计划
2. 自动化只允许更新当前工作项的事实字段
3. `runtime = codex` 与 `runtime = claude_code` 的区别

- [ ] **Step 2: 让 `feature-doc-authoring` 支持 authoring mode**

至少写明：

1. `gate mode`
2. `authoring mode`
3. 自动补文档后必须再跑一次门禁

- [ ] **Step 3: 给计划与执行 skill 写入模型路由**

至少写明：

1. `writing-plans` 主模型 `gpt-5.4 + xhigh`
2. `subagent-driven-development` / `executing-plans` 主模型 `gpt-5.2-codex + high`
3. `requesting-code-review` / `code-reviewing` 主模型 `claude-sonnet-4-6-20260218`，优先 `review`

### Task 4: 更新共享协作协议与工具说明

**Files:**
- Modify: `docs/engineering/collaboration-workflow.md`
- Modify: `tooling/claude-providers/README.md`

- [ ] **Step 1: 在协作工作流中写明自动化行为升级**

至少写明：

1. `planning` 阶段若特性文档缺失，先进入补文档链路
2. 特性文档与实施计划可以由自动化直接产出
3. 代码检视优先 `Claude Sonnet 4.6`

- [ ] **Step 2: 在本地 Claude provider 文档中写明自动化主模型 / 本地备选入口**

至少写明：

1. 补特性文档
2. 写实施计划
3. 推进开发计划
4. 代码检视

### Task 5: 同步交付状态并做最终校验

**Files:**
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 新增当前进行中的治理项**

记录：

1. 来源设计
2. 来源计划
3. 当前状态
4. 最新完成
5. 下一动作

- [ ] **Step 2: 运行最小校验**

Run:

```bash
python3 -m json.tool ai/project/agents/model-routing.json >/dev/null
rg -n "gpt-5.4|gpt-5.2-codex|claude-sonnet-4-6-20260218|qwen3-coder-plus" \
  ai/skills docs/engineering tooling/claude-providers/README.md
```

Expected:

1. `model-routing.json` 解析通过
2. 关键模型与阶段约定已出现在对应 skill、协作文档与工具说明中
