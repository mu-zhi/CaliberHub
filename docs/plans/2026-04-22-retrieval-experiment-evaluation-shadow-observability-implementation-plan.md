# 检索实验评测、灰度与运行观测实施计划

> **For agentic workers:** 默认按 `subagent-driven-development（子代理驱动开发技能）` 执行；进入实现前先跑 `test-driven-development（测试驱动开发技能）`，实现完成后必须走 `requesting-code-review（请求代码审查技能）`、`code-reviewing（代码检视技能）` 与 `feature-test-report（特性测试与验收技能）`。

**Feature Doc:** `docs/architecture/features/iteration-02-runtime-and-governance/08d-检索实验评测、灰度与运行观测.md`

**Goal:** 建立“离线评测 -> `Shadow Mode（影子模式）` -> 灰度门禁 -> 运行观测 -> 回退规则”的检索实验治理链路。

**Scope:** 仅覆盖评测指标、影子模式开关、灰度范围、审计字段、监控视图和回退规则；不实现新的业务工作台，不重写知识生产 Prompt 语义。

**Preconditions:**

1. 以 `08d-检索实验评测、灰度与运行观测.md` 为唯一特性真源。
2. `08b` 和 `08c` 已定义实验适配器与索引版本边界；本计划只承接评测、灰度与观测。
3. 先写“实验链路不会影响正式响应”的失败测试，再接影子模式与监控实现。

---

## Task 1: 先写离线评测与影子模式失败测试

**Files:**

- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/RetrievalExperimentEvaluationTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
- Create: `scripts/run_retrieval_experiment_eval.sh`

- [ ] **Step 1: 固化 `scene hit@5（前 5 命中率）`、证据 `precision@10（前 10 精确率）` 与 `Policy false allow（策略误放行）` 的基线断言**

- [ ] **Step 2: 固化“影子模式只记录、不影响正式响应”的红灯用例**

Run:

- `cd backend && mvn -q -Dtest=RetrievalExperimentEvaluationTest,KnowledgePackageApiIntegrationTest test`
- `bash scripts/run_retrieval_experiment_eval.sh --dry-run`

Expected:

- 测试失败，提示系统尚未表达统一评测指标、影子模式或正式结果隔离规则。

## Task 2: 建立离线评测结果与影子模式配置

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/graphrag/GraphRuntimeProperties.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/graphrag/GraphRuntimeConfig.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/RetrievalExperimentEvaluationService.java`
- Modify: `scripts/run_retrieval_experiment_eval.sh`

- [ ] **Step 1: 增加 `shadow_mode_enabled（影子模式开关）`、灰度范围和评测阈值配置**

- [ ] **Step 2: 生成统一评测结果对象与回放结果摘要**

- [ ] **Step 3: 复跑后端测试与脚本**

Run:

- `cd backend && mvn -q -Dtest=RetrievalExperimentEvaluationTest,KnowledgePackageApiIntegrationTest test`
- `bash scripts/run_retrieval_experiment_eval.sh --snapshot-id demo --adapter LightRAG`

Expected:

- 系统能输出统一评测结果，且影子模式配置已可被读取。

## Task 3: 补齐审计字段、监控视图与灰度门禁

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/GraphAuditEventAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/graphrag/AuditEventPO.java`
- Modify: `frontend/src/pages/MonitoringAuditPage.jsx`
- Modify: `frontend/src/pages/SystemPage.jsx`
- Create: `frontend/src/pages/MonitoringAuditPage.retrievalExperiment.test.jsx`

- [ ] **Step 1: 为实验链路补充 `trace_id（追踪编号）`、`snapshot_id（快照标识）`、`adapter_name（适配器名称）`、`index_version（索引版本）`、`latency_ms（耗时毫秒）` 与引用数量字段**

- [ ] **Step 2: 在系统管理页补灰度范围与停机开关**

- [ ] **Step 3: 在监控页展示评测摘要、影子模式状态、误放行风险与回退建议**

- [ ] **Step 4: 复跑前端测试与构建**

Run:

- `cd frontend && npm test -- src/pages/MonitoringAuditPage.retrievalExperiment.test.jsx`
- `cd frontend && npm run build`

Expected:

- 系统管理页与监控页都能表达实验灰度与观测结果，前端测试与构建通过。

## Task 4: 测试文档骨架、交付状态与回退规则收口

**Files:**

- Create: `docs/testing/features/iteration-02-runtime-and-governance/08d-检索实验评测、灰度与运行观测-测试报告.md`
- Modify: `docs/engineering/current-delivery-status.md`
- Modify: `docs/architecture/features/iteration-02-runtime-and-governance/11b-运维验收与上线保障.md`

- [ ] **Step 1: 在测试报告中登记离线评测命令、影子模式命令、灰度停机条件与 `p95（第 95 百分位延迟）` 验证项**

- [ ] **Step 2: 在交付状态中登记 `08d` 的实现入口与风险**

- [ ] **Step 3: 在上线保障文档中补实验链路停机与回退引用**

- [ ] **Step 4: 复核文档与脚本链接一致性**

Run:

- `rg -n "08d-检索实验评测、灰度与运行观测|2026-04-22-retrieval-experiment-evaluation-shadow-observability-implementation-plan" docs/architecture/features docs/plans docs/engineering/current-delivery-status.md docs/architecture/features/iteration-02-runtime-and-governance/11b-运维验收与上线保障.md`

Expected:

- 特性文档、实施计划、测试报告、交付状态与上线保障文档口径一致，可进入实现态。
