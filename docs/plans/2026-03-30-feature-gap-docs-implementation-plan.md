# Feature Gap Docs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the current feature-document coverage gaps by first bringing the highest-priority runtime/governance feature docs up to the minimum completeness bar, then adding four missing scenario/topic feature docs for dictionary governance, join-relation governance, inference asset visibility, and OpenAPI/frontend contract consumption.

**Architecture:** Treat `docs/architecture/system-design.md` and `docs/architecture/frontend-workbench-design.md` as the source design truth, and make `docs/architecture/features/` the scenario-level execution surface. Existing docs keep their scope but gain the missing “通用补充项 / 指标统计口径 / 接口边界 / 存储边界 / 场景边界” sections; missing atomic capabilities are added as new feature docs with explicit routing and neighboring references.

**Tech Stack:** Markdown documentation, repository feature-doc standard, current-delivery-status routing rules

---

### Task 1: Write the documentation execution plan

**Files:**
- Create: `docs/plans/2026-03-30-feature-gap-docs-implementation-plan.md`

- [ ] **Step 1: Capture the target document set**

List the work scope in the plan:

```md
- Patch existing docs: `07-publish-check-gray-release-and-rollback.md`, `11-monitoring-audit-and-impact-analysis.md`, `12-global-shell-navigation-and-context-handoff.md`, `12a-home-overview-and-state-dispatch.md`
- Create new docs: `03a-dictionary-governance.md`, `05a-join-relation-governance.md`, `04a-inference-asset-detail-and-visibility.md`, `12b-openapi-contract-and-frontend-consumption.md`
- Sync indexes: `docs/architecture/features/README.md`, `docs/plans/2026-03-28-feature-doc-iteration-roadmap-design.md`
```

- [ ] **Step 2: Verify the plan file exists**

Run: `test -f docs/plans/2026-03-30-feature-gap-docs-implementation-plan.md && echo OK`
Expected: `OK`

### Task 2: Bring the highest-priority existing feature docs to minimum completeness

**Files:**
- Modify: `docs/architecture/features/iteration-02-runtime-and-governance/07-publish-check-gray-release-and-rollback.md`
- Modify: `docs/architecture/features/iteration-02-runtime-and-governance/11-monitoring-audit-and-impact-analysis.md`
- Modify: `docs/architecture/features/iteration-02-runtime-and-governance/12-global-shell-navigation-and-context-handoff.md`
- Modify: `docs/architecture/features/iteration-02-runtime-and-governance/12a-home-overview-and-state-dispatch.md`

- [ ] **Step 1: Add metric-caliber language**

For each file, rewrite the metric section so each key metric includes:

```md
- 分子
- 分母
- 统计窗口
- 若当前阶段仅能用回放集或灰度集统计，则显式写明
```

- [ ] **Step 2: Add “通用补充项”**

For each file, add a structured block that explicitly covers:

```md
- 主对象
- 默认时间语义
- 覆盖范围和缺口
- 策略或审批边界
- 前端入口、详情页和跳转链路
- 接口或数据边界
- 存储落位边界
```

- [ ] **Step 3: Add explicit scene boundaries**

Append a boundary statement like:

```md
本场景的边界是“……”。它不负责……
```

- [ ] **Step 4: Verify completeness signals**

Run:

```bash
rg -n "通用补充项|分子|分母|统计窗口|接口或数据边界|存储落位边界|本场景的边界是" \
  docs/architecture/features/iteration-02-runtime-and-governance/07-publish-check-gray-release-and-rollback.md \
  docs/architecture/features/iteration-02-runtime-and-governance/11-monitoring-audit-and-impact-analysis.md \
  docs/architecture/features/iteration-02-runtime-and-governance/12-global-shell-navigation-and-context-handoff.md \
  docs/architecture/features/iteration-02-runtime-and-governance/12a-home-overview-and-state-dispatch.md
```

Expected: every file returns hits for all required completeness markers.

### Task 3: Add the missing atomic feature docs

**Files:**
- Create: `docs/architecture/features/iteration-01-knowledge-production/03a-dictionary-governance.md`
- Create: `docs/architecture/features/iteration-01-knowledge-production/04a-inference-asset-detail-and-visibility.md`
- Create: `docs/architecture/features/iteration-01-knowledge-production/05a-join-relation-governance.md`
- Create: `docs/architecture/features/iteration-02-runtime-and-governance/12b-openapi-contract-and-frontend-consumption.md`

