# 数据直通车项目 · 统一术语表

本文统一定义项目术语的中英文映射与使用口径。设计文档、开发文档、用户手册和样例说明中出现的新英文术语、缩写、变量名、常量名、指标名，先补本文，再进入正文。

---

## 核心概念术语

### 系统与模块

| 中文术语     | 英文术语                       | 技术代号    | 说明               |
| -------- | -------------------------- | ------- | ---------------- |
| 数据直通车    | Data Express               | -       | 整体平台名称           |
| 数据地图     | Data Map                   | CALIBER | 业务知识的结构化载体；      |
| 口径治理     | Caliber Governance         | -       | 将口径文档结构化为数据地图的过程 |
| 知识梳理服务   | Knowledge Curation Service | -       | 利用大模型辅助口径治理的AI场景 |
| 代码智能体 | Codex | Codex | 用于本项目方案设计、代码实现、变更评审与文档同步的智能协作代理 |
| 自然语言转SQL | Natural Language to SQL    | NL2SQL  | 将自然语言查询转换为SQL语句  |
| 文本转SQL | Text to SQL | Text2SQL | 历史调研与外部材料中的常见叫法；当前仓库正式口径统一使用 `NL2SQL（自然语言转SQL，Natural Language to SQL）` |
| 命令行接口 | Command Line Interface | CLI | 通过终端子命令、参数和退出码暴露稳定能力的调用边界 |
| 谷歌浏览器 | Google Chrome | Chrome | 当前项目内浏览器桥接能力首轮唯一支持的宿主浏览器 |
| Apple 事件 JavaScript 执行 | JavaScript from Apple Events | - | `Google Chrome（谷歌浏览器）` 中允许 `AppleScript（苹果脚本）` 执行页面 `JavaScript（网页脚本语言）` 的权限开关 |
| ChatGPT 浏览器桥接器 | ChatGPT Browser Bridge | - | 面向仓库内 `skill（技能）` / `agent（智能体）` 调用的项目级脚本能力，用于读取和操作已打开的 `https://chatgpt.com/` 标签页 |

### 业务概念

| 中文术语     | 英文术语                             | 缩写         | 说明                                                                   |
| -------- | -------------------------------- | ---------- | -------------------------------------------------------------------- |
| 主题节点     | Fact / Topic Node                | Topic Node | 导航与聚合节点，当前主要用于展示层或消费层组织，不属于上游核心主图节点                                  |
| 数据主题     | Data Subject                     | -          | 业务侧历史叫法，当前不作为上游核心主图节点，通常映射为展示层导航组织对象                                 |
| 业务场景     | Business Scene                   | Scene      | 围绕一个主业务对象组织的正式语义组织单元，承载边界、默认时间解释、主路径、关键规则、允许返回字段与 `plan_refs（方案引用列表）` |
| 业务场景边界 | Scene Boundary                   | -          | 挂在 `Scene（业务场景）` 上的运行约束，承载适用范围、不适用范围、默认时间和标准输出字段等当前场景边界信息 |
| 候选实体图谱 | Candidate Entity Graph           | -          | 知识生产期按 `material_id（材料标识）` / `task_id（任务标识）` 隔离的未发布候选图，用于承载候选场景、候选方案、业务语义、物理来源和候选关系，不直接进入已发布主图 |
| 导入中活图谱 | Import Live Graph                | -          | 导入预处理期间通过 `SSE（服务端事件流，Server-Sent Events）` 驱动前端实时渲染的候选实体图谱动态视图，随 `graph_patch（图谱增量补丁）` 逐批生长，导入完成后收敛为最终候选图快照 |
| 图谱增量补丁 | Graph Patch                      | -          | 候选实体图谱在导入过程中按批次回传的增量更新单元，携带新增 / 更新节点、关系、焦点对象和批次摘要，不返回整图快照 |
| 候选场景 | Candidate Scene                   | -          | 解析抽取阶段产出的候选业务场景对象，经人工确认后才可转正为 `Scene（业务场景）` |
| 候选方案资产 | Candidate Plan                    | -          | 解析抽取阶段产出的候选方案对象，经人工确认后才可转正为 `Plan（方案资产）` |
| 候选证据片段 | Candidate Evidence Fragment       | -          | 解析抽取阶段产出的候选证据对象，经人工确认后才可转正为 `Evidence Fragment（证据片段）` |
| 标识对象     | Identifier                       | -          | 承载客户号、户口号、协议号、批次号、证件号等可用于输入槽位和关系收敛的语义对象 |
| 原生词元     | Raw Token                        | -          | 用户问题或原始材料中尚未归一的表达，属于运行时输入与归一素材，不是正式图节点类型 |
| 规范词元     | Canonical Term                   | -          | 当前上游核心业务节点，承载原生表达归并、消歧、选主后的标准化入口                                               |
| 统一实体     | Canonical Entity                 | -          | 关系型控制库中的长期稳定共享身份对象，用于把多个场景中的正式资产归并到同一个可治理真源，不随单次发布复制 |
| 统一键       | Canonical Key                    | -          | 用于唯一标识某个 `Canonical Entity（统一实体）` 的正式稳定键；显式业务键优先，物理身份与结构化来源其次 |
| 统一成员归属  | Canonical Membership             | -          | 记录某个 `Scene Asset（场景资产实例）` 属于哪个 `Canonical Entity（统一实体）` 的正式关系，支持置信度、依据和人工覆盖 |
| 统一解析审计记录 | Canonical Resolution Audit       | -          | 记录统一实体解析过程中的自动判断、人工覆盖、拆分决议与拒绝归并原因的审计对象 |
| 待复核       | Needs Review                     | NEEDS_REVIEW | 表示对象已进入统一实体解析流程，但因缺少显式统一键、命中冲突规则或置信度不足而不能自动归并，需要人工确认 |
| 场景层       | Scene Layer                      | -          | 三层架构中的运行时主中心，承载 `Domain（业务领域）` 与 `Scene（业务场景）` |
| 语义层       | Semantic Layer                   | -          | 三层架构中的语义组织层，承载词元、术语、口径、概念、规则、边界锚点与白盒关联 |
| 推理层       | Inference Layer                  | -          | 六层架构中的独立推理组织层，负责把语义规则、元数据事实和证据沉淀为可发布、可消费、可审计的推理资产 |
| 业务领域     | Business Domain                  | Domain     | 场景的组织层，如零售、对公等                                                       |
| 业务术语     | Business Term                    | -          | 当前上游核心主图节点，承载业务词汇、别名与术语解释                                            |
| 业务子图     | Business Subgraph                | -          | 针对当前问题从主图裁切出的局部知识网络，用于上游向下游交付受控上下文                                   |
| 场景发现结果 | Scene Discovery Result           | -          | 当上游未命中正式 `Scene（业务场景）` 时返回的受控发现结果，只包含领域候选、场景候选、命中词元与澄清问题，不等同于业务子图 |
| 业务口径     | Business Caliber                 | -          | 当前上游核心主图节点，承载业务定义、统计范围、计算边界与解释要求                                     |
| 字段概念     | Field Concept                    | -          | 当前上游核心业务节点，承载可单独落成标准输出字段的语义对象                                           |
| 组合概念     | Composite Concept                | -          | 当前上游核心业务节点，定义业务对象静态结构；不直接承载查询动作、过滤条件与时间范围                     |
| 边界概念     | Scope Concept                   | -          | 当前上游核心业务节点，用于定义业务域边界与适用范围，是可被多个场景复用的语义边界锚点                                                 |
| 数据口径     | Data Specification               | -          | 业务侧历史叫法，当前在主图中统一映射为“业务口径”                                            |
| 字典       | Dictionary                       | Dict       | 码值与术语解释的映射表                                                          |
| 码值说明     | Code Mappings                    | -          | 场景内嵌的码值映射说明，来源于 SQL 注释                                               |
| 派生规则     | Derivation Rule                  | -          | 参数转换规则（如证件号→客户号）                                                     |
| 推理规则     | Inference Rule                   | -          | 推理层中的规则实例化绑定对象，表达某条可执行推理规则在当前语义基线、来源约束和证据约束下的正式定义 |
| 推理结论     | Inference Assertion              | -          | 进入已发布快照的正式推理事实对象，承载标识收敛、时态收敛、关系补全和候选场景收敛等结果 |
| 推理链       | Inference Chain                  | -          | 表达推理结论如何由输入事实、规则路径和中间节点逐步推出的结构化解释链路 |
| 推理置信记录   | Inference Confidence Record      | -          | 置信度推理的评分、阈值命中与人工复核结论记录 |
| 推理适用边界   | Inference Scope Profile          | -          | 定义推理规则或推理结论适用于哪些业务域、时间范围、来源范围和禁止场景的治理对象 |
| 置信阈值组   | Confidence Thresholds            | -          | 挂在 `Inference Scope Profile（推理适用边界）` 上的受治理阈值配置集合，用于定义候选、复核、阻断等门槛 |
| 推理快照灰度发布 | Inference Snapshot Gray Release  | -          | 让新的 `snapshot_id（运行态快照标识）` / `inference_snapshot_id（推理快照标识）` 绑定对先在限定范围内生效的发布方式 |
| 降级契约     | Degradation Contract             | -          | 规定运行推理在超时、断言缺失、依赖异常时如何从完整推理退化到确定性推理、模板路径或澄清返回的正式约束 |
| LLM 产出隔离 | LLM Output Isolation             | -          | 规定 `LLM（大语言模型）` 在查询改写、槽位补齐阶段的输出只能作为临时提示，不能直接写入正式推理资产或证据资产的治理要求 |
| 表间关联关系对象 | Join Relation Object             | -          | 当前上游核心主图节点，承载白盒关联关系及其证据、版本与适用边界                                      |
| SQL代码段   | SQL Snippet                      | -          | 当前上游核心主图节点，承载白盒查询路径、示例写法与证据锚点                                        |
| 服务说明     | Service Spec                     | -          | 业务场景的发布态，对外提供输入/输出/限制与错误说明，不属于当前上游核心主图节点                             |
| 方案资产     | Plan Asset                       | Plan       | 围绕同一 `Scene（业务场景）` 定义的受控方案知识资产，表达路由前置条件、时间有效窗口、覆盖声明、来源路径、关联契约与策略引用 |
| 工作流方案 | Workflow Plan | - | 历史执行编排层对象，用于组织多个 `Plan（方案资产）` 的执行顺序与合并规则；当前不纳入上游正式对象模型 |
| 输出契约     | Output Contract                  | -          | 约束知识底座对外承诺的标准输出字段、可选字段、脱敏要求与说明信息 |
| 覆盖声明     | Coverage Declaration             | -          | 用于表达时间覆盖、产品覆盖、机构覆盖、历史缺口、外部依赖与不保证项的正式治理对象 |
| 覆盖分段     | Coverage Segment                 | -          | `Coverage Declaration（覆盖声明）` 内部的结构化覆盖单元，表达特定时间段、来源、完整度与回退动作 |
| 场景类型     | Scene Type                       | -          | `Scene（业务场景）` 的正式分类字段，用于区分事实明细、画像查询、变更轨迹、审计日志等场景类型 |
| 输入槽位模式 | Input Slot Schema                | -          | 约束运行时输入结构的正式契约，定义标识、时间、输出、风险等槽位及其校验、标准化和澄清规则 |
| 时间语义 | Time Semantic                    | -          | 表达申请日期、交易日期、资金日期、份额日期等候选时间解释的语义对象，供 `Time Semantic Selector（时间语义选择器）` 统一确认和发布 |
| 服务端事件流 | Server-Sent Events               | SSE     | 服务端持续向前端单向推送事件的接口模式，本项目用于导入进度、候选草稿和图谱增量补丁回传 |
| 标识谱系     | Identifier Lineage               | -          | 表达客户号、户口号、证件号、协议号、批次号等多标识之间归一、派生和回溯关系的治理对象 |
| 来源接入契约 | Source Intake Contract           | -          | 对原始口径材料、工单样例和 SQL 样例做结构化登记与完整性校验的接入契约 |
| 来源契约     | Source Contract                  | -          | `Plan（方案资产）` 绑定真实来源表、字段、快照周期与可用性条件时使用的结构化来源约束对象 |
| 契约视图     | Contract View                    | -          | 一等受治理资产，用于按角色、用途或审批上下文裁剪 `Output Contract（输出契约）` 并形成可发布、可审计的字段级生效视图 |
| 知识拆解结果 | Knowledge Decomposition Result   | -          | 面向复合请求输出的知识拆解结果，包含子问题、场景候选、方案候选、合并提示与澄清问题 |
| 知识包       | Knowledge Package                | -          | 上游对下游正式交付的受控知识输出，至少包含场景、方案、覆盖、策略、证据、路径与审计追踪信息 |
| 查询改写     | Query Rewrite                    | -          | 运行时在正式检索前对原始问题做标准化、补全和意图重写的步骤 |
| 槽位补齐     | Slot Filling                     | -          | 运行时从问题中抽取并补全标识、时间、输出与风险槽位的步骤 |
| 场景召回     | Scene Recall                     | -          | 运行时按领域、对象、槽位与词元召回候选 `Scene（业务场景）` 的步骤 |
| 方案选择     | Plan Selection                   | -          | 运行时按覆盖、策略、输入契约和来源约束选择 `Plan（方案资产）` 的步骤 |
| 知识反馈     | Knowledge Feedback               | -          | 对知识消费结果的反馈对象，承载命中情况、人工改写、业务接受情况与覆盖争议 |
| 取数方案     | Data Retrieval Plan              | -          | 历史叫法；当前统一收口为 `Plan Asset（方案资产）`，不再只表示 `Scene（业务场景）` 内嵌的 `sql_variants（方案变体列表）` |
| 域级业务概述   | Domain Overview                  | -          | 业务领域层面的背景知识，承载大段业务说明文本                                               |
| 来源表      | Source Table                     | -          | 当前上游核心主图节点，承载物理元数据层中的表对象                                             |
| 字段       | Column                           | -          | 当前上游核心主图节点，承载物理元数据层中的字段对象                                            |
| 证据片段     | Evidence Fragment                | -          | 可被对象或关系引用的原始证据切片，通常带来源、行号和指纹信息                                       |
| 版本快照     | Version Snapshot                 | -          | 当前上游核心主图节点，承载版本比较、替代关系与回放依据                                          |
| 缺口任务     | Gap Task                         | -          | 针对不完整材料、缺少字段或覆盖信息建立的正式治理任务，用于阻断发布并跟踪补齐状态 |
| 复核任务     | Review Task                      | -          | 承接业务、技术、合规复核过程的治理任务对象，用于记录处理人、结论与门禁结果 |
| 审计事件     | Audit Event                      | -          | 记录发布、审批、导出、查询与回退动作的可检索审计对象 |
| 计划中间表示   | Plan Intermediate Representation | PlanIR     | 下游消费侧使用的运行时白盒中间态，输出选中的 `Scene（业务场景）` / `Plan（方案资产）`、参数填充、风险门禁与证据解释；它是上游知识底座的正式输出接口，不是上游内部执行责任，也不是直接 SQL |

