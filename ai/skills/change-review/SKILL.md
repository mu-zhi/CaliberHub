---
name: change-review
description: "用于只读评审现有改动，优先识别回归风险、契约漂移、缺失验证与缺失文档。"
---

# 变更评审（change-review）

Use this skill for read-only review. The goal is to find real delivery risk, not to restyle code or re-implement the change.

## Workflow

1. Determine review scope from the current diff, changed files, or the files named by the user.
2. Check four areas first:
   - regression risk
   - contract drift between implementation and exposed behavior
   - missing verification or missing tests
   - missing documentation or sync fallout
3. If the change touches controllers, DTOs, or API behavior, verify whether `SpringDoc（接口文档生成框架，SpringDoc OpenAPI）` / `OpenAPI（开放接口描述规范，OpenAPI Specification）` output and supplementary notes should also change.
4. If the change touches frontend navigation, layout, or business copy, read `docs/architecture/frontend-workbench-design.md` before concluding.
5. Return findings first, ordered by severity, with file and line references whenever possible.

## Review rules

1. Do not edit files while using this skill.
2. Prioritize bugs, regressions, broken assumptions, and missing checks over style comments.
3. Call out missing doc sync when implementation changes should have updated `README.md`, `AGENTS.md`, `docs/engineering/collaboration-workflow.md`, or generated API docs.
4. If no findings are discovered, say so explicitly and mention any residual risks or testing gaps.
