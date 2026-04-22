# 项目总览

> 本文用于收口仓库定位、正文真源、前后端模块现状与阅读顺序。当前开发进度、下一动作与责任人仍以 [当前交付状态](./current-delivery-status.md) 为唯一真源；本文不并行维护滚动进度摘要。

---

## 1. 项目定位

- 数据直通车是一个文档优先、代码可运行的治理型工作台项目。
- 当前首轮交付主线固定为“代发 / 薪资域真实库端到端闭环”。这一轮不是把所有工作台一次性产品化，而是先把知识生产、发布投影、数据地图、运行检索与知识包串成一条可联调、可回归、可审查的正式主链路。
- `docs/` 负责正式设计、实施约束和当前状态；`frontend/` 与 `backend/` 负责把已确认的设计落成可运行代码；`ai/` 与 `scripts/` 负责团队协作、评审、桥接和自动化入口。
- 截至本轮梳理，仓库内已有 36 份特性文档、66 份实施计划、25 个前端页面组件入口和 16 个后端接口入口。结构已经基本稳定，当前主要问题不是目录失控，而是“哪些能力已正式收口、哪些仍在建、哪些尚未产品化”需要统一口径。

## 2. 真源与阅读顺序

| 想回答的问题 | 优先入口 | 说明 |
| --- | --- | --- |
| 项目到底要解决什么问题 | [系统设计](../architecture/system-design.md) | 这里定义系统对象、治理边界、主链路、存储分层与接口契约。 |
| 前端工作台应该长成什么样 | [前端工作台设计](../architecture/frontend-workbench-design.md) | 这里定义一级导航、页面结构、状态表达、中文口径与前端硬约束。 |
| 场景和专题能力已经拆到哪一层 | [特性文档目录](../architecture/features/README.md) | 这里看每个正式场景和专题能力是否已经独立成文。 |
| 当前实际做到哪一步 | [当前交付状态](./current-delivery-status.md) | 这里看进行中工作项、近期待验收、主要风险与下一阶段执行顺序。 |
| 这个项目按什么流程推进 | [协作工作流](./collaboration-workflow.md)、[开发手册](./development-manual.md) | 前者定义协作协议和门禁，后者定义开发入口、脚本、模型路由和验活要求。 |
| 前后端接口如何对齐 | 后端 `/v3/api-docs` 与 [前端契约入口](../../frontend/src/api/contracts.ts) | 后端以 `SpringDoc（接口文档生成框架，SpringDoc OpenAPI）` 生成 `OpenAPI（开放接口描述规范，OpenAPI Specification）`，前端按契约层消费。 |

## 3. 仓库结构与代码边界

| 目录 | 当前角色 | 不承担的角色 |
| --- | --- | --- |
| `docs/` | 正式正文真源、设计基线、实施约束、交付状态 | 不承载临时试验稿和外部调研原文 |
| `frontend/` | 工作台界面、路由壳层、状态仓库、浏览器级回归 | 不替代正式设计文档 |
| `backend/` | 领域服务、接口契约、安全、真实库迁移、图读与发布链路 | 不单独维护并行版接口说明 |
| `scripts/` | 启动、联调、验收、桥接和自动化脚本 | 不作为正式规则真源 |
| `ai/` | 技能、代理、上下文、项目级协作资产 | 不作为产品设计正文 |
| `research/` / `third_party/` / `archive/` | 调研、快照、历史材料 | 不作为当前生效方案入口 |

## 4. 前端模块清单

状态口径说明：

- 本节只表达模块成熟度，不记录责任人、最后更新时间和具体下一动作；这些仍以 [当前交付状态](./current-delivery-status.md) 为准。
- `已实现`：当前路由已按 [routes.js](../../frontend/src/routes.js) 的正式入口接入，且本分支已有对应页面实现。
- `在建`：当前页面或壳层已存在，但 [当前交付状态](./current-delivery-status.md) 仍显示为 `implementing（开发中）` 或 `reviewing（测试与评审中）`。
- `规划中`：当前路由仍是后续建设入口，或者对应能力尚未进入正式页面实现。
- `原型评审`：当前只用于结构和交互评审，不纳入正式交付范围。