- [ ] **Step 1: Use the feature-doc standard structure**

Each new file must contain:

```md
> 迭代归属：
> 来源主文档：

# 一、特性概述
# 二、指标
# 三、特性全景
# 四、特性说明
```

- [ ] **Step 2: Route each doc to the right neighboring capabilities**

Write each new file so it explicitly references adjacent existing docs:

```md
03a -> neighbors `03`
04a -> neighbors `04`, `08`, `11`, `10`
05a -> neighbors `05`
12b -> neighbors `12`, `12a`, `07`, `08`, `10`, `11`
```

- [ ] **Step 3: Verify every new doc includes minimum completeness language**

Run:

```bash
rg -n "通用补充项|接口或数据边界|存储落位边界|本场景的边界是|统计口径|分子|分母|统计窗口" \
  docs/architecture/features/iteration-01-knowledge-production/03a-dictionary-governance.md \
  docs/architecture/features/iteration-01-knowledge-production/04a-inference-asset-detail-and-visibility.md \
  docs/architecture/features/iteration-01-knowledge-production/05a-join-relation-governance.md \
  docs/architecture/features/iteration-02-runtime-and-governance/12b-openapi-contract-and-frontend-consumption.md
```

Expected: every new file contains the minimum completeness markers.

### Task 4: Sync feature indexes and route maps

**Files:**
- Modify: `docs/architecture/features/README.md`
- Modify: `docs/plans/2026-03-28-feature-doc-iteration-roadmap-design.md`

- [ ] **Step 1: Insert the new docs into the feature directory index**

Update the grouped feature lists so the new files appear in the right iteration sections.

- [ ] **Step 2: Update the roadmap coverage list**

Reflect the new atomic docs in the roadmap design and note that they are split from main capability clusters rather than new business-domain samples.

- [ ] **Step 3: Verify index sync**

Run:

```bash
rg -n "03a-dictionary-governance|04a-inference-asset-detail-and-visibility|05a-join-relation-governance|12b-openapi-contract-and-frontend-consumption" \
  docs/architecture/features/README.md \
  docs/plans/2026-03-28-feature-doc-iteration-roadmap-design.md
```

Expected: all four names appear in both routing/index documents.

### Task 5: Final self-check and handoff

**Files:**
- Modify: `docs/architecture/features/iteration-02-runtime-and-governance/07-publish-check-gray-release-and-rollback.md`
- Modify: `docs/architecture/features/iteration-02-runtime-and-governance/11-monitoring-audit-and-impact-analysis.md`
- Modify: `docs/architecture/features/iteration-02-runtime-and-governance/12-global-shell-navigation-and-context-handoff.md`
- Modify: `docs/architecture/features/iteration-02-runtime-and-governance/12a-home-overview-and-state-dispatch.md`
- Modify: `docs/architecture/features/iteration-01-knowledge-production/03a-dictionary-governance.md`
- Modify: `docs/architecture/features/iteration-01-knowledge-production/04a-inference-asset-detail-and-visibility.md`
- Modify: `docs/architecture/features/iteration-01-knowledge-production/05a-join-relation-governance.md`
- Modify: `docs/architecture/features/iteration-02-runtime-and-governance/12b-openapi-contract-and-frontend-consumption.md`
- Modify: `docs/architecture/features/README.md`
- Modify: `docs/plans/2026-03-28-feature-doc-iteration-roadmap-design.md`

- [ ] **Step 1: Run the minimum-completeness grep sweep**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
root = Path('docs/architecture/features')
for path in sorted(root.rglob('*.md')):
    if path.name == 'README.md':
        continue
    text = path.read_text()
    required = ['迭代归属：', '来源主文档：', '# 二、指标', '# 三、特性全景', '# 四、特性说明']
    missing = [item for item in required if item not in text]
    if missing:
        print(path, 'MISSING', missing)
PY
```

Expected: no missing required structural markers for the touched files.

- [ ] **Step 2: Review the diff**

Run: `git diff -- docs/architecture/features docs/plans/2026-03-28-feature-doc-iteration-roadmap-design.md docs/plans/2026-03-30-feature-gap-docs-implementation-plan.md`
Expected: diff shows only the planned doc additions and completeness upgrades.
