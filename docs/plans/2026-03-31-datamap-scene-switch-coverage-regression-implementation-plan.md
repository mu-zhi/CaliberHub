# 发布投影与数据地图前端场景切换 / 覆盖追踪 / 快照定位回归实施计划

> 对齐范围：前端数据地图工作台与前端测试，仅限 `frontend/src/components/datamap/**` 与 `frontend/src/pages/WorkbenchContextPages.test.jsx`。
> 参考特性文档：`docs/architecture/features/iteration-02-runtime-and-governance/10-data-map-browsing-and-coverage-tracing.md`
> 对齐状态项：`docs/engineering/current-delivery-status.md` 中“发布投影与数据地图前端场景切换、覆盖追踪视图与更多投影回归”条目。

## 任务 1：补快照上下文定位稳定性回归（红）

- [ ] 修改 `frontend/src/components/datamap/dataMapContextBootstrap.test.js`
  - [ ] 补用例验证同资产在同一轮上下文内只一次自动聚焦。
  - [ ] 补用例验证“同一上下文轮次 key 改变（`snapshotId`）时需允许再次自动聚焦（用于上下文快照切换后定位稳定性）。
  - [ ] 预期结果：当 `snapshotId` 从 `1001` 切到 `1002` 且 focus flag 重置时，`resolveAutoFocusDecision` 再次返回 `shouldAutoFocus=true`。

- [ ] 修改 `frontend/src/components/datamap/DataMapContainer.render.test.jsx` 或新增交互测试（建议新增 `frontend/src/components/datamap/DataMapContainer.interaction.test.jsx`）
  - [ ] 补用例：启动后在“浏览模式”和“资产图谱”之间切换，验证“快照过滤”“对象筛选芯片状态”等治理过滤条件被保留。
  - [ ] 预期结果：切回“资产图谱”后 `snapshotId` 输入框和已选芯片未被重置。

- [ ] 运行测试：
  - `cd frontend && npm test -- frontend/src/components/datamap/dataMapContextBootstrap.test.js frontend/src/components/datamap/DataMapContainer.render.test.jsx`
  - 期望结果：红（`snapshotId` 相关行为用例失败）。

## 任务 2：修复场景切换 + 快照定位回归

- [ ] 修改 `frontend/src/components/datamap/DataMapContainer.jsx`
  - [ ] 将上下文位点重置从仅 `focusAssetRef` 扩展为 `focusAssetRef + snapshotId` 组合键。
  - [ ] 保留现有场景切换/用户手动覆盖行为，不影响手动 override 语义。
  - [ ] 预期结果：同一 `asset_ref` 在不同 `snapshotId` 时不会继承旧上下文一次性聚焦状态。

- [ ] 再次运行测试：
  - `cd frontend && npm test -- frontend/src/components/datamap/dataMapContextBootstrap.test.js frontend/src/components/datamap/DataMapContainer.render.test.jsx`
  - 期望结果：该任务新增用例转为通过。

## 任务 3：补覆盖追踪回归（关系/证据链）

- [ ] 修改 `frontend/src/pages/datamap-adapter.test.js`
  - [ ] 补例：基于 `relationTypes` 与 `coverageExplanation` 的图谱规范化回归，确认边关系类型与覆盖解释随快照透传。
  - [ ] 预期结果：边对象保留 `relationType` 与 `coverageExplanation`，便于覆盖追踪面板与关系详情稳定展示。

- [ ] 修改 `frontend/src/pages/WorkbenchContextPages.test.jsx`
  - [ ] 补例：带 `snapshot_id + inference_snapshot_id` 的 `/map` 上下文 SSR 渲染存在快照上下文提示，且不出现快照退化误读。
  - [ ] 预期结果：保留“历史快照回放”上下文态信息，无快照命中失败误导。

- [ ] 运行测试：
  - `cd frontend && npm test -- frontend/src/pages/datamap-adapter.test.js frontend/src/pages/WorkbenchContextPages.test.jsx`
  - 期望结果：回归测试通过。

## 任务 4：局部联动验收（最小）

- [ ] 运行命令（按顺序）：
  - `cd frontend && npm test -- src/components/datamap/dataMapContextBootstrap.test.js src/components/datamap/DataMapContainer.render.test.jsx src/pages/datamap-adapter.test.js src/pages/WorkbenchContextPages.test.jsx`
  - `cd frontend && npm run build`
  - 期望结果：关键测试通过、构建成功。

- [ ] 任务内评审点：
  - [ ] 对比变更前后：场景切换后治理过滤状态是否保留。
  - [ ] 验证自动聚焦是否只在上下文轮次（含 `snapshotId`）变更时重置。
  - [ ] 验证覆盖关系字段在图谱适配层和前端回放场景中可读且不丢失。
