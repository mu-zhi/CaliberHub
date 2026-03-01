---
name: update-project-docs
description: "Synchronize 数据直通车 project documents after plan changes. Use when user says '更新项目文档' or asks to update/sync project docs by the latest README rules, classify new方案内容 into design/development/reference docs, and run a full-document consistency pass."
---

# Update Project Docs

## Workflow

1. Read `/Users/rlc/Code/数据直通车/README.md` and use the section `项目文档同步更新规范（12条，生效版）` as the only active rule set.
2. Extract the latest plan deltas from the user request and changed project context.
3. Map each delta with `references/doc-routing-map.md` before editing files.
4. Update impacted docs in place; never create suffix variants such as `最终版` or `修改版`.
5. Run a full consistency pass across all project markdown docs.
6. Sync `README.md` navigation and structure block when document topology changes.
7. Return a structured sync report.

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
- Ensure each updated main doc has explicit: 目标、范围、读者、当前阶段定位.
- Apply terminology from `/Users/rlc/Code/数据直通车/01-设计文档/08-数据直通车-统一术语表.md`.

## Full Consistency Pass

Scan all `*.md` files under:

- `/Users/rlc/Code/数据直通车/01-设计文档`
- `/Users/rlc/Code/数据直通车/02-开发文档`
- `/Users/rlc/Code/数据直通车/03-其他文档`

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
