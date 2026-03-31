# 解析抽取与证据确认测试文档

> 对应特性文档：`docs/architecture/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation.md`
> 当前阶段：首轮实现已完成，等待补浏览器级联调与代码检视结论

## 1. 测试目标

验证“导入中活图谱”首轮最小闭环是否成立，重点覆盖：

1. `/api/import/preprocess-stream` 是否在 `done` 前发出 `graph_patch`
2. `PreprocessResultDTO` 与 `ImportTaskDTO.preprocessResult` 是否携带可恢复的 `candidateGraph`
3. `KnowledgePage` 是否能在导入态渲染活图谱空态、增量补丁和默认 Inspector
4. 恢复导入任务时是否能从最终快照重新还原图谱

## 2. 测试范围

本轮只覆盖“导入 -> 流式补丁 -> 完成态快照 -> 页面恢复”这一条最小闭环。

## 3. 测试环境

1. 前端：`frontend`，React / D3 / Vitest
2. 后端：`backend`，Spring Boot / MockMvc / Maven
3. 联调口径：前端 `5174`，后端 `8082`

## 4. 预设测试案例

| 编号 | 用例 | 输入 | 预期输出 | 实际结果 |
| --- | --- | --- | --- | --- |
| TC-01 | 流式补丁先于完成态 | 导入一份代发样例材料 | `graph_patch` 在 `done` 前出现 | 已通过：`ImportPreprocessStreamApiIntegrationTest` 命中 `event:graph_patch` 且先于 `event:done` |
| TC-02 | 完成态可恢复候选图 | 查询任务详情 | `preprocessResult.candidateGraph.nodes/edges` 存在 | 已通过：`ImportTaskApiIntegrationTest` 断言任务详情与 `OpenAPI` 同时携带 `candidateGraph` |
| TC-03 | 页面渲染活图谱空态 | 打开 `KnowledgePage` 导入预设 | 出现“候选实体图谱”“正在等待首批实体” | 已通过：`KnowledgePage.render.test.jsx` SSR 断言命中文案 |
| TC-04 | 恢复任务还原图谱 | 恢复刚完成的导入任务 | 画布节点与关系恢复 | 已通过：`importLiveGraphState.test.js` 覆盖快照恢复与选中节点保留 |

## 5. 实际执行命令

1. `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest,OpenApiDocumentationIntegrationTest,ImportPreprocessStreamApiIntegrationTest test`
2. `cd frontend && npm test -- src/components/knowledge/importLiveGraphState.test.js src/pages/KnowledgePage.render.test.jsx`
3. `cd frontend && npm run build`
4. `cd backend && mvn -q spring-boot:run -Dspring-boot.run.arguments=--server.port=8082`
5. `cd frontend && OPENAPI_SCHEMA_URL=http://127.0.0.1:8082/v3/api-docs npm run generate:openapi`

## 6. 结果摘要

1. 后端已在 `PreprocessResultDTO` 中持久化 `candidateGraph`，并在 `/api/import/preprocess-stream` 中追加 `graph_patch` 事件。
2. 前端 `KnowledgePage` 已接入导入中活图谱壳层，空态、默认 `Inspector`、节点点击详情与任务恢复共用同一状态机。
3. `OpenAPI` 类型快照已重新生成，前后端契约名词已对齐。

## 7. 剩余风险

1. 当前 `graph_patch` 为“归一完成后发出首批图谱补丁”的首版实现，尚未细化为更高频的多批次增长动画。
2. 仍缺少真实浏览器联调与交互级 `E2E（端到端，End-to-End）` 回归，当前以前后端契约测试与前端渲染/状态机测试为主。