---

## 技术术语

### 数据与存储

| 中文术语 | 英文全称 | 缩写 | 说明 |
|---------|---------|------|------|
| 结构化查询语言 | Structured Query Language | SQL | 数据库查询语言 |
| 公用表表达式 | Common Table Expression | CTE | SQL中的WITH子句 |
| JavaScript对象表示法 | JavaScript Object Notation | JSON | 轻量级数据交换格式 |
| 带注释的JSON展示格式 | JSON with Comments | JSONC | 用于方案讨论的 JSON 展示格式，允许写注释 |
| 可扩展标记语言 | Extensible Markup Language | XML | 用于表达结构化图形、配置和交换格式的标记语言 |
| 电子表格 | Excel Spreadsheet | Excel | 微软办公软件 |
| 演示文稿 | Presentation | PPT | 用于汇报、评审和培训的逐页展示文稿 |
| 领域专用语言 | Domain-Specific Language | DSL | 针对特定业务或执行约束设计的结构化表达语言；在本项目历史材料中主要用于表达声明式取数方案，但当前未作为上游正式对象 |
| 数据库 | Database | DB | 数据存储系统 |
| 高斯数据库 | GaussDB | - | 华为云数据库产品 |
| draw.io 图示编辑工具 | draw.io Diagramming Tool | drawio | 用于编辑、导入和维护多页架构图的图示工具 |

### AI与算法

| 中文术语 | 英文全称 | 缩写 | 说明 |
|---------|---------|------|------|
| 人工智能 | Artificial Intelligence | AI | 机器智能技术 |
| 大语言模型 | Large Language Model | LLM | 基于Transformer的语言模型 |
| 检索增强生成 | Retrieval-Augmented Generation | RAG | 结合检索和生成的AI技术 |
| 图检索增强生成 | Graph Retrieval-Augmented Generation | GraphRAG | 让图结构参与检索、上下文组织、约束与生成的增强生成范式 |
| 预处理实验适配器 | Preprocess Experiment Adapter | - | 知识生产期在材料标准化之后调用的统一实验接口，只生成候选实体、候选关系、候选证据和引用元数据，不直接写正式治理资产 |
| 运行检索实验适配器 | Retrieval Experiment Adapter | - | 运行时在查询改写 / 槽位补齐之后、正式场景召回之前调用的统一实验接口，只补候选场景、统一实体、证据引用与得分，不直接输出正式决策 |
| 实验检索索引 | Experimental Retrieval Index | - | 按 `snapshot_id（运行态快照标识）` 锁定的只读实验索引，只接收 `PUBLISHED（已发布）` 快照摘要，用于回放评测、影子模式和检索增强侧车 |
| 侧车服务 | Sidecar Service | - | 以独立进程或独立服务形态运行、由主系统调用的辅助能力，不承担主系统正式真源或最终决策职责 |
| 影子模式 | Shadow Mode | - | 与正式请求并行运行实验链路、记录结果但不影响正式返回的验证方式 |
| 前 k 命中率 | Hit at K | hit@k | 统计目标结果是否出现在前 `k` 个候选中的指标 |
| 前 k 精确率 | Precision at K | precision@k | 统计前 `k` 个候选中有多少比例为正确结果的指标 |
| 基于词频与逆文档频率的文本相关性算法 | Best Matching 25 | BM25 | 常用于关键词检索与文本相关性排序的经典打分算法 |
| 语义召回 | Semantic Retrieval | - | 面向场景、术语、规则、口径等语义对象的检索方式，通常组合向量检索、关键词和术语归一 |
| 确定性定位 | Deterministic Lookup | - | 面向数据库、schema、表、字段等物理对象的精确定位方式，以结构化标识和精确匹配为主 |
| 结构化索引 | Structured Index | - | 按数据库、schema、表名、字段名、唯一标识符等结构化键建立的检索索引 |
| 精确匹配 | Exact Match | - | 完全按规范名称或结构化键匹配对象的检索方式 |
| 模糊匹配 | Fuzzy Match | - | 在名称存在轻微差异、别名或噪声时使用的近似匹配方式 |
| 开放接口描述规范 | OpenAPI Specification | OpenAPI | 用于描述 REST 接口的标准规范 |
| 接口文档生成框架 | SpringDoc OpenAPI | SpringDoc | Spring Boot（应用框架）中生成 OpenAPI 文档的常用实现 |
| 接口文档生态 | Swagger | Swagger | OpenAPI 相关的接口描述、调试与展示生态 |
| 光学字符识别 | Optical Character Recognition | OCR | 图像文字识别技术 |
| 向量数据库 | Vector Database | - | 存储和检索向量的专用数据库 |

