# 当前交付状态

> 本文件是项目内 `Delivery Status（交付状态）` 的唯一真源，用于团队内部日常推进、任务接力和阶段验收前对齐。历史事项只回填仍在推进或即将接力的工作，不追求补全已失真的旧周报。

---

## 仓库总览

- 当前阶段：代发 / 薪资域真实库端到端闭环首批实现与真实库联调
- 本阶段目标：一是保持“代发 / 薪资域真实库端到端闭环”作为当前主目标；二是把开发流程基线收口到“特性文档 -> 实施计划 -> Codex 实现 -> Claude Code 代码检视 -> 测试验收 -> 分支收尾”的正式链路，并继续推进数据地图高可见层与知识生产链路实现。
- 本阶段退出条件：
  - [x] 最高原则已声明交付状态唯一真源
  - [x] 协作工作流已定义交付状态更新门禁
  - [x] 交付状态字段与状态枚举已有正式契约
  - [x] “特性文档 -> 实施计划 -> 实现”硬门禁已写入最高原则与协作工作流
  - [x] 代发 / 薪资域端到端主链路已回写主文档与相关特性文档
  - [x] 真实数据库建模与迁移方案已落地
  - [x] 知识生产链路、发布投影与数据地图、运行检索与知识包三份实施计划已创建
  - [x] 当前阶段工作项已按父子关系重新收口到本文件
  - [x] 数据地图高可见层规则已回写主文档
  - [x] 项目级流程 skill 已补齐并接入 `AGENTS.md`
  - [x] `Codex` 开发、`Claude Code` 代码检视与测试文档门禁已写回协作流程
  - [x] 数据地图对应特性文档已补齐并满足最低完备性
  - [x] 数据地图实施计划已创建并显式引用对应特性文档
  - [x] 特性文档与实施计划同步产出门禁已写回共享协作协议、特性文档标准与特性缺口巡检规则
- 最近更新时间：2026-03-31

## 进行中工作项

