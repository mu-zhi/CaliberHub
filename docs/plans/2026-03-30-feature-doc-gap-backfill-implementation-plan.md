# 首批特性文档缺口补齐 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补强现有 `03 资产建模与治理对象编辑` 特性文档，并新增 `09b 统一接口与事件契约`、`11b 运维验收与上线保障` 两份专题特性文档，同时把目录、路线稿和交付状态真源同步到一致状态。

**Architecture:** 采用“一次补三份文档、一次同步路由”的收口方式。先以主方案缺口映射结果为输入补强 `03` 的对象真源定义，再新增两份横切专题文档承接统一契约与上线保障，最后把 `features/README`、路线稿和 `current-delivery-status` 统一回写，避免新文档成为孤点。

**Tech Stack:** Markdown design docs, project workflow docs, `rg` / `test -f` lightweight validation

---

### Task 1: 补强 `03` 的正式治理对象定义

**Files:**
- Modify: `docs/architecture/features/iteration-01-knowledge-production/03-资产建模与治理对象编辑.md`

- [ ] **Step 1: 回写头注与指标口径**

```md
> 来源主文档：`system-design.md` §5.1.1-§5.1.3、§6.1、§7.4-§7.7；`frontend-workbench-design.md` §4.3、§7.5

1. 正式治理对象必填字段完整率达到 `100%`。统计口径：...
```

- [ ] **Step 2: 回写缺失的一等治理对象**

```md
2. `Dictionary（字典）` 必须作为独立资产登记...
3. `Input Slot Schema（输入槽位模式）` 必须明确槽位名、类型...
4. `Identifier Lineage（标识谱系）` 只允许登记稳定、可审计...
5. `Time Semantic Selector（时间语义选择器）` 必须定义默认时间...
```

- [ ] **Step 3: 回写对象引用校验与边界**

```md
9. `Plan（方案资产）` 进入送复核前，必须完成对象引用一致性校验...
12. 本场景的边界是“完成正式治理对象建模”...
```

- [ ] **Step 4: 验证关键对象名已进入 `03`**

Run: `rg -n "Dictionary|Identifier Lineage|Time Semantic Selector|Contract View|Coverage Declaration|Policy" docs/architecture/features/iteration-01-knowledge-production/03-资产建模与治理对象编辑.md`

Expected: 命中 6 类关键对象。

### Task 2: 新增 `09b 统一接口与事件契约`

**Files:**
- Create: `docs/architecture/features/iteration-02-runtime-and-governance/09b-统一接口与事件契约.md`

- [ ] **Step 1: 写文档头部与指标**

```md
# 统一接口与事件契约特性文档

> 迭代归属：迭代二运行与治理工作台
> 来源主文档：`system-design.md` §9.1.3、§9.3、§9.4；`frontend-workbench-design.md` §4.4、§4.5、§4.6、§4.7、§7.5
```

- [ ] **Step 2: 写统一契约正文**

```md
1. 凡被前端、`BFF`、外部服务或自动化流程稳定消费的正式接口，都必须进入 `OpenAPI` 契约管理范围。
4. 决策码负责表达系统如何处理请求，例如 `allow`、`need_approval`、`deny`、`clarification`、`partial` ...
6. 每条事件流都必须定义幂等边界、重复消费去重规则和重放处理方式。
```

- [ ] **Step 3: 验证新文件存在**

Run: `test -f docs/architecture/features/iteration-02-runtime-and-governance/09b-统一接口与事件契约.md && echo ok`

Expected: 输出 `ok`

### Task 3: 新增 `11b 运维验收与上线保障`

**Files:**
- Create: `docs/architecture/features/iteration-02-runtime-and-governance/11b-运维验收与上线保障.md`

- [ ] **Step 1: 写文档头部与指标**

```md
# 运维验收与上线保障特性文档

> 迭代归属：迭代二运行与治理工作台
> 来源主文档：`system-design.md` §11.1、§11.5、§12.2-§12.4；`frontend-workbench-design.md` §4.4、§4.7、§5.3
```

- [ ] **Step 2: 写上线保障正文**

```md
1. `NFR（非功能需求，Non-Functional Requirements）` 基线必须在正式上线前给出可验证门槛...
3. 样板回放集必须固定首批核心样板...
8. 上线回退触发条件必须结构化定义...
```

- [ ] **Step 3: 验证新文件存在**

Run: `test -f docs/architecture/features/iteration-02-runtime-and-governance/11b-运维验收与上线保障.md && echo ok`

Expected: 输出 `ok`

### Task 4: 同步目录、路线稿与交付状态

**Files:**
- Modify: `docs/architecture/features/README.md`
- Modify: `docs/plans/2026-03-28-feature-doc-iteration-roadmap-design.md`
- Modify: `docs/engineering/current-delivery-status.md`
- Create: `docs/plans/2026-03-30-feature-doc-gap-backfill-implementation-plan.md`

- [ ] **Step 1: 把 `09b / 11b` 写入特性目录与路线稿**

```md
- [`09b-统一接口与事件契约.md`](...)：统一接口与事件契约
- [`11b-运维验收与上线保障.md`](...)：运维验收与上线保障
```

- [ ] **Step 2: 在交付状态中登记本次补文档事项**

```md
| 首批特性文档缺口补齐（03 / 09b / 11b） | 特性文档治理基线 | ... | `done（完成）` | ... |
```

- [ ] **Step 3: 运行轻量校验**

Run: `rg -n "09b-unified-api-and-event-contracts|11b-operational-readiness-and-release-assurance|首批特性文档缺口补齐" docs/architecture/features/README.md docs/plans/2026-03-28-feature-doc-iteration-roadmap-design.md docs/engineering/current-delivery-status.md docs/plans/2026-03-30-feature-doc-gap-backfill-implementation-plan.md`

Expected: 四个文件都命中新文档或新事项。