### 知识图谱与图计算

| 中文术语 | 英文全称 | 缩写 | 说明 |
|---------|---------|------|------|
| 知识图谱 | Knowledge Graph | KG | 以实体、关系与语义组织知识的结构化网络 |
| 实体 | Entity | - | 在知识图谱中可被独立识别、独立引用并可进入治理边界的对象 |
| 关系 | Relation | - | 连接两个实体并表达明确业务语义的事实联系或结构连接 |
| 属性 | Attribute | - | 附着在实体或关系上的描述性字段，用于表达状态、边界、来源和约束 |
| 三元组 | Triple | - | 由主语、谓语、宾语构成的最小知识表达单元 |
| 资源描述框架 | Resource Description Framework | RDF | 以三元组统一表达图谱事实的标准模型 |
| 图查询语言 | SPARQL Protocol and RDF Query Language | SPARQL | 面向 RDF 图的查询语言 |
| 本体语言 | Web Ontology Language | OWL | 用于描述概念层级、约束与推理规则的语义网语言 |
| 模式 | Schema | - | 规定实体、关系、属性与约束的骨架 |
| 本体 | Ontology | - | 对边界概念、关系与约束的形式化定义 |
| 图推理 | Graph Reasoning | - | 基于已知实体、关系、约束和规则推出可解释结论的过程 |
| 属性图 | Property Graph | - | 节点和边都可携带属性的图模型 |
| 图数据库 | Graph Database | - | 面向图结构存储、检索与遍历的数据库 |
| 标签属性图 | Labeled Property Graph | LPG | 以带标签的节点、边及其属性组织数据的属性图模型 |
| Neo4j 图数据库产品 | Neo4j | - | 常见的属性图数据库产品与图应用开发生态 |
| NebulaGraph 图数据库产品 | NebulaGraph | - | 面向分布式场景的属性图数据库产品 |
| 亚马逊云图数据库产品 | Amazon Neptune | - | 亚马逊云提供的托管图数据库产品，同时支持属性图与 RDF 图访问方式 |
| TigerGraph 图数据库产品 | TigerGraph | - | 面向企业级图计算与图应用的分布式图数据库产品 |
| JanusGraph 图数据库产品 | JanusGraph | - | 开源可扩展属性图数据库产品，常与外部存储和索引后端配合部署 |
| 图查询语言 | Cypher Query Language | Cypher | 常见于属性图数据库的声明式查询语言 |
| 开放 Cypher 兼容查询语言 | openCypher | openCypher | 面向属性图的开放兼容查询语言口径 |
| Nebula 图查询语言 | Nebula Graph Query Language | nGQL | NebulaGraph 使用的查询语言，部分兼容 openCypher |
| TigerGraph 图查询语言 | Graph SQL | GSQL | TigerGraph 使用的图查询语言 |
| 图遍历查询语言 | Gremlin Graph Traversal Language | Gremlin | 面向属性图遍历与图计算的查询语言 |
| 亚马逊云图分析服务 | Neptune Analytics | - | Amazon Neptune 配套的图分析与向量检索服务 |
| 向量索引 | Vector Index | - | 用于按向量相似度建立索引并支持近邻检索的能力 |
| 统一资源标识符 | Uniform Resource Identifier | URI | 用于唯一标识图中节点或关系的标准字符串 |
| 图元素 | Graph Element | - | 顶点与边的统一抽象，用于描述图中可治理图元的共同结构 |
| 初级三元组 | Preliminary Triple | - | 文本抽取后尚未完成推理、验证与合并的候选事实 |
| 子图 | Subgraph | - | 从完整图中按问题或任务裁剪出的局部关系网络 |
| 实体统一 | Entity Resolution | - | 识别跨来源记录是否指向同一现实对象的过程 |
| 顶点 | Vertex | - | 图中的节点型对象；在本项目中仅指合法进入主图的独立治理对象 |
| 边 | Edge | - | 图中连接两个顶点的关系；在本项目中承载包含、使用、依赖、读取等语义 |
| 混合检索 | Hybrid Retrieval | - | 将图检索、文本检索、向量检索、规则召回等组合使用的检索方式 |
| 局部搜索 | Local Search | - | 面向具体实体、关系或局部路径的问题检索方式 |
| 全局搜索 | Global Search | - | 面向全域主题、整体结构或社区摘要的问题检索方式 |
| DRIFT 搜索 | Dynamic Reasoning and Inference with Flexible Traversal | DRIFT Search | 结合局部与全局信息、逐步扩展问题上下文的图检索方式 |
| 语义信息注入 | Semantic Injection | - | 将术语、码值、时间语义和边界说明注入抽取结果，连接文本知识与元数据落点 |
| 元数据概念映射 | Metadata-to-Concept Mapping | - | 将数据库、schema、表、字段等元数据对象映射到表概念、字段概念或组合概念 |
| SQL引擎验证器 | SQL Engine Validator | - | 对候选路径或生成 SQL 的可执行性、口径一致性和字段落点进行校验 |

### 核心模型字段与治理变量