| 工作项 | 归属阶段 | 来源设计 | 来源计划 | 测试文档 | 当前状态 | 最新完成 | 下一动作 | 退出条件 | 阻塞项 | 责任人 | 最后更新时间 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 导入中活图谱首轮实现 | 知识生产链路实施子线 | [前端工作台设计](../architecture/frontend-workbench-design.md)、[系统设计](../architecture/system-design.md)、[解析抽取与证据确认](../architecture/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation.md) | [2026-03-30-import-live-graph-implementation-plan.md](../plans/2026-03-30-import-live-graph-implementation-plan.md) | [02-parsing-and-evidence-confirmation-test-report.md](../testing/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation-test-report.md) | `reviewing（测试与评审中）` | 已完成后端 `candidateGraph` 快照持久化、`/api/import/preprocess-stream` 的 `graph_patch` 事件、`KnowledgePage` 的“左侧候选实体图谱 + 右侧 Inspector（检查面板）”壳层、前端状态机与 `OpenAPI（开放接口描述规范，OpenAPI Specification）` 类型同步；关键后端契约测试、前端渲染测试与构建均已通过，`http://127.0.0.1:8082/v3/api-docs` 已可返回最新契约 | 进入代码检视与浏览器级联调，确认是否需要把当前“单批次图谱补丁”继续细化为更密集的增长事件 | 后端流式契约、任务恢复快照、前端空态与默认 Inspector 已稳定，补充代码检视与浏览器联调后即可并入知识生产主链路 | 当前 `graph_patch` 仍是“归一完成后发出首批补丁”的首版，尚未补 `E2E` 级别交互回归 | Codex（实现） | 2026-03-30 |
| 交付状态治理文档落地 | 工程协作治理基线 | [总体原则](standards/overall-principles.md)、[协作工作流](collaboration-workflow.md)、[交付状态契约](standards/delivery-status-contract.md) | [2026-03-28-delivery-status-governance-implementation-plan.md](../plans/2026-03-28-delivery-status-governance-implementation-plan.md) | 纯文档同步，豁免 | `done（完成）` | 已将“先特性文档、再实施计划、再按计划实现”的硬门禁写入 [AGENTS.md](../../AGENTS.md) 与 [协作工作流](collaboration-workflow.md)，并根据 OpenAI `Harness engineering` 与终端中的 `Claude Code` 只读评审反馈补齐“仓库即执行真源、脚手架任务前置、机械化门禁分阶段激活、Golden Path（黄金路径样例）试点、坏模式回收并入评审、豁免记录四要素”，同时同步更新 [术语表](../glossary.md) | 将这套增补条款作为后续共享规则调整与端到端主链路推进的统一约束继续执行 | 流程基线文档、技能、状态契约、术语表与新增增补条款已一致，团队可按统一规则接力 | 无 | 团队共享 | 2026-03-29 |
| 特性缺口文档补齐与专题拆分 | 文档真源收口 | [知识图谱与数据地图方案](../architecture/system-design.md)、[前端界面与工作台设计](../architecture/frontend-workbench-design.md)、[特性文档目录](../architecture/features/README.md) | [2026-03-30-feature-gap-docs-implementation-plan.md](../plans/2026-03-30-feature-gap-docs-implementation-plan.md) | 纯文档同步，豁免 | `reviewing（评审验证中）` | 已补齐 [07 发布检查、灰度发布与回滚](../architecture/features/iteration-02-runtime-and-governance/07-publish-check-gray-release-and-rollback.md)、[11 监控审计与影响分析](../architecture/features/iteration-02-runtime-and-governance/11-monitoring-audit-and-impact-analysis.md)、[12 全局壳层、导航与跨工作台上下文跳转](../architecture/features/iteration-02-runtime-and-governance/12-global-shell-navigation-and-context-handoff.md)、[12a 首页总览与状态分发](../architecture/features/iteration-02-runtime-and-governance/12a-home-overview-and-state-dispatch.md) 的最低完备性，并新增 [03a 字典治理](../architecture/features/iteration-01-knowledge-production/03a-dictionary-governance.md)、[04a 推理资产详情与前台可见](../architecture/features/iteration-01-knowledge-production/04a-inference-asset-detail-and-visibility.md)、[05a 表间关联关系治理](../architecture/features/iteration-01-knowledge-production/05a-join-relation-governance.md)、[12b OpenAPI 契约与前端消费](../architecture/features/iteration-02-runtime-and-governance/12b-openapi-contract-and-frontend-consumption.md) 四份专题特性文档，同时同步更新了 [特性文档拆分与迭代路线设计稿](../plans/2026-03-28-feature-doc-iteration-roadmap-design.md) 与特性目录索引 | 继续完成专题文档合法性收口和其余 `09 / 04 / 05 / 06` 的最低完备性补齐，并进入特性评审闭环 | 新增专题文档已被目录与路线稿正式接纳，交付状态、特性目录与实施计划三处口径一致，且剩余高优先文档全部补齐最低完备性 | `09`、`04`、`05`、`06` 仍未补齐完整门禁；专题文档评审刚完成首轮，尚未做二次修订 | Codex（文档推进） | 2026-03-30 |
| 代发 / 薪资域真实库端到端闭环 | 首轮主链路收口 | [知识图谱与数据地图方案](../architecture/system-design.md)、[材料接入与来源接入契约登记](../architecture/features/iteration-01-knowledge-production/01-source-intake-registration.md)、[解析抽取与证据确认](../architecture/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation.md)、[资产建模与治理对象编辑](../architecture/features/iteration-01-knowledge-production/03-asset-modeling-and-governance-editing.md)、[运行决策与知识包生成](../architecture/features/iteration-02-runtime-and-governance/08-runtime-decision-and-knowledge-package.md)、[数据地图浏览与覆盖追踪](../architecture/features/iteration-02-runtime-and-governance/10-data-map-browsing-and-coverage-tracing.md) | [真实数据库建模与迁移方案](../plans/2026-03-29-real-database-control-asset-implementation-plan.md)、[知识生产链路实施计划](../plans/2026-03-29-payroll-knowledge-production-implementation-plan.md)、[发布投影与数据地图实施计划](../plans/2026-03-29-payroll-publish-datamap-implementation-plan.md)、[运行检索与知识包实施计划](../plans/2026-03-29-payroll-runtime-knowledge-package-implementation-plan.md) | [10-data-map-browsing-and-coverage-tracing-test-report.md](../testing/features/iteration-02-runtime-and-governance/10-data-map-browsing-and-coverage-tracing-test-report.md) | `executing_plan（按计划执行中）` | 已完成候选场景 / 证据持久化首段实现：新增 `V14` 后续迁移承接候选资产表，导入质检确认会落库候选 `Scene（业务场景）/ Evidence（证据）`；发布链路补齐 `snapshotId` 返回并把快照标识传入图谱投影状态；运行检索已能在 `代发 / 薪资域` 多场景间区分“代发明细查询 / 代发批次结果查询”，混合问题返回 `Knowledge Decomposition Result（知识拆解结果）` 风格的澄清结果 | 继续按数据地图与运行工作台子线补 UI 验证、快照锁定跳转与更多多场景回放用例，并审查并行工作区中的其余候选资产实现是否可合并 | 候选持久化、正式资产发布快照、图谱投影状态与多场景知识包首轮闭环全部通过定向测试，随后进入数据地图与工作台体验收口 | 并行实现工作区仍有未审查的候选资产相关改动；前端当前只补了主入口编排，尚未补齐更细的交互断言 | Codex（方案执行） | 2026-03-29 |
| 数据地图高可见层并行支撑 | 代发 / 薪资域端到端闭环子线 | [前端工作台设计](../architecture/frontend-workbench-design.md)、[数据地图浏览与覆盖追踪特性文档](../architecture/features/iteration-02-runtime-and-governance/10-data-map-browsing-and-coverage-tracing.md)、[2026-03-29 数据地图高可见层方案可行性与缺陷评审稿](../plans/2026-03-29-data-map-high-visibility-feasibility-review-design.md) | [2026-03-29-data-map-high-visibility-implementation-plan.md](../plans/2026-03-29-data-map-high-visibility-implementation-plan.md)、[发布投影与数据地图实施计划](../plans/2026-03-29-payroll-publish-datamap-implementation-plan.md) | [10-data-map-browsing-and-coverage-tracing-test-report.md](../testing/features/iteration-02-runtime-and-governance/10-data-map-browsing-and-coverage-tracing-test-report.md) | `done（完成）` | 已完成“画布主导”方案收敛，并把自动切 `lineage（资产图谱）`、单轮自动定位、节点优先于路径、自动切 `路径高亮` 与分级降级规则回写到设计与特性文档；前端目标测试、后端主路径集成测试、`Claude Code` 代码检视复核和服务级验活全部通过，`http://127.0.0.1:5173/` 与 `http://127.0.0.1:8080/v3/api-docs` 均返回 `200 OK` | 进入 `finishing-a-development-branch（开发分支收尾技能）`，等待分支处理选择，并将已验证能力继续作为端到端主链路的解释子线维护 | 当前工作项已经满足完成条件，只剩分支处理动作 | 无 | Review Agent（收尾推进） | 2026-03-29 |

