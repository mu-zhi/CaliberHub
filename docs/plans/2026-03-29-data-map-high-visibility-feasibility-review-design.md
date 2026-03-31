# 2026-03-29 数据地图高可见层方案可行性与缺陷评审稿

本文是围绕“数据地图高可见层”当前方案与现状实现所做的只读评审过程稿，用于在进入下一轮特性文档补齐、实施计划编写和外部讨论前，先把可行性判断、主要缺陷和优先动作写清楚。正式设计真源仍以 [`system-design.md`](../architecture/system-design.md)、[`frontend-workbench-design.md`](../architecture/frontend-workbench-design.md) 与对应特性文档为准；本文不替代主文档，只负责沉淀当前评审结论。

## 一、评审范围

本次评审只覆盖“数据地图高可见层”这一条当前在途主线，不扩展到整个项目的所有业务样板场景。评审输入主要来自以下文件与实现现状：

1. [`docs/architecture/system-design.md`](../architecture/system-design.md)
2. [`docs/architecture/frontend-workbench-design.md`](../architecture/frontend-workbench-design.md)
3. [`docs/architecture/features/iteration-02-runtime-and-governance/10-数据地图浏览与覆盖追踪.md`](../architecture/features/iteration-02-runtime-and-governance/10-数据地图浏览与覆盖追踪.md)
4. [`docs/engineering/collaboration-workflow.md`](../engineering/collaboration-workflow.md)
5. [`docs/engineering/current-delivery-status.md`](../engineering/current-delivery-status.md)
6. [`frontend/src/components/datamap/DataMapContainer.jsx`](../../frontend/src/components/datamap/DataMapContainer.jsx)
7. [`frontend/src/pages/datamap-adapter.js`](../../frontend/src/pages/datamap-adapter.js)
8. [`backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DataMapController.java`](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DataMapController.java)
9. [`backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DataMapGraphController.java`](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DataMapGraphController.java)
10. [`backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/DataMapQueryAppService.java`](../../backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/DataMapQueryAppService.java)
11. [`backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/GraphQueryService.java`](../../backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/GraphQueryService.java)

## 二、当前事实基线

### 2.1 方案层事实

1. 项目总体定位是“受控知识底座”，不是直接执行取数系统；数据地图被定义为治理产品面的核心解释界面。
2. 数据地图高可见层当前强调“画布主导”，并新增了带 `ctx` 进入时自动切 `lineage（资产图谱）`、单轮自动定位、节点优先于路径、自动切 `路径高亮` 与分级降级等规则，相关结论已经回写到 [`frontend-workbench-design.md`](../architecture/frontend-workbench-design.md)。
3. [`current-delivery-status.md`](../engineering/current-delivery-status.md) 明确写明：数据地图高可见层仍处于 `brainstorming（方案中）`，下一动作是补齐对应特性文档，未到实施计划与实现阶段。

### 2.2 代码层事实

1. 前端数据地图核心容器 [`DataMapContainer.jsx`](../../frontend/src/components/datamap/DataMapContainer.jsx) 已达到 1571 行，已经承载了浏览模式、资产图谱模式、过滤器、节点详情、关系详情、影响分析、日志面板和带上下文跳转运行决策台等复杂能力。
2. 前端适配层 [`datamap-adapter.js`](../../frontend/src/pages/datamap-adapter.js) 已经对接了列浏览、图谱查询、节点详情、影响分析和场景详情接口，说明高可见层并非停留在原型状态，而是已有成型实现骨架。
3. 后端接口已经同时存在两条数据地图链路：
   - [`DataMapController.java`](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DataMapController.java) 提供 `/api/assets/columns`、`/api/assets/business-domains`、`/api/assets/lineage/{sceneId}` 一类偏旧的树列与血缘接口。
   - [`DataMapGraphController.java`](../../backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DataMapGraphController.java) 提供 `/api/datamap/graph`、`/api/datamap/node/{id}/detail`、`/api/datamap/impact-analysis` 一类偏新的一体化图谱接口。
4. 后端查询实现也存在“双轨”：
   - [`DataMapQueryAppService.java`](../../backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/DataMapQueryAppService.java) 仍保留以 `SceneDTO` 和列树分组为核心的旧路径。
   - [`GraphQueryService.java`](../../backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/datamap/GraphQueryService.java) 则转向 `GraphSceneBundle`、`DataMapGraphDtoAdapter` 与过滤选项。

### 2.3 验证层事实

1. 前端当前可见的数据地图专项测试主要是 [`frontend/src/pages/datamap-adapter.test.js`](../../frontend/src/pages/datamap-adapter.test.js)，尚未体现对高可见层核心交互的成体系覆盖。
2. 后端已有集成流中出现了数据地图相关校验，尤其是 `MvpKnowledgeGraphFlowIntegrationTest` 已覆盖 `/api/datamap/graph`、节点详情和影响分析调用，但从当前入口看，更接近“链路可达”验证，不等同于“高可见层行为契约”验证。

## 三、可行性判断

