# 发布投影与数据地图前端场景切换 / 覆盖追踪回归（V2）实施计划

## 范围
- 限定前端数据地图工作台与测试：`frontend/src/components/datamap/**`、`frontend/src/pages/datamap-adapter.test.js`、`frontend/src/pages/WorkbenchContextPages.test.jsx`
- 目标：修复快照上下文定位稳定性、过滤状态保留、覆盖关系字段回归可见性。

## 任务 1（约 2 分钟）上下文轮次键模型
- 编辑 `frontend/src/components/datamap/dataMapContextBootstrap.js`
  - 增加 `buildContextRoundKey`（场景 + snapshot）用于上下文一轮定位判定
- 编辑 `frontend/src/components/datamap/dataMapContextBootstrap.test.js`
  - 新增用例：相同 `contextAssetRef` 不同 `snapshotId` 的上下文轮次键应不同
- 预期：测试失败→通过后保证切换 `snapshotId` 可触发新一轮自动定位。

## 任务 2（约 3 分钟）前端状态保留回归
- 新增 `frontend/src/components/datamap/DataMapContainer.interaction.test.jsx`
  - 场景：在资产图谱模式下设置 `snapshotId` 与 `objectTypes` 过滤后切回浏览模式，再切回资产图谱
  - 断言：`snapshotId` 与 `执行方案` 过滤芯片仍保留
- 预期：不应因切换模式丢失治理过滤状态。

## 任务 3（约 2 分钟）覆盖追踪字段保留
- 编辑 `frontend/src/pages/datamap-adapter.test.js`
  - 新增关系边回归：`relationType` 与 `coverageExplanation` 在 `normalizeLineageGraph` 后仍保留
- 预期：关系详情可读入覆盖说明字段，关系追踪稳定。

## 任务 4（约 2 分钟）定位修复最小实现
- 编辑 `frontend/src/components/datamap/DataMapContainer.jsx`
  - 以 `buildContextRoundKey` 替代仅 `focusAssetRef` 的上下文重置键
  - 当 `focusAssetRef + snapshotId` 发生变化时重置 `autoFocusConsumedRef` 与用户抢占标记
- 预期：同资产不同快照上下文切换后可重新执行一次自动焦点。

## 任务 5（约 2 分钟）验证门禁
- 运行
  - `cd frontend && npm test -- src/components/datamap/dataMapContextBootstrap.test.js src/components/datamap/DataMapContainer.interaction.test.jsx src/pages/datamap-adapter.test.js`
  - `cd frontend && npm test -- src/pages/WorkbenchContextPages.test.jsx`
  - `cd frontend && npm run build`
- 输出：相关测试通过 + 构建成功。