## 下一阶段工作

| 工作项 | 启动前置条件 | 对应设计/计划 | 启动后第一步 | 责任人 |
| --- | --- | --- | --- | --- |
| 特性文档第二轮补齐与评审修订 | 首轮专题文档拆分与运行治理文档最低完备性已落盘 | [2026-03-30-feature-gap-docs-implementation-plan.md](../plans/2026-03-30-feature-gap-docs-implementation-plan.md)、[特性文档拆分与迭代路线设计稿](../plans/2026-03-28-feature-doc-iteration-roadmap-design.md) | 先按评审意见同步目录规则与交付状态，再补 `09 / 04 / 05 / 06` 的门禁缺口 | Codex（文档推进） |
| 知识生产链路实施 | 候选资产表与导入质检确认已落地 | [知识生产链路实施计划](../plans/2026-03-29-payroll-knowledge-production-implementation-plan.md) | 继续把候选资产确认结果接入正式治理对象编辑与完成态收口 | Codex（实现） |
| 发布投影与数据地图实施 | 发布响应已返回快照标识且图谱投影状态可读 | [发布投影与数据地图实施计划](../plans/2026-03-29-payroll-publish-datamap-implementation-plan.md) | 以前端数据地图为入口，补快照定位、场景切换与更多投影回归验证 | Codex（实现） |
| 运行检索与知识包实施 | 多场景召回、计划选择与澄清结果已打通首轮后端闭环 | [运行检索与知识包实施计划](../plans/2026-03-29-payroll-runtime-knowledge-package-implementation-plan.md) | 继续补前端工作台针对澄清结果的交互断言与更多场景覆盖测试 | Codex（实现） |
| 团队接力门禁试运行 | 代发 / 薪资域主链路已进入按计划实现 | [协作工作流](collaboration-workflow.md)、[交付状态契约](standards/delivery-status-contract.md) | 以端到端主链路为样本，在“文档收口 / 计划落地 / 实现启动”三个时点继续更新本文件 | 团队共享 |
| 状态巡检驱动的半自动多智能体试运行 | 数据地图样本已进入真实状态流转 | [协作工作流](collaboration-workflow.md)、[交付状态契约](standards/delivery-status-contract.md)、[2026-03-29-data-map-high-visibility-implementation-plan.md](../plans/2026-03-29-data-map-high-visibility-implementation-plan.md) | 启用状态巡检类 automation，对 `planning / implementing / reviewing / fixing` 状态推荐对应角色 agent，并以数据地图样本校验提示是否顺手 | 团队共享 |
| 特性缺口巡检补“计划缺口”维度 | 共享协作协议、特性文档标准与巡检 skill 已同步新门禁 | [协作工作流](collaboration-workflow.md)、[scenario-feature-doc-standard.md](standards/scenario-feature-doc-standard.md)、[feature-doc-coverage-mapping](../../ai/skills/feature-doc-coverage-mapping/SKILL.md) | 下一轮特性缺口巡检输出中，显式区分“缺特性文档”和“缺实施计划”两类缺口。 | 团队共享 |

