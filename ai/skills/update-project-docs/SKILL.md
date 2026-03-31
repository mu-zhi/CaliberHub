---
name: update-project-docs
description: "用于在方案或规则变化后同步项目文档、术语表和导航入口。"
---

# 更新项目文档（update-project-docs）

## Workflow

1. Read `/Users/rlc/LingChao_Ren/1.2、数据直通车/README.md` and use the section `项目文档同步更新规范（12条，生效版）` as the only active rule set.
2. Extract the latest plan deltas from the user request and changed project context.
3. Map each delta with `references/doc-routing-map.md` before editing files.
4. If the delta affects collaboration workflow, shared trigger phrases, or completion evidence, also update `/Users/rlc/LingChao_Ren/1.2、数据直通车/AGENTS.md` and `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/engineering/collaboration-workflow.md`.
5. Update impacted docs in place; never create suffix variants such as `最终版` or `修改版`.
6. Run a full consistency pass across all project markdown docs.
7. Sync `README.md` navigation and structure block when document topology changes.
8. Return a structured sync report.

## Delta Extraction

Normalize each delta to this schema:

- `change_type`: add / modify / remove
- `domain`: design / development / reference
- `impact`: business scope and technical scope
- `phase`: current stage
- `source`: where this delta came from

Ask concise follow-up questions only when `domain` or `impact` cannot be inferred.

## Editing Standards

- Keep one master doc per topic.
- Preserve existing document framing and update the most relevant section.
- Do not mechanically require “目标 / 范围 / 读者 / 当前阶段定位”. Keep only openings that carry real business value.
- Apply terminology from `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/glossary.md`.

## Full Consistency Pass

Scan all `*.md` files under:

- `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs`
- `/Users/rlc/LingChao_Ren/1.2、数据直通车/research`
- `/Users/rlc/LingChao_Ren/1.2、数据直通车/archive`

If the delta changes collaboration workflow or shared triggers, also scan:

- `/Users/rlc/LingChao_Ren/1.2、数据直通车/AGENTS.md`
- `/Users/rlc/LingChao_Ren/1.2、数据直通车/README.md`

Check these items:

- terminology consistency
- strategic narrative consistency (`数据直通车2.0` / `口径治理` / `知识梳理服务`)
- stale or broken internal links
- process-history wording that violates rule 12

## Output Format

Return four sections:

1. `updated_files`: changed files + one-line reason
2. `checked_no_change`: scanned files without edits
3. `rule_exceptions`: unresolved rule conflicts or missing inputs
4. `next_actions`: only when user decisions are required

## Resource

- `references/doc-routing-map.md`: canonical routing rules and sync checklist for this project
