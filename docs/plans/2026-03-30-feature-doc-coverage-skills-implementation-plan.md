# Feature Doc Coverage Skills Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 固化“主方案能力 -> 特性文档”映射 skill，并增强特性文档校验 skill 与共享触发规则。

**Architecture:** 采用双 skill 拆分：新增 `feature-doc-coverage-mapping` 负责能力映射与缺口识别；保留 `feature-doc-authoring` 负责单文档或批量最低完备性校验。再把触发规则与协作说明回写到共享文档。

**Tech Stack:** Markdown skill files, project workflow docs, Codex automation directive

---

### Task 1: 新增能力映射 skill

**Files:**
- Create: `ai/skills/feature-doc-coverage-mapping/SKILL.md`

- [ ] **Step 1: 写 skill 正文**

写出触发条件、输入文档、原子能力拆分规则、`已覆盖 / 部分覆盖 / 缺失` 判定标准、建议新增专题文档门槛和输出格式。

- [ ] **Step 2: 验证 skill 文件存在**

Run: `test -f ai/skills/feature-doc-coverage-mapping/SKILL.md && echo ok`
Expected: 输出 `ok`

### Task 2: 增强特性文档校验 skill

**Files:**
- Modify: `ai/skills/feature-doc-authoring/SKILL.md`

- [ ] **Step 1: 扩写触发条件与工作流**

补充批量检查、映射结果驱动检查、输出模板和“不自动改正文”的边界。

- [ ] **Step 2: 验证关键触发词已写入**

Run: `rg -n "批量|映射|自动化|writing-plans" ai/skills/feature-doc-authoring/SKILL.md`
Expected: 命中新增触发和工作流关键词

### Task 3: 同步共享触发规则

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/engineering/collaboration-workflow.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 更新共享技能与自动触发说明**

在 `AGENTS.md` 和 `collaboration-workflow.md` 中补充 `feature-doc-coverage-mapping` 的职责、触发短语和 automation 串联方式。

- [ ] **Step 2: 同步交付状态**

在 `current-delivery-status.md` 中登记本次 skill 固化事项与下一动作。

- [ ] **Step 3: 运行轻量校验**

Run: `rg -n "feature-doc-coverage-mapping|feature-doc-authoring" AGENTS.md docs/engineering/collaboration-workflow.md docs/engineering/current-delivery-status.md ai/skills/feature-doc-coverage-mapping/SKILL.md ai/skills/feature-doc-authoring/SKILL.md`
Expected: 五个文件都命中新 skill 或增强后的检查 skill