## 近期待验收

| 工作项 | 验证命令 | 验证结果 | 状态 | 责任人 |
| --- | --- | --- | --- | --- |
| 导入中活图谱首轮实现 | `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest,OpenApiDocumentationIntegrationTest,ImportPreprocessStreamApiIntegrationTest test`；`cd frontend && npm test -- src/components/knowledge/importLiveGraphState.test.js src/pages/KnowledgePage.render.test.jsx`；`cd frontend && npm run build`；`cd backend && mvn -q spring-boot:run -Dspring-boot.run.arguments=--server.port=8082`；`cd frontend && OPENAPI_SCHEMA_URL=http://127.0.0.1:8082/v3/api-docs npm run generate:openapi` | 已通过：后端任务恢复快照、流式 `graph_patch` 契约、前端活图谱空态与状态机测试、前端构建、`8082` 端口接口探活与 `OpenAPI` 类型生成全部完成 | `reviewing（测试与评审中）` | Codex（实现） |
| 交付状态治理文档落地 | `rg -n "current-delivery-status|delivery-status-contract|特性文档|实施计划|不得开工" AGENTS.md README.md docs/README.md docs/plans/README.md docs/engineering/collaboration-workflow.md docs/engineering/standards/overall-principles.md docs/engineering/standards/delivery-status-contract.md docs/engineering/current-delivery-status.md` | 已命中 `AGENTS.md` 中的硬门禁、`docs/engineering/collaboration-workflow.md` 中的操作化展开、交付状态契约与本文件中的统一入口引用 | `reviewing（评审验证中）` | 团队共享 |
| 端到端主链路设计收口 | `rg -n "当前首轮交付主链路与真实库边界|代发 / 薪资域|真实关系型数据库|图谱存储只承担已发布快照的投影|Knowledge Decomposition Result" docs/architecture/system-design.md docs/architecture/features/iteration-01-knowledge-production/01-source-intake-registration.md docs/architecture/features/iteration-01-knowledge-production/02-parsing-and-evidence-confirmation.md docs/architecture/features/iteration-01-knowledge-production/03-asset-modeling-and-governance-editing.md docs/architecture/features/iteration-02-runtime-and-governance/08-runtime-decision-and-knowledge-package.md docs/architecture/features/iteration-02-runtime-and-governance/10-data-map-browsing-and-coverage-tracing.md` | 需命中“代发 / 薪资域多场景”“真实关系库基线”“图谱仅投影”和“知识拆解结果”等口径 | `passed（已通过）` | Codex（文档推进） |
| 四份实施计划落地 | `ls docs/plans/2026-03-29-*-implementation-plan.md` | 已生成真实数据库、知识生产、发布投影与数据地图、运行检索与知识包四份计划 | `passed（已通过）` | Codex（计划编写） |
| 首批真实库切口实现 | `cd backend && mvn -q test && mvn -q -DskipTests package` | 已通过：`materialId` 已出现在导入响应与任务详情中，`caliber_source_material` 已持久化来源材料，全量后端测试与打包通过 | `passed（已通过）` | Codex（实现） |
| 本地 MySQL 真实库探活 | `cd backend && mvn -q spring-boot:run`，随后 `curl -sSf http://127.0.0.1:8080/v3/api-docs | jq -r '.openapi, .info.title, (.paths | length)'` | 已通过：后端在本机 MySQL `caliber` 库成功迁移到 `v14` 并启动，`/v3/api-docs` 返回 `3.1.0 / 数据直通车 API / 67` | `passed（已通过）` | Codex（联调） |

