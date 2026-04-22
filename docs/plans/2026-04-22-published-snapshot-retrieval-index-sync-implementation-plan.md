# 已发布快照检索索引同步与版本锁定实施计划

> **For agentic workers:** 默认按 `subagent-driven-development（子代理驱动开发技能）` 执行；进入实现前先跑 `test-driven-development（测试驱动开发技能）`，实现完成后必须走 `requesting-code-review（请求代码审查技能）`、`code-reviewing（代码检视技能）` 与 `feature-test-report（特性测试与验收技能）`。

**Feature Doc:** `docs/architecture/features/iteration-02-runtime-and-governance/08c-已发布快照检索索引同步与版本锁定.md`

**Goal:** 建立“已发布快照摘要导出 -> 实验检索索引构建 -> `snapshot_id（快照标识）` 与 `index_version（索引版本）` 绑定 -> 回滚 / 退役同步”的正式链路。

**Scope:** 仅覆盖已发布快照摘要导出、索引清单持久化、版本锁定、发布回滚协同和最小监控可见反馈；不实现运行排序策略，不实现离线评测。

**Preconditions:**

1. 以 `08c-已发布快照检索索引同步与版本锁定.md` 为唯一特性真源。
2. 现有发布主链路仍由 `SceneVersionAppService（场景版本应用服务）`、`ScenePublishGateAppService（场景发布门禁应用服务）` 和图投影相关服务承接。
3. 先写“非 `PUBLISHED（已发布）` 对象不得进入实验索引”的失败测试，再接索引同步实现。

---

## Task 1: 先写索引版本锁定与非发布对象隔离失败测试

**Files:**

- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/ExperimentalRetrievalIndexSyncServiceTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/CanonicalSnapshotBindingServiceTest.java`

- [ ] **Step 1: 固化“只接收已发布快照摘要”的红灯断言**

至少断言：

```java
assertThat(manifest.getSnapshotId()).isEqualTo(snapshotId);
assertThat(manifest.getSourceStatus()).isEqualTo("PUBLISHED");
assertThat(manifest.getDraftLeakCount()).isZero();
```

- [ ] **Step 2: 固化“运行请求必须命中唯一索引版本”的失败测试**

Run:

- `cd backend && mvn -q -Dtest=ExperimentalRetrievalIndexSyncServiceTest,CanonicalSnapshotBindingServiceTest,KnowledgePackageApiIntegrationTest test`

Expected:

- 测试失败，提示当前系统尚未表达实验索引清单、版本绑定或快照隔离规则。

## Task 2: 建立实验索引清单与版本映射持久化

**Files:**

- Create: `backend/src/main/resources/db/migration/V22__experimental_retrieval_index_manifest.sql`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/graphrag/ExperimentalRetrievalIndexManifestPO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/graphrag/ExperimentalRetrievalIndexManifestMapper.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/ExperimentalRetrievalIndexSyncService.java`

- [ ] **Step 1: 定义 `snapshot_id（快照标识）`、`index_version（索引版本）`、状态、失败原因和回退指针字段**

- [ ] **Step 2: 建立索引构建与退役的台账记录**

- [ ] **Step 3: 复跑后端测试**

Run:

- `cd backend && mvn -q -Dtest=ExperimentalRetrievalIndexSyncServiceTest,CanonicalSnapshotBindingServiceTest test`

Expected:

- 系统可以记录实验索引版本，但仍未把发布与回滚动作接进去。

## Task 3: 将索引同步接入发布、回滚与退役主链路

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/SceneVersionAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/ScenePublishGateAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/GraphProjectionAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/KnowledgePackageQueryAppService.java`

- [ ] **Step 1: 在发布成功后导出已发布摘要并构建实验索引版本**

- [ ] **Step 2: 在回滚或退役时同步刷新 `snapshot_id（快照标识）` 与 `index_version（索引版本）` 映射**

- [ ] **Step 3: 在运行读取前校验索引版本是否与请求快照一致**

- [ ] **Step 4: 复跑后端回归**

Run:

- `cd backend && mvn -q -Dtest=ExperimentalRetrievalIndexSyncServiceTest,CanonicalSnapshotBindingServiceTest,KnowledgePackageApiIntegrationTest test`

Expected:

- 运行请求能按 `snapshot_id（快照标识）` 读取唯一有效索引版本；缺失或错配时稳定降级。

## Task 4: 收口最小监控反馈与测试文档骨架

**Files:**

- Modify: `frontend/src/pages/MonitoringAuditPage.jsx`
- Create: `frontend/src/pages/MonitoringAuditPage.indexSync.test.jsx`
- Create: `docs/testing/features/iteration-02-runtime-and-governance/08c-已发布快照检索索引同步与版本锁定-测试报告.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 在监控页展示索引版本、构建状态与错配告警**

- [ ] **Step 2: 在测试报告中登记版本锁定、回滚与退役验证命令**

- [ ] **Step 3: 在交付状态中登记 `08c` 的实现入口与风险**

- [ ] **Step 4: 复跑前端测试与文档校验**

Run:

- `cd frontend && npm test -- src/pages/MonitoringAuditPage.indexSync.test.jsx`
- `rg -n "08c-已发布快照检索索引同步与版本锁定|2026-04-22-published-snapshot-retrieval-index-sync-implementation-plan" docs/architecture/features docs/plans docs/engineering/current-delivery-status.md`

Expected:

- 监控页、测试报告、交付状态与特性文档口径一致，可进入实现态。
