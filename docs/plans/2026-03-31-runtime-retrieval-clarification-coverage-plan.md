# 运行检索与知识包更多场景覆盖测试实施计划（2026-03-31）

> 目标：在「运行决策台」补齐多场景命中、降级链路与澄清分支的 TDD 覆盖，并统一前后端契约字段（`runtimeMode` / `degradeReasonCodes` / `inferenceSnapshotId`）。

## 任务清单（每项 2-5 分钟）

- [x] 任务 1（2分钟）：仓库 readiness 与前置条件确认（文档端）
  - 文件：`docs/engineering/current-delivery-status.md`
  - 产物：确认工件为 `planning -> implementing`，本项下一动作从“澄清分支收口”切到“多场景/降级回归补齐”。
  - 验证命令：
    - `rg -n "运行检索与知识包更多场景覆盖测试|运行决策台|前端工作台" docs/engineering/current-delivery-status.md`
  - 预期：
    - 当前项存在且目标清晰（多场景 + 降级 + 澄清）；
    - `最近更新` 与 `近期待验收` 有本次任务入口。
  - 问题清单：
    - 未确认是否有 feature/doc 最新分支（影响范围：文档先行约束）。
  - 风险评估：
    - 0.5 - 文档状态未更新导致并行实现与测试口径偏离。

- [x] 任务 2（5分钟）：补充后端红色测试（先失败）
  - 文件：`backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
  - 代码变更：
    - 增加 `degradeReasonCodes`/`runtimeMode` 的断言；
    - 增加跨场景澄清分支字段断言（包含 `reasonCode`/`runtimeMode`/`degradeReasonCodes`/`trace.inferenceSnapshotId`）；
    - 增加模板场景 `IDENTIFIER_REQUIRED` 降级链路测试（同样断言 trace 与降级字段）。
  - 验证命令：
    - `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`
  - 预期：
    - 现有实现未补齐字段时，测试失败（Red）。
  - 问题清单：
    - 测试环境中 `trace.inferenceSnapshotId` 语义是否应与 `trace.snapshotId` 一致需确认。
  - 风险评估：
    - 0.6 - 当前接口返回字段语义偏差会导致测试长期失效。

- [x] 任务 3（5分钟）：后端契约与查询服务实现（Green）
  - 文件：
    - `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackageTraceDTO.java`
    - `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/graphrag/KnowledgePackageQueryAppService.java`
  - 代码变更：
    - `KnowledgePackageTraceDTO` 新增 `inferenceSnapshotId`;
    - `KnowledgePackageQueryAppService` 中统一透传 `runtimeMode / degradeReasonCodes / inferenceSnapshotId`；
    - 澄清分支与拒绝分支附带降级字段，`withTrace(...)` 统一补充。
  - 验证命令：
    - `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`
  - 预期：
    - 新增测试通过；
    - 回归字段出现在 JSON：`runtimeMode`、`degradeReasonCodes`、`trace.inferenceSnapshotId`。
  - 问题清单：
    - `runtimeMode` 取值规范未统一；需保证前端渲染不依赖具体语义。
  - 风险评估：
    - 0.5 - 兼容历史前端读取时，新增字段仅附加不破坏旧用法。

- [x] 任务 4（4分钟）：前端工作台交互与展示回归（Green）
  - 文件：
    - `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
    - `frontend/src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx`
  - 代码变更：
    - 在“知识包摘要”与“澄清”卡片中展示 `runtimeMode` 和 `degradeReasonCodes`；
    - 澄清分支显示 `reasonCode`、`degradeReasonCodes`、`trace.inferenceSnapshotId`；
    - 覆盖子问题链路与模板场景降级提示（`IDENTIFIER_REQUIRED`）。
  - 验证命令：
    - `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx`
  - 预期：
    - 新增断言全部通过，未回归现有子问题按钮链路。
  - 问题清单：
    - 测试依赖 `fetch` mock 组装稳定性需保持。
  - 风险评估：
    - 0.4 - UI 文案改动影响快照或历史截图验证。

- [x] 任务 5（2分钟）：服务契约对齐（前端类型）
  - 文件：`frontend/src/types/openapi.d.ts`
  - 代码变更：
    - 更新 `KnowledgePackageQueryCmd` 增补场景/方案/快照/slotHints 字段；
    - `KnowledgePackageDTO` 增补 `runtimeMode / degradeReasonCodes`；
    - `KnowledgePackageTraceDTO` 增补 `inferenceSnapshotId`；
    - `KnowledgePackageClarificationDTO` 增补 `planCandidates / mergeHints / reasonCode`。
  - 验证命令：
    - `rg -n "KnowledgePackageTraceDTO|KnowledgePackageQueryCmd|runtimeMode|degradeReasonCodes|inferenceSnapshotId" frontend/src/types/openapi.d.ts`
  - 预期：
    - 类型覆盖与后端返回字段一致。
  - 问题清单：
    - openapi 源文件为手工维护场景下易漂移。
  - 风险评估：
    - 0.2 - 未做自动同步时需手工保持一致版本。

- [x] 任务 6（1分钟）：状态文档收口 + 本次 run 记事
  - 文件：
    - `docs/engineering/current-delivery-status.md`
    - `/Users/rlc/.config/superpowers/automations/automation-3/memory.md`
  - 代码变更：
    - 更新项目信息状态为“实现中/验收命令新增”；
    - memory 记录本轮时间与已提交范围。
  - 验证命令：
    - `rg -n "运行检索与知识包更多场景覆盖测试" docs/engineering/current-delivery-status.md`
  - 预期：
    - 文档链路与本轮代码范围可追溯。
  - 问题清单：
    - 若未同步到 `current-delivery-status`，会违反仓库流程。
  - 风险评估：
    - 0.1 - 文档未更新可能导致后续交接误判。

## 里程碑交付
- 完成 `backend` / `frontend` 的红绿回归；
- `run all` 命令列表见验收（本文件末）。

## 验收命令（最终）
- `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`
- `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx`
