# RAG 运行检索增强侧车与候选合并实施计划

> **For agentic workers:** 默认按 `subagent-driven-development（子代理驱动开发技能）` 执行；进入实现前先跑 `test-driven-development（测试驱动开发技能）`，实现完成后必须走 `requesting-code-review（请求代码审查技能）`、`code-reviewing（代码检视技能）` 与 `feature-test-report（特性测试与验收技能）`。

**Feature Doc:** `docs/architecture/features/iteration-02-runtime-and-governance/08b-rag-运行检索增强侧车与候选合并.md`

**Goal:** 在运行主线中接入统一 `Retrieval Experiment Adapter（运行检索实验适配器）`，为正式 `Scene Recall（场景召回）` 和证据召回补候选，同时保持正式决策链不变。

**Scope:** 仅覆盖实验适配器契约、候选合并、知识包调试块和最小前端可见反馈；不实现实验索引构建，不实现离线评测与 `Shadow Mode（影子模式）`。

**Preconditions:**

1. 以 `08b-rag-运行检索增强侧车与候选合并.md` 为唯一特性真源。
2. 已存在 `KnowledgePackageQueryAppService（知识包查询应用服务）` 和 `GraphRagQueryAppService（图检索查询应用服务）` 作为运行链路入口。
3. 先写“实验结果不能直接改变正式决策”的失败测试，再接候选合并实现。

---

## Task 1: 先写运行契约与正式决策隔离失败测试

**Files:**

- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/RetrievalExperimentAdapterContractTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/query/SceneQueryAppServiceTest.java`

- [ ] **Step 1: 固化统一输入输出契约断言**

至少断言：

```java
assertThat(result.getCandidateScenes()).isNotEmpty();
assertThat(result.getDecision()).isNull();
assertThat(result.getReferenceRefs()).isNotEmpty();
```

- [ ] **Step 2: 固化“实验候选不能绕过正式策略门禁”的红灯用例**

Run:

- `cd backend && mvn -q -Dtest=RetrievalExperimentAdapterContractTest,SceneQueryAppServiceTest,KnowledgePackageApiIntegrationTest test`

Expected:

- 测试失败，提示运行主线尚未表达实验适配器契约，或实验输出仍可能直接影响正式 `decision（决策）`。

## Task 2: 引入统一 `Retrieval Experiment Adapter（运行检索实验适配器）`

**Files:**

- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/support/RetrievalExperimentSupport.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/supportimpl/application/LightRagRetrievalExperimentSupportImpl.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/KnowledgePackageQueryAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/GraphRagQueryAppService.java`

- [ ] **Step 1: 定义统一实验输入输出对象**

- [ ] **Step 2: 增加首个 `LightRAG（开源 GraphRAG 检索框架，LightRAG）` 适配实现**

- [ ] **Step 3: 把实验调用放到 `Query Rewrite（查询改写）` / `Slot Filling（槽位补齐）` 与正式 `Scene Recall（场景召回）` 之间**

- [ ] **Step 4: 复跑后端测试**

Run:

- `cd backend && mvn -q -Dtest=RetrievalExperimentAdapterContractTest,SceneQueryAppServiceTest,KnowledgePackageApiIntegrationTest test`

Expected:

- 运行链路能读取实验候选，但正式 `allow（允许）` / `need_approval（需要审批）` / `deny（拒绝）` 结果仍由现有链路给出。

## Task 3: 实现候选合并与知识包调试块

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackageTraceDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackageEvidenceDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackageDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/KnowledgePackageQueryAppService.java`

- [ ] **Step 1: 为知识包增加实验引用调试块与适配器元数据**

- [ ] **Step 2: 固化“正式规则优先、实验候选补充”的合并顺序**

- [ ] **Step 3: 在适配器失败时直接回退正式召回路径**

- [ ] **Step 4: 复跑运行回归**

Run:

- `cd backend && mvn -q -Dtest=RetrievalExperimentAdapterContractTest,KnowledgePackageApiIntegrationTest test`

Expected:

- 知识包仅增加实验调试字段，不改变现有正式契约语义。

## Task 4: 收口运行决策台最小可见反馈

**Files:**

- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx`
- Modify: `frontend/src/pages/MonitoringAuditPage.jsx`

- [ ] **Step 1: 在知识包调试区展示实验候选来源、引用数量与分数组成**

- [ ] **Step 2: 在监控页展示实验适配器失败与降级摘要**

- [ ] **Step 3: 保持现有澄清、覆盖与审批提示不回归**

- [ ] **Step 4: 复跑前端测试与构建**

Run:

- `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx`
- `cd frontend && npm run build`

Expected:

- 运行决策台能看见实验调试块，监控页能看见失败摘要，前端测试与构建通过。

## Task 5: 测试文档骨架与交付状态同步

**Files:**

- Create: `docs/testing/features/iteration-02-runtime-and-governance/08b-rag-运行检索增强侧车与候选合并-测试报告.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 在测试报告中登记 `scene hit@5（前 5 命中率）`、证据 `precision@10（前 10 精确率）` 与 `Policy false allow（策略误放行）`**

- [ ] **Step 2: 在交付状态中登记 `08b` 的实现入口与风险**

- [ ] **Step 3: 校验文档链接与文件名一致性**

Run:

- `rg -n "08b-rag-运行检索增强侧车与候选合并|2026-04-22-rag-runtime-retrieval-sidecar-implementation-plan" docs/architecture/features docs/plans docs/engineering/current-delivery-status.md`

Expected:

- 特性文档、实施计划、测试报告与交付状态四处口径一致，可进入实现态。
