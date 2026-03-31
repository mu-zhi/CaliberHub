# Root Information Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize all non-code auxiliary files at the repository root into GitHub-style English domain directories, while keeping the repository navigable and fixing path references.

**Architecture:** Keep code domains (`frontend/`, `backend/`, `scripts/`) in place, move project正文文档 into `docs/`, move AI协作资产 into `ai/`, split reference material into `research/`, `third_party/`, and `archive/`, and isolate runtime/tool outputs into `tooling/` and `generated/`. Rewrite root navigation and shared workflow docs after the moves so every repository-level entry points at the new canonical paths.

**Tech Stack:** Git, Markdown, shell file operations, ripgrep, repository-local AI asset directories

---

## Task 1: Write the Execution Plan File

**Files:**

- Create: `docs/plans/2026-03-28-root-information-architecture-implementation-plan.md`

- [ ] **Step 1: Write the plan file**

Create this file with the approved root-domain structure, move list, verification commands, and risk notes.

- [ ] **Step 2: Verify the plan file exists**

Run: `test -f 'docs/plans/2026-03-28-root-information-architecture-implementation-plan.md'`

Expected: exit code `0`

## Task 2: Create the New Root-domain Skeleton

**Files:**

- Create: `docs/README.md`
- Create: `docs/architecture/`
- Create: `docs/engineering/`
- Create: `docs/engineering/standards/`
- Create: `docs/user-guide/`
- Create: `docs/assets/diagrams/`
- Create: `ai/README.md`
- Create: `ai/agents/`
- Create: `ai/contexts/`
- Create: `ai/hooks/`
- Create: `ai/rules/`
- Create: `ai/skills/`
- Create: `ai/project/`
- Create: `research/README.md`
- Create: `research/source-materials/`
- Create: `research/source-materials/sql-samples/`
- Create: `research/best-practices/`
- Create: `research/industry-research/`
- Create: `research/papers/`
- Create: `third_party/README.md`
- Create: `archive/README.md`
- Create: `archive/pending-migration/`
- Create: `tooling/README.md`
- Create: `generated/README.md`

- [ ] **Step 1: Create the directory tree**

Run:

```bash
mkdir -p \
  docs/architecture docs/engineering/standards docs/user-guide docs/assets/diagrams \
  ai/agents ai/contexts ai/hooks ai/rules ai/skills ai/project \
  research/source-materials/sql-samples research/best-practices research/industry-research research/papers \
  third_party archive/pending-migration tooling generated
```

Expected: directories exist with no error output

- [ ] **Step 2: Verify the skeleton**

Run:

```bash
find docs ai research third_party archive tooling generated -maxdepth 2 | sort
```

Expected: the new top-level domains are visible

## Task 3: Move Project正文文档 to `docs/`

**Files:**

- Move: `01-设计文档/00-统一术语表.md` -> `docs/glossary.md`
- Move: `01-设计文档/01-知识图谱与数据地图方案.md` -> `docs/architecture/system-design.md`
- Move: `01-设计文档/02-前端界面与工作台设计.md` -> `docs/architecture/frontend-workbench-design.md`
- Move: `01-设计文档/00-相关图示.drawio.xml` -> `docs/assets/diagrams/system-design.drawio.xml`
- Move: `02-开发文档/06-协作工作流.md` -> `docs/engineering/collaboration-workflow.md`
- Move: `02-开发文档/开发规范及要求/整体原则.md` -> `docs/engineering/standards/overall-principles.md`
- Move: `02-开发文档/开发规范及要求/落地细则.md` -> `docs/engineering/standards/implementation-details.md`
- Move: `02-开发文档/开发规范及要求/复杂应用架构设计规范.md` -> `docs/engineering/standards/complex-architecture-guidelines.md`
- Move: `02-开发文档/开发规范及要求/应用场景类特性文档编写标准.md` -> `docs/engineering/standards/scenario-feature-doc-standard.md`

- [ ] **Step 1: Move the files**

Run:

```bash
mv '01-设计文档/00-统一术语表.md' 'docs/glossary.md'
mv '01-设计文档/01-知识图谱与数据地图方案.md' 'docs/architecture/system-design.md'
mv '01-设计文档/02-前端界面与工作台设计.md' 'docs/architecture/frontend-workbench-design.md'
mv '01-设计文档/00-相关图示.drawio.xml' 'docs/assets/diagrams/system-design.drawio.xml'
mv '02-开发文档/06-协作工作流.md' 'docs/engineering/collaboration-workflow.md'
mv '02-开发文档/开发规范及要求/整体原则.md' 'docs/engineering/standards/overall-principles.md'
mv '02-开发文档/开发规范及要求/落地细则.md' 'docs/engineering/standards/implementation-details.md'
mv '02-开发文档/开发规范及要求/复杂应用架构设计规范.md' 'docs/engineering/standards/complex-architecture-guidelines.md'
mv '02-开发文档/开发规范及要求/应用场景类特性文档编写标准.md' 'docs/engineering/standards/scenario-feature-doc-standard.md'
```

Expected: files are now under `docs/`

- [ ] **Step 2: Verify the new documentation paths**

Run:

```bash
find docs -maxdepth 3 -type f | sort
```

Expected: the moved files appear under their new canonical locations

## Task 4: Move User Manual, Research Material, and Archive Content

**Files:**