| 状态 | 模块 | 主要代码入口 | 当前事实 |
| --- | --- | --- | --- |
| 已实现 | 首页总览 | [HomePage.jsx](../../frontend/src/pages/HomePage.jsx) | 首页总览与状态分发已经落地，是工作台默认入口。 |
| 已实现 | 数据地图主入口 | [AssetsPage.jsx](../../frontend/src/pages/AssetsPage.jsx) | `地图 / 业务场景 / 资产图谱` 三个正式视图已接到统一页面壳层。 |
| 已实现 | 知识生产台主入口 | [KnowledgePage.jsx](../../frontend/src/pages/KnowledgePage.jsx) | 材料接入、资产建模、领域配置已经合并在统一工作台入口下。 |
| 已实现 | 发布中心 | [PublishCenterPage.jsx](../../frontend/src/pages/PublishCenterPage.jsx) | 发布检查、版本与回滚相关入口已经成型。 |
| 已实现 | 运行决策台 | [KnowledgePackageWorkbenchPage.jsx](../../frontend/src/pages/KnowledgePackageWorkbenchPage.jsx) | 运行检索、澄清结果和知识包主线已经有正式页面承载。 |
| 已实现 | 全局工具区 | [WorkspacePage.jsx](../../frontend/src/pages/WorkspacePage.jsx)、[SystemPage.jsx](../../frontend/src/pages/SystemPage.jsx) | 个人协作、系统设置、大模型配置和提示词配置已收口到工具区。 |
| 在建 | 顶部登录闭环与全局壳层安全收口 | [App.jsx](../../frontend/src/App.jsx) | 顶部角色切换、登录、退出已经接入；当前还在做代码检视和页面人工回归。 |
| 在建 | 审批与导出 | [ApprovalExportPage.jsx](../../frontend/src/pages/ApprovalExportPage.jsx) | 页面已存在，且已接入真实服务导出记录查询，但整体仍是样例态模块。 |
| 在建 | 监控与审计 | [MonitoringAuditPage.jsx](../../frontend/src/pages/MonitoringAuditPage.jsx) | 页面已存在，但监控、审计、影响分析的正式链路仍未全部收口。 |
| 在建 | 知识生产台任务主线页最终稿收口 | [KnowledgePage.jsx](../../frontend/src/pages/KnowledgePage.jsx) | 主线结构、折叠摘要和跨台跳转规则已经定稿，仍缺最终测试报告与评审收口。 |
| 规划中 | 数据地图语义视图、字典、派生规则 | [routes.js](../../frontend/src/routes.js) | `/map/views`、`/map/dicts`、`/map/rules` 仍处于后续建设。 |
| 规划中 | 知识生产质量反馈 | [routes.js](../../frontend/src/routes.js) | `/production/feedback` 仍未进入正式页面实现。 |
| 原型评审 | 原型工作台 | [PrototypeIndexPage.jsx](../../frontend/src/pages/PrototypeIndexPage.jsx) 与 `frontend/src/pages/prototypes/` | 仅用于原型评审，不计入正式交付范围。 |

## 5. 后端模块清单

状态口径说明：

- 本节只表达后端能力的产品化成熟度，不替代 [当前交付状态](./current-delivery-status.md) 中的任务状态。
- `已实现`：后端已经暴露正式接口，且该能力已经进入当前主链路或正式前端消费。
- `在建`：后端已有接口或服务入口，但当前交付状态仍在补契约、补回归或补产品化链路。
- `规划中`：后端已有探索性接口或能力雏形，但尚未成为当前阶段的正式工作台能力。

| 状态 | 模块 | 主要代码入口 | 当前事实 |
| --- | --- | --- | --- |
| 已实现 | 系统鉴权与访问控制 | [AuthController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/AuthController.java)、[SecurityConfig.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/SecurityConfig.java) | 登录令牌、`JWT（JSON Web Token，令牌格式）` 鉴权、角色矩阵和接口限流已经接入正式运行链路。 |
| 已实现 | 领域、场景与治理对象基础接口 | [DomainController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DomainController.java)、[SceneController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/SceneController.java)、[GraphAssetController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/GraphAssetController.java) | 领域、场景、方案、覆盖声明、策略对象、输入槽位模式等基础治理对象已经有正式接口入口。 |
| 已实现 | 导入、候选图谱与任务恢复主线 | [ImportController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ImportController.java) | 预处理、流式事件、任务恢复、候选图谱评审与确认主线已打通首轮闭环。 |
| 已实现 | 数据地图图读与节点详情 | [DataMapGraphController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DataMapGraphController.java)、[DataMapController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DataMapController.java) | 图读、节点详情、字段列、业务域和资产谱系查询已形成正式入口。 |
| 已实现 | 运行检索、场景召回与方案选择 | [GraphRagController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/GraphRagController.java)、[NlController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/NlController.java) | `GraphRAG（图检索增强生成，Graph Retrieval-Augmented Generation）` 查询、场景召回、方案选择和自然语言查询反馈已具备接口基础。 |
| 已实现 | 服务导出与系统配置 | [ServiceSpecController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ServiceSpecController.java)、[LlmPreprocessConfigController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/LlmPreprocessConfigController.java) | 服务级导出记录、大模型预处理配置和提示词配置都已有正式接口。 |
| 在建 | 跨场景统一实体层与领域级图谱融合 | [Neo4jGraphReadService.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/Neo4jGraphReadService.java)、[DataMapGraphController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DataMapGraphController.java) | 统一实体层、关系冻结、可见性快照与图投影一致性仍在正式收口。 |
| 在建 | 场景版本、引用与差异演进 | [SceneEvolutionController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/SceneEvolutionController.java) | 场景引用、版本、差异对比已经有接口入口，但仍需与发布链和前端体验继续对齐。 |
| 在建 | 对齐报告、影响分析与监控支撑 | [AlignmentController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/AlignmentController.java)、[ImpactAnalysisController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ImpactAnalysisController.java)、[ImpactController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ImpactController.java) | 后端接口已经存在，但对应前端模块仍未完成正式化。 |
| 规划中 | 语义视图正式消费 | [SemanticViewController.java](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/SemanticViewController.java) | 后端已有语义视图接口入口，前端正式路由仍处于后续建设。 |

## 6. 本轮梳理结论

- 项目当前已经形成三层稳定骨架：文档真源、可运行代码、交付状态真源。
- 前端一级工作台和全局壳层已经较完整，但“审批与导出”“监控与审计”仍停留在样例态或在建态。
- 后端接口面已经比正式前端消费面更宽，后续重点不是继续铺接口，而是把已存在能力按交付状态逐项收口成正式链路。
- 下一阶段的具体执行顺序已经回写到 [当前交付状态](./current-delivery-status.md)；如需判断“下一步先做什么”，以该文件中的顺序表为准，不再单独维护其他清单。
