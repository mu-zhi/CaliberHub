# Data Map High Visibility Implementation Plan

> **For agentic workers:** 默认按 `subagent-driven-development（子代理驱动开发技能）` 执行；进入实现前先跑 `test-driven-development（测试驱动开发技能）`，实现完成后必须走 `code-reviewing（代码检视技能）` 与 `feature-test-report（特性测试与验收技能）`。

**Goal:** 把“数据地图高可见层”从“代码已先行、文档在追赶”的状态收口为可治理、可测试的首个真实样本，首批只实现并验证“上下文进入 -> 自动图谱定位 -> 节点/关系详情 -> 影响分析 -> 跳转运行决策台”这一条最小闭环。

**Design Inputs:**

- `docs/architecture/features/iteration-02-runtime-and-governance/10-数据地图浏览与覆盖追踪.md`
- `docs/architecture/frontend-workbench-design.md`
- `docs/plans/2026-03-29-data-map-high-visibility-feasibility-review-design.md`

**Tech Stack:** React, Vite, existing `DataMapContainer` / `datamap-adapter` frontend stack, Spring Boot, Maven, current data map integration tests

---

## File Map

- Create: `frontend/src/components/datamap/dataMapContextBootstrap.js`
- Create: `frontend/src/components/datamap/dataMapContextBootstrap.test.js`
- Modify: `frontend/src/components/datamap/DataMapContainer.jsx`
- Modify: `frontend/src/pages/datamap-adapter.js`
- Modify: `frontend/src/pages/datamap-adapter.test.js`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MvpKnowledgeGraphFlowIntegrationTest.java`
- Create: `docs/testing/features/iteration-02-runtime-and-governance/10-数据地图浏览与覆盖追踪-测试报告.md`

## Scope

- 本批次只收口“高可见反馈最小闭环”，不做新工作台扩张。
- 本批次把 `/api/datamap/*` 明确为高可见层正式主路径；若仍需要兼容 `/api/assets/*`，只能作为旧浏览能力保留，不新增依赖。
- 本批次允许保留 `DataMapContainer.jsx` 主容器，但需要先把上下文引导与自动定位规则抽离成可测试的纯逻辑。

## Task 1: 固化上下文引导规则的失败测试

**Files:**

- Create: `frontend/src/components/datamap/dataMapContextBootstrap.test.js`
- Modify: `frontend/src/pages/datamap-adapter.test.js`

- [ ] **Step 1: 为高可见反馈补失败测试**

新增 `frontend/src/components/datamap/dataMapContextBootstrap.test.js`，至少覆盖以下四个失败场景：

1. 带 `ctx` 进入时会自动切到 `lineage（资产图谱）` 视图。
2. 自动定位只在单轮上下文中触发一次。
3. 未命中节点时返回明确降级结果，而不是伪高亮。
4. 用户手动切视图后，自动抢占应失效。

同时在 `frontend/src/pages/datamap-adapter.test.js` 中补一组失败测试，验证页面适配层优先使用 `/api/datamap/*` 作为图谱主路径。

- [ ] **Step 2: 运行测试验证 Red**

Run:

- `cd frontend && npm test -- src/components/datamap/dataMapContextBootstrap.test.js src/pages/datamap-adapter.test.js`

Expected:

- 新增上下文引导测试 FAIL
- 适配层主路径测试 FAIL

## Task 2: 抽离上下文引导纯逻辑并接入容器

**Files:**

- Create: `frontend/src/components/datamap/dataMapContextBootstrap.js`
- Modify: `frontend/src/components/datamap/DataMapContainer.jsx`

- [ ] **Step 1: 实现可测试的上下文引导模块**

在 `frontend/src/components/datamap/dataMapContextBootstrap.js` 中抽离纯逻辑函数，至少包含：

1. `resolveDataMapEntryContext`
2. `resolveAutoFocusDecision`
3. `resolveContextFallbackState`

要求：

- 输入只接收页面上下文、当前视图、图谱查询结果和用户是否已手动接管。
- 输出显式区分“自动切图谱”“自动聚焦目标节点”“降级提示”“忽略上下文”四类结果。

- [ ] **Step 2: 在 `DataMapContainer.jsx` 中接入纯逻辑**

把当前 `ctx` 驱动、自动切图谱、自动定位、未命中降级提示和手动让位逻辑改为消费 `dataMapContextBootstrap.js`，避免继续把生命周期判断散落在组件体内。

- [ ] **Step 3: 运行测试验证 Green**

Run:

- `cd frontend && npm test -- src/components/datamap/dataMapContextBootstrap.test.js src/pages/datamap-adapter.test.js`

Expected:

- 上述测试 PASS

## Task 3: 收口前端主路径与影响分析闭环

**Files:**

- Modify: `frontend/src/pages/datamap-adapter.js`
- Modify: `frontend/src/pages/datamap-adapter.test.js`

- [ ] **Step 1: 收口前端适配层主路径**

调整 `frontend/src/pages/datamap-adapter.js`：

1. 图谱查询、节点详情与影响分析优先走 `/api/datamap/*`。
2. 若仍保留 `/api/assets/*`，显式限定为旧列树浏览或兼容读取，不允许新高可见逻辑继续依赖它。
3. 对接口降级和错误返回补结构化说明，避免前端静默吞掉主路径失败。

- [ ] **Step 2: 补齐适配层断言**

在 `frontend/src/pages/datamap-adapter.test.js` 中增加断言：

1. 图谱查询调用 `/api/datamap/graph`
2. 节点详情调用 `/api/datamap/node/{id}/detail`
3. 影响分析调用 `/api/datamap/impact-analysis`

- [ ] **Step 3: 运行测试**

Run:

- `cd frontend && npm test -- src/pages/datamap-adapter.test.js`

Expected:

- PASS

## Task 4: 补后端主路径集成验证与首份测试文档

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MvpKnowledgeGraphFlowIntegrationTest.java`
- Create: `docs/testing/features/iteration-02-runtime-and-governance/10-数据地图浏览与覆盖追踪-测试报告.md`

- [ ] **Step 1: 增加后端主路径断言**

在 `MvpKnowledgeGraphFlowIntegrationTest.java` 中补强当前数据地图相关集成断言，至少覆盖：

1. `/api/datamap/graph`
2. `/api/datamap/node/{id}/detail`
3. `/api/datamap/impact-analysis`

并把该组测试命名收口为“高可见层主路径可达”，避免继续只表达“链路可达”。

- [ ] **Step 2: 建立测试文档初稿**

在 `docs/testing/features/iteration-02-runtime-and-governance/10-数据地图浏览与覆盖追踪-测试报告.md` 中预置：

1. 对应特性文档路径
2. 首轮验收范围
3. 测试案例骨架
4. `P0 / P1 / P2` 缺陷清单区
5. 放行结论区

此时只允许写“待进入 reviewing 后补执行结果”，不提前填通过结论。

- [ ] **Step 3: 运行验证**

Run:

- `cd backend && mvn -q test -Dtest=MvpKnowledgeGraphFlowIntegrationTest`

Expected:

- PASS

## Task 5: 完成实现批次后的评审与验收

**Files:**

- Modify: `docs/engineering/current-delivery-status.md`
- Modify: `docs/testing/features/iteration-02-runtime-and-governance/10-数据地图浏览与覆盖追踪-测试报告.md`

- [ ] **Step 1: 代码完成后执行 Review Agent 链路**

按顺序执行：

1. `requesting-code-review`
2. `code-reviewing`
3. 发现 `P0/P1` 时回到实现修复
4. `feature-test-report`

- [ ] **Step 2: 回填状态真源与测试文档**

在 `docs/engineering/current-delivery-status.md` 中把当前工作项切到 `reviewing（评审验证中）`，并回填测试文档路径；在测试文档中补执行结果、缺陷清单与最终结论。

- [ ] **Step 3: 运行最终验证**

Run:

- `cd frontend && npm test -- src/components/datamap/dataMapContextBootstrap.test.js src/pages/datamap-adapter.test.js`
- `cd backend && mvn -q test -Dtest=MvpKnowledgeGraphFlowIntegrationTest`

Expected:

- 全部 PASS
- 无未关闭 `P0/P1`
- 可进入 `finishing-a-development-branch`
