# RAG 预处理实验适配与候选回写实施计划

> **For agentic workers:** 默认按 `subagent-driven-development（子代理驱动开发技能）` 执行；进入实现前先跑 `test-driven-development（测试驱动开发技能）`，实现完成后必须走 `requesting-code-review（请求代码审查技能）`、`code-reviewing（代码检视技能）` 与 `feature-test-report（特性测试与验收技能）`。

**Feature Doc:** `docs/architecture/features/iteration-01-knowledge-production/02f-rag-预处理实验适配与候选回写.md`

**Goal:** 在导入预处理阶段接入统一 `Preprocess Experiment Adapter（预处理实验适配器）`，把实验引擎输出稳定回写到候选层，同时保证实验结果不能直接进入正式治理资产。

**Scope:** 仅覆盖材料标准化后的实验调用、候选图 / 候选证据回写、实验运行记录和知识生产台最小可见反馈；不实现正式对象转正、不实现发布后运行检索。

**Preconditions:**

1. 以 `02f-rag-预处理实验适配与候选回写.md` 为唯一特性真源。
2. 现有 `ImportCommandAppService（导入命令应用服务）`、`ImportCandidateGraphBuildService（候选图构建服务）` 与知识生产台页面继续作为主入口。
3. 先固化“实验结果不得写正式资产”的失败测试，再接实验适配器实现。

---

## Task 1: 先写候选层隔离与适配器契约失败测试

**Files:**

- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/PreprocessExperimentAdapterContractTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCommandAppServiceTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportPreprocessStreamApiIntegrationTest.java`

- [ ] **Step 1: 固化统一输入输出契约断言**

至少断言：

```java
assertThat(result.getCandidateEntities()).isNotEmpty();
assertThat(result.getReferenceRefs()).isNotEmpty();
assertThat(result.getFormalAssetWrites()).isEmpty();
```

- [ ] **Step 2: 固化“实验结果不得写正式资产”的红灯用例**

Run:

- `cd backend && mvn -q -Dtest=PreprocessExperimentAdapterContractTest,ImportCommandAppServiceTest,ImportPreprocessStreamApiIntegrationTest test`

Expected:

- 测试失败，提示系统尚未表达统一实验适配器契约，或实验结果仍可能越过候选层边界。

## Task 2: 引入统一 `Preprocess Experiment Adapter（预处理实验适配器）` 与实验运行记录

**Files:**

- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/support/PreprocessExperimentSupport.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/supportimpl/application/LightRagPreprocessExperimentSupportImpl.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/ImportExperimentRunPO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/ImportExperimentRunMapper.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCommandAppService.java`

- [ ] **Step 1: 定义统一实验输入输出对象**

- [ ] **Step 2: 增加首个 `LightRAG（开源 GraphRAG 检索框架，LightRAG）` 适配实现**

- [ ] **Step 3: 为每次实验调用写入任务级运行记录**

- [ ] **Step 4: 复跑后端测试**

Run:

- `cd backend && mvn -q -Dtest=PreprocessExperimentAdapterContractTest,ImportCommandAppServiceTest,ImportPreprocessStreamApiIntegrationTest test`

Expected:

- 实验适配器可被调用，且每次运行都能留下 `import_task_id（导入任务标识）`、`adapter_name（适配器名称）`、`adapter_version（适配器版本）` 与引用数量。

## Task 3: 将实验结果回写到候选图与候选证据层

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCandidateGraphBuildService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCandidateGraphCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/PreprocessResultDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportTaskDTO.java`

- [ ] **Step 1: 把实验候选映射为候选图节点、候选图边和候选证据**

- [ ] **Step 2: 在 DTO 中补实验来源、引用定位和警告摘要**

- [ ] **Step 3: 保持正式 `Scene（业务场景）` / `Plan（方案资产）` / `Evidence Fragment（证据片段）` 零写入**

- [ ] **Step 4: 复跑导入相关回归**

Run:

- `cd backend && mvn -q -Dtest=PreprocessExperimentAdapterContractTest,ImportCommandAppServiceTest,ImportPreprocessStreamApiIntegrationTest test`

Expected:

- 实验候选可在导入任务返回和事件流中被看到，但数据库中的正式治理资产表无新增写入。

## Task 4: 收口知识生产台最小可见反馈

**Files:**

- Modify: `frontend/src/pages/KnowledgePage.jsx`
- Modify: `frontend/src/components/knowledge/CandidateEntityGraphPanel.jsx`
- Modify: `frontend/src/components/knowledge/importLiveGraphState.js`
- Modify: `frontend/src/components/knowledge/importLiveGraphState.test.js`
- Modify: `frontend/src/pages/KnowledgePage.render.test.jsx`

- [ ] **Step 1: 在候选图与任务详情中显示实验适配器名称、版本与引用摘要**

- [ ] **Step 2: 明确标识“实验候选”与“正式候选”差异**

- [ ] **Step 3: 保持导入中活图谱现有状态机与降级提示不回归**

- [ ] **Step 4: 复跑前端测试与构建**

Run:

- `cd frontend && npm test -- src/components/knowledge/importLiveGraphState.test.js src/pages/KnowledgePage.render.test.jsx`
- `cd frontend && npm run build`

Expected:

- 知识生产台能展示实验候选来源与引用信息，前端测试与构建通过。

## Task 5: 测试文档骨架与交付状态同步

**Files:**

- Create: `docs/testing/features/iteration-01-knowledge-production/02f-rag-预处理实验适配与候选回写-测试报告.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 先登记门禁型指标与最小验证命令**

- [ ] **Step 2: 在交付状态中登记 `02f` 的实现入口与风险**

- [ ] **Step 3: 复核文档链接一致性**

Run:

- `rg -n "02f-rag-预处理实验适配与候选回写|2026-04-22-rag-preprocess-experiment-adapter-implementation-plan" docs/architecture/features docs/plans docs/engineering/current-delivery-status.md`

Expected:

- 特性文档、实施计划、测试报告与交付状态四处口径一致，可进入实现态。