### 3.1 本方案可落地的前提

在当前边界下，数据地图高可见层是可落地的，但前提不是继续横向扩张能力，而是先把“现有实现已经承载了什么”与“正式文档准备承诺什么”对齐。当前已有前后端接口、容器级页面实现和集成链路，说明技术上并非从零起步；真正的难点是治理化落地，而不是纯编码可行性。

### 3.2 本方案最可能成功的原因

1. 数据地图承担的是“治理对象解释与追踪”而不是“直接执行查询”，与项目“受控知识底座”的定位一致，边界相对清晰。
2. 前后端都已经具备可工作的实现底盘，不需要从概念验证重新开始，具备收敛为正式能力的现实基础。
3. 当前新增的高可见层规则具有明确用户价值，例如上下文带入后的自动定位、节点优先于路径和分级降级，这些都能直接改善跨工作台追踪体验。

## 四、主要缺陷与风险

以下风险按当前危险程度排序。

### 4.1 文档与代码已出现明显错位

这是当前最危险的问题。[`current-delivery-status.md`](../engineering/current-delivery-status.md) 与协作工作流都把数据地图高可见层标记为“仍在补特性文档、尚未进入计划与实现”，但 [`DataMapContainer.jsx`](../../frontend/src/components/datamap/DataMapContainer.jsx) 已经承载了大量目标能力。这意味着当前流程不是在前置约束实现，而是在为已有代码补票。

如果继续沿用“文档还没完成，所以实现不能开始”的表述，团队会同时面对两套现实：

1. 流程现实：尚未允许实现。
2. 代码现实：实现已经存在并持续演化。

这会让后续所有门禁都失去约束力，只剩补材料的摩擦成本。

### 4.2 数据地图接口与实现存在双轨结构

后端同时保留 `/api/assets/*` 与 `/api/datamap/*` 两套入口，查询实现也同时存在 `DataMapQueryAppService` 与 `GraphQueryService` 两种路径。这种双轨在过渡期可以理解，但如果不尽快明确“哪条是正式高可见层主路径”，后续会出现：

1. 前端适配层和测试难以判断该绑定哪套契约。
2. 特性文档难以准确描述正式输入输出边界。
3. 后续重构时容易把旧路径误当正式能力保留。

### 4.3 特性文档对当前实现承载不足

[`10-数据地图浏览与覆盖追踪.md`](../architecture/features/iteration-02-runtime-and-governance/10-数据地图浏览与覆盖追踪.md) 目前仍偏骨架化，已经写出业务视图、路径视图、覆盖视图和过滤器等总规则，但对以下当前代码中已经存在的行为承载不足：

1. 带 `ctx` 进入时的自动切图谱、自动定位和单轮抢占规则。
2. 节点详情、关系详情、影响分析和日志面板的协同关系。
3. 运行决策台跳转所依赖的上下文打包规则。
4. 旧列树浏览与新图谱浏览之间的关系边界。

如果特性文档不先补齐，后面的实施计划会只能围绕“文档愿景”拆任务，而不能围绕“真实实现差距”拆任务。

### 4.4 前端核心容器体量较大，需先做内部结构评审再决定是否拆分

[`DataMapContainer.jsx`](../../frontend/src/components/datamap/DataMapContainer.jsx) 已达到 1571 行，并集中处理模式切换、查询状态、日志、布局、过滤器、节点与边详情、影响分析、上下文接收和跨工作台跳转。仅凭体量本身，不能直接判定其设计必然失衡；但这至少说明高可见层的产品收敛、状态编排和交互协同正在被一个单容器承接，应该先补一次内部结构评审，再决定是否拆分。当前主要风险是：

1. 若状态职责没有清晰分层，会难以针对高可见层细分行为建立稳定测试。
2. 会让“正式交互能力”和“过渡期试探性 UI”更难区分。
3. 若后续文档收敛方向调整，潜在重构成本会被放大。

### 4.5 测试覆盖与门禁口径尚未跟上高可见层复杂度

当前前端可见的专项测试重点还在适配层，尚未形成针对高可见层核心交互规则的测试矩阵，例如：

1. 带 `ctx` 进入时是否自动切 `lineage（资产图谱）`。
2. 自动定位是否只在单轮上下文中触发一次。
3. 图中未命中目标节点时是否按文档要求降级，而不是伪高亮。
4. 节点优先于路径的视觉和状态逻辑是否稳定。

这会让项目在口头上强调“TDD 和门禁”，但在这条主线上暂时缺少能真正支撑门禁的测试颗粒度。更直接地说，如果数据地图高可见层的核心行为可以在缺少先失败测试与专项门禁的情况下先行落地，那么当前流程对团队来说就不是强约束，而是选择性执行。

### 4.6 协作流程正在继续增重，但尚未先消化现状偏差

从 [`AGENTS.md`](../../AGENTS.md)、[`collaboration-workflow.md`](../engineering/collaboration-workflow.md) 和交付状态文件看，当前项目还在继续补充 skill、门禁和状态同步要求。流程治理本身不是问题，但在“文档与代码已经错位”的背景下继续增重，会让团队更难处理历史先行实现。