| 英文变量名                        | 中文解释         | 说明                                                                                                                                                                                     |
| ---------------------------- | ------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ele_uuid`                   | 物理唯一标识符      | 图元素不可变物理标识，采用 ``{ele_uid}::{YYYYMMDDHHMMSSmmm}::{src_fingerprint_8}`` 结构                                                                                                               |
| `ele_attr_msk`               | 属性掩码         | 64 位掩码字段，用位标记节点是否携带对扩展属性读取有效的属性                                                                                                                                                        |
| `conf_score`                 | 置信度分值        | 上游知识图谱对象级门禁指标，用于快速评分与发布门禁                                                                                                                                                              |
| `review_status`              | 人工确认状态       | 对象当前人工确认结论，固定取值为 `UNREVIEWED（未确认）`、`VERIFIED（已确认）`、`REJECTED（已驳回）`                                                                                                                     |
| `snapshot_id`                | 运行态快照标识      | 已发布运行态图投影的绑定标识，查询、评分与输出在同一 `snapshot_id（运行态快照标识）` 下执行                                                                                                                                  |
| `inference_snapshot_id`      | 推理快照标识       | 与 `snapshot_id（运行态快照标识）` 强绑定的推理资产快照标识，用于保证控制资产、推理资产与运行读取版本一致                                                                                                                          |
| `rule_code`                  | 推理规则编码       | `Inference Rule（推理规则）` 的稳定逻辑标识，用于唯一标识一条可执行推理规则实例化绑定                                                                                                                                    |
| `derivation_rule_ref`        | 派生规则引用       | `Inference Rule（推理规则）` 回链 `Derivation Rule（派生规则）` 的引用字段，用于防止规则语义和推理实现双写漂移                                                                                                                     |
| `assertion_id`               | 推理结论标识       | `Inference Assertion（推理结论）` 的稳定逻辑标识，用于唯一标识一条正式推理事实                                                                                                                                    |
| `chain_id`                   | 推理链标识        | `Inference Chain（推理链）` 的稳定逻辑标识，用于追踪单条推理结论的解释链路                                                                                                                                        |
| `record_id`                  | 置信记录标识       | `Inference Confidence Record（推理置信记录）` 的稳定逻辑标识，用于追踪置信度评分与复核结论                                                                                                                              |
| `scope_code`                 | 推理适用边界编码     | `Inference Scope Profile（推理适用边界）` 的稳定逻辑标识，用于表达一组正式适用范围约束                                                                                                                                |
| `confidence_thresholds`      | 置信阈值组        | `Inference Scope Profile（推理适用边界）` 中的受治理阈值配置集合，用于区分候选、复核、阻断等门槛                                                                                                                         |
| `gray_release_scope`         | 灰度发布范围       | 定义新快照对先行生效的领域、场景、角色、机构、样板请求集或流量分片范围                                                                                                                                            |
| `degrade_mode`               | 降级模式         | 定义运行推理在依赖异常时允许退化到哪些模式，如 `deterministic_only（仅确定性推理）`、`template_only（仅模板路径）`、`clarification_only（仅澄清返回）`                                                                           |
| `assertion_refs`             | 推理结论引用列表     | `Knowledge Package（知识包）` 中命中的推理结论引用集合，用于回到具体 `Inference Assertion（推理结论）`                                                                                                                   |
| `assertion_summary`          | 推理结论摘要       | `Knowledge Package（知识包）` 中对命中推理结论的摘要性说明字段                                                                                                                                                  |
| `confidence_hits`            | 置信命中结果       | 运行时返回中用于表达哪些推理结论命中了置信门禁、阈值或复核条件的结构化字段                                                                                                                                         |
| `scope_profile_refs`         | 适用边界引用列表     | `Knowledge Package（知识包）` 中绑定的推理适用边界引用集合，用于解释推理结果在何种范围内成立                                                                                                                             |
| `runtime_mode`               | 运行模式         | `Knowledge Package（知识包）` 返回中标记本次请求实际采用的推理模式，如 `full_inference（完整推理）`、`deterministic_only（仅确定性推理）`、`template_only（仅模板路径）`、`clarification_only（仅澄清返回）`                          |
| `degrade_reason_codes`       | 降级原因编码列表     | `Knowledge Package（知识包）` 返回中用于说明为何触发推理降级的结构化原因编码集合                                                                                                                                   |
| `llm_output_class`           | LLM 产出分类      | 标记 `LLM（大语言模型）` 临时输出属于 `rewrite_hint（改写提示）`、`slot_hint（槽位提示）` 或 `clarification_hint（澄清提示）` 等类别的字段                                                                                              |
| `line_from_uid`              | 边起点逻辑标识      | 边元素指向起点图元素的 `ele_uid（逻辑唯一标识符）`                                                                                                                                                         |
| `line_to_uid`                | 边终点逻辑标识      | 边元素指向终点图元素的 `ele_uid（逻辑唯一标识符）`                                                                                                                                                         |
| `attr_title`                 | 扩展属性标题       | 扩展属性的标题名，用于同一对象下的属性分组与去重                                                                                                                                                               |
| `attr_uuid`                  | 扩展属性物理唯一标识符  | 扩展属性不可变物理标识，用于审计与版本追踪                                                                                                                                                                  |
| `attr_value`                 | 扩展属性内容       | 承载扩展属性正文内容的文本字段                                                                                                                                                                        |
| `attr_src`                   | 扩展属性来源       | 记录扩展属性来自哪段材料、SQL 或人工输入                                                                                                                                                                 |
| `attr_puuid`                 | 父扩展属性物理唯一标识符 | 用于表达扩展属性继承链上的父属性                                                                                                                                                                       |
| `attr_conf_score`            | 扩展属性置信度分值    | 扩展属性级别的置信度，用于细粒度评分与门禁                                                                                                                                                                  |
| `standard_output_fields`     | 标准输出字段清单     | 场景对外承诺的字段概念列表与必填约束                                                                                                                                                                     |
| `field_concept_uid`          | 字段概念标识       | `standard_output_fields` 中引用 `字段概念（Field Concept）` 的逻辑标识                                                                                                                               |
| `display_order`              | 展示顺序         | `standard_output_fields` 中控制字段展示次序的正整数                                                                                                                                                 |
| `src_fingerprint_8`          | 来源摘要 8 位值    | 对 `ele_src（创建来源）` 做稳定摘要后得到的 8 位标识，用于压缩 `ele_uuid`                                                                                                                                      |
| `asset_reference`            | 引用关系（集合术语）   | `Scene（业务场景）` 与共享资产之间所有 `USES_*（使用类关系）` 类图边的集合性称呼；不是独立存储结构，图边本体是 `USES_CALIBER（使用业务口径）`、`USES_RULE（使用派生规则）`、`USES_JOIN（使用关联关系）`、`USES_SNIPPET（使用SQL代码段）`、`USES_DICT（使用字典）` 等关系（见第 4 章） |
| `ref_policy`                 | 引用策略         | 控制已发布场景绑定共享资产版本的策略，默认 `LOCKED`                                                                                                                                                         |
| `merge_score`                | 合并评分         | 合并管道的复合评分，默认由向量相似度、`BM25（基于词频与逆文档频率的文本相关性算法，Best Matching 25）` 相似度与结构重合度共同计算                                                                                                           |
| `governance_tier`            | 治理等级         | 资产治理分层字段，固定取值为 `CORE（核心）`、`SHARED（共享）`、`EXPLORATORY（探索）`                                                                                                                               |
| `visibility_roles`           | 可见角色列表       | 控制 `Domain（业务领域）` 或 `Scene（业务场景）` 对哪些角色可见的角色集合                                                                                                                                         |
| `is_sandbox`                 | 沙箱标记         | 标记当前结果或 `PlanIR（计划中间表示，Plan Intermediate Representation）` 仅用于验证与限量试运行，不可直接用于生产交付                                                                                                       |
| `allowed_edge_types`         | 允许边类型集合      | 运行时 `BFS（广度优先搜索，Breadth-First Search）` 允许扩展的边类型白名单                                                                                                                                     |
| `max_expand_per_node`        | 单节点最大扩展数     | 运行时为抑制超级节点扩散而设置的单节点扩展上限                                                                                                                                                                |
| `domain_candidates`          | 领域候选列表       | `Scene Discovery Result（场景发现结果）` 中返回的候选业务领域列表                                                                                                                                          |
| `scene_candidates`           | 场景候选列表       | `Scene Discovery Result（场景发现结果）` 中返回的候选业务场景列表                                                                                                                                          |
| `plan_refs`                  | 方案引用列表       | `Scene（业务场景）` 绑定的 `Plan（方案资产）` 标识集合，用于表达场景下可选方案资产                                                                                                                                      |
| `plan_id`                    | 方案标识         | `Plan（方案资产）` 的唯一业务标识                                                                                                                                                                   |
| `scene_ref`                  | 场景引用         | `Plan（方案资产）` 或 `PlanIR（计划中间表示）` 指向 `Scene（业务场景）` 的引用字段                                                                                                                                 |
| `view_code`                  | 契约视图标识       | `Contract View（契约视图）` 的稳定逻辑主键                                                                                                                                                          |
| `role_scope`                 | 角色范围         | 用于表达当前契约视图、方案或策略适用的角色边界                                                                                                                                                                |
| `route_preconditions`        | 路由前置条件       | `Plan（方案资产）` 的前置命中条件，用于表达产品线区分、时间语义区分、入口键限制等硬条件                                                                                                                                        |
| `source_contract_refs`       | 来源契约引用集合     | `Plan（方案资产）` 绑定的 `Source Contract（来源契约）` 引用列表                                                                                                                                          |
| `time_semantic_selector_ref` | 时间语义选择器引用    | `Plan（方案资产）` 或 `Scene（业务场景）` 绑定的 `Time Semantic Selector（时间语义选择器）` 标识                                                                                                                  |
| `valid_from`                 | 生效起始时间       | `Plan（方案资产）` 的时间有效窗口起点                                                                                                                                                                 |
| `valid_to`                   | 生效结束时间       | `Plan（方案资产）` 的时间有效窗口终点                                                                                                                                                                 |
| `coverage_statement`         | 覆盖声明         | `Plan（方案资产）` 或 `Coverage Declaration（覆盖声明）` 中用于描述可覆盖范围、缺口与不保证项的核心字段                                                                                                                    |
| `source_tables`              | 来源表集合        | `Plan（方案资产）` 使用的候选物理表集合                                                                                                                                                                |
| `join_contract`              | 关联契约         | `Plan（方案资产）` 中对 join（关联）键、关联方向与约束条件的结构化定义                                                                                                                                              |
| `default_time_field`         | 默认时间字段       | `Plan（方案资产）` 在默认时间语义下优先使用的物理时间字段                                                                                                                                                       |
| `dedupe_strategy`            | 去重策略         | `Plan（方案资产）` 对历史补齐、多源重叠和重复记录的处理规则                                                                                                                                                      |
| `manual_required`            | 需人工处理标记      | 标识当前 `Plan（方案资产）` 是否必须人工确认或人工继续处理                                                                                                                                                      |
| `policy_refs`                | 策略引用         | `Plan（方案资产）` 绑定的 `Policy（策略对象）` 引用集合                                                                                                                                                   |
| `evidence_refs`              | 证据引用         | 结构化对象绑定的证据片段引用集合                                                                                                                                                                       |
| `scene_type`                 | 场景类型         | `Scene（业务场景）` 的正式分类字段，固定使用受治理的 `Scene Type（场景类型）` 枚举                                                                                                                                   |
| `input_slots`                | 输入槽位集合       | `Knowledge Package（知识包）` 或运行时判定结果中返回的结构化槽位解析结果                                                                                                                                         |
| `default_time_range`         | 默认时间范围       | 输入槽位中的默认时间区间表达                                                                                                                                                                         |
| `explicit_time_semantic`     | 显式时间语义       | 输入槽位中由用户明确指定的时间解释，如申请日期或交易日期                                                                                                                                                           |
| `as_of_time`                 | 截止时点         | 输入槽位中用于表达“按某一时点回看最近状态”的时间参数                                                                                                                                                            |
| `coverage_request`           | 覆盖请求         | 输入槽位中对历史覆盖、全量覆盖或特定覆盖段的要求                                                                                                                                                               |
| `need_export`                | 导出诉求标记       | 输入槽位中标识本次请求是否包含导出需求                                                                                                                                                                    |
| `approval_context`           | 审批上下文        | 输入槽位中用于承接审批用途、审批单据或审批链路信息的结构化字段                                                                                                                                                        |
| `approval_template`          | 审批模板         | 用于表达高敏字段、导出任务或契约视图绑定的审批模板标识                                                                                                                                                            |
| `coverage_segments`          | 覆盖分段集合       | `Coverage Declaration（覆盖声明）` 中按时间、来源和完整度拆分后的覆盖段列表                                                                                                                                      |
| `source_priority`            | 来源优先级        | 用于表达 `Coverage Segment（覆盖分段）` 或 `Plan（方案资产）` 内多来源的优先顺序                                                                                                                                 |
| `time_semantic_selector`     | 时间语义选择器      | 用于表达默认时间、备选时间、触发澄清词与解释优先级的结构化配置                                                                                                                                                        |
| `required_outputs`           | 必返输出字段       | `Output Contract（输出契约）` 中必须由当前方案正式承诺返回的字段集合                                                                                                                                            |
| `optional_outputs`           | 可选输出字段       | `Output Contract（输出契约）` 中可在满足条件时补充返回的字段集合                                                                                                                                              |
| `masked_outputs`             | 脱敏输出字段       | `Output Contract（输出契约）` 中需要按策略脱敏返回的字段集合                                                                                                                                                |
| `restricted_outputs`         | 受限输出字段       | `Output Contract（输出契约）` 中仅在满足额外权限或审批上下文时可返回的字段集合                                                                                                                                       |
| `forbidden_outputs`          | 禁止输出字段       | `Output Contract（输出契约）` 中不允许通过通用链路直接返回的字段集合                                                                                                                                            |
| `effective_contract_view`    | 生效契约视图       | 当前请求在角色、用途和审批条件下实际生效的 `Contract View（契约视图）` 标识或内容块                                                                                                                                     |
| `completeness_level`         | 完整度等级        | `Coverage Declaration（覆盖声明）` 或运行时返回中表达 `FULL / PARTIAL / GAP` 的结构化字段                                                                                                                   |
| `matched_segment`            | 命中覆盖分段       | 运行时返回中当前请求命中的 `Coverage Segment（覆盖分段）` 标识或摘要                                                                                                                                           |
| `primary_path`               | 主路径          | `Knowledge Package（知识包）` 中当前最优命中路径的结构化表达                                                                                                                                               |
| `path_type`                  | 路径类型         | 运行时路径结果中表达“模板路径 / 图查询路径 / 混合路径”的字段                                                                                                                                                     |
| `source_refs`                | 来源引用         | 路径或知识包中引用真实来源表、字段或来源契约的结构化集合                                                                                                                                                           |
| `fallback`                   | 回退动作         | `Coverage Declaration（覆盖声明）` 或运行时输出中对覆盖缺口后的默认处理动作说明                                                                                                                                    |
| `fallback_action`            | 回退动作标识       | `Coverage Declaration（覆盖声明）`、`Coverage Engine（覆盖引擎）` 或运行时返回中明确的回退决策字段                                                                                                                  |
| `route_reason`               | 路由原因         | `Plan（方案资产）` 被选中时用于解释命中原因的结构化字段                                                                                                                                                        |
| `policy_hits`                | 命中策略列表       | 运行时返回中记录命中的 `Policy（策略对象）` 规则集合                                                                                                                                                        |
| `approval_required`          | 是否需要审批       | 运行时策略输出中用于表达当前请求是否必须进入审批链路的结构化字段                                                                                                                                                       |
| `masking_plan`               | 脱敏方案         | 运行时或导出返回中用于表达字段级脱敏处理方案的结构化字段                                                                                                                                                           |
| `coverage_explanation`       | 覆盖解释         | 运行时返回中对当前覆盖段、缺口或部分覆盖原因的说明字段                                                                                                                                                            |
| `risk_level`                 | 风险等级         | 运行时、影响分析或治理任务中用于表达风险高低的结构化字段                                                                                                                                                           |
| `reason_codes`               | 原因编码列表       | 运行时返回中用于表达决策、阻断或降级原因的编码集合                                                                                                                                                              |
| `evidence_summary`           | 证据摘要         | `Knowledge Package（知识包）` 中对命中证据的摘要性说明字段                                                                                                                                                |
| `generated_at`               | 生成时间         | `Knowledge Package（知识包）` 或审计输出生成的时间戳字段                                                                                                                                                 |
| `asset_ref`                  | 资产引用         | 接口、审计或影响分析中引用单个治理资产的统一字段                                                                                                                                                               |
| `asset_refs`                 | 资产引用集合       | 事件载荷或返回结果中引用多个治理资产的结构化集合                                                                                                                                                               |
| `actor`                      | 执行主体         | 审计事件中记录执行动作的主体字段，可对应人或系统                                                                                                                                                               |
| `idempotency_key`            | 幂等键          | 发布、审批、导出等异步操作用于防重的请求标识                                                                                                                                                                 |
| `event_id`                   | 事件标识         | 异步事件的唯一标识字段                                                                                                                                                                            |
| `event_name`                 | 事件名称         | 异步事件的标准名称字段                                                                                                                                                                            |
| `occurred_at`                | 发生时间         | 异步事件实际发生时间的时间戳字段                                                                                                                                                                       |
| `job_id`                     | 任务标识         | 发布、审批、导出等异步任务的统一任务编号                                                                                                                                                                   |
| `job_status`                 | 任务状态         | 异步任务查询接口返回的当前状态字段                                                                                                                                                                      |
| `current_step`               | 当前步骤         | 异步任务查询接口返回的当前执行步骤字段                                                                                                                                                                    |
| `archive_ref`                | 归档引用         | 导出文件或审计材料在对象存储中的归档引用字段                                                                                                                                                                 |
| `download_token`             | 下载令牌         | 导出下载接口返回的临时访问令牌                                                                                                                                                                        |
| `expire_at`                  | 失效时间         | 下载令牌或临时授权到期时间字段                                                                                                                                                                        |
| `source_system`              | 来源系统         | 事件载荷中用于表达事件或材料来源系统的字段                                                                                                                                                                  |
| `file_fingerprint`           | 文件指纹         | 导出文件或归档材料的稳定摘要字段                                                                                                                                                                       |
| `sub_questions`              | 子问题列表        | `Knowledge Decomposition Result（知识拆解结果）` 中拆出的子问题集合                                                                                                                                     |
| `plan_candidates`            | 方案候选列表       | `Knowledge Decomposition Result（知识拆解结果）` 中返回的候选方案资产集合                                                                                                                                  |
| `merge_hints`                | 合并提示         | `Knowledge Decomposition Result（知识拆解结果）` 中返回的结果合并建议                                                                                                                                    |
| `matched_terms`              | 命中词元列表       | `Scene Discovery Result（场景发现结果）` 中返回的已命中词元或术语列表                                                                                                                                        |
| `clarification_question`     | 澄清问题         | `Scene Discovery Result（场景发现结果）` 中返回的建议澄清问句                                                                                                                                            |
| `clarification_questions`    | 澄清问题列表       | `Knowledge Decomposition Result（知识拆解结果）` 中返回的多条澄清问题集合                                                                                                                                  |

### 指标与门禁变量

| 英文变量名 | 中文解释 | 说明 |
|---------|---------|------|
| `schema_link_score` | 模式链接得分 | 衡量 `Schema Linking（模式链接）` 的正确性与稳定性 |
| `boundary_violation_recall` | 边界违规召回率 | 衡量边界违规样本被识别出来的比例 |
| `boundary_false_positive_rate` | 边界误拦截率 | 衡量正常请求被误判为高风险的比例 |
| `reliability_score` | 可靠性得分 | 综合衡量链路稳定性与可控性的评分 |
| `rules_hit` | 命中规则列表 | 记录本次判定命中的规则集合 |
| `trace_id` | 追踪编号 | 用于串联判定过程、审计日志与回放记录 |
| `operator_id` | 操作人标识 | 表达查询人、审批人、发布执行人的统一身份标识字段 |
| `operator_role` | 操作角色 | 对外接口或审批链路中用于表达当前操作人角色的字段 |
| `api_version` | 接口版本 | 对外接口请求或响应中携带的版本标识字段 |
| `decision` | 决策结果 | 对运行时判定、审批或策略执行结果的结构化字段 |
| `reason_code` | 原因编码 | 对运行时决策、发布阻断或错误进行结构化解释的编码字段 |
| `domain_code` | 领域编码 | `Domain（业务领域）` 的稳定业务编码字段 |
| `scene_code` | 场景编码 | `Scene（业务场景）` 的稳定业务编码字段 |
| `scene_name` | 场景名称 | `Scene（业务场景）` 的展示名称字段 |
| `plan_code` | 方案编码 | `Plan（方案资产）` 的稳定业务编码字段 |
| `plan_version` | 方案版本 | 运行时返回中当前命中 `Plan（方案资产）` 的版本字段 |
| `selected_scene` | 已选场景 | 运行时请求中显式指定的 `Scene（业务场景）` 标识 |
| `selected_plan` | 已选方案 | 运行时请求中显式指定的 `Plan（方案资产）` 标识 |
| `requested_fields` | 请求字段列表 | 查询、审批或导出链路中申请返回的字段集合 |
| `required_fields` | 必需字段列表 | 运行时请求、契约校验或审批链路中声明必须返回的字段集合 |
| `optional_fields` | 可选字段列表 | 运行时请求、契约校验或审批链路中声明可补充返回的字段集合 |
| `purpose` | 使用目的 | 查询、审批或导出时声明本次使用场景的字段 |
| `sensitivity_hits` | 敏感命中列表 | 运行时策略判定中命中的敏感字段或敏感规则集合 |
| `sensitivity_scope` | 敏感范围 | 方案、契约视图或策略对象适用的敏感字段边界 |
| `high_sensitivity_request` | 高敏请求标记 | 用于标识当前请求是否命中高敏字段或高敏用途的结构化标记 |
| `check_items` | 检查项列表 | 发布检查接口返回的通过项或待检查项集合 |
| `blocked_items` | 阻断项列表 | 发布或任务状态接口返回的阻断原因集合 |
| `accepted` | 已受理标记 | 异步接口返回中表示任务是否被系统接受的字段 |
| `affected_assets` | 受影响资产集合 | 影响分析接口返回的资产影响范围集合 |
| `recommended_action` | 建议动作 | 影响分析或运维返回的建议处置动作字段 |
| `clarification_hint` | 澄清提示 | 场景搜索接口返回的澄清建议字段 |
| `hard_filter_result` | 硬过滤结果 | 方案选择接口返回的覆盖、策略和来源硬过滤结果 |
| `executed_status` | 执行状态 | 审批决策或异步任务执行后的结果状态字段 |
| `changed_source` | 变更来源 | 影响分析接口中触发分析的来源变更对象字段 |
| `effective_time_range` | 生效时间范围 | 审批单或查询链路中声明本次操作适用时间范围的字段 |
| `approved_fields` | 已批准字段列表 | 审批通过后允许导出的字段集合 |
| `approver` | 审批人 | 审批状态或审批决策返回中的审批人字段 |
| `comment` | 审批备注 | 审批决策链路中记录说明或备注的字段 |
| `audit_requirements` | 审计要求 | 策略输出中对留痕、审批和导出审计的要求字段 |
| `coverage_range` | 覆盖范围 | 覆盖引擎或方案索引中表达覆盖边界的字段 |
| `required_role` | 必需角色 | 契约视图或受限输出字段要求的角色约束 |
| `applicable_scope` | 适用范围 | `Scene（业务场景）` 或场景边界中声明适用范围的字段 |
| `inapplicable_scope` | 不适用范围 | `Scene（业务场景）` 或场景边界中声明不适用范围的字段 |
| `exclusions` | 不保证项列表 | 用于表达当前场景或覆盖声明明确不承诺、不兜底返回的内容集合 |
| `primary_object` | 主对象 | `Scene（业务场景）` 当前围绕的主业务对象字段 |
| `default_time_semantic` | 默认时间语义 | `Scene（业务场景）` 的默认时间解释字段 |
| `version_policy` | 版本策略 | 治理对象发布、冻结和替代时遵循的版本规则字段 |
| `owner` | 归属责任人 | 治理对象或任务的责任人字段 |
| `version_tag` | 版本标签 | 用于表达 `Version Snapshot（版本快照）` 语义版本或发布标签的字段 |
| `source_ingest_id` | 来源接入任务标识 | 原始材料导入链路的幂等与追踪标识 |
| `publish_job_id` | 发布任务标识 | 发布检查、快照切换与发布执行链路的幂等标识 |
| `approval_job_id` | 审批任务标识 | 高敏审批任务的幂等与追踪标识 |
| `export_job_id` | 导出任务标识 | 导出执行链路的幂等与追踪标识 |
| `gap_code` | 缺口任务编码 | `Gap Task（缺口任务）` 的稳定业务编码字段 |
| `review_code` | 复核任务编码 | `Review Task（复核任务）` 的稳定业务编码字段 |
| `mask_type` | 脱敏类型 | 字段级控制中用于表达 `FULL_MASK`、`PARTIAL_MASK`、`HASH_ONLY` 等脱敏方式的字段 |
| `join_relations` | 表间关联关系 | 用于结构化表达场景内多表 join（关联）关系 |
| `alignment_refs` | 对齐引用 | 用于记录 `计划中间表示（Plan Intermediate Representation）` 或治理对象引用的对齐结果 |
| `scene_hit_rate` | 场景命中率 | 衡量运行时请求命中正式 `Scene（业务场景）` 的比例 |
| `slot_clarification_rate` | 槽位澄清率 | 衡量运行时因为槽位缺失或歧义而触发澄清的比例 |
| `plan_select_hit_rate` | 方案选择命中率 | 衡量运行时首选 `Plan（方案资产）` 与人工或基准答案一致的比例 |
| `coverage_hit_distribution` | 覆盖命中分布 | 统计 `Coverage Declaration（覆盖声明）` 命中 `FULL / PARTIAL / GAP` 的分布情况 |
| `policy_hit_distribution` | 策略命中分布 | 统计 `Policy（策略对象）` 命中 `allow / need_approval / deny` 的分布情况 |
| `publish_pass_rate` | 发布通过率 | 衡量发布门禁校验一次通过的比例 |
| `metadata_freshness` | 元数据新鲜度 | 衡量表、字段、快照等元数据与实际环境保持一致的及时性 |
| `evidence_coverage` | 证据覆盖率 | 衡量已发布场景或方案资产拥有充足 `Evidence Fragment（证据片段）` 支撑的比例 |
| `approval_queue_backlog` | 审批队列积压量 | 衡量高敏审批任务积压程度的运行指标 |
| `api_error_rate` | 接口错误率 | 衡量运行服务或治理接口返回错误的比例指标 |
| `graph_timeout` | 图查询超时指标 | 衡量图查询链路超时次数或超时率的运行指标 |
| `supersede` | 替代关系 | 用于表达新资产版本替代旧资产版本的关系名称或关系类型 |

### 规则常量与判定编码

| 英文常量名 | 中文解释 | 说明 |
|---------|---------|------|
| `ROUTE_GATE` | 路由前置条件 | 用于表达某个 `Plan（方案资产）` 生效前必须满足的入口或产品路由条件 |
| `TIME_SELECTOR` | 时间语义选择器 | 用于表达默认时间语义冲突或需显式选定时间字段的规则类型 |
| `COVERAGE_RULE` | 覆盖规则 | 用于表达时间覆盖、机构覆盖、产品覆盖和历史缺口约束 |
| `DENY_RULE` | 阻断规则 | 用于表达命中后必须直接阻断的规则类型 |
| `MANUAL_HANDOFF` | 人工转办规则 | 用于表达必须转人工继续处理或复核的规则类型 |
| `EXTERNAL_DEPENDENCY` | 外部依赖规则 | 用于表达必须依赖外部系统、外部维护人或外部口径材料的规则类型 |
| `FULL` | 完整覆盖 | `Coverage Declaration（覆盖声明）` 中表示覆盖完整、可按默认路由直接使用的状态编码 |
| `PARTIAL` | 部分覆盖 | `Coverage Declaration（覆盖声明）` 中表示仅部分覆盖、需要补充说明或审批的状态编码 |
| `GAP` | 覆盖缺口 | `Coverage Declaration（覆盖声明）` 中表示存在关键缺口、不能按默认链路直接放行的状态编码 |
| `ALLOW` | 允许 | `Policy（策略对象）` 中表示允许按当前契约返回的决策编码 |
| `REQUIRE_APPROVAL` | 需要审批 | `Policy（策略对象）` 中表示需进入审批或复核链路的决策编码 |
| `DENY` | 拒绝 | `Policy（策略对象）` 中表示当前请求必须阻断的决策编码 |
| `CHECKING` | 检查中 | 用于表达发布任务、审批任务或导出任务正在执行检查的状态编码 |
| `BLOCKED` | 已阻断 | 用于表达发布、审批或导出链路因门禁失败而被阻断的状态编码 |
| `FULL_MASK` | 全量遮蔽 | 用于表达字段返回时整体掩码的脱敏类型编码 |
| `PARTIAL_MASK` | 部分遮蔽 | 用于表达字段返回时保留部分信息的脱敏类型编码 |
| `HASH_ONLY` | 仅哈希 | 用于表达字段返回时只保留哈希值的脱敏类型编码 |
| `NONE` | 无回退动作 | 用于表达当前覆盖命中后不需要额外回退处理的动作编码 |
| `NEED_CLARIFICATION` | 需要澄清 | 用于表达当前请求需先补齐时间、标识或口径澄清后才能继续处理的动作编码 |
| `FACT_DETAIL` | 事实明细场景 | `Scene Type（场景类型）` 中表示明细查询类场景的枚举值 |
| `FACT_AGGREGATION` | 事实聚合场景 | `Scene Type（场景类型）` 中表示结果聚合或批次汇总类场景的枚举值 |
| `ENTITY_PROFILE` | 实体画像场景 | `Scene Type（场景类型）` 中表示客户、账户等画像查询类场景的枚举值 |
| `CHANGE_TRACE` | 变更轨迹场景 | `Scene Type（场景类型）` 中表示变更链路或轨迹查询类场景的枚举值 |
| `AUDIT_LOG` | 审计日志场景 | `Scene Type（场景类型）` 中表示高敏审计日志类场景的枚举值 |
| `WATCHLIST_CONTROL` | 名单管控场景 | `Scene Type（场景类型）` 中表示名单命中、管控核验类场景的枚举值 |
| `S0` | 无敏感级别 | 敏感等级中表示无敏感字段的级别编码 |
| `S1` | 低敏级别 | 敏感等级中表示普通内部字段的级别编码 |
| `S2` | 中敏级别 | 敏感等级中表示需默认脱敏返回的级别编码 |
| `S3` | 高敏级别 | 敏感等级中表示默认需审批或角色裁剪的级别编码 |
| `S4` | 极高敏级别 | 敏感等级中表示默认拒绝或专审的级别编码 |
| `SCENE_NOT_FOUND` | 场景未命中 | 运行时未召回正式 `Scene（业务场景）` 时的原因或错误编码 |
| `PLAN_NOT_ELIGIBLE` | 方案不可运行 | 场景已命中但没有满足硬门禁的可运行方案时的原因编码 |
| `COVERAGE_PARTIAL` | 覆盖部分满足 | 表示当前请求仅命中部分覆盖，需要审批、降级或显式说明的原因编码 |
| `POLICY_APPROVAL_REQUIRED` | 策略要求审批 | 表示命中 `Policy（策略对象）` 后必须走审批链路的原因编码 |
| `POLICY_DENIED` | 策略拒绝 | 表示命中高敏红线、角色红线或专审规则后的拒绝原因编码 |
| `EVIDENCE_INSUFFICIENT` | 证据不足 | 表示当前返回或发布缺乏足够 `Evidence Fragment（证据片段）` 支撑的原因编码 |
| `METADATA_DRIFT_BLOCKED` | 元数据漂移阻断 | 表示来源表、字段或快照漂移导致当前请求被阻断的原因编码 |
| `GRAPH_TIMEOUT` | 图查询超时 | 表示图查询服务超时的原因或错误编码 |
| `PUBLISH_BLOCKED` | 发布阻断 | 表示发布任务因门禁失败未能切换快照的原因编码 |
| `source_ingest_requested` | 来源接入已发起 | 表示原始材料已进入接入链路的事件名称 |
| `source_parsed` | 来源解析完成 | 表示解析与证据提取已完成的事件名称 |
| `metadata_aligned` | 元数据已对齐 | 表示表、字段和快照元数据已完成对齐的事件名称 |
| `review_requested` | 已发起复核 | 表示业务、技术或合规复核已创建的事件名称 |
| `snapshot_published` | 快照已发布 | 表示运行面快照切换完成的事件名称 |
| `metadata_drift_detected` | 检测到元数据漂移 | 表示来源元数据异常或漂移被识别的事件名称 |
| `approval_submitted` | 审批已提交 | 表示高敏审批任务已经提交的事件名称 |
| `approval_decided` | 审批已决策 | 表示审批结论已经回写的事件名称 |
| `export_generated` | 导出已生成 | 表示导出文件已经生成并归档的事件名称 |
| `DEV` | 开发环境 | 用于表达本地开发、匿名样例验证与快速联调环境的环境编码 |
| `TEST` | 测试环境 | 用于表达集成测试、回放测试与性能基线验证环境的环境编码 |
| `PROD` | 生产环境 | 用于表达正式运行、审批留痕与发布门禁生效环境的环境编码 |

### 接口术语

| 中文术语 | 英文全称 | 缩写 | 说明 |
|---------|---------|------|------|
| 场景搜索接口 | Scene Search API | - | 运行时按问题与槽位召回 `Scene（业务场景）` 候选的对外接口 |
| 方案选择接口 | Plan Select API | - | 运行时按覆盖与策略选择 `Plan（方案资产）` 的对外接口 |
| 知识包接口 | Knowledge Package API | - | 向下游返回 `Knowledge Package（知识包）` 的正式接口 |
| 发布检查接口 | Publish Check API | - | 执行发布门禁并返回阻断项的对外接口 |
| 发布执行接口 | Publish Execute API | - | 触发快照切换与发布执行的异步接口 |
| 影响分析接口 | Impact Analysis API | - | 查询版本、元数据和策略变更影响范围的对外接口 |
| 审批提交接口 | Approval Submit API | - | 提交高敏审批或导出审批申请的异步接口 |
| 审批决策接口 | Approval Decision API | - | 回写审批结论并触发后续执行的异步接口 |
| 发布任务状态接口 | Publish Job Status API | - | 查询发布执行进度、当前步骤与阻断项的同步接口 |
| 审批状态接口 | Approval Status API | - | 查询审批任务状态与审批结论的同步接口 |
| 导出提交接口 | Export Submit API | - | 提交导出任务的异步接口 |
| 导出状态接口 | Export Status API | - | 查询导出任务状态、脱敏方案和归档信息的同步接口 |
| 导出下载令牌接口 | Export Download Token API | - | 返回导出文件临时下载令牌的同步接口 |

### 关键业务字段

| 英文字段名 | 中文解释 | 说明 |
|---------|---------|------|
| `ESTB_TM` | 建立时间 / 申请时间戳 | 在基金申购申请类场景中常用于表达客户发起申请的时间 |
| `ENTR_DT` | 委托日期 | 用于表达委托进入业务处理链路的日期 |
| `TRX_CD` | 交易代码 | 用于区分申购、赎回、分红、撤单等交易类型 |
| `TRX_OBJ_ID` | 交易标的编号 | 用于标识交易对应的产品对象 |
| `ENTR_AMT` | 委托金额 | 客户发起委托时填写的金额 |
| `FND_TRX_AMT` | 资金交易金额 | 资金实际进入基金交易链路的金额 |
| `FEE_AMT` | 费用金额 | 交易过程中产生的费用金额 |
| `TRX_CHNL_TYP_CD` | 交易渠道类型代码 | 用于区分柜台、手机银行、电话等交易渠道 |
| `PROTOCOL_NBR` | 合作方协议号 / 代发协议号 | 代发场景中的协议主键，用于连接协议出款户口与合作方协议 |
| `CARD_NBR` | 户口号 / 出款户口号 | 协议出款户口表中的对公户口号 |
| `CLIENT_NBR` | 客户号 | 协议出款户口表中的客户编号 |
| `AGR_ID` | 合作方协议编号 | 合作方协议表中的协议主键，可与代发协议号对齐 |
| `CUST_ID` | 客户编号 | 合作方协议表或代发明细表中的客户编号 |
| `AGR_STS_CD` | 协议状态代码 | 合作方协议当前状态的代码字段 |
| `EFT_DT` | 生效日期 | 合作方协议开始生效的日期 |
| `AUTO_PAY_ARG_ID` | 自动缴费合约编号 | 代发明细表和代发批次表中用于串联协议与明细的关键字段 |
| `AGN_BCH_SEQ` | 代发批次号 | 代发批次或代发明细对应的批次编号 |
| `EAC_NBR` | 户口号码 | 代发明细中的收款户口号 |
| `EAC_NM` | 户口名称 | 代发明细中的收款户口名称 |
| `TRX_AMT` | 交易金额 | 代发明细中的实际交易金额 |
| `TRX_DT` | 交易日期 | 代发明细场景默认时间字段 |
| `TRX_TXT_CD` | 交易摘要代码 | 用于表达交易摘要或业务摘要的代码字段 |
| `CUST_SMR` | 客户摘要 | 用于补充交易摘要说明的文本字段 |
| `AGN_STS_CD` | 代发状态代码 | 用于标识代发明细或批次当前处理状态的代码字段 |
| `UID` | 统一标识 | 工单、管控和多标识归一链路中使用的统一对象标识 |
| `CUSTOMERID` | 客户标识字段 | 工单或外部输入中用于表达客户标识的入口字段 |
| `ACCOUNT_NUMBER` | 账号字段 | 外部输入或工单中用于表达账户标识的入口字段 |
| `CUST_UID` | 客户统一标识字段 | 外部输入中用于表达客户统一标识的入口字段 |
| `ID_NO` | 证件号码字段 | 外部输入中用于表达证件号的入口字段 |
| `cust_id` | 客户编号槽位 | 运行时输入槽位中表达客户编号的标准字段 |
| `cust_uid` | 客户统一标识槽位 | 运行时输入槽位中表达客户统一标识的标准字段 |
| `eac_nbr` | 户口号槽位 | 运行时输入槽位中表达户口号的标准字段 |
| `eac_seq_nbr` | 户序号槽位 | 运行时输入槽位中表达户口顺序号或子账户序号的标准字段 |
| `cert_no` | 证件号槽位 | 运行时输入槽位中表达证件号码的标准字段 |
| `protocol_nbr` | 协议号槽位 | 运行时输入槽位中表达协议编号的标准字段 |
| `agn_bch_seq` | 代发批次号槽位 | 运行时输入槽位中表达代发批次号的标准字段 |
| `product_id` | 产品标识槽位 | 运行时输入槽位中表达基金、理财或零售产品标识的标准字段 |

### 关键业务代码

| 代码 | 中文解释 | 说明 |
|---------|---------|------|
| `LK02` | 基金产品线代码 | 在财富产品查询中用于标识基金产品线 |
| `LW24` | 理财产品线代码 | 在财富产品查询中用于标识理财产品线 |
| `FD22` | 申购交易代码 | 在基金交易中用于标识申购动作 |
| `FD24` | 赎回交易代码 | 在基金交易中用于标识赎回动作 |
| `DSK` | 柜台渠道代码 | 用于标识柜台发起的交易渠道 |
| `MPH` | 手机银行渠道代码 | 用于标识手机银行发起的交易渠道 |
| `INT` | 个人银行大众版渠道代码 | 用于标识个人银行大众版发起的交易渠道 |
| `IEX` | 个人银行专业版渠道代码 | 用于标识个人银行专业版发起的交易渠道 |
| `SC_RTL_PROFILE_BASIC` | 零售基础画像场景编码示例 | 用于说明 `Scene（业务场景）` 命名建议采用“业务域_场景_语义”风格 |
| `PL_PAYROLL_DETAIL_HISTORY` | 薪资历史明细方案编码示例 | 用于说明 `Plan（方案资产）` 命名建议采用“业务域_场景_语义”风格 |

### 软件架构

| 中文术语 | 英文全称 | 缩写 | 说明 |
|---------|---------|------|------|
| 亚马逊云 | Amazon Web Services | AWS | 亚马逊云基础设施与托管服务平台 |
| 应用程序接口 | Application Programming Interface | API | 系统间交互接口 |
| 表述性状态转移 | Representational State Transfer | REST/RESTful | Web服务架构风格 |
| 数据传输对象 | Data Transfer Object | DTO | 跨层传输的数据结构 |
| 持久化对象 | Persistent Object | PO | 数据库映射对象 |
| 值对象 | Value Object | VO | 视图层数据对象 |
| 命令查询职责分离 | Command Query Responsibility Segregation | CQRS | 读写分离架构模式 |
| 领域驱动设计 | Domain-Driven Design | DDD | 软件设计方法论 |
| 容器编排系统 | Kubernetes | K8s | 容器编排平台 |
| 来源接入服务 | Source Intake Service | - | 接收原始材料、校验 `Source Intake Contract（来源接入契约）` 并生成导入任务的服务组件 |
| 解析与证据服务 | Parsing & Evidence Service | - | 负责文档、SQL 与工单解析，并产出候选资产与 `Evidence Fragment（证据片段）` 的服务组件 |
| 元数据对齐服务 | Metadata Alignment Service | - | 对齐来源表、字段、快照周期与新鲜度事实的服务组件 |
| 资产注册服务 | Asset Registry Service | - | 负责资产主键、版本、关系、状态机与快照登记的服务组件 |
| 复核与发布服务 | Review & Publish Service | - | 组织业务、技术、合规复核并执行发布门禁与快照切换的服务组件 |
| 推理构建服务 | Inference Build Service | - | 在知识生产期执行候选三元组收敛、确定性推理、置信度推理、冲突检测、推理链生成和候选结论归档的服务组件 |
| 运行推理服务 | Inference Runtime Service | - | 在运行时只读取已发布推理资产，完成标识收敛、时间语义收敛、关系补全、候选路径收敛，并按降级契约输出标准化推理断言与降级原因的服务组件 |
| 场景召回服务 | Scene Recall Service | - | 按问题、领域与槽位召回候选 `Scene（业务场景）` 的运行时服务 |
| 方案选择服务 | Plan Selection Service | - | 按输入槽位、覆盖、策略和来源约束选择可运行 `Plan（方案资产）` 的运行时服务 |
| 覆盖引擎 | Coverage Engine | - | 对 `Coverage Declaration（覆盖声明）` 和 `Coverage Segment（覆盖分段）` 做命中判定、完整度计算与回退解释的运行时服务 |
| 策略决策服务 | Policy Decision Service | - | 输出 `allow（允许）`、`need_approval（需要审批）`、`deny（拒绝）` 及字段级可见视图的运行时服务 |
| 路径模板 / 图查询服务 | Path Template / Graph Query Service | - | 在合格方案范围内执行模板化路径命中或受限图查询的运行时服务 |
| 数据地图门户 | Data Map Portal | - | 面向治理与消费方提供浏览、追溯、发布与差异分析能力的产品门户 |
| 审计与监控服务 | Audit & Monitoring Service | - | 聚合审计事件、运行指标、告警和日志检索能力的服务组件 |

### 流程与管理

| 中文术语 | 英文全称 | 缩写 | 说明 |
|---------|---------|------|------|
| 业务流程管理 | Business Process Management | BPM | 流程自动化系统 |
| 办公自动化 | Office Automation | OA | 企业办公系统 |
| 有向无环图 | Directed Acyclic Graph | DAG | 任务依赖关系图 |
| 最小可行产品 | Minimum Viable Product | MVP | 具备核心功能的最小版本 |
| 非功能需求 | Non-Functional Requirements | NFR | 用于约束性能、可靠性、安全性与可观测性的非功能目标 |
| 运行手册 | Runbook | - | 用于指导告警处置、故障排查、恢复和回滚的标准运维手册 |
| 高可用 | High Availability | HA | 用于保证关键控制面和运行面持续可用的架构能力 |
| 灾难恢复 | Disaster Recovery | DR | 在严重故障或机房级异常后恢复系统能力的设计与演练要求 |
| 恢复点目标 | Recovery Point Objective | RPO | 允许丢失的数据时间窗口目标 |
| 恢复时间目标 | Recovery Time Objective | RTO | 从故障到恢复可服务状态的时间目标 |
| 服务级别协议 | Service Level Agreement | SLA | 用于约束治理任务、运维处置和平台服务时效的协议口径 |
| 职责分工矩阵 | Responsible / Accountable / Consulted / Informed | RACI | 用于明确流程中执行、负责、协同和知会角色的责任矩阵 |
| 交付状态 | Delivery Status | - | 团队内部用于记录当前开发进度、下一动作、阻塞项和责任人的仓库级唯一状态入口 |
| 唯一真源 | Single Source of Truth | SSOT | 在同一主题上只保留一份正式维护入口，其他文档只允许引用链接的文档治理原则 |
| 任务接力契约 | Handoff Contract | - | 用于约束任务交接时必须写明当前状态、下一动作、阻塞项、责任人与更新时间的协作契约 |
| 智能体优先工程化 | Agent-First Engineering | - | 以 `AI（人工智能）` 可稳定理解、执行、评审和接力为目标组织仓库、文档、脚本和门禁的工程化方法 |
| 纵深优先执行 | Depth-First Working | - | 先固化边界、契约、测试入口和复用构件，再逐层展开业务实现的执行方式 |
| 脚手架任务 | Scaffolding Task | - | 为后续稳定实施提前补齐的测试夹具、`Mock（模拟对象）`、纯逻辑抽离、脚本、模板和断言工具等任务 |
| 机械化门禁 | Mechanical Enforcement | - | 把共享规则逐步转为 `lint（静态检查）`、结构校验脚本、测试或 `CI（持续集成，Continuous Integration）` 等可执行门禁的治理方式 |
| 黄金路径样例 | Golden Path | - | 为共享目录、接口、页面主路径或协作规则提供的最小可复用参考样例或模板，用于降低后续实现与评审歧义 |
| 坏模式回收 | Bad Pattern Cleanup | - | 在评审或验收阶段主动识别并回收重复实现、脆弱测试、命名漂移、失效计划和未回写文档等问题的治理动作 |

---

## 技术代号说明

### CALIBER系列

| 代号 | 含义 | 使用场景 |
|------|------|---------|
| `CALIBER_IMPORT_V2` | 数据直通车当前支持的资产模型导入格式（V2） | JSON 模板类型标识 |
| `CALIBER_IMPORT_V1` | 数据直通车历史导入格式（V1） | 存量兼容标识，不再新增 |
| `CALIBER_EXPORT_V1` | 数据地图导出格式第1版 | 导出配置标识 |
| `caliber_import_task` | 导入任务表 | 数据库表名 |
| `caliber_scene` | 业务场景表 | 数据库表名 |
| `caliber_scene_version` | 场景版本表 | 数据库表名 |
| `caliber_topic_node` | 主题节点表 | 数据库表名（历史或预留，不代表当前核心主图已启用） |
| `caliber_dict` | 字典表 | 数据库表名 |
| `caliber_service_spec` | 服务说明表 | 数据库表名（发布态消费契约存储，不代表当前核心主图已启用） |
| `caliber_evidence_source` | 证据来源表 | 数据库表名 |
| `caliber_evidence_fragment` | 证据片段表 | 数据库表名 |
| `caliber_evidence_link` | 证据引用表 | 数据库表名 |
| `caliber_publish_confirmation` | 发布确认表 | 数据库表名 |

> **重要说明**：CALIBER是数据地图的技术代号，用于API、数据库、配置文件等技术层面。面向用户时统一使用"数据地图"。

---

## 术语使用规范

### 文档中的使用规则

1. **每次出现**：必须使用完整格式
   - 格式：`英文术语（中文翻译，English Full Name）`
   - 示例：`RAG（检索增强生成，Retrieval-Augmented Generation）`

2. **覆盖范围**：正文、表格、图注、样例字段说明统一执行
   - 不允许“前文已解释，后文省略”的写法

3. **例外范围**：仅以下内容可不重复注释
   - 代码常量（如 `CALIBER_IMPORT_V2`）
   - 接口路径（如 `/api/nl/validate`）
   - 论文标题原文（保持检索一致性）

4. **变量与指标**：变量名、常量名、指标名每次出现都必须带中文解释
   - 示例：`schema_link_score（模式链接得分）`
   - 示例：`trace_id（追踪编号）`

5. **新增项入表**：正文中新出现的英文术语、缩写、变量名、常量名、指标名，如未被本文档收录，必须先补充到术语表，再进入主方案、评审记录、用户手册与其他说明文档
   - 示例：新增 `join_relations（表间关联关系）` 时，先补术语表，再在方案正文中使用
   - 示例：新增 `alignment_refs（对齐引用）` 时，先补术语表，再在样例字段说明中使用

6. **JSON / JSONC（带注释的JSON展示格式）样例**：若标准 `JSON（JavaScript对象表示法）` 无法直接写中文注释，必须采用以下任一方式补充中文解释
   - 使用 `JSONC（带注释的JSON展示格式）`
   - 在样例前后补充字段说明表
   - 在段落中逐字段解释变量含义

### 代码中的使用规则

1. **变量命名**：使用英文
   - 示例：`sceneId`、`caliberImportV2`

2. **注释说明**：使用中文
   - 示例：`// 业务场景ID`

3. **API路径**：使用英文
   - 示例：`/api/caliber/scenes`

---
