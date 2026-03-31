---
name: requesting-code-review
description: "用于任务之间的代码审查门禁，优先检查规格符合性、回归风险、契约漂移、缺失验证和缺失文档。"
---

# 请求代码审查（requesting-code-review）

## Scope

适用于单任务完成后的任务间门禁，不替代所有任务完成后的整体代码检视（`code-reviewing`）。

## Workflow

1. 在当前任务完成后、进入下一个任务前触发。
2. 默认优先使用 `claude-sonnet-4-6-20260218` 做只读快速审查，并优先带 `review` skill。
3. 先做规格符合性检查：
   - 是否满足特性文档与计划要求
   - 是否引入行为偏移
4. 再触发 `change-review` 做代码质量与风险检查。
5. 若审查提出问题，先修复并重新审查，不进入下一任务。
6. 若审查结论影响状态、阻塞项或下一动作，更新 `docs/engineering/current-delivery-status.md` 的当前工作项事实字段。

## Model routing

1. 主模型：`claude-sonnet-4-6-20260218`
2. 主入口：`bash scripts/claude_review.sh`
3. 优先 skill：`review`
4. 备选模型：`gpt-5.4 + xhigh`

## Output

Return:

1. 已检查范围
2. 发现的问题
3. 是否允许进入下一任务