当前更需要的是先把数据地图这条主线纳入统一事实，而不是继续抽象新的流程层。

### 4.7 `ctx` 驱动状态缺少完整生命周期定义

高可见层当前非常强调带 `ctx` 进入后的自动切图谱、自动定位、单轮抢占和用户手动操作后的让位规则，但这组规则的状态生命周期仍未在特性文档中完整定义。例如：

1. 从运行决策台带 `ctx` 跳入后，用户手动切换视图再返回，`ctx` 应该清空、保留还是降级为只读提示。
2. 自动定位触发一次后，后续刷新、重进页面或切换视图是否还应继承该轮上下文。
3. 图谱未命中目标节点时，当前页面应该如何回退到非高亮态，并如何保留排障信息。

如果这部分继续只由代码行为隐式决定，后续补测试也只能验证“当前代码怎么做”，而不是验证“这是不是正式契约”。

### 4.8 文档体系双轨会持续干扰评审落点

仓库根 [`README.md`](../../README.md) 仍保留中文编号文档体系导航，而当前协作执行明显已转向 `docs/architecture` 与 `docs/engineering`。如果这一点不明确，评审类文档和后续修正文档很容易再次落到并行真源里，导致同一主题多处维护。

## 五、当前最危险的矛盾判断

当前最危险的矛盾不是“方案太大”，也不是单纯“流程太重”，而是：团队正在用一套“文档前置驱动实现”的流程，管理一个“实现已经先行、文档在追赶”的现实。换句话说，真正的核心矛盾是“流程模型与开发节奏不匹配”；文档、流程、代码错位只是这一矛盾的外在表现。

更具体地说：

1. 代码已经先行承载了高可见层的相当一部分实现。
2. 文档与交付状态仍把这条主线表述为“尚未进入实现”。
3. 流程门禁继续增重，但并没有先把这条已存在的实现纳入统一事实。

如果这个矛盾不先处理，后续不论是补特性文档、写实施计划还是补 TDD 证据，都会变成追赶历史实现，而不是驱动下一轮稳态演进。更糟的是，补票动作还会默认一个并不真实的前提，即“团队现在才刚准备开始实现”，这会污染后续所有基于文档的决策。

## 六、优先建议

如果当前只能先做三件事，建议顺序如下。

### 6.1 先做数据地图现状审计，并明确“代码即当前事实”的收口方式

需要把 [`DataMapContainer.jsx`](../../frontend/src/components/datamap/DataMapContainer.jsx)、[`datamap-adapter.js`](../../frontend/src/pages/datamap-adapter.js) 和数据地图后端接口逐项梳理成“已实现能力 / 半实现能力 / 文档目标能力”三栏对照表。目标不是追责，而是先承认当前代码已经构成事实基线。

### 6.2 先补齐特性文档，再写实施计划

下一轮最应该补的不是更高层的总纲，而是直接把高可见层现状能力回写到 [`10-数据地图浏览与覆盖追踪.md`](../architecture/features/iteration-02-runtime-and-governance/10-数据地图浏览与覆盖追踪.md)，至少补齐：

1. 通用补充项。
2. 带上下文进入的高可见反馈规则。
3. 新旧接口边界与前端承载页面边界。
4. 影响分析、节点详情、关系详情和跨工作台跳转的责任划分。

### 6.3 明确数据地图正式主路径，并为高可见层补专项测试门禁

需要尽快明确：

1. `/api/assets/*` 与 `/api/datamap/*` 哪条是当前正式主路径，哪条是兼容过渡；在给出结论前，必须先查清楚旧 `/api/assets/*` 的当前调用方，不然无法判断其是否真的是“可收口的过渡接口”。
2. 前端高可见层至少要补哪些行为测试，才能让后续 TDD 与变更评审有实际抓手。

没有这一步，实施计划会继续建立在模糊契约之上。此外，这个“正式主路径”决策不能停留在口头共识里，需要明确责任人、时间点和文档落点。

## 七、建议带去下一轮外部讨论的问题

为了让下一轮和外部评审工具或外部审稿人讨论更聚焦，建议围绕以下问题展开：

1. 在当前代码已先行的前提下，应该如何定义“现状即事实”的最小治理动作，才能避免流程补票失控。
2. 数据地图的正式主路径是否应该彻底收口到 `/api/datamap/*`，以及旧 `/api/assets/*` 应如何处理。
3. 高可见层最小可验收范围是否应该先收敛为“上下文进入 -> 自动图谱定位 -> 节点/关系详情 -> 影响分析 -> 跳转运行台”这一条闭环，而不是继续并行扩展多视图和全套工作台表达。

## 八、评审结论

数据地图高可见层不是不可落地，相反，它已经具备较强的落地基础。当前真正需要解决的，不是“要不要做”，而是“如何让文档、流程和代码重新对齐”。在这一步做对之前，继续增加门禁、继续扩功能或继续抽象总纲，都会放大当前错位。
