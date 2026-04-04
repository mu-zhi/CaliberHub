# 2026-04-04 跨场景子图融合与三元组生成实施计划

> 来源设计：`docs/architecture/system-design.md` §8.1.2、§9.1.1；`docs/architecture/features/iteration-02-runtime-and-governance/10b-跨场景统一实体层与领域级图谱融合.md`
> 适用范围：跨场景统一实体层、领域级聚合图、发布冻结与图投影
> 执行方式：按仓库协作规则先走 `TDD（测试驱动开发，Test-Driven Development）`，每个任务完成后进入任务间审查

## 1. 目标

把“跨场景统一实体层与领域级图谱融合”收口为可执行实现闭环：控制库真源、正式场景资产锚点、统一实体解析、快照冻结、关系库 `DOMAIN` 聚合图、`Neo4j` 投影与校验全部使用同一口径。

## 2. 约束摘要

1. 真源固定为 `MySQL（关系型数据库，MySQL）` 控制库；`Neo4j（图数据库产品，Neo4j）` 只承接已发布 `snapshot` 的只读投影。
2. 关系作用域固定为 `Candidate（候选）`、`Formal Control（正式真源）`、`Snapshot（冻结可见边实例）` 三层。
3. `SCENE_MEMBERSHIP（场景成员归属）` 只能来自已发布 `scene_id + snapshot_id` 对应的场景资产成员集。
4. `relation_group（关系分组）` 固定映射为：`SCENE_MEMBERSHIP -> control`、`INSTANCE_OF -> control`、`MAPS_TO_SOURCE -> metadata`、`APPLIES_POLICY -> control`、`SUPPORTED_BY -> evidence`。

## 3. 文件清单

- 修改：`backend/src/main/resources/db/migration/*`
- 修改：`backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/*`
- 修改：`backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/*`
- 修改：`backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/*`
- 修改：`backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/*`
- 修改：`backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/*`
- 修改：`frontend/src/pages/datamap-adapter.js`
- 修改：`frontend/src/components/datamap/*`
- 修改：`docs/testing/features/iteration-02-runtime-and-governance/10b-跨场景统一实体层与领域级图谱融合-测试报告.md`

## 4. 任务拆分

### Task 0：先补失败测试与测试文档骨架

文件：
- `docs/testing/features/iteration-02-runtime-and-governance/10b-跨场景统一实体层与领域级图谱融合-测试报告.md`
- `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/CanonicalSnapshotBindingServiceTest.java`
- `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MvpKnowledgeGraphFlowIntegrationTest.java`

操作：
- 补 `SCENE_MEMBERSHIP` 必须来源于已发布 `scene snapshot` 的失败测试。
- 补 `APPLIES_POLICY -> control`、`MAPS_TO_SOURCE -> metadata`、`SUPPORTED_BY -> evidence` 的 `relation_group` 断言。
- 在测试文档中补“控制层真源 / 快照层可见实例不得混用”的验收项。

运行：
- `cd backend && mvn -q -Dtest=CanonicalSnapshotBindingServiceTest,MvpKnowledgeGraphFlowIntegrationTest test`

预期：
- 先看到失败测试，证明当前实现还未完全锁定最终口径。

### Task 1：固化控制库表结构与约束

文件：
- `backend/src/main/resources/db/migration/*`

操作：
- 新增或校准 `canonical_entity`
- 新增或校准 `canonical_entity_membership`
- 新增或校准 `canonical_relation`
- 新增或校准 `canonical_resolution_audit`
- 新增或校准 `canonical_snapshot_membership`
- 新增或校准 `canonical_snapshot_relation_visibility`
- 固定 `scene_asset_ref`、`relation_version`、`edge_id`、`relation_group` 的字段约束

运行：
- `cd backend && mvn -q -DskipTests package`

预期：
- 迁移脚本可编译，数据库对象命名与正式设计一致。

### Task 2：统一场景资产锚点