- Move: `03-其他文档/01-当前业务侧的业务场景分类` -> `research/source-materials/01-当前业务侧的业务场景分类.md`
- Move: `03-其他文档/02-工单样例集.sql` -> `research/source-materials/sql-samples/02-工单样例集.sql`
- Move: `03-其他文档/03-口径文档现状-零售客户信息查询.sql` -> `research/source-materials/sql-samples/03-口径文档现状-零售客户信息查询.sql`
- Move: `03-其他文档/04-口径文档现状-零售客户信息变更.sql` -> `research/source-materials/sql-samples/04-口径文档现状-零售客户信息变更.sql`
- Move: `03-其他文档/05-口径文档现状-代发明细查询.sql` -> `research/source-materials/sql-samples/05-口径文档现状-代发明细查询.sql`
- Move: `03-其他文档/06-口径文档现状-基金理财保险交易查询.sql` -> `research/source-materials/sql-samples/06-口径文档现状-基金理财保险交易查询.sql`
- Move: `03-其他文档/07-官方最佳实践总览.md` and `08-12` -> `research/best-practices/`
- Move: `03-其他文档/13-业界参考-SuperSonic技术分享文档.md` and `14-21`, `23` -> `research/industry-research/`
- Move: `03-其他文档/22-跨平台论文全集` -> `research/papers/collection`
- Move: `03-其他文档/00-待梳理迁移` -> `archive/pending-migration`
- Move: `03-其他文档/MiroFish` -> `third_party/mirofish`

- [ ] **Step 1: Move research and archive content**

Run the corresponding `mv` commands for each file and directory, renaming the extensionless scene-classification file to `.md`.

Expected: `research/`, `archive/`, and `third_party/` hold the former reference material

- [ ] **Step 2: Verify the split**

Run:

```bash
find research third_party archive -maxdepth 3 | sort | sed -n '1,240p'
```

Expected: references are no longer mixed under `03-其他文档/`

## Task 5: Move AI Assets and tooling/runtime Directories

**Files:**

- Move: `agents/` -> `ai/agents/`
- Move: `contexts/` -> `ai/contexts/`
- Move: `hooks/` -> `ai/hooks/`
- Move: `rules/` -> `ai/rules/`
- Move: `skills/` -> `ai/skills/`
- Move: `.agents/` -> `ai/project/agents/`
- Move: `.superpowers/` -> `ai/project/superpowers/`
- Move: `.claude-2-config/` -> `tooling/claude-2-config/`
- Move: `.tools/` -> `tooling/codex-tools/`
- Move: `.playwright-cli/` -> `tooling/playwright-cli/`
- Move: `dist/` -> `generated/dist/`
- Move: `output/` -> `generated/output/`

- [ ] **Step 1: Move the directories**

Run the corresponding `mv` commands to relocate AI assets and generated/tooling folders into their new domains.

Expected: root-level auxiliary directories shrink to the agreed canonical set

- [ ] **Step 2: Verify the new roots**

Run:

```bash
find ai tooling generated -maxdepth 3 | sort | sed -n '1,260p'
```

Expected: the moved directories appear under their new domains

## Task 6: Rewrite Entry Docs and Repository Navigation

**Files:**

- Modify: `README.md`
- Modify: `AGENTS.md`
- Create: `docs/README.md`
- Create: `ai/README.md`
- Create: `research/README.md`
- Create: `third_party/README.md`
- Create: `archive/README.md`
- Create: `tooling/README.md`
- Create: `generated/README.md`
- Modify: `docs/plans/README.md`
- Modify: moved docs under `docs/` where old paths are still referenced

- [ ] **Step 1: Rewrite root navigation**

Update the root `README.md` so it describes the new GitHub-style top-level domains, points all project正文入口 into `docs/`, and removes dead links to retired numbered roots.

- [ ] **Step 2: Rewrite shared collaboration rules**

Update `AGENTS.md` and `docs/engineering/collaboration-workflow.md` so they reference the new canonical paths under `docs/` and `ai/`.

- [ ] **Step 3: Add domain READMEs**

Create concise boundary docs for `docs/`, `ai/`, `research/`, `third_party/`, `archive/`, `tooling/`, and `generated/`.

- [ ] **Step 4: Fix moved document links**

Search for old paths such as `01-设计文档/`, `02-开发文档/`, `03-其他文档/`, `04-用户手册/`, `agents/`, `skills/`, `hooks/`, `rules/`, `contexts/`, `.agents/`, `.superpowers/`, `.playwright-cli/`, and update them to the new locations.

## Task 7: Clean Obsolete Directories and Run Verification

**Files:**

- Remove if empty: `01-设计文档/`
- Remove if empty: `02-开发文档/`
- Remove if empty: `03-其他文档/`
- Remove if empty: `04-用户手册/`

- [ ] **Step 1: Remove empty retired directories**

Run:

```bash
find '01-设计文档' '02-开发文档' '03-其他文档' '04-用户手册' -type d -empty -delete
```

Expected: empty legacy roots are removed where safe

- [ ] **Step 2: Verify no stale path references remain**

Run:

```bash
rg -n "01-设计文档|02-开发文档|03-其他文档|04-用户手册|agents/|contexts/|hooks/|rules/|skills/|\\.agents/|\\.superpowers/|\\.playwright-cli/|\\.claude-2-config/|\\.tools/|^docs/$" .
```

Expected: only intentional compatibility mentions remain

- [ ] **Step 3: Run link verification**

Run:

```bash
python3 'ai/skills/doc-organizer/scripts/check_links.py' '.'
```

Expected: internal links report no repository-level dead links

- [ ] **Step 4: Review the final diff**

Run:

```bash
git status --short
git diff --stat
```

Expected: the diff is dominated by the planned moves and navigation rewrites
