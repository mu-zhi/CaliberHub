---
name: code-reviewing
description: "用于实现完成后的代码检视，由 Claude Code 执行只读评审，检视通过后才进入测试；未通过则回到 Codex 修复并复检。"
---

# 代码检视（code-reviewing）

## Use when

- Codex 已完成当前批实现
- 准备进入测试前的质量门禁
- 不适用于任务之间的快速门禁；任务间快速门禁由 `requesting-code-review` 承担

## Workflow

1. 默认由 `Claude Code` 执行只读评审，优先入口为 `bash scripts/claude_review.sh`。
2. 代码检视优先使用 `claude-sonnet-4-6-20260218`，并优先带 `review` skill。
3. 重点检查：
   - 规格符合性
   - 回归风险
   - 契约漂移
   - 缺失验证
   - 缺失文档
4. 输出 findings first，问题关闭前不进入测试。
5. 若存在问题，交还 `Codex` 修复。
6. 修复后再次由 `Claude Code` 复检。
7. 复检通过后，才允许进入 `feature-test-report` / `reviewing`。

## Model routing

1. 主模型：`claude-sonnet-4-6-20260218`
2. 主入口：`bash scripts/claude_review.sh`
3. 优先 skill：`review`
4. 备选模型：`gpt-5.4 + xhigh`

## Guardrails

1. 代码检视阶段不直接修改生产代码。
2. 代码检视与测试阶段是两个门禁，不混用。
3. 若问问 provider 不可用、超时或额度不足，再退回 `Codex gpt-5.4 + xhigh`，不先重复撞同一出口。