文件：
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/*`
- `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/*`

操作：
- 为 `Scene / Plan / Coverage Declaration / Source Contract / Policy / Evidence Fragment / Output Contract / Contract View` 统一生成或映射 `scene_asset_ref`
- 固定格式为 `scene-asset:{scene_id}:{asset_type}:{asset_id}`

运行：
- `cd backend && mvn -q -Dtest=CanonicalEntityResolutionServiceTest,SceneGraphAssetSyncCanonicalTest test`

预期：
- 同一正式资产在解析、冻结、聚合、投影环节共享同一锚点。

### Task 3：实现统一实体解析服务

文件：
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/CanonicalEntityResolutionService.java`
- `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/CanonicalEntityResolutionServiceTest.java`

操作：
- 固定四类对象的统一键解析规则
- 缺显式统一键时进入 `NEEDS_REVIEW（待复核）`
- 生成 `canonical_entity / canonical_entity_membership / canonical_relation / canonical_resolution_audit`
- 固定 `CanonicalRelation` 端点为 `CanonicalEntity -> CanonicalEntity`

运行：
- `cd backend && mvn -q -Dtest=CanonicalEntityResolutionServiceTest test`

预期：
- 测试通过，且不再允许把 `SceneAsset -> CanonicalEntity` 误写成 `CANONICAL_RELATION`。

### Task 4：实现发布冻结服务

文件：
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/CanonicalSnapshotBindingService.java`
- `backend/src/test/java/com/cmbchina/datadirect/caliber/application/service/command/graphrag/CanonicalSnapshotBindingServiceTest.java`

操作：
- 冻结 `canonical_snapshot_membership`
- 冻结 `canonical_snapshot_relation_visibility`
- 固定 `SCENE_MEMBERSHIP` 来源为已发布 `scene snapshot` 成员集
- 禁止从当前可变场景内容现算

运行：
- `cd backend && mvn -q -Dtest=CanonicalSnapshotBindingServiceTest test`

预期：
- 同一 `scene_id + snapshot_id` 的成员集在冻结层和领域级图读层保持一致。

### Task 5：实现关系库 DOMAIN 聚合图

文件：
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/GraphReadService.java`
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/DataMapGraphDtoAdapter.java`
- `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MvpKnowledgeGraphFlowIntegrationTest.java`

操作：
- 聚合 `SCENE_MEMBERSHIP / INSTANCE_OF / CANONICAL_RELATION`
- 固定 `relation_group` 映射
- 在返回体中显式携带 `readSource`、`snapshot_id`、`relation_group`

运行：
- `cd backend && mvn -q -Dtest=MvpKnowledgeGraphFlowIntegrationTest test`

预期：
- `root_type=DOMAIN` 返回的关系库图结果与正式设计口径一致。

### Task 6：补标准化 statement 导出

文件：
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/*`
- `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/*`

操作：
- 不建 `graph_statement_ledger`
- 但补按 `snapshot_id` 导出标准化 statement 的统一接口或内部服务

运行：
- `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest,MvpKnowledgeGraphFlowIntegrationTest test`

预期：
- 同一 `snapshot_id` 可导出统一 statement 列表，供回放与重投影使用。

### Task 7：实现 Neo4j 投影与投影校验

文件：
- `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/*`
- `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/*`
- `frontend/src/pages/datamap-adapter.js`

操作：
- 只消费冻结结果进行投影
- 固定 `relation_group` 与 `relation_type` 映射
- 不一致时降级回 `RELATIONAL`

运行：
- `bash scripts/run_canonical_entity_gate.sh`

预期：
- 图投影校验能阻断不一致快照进入 `Neo4j` 读路径。

## 5. 最低验收

1. 缺统一键对象进入 `NEEDS_REVIEW`，不得自动强归并。
2. `SCENE_MEMBERSHIP` 只来自已发布 `scene snapshot`。
3. `APPLIES_POLICY` 的 `relation_group` 为 `control`。
4. `relation_version` 跨快照语义不变时保持稳定。
5. `edge_id` 随 `snapshot_id` 变化。
6. 关系库 `DOMAIN` 图与 `Neo4j` 图在同一 `snapshot_id` 下结果一致，或在校验失败时显式降级。
