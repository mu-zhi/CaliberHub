---
name: writing-plans
description: "用于把已确认设计拆成可执行小任务；在自动化链路中允许直接生成实施计划，并明确主模型、备选模型和状态同步要求。"
---

# 实施计划编写（writing-plans）

## Preconditions

- 设计已确认
- 对应特性文档已存在并满足最低完备性

## Workflow

1. 先读取对应特性文档，确认目标、边界、页面或接口承载点。
2. 如果特性文档来自自动补写链路，先确认 `feature-doc-authoring` 的门禁结果为 `通过`。
3. 将工作拆成 2 至 5 分钟级别的小任务。
4. 每个任务必须写清：
   - 精确文件路径
   - 需要落地的代码或改动要点
   - 测试命令
   - 预期输出
5. 计划文件统一写入 `docs/plans/`，命名为 `YYYY-MM-DD-<topic>-implementation-plan.md`。
6. 计划文件开头必须显式引用特性文档路径。
7. 计划落地后同步 `docs/engineering/current-delivery-status.md` 的：
   - `来源计划`
   - `下一动作`
   - `最后更新时间`

## Model routing

自动写实施计划固定采用以下模型约定：

1. 主模型：`gpt-5.4 + xhigh`
2. 备选模型：`claude-sonnet-4-6-20260218`
3. 计划复核主模型：`claude-sonnet-4-6-20260218`
4. 计划复核备选：`gpt-5.4 + xhigh`

若使用本地 `Claude Code` 作为备选链路，统一走：

```bash
bash scripts/claude_mux.sh wenwen claude-sonnet-4-6-20260218 --print "<prompt>"
```

## Guardrails

1. 没有特性文档，不写计划。
2. 特性文档门禁未通过，不写计划。
3. 不允许使用“后续补充”“类似处理”等占位描述。
4. 如果任务跨前后端或影响契约，显式写出验证链路。