## 主要风险与外部依赖

| 风险/依赖 | 影响范围 | 当前状态 | 应对动作 | 责任人 |
| --- | --- | --- | --- | --- |
| `Flyway（数据库迁移工具）` 对 MySQL 9.3 仅给出“未测试”告警 | 后续继续升级 MySQL 或 Flyway 版本时仍需回归验证迁移 | 已缓解 | 当前已通过 `flyway-mysql`、迁移脚本兼容收敛与本地真实库探活打通运行链路；后续升级数据库版本时继续保留兼容性测试 | Codex（方案执行） |
| 并行实现工作区已出现候选场景 / 证据持久化相关新增文件，但主线程尚未完成审查 | 后续知识生产链路实现可能把“并行试作代码”和“已验证主线改动”混在一起 | 已识别 | 在继续推进知识生产链路前，先审查并行工作区结果，只合并通过主线程验证的候选资产实现 | Codex（实现） |
| 数据地图高可见层代码曾先行于正式文档与测试收口 | 若不持续保持“文档真源 -> 计划 -> 实现 -> 验证”顺序，后续高可见层调整仍可能再次失真 | 已识别 | 继续以 [10-data-map-browsing-and-coverage-tracing.md](../architecture/features/iteration-02-runtime-and-governance/10-data-map-browsing-and-coverage-tracing.md) 与对应测试文档作为唯一约束，发现偏差先回写文档与状态真源，再扩功能 | Codex（实现推进） |
| 存量工作缺少最新责任人与真实状态 | 首轮回填尚未全部完成前，本文件仍需继续补真实在途事项 | 已识别 | 回填时只保留能追溯到正式设计、实施计划和真实执行面的事项，并逐项补责任人 | 团队共享 |
| 团队继续在 `README.md`、计划文档或聊天记录中并行维护滚动进度摘要 | 会再次形成多口径，削弱任务接力效率 | 已识别 | 在评审与文档同步中把并行摘要视为契约违规，并统一收口为本文件链接 | 团队共享 |
