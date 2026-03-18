---
name: update-project-docs
description: Synchronize project documents after plan changes, keeping glossary, links, and bilingual term annotations consistent.
metadata:
  author: codex
  version: "1.1.1"
  argument-hint: <changed-docs-or-topic>
---

# Update Project Docs

Use this skill when the user asks to update project documents, synchronize plans, or apply documentation governance rules across the repository.

## Required workflow

1. Read the 12-item doc sync rules and the 6-step update flow in `README.md`.
2. Classify changes into `01-设计文档`、`02-开发文档`、`03-其他文档` before editing.
3. If the document is business-facing, scenario-facing, or feature-facing, also read `02-开发文档/开发规范及要求/应用场景类特性文档编写标准.md` and absorb its writing style.
4. Update the target documents.
5. Scan linked docs for terminology, wording, link consistency, and writing-style consistency.
6. Run an explicit “去 AI 味” scan before closing.
7. Update `README.md` navigation if file names, order, or document roles changed.
8. Report:
   - updated files
   - core changes
   - checked but unchanged files

## Terminology governance

1. Every English term, abbreviation, variable, constant, and metric in documentation must carry a Chinese explanation.
2. Apply the rule in正文、表格、图注、样例字段说明, not only in the first occurrence.
3. If a new English term, abbreviation, variable, constant, or metric is not already defined in `01-设计文档/02-统一术语表.md`, add it there before or together with the main document change.
4. When standard `JSON（JavaScript对象表示法）` cannot hold inline comments, use one of:
   - `JSONC（带注释的JSON展示格式）`
   - a field description table
   - paragraph-based field explanations

## Scope guardrails

1. Do not create parallel “final/fixed/new” document versions.
2. Keep one master document per topic.
3. Remove stale links when renaming, moving, adding, or deleting documents.
4. Preserve current-valid information only; do not keep migration narrative in main docs unless explicitly requested.
5. Do not mechanically prepend “目标 / 范围 / 读者 / 当前阶段定位”. Add a short opening note only when it carries real business value.

## Business writing rules

When writing or revising project documents, prefer business-style writing over tutorial-style or model-style prose.

1. Write around business facts, boundaries, rules, paths, evidence, versions, and responsibilities.
2. Use the project’s real objects and real scene names when examples are needed; avoid generic placeholders such as “张三 / 项目A / 客户B” unless the user explicitly wants generic teaching examples.
3. For scenario or feature documents, prioritize this order when applicable:
   - 背景 / 目标
   - 适用范围 / 不适用范围
   - 条件与约束
   - 业务口径
   - 路径 / 规则 / 证据
   - 风险与边界
4. Theory documents must land back to the project. Every major concept should answer one of:
   - 在数据直通车里它对应什么对象
   - 在数据直通车里它解决什么问题
   - 在数据直通车里如果不用它会出现什么问题
5. Prefer direct Chinese and stable nouns. Reduce rhetorical framing and abstract motivational language.
6. Use lists only when the content is inherently list-shaped. If a short paragraph can carry the point, prefer prose.
7. Prefer short, factual sentences. Avoid stacking multiple abstract clauses in one sentence.
8. When introducing a concept, say what it is, where it lands in the project, and what its boundary is.

## “去 AI 味” hard rules

Before finalizing any document, rewrite paragraphs that exhibit these patterns:

1. Empty abstract verbs without concrete subject or object, such as “赋能”“沉淀能力”“全面提升”“构建闭环”, unless the paragraph immediately states具体对象、具体动作、具体结果。
2. Symmetric but low-information lists that could apply to any project.
3. Repeated rhetorical patterns like “不是 A，而是 B” used only for style rather than clarification.
4. Tutorial-style filler such as “你可以把它理解成” repeated too often in formal design documents.
5. Meta commentary about writing itself, such as “这一节我们来讲”“这篇文档要回答”.
6. Overly polished summary lines that add no new fact.
7. Long conceptual sections that do not land on project objects, project scenes, project rules, or project evidence.

Use this rewrite test:

- If a sentence can be deleted without losing a concrete fact, rule, boundary, or decision, rewrite or remove it.
- If a paragraph cannot point to a project noun, a business boundary, or a design decision, rewrite it.
- If a list item sounds interchangeable with another company’s project, rewrite it using this project’s objects and constraints.

## Style preference by document type

1. `01-设计文档`
   - Prefer “定义、边界、对象、关系、规则、路径、证据、版本、职责边界”.
   - Do not write like a tutorial unless the document is explicitly a learning or theory document.
2. `02-开发文档`
   - Prefer “输入、输出、约束、流程、接口、门禁、异常、验收”.
   - Keep wording implementation-oriented and avoid product宣讲口吻.
3. `03-其他文档`
   - Keep the original role clear: 调研、样例、评审、归档.
   - Do not over-upgrade reference material into formal design language.

## Final pre-submit checklist

1. Terminology is annotated and consistent with the glossary.
2. The document uses project-native nouns rather than generic teaching placeholders.
3. Boundaries are explicit: what is included, excluded, defaulted, or deferred.
4. Rules, paths, and evidence are written more prominently than rhetoric.
5. Paragraphs read like business/design documentation, not model-generated exposition.
6. `README.md` is updated if navigation or role changed.
