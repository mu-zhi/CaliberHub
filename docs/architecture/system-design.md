# 知识图谱与数据地图方案

## 1. 项目背景

### 1.1 业务背景与角色定位

数据直通车建设的业务起点，来源于银行大数据部门公共支持组长期承担的临时取数服务职责。当前典型模式是：分行业务人员通过内部工单发起数据申请，申请经审批后流转至总行支持团队，由支持人员理解业务诉求、确认业务和数据口径、编写 `SQL（结构化查询语言，Structured Query Language）` 从数据湖取数并交付结果。经过多年运行，团队已经积累了大量高频重复的取数场景、历史 `SQL（结构化查询语言，Structured Query Language）` 样例和规模可观的口径文档，这些内容本质上已经形成了一套高价值但尚未结构化治理的业务知识资产。与此同时，现有“数据直通车”系统已经具备模板取数、模板开发、日志记录、系统管理以及 `OA（办公自动化，Office Automation）` 审批集成等基础能力，为后续升级提供了运行底座。

### 1.2 数据直通车 1.0 的形成与瓶颈

数据直通车 1.0 的建设逻辑，建立在长期运营中对高频重复需求的观察之上。团队在处理大量取数需求后发现，较高比例的查询在业务语义上高度相似，变化主要集中在查询时间、客户身份、机构范围等参数，因此将这些需求抽象为查询模板，能够在一定阶段内显著提升交付效率。该模式解决了部分重复劳动问题，但随着业务复杂度提升，其局限也逐渐显现：模板供给依赖人工配置和持续维护，业务场景难以穷尽，模板本身表达能力有限，复杂需求仍然需要依赖人工编写 `SQL（结构化查询语言，Structured Query Language）`，系统整体扩展能力受到明显约束。

### 1.3 核心矛盾已经从“执行侧”转向“供给侧”

从现状看，数据直通车的主要瓶颈已经不再是审批流程或取数执行本身，而是知识与模板的供给侧能力不足。现有口径文档虽然信息丰富，但长期以自由文本、`SQL（结构化查询语言，Structured Query Language）` 片段、历史备注、附件和示例脚本等形式分散沉淀，缺乏统一的结构化组织与治理机制，导致“找不到、看不懂、不可信、不可复用、不可治理、不可自动化”等问题并存。对使用者而言，复杂查询还普遍存在语义断层、路径迷失、跨系统标识映射复杂、共享语义维护成本高、需求边界不清等问题；对新同事而言，往往需要逐行阅读 `SQL（结构化查询语言，Structured Query Language）` 才能理解场景口径、输入输出和关键表关系。换言之，系统真正缺的不是更多零散脚本，而是一套能把业务知识、数据口径和历史经验持续沉淀下来的统一知识底座。

### 1.4 从模板取数走向智能数据服务的演进方向

结合产品设计与定位材料，数据直通车的远期方向已经明确：系统将从 1.0 的模板取数能力，逐步演进为以“数据地图 + 自然语言查询”为核心的智能数据服务体系。一方面，需要把多年积累的业务分类、场景说明、口径定义、字段语义和规则知识组织成可浏览、可搜索、可导航的数据地图；另一方面，需要在此基础上逐步支撑自然语言提数、场景解释、知识检索，以及后续更受控的下游 `SQL（结构化查询语言，Structured Query Language）` 生成与执行链路。这个演进方向说明，未来系统竞争力不只来自“能否执行查询”，更来自“能否理解业务问题、能否稳定解释数据、能否将历史经验转化为可复用知识”。

### 1.5 本项目的建设必要性

基于上述背景，本项目首先要解决的，不是直接建设一个端到端自动生成 `SQL（结构化查询语言，Structured Query Language）` 的系统，而是先补齐智能化能力真正缺失的上游基础，即口径治理与知识供给能力。已有定位材料已经明确指出：口径治理与数据地图并非两套割裂系统，而是同一条建设主线的两个阶段；只有先将散落在文档、工单、历史 `SQL（结构化查询语言，Structured Query Language）` 和人工经验中的知识沉淀为结构化、可编辑、可校验、可发布、可追溯的语义资产，后续的数据地图、`RAG（检索增强生成，Retrieval-Augmented Generation）`、`PlanIR（计划中间表示，Plan Intermediate Representation）` 以及自然语言取数能力才有稳定可信的知识来源。也正因如此，本方案建设的不是一个“直接执行取数”的系统，而是一套面向口径治理、问题缩域、证据追溯和下游智能消费的受控知识底座。

### 1.6 当前阶段定位

在当前阶段，项目的核心任务是围绕“口径治理 -> 数据地图 -> 可控智能化”建立统一的知识组织与服务框架：以前台能够理解的业务问题为入口，以 `Scene（业务场景，Business Scene）` 作为最小业务收敛单元，以 `Plan（方案资产，Plan Asset）`、`Output Contract（输出契约，Output Contract）`、`Contract View（契约视图，Contract View）`、`Coverage Declaration（覆盖声明，Coverage Declaration）`、`Policy（策略对象，Policy）`、`Evidence Fragment（证据片段，Evidence Fragment）` 等对象作为正式治理资产，并持续沉淀语义解释、派生规则、字典、主题节点等支撑知识，形成可编辑、可校验、可发布、可追溯、可服务化输出的知识体系。

这样做的目的，是先把“知识从哪里来、是否可信、适用边界是什么、为什么这样解释”这些基础问题解决清楚，再以 `Knowledge Package（知识包，Knowledge Package）` 而不是裸 `SQL（结构化查询语言，Structured Query Language）` 的形式支撑下游审批、解释、执行与自然语言能力，确保后续系统演进符合银行场景对合规、安全、审计和可回滚的要求。

### 1.6.1 当前首轮交付主链路与真实库边界

当前首轮交付不按“全域平台一次铺开”验收，而是固定聚焦 `代发 / 薪资域` 的端到端主链路，并要求形成“单业务域多场景”而非单样例演示。首轮主链路的验收顺序固定为：前端上传口径文档 -> 建立 `Source Material（来源材料）`、`Import Task（导入任务）` 与 `Source Intake Contract（来源接入契约）` -> 完成解析抽取与证据确认 -> 落成正式治理资产 -> 生成 `Version Snapshot（版本快照）` -> 向图谱侧投影 -> 运行检索 -> 返回结构化 `Knowledge Package（知识包）`。

首轮交付的存储边界同步固定如下：

1. `关系型控制库` 是首轮正式主数据真源，至少承载来源材料、导入任务、来源接入契约、证据片段、业务场景、方案资产、输出契约、契约视图、覆盖声明、策略对象、来源契约和版本快照。
2. `图谱存储` 只承担已发布快照的投影、浏览与检索，不承担正式治理资产真源；任何图谱节点和关系都必须能回到关系型控制库中的正式对象。
3. 首轮运行与联调必须使用真实关系型数据库，不再把 `H2（内存数据库）` 作为运行基线；当前设计、迁移脚本和本地联调按 `MySQL（关系型数据库，MySQL）` 兼容基线推进。
4. 数据地图高可见层属于首轮并行子线，其验收职责是支撑主链路解释与上下文定位，而不是独立替代主链路。

## 2. 执行摘要

数据直通车上游建设对象是“受控知识底座”，不是“受控取数执行系统”。系统负责把口径文档、工单样例、历史 `SQL（结构化查询语言，Structured Query Language）`、人工确认与元数据事实沉淀为可治理、可发布、可解释的知识资产，并以 `Knowledge Package（知识包）` 的形式向下游 `PlanIR（计划中间表示，Plan Intermediate Representation）`、`NL2SQL（自然语言转 SQL，Natural Language to SQL）`、人工复核和数据地图产品提供统一输入边界。

本方案的运行时主线固定为：`Query Rewrite（查询改写）` -> `Slot Filling（槽位补齐）` -> `Inference Runtime（运行推理）` -> `Scene Recall（场景召回）` -> `Plan Selection（方案选择）` -> `Coverage Engine（覆盖引擎）` -> `Coverage Declaration（覆盖声明）` / `Policy（策略对象）` / `Output Contract（输出契约）` 硬过滤 -> `Path Template / Graph Query Service（路径模板 / 图查询服务）` -> `Knowledge Package（知识包）` 输出。运行时返回的不是裸表、裸字段或临时脚本，而是受控知识包。

系统设计采用“控制资产层 + 场景层 + 语义层 + 推理层 + 元数据层 + 治理层”的分层模型，并通过三类能力形成闭环：

1. 知识生产面：负责材料接入、解析抽取、证据沉淀、资产建模、推理构建、元数据对齐、复核发布与快照切换。
2. 运行服务面：负责运行推理、场景召回、方案选择、覆盖判定、策略决策、路径解析和知识包构建。
3. 治理产品面：负责数据地图浏览、覆盖矩阵、影响分析、发布中心、审计查询与运维视图。

首发阶段聚焦代发、财富、零售三类样板域，优先打穿多标识归一、历史分段覆盖、多时间语义、高敏字段控制、审批导出和证据追溯六条关键能力，形成可复制的治理模板。

当前首轮交付进一步收敛为：先在 `代发 / 薪资域` 内打穿 2 至 3 个已发布业务场景的真实链路，验证“真实关系型控制库 -> 正式治理资产 -> 已发布快照 -> 图谱投影 -> 运行检索 -> 知识包返回”这一条主线，再向财富与零售域复制。

### 2.1 治理产品面的前端承载

治理产品面的页面信息架构、中文界面口径、导航结构、权限表达、工作台骨架和关键交互，统一由 [前端界面与工作台设计](frontend-workbench-design.md) 承接。本文继续只定义对象、边界、规则、运行主线和接口契约，不在主文档内展开页面级布局与视觉细节。

该分工属于项目级文档路由原则，而不是临时写作习惯：凡属前端页面结构、导航、交互、状态表达和评审留痕，均回写前端主文档；凡属方案对象、运行主线、接口契约和治理边界，均回写本文。同一需求若同时影响两类内容，必须拆分更新两份主文档，不互相挤占。

## 3. 项目目标、范围与职责边界

项目目标不是“把文档和 `SQL（结构化查询语言，Structured Query Language）` 放进图数据库”，而是建设一套可被治理、可被发布、可被解释、可被下游可靠消费的受控知识底座。该底座把原始口径文档、工单样例、历史 `SQL（结构化查询语言，Structured Query Language）`、人工确认与元数据事实收敛成可运行的知识资产，并以 `Knowledge Package（知识包）` 的形式供下游消费。

### 3.1 设计目标

1. 把用户自然语言问题先收敛到可解释的 `Scene（业务场景）`，再收敛到满足 `Coverage Declaration（覆盖声明）`、`Policy（策略对象）` 与 `Output Contract（输出契约）` 的可运行 `Plan（方案资产）`。
2. 让所有正式输出都具备证据可回溯、版本可追踪、风险可判断、覆盖可说明的能力。
3. 把时间语义冲突、历史分段、多入口标识、高敏字段和资料不完整问题制度化处理。
4. 形成从材料接入到发布、从运行到反馈、从审查到回退的完整闭环。

### 3.2 非目标

1. 不直接生成最终 `SQL（结构化查询语言，Structured Query Language）`，不承担执行编排与结果装配。
2. 不在本系统内定义业务前台的具体交互页面与视觉细节。
3. 不在主文档中绑定某一特定图数据库厂商产品。
4. 不把缺口任务、审批系统、统一认证系统完全重建在本系统内；本系统通过接口对接这些基础能力。

### 3.3 角色与职责边界

| 角色 | 主要职责 |
| --- | --- |
| 业务口径负责人 | 确认 `Scene（业务场景）` 边界、结果口径、默认时间语义、不可适用范围 |
| 数据治理 / 数据产品 | 维护 `Source Intake Contract（来源接入契约）`、资产台账、发布门禁、缺口任务与样板场景清单 |
| 数据研发 / 平台研发 | 实现解析、对齐、发布、检索、接口与运行组件 |
| 合规 / 安全 | 定义敏感等级、审批链路、导出规则、审计留痕要求 |
| 运维 / SRE | 保障存储、服务、队列、监控、告警、备份与恢复 |
| 下游消费方 | 消费 `Knowledge Package（知识包）` 和审计解释信息，不绕过上游边界直接使用原始裸表 |

### 3.4 设计输入与样板约束

| 来源材料 | 暴露出的约束 | 设计落点 |
| --- | --- | --- |
| 工单样例集 | 同一请求可通过 `CUSTOMERID`、`ACCOUNT_NUMBER`、`CUST_UID`、`ID_NO` 匹配 `UID`，并需按投诉日期回看当日及之前的最近管控记录 | `Input Slot Schema（输入槽位模式）`、`Identifier Lineage（标识谱系）`、`as_of_time（截止时点）`、敏感结果裁剪 |
| 零售客户信息查询 | 当前状态查询大量依赖 `current_date - 1` 快照，且账户、户口、财富账户语义复杂 | 快照新鲜度、默认时间语义、`ENTITY_PROFILE` 场景拆分 |
| 零售客户信息变更 | 存在“场景描述未提供 / 结果字段未提供”的不完整材料；密码修改日志为高敏审计类数据 | `Source Intake Contract（来源接入契约）`、`Gap Task（缺口任务）`、`S4` 高敏策略、专审链路 |
| 代发明细查询 | 2004 年前不可查；2004-2013 多历史源；2014+ 主表；协议号与批次号是不同入口 | `Coverage Segment（覆盖分段）`、历史 `Plan（方案资产）`、跨段去重、`Scene（业务场景）` / `Plan（方案资产）` 拆分 |
| 基金理财保险查询 | 申请日期、委托日期、资金日期、份额日期常被混淆；SA 理财只覆盖 2020-08 以后 | `Time Semantic Selector（时间语义选择器）`、`Coverage Declaration（覆盖声明）`、默认澄清与场景切换 |

## 4. 系统上下文与总体架构

本章从系统上下文视角定义知识底座与上下游系统之间的边界。系统位于“原始材料与治理参与者”和“下游消费链路”之间，分成知识生产面与运行服务面两大子系统。知识生产面负责把材料变成受治理资产；运行服务面负责按当前请求产出受控知识包。

### 4.1 系统上下文

| 边界对象 | 包含内容 | 边界说明 |
| --- | --- | --- |
| 上游输入 | 口径文档、工单样例、历史 `SQL（结构化查询语言，Structured Query Language）`、元数据平台、人工确认、组织 / 字典基础数据 | 由接入契约和导入校验约束，不能绕过 |
| 本系统 - 知识生产面 | `Source Intake Service（来源接入服务）`、`Parsing & Evidence Service（解析与证据服务）`、`Asset Registry Service（资产注册服务）`、`Inference Build Service（推理构建服务）`、`Metadata Alignment Service（元数据对齐服务）`、`Review & Publish Service（复核与发布服务）` | 产生可发布快照与治理台视图 |
| 本系统 - 运行服务面 | `Inference Runtime Service（运行推理服务）`、`Scene Recall Service（场景召回服务）`、`Plan Selection Service（方案选择服务）`、`Coverage Engine（覆盖引擎）`、`Policy Decision Service（策略决策服务）`、`Path Template / Graph Query Service（路径模板 / 图查询服务）`、`Knowledge Package API（知识包接口）` | 服务下游消费和数据地图查询 |
| 下游系统 | `PlanIR（计划中间表示，Plan Intermediate Representation）`、`NL2SQL（自然语言转 SQL，Natural Language to SQL）`、人工复核台、导出 / 审批系统、查询解释组件 | 只消费受控知识包，不反向改写已发布资产 |
| 横切能力 | 统一认证、审批、审计日志、监控告警、对象存储、消息队列 | 以平台能力方式接入，纳入本设计约束 |

### 4.2 总体架构分层

| 层级    | 核心对象                                                                                                                                                                                            | 职责                                |
| ----- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------- |
| 控制资产层 | `Plan（方案资产）`、`Output Contract（输出契约）`、`Contract View（契约视图）`、`Coverage Declaration（覆盖声明）`、`Policy（策略对象）`、`Input Slot Schema（输入槽位模式）`、`Time Semantic Selector（时间语义选择器）`                            | 决定硬门禁与正式承诺边界                      |
| 场景层   | `Domain（业务领域）`、`Scene（业务场景）`、`Scene Type（场景类型）`、业务场景边界                                                                                                                                          | 定义业务问题、主对象与分类边界                   |
| 语义层   | `Canonical Term（规范词元）`、`Business Term（业务术语）`、`Business Caliber（业务口径）`、`Field Concept（字段概念）`、`Composite Concept（组合概念）`、`Derivation Rule（派生规则）`、`Dictionary（字典）`、`Join Relation Object（表间关联关系对象）` | 定义可被推理消费的术语语义、规则语义与对象语义           |
| 推理层   | `Inference Rule（推理规则）`、`Inference Assertion（推理结论）`、`Inference Chain（推理链）`、`Inference Confidence Record（推理置信记录）`、`Inference Scope Profile（推理适用边界）`                                               | 把语义规则、元数据事实和证据沉淀为可发布、可消费、可解释的推理资产 |
| 元数据层  | `Database（数据库）`、`Schema（模式）`、`Source Table（来源表）`、`Column（字段）`、快照周期、可用性与新鲜度事实                                                                                                                    | 提供真实物理落点与变更事实                     |
| 治理层   | `Evidence Fragment（证据片段）`、`Version Snapshot（版本快照）`、`Review Task（复核任务）`、`Gap Task（缺口任务）`、影响分析、`Audit Event（审计事件）`                                                                                | 承接证据、版本、审批、缺口与发布                  |

知识生产期在上述正式层级之外，额外存在一层按 `material_id（材料标识）` / `task_id（任务标识）` 隔离的 `Candidate Entity Graph（候选实体图谱）`。它只服务“解析抽取 -> 人工确认 -> 资产建模”这段链路，不进入已发布主图，也不被运行时直接消费。候选图中的节点分三组：

1. `治理候选层`：`Candidate Scene（候选场景）`、`Candidate Plan（候选方案资产）`、`Candidate Evidence Fragment（候选证据片段）`。
2. `业务语义层`：`Identifier（标识对象）`、`Field Concept（字段概念）`、`Business Term（业务术语）`、`Time Semantic（时间语义）`。
3. `物理来源层`：`Source Table（来源表）`、`Column（字段）`、候选表间关联。

### 4.3 服务组件拆分

| 组件 | 职责 | 主责任 |
| --- | --- | --- |
| `Source Intake Service（来源接入服务）` | 接收原始材料、校验 `Source Intake Contract（来源接入契约）`、生成导入任务与缺口任务 | 治理 / 数据产品 |
| `Parsing & Evidence Service（解析与证据服务）` | 解析文档、`SQL（结构化查询语言，Structured Query Language）`、工单，提取证据片段与候选资产 | 数据研发 |
| `Asset Registry Service（资产注册服务）` | 维护资产主键、版本、关系、状态机与缓存刷新，支撑资产建模 | 平台研发 |
| `Inference Build Service（推理构建服务）` | 在知识生产期执行候选三元组收敛、确定性推理、置信度推理、冲突检测、推理链生成和候选结论归档，并把正式生效候选送入复核与发布 | 数据研发 |
| `Metadata Alignment Service（元数据对齐服务）` | 校验建模阶段声明的逻辑字段，对齐表、快照周期与可用性，生成 `Source Contract（来源契约）` | 数据研发 |
| `Review & Publish Service（复核与发布服务）` | 组织业务、技术、合规复核，执行发布门禁与快照切换 | 治理 + 研发 |
| `Inference Runtime Service（运行推理服务）` | 只读取当前已发布推理资产，完成标识收敛、时间语义收敛、关系补全、候选路径收敛和解释链拼装，并向场景、方案和路径决策输出标准化推理断言 | 运行服务 |
| `Scene Recall Service（场景召回服务）` | 按问题与槽位召回 `Domain（业务领域）` / `Scene（业务场景）` 候选 | 运行服务 |
| `Plan Selection Service（方案选择服务）` | 按输入槽位、`Coverage Declaration（覆盖声明）`、`Policy（策略对象）`、`Source Contract（来源契约）` 过滤并选择可运行 `Plan（方案资产）` | 运行服务 |
| `Coverage Engine（覆盖引擎）` | 对 `Coverage Declaration（覆盖声明）` 与 `Coverage Segment（覆盖分段）` 做命中判定、完整度判断、回退动作选择与覆盖解释生成 | 运行服务 |
| `Policy Decision Service（策略决策服务）` | 输出 `allow（允许）` / `need_approval（需要审批）` / `deny（拒绝）`，并给出字段级视图 | 运行服务 |
| `Path Template / Graph Query Service（路径模板 / 图查询服务）` | 在运行推理服务给出的合格推理断言和候选路径范围内走模板或受限图查询 | 运行服务 |
| `Knowledge Package API（知识包接口）` | 统一对外提供知识包、错误码与追踪信息 | 运行服务 |
| `Data Map Portal（数据地图门户）` | 提供浏览、追溯、差异、发布与运维视图 | 治理产品 |
| `Audit & Monitoring Service（审计与监控服务）` | 聚合事件、指标、告警与运行日志 | 运维 / SRE |

### 4.4 图谱推理设计

图谱推理在本项目中不再只是语义层中的附属规则能力，也不是图查询服务的旁路补丁，而是一等核心能力。它同时承担两类职责：在知识生产期把材料、元数据和证据收敛成可发布的正式推理资产；在运行时只消费当前已发布推理资产，直接参与 `Scene（业务场景）`、`Plan（方案资产）` 和路径决策，但不在运行时临时生成新的正式事实。

当前阶段允许进入决策链的推理仅限两类：一类是显式规则、标识映射、时态规则、受限图模式匹配等可审计确定性推理；另一类是多跳关系收敛、候选关系补全等置信度推理，但必须同时具备证据引用、适用边界、置信分值与复核结论。`LLM（大语言模型）` 语义推断当前阶段不能直接进入正式事实层。

### 4.4.1 规则二分法与层间边界

1. `Derivation Rule（派生规则）` 继续留在语义层，只定义规则的形式语义，包括输入变量、输出变量、逻辑表达式和业务解释。
2. `Inference Rule（推理规则）` 留在推理层，表达某条可执行推理规则在当前语义基线、来源约束和证据约束下的实例化绑定，不重复定义规则语义本身。
3. 推理层产出的正式对象是 `Inference Assertion（推理结论）`、`Inference Chain（推理链）` 等运行后资产；语义层不直接保存“规则已经跑出了什么结论”。
4. 控制资产层继续保留对 `Coverage Declaration（覆盖声明）`、`Policy（策略对象）`、`Output Contract（输出契约）` 的最终硬门禁解释权。推理层负责“可推导出什么”，控制资产层负责“推出来的结果能不能被正式使用”。

### 4.4.2 推理资产模型

1. `Inference Rule（推理规则）`：定义可执行推理规则的实例化绑定、触发条件、适用范围、置信度策略和来源约束。
2. `Inference Assertion（推理结论）`：定义正式进入快照的推理结果，例如标识收敛结论、时态收敛结论、关系补全结论和候选场景收敛结论。
3. `Inference Chain（推理链）`：定义某条推理结论的输入事实、规则路径、中间节点和解释摘要，是审计、复核和运行解释的核心对象。
4. `Inference Confidence Record（推理置信记录）`：定义置信度推理的评分结果、阈值命中情况、人工复核要求和复核结论。
5. `Inference Scope Profile（推理适用边界）`：定义推理结论适用的业务域、时间范围、来源范围、对象范围、禁止适用场景、`confidence_thresholds（置信阈值组）`、`gray_release_scope（灰度发布范围）` 和 `degrade_mode（降级模式）`。

### 4.4.3 置信门禁与阈值治理

1. 置信度阈值、候选阈值、人工复核阈值和阻断阈值不得散落在运行代码、脚本常量或人工口口相传规则中，必须作为 `Inference Scope Profile（推理适用边界）` 的正式受治理字段发布。
2. 不同 `Scene Type（场景类型）`、关系类型、风险等级和业务域允许采用不同 `confidence_thresholds（置信阈值组）`，但每组阈值都必须具备版本、责任人、适用边界和回放验证记录。
3. 运行时只能读取当前 `inference_snapshot_id（推理快照标识）` 绑定的阈值配置，不允许按线程、请求或调用方动态覆盖正式阈值。
4. 任一阈值调整都按“影响正式决策链”的变更处理，必须经过样板回放、差异分析和发布门禁，才能进入新的推理快照。

### 4.4.4 推理资产与 `Knowledge Package（知识包）` 的协同发布

1. 所有正式生效的推理结果必须在知识生产期完成构建、复核和发布，不允许在运行时临时写入新的正式推理事实。
2. 推理资产不走独立发布线，而是与当前 `Knowledge Package（知识包）` 依赖的控制资产、语义资产和元数据事实共同进入同一发布批次。
3. 每个 `snapshot_id（快照标识）` 必须绑定唯一的 `inference_snapshot_id（推理快照标识）`；运行时读取 `Scene（业务场景）`、`Plan（方案资产）` 与推理资产时，必须以该映射做强一致版本对齐，禁止跨快照混读。
4. 任一推理资产复核失败、置信度未达阈值、适用边界不完整或证据链不可回溯时，不得进入 `PUBLISHED（已发布）` 快照。

### 4.4.5 运行时断言注入、降级契约与冲突仲裁

1. `Inference Runtime Service（运行推理服务）` 对 `Scene Recall Service（场景召回服务）`、`Plan Selection Service（方案选择服务）` 和 `Path Template / Graph Query Service（路径模板 / 图查询服务）` 输出统一的推理断言视图，至少包含：断言类型、主对象、关联对象、置信分值、适用边界、证据引用和解释摘要。
2. `Scene Recall（场景召回）` 只消费“收敛后的主对象、候选场景和推理理由”；`Plan Selection（方案选择）` 只消费“与当前方案筛选相关的推理断言”；`Path Resolution（路径解析）` 只消费“可进入路径命中的推理关系与候选路径”。
3. 运行时降级必须遵循固定序列：`full_inference（完整推理） -> deterministic_only（仅确定性推理） -> template_only（仅模板路径） -> clarification_only（仅澄清返回）`。触发条件包括推理超时、推理断言索引不可读、置信度记录缺失、适用边界不完整或下游控制门禁拒绝消费候选断言。
4. 一旦发生降级，系统必须在 `Knowledge Package（知识包）` 的 `inference（推理信息）` 字段中返回 `runtime_mode（运行模式）` 与 `degrade_reason_codes（降级原因编码列表）`；任何降级路径都不得补写未发布断言、不得把候选结论伪装成正式结论，也不得绕过 `Coverage Declaration（覆盖声明）`、`Policy（策略对象）` 与 `Output Contract（输出契约）`。
5. 冲突仲裁遵循三级决策树：确定性推理冲突直接阻断发布；置信度推理冲突降级为候选断言并挂人工复核；适用边界冲突先收窄 `Inference Scope Profile（推理适用边界）` 后再决定是否允许条件发布。
6. 推理冲突不绕过现有硬门禁。进入运行时前，所有断言仍需接受 `Coverage Declaration（覆盖声明）`、`Policy（策略对象）` 与 `Output Contract（输出契约）` 的最终约束。

### 4.4.6 推理快照灰度发布与回滚

1. 推理灰度发布只能针对已经通过发布门禁的 `snapshot_id（快照标识）` / `inference_snapshot_id（推理快照标识）` 绑定对执行，禁止把 `DRAFT（草稿）` 或未复核推理资产直接暴露给灰度流量。
2. `gray_release_scope（灰度发布范围）` 必须结构化定义到领域、场景、角色、机构、样板请求集或流量分片；灰度流量外的请求继续读取上一稳定快照对。
3. 灰度期间必须持续比较新旧快照对的场景命中率、方案命中率、推理降级率、误路由率和高敏误放行风险；任一核心指标超阈值时，发布任务应自动冻结，并优先回退到上一稳定快照对。
4. 回滚单位永远是成对快照：`snapshot_id（快照标识）` 与 `inference_snapshot_id（推理快照标识）` 一起回退，禁止只回滚控制资产或只回滚推理资产。

### 4.4.7 `LLM（大语言模型）` 产出隔离规范

1. `Query Rewrite（查询改写）` 和 `Slot Filling（槽位补齐）` 阶段允许使用 `LLM（大语言模型）` 做语义补全，但输出只能作为临时运行信号，例如 `rewrite_hint（改写提示）`、`slot_hint（槽位提示）`、`clarification_hint（澄清提示）`。
2. `LLM（大语言模型）` 临时信号若不能被已发布 `Inference Assertion（推理结论）`、`Inference Rule（推理规则）`、`Source Contract（来源契约）` 或显式校验规则二次验证，只能进入澄清问题或候选提示，不得直接进入正式决策断言。
3. 运行时 `LLM（大语言模型）` 输出不得回写 `Inference Assertion（推理结论）`、`Inference Chain（推理链）`、`Evidence Fragment（证据片段）`、`Scene（业务场景）`、`Plan（方案资产）` 或 `Source Contract（来源契约）`。
4. `LLM（大语言模型）` 服务异常时，运行面退回规则抽取、关键词归一和澄清问题路径；不能因 `LLM（大语言模型）` 不可用而放宽正式推理门禁。

### 4.4.7.1 导入预处理 OpenAI 集成迁移拆分

1. 导入预处理链路的 OpenAI 集成迁移不得作为一个“大特性”直接整体推进，必须拆成三个专题特性：`provider 路由与模型治理`、`OpenAI Responses 预处理适配`、`预处理结构化输出与 Prompt 治理`。
2. `provider 路由与模型治理` 负责 provider 预设、模型能力矩阵、参数白名单、切换审计、灰度验证与回退；不负责具体 Prompt 文案和响应解析。
3. `OpenAI Responses 预处理适配` 负责把统一预处理域请求映射到 OpenAI `Responses API（响应式接口）` 并回收统一结果；不得通过“只替换 endpoint”方式硬迁移。
4. `预处理结构化输出与 Prompt 治理` 负责 Prompt 版本、Schema、结构化校验、预览测试和人工审核标记；它不定义 provider 路由规则，只消费 provider 能力矩阵。
5. 三个专题特性必须按“provider 治理先行 -> Responses 适配 -> Prompt / Schema 治理收口”的顺序实施；若任一前置专题未落地，后续专题不得直接越级实现。

### 4.4.8 推理链归档与规模控制

1. `Inference Chain（推理链）` 不是所有中间路径的无差别全量归档对象，而应分级管理。
2. 面向监管解释、审计溯源和发布复核的关键推理链必须全量保存。
3. 面向日常运行优化的中间候选路径允许只保存摘要，例如起点、终点、跳数、关键节点编号和核心证据引用。
4. 未进入正式发布快照的临时候选推理链在复核完成后应按归档策略清理，避免指数级膨胀为长期治理负担。

## 5. 统一资产模型与状态机设计

本章以“控制资产 + 图谱资产 + 治理资产”三组对象构成正式资产模型。目标不是把所有对象都塞进同一存储，而是保证：无论对象落在图数据库、关系型库还是对象存储中，都受同一套治理语义、版本管理和发布门禁约束。

### 5.1 受治理资产清单

| 对象                                    | 中文      | 关键字段                                                                                                                      | 主要用途                                                           |
| ------------------------------------- | ------- | ------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| `Domain（业务领域）`                        | 业务领域    | `domain_code` 命名空间、领域边界、可见角色                                                                                              | 运行时领域缩域和权限入口                                                   |
| `Scene（业务场景）`                         | 业务场景    | `scene_code`、`scene_type`、主对象、默认时间、边界说明                                                                                   | 定义稳定业务问题                                                       |
| `Plan（方案资产）`                          | 方案资产    | `plan_code`、`route_preconditions（路由前置条件）`、`source_priority（来源优先级）`、`dedupe_strategy（去重策略）`                                | 定义受控可运行解法                                                      |
| `Output Contract（输出契约）`               | 输出契约    | `required_outputs`、`optional_outputs`、`masked_outputs`、`restricted_outputs`、`forbidden_outputs`                           | 定义正式承诺的输出视图                                                    |
| `Contract View（契约视图）`                 | 契约视图    | `view_code`、`role_scope`、`mask_type`、`required_role`、`approval_template`                                                  | 定义按角色、用途与审批上下文可见的字段级输出视图                                       |
| `Coverage Declaration（覆盖声明）`          | 覆盖声明    | `coverage_segments`、完整度、排除项、回退动作                                                                                          | 说明时间与来源覆盖边界                                                    |
| `Policy（策略对象）`                        | 策略对象    | 访问、脱敏、审批、导出、留痕规则                                                                                                          | 输出 `allow（允许）` / `need_approval（需要审批）` / `deny（拒绝）`            |
| `Input Slot Schema（输入槽位模式）`           | 输入槽位模式  | 槽位名、类型、必填性、标准化规则、澄清提示                                                                                                     | 约束运行时输入结构                                                      |
| `Time Semantic Selector（时间语义选择器）`     | 时间语义选择器 | 默认时间、可选时间、触发澄清词、解释优先级                                                                                                     | 解决多日期口径冲突                                                      |
| `Inference Rule（推理规则）`                | 推理规则    | `rule_code`、触发条件、适用边界、置信度策略、`derivation_rule_ref`                                                                         | 定义可执行推理规则的实例化绑定                                                |
| `Inference Assertion（推理结论）`           | 推理结论    | `assertion_id`、结论类型、主语、谓语、宾语 / 目标值、适用范围、复核状态                                                                              | 定义正式进入快照的推理事实                                                  |
| `Inference Chain（推理链）`                | 推理链     | `chain_id`、输入事实、规则路径、中间节点、解释摘要                                                                                            | 定义推理结论的解释链路                                                    |
| `Inference Confidence Record（推理置信记录）` | 推理置信记录  | `record_id`、置信分值、评分因子、阈值命中、复核结论                                                                                           | 定义置信度推理门禁                                                      |
| `Inference Scope Profile（推理适用边界）`     | 推理适用边界  | `scope_code`、业务域范围、时间范围、来源范围、禁止场景、`confidence_thresholds（置信阈值组）`、`gray_release_scope（灰度发布范围）`、`degrade_mode（降级模式）`        | 定义推理结论的适用边界、阈值治理和灰度 / 降级约束                                     |
| `Source Intake Contract（来源接入契约）`      | 来源接入契约  | 材料标题、主对象、结果字段、覆盖、敏感级别                                                                                                     | 约束材料可否入生产                                                      |
| `Source Contract（来源契约）`               | 来源契约    | 表 / 字段、快照周期、新鲜度、有效条件、`snapshot_freshness_probe（快照新鲜度探测）`                                                                  | 绑定真实来源及其可用性，并支持快照退避探测                                          |
| `Dictionary（字典）`                      | 字典      | `dict_code`、`dict_category`（`CODE_TRANSLATION` / `ENUM_DEFINITION` / `BUSINESS_MAPPING`）、条目列表、版本、引用方                      | 统一管理码值翻译、枚举定义和业务映射，防止同一码值在不同场景下出现不同解释                          |
| `Join Relation Object（表间关联关系对象）`      | 表间关联关系  | `join_code`、`source_table`、`target_table`、`join_keys`、`join_type`、`filter_conditions`、`business_semantics`、`evidence_ref` | 定义两张来源表之间的业务关联含义、标准连接键和推荐关联类型，为下游 `NL2SQL` / `PlanIR` 提供多表拼装基线 |
| `Evidence Fragment（证据片段）`             | 证据片段    | 类型、来源、锚点、指纹、确认状态                                                                                                          | 可回溯依据                                                          |
| `Version Snapshot（版本快照）`              | 版本快照    | `snapshot_id`、`version_tag`、状态、变更摘要                                                                                       | 发布切换与回滚锚点                                                      |
| `Gap Task（缺口任务）`                      | 缺口任务    | `gap_code`、阻断等级、责任人、截止状态                                                                                                  | 不完整材料治理                                                        |
| `Review Task（复核任务）`                   | 复核任务    | `review_code`、复核类型、处理人、结论                                                                                                 | 业务 / 技术 / 合规复核闭环                                               |
| `Audit Event（审计事件）`                   | 审计事件    | `trace_id`、`actor`、`action`、`asset_ref`、`decision`                                                                        | 审计与追踪                                                          |

### 5.1.0 `Candidate Entity Graph（候选实体图谱）` 作为导入期隔离层

`Candidate Entity Graph（候选实体图谱）` 不是正式受治理资产，也不是已发布主图中的稳定节点类型。它是知识生产期为“实体抽取 + 关系确认 + 证据挂载”提供的任务级隔离工作层，生命周期受 `material_id（材料标识）`、`task_id（任务标识）` 和复核状态约束。

1. 候选图必须与已发布主图隔离。候选节点和候选边不能直接进入运行时查询、发布快照、影响分析主路径或正式知识包输出。
2. 候选图的首轮固定节点类型为 10 类：`Candidate Scene（候选场景）`、`Candidate Plan（候选方案资产）`、`Candidate Evidence Fragment（候选证据片段）`、`Identifier（标识对象）`、`Field Concept（字段概念）`、`Business Term（业务术语）`、`Time Semantic（时间语义）`、`Source Table（来源表）`、`Column（字段）`、候选表间关联。
3. 候选图必须遵守“场景先归一、差异下沉到方案”的建模原则：同一业务问题下出现多个 `Step（步骤）`、方法分支、时段切换、历史表切换或来源路径差异时，默认只保留一个共同的 `Candidate Scene（候选场景）`，多个 `Candidate Plan（候选方案资产）` 通过 `SCENE_HAS_PLAN` 挂在该场景下。
4. 只有当主对象、适用范围、不适用范围或标准输出承诺发生实质变化时，才允许拆分为多个 `Candidate Scene（候选场景）`；来源表、过滤条件、时间语义、`SQL` 写法和中间路由差异，默认都属于 `Candidate Plan（候选方案资产）` 层差异。
5. 候选图的设计目标是“帮助确认正式对象”，不是“替代正式对象”。其中 `Candidate Scene` / `Candidate Plan` / `Candidate Evidence Fragment` 是最接近正式对象的治理候选层，其余语义与来源节点用于辅助确认、解释和后续落位。
6. 候选图节点的确认映射固定如下：`Candidate Scene -> Scene（业务场景）`、`Candidate Plan -> Plan（方案资产）`、`Candidate Evidence Fragment -> Evidence Fragment（证据片段）`、`Identifier -> EntityAlias（实体别名）` / `Input Slot Schema（输入槽位模式）`、`Field Concept -> Output Contract（输出契约）` / `Contract View（契约视图）`、`Time Semantic -> Time Semantic Selector（时间语义选择器）` / `Coverage Declaration（覆盖声明）`、`Source Table` / `Column` / 候选表间关联 -> `Source Contract（来源契约）` / `Join Relation Object（表间关联关系对象）`。
7. 候选图中的高风险对象不得自动转正，至少包括：标识归一冲突、时间语义冲突、来源表不一致、关联键不一致、候选关系置信度未达阈值等情况。
8. 候选图允许参考 `mirofish` 的 `ontology -> graph build -> entity readback` 流程，但实体类型必须适配本项目图谱，不复用跨域通用的 `Person（个人）`、`Organization（组织）` 等社媒模拟本体。
9. 候选图在导入期优先通过 `Import Preprocess Stream API（导入预处理事件流接口）` 进行增量回传，事件流固定为 `start`、`stage`、`graph_patch`、`draft`、`done`、`error` 六类事件；其中 `graph_patch` 专门服务“导入中活图谱”，不得与阶段进度或草稿状态混用。
10. `graph_patch` 必须表达“增量补丁”而不是“整图快照”，至少包含 `patchSeq（补丁序号）`、`stageKey（阶段键）`、`chunkIndex（分块序号）`、`chunkTotal（分块总数）`、`addedNodes（新增节点列表）`、`updatedNodes（更新节点列表）`、`addedEdges（新增边列表）`、`updatedEdges（更新边列表）`、`focusNodeIds（当前焦点节点列表）` 和 `summary（本批次摘要）`。
11. 候选图导入中不做硬删除；若发生实体归一、别名合并或关系修正，旧节点 / 旧边先以 `merged（已合并）`、`superseded（被替代）`、`downgraded（降级待确认）` 等状态弱化显示，最终由 `done` 事件或后续正式确认动作完成收敛。
12. 前端消费 `graph_patch` 时，只允许局部补丁更新和局部重排；最终 `done` 事件返回的完成态结果用于校准与补齐，不得反向覆盖掉用户在导入过程中的当前焦点和已展开详情。

### 5.1.1 `Contract View（契约视图）` 作为一等受治理资产

`Contract View（契约视图）` 不是 `Output Contract（输出契约）` 的派生备注，而是正式受治理资产。它必须具备独立逻辑标识、版本标识、角色可见范围、字段裁剪规则、脱敏方案、审批模板和发布状态；发布门禁、审计回溯、缓存失效和影响分析都必须能直接追踪到具体 `view_code`。

### 5.1.2 `Dictionary（字典）` 作为一等受治理资产

`Dictionary（字典）` 不是语义层的附属概念，而是正式受治理资产。口径文档中大量散落的码值翻译（如交易代码 FD01-FD98 的 30 余种中文翻译、渠道代码 TEL/DSK/MPH 的 12 种翻译、卡片等级 010/020/040/060/080 的 5 种翻译等）如果不被统一治理，不同 `Plan（方案资产）` 可能对同一码值给出不同中文解释，导致口径不一致。

1. 每个 `Dictionary` 必须具备独立的 `dict_code`、版本标识和发布状态。
2. `dict_category` 按用途分为三类：`CODE_TRANSLATION`（码值翻译，如交易代码到中文名称）、`ENUM_DEFINITION`（枚举定义，如卡片等级到业务含义）、`BUSINESS_MAPPING`（业务映射，如来源系统到标准名称）。
3. `Output Contract（输出契约）` 的 `required_outputs` 中凡涉及码值字段，必须引用已发布的 `Dictionary` 资产，禁止在 SQL 注释或口头约定中定义翻译规则。
4. 发布检查中要求每个 `Dictionary` 至少被一个 `Output Contract` 或 `Derivation Rule（派生规则）` 引用，避免产生孤儿字典。

### 5.1.3 `Join Relation Object（表间关联关系对象）` 作为一等受治理资产

`Join Relation Object（表间关联关系对象）` 定义两张来源表之间的业务关联含义和标准连接键。口径文档中实际查询普遍涉及多表关联（如断卡排查涉及 8 张表的关联链），纯图谱语义检索无法完整描述这些物理层面的表间关系。

1. 每个 `Join Relation Object` 必须具备独立的 `join_code`、版本标识和发布状态。
2. 关键字段包括：`source_table`（源表）、`target_table`（目标表）、`join_keys`（连接键列表）、`join_type`（推荐关联类型，如 `INNER` / `LEFT` / `FULL`）、`filter_conditions`（关联时的固定过滤条件，如 `FRZ_CD = 'X19'`）、`business_semantics`（中文业务连接含义，如"通过客户 UID 关联户口信息"）和 `evidence_ref`（证据引用）。
3. `join_type` 和 `filter_conditions` 是本系统为下游 `NL2SQL（自然语言转 SQL）` 和 `PlanIR（计划中间表示）` 提供的**受治理推荐元数据**。本系统自身不直接拼接和执行 SQL，但有责任把口径文档中沉淀的关联知识（包括关联类型和过滤条件）结构化输出，使下游消费方能据此生成正确的 SQL，而不是自行猜测。

### 5.2 关键关系与基数约束

| 关系 | 约束 | 设计意义 |
| --- | --- | --- |
| `Domain（业务领域） 1:N Scene（业务场景）` | 一个领域包含多个场景；一个场景只能归属一个主领域 | 避免跨域重复挂载导致越界 |
| `Scene（业务场景） 1:N Plan（方案资产）` | 一个场景至少一个 `Plan（方案资产）`；历史路径、批次路径、高敏路径应拆独立 `Plan（方案资产）` | 将变化留在 `Plan（方案资产）`，不污染 `Scene（业务场景）` 稳定定义 |
| `Plan（方案资产） 1:N Contract View（契约视图）` | 同一 `Plan（方案资产）` 可按角色 / 用途输出多个契约视图 | 保证字段级裁剪可配置 |
| `Plan（方案资产） 1:N Coverage Segment（覆盖分段）` | 一个 `Plan（方案资产）` 可有多个时间 / 产品 / 来源覆盖分段 | 支持历史切换与部分覆盖说明 |
| `Plan（方案资产） 1:1..N Policy（策略对象）` | 一个 `Plan（方案资产）` 至少绑定一条有效策略，可按角色扩展为多条 | 高敏路径必须有专门策略 |
| `Derivation Rule（派生规则） 1:N Inference Rule（推理规则）` | 一条语义规则可实例化为多条推理规则；推理规则必须回链语义规则 | 防止规则语义与推理实现双写漂移 |
| `Inference Rule（推理规则） 1:N Inference Assertion（推理结论）` | 一条推理规则可产生多条已发布推理结论 | 固化“规则定义”和“规则运行结果”的分层 |
| `Inference Assertion（推理结论） 1:1..N Inference Chain（推理链）` | 每条正式推理结论至少绑定一条可解释推理链 | 满足审计、复核和运行解释要求 |
| `Inference Assertion（推理结论） 1:0..N Inference Confidence Record（推理置信记录）` | 只有置信度推理需要绑定置信记录 | 将置信门禁限定在需要评分的推理类型上 |
| `Scene（业务场景）` / `Plan（方案资产）` N:N `Evidence Fragment（证据片段）` | 同一证据片段可支撑多个对象 | 避免证据重复存储 |
| `Plan（方案资产） 1:N Join Relation Object（表间关联关系对象）` | 一个 `Plan（方案资产）` 可引用多个表间关联关系 | 支持多表查询场景的关联知识结构化 |
| `Output Contract（输出契约）` / `Derivation Rule（派生规则）` N:N `Dictionary（字典）` | 同一字典可被多个契约或规则引用 | 统一码值翻译口径，避免双写漂移 |
| `Published Snapshot（已发布快照） 1:N Runtime Read（运行时读取）` | 运行时只读取当前有效快照 | 避免读取中间态 |

### 5.3 主键、命名与唯一性规则

1. `Scene（业务场景）`、`Plan（方案资产）`、`Output Contract（输出契约）`、`Contract View（契约视图）`、`Coverage Declaration（覆盖声明）`、`Policy（策略对象）`、`Inference Rule（推理规则）`、`Inference Assertion（推理结论）`、`Inference Chain（推理链）`、`Source Contract（来源契约）`、`Version Snapshot（版本快照）` 均必须有稳定逻辑主键和不可变物理版本标识，禁止仅靠名称做唯一性约束。
2. 命名建议采用“业务域_场景_语义”风格，例如 `SC_RTL_PROFILE_BASIC`、`PL_PAYROLL_DETAIL_HISTORY`；同一类资产命名规则必须统一。
3. 已发布资产的逻辑主键不可复用到语义不同对象；语义发生实质变化时应新建新对象，并用 `supersede（替代关系）` 衔接。
4. 运行时 `trace_id（追踪编号）` 与 `snapshot_id（快照标识）` 必须在所有 API、审批记录和审计事件中贯通。

### 5.4 生命周期与状态机

| 对象 | 状态流 | 说明 |
| --- | --- | --- |
| 资产状态 | `DRAFT（草稿） -> REVIEWED（已复核） -> PUBLISHED（已发布） -> RETIRED（已退役）` | 核心资产统一状态机；`REJECTED（已驳回）` 作为复核结果而非长期运行态 |
| 导入任务状态 | `CREATED -> VALIDATED -> PARSED -> MODELLED -> ALIGNED -> READY_FOR_REVIEW -> CLOSED` | 材料接入闭环；任务只承载流程状态，不替代来源材料与正式治理资产本身 |
| 发布任务状态 | `PENDING -> CHECKING -> APPROVED / BLOCKED -> GRAYING -> SWITCHED / ROLLED_BACK -> ARCHIVED` | 发布门禁、灰度切换与回滚闭环 |
| 审批任务状态 | `SUBMITTED -> PRECHECKED -> APPROVED / REJECTED -> EXECUTED -> ARCHIVED` | 高敏导出与专审闭环 |
| 缺口任务状态 | `OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED / WAIVED -> VERIFIED` | 不完整材料治理闭环 |

### 5.5 发布与快照切换原则

1. 运行面只读取 `PUBLISHED（已发布）` 快照，不读取 `DRAFT（草稿）`、复核中或切换中的对象。
2. 一次发布切换必须以 `snapshot_id（快照标识）` 为原子边界：控制资产、图谱关系、缓存索引和接口版本号要么一起切换，要么全部不切换。
3. 若缓存或向量索引刷新失败，发布任务状态保持 `BLOCKED` 或“部分不可见”，禁止对外生效。
4. 任何回滚都以最近一个稳定 `snapshot_id（快照标识）` / `inference_snapshot_id（推理快照标识）` 绑定对为单位，不能只回滚其中一部分资产。
5. `snapshot_id（快照标识）` 与 `inference_snapshot_id（推理快照标识）` 必须强绑定；运行服务不得使用控制资产和推理资产的跨版本组合。
6. 灰度发布必须绑定成对快照和明确灰度范围；未命中灰度范围的请求继续读取上一稳定快照对。
7. 灰度期间若误路由率、推理降级率、高敏误放行风险或关键 `API（应用程序接口，Application Programming Interface）` 异常超阈值，发布任务状态应自动转 `BLOCKED（阻断）` 或冻结，并按成对快照回滚。

## 6. 核心业务流程设计

完整系统设计不仅要说明“有哪些对象”，还要说明“这些对象如何被生产、发布、使用和回退”。本章给出四条必须闭环的核心流程：材料接入与发布、运行时查询、审批与导出、漂移与回退。

### 6.1 材料接入与发布流程

1. 材料登记：填写 `Source Intake Contract（来源接入契约）`，校验主对象、结果字段、默认时间、覆盖、敏感级别等关键必填项。
2. 解析抽取：文档、`SQL（结构化查询语言，Structured Query Language）`、工单双路解析，先生成适配本项目图谱的候选本体，再按 `material_id（材料标识）` / `task_id（任务标识）` 构建隔离的 `Candidate Entity Graph（候选实体图谱）`，产出候选场景、候选方案、标识对象、字段概念、时间语义、来源表 / 字段、候选表间关联、证据与缺口标记；导入期通过 `Import Preprocess Stream API（导入预处理事件流接口）` 以阶段事件和 `Graph Patch（图谱增量补丁）` 回传实时进度与活图谱状态。
3. 资产建模：根据解析结果，建立或更新 `Scene（业务场景）`、`Plan（方案资产）`、`Output Contract（输出契约）`、`Contract View（契约视图）`、`Coverage Declaration（覆盖声明）`、`Policy（策略对象）`、`Inference Rule（推理规则）`、`Inference Scope Profile（推理适用边界）`（含 `confidence_thresholds（置信阈值组）`、`gray_release_scope（灰度发布范围）`、`degrade_mode（降级模式）`）、`Evidence Fragment（证据片段）` 等逻辑资产。
4. 推理构建：由 `Inference Build Service（推理构建服务）` 执行候选三元组收敛、确定性推理、置信度推理、冲突检测、推理链生成和候选结论归档，产出待发布的 `Inference Assertion（推理结论）`、`Inference Chain（推理链）` 与 `Inference Confidence Record（推理置信记录）`。
5. 元数据对齐：以资产建模阶段填写的逻辑输出字段和来源线索为输入，去底层物理库对齐表、字段、快照周期与可用性；若对齐失败或缺字段，则生成 `Gap Task（缺口任务）` 阻断发布并强制退回修改模型；若对齐成功，则固化为 `Source Contract（来源契约）` 并与 `Plan` 绑定。
6. 复核：分别进入业务、技术、合规复核；推理冲突或置信度未达阈值的结论转人工复核；`Gap Task（缺口任务）` 同步建立。
7. 治理规则评估：`Import Confirm（导入确认）` 后必须先执行 `IMPORT_CONFIRM` 阶段显式治理规则评估，只允许创建 `Scene Draft（场景草稿）` 与 `Gap Task（缺口任务）`，不允许隐式补齐正式治理对象；当前首轮固定覆盖 `Dictionary（字典）`、`Identifier Lineage（标识链）` 与 `Time Semantic Selector（时间语义选择器）` 三类完整性规则。
8. 发布检查：执行 `Publish Check API（发布检查接口）`，在原有控制资产检查之外，复用 `PRE_PUBLISH` 阶段治理规则评估结果与未关闭阻断级缺口摘要，校验契约可兑现、覆盖完整、策略可执行、推理资产可回溯、证据可回溯、元数据可落位与治理对象完整性。
9. 灰度准备：生成新 `snapshot_id（快照标识）` 与唯一 `inference_snapshot_id（推理快照标识）`，刷新缓存和索引，并按 `gray_release_scope（灰度发布范围）` 准备灰度生效窗口。
10. 灰度切换：仅对命中灰度范围的请求暴露新快照对，持续比较新旧路由、命中、降级和风险指标。
11. 全量切换与归档：灰度达标后对运行面全量可见；若不达标则按快照对回滚，并写入变更摘要、差异、审批结论、责任人、`trace_id（追踪编号）`。

### 6.1.1 `Source Intake Contract（来源接入契约）` 与 `Source Contract（来源契约）` 的衔接原则

1. `Source Intake Contract（来源接入契约）` 承载的是材料进入知识生产链路时的声明性信息，重点记录业务侧已知的来源表线索、结果字段、默认时间、覆盖说明、敏感级别和材料出处。
2. `Source Contract（来源契约）` 承载的是经过元数据对齐、规则校验和复核确认后的可运行来源约束，重点落为正式来源表 / 字段、快照周期、新鲜度、有效条件和运行时可用性判断。
3. 两者之间不是简单复制关系。`Source Intake Contract（来源接入契约）` 中能被验证的声明项，才进入 `Source Contract（来源契约）`；无法验证、彼此冲突或缺少依据的部分，必须转入 `Gap Task（缺口任务）` 或复核阻断项。
4. 运行时只把 `Source Contract（来源契约）` 作为来源可信性的正式依据；`Source Intake Contract（来源接入契约）` 继续保留其接入声明、材料来源和追溯价值，用于解释“这条来源约束最初是如何进入生产链路的”。
5. 为保证首轮真实库链路可追溯，`Source Intake Contract（来源接入契约）` 必须始终可回到独立的 `Source Material（来源材料）` 与 `Import Task（导入任务）`；来源材料、材料附件引用、材料指纹和导入任务不得再被简化为场景草稿上的临时字段。

### 6.2 运行时查询与知识包生成流程

| 步骤 | 阶段 | 关键动作 |
| --- | --- | --- |
| 1 | `Query Rewrite（查询改写）` + `Slot Filling（槽位补齐）` | 抽取标识、时间、产品范围、期望输出、用途、导出意图，并把 `LLM（大语言模型）` 结果限制为临时 `rewrite_hint（改写提示）` / `slot_hint（槽位提示）` |
| 2 | `Inference Runtime（运行推理）` | 基于当前 `inference_snapshot_id（推理快照标识）` 和已发布阈值配置执行标识收敛、时间语义收敛、关系补全和候选路径收敛，输出标准化推理断言 |
| 3 | `Scene Recall（场景召回）` | 按领域、主对象、`Scene Type（场景类型）`、术语命中与推理断言召回候选场景 |
| 4 | `Plan Selection（方案选择）` | 按 `Input Slot Schema（输入槽位模式）` 兼容性、`Source Contract（来源契约）`、主对象限制与推理断言初筛候选 `Plan（方案资产）` |
| 5 | `Coverage Engine（覆盖引擎）` | 对 `Coverage Declaration（覆盖声明）` 和 `Coverage Segment（覆盖分段）` 做命中判定、完整度计算和回退动作选择 |
| 6 | `Path Resolution（路径解析）` | 优先走确定性模板；模板不足时，在推理断言给出的合格范围内做受限图查询 |
| 7 | `Policy Decision（策略决策）` | 输出 `allow（允许）` / `need_approval（需要审批）` / `deny（拒绝）`，并绑定可见契约视图 |
| 8 | `Knowledge Package Build（知识包构建）` | 组装 `scene`、`plan`、`contract`、`coverage`、`policy`、`path`、`inference`、`evidence`、`risk`、`trace` |
| 9 | `Return（返回）` | 返回 `Knowledge Package（知识包）` 或 `Scene Discovery Result（场景发现结果）`，不返回裸表或裸字段 |

### 6.2.1 复合请求当前阶段处理策略

1. 当前阶段正式支持的标准输入是“单一主业务目标”请求，即一次请求最终只产出一个 `Knowledge Package（知识包）`。
2. `Query Rewrite（查询改写）` 与 `Slot Filling（槽位补齐）` 阶段必须识别复合请求；一旦识别出多个主对象、多个时间语义、多个风险目的或多个互不兼容的输出目标，不直接进入单一路径生成。
3. 只有当多个子意图最终能收敛到同一 `Scene（业务场景）`、同一 `Plan（方案资产）` 家族、同一 `Contract View（契约视图）` 和同一覆盖 / 策略边界时，才允许被归并为一次标准请求继续处理。
4. 若子意图落入不同 `Scene（业务场景）`、不同 `Plan（方案资产）`、不同时间语义或不同风险边界，系统必须返回 `Knowledge Decomposition Result（知识拆解结果）`，至少包含 `sub_questions`、`scene_candidates`、`plan_candidates`、`merge_hints`、`clarification_questions`，并要求上游交互层拆分后分别处理。
5. 当前阶段不支持一次请求同时产出多个 `Knowledge Package（知识包）`，也不支持跨场景、跨方案的自动合并结果输出。

### 6.2.2 `LLM（大语言模型）` 隔离与运行时降级规则

1. `Query Rewrite（查询改写）` 与 `Slot Filling（槽位补齐）` 阶段允许调用 `LLM（大语言模型）`，但其产出默认归类为临时提示，不具备正式事实地位。
2. 临时提示只有在被已发布 `Inference Assertion（推理结论）`、`Inference Rule（推理规则）`、`Source Contract（来源契约）` 或显式校验规则再次验证后，才能影响场景召回、方案选择或路径解析；否则只允许进入澄清问题。
3. `Inference Runtime（运行推理）` 超时、推理断言缺失、阈值配置不可读或依赖服务熔断时，必须依次退化到 `deterministic_only（仅确定性推理）`、`template_only（仅模板路径）`、`clarification_only（仅澄清返回）`。
4. 降级时必须返回结构化原因编码；若已经降级到 `clarification_only（仅澄清返回）` 仍不能形成受控结果，则返回受控拒绝或知识拆解结果，不返回未经验证的推理事实。

### 6.3 审批与导出流程

1. 当请求包含 `need_export（导出诉求标记） = true` 且命中 `S2` 以上字段，或场景类型为 `AUDIT_LOG` / `WATCHLIST_CONTROL` 时，运行面只返回 `need_approval（需要审批）`，不直接返回完整字段。
2. 审批单需携带 `trace_id（追踪编号）`、`scene_code`、`plan_code`、`requested_fields`、`purpose`、`operator_role`、`effective_time_range`、`masking_plan`。
3. 审批通过后，由导出执行器按审批单生成一次性导出任务；导出文件需绑定审批号、操作者、导出时间并进入审计台账。
4. 审批被拒绝或超时，查询结果保持 `deny（拒绝）` 或 `need_approval（需要审批）`，不得回落为普通 `allow（允许）`。

### 6.4 元数据漂移、失败回退与回滚流程

1. 元数据平台不可用：本批次元数据对齐暂停；相关候选对象留在 `DRAFT（草稿）`，不得自动发布。
2. 语义表示生成服务不可用：语义层退化为关键词与精确匹配；无精确命中时不做自动合并，仅进入待复核队列。
3. 图查询或图写入超时：写入事务全回滚；读取返回 `GRAPH_TIMEOUT`，不返回过期缓存。
4. `Inference Runtime（运行推理）` 超时、推理断言索引失效或阈值配置不可读：优先按降级契约退化到 `deterministic_only（仅确定性推理）` 或 `template_only（仅模板路径）`；若仍无法形成合格结果，则返回 `INFERENCE_ASSERTION_UNAVAILABLE` 或受控澄清结果。
5. `LLM（大语言模型）` 服务不可用或异常漂移：运行面退回规则抽取、关键词归一和澄清问题路径；`LLM（大语言模型）` 输出不得以缓存旁路方式回灌正式推理层。
6. 检测到 `Source Contract（来源契约）` 绑定表 / 字段失效时，必须按“字段 -> 关系 -> `Plan（方案资产）` -> `Scene（业务场景）` 契约影响”顺序级联评估；一旦 `required_outputs` 不可落位，发布结论直接转为阻断或运行时转 `deny（拒绝）`。

### 6.5 并发与一致性原则

1. 同一 `Scene（业务场景）` 下、同一类核心资产的发布任务串行执行，避免多任务并发覆盖。
2. 写入必须是单事务原子单元：资产、关系、证据挂载、快照记录、缓存刷新指令要么全成，要么全回滚。
3. 查询只读取最新稳定快照；切换过程中的中间态对运行服务不可见。
4. 所有异步任务均需幂等键：`source_ingest_id（来源接入任务标识）`、`publish_job_id（发布任务标识）`、`approval_job_id（审批任务标识）`、`export_job_id（导出任务标识）`。

## 7. 控制资产详细设计

本章把运行时真正参与硬门禁的控制资产设计写实。所有决定“能否返回、返回什么、在哪些条件下返回”的对象，都不应只存在于说明文字里，而必须落到结构化字段和发布门禁里。

### 7.1 `Scene（业务场景）` 设计要点

1. `Scene（业务场景）` 只定义稳定业务问题，不直接承担具体来源切换、历史拼接和字段级裁剪。
2. `Scene（业务场景）` 必填字段包括：`scene_code`、`scene_name`、`scene_type`、`primary_object`、`default_time_semantic`、`applicable_scope`、`inapplicable_scope`、`owner`、`version_policy`。
3. `Scene（业务场景）` 拆分条件包括：主对象变化、输出粒度变化、默认时间语义变化且影响业务解释、风险等级升高到审计 / 名单核验，或契约 / 覆盖 / 策略差异过大。
4. `Scene（业务场景）` 可选挂接 `plan_exclusion_constraints`（方案互斥约束引用列表），用于声明同一场景下多个 `Plan（方案资产）` 之间的产品互斥关系。例如"SA 理财交易查询"场景下，"非朝朝宝 Plan"和"朝朝宝 Plan"必须通过产品码互斥。`Plan Selection Service（方案选择服务）` 在选择时统一校验互斥约束的完备性（不重叠、不遗漏）。互斥约束作为 `Scene` 级受治理资产管理，新增产品只需修改约束，不需改动 `Plan` 本体。

### 7.2 `Plan（方案资产）` 设计要点

| 项    | 定义                                                                                                                                                                |
| ---- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 主键   | `plan_code`                                                                                                                                                       |
| 关键字段 | `scene_code`、`route_preconditions（路由前置条件）`、`source_priority（来源优先级）`、`source_contract_refs`、`time_semantic_selector_ref`、`dedupe_strategy（去重策略）`、`fallback_action` |
| 发布要求 | 必须绑定至少一个有效 `Coverage Declaration（覆盖声明）`、`Policy（策略对象）`、`Evidence Fragment（证据片段）` 和 `Contract View（契约视图）`；必须能回到真实来源                                                |
| 典型拆分 | 当前明细 vs 历史明细；申请记录 vs 资金交易；普通画像 vs 高敏审计                                                                                                                            |

### 7.3 `Output Contract（输出契约）` 设计要点

| 字段类别 | 语义 |
| --- | --- |
| `required_outputs` | 最小可交付字段；一旦无法落位，`Plan（方案资产）` / `Scene（业务场景）` 不可发布或运行时直接 `deny（拒绝）` |
| `optional_outputs` | 可附带返回字段；无值不阻断 |
| `masked_outputs` | 可返回但默认脱敏；具体脱敏方式由 `Policy（策略对象）` 决定 |
| `restricted_outputs` | 只有特定角色或审批通过后可返回 |
| `forbidden_outputs` | 无论何种普通请求均不可直接返回，只能进入专审或不返回 |

### 7.4 `Coverage Declaration（覆盖声明）` 设计要点

| 字段 | 要求 |
| --- | --- |
| `coverage_segments` | 按时间、产品、对象或来源切段定义可覆盖范围 |
| `completeness_level` | 固定为 `FULL` / `PARTIAL` / `GAP` |
| `exclusions` | 明确不保证项，例如 2004 年前柜面代发不可查 |
| `fallback_action` | 固定为 `NONE` / `REQUIRE_APPROVAL` / `NEED_CLARIFICATION` / `DENY` |
| `source_priority` | 跨段或多源冲突时的可信度顺序 |
| `dedupe_strategy` | 跨段拼接时的主键、优先级与去重规则 |
| `source_composition` | 当同一 `Coverage Segment（覆盖分段）` 内存在多个来源表时，声明该段来自哪些 `Source Contract（来源契约）` 的并集、优先级合并与去重规则。例如代发明细 2004-2013 段涉及 `PKTRSLOGP`、`EPHISTRXP1/2`、`UNICORE_EPHISTRXP_YEAR` 等多张历史表，需在此字段明确各表的适用子区间和合并优先级 |
| `data_quality_note` | 对已知数据质量问题做结构化声明，固定枚举为 `POSSIBLY_INCOMPLETE`（可能缺数）/ `DISCONTINUOUS`（数据不连续）/ `OVERLAP_WITH_OTHER_SEGMENT`（与其他分段有重叠）/ `VENDOR_CONFIRMED`（来源方已确认完整）。禁止使用自由文本替代，确保运行时和治理视图可结构化消费 |

### 7.5 `Policy（策略对象）` 设计要点

| 项 | 说明 |
| --- | --- |
| 输入 | `scene_type`、`plan_code`、`requested_fields`、`role`、`purpose`、`need_export`、`approval_context`、`sensitivity_hits` |
| 输出 | `decision`、`effective_contract_view`、`masking_plan`、`approval_required`、`audit_requirements` |
| 三态语义 | `allow（允许）` / `need_approval（需要审批）` / `deny（拒绝）` |
| 强约束 | 高敏日志与强命中名单类对象默认不进入普通 `allow（允许）` |

### 7.6 `Input Slot Schema（输入槽位模式）` 与标识归一

| 槽位类别 | 典型字段 | 作用 |
| --- | --- | --- |
| 标识槽位 | `cust_id`、`cust_uid`、`eac_nbr`、`eac_seq_nbr`、`cert_no`、`protocol_nbr`、`agn_bch_seq`、`product_id` | 决定入口实体和主查询对象 |
| 时间槽位 | `default_time_range`、`explicit_time_semantic`、`as_of_time`、`coverage_request` | 决定默认时间和是否切换历史 `Plan（方案资产）` |
| 输出槽位 | `required_fields`、`optional_fields`、`need_export` | 决定契约是否满足和是否触发审批 |
| 风险槽位 | `purpose`、`role`、`approval_context`、`high_sensitivity_request` | 决定策略三态输出 |

样例驱动要求如下：工单样例表明同一用户识别链可能经过 `ACCOUNT_NUMBER`、`CUSTOMERID`、`CUST_UID`、`ID_NO` 等多种入口，因此标识槽位必须先做标准化与优先级判断，再进入 `Scene Recall（场景召回）`，不能把所有入口等价丢给图检索。

每个 `Input Slot Schema（输入槽位模式）` 必须挂接 `identifier_resolution_strategy`（标识收敛策略），定义标识尝试的有序优先队列（如 `[EAC_ID, CUST_UID, CUST_ID, DOC_NBR]`）和匹配失败的回退动作（`CLARIFICATION` / `DENY`）。该策略随 `Input Slot Schema` 一同进入版本快照发布，不可在运行时动态修改优先级。工单样例（断卡排查场景中 4 路 `UNION` 逐一尝试匹配的模式）表明：这不是静态映射关系，而是运行时试探与收敛策略，必须被正式治理。

### 7.6.1 `Identifier Lineage（标识谱系）` 最小数据模型

| 字段 | 最小要求 |
| --- | --- |
| 谱系编码 | 具备稳定主键，用于标识一条可治理的标识映射关系 |
| 源标识类型 | 明确来源标识类别，例如客户号、户口号、证件号、协议号、批次号 |
| 目标标识类型 | 明确映射后的目标标识类别，不允许使用模糊自由文本代替 |
| 映射方向 | 说明是单向派生、双向可回溯，还是历史分段条件下的条件映射 |
| 规则引用 | 必须能回到具体 `Derivation Rule（派生规则）`、人工确认规则或既有口径说明 |
| 有效范围 | 至少声明适用时间范围、适用系统范围和失效条件 |
| 来源约束 | 必须挂接相关 `Source Contract（来源契约）` 或来源系统约束，说明映射成立的前提 |
| 证据引用 | 必须具备 `Evidence Fragment（证据片段）` 支撑，缺证据不得发布 |
| 置信与状态 | 必须标明当前可信度、复核状态和是否允许进入 `PUBLISHED（已发布）` 快照 |

当前阶段对 `Identifier Lineage（标识谱系）` 的治理要求是：先支持稳定、可审计、可回溯的显式映射关系，不支持把来源不明的经验性映射直接当作运行时默认规则。

### 7.7 `Time Semantic Selector（时间语义选择器）`

1. 每个存在多时间语义冲突的 `Scene（业务场景）` 或 `Plan（方案资产）` 都必须挂接 `Time Semantic Selector（时间语义选择器）`。
2. 选择器至少维护：默认时间、备选时间、触发澄清关键词、解释文案、超范围时回退动作。
3. 基金场景至少区分申请日期、委托日期、资金日期、份额日期；零售快照类场景至少区分快照日期与业务发生日期；工单追溯类场景需支持 `as_of_time（截止时点）`。

## 8. 数据与存储设计

本系统不是单一数据库工程，而是多种存储协同的系统设计。图数据库用于关系表达与寻路，关系型数据库用于控制资产和任务治理，对象存储用于原文与附件，向量索引用于语义召回，事件与日志用于审计和运维。

### 8.1 存储分层与落位

| 存储 | 承载对象 | 主要用途 |
| --- | --- | --- |
| 图存储 | `Scene（业务场景）`、语义对象、推理断言关系、元数据关系、`Evidence Fragment（证据片段）` / `Version Snapshot（版本快照）` 关系、路径模板引用 | 图遍历、依赖分析、推理链回放、证据追溯 |
| 关系型控制库 | `Plan（方案资产）`、`Output Contract（输出契约）`、`Contract View（契约视图）`、`Coverage Declaration（覆盖声明）`、`Policy（策略对象）`、`Inference Rule（推理规则）`、`Inference Scope Profile（推理适用边界）`、`Source Contract（来源契约）`、`Canonical Entity（统一实体）`、`Canonical Membership（统一成员归属）`、`Canonical Relation（统一实体关系）`、任务台账、`SLA（服务级别协议，Service Level Agreement）` 配置 | 强结构治理、幂等、事务 |
| 向量索引 | 术语、场景摘要、历史问法、说明文本、证据摘要 | 语义召回 |
| 对象存储 | 原始文档、`SQL（结构化查询语言，Structured Query Language）`、截图、导出附件、审计文件 | 原文留存与低成本归档 |

知识生产期的 `Candidate Entity Graph（候选实体图谱）` 可复用图存储能力，但必须使用独立任务级命名空间或隔离 `graph_id`，禁止与已发布主图、运行时快照和正式推理图混存。

| 事件 / 日志库 | 发布事件、审批事件、接口调用、告警事件、审计明细 | 监控与审计 |

### 8.1.1 `Neo4j（图数据库产品，Neo4j）` Phase 1 提前接入边界

当前为了提前接入图数据库、又不打破首轮“关系型控制库是真源”的收口边界，Phase 1 固定采用“`Neo4j` 作为已发布快照图读模型”的方案，而不是把图数据库直接升级为正式主数据真源。该阶段的目标是优先把最适合图存储的浏览、拓扑和影响分析链路切过去，不把治理门禁、运行判定和知识包主链路一起迁移。

1. `MySQL（关系型数据库，MySQL）` 继续承载正式治理资产、发布快照与运行判定所需的真源对象；`Neo4j` 只承接 `PUBLISHED（已发布）` 快照子图的只读投影，不写草稿态、候选态或未复核对象。
2. Phase 1 图读模型只服务 `Data Map Graph API（数据地图图谱接口）` 与 `Impact Analysis API（影响分析接口）` 两条读链路；`Plan Select API（方案选择接口）`、`Knowledge Package API（知识包接口）`、`Coverage Engine（覆盖引擎）`、`Policy Decision Service（策略决策服务）` 继续走关系型主路径。
3. 图读模型的最小对象集固定为：`Domain（业务领域）`、`Version Snapshot（版本快照）`、`Scene（业务场景）`、`Plan（方案资产）`、`Path Template（路径模板）`、`Contract View（契约视图）`、`Output Contract（输出契约）`、`Coverage Declaration（覆盖声明）`、`Policy（策略对象）`、`Evidence Fragment（证据片段）`、`Source Contract（来源契约）`、`Source Intake Contract（来源接入契约）`；对象关系与数据地图现有画布拓扑保持一一对应，不借 Phase 1 顺手扩展新的运行期推理主链路对象。
4. 发布完成后必须以 `scene_id（场景标识） + snapshot_id（快照标识）` 为单位同步执行图投影和发布时一致性校验；若投影失败、校验失败或版本不匹配，`MySQL` 中的发布结果仍保持有效，但该快照不得进入图读路径，数据地图必须自动退回关系库聚合结果，且不得展示旧版图快照伪装成最新结果。
5. Phase 1 允许 `Node Detail（节点详情）`、覆盖检查、策略解释等属性型内容继续回读关系型控制库补充细节，不要求把审计全文、说明文本和字段级裁剪全文冗余写入图节点；图侧重点承接节点、关系、版本锚点和遍历读路径。
6. Phase 1 不引入独立图同步队列、双真源补偿事务或图侧反向修复写回；图侧发现不一致时只能报警、挂校验失败并触发读侧降级，不允许反向改写关系型真源。

### 8.1.2 跨场景统一实体层

为了解决“同一物理表、同一策略语义、同一证据出处、同一输出契约在不同场景中重复建模但互不相认”的问题，控制库必须新增跨场景统一实体真源层，而不是继续依赖场景间临时横向连边。

1. 统一实体层固定落在 `MySQL` 控制库中，属于正式治理真源的一部分；`Neo4j` 只能投影统一实体在某个已发布快照中的可见结果，不得反向承载统一实体主数据。
2. 统一实体层的最小对象集固定为：`Canonical Entity（统一实体）`、`Canonical Membership（统一成员归属）`、`Canonical Relation（统一实体关系）`、`Canonical Resolution Audit（统一解析审计记录）`。首轮统一实体类型固定覆盖 `Source Contract（来源契约）`、`Policy（策略对象）`、`Evidence（证据出处）`、`Output Contract（输出契约）` 四类正式对象。
3. 统一实体是长期稳定身份，不随单次发布复制。发布动作冻结的是“哪些场景资产实例在本次 `snapshot_id（快照标识）` 下属于哪些统一实体，以及哪些统一实体关系在当前快照中可见”的结果，而不是复制新的统一实体副本。
4. 场景资产实例仍然保留场景内语义、编辑历史、审核记录和快照绑定，是运行时最终执行对象。统一实体层只负责共享身份、领域级连接骨架、跨场景解释和检索辅助，不直接替代 `scene_id + snapshot_id + plan_id` 这类正式运行边界。
5. 场景正式资产首轮范围固定为：`Scene（业务场景）`、`Plan（方案资产）`、`Coverage Declaration（覆盖声明）`、`Source Contract（来源契约）`、`Policy（策略对象）`、`Evidence Fragment（证据片段）`、`Output Contract（输出契约）`、`Contract View（契约视图）`。统一实体层复用这些正式资产的稳定锚点，不得在实现中把“统一实体首轮范围”误替换为“正式场景资产首轮范围”。
6. 场景资产实例的稳定锚点固定采用 `scene_asset_ref = scene-asset:{scene_id}:{asset_type}:{asset_id}`。同一正式资产在接口、发布冻结和图投影中的主语标识必须保持一致，不允许不同层各自拼接临时引用。
7. 四类对象的 `canonical_key（统一键）` 必须遵循“显式业务键优先，物理身份次之，结构化来源再次，指纹只做建议”的统一规则：`Source Contract` 以 `sourceSystem + normalizedPhysicalTable` 为正式锚点；`Policy` 依赖显式 `policySemanticKey（策略语义键）`；`Evidence` 依赖 `originType + originRef + originLocator（证据出处定位键）`；`Output Contract` 依赖显式 `contractSemanticKey（输出契约语义键）`。场景派生 `code` 不得继续充当全局共享身份。
8. 自动归并强度必须分层：`Source Contract` 可按正式物理锚点自动归并；`Policy / Evidence / Output Contract` 在缺少显式统一键时只能进入 `NEEDS_REVIEW（待复核）`，不得自动强归并。所有自动判断都必须保留 `match_basis（命中依据）`、`confidence_score（置信度）`、`resolution_rule_version（解析规则版本）` 供发布、审计和回放使用。
9. `SCENE_MEMBERSHIP（场景成员归属）` 在领域级图中的来源必须固定为“已发布 `scene_id + snapshot_id` 对应的场景资产成员集”，不得从当前可变场景内容现算；否则禁止返回 `root_type=DOMAIN` 的聚合结果。
10. `CANONICAL_RELATION（统一实体关系）` 首轮端点固定为 `Canonical Entity -> Canonical Entity`。`Scene Asset（场景资产实例） -> Canonical Entity（统一实体）` 只能使用 `INSTANCE_OF（实例归属）`，不得伪装成统一实体关系。
11. `Runtime Retrieval（运行检索）` 可复用统一实体层做跨场景缩域、共享对象扩展和命中解释，但最终知识包仍然必须回落到某个正式已发布场景实例集合；`Impact Analysis（影响分析）` 则允许先在统一实体层扩散，再映射回受影响的场景成员实例与快照。
12. “跨场景统一实体层与领域级图谱融合”作为当前首个 `Depth-First Working（纵深优先执行）` 试点闭环，固定采用六层边界推进：`Types（类型与契约层）` 只定义统一实体、成员归属、冻结归属和领域级返回契约；`Config（规则与路由层）` 只维护归并策略、门禁开关与固定角色智能体路由；`Repo（持久化访问层）` 只负责 `MySQL` / `Neo4j` 的正式读写，不承载归并决策；`Service（领域服务层）` 负责统一实体解析、成员归属维护、发布冻结和领域级聚合；`Runtime（运行时消费层）` 只消费已发布、已冻结的统一身份结果；`UI（前端表达层）` 只渲染正式 `DTO（数据传输对象，Data Transfer Object）`，不得自行拼装统一实体真相。
13. 六层边界属于首轮开发硬约束：`Runtime` 不得直接读取控制库中的草稿态统一实体；`Publish（发布）` 不得复制统一实体本体；`UI` 不得绕过接口自行推断成员归属；`Service` 不得绕过 `Repo` 直接散落 `SQL（结构化查询语言，Structured Query Language）` 或 `Cypher（图查询语言）`。任一边界被打破时，必须通过脚本、测试或 `CI（持续集成，Continuous Integration）` 门禁显式拒绝，而不是依赖文档提醒。

### 8.2 索引、缓存与刷新规则

1. `Scene Recall（场景召回）` 需要维护术语、场景名、别名、样例问法、主对象、领域与 `Scene Type（场景类型）` 的倒排与向量索引。
2. `Inference Runtime（运行推理）` 需要维护推理断言类型、主对象、适用边界、置信分值、`inference_snapshot_id（推理快照标识）` 和关键证据的索引。
3. `Plan Selection（方案选择）` 需要维护 `plan_code`、`scene_code`、状态、`time_semantic`、`coverage_range`、`role_scope`、`sensitivity_scope` 的关系型索引。
4. `Knowledge Package（知识包）` 构建可使用只读缓存，但缓存必须同时绑定 `snapshot_id（快照标识）` 与 `inference_snapshot_id（推理快照标识）`；当任一版本变化时必须整体失效或差量刷新。
5. 禁止使用与快照版本无关的全局缓存，避免出现“查询使用旧契约，发布中心显示新推理结论”或“查询使用旧推理结论，发布中心显示新覆盖”的错配。

### 8.3 一致性、事务与幂等

| 主题 | 规则 |
| --- | --- |
| 幂等键 | `source_ingest_id（来源接入任务标识）`、`publish_job_id（发布任务标识）`、`approval_job_id（审批任务标识）`、`export_job_id（导出任务标识）`、`trace_id（追踪编号）` |
| 写入事务 | 资产写入 + 关系写入 + 证据挂载 + 快照追加 + 缓存刷新指令为单事务单元 |
| 读取一致性 | 运行服务只读最新稳定快照；不读 `CHECKING` 状态任务 |
| 冲突策略 | 同一逻辑主键在基线变化后写入，一律转人工复核，不做自动覆盖 |
| 图读切换 | 图读只允许命中“最近一次发布时图投影校验通过”的 `scene_id + snapshot_id`；若 `Neo4j` 不可达、校验失败、图快照缺失或版本不一致，读链路必须降级回关系库聚合，不得返回旧图 |

### 8.4 数据保留与归档

1. 原文、证据和审批材料的保留期不在代码中硬编码，应由合规配置驱动；系统必须支持按资产类型配置不同保留策略。
2. 已退役资产默认转归档态，但其历史 `snapshot_id（快照标识）` 与审计链路不得删除，确保可追溯。
3. 导出文件、专审附件、审批意见需支持对象存储归档与审计检索。
4. `Inference Chain（推理链）` 必须实行分级归档：监管解释和发布复核必需链路全量保存，运行优化类中间链路允许摘要保存，未进入正式快照的临时候选链路按策略清理。

## 9. 接口与事件契约设计

本章定义系统对外接口、异步事件与最小返回契约，覆盖请求字段、返回字段、错误码、鉴权方式、幂等语义与同步 / 异步边界。

### 9.1 接口设计总原则

1. 所有对外接口必须返回 `trace_id（追踪编号）`、`snapshot_id（快照标识）`、`decision（决策）`、`reason_code（原因编码）`；禁止只返回“成功 / 失败”文字。
2. 读接口与写接口分离；发布、审批、导出等变更类操作必须带幂等键。
3. 运行服务默认同步返回；发布、审批、导出等长事务通过异步任务接口 + 查询状态接口完成。
4. 接口版本化：请求头或路径必须携带 `api_version（接口版本）`；`Breaking Change（不兼容变更，Breaking Change）` 通过新版本发布。

| 接口 | 类型 | 鉴权与幂等 | 关键请求 | 关键返回 | 备注 |
| --- | --- | --- | --- | --- | --- |
| `Scene Search API（场景搜索接口）` | 同步 | 认证必需；读接口不要求幂等键 | 用户问题、领域限定、已知槽位、`operator_role`、`operator_id`、`api_version` | `scene_candidates`、`matched_terms`、`clarification_hint`、`trace_id`、`snapshot_id` | 返回候选场景，不直接返回方案 |
| `Plan Select API（方案选择接口）` | 同步 | 认证必需；读接口不要求幂等键 | `scene_code`、`slots`、`requested_fields`、`role`、`purpose`、`need_export` | `plan_candidates`、`hard_filter_result`、`decision`、`reason_code`、`trace_id`、`snapshot_id` | 依赖有效快照 |
| `Import Preprocess Stream API（导入预处理事件流接口）` | 异步事件流 | 认证必需；写接口需校验导入权限与责任域 | `rawText`、`sourceType`、`sourceName`、`preprocessMode`、`autoCreateDrafts`、`operator` | `start`、`stage`、`graph_patch`、`draft`、`done`、`error` | 基于 `SSE（服务端事件流，Server-Sent Events）` 的导入期事件流；用于回传导入阶段进度、导入中活图谱补丁和候选草稿状态，`done` 返回与最终预处理结果同构的完成态载荷 |
| `Knowledge Package API（知识包接口）` | 同步 | 认证必需；读接口不要求幂等键 | `selected_scene` / `selected_plan` 或原始问题、`slots`、`role`、`purpose`、`need_export` | `scene`、`plan`、`contract`、`coverage`、`policy`、`path`、`inference`、`evidence`、`risk`、`trace` | 主消费接口 |
| `Home Overview Summary API（首页总览摘要接口）` | 同步 | 认证必需；读接口不要求幂等键 | `operator_id`、`operator_role`、`snapshot_id`、`api_version` | `version_summary`、`publish_blocked_count`、`approval_pending_count`、`runtime_health`、`trace_id` | 首页四张一级状态卡的聚合接口；可由 `BFF（前端聚合层，Backend For Frontend）` 承接，但仍属于正式只读契约 |
| `Recent Context Resume API（最近处理中对象续办接口）` | 同步 | 认证必需；读接口不要求幂等键 | `operator_id`、`limit`、`api_version` | `resume_items[]`、`target_workbench`、`context_package`、`trace_id` | 首页“继续处理”与最近处理中对象入口 |
| `Todo Summary API（待办摘要接口）` | 同步 | 认证必需；读接口不要求幂等键 | `operator_id`、`operator_role`、`limit`、`api_version` | `todo_groups[]`、`total_count`、`trace_id` | 首页“我的待办”聚合接口 |
| `Risk Summary API（重点风险摘要接口）` | 同步 | 认证必需；读接口不要求幂等键 | `operator_id`、`operator_role`、`snapshot_id`、`api_version` | `risk_items[]`、`risk_level_summary`、`trace_id` | 首页重点风险摘要接口，返回可直接跳转的风险对象或任务对象 |
| `Data Map Graph API（数据地图图谱接口）` | 同步 | 认证必需；读接口不要求幂等键 | `root_type`、`root_id`、`snapshot_id`、`inference_snapshot_id`、`object_types`、`statuses`、`relation_types`、`sensitivity_scopes`、`node_type`、`relation_group`、`projection_hints` | `readSource`、`projectionVerificationStatus`、`projectionVerifiedAt`、`node_type`、`relation_group`、`snapshot_id`、`inference_snapshot_id`、`projection_hints`、`root_node_id`、`scene_id`、`scene_name`、`nodes[]`（每项含 `asset_ref`、`node_type`、`label`、`status`）、`edges[]`（每项含 `edge_id`、`relation_group`、`relation_type`） | 面向前端资产图谱工作台的只读 Graph DTO 接口；`root_type=SCENE` 时返回单场景已发布快照图，`root_type=DOMAIN` 时允许返回“统一实体 + 场景成员实例 + 正式归属关系”的领域级聚合结果。领域级结果当前默认由关系库聚合，只有在未来补齐按快照冻结的统一实体投影与校验后才允许切换图读模型。所有返回体顶层必须显式带出 `readSource（读源，设计口径 read_source）`、`projectionVerificationStatus（图投影校验状态，设计口径 projection_verification_status）`、`projectionVerifiedAt（图投影校验时间，设计口径 projection_verified_at）`、`snapshot_id`、`inference_snapshot_id`、`projection_hints` 六类正式口径字段，前端据此判断当前结果来自关系库还是图读模型，以及是否处于领域级聚合视图。 |
| `Data Map Node Detail API（数据地图节点详情接口）` | 同步 | 认证必需；读接口不要求幂等键 | `asset_ref` | `asset_ref`、`node`、`attributes` | 返回右侧详情面板需要的统一资产属性，避免前端直连多个对象接口 |
| `Data Map Edge Detail API（数据地图边详情接口）` | 同步 | 认证必需；读接口不要求幂等键 | `edge_id`、`relation_type`、`snapshot_id`、`inference_snapshot_id` | `edge_id`、`source_node`、`target_node`、`source_asset_ref`、`target_asset_ref`、`relation_type`、`relation_group`、`relation_version`、`direction`、`evidence_refs`、`snapshot_id`、`inference_snapshot_id`、`attributes` | 支撑图谱工作台右侧"边缘/关系详情"页签，返回边属性及证据；`source_asset_ref` / `target_asset_ref` 为边两端资产的统一引用标识，`relation_group` 标注关系所属分组（控制 / 推理 / 语义 / 元数据 / 证据），`relation_version` 为关系自身的版本标识，用于差异对比和审计 |
| `Publish Check API（发布检查接口）` | 同步 | 治理后台权限 | `publish_job_id` 或候选快照引用 | `check_items`、`blocked_items`、`status`、`reason_code` | 治理后台使用 |
| `Publish Execute API（发布执行接口）` | 异步 | 治理后台权限；必须携带 `idempotency_key` | `publish_job_id`、`idempotency_key`、`operator_id` | `job_id`、`accepted`、`trace_id` | 仅治理后台可调用 |
| `Impact Analysis API（影响分析接口）` | 同步 | 治理权限或审计权限 | `asset_ref` / `snapshot_id` / `changed_source` | `affected_assets`、`risk_level`、`recommended_action` | 支持灰度与回滚 |
| `Approval Submit API（审批提交接口）` | 异步 | 认证必需；必须携带 `approval_job_id` 或 `idempotency_key` | `trace_id`、`requested_fields`、`purpose`、`operator_id`、`evidence_refs` | `approval_job_id`、`status` | 高敏 / 导出场景 |
| `Approval Decision API（审批决策接口）` | 异步 | 审批权限；必须携带审批任务标识 | `approval_job_id`、`decision`、`approver`、`comment` | `executed_status`、`trace_id` | 与统一审批平台对接 |

首页总览相关接口属于正式读接口，而不是只存在于页面内部的临时拼装逻辑。若由 `BFF（前端聚合层，Backend For Frontend）` 承接，也必须进入正式 `OpenAPI（开放接口描述规范，OpenAPI Specification）` 契约与版本管理范围。

### 9.1.0 `Import Preprocess Stream API` 补充说明

#### 事件流与顺序约束

1. 正常顺序固定为：`start -> stage* -> graph_patch* -> draft* -> done`；其中 `stage`、`graph_patch`、`draft` 都允许出现多次，`error` 只在失败时替代 `done` 作为结束事件。
2. `stage` 只负责阶段进度、阶段文案和分块完成度；`draft` 只负责候选草稿和落库状态；`graph_patch` 只负责导入中活图谱的增量变化。三类事件不得混装成一个“大杂烩”事件对象。
3. `graph_patch` 的幂等边界固定为 `taskId（任务标识） + patchSeq（补丁序号）`；前端收到重复补丁时必须可安全去重，不因重试或网络抖动重复加节点。

#### `graph_patch` 补丁约定

1. 补丁载荷至少包含：`patchSeq`、`stageKey`、`chunkIndex`、`chunkTotal`、`addedNodes[]`、`updatedNodes[]`、`addedEdges[]`、`updatedEdges[]`、`focusNodeIds[]`、`summary`。
2. `addedNodes[]` / `updatedNodes[]` 中的节点都必须带稳定 `id`、`nodeType`、`label`、`status`、`confidenceScore`、`evidenceRefs`；`addedEdges[]` / `updatedEdges[]` 中的边都必须带稳定 `id`、`sourceId`、`targetId`、`relationType`、`status`、`confidenceScore`。
3. 导入过程中不返回“删除节点 / 删除边”指令。若需要表达归一、合并、驳回或关系降级，统一通过 `status` 变更和补充摘要完成，避免前端活图谱发生突兀大跳。
4. `done` 事件返回的完成态结果必须能与 `graph_patch` 所累积的候选图相互校验：若最终收敛结果与中途补丁不一致，后端需通过 `updatedNodes[]` / `updatedEdges[]` 在最后一批补丁中给出修正依据，而不是让前端静默覆盖。

### 9.1.1 `Data Map Graph API` 补充说明

#### 投影与分组字段约定

1. **`projection_hints`**：前端在请求时传入当前视图所需的字段子集标识（如 `summary`、`full`、`graph_only`），后端据此裁剪 `nodes[]` 和 `edges[]` 内部返回的属性深度，避免全量属性传输；该字段同时出现在返回体中，供前端确认实际使用的投影口径。用于前端按视图稳定投影，保证不同视图（业务视图 / 路径视图 / 覆盖视图）获取的字段范围可预期。
2. **`relation_group`**：标注边所属的逻辑分组（`control` / `inference` / `semantic` / `metadata` / `evidence`），用于前端对边进行视觉分层着色和按分组过滤；在 `Data Map Edge Detail API` 返回中同样作为一等字段出现。用于边的分组和视觉分层，前端据此决定边的色相、线型和过滤器归属。跨场景统一实体首轮固定映射为：`SCENE_MEMBERSHIP -> control`、`INSTANCE_OF -> control`、`MAPS_TO_SOURCE -> metadata`、`APPLIES_POLICY -> control`、`SUPPORTED_BY -> evidence`；关系库聚合与 `Neo4j` 投影必须复用同一映射。
3. **`relation_version`**：`Data Map Edge Detail API` 返回的关系自身版本标识，用于边级版本追溯、差异对比和审计回放；当两个快照之间同一关系的 `relation_version` 不同时，发布中心和影响分析可据此定位关系级变更。

#### Phase 1 图读切换与校验规则

1. `Data Map Graph API` 与 `Impact Analysis API` 在 Phase 1 内允许优先读取 `Neo4j` 图读模型，但前提是目标 `scene_id + snapshot_id` 最近一次发布时图投影校验状态为 `PASSED（已通过）`；若请求未显式传入 `snapshot_id`，后端必须先解析出当前场景的最新稳定已发布快照，再基于该快照决定是否读图。
2. 图读切换的判断顺序固定为：先锁定目标 `snapshot_id`，再检查该快照的图投影校验状态，最后决定 `readSource`；禁止先读图再回填快照，也禁止同一响应混读不同快照的节点和边。
3. `readSource` 的正式枚举值固定为 `NEO4J（图数据库读模型）` 与 `RELATIONAL（关系库聚合回退）`；`projectionVerificationStatus` 至少覆盖 `PASSED（已通过）`、`FAILED（失败）`、`PENDING（待校验）`、`SKIPPED（跳过）` 四种状态。
4. 发布时图投影校验必须覆盖：节点集合、边集合、`asset_ref（资产引用）`、`relation_type（关系类型）`、`snapshot_id（快照标识）` 与关系版本锚点的一致性；任一校验项失败时，都必须阻止该快照进入图读路径。
5. 读侧降级只允许从 `NEO4J` 退回 `RELATIONAL`，不允许反向把关系库结果回写图侧，也不允许在运行时偷偷重建图；图投影修复只能通过明确的重投影或重新发布动作完成。

#### 领域级统一实体返回规则

1. `root_type=DOMAIN` 时，`Data Map Graph API` 返回的主骨架不再是“该领域最新一个场景”的替代结果，而是该领域下多个已发布场景成员实例与统一实体的聚合视图。
2. 领域级结果允许同时包含 `Domain（业务领域）`、`Scene（业务场景）`、场景资产实例节点、统一实体节点，以及 `INSTANCE_OF（实例属于统一实体）`、`CANONICAL_RELATION（统一实体关系）`、`SCENE_MEMBERSHIP（场景成员归属）` 三类正式关系边。
3. `root_type=DOMAIN` 的读侧默认走 `RELATIONAL（关系库聚合回退）`，直到控制库已补齐“统一实体归属冻结 -> 图投影 -> 发布时一致性校验”的完整闭环，才允许切到 `NEO4J（图数据库读模型）`。
4. 领域级结果中的统一实体节点必须能反查到控制库中的 `canonical_entity` 及其 profile；领域级边必须能反查到 `canonical_entity_membership` 或 `canonical_entity_relation` 的正式记录，不允许由前端或图侧临时猜测。
5. `SCENE_MEMBERSHIP` 必须由已发布 `scene_id + snapshot_id` 对应的场景资产成员集生成，不得在查询时从当前可变场景关系现算；领域级响应中的 `snapshot_id` 既是 `INSTANCE_OF` / `CANONICAL_RELATION` 的冻结边界，也是 `SCENE_MEMBERSHIP` 的正式口径边界。

#### 关系类型枚举

支持在 `relation_types（关系类型过滤条件）` 中传入以下 13+ 种正式枚举值，以支撑前端关系类型过滤器和边的视觉分层区分：

1. **控制资产关系**：`CONTAINS_PLAN（包含方案）`、`COVERS_SEGMENT（覆盖分段）`、`DECLARES_CONTRACT（声明契约）`、`APPLIES_POLICY（适用策略）`
2. **推理资产关系**：`IMPLEMENTS_RULE（实现推理规则）`、`PRODUCES_ASSERTION（产出推理结论）`、`FORMS_CHAIN（形成推理链）`
3. **语义资产关系**：`DEFINES_TERM（定义术语）`、`USES_CALIBER（使用口径）`、`DEPENDS_ON_RULE（依赖规则）`
4. **元数据关联**：`MAPS_TO_SOURCE（映射至来源表）`、`BINDS_TO_COLUMN（绑定至物理列）`、`JOIN_WITH（表间物理关联）`
5. **证据关联**：`SUPPORTED_BY（由…支撑）`

### 9.1.2 异步任务状态与导出接口补充

对于发布、审批、导出等长事务，系统必须同时提供“提交接口 + 状态查询接口 + 事件回写”三件套，禁止只提供提交动作而缺少统一状态查询。

| 接口 | 类型 | 关键请求 | 关键返回 | 说明 |
| --- | --- | --- | --- | --- |
| `Publish Job Status API（发布任务状态接口）` | 同步 | `job_id`、`operator_id` | `job_status`、`current_step`、`blocked_items`、`trace_id` | 查询发布执行进度与阻断项 |
| `Approval Status API（审批状态接口）` | 同步 | `approval_job_id`、`operator_id` | `status`、`decision`、`approver`、`trace_id` | 查询审批状态与审批结论 |
| `Export Submit API（导出提交接口）` | 异步 | `trace_id`、`approved_fields`、`approval_job_id`、`operator_id`、`idempotency_key` | `export_job_id`、`accepted`、`trace_id` | 提交导出任务 |
| `Export Status API（导出状态接口）` | 同步 | `export_job_id`、`operator_id` | `status`、`file_fingerprint`、`masking_plan`、`archive_ref` | 查询导出进度与归档信息 |
| `Export Download Token API（导出下载令牌接口）` | 同步 | `export_job_id`、`operator_id` | `download_token`、`expire_at`、`trace_id` | 仅返回临时下载凭证，不直接暴露对象存储路径 |

### 9.1.3 `OpenAPI（开放接口描述规范，OpenAPI Specification）` 发布与消费规则

1. 后端必须稳定暴露 `/v3/api-docs` 作为当前环境的正式接口契约输出；该输出属于运行时可校验资产，而不是仅供人工参考的附属文档。
2. 前端路径常量、类型快照和契约校验必须从 `/v3/api-docs` 生成，不得长期依赖页面源码里的手写接口路径或口头约定。
3. 接口路径、路径参数名、请求体或响应体一旦变更，必须先更新 `/v3/api-docs` 再推进前端接入；若前端生成的契约快照未更新，则该接口变更视为未完成交付。
4. 正式接口文档必须按稳定业务域分组展示，不得把 `controller（控制器）` 类名、内部包名或自动推断出的技术标签直接暴露为正式 `tag（分组标签）`。`Swagger UI（接口调试与展示界面）` 首屏看到的必须是消费方能理解的业务分组，而不是后端实现结构。
5. 被前端、`BFF（前端聚合层，Backend For Frontend）`、自动化流程或外部服务稳定消费的正式接口，必须显式声明稳定的 `summary（摘要）`、必要的 `description（说明）` 与 `operationId（操作标识）`；禁止继续依赖 `list_2`、`query_1` 一类自动生成且随实现波动的兜底命名。
6. 正式接口的成功响应码和错误响应码必须在 `OpenAPI` 中显式登记，并与真实运行行为一致。创建类接口不得继续只暴露默认 `200`，而忽略真实 `201`；校验失败、资源缺失、冲突阻断和内部错误等公共错误返回必须能统一映射到正式错误模型。
7. `ApiErrorDTO（接口错误返回对象）` 等公共错误对象，以及首批高频核心 `DTO（数据传输对象，Data Transfer Object）`，必须具备可读的模型说明。字段名允许沿用代码命名，但字段语义、单位或上下文若不能自解释，必须在 `schema（数据模型描述）` 中补说明，避免消费方反向阅读源码猜语义。
8. 正式接口文档质量必须进入可执行门禁：至少通过集成测试与真实服务探活同时校验关键 `tag`、`summary`、`operationId`、响应码和公共模型说明，禁止把“看起来顺眼”当作唯一验收标准。

### 9.2 `Knowledge Package（知识包）` 最小返回契约

| 字段块 | 最小内容 |
| --- | --- |
| `scene` | `scene_code`、`scene_name`、`scene_type` |
| `plan` | `plan_code`、`plan_version`、`route_reason` |
| `contract` | `effective_contract_view`、`required_outputs` / `optional_outputs` / `masked_outputs` / `restricted_outputs` / `forbidden_outputs` |
| `coverage` | `matched_segment`、`completeness_level`、`fallback_action`、`coverage_explanation`、`actual_snapshot_date`（实际使用的快照日期）、`snapshot_fallback_occurred`（是否发生快照退避，布尔值）、`data_quality_notes`（命中分段的数据质量标注列表） |
| `policy` | `decision`、`approval_required`、`masking_plan`、`policy_hits` |
| `path` | `primary_path`、`path_type`、`source_refs` |
| `inference` | `inference_snapshot_id`、`assertion_refs`、`assertion_summary`、`confidence_hits`、`scope_profile_refs`、`runtime_mode`、`degrade_reason_codes` |
| `evidence` | `evidence_refs`、`evidence_summary` |
| `risk` | `risk_level`、`reason_codes` |
| `trace` | `trace_id`、`snapshot_id`、`generated_at` |

### 9.3 错误码与决策码基线

| 代码 | 说明 |
| --- | --- |
| `SCENE_NOT_FOUND` | 未命中正式 `Scene（业务场景）`，返回 `Scene Discovery Result（场景发现结果）` |
| `PLAN_NOT_ELIGIBLE` | `Scene（业务场景）` 命中但无可运行 `Plan（方案资产）` |
| `COVERAGE_PARTIAL` | 请求超出完整覆盖，仅能部分返回或需审批 |
| `POLICY_APPROVAL_REQUIRED` | 策略要求审批 |
| `POLICY_DENIED` | 命中高敏红线或角色不满足 |
| `EVIDENCE_INSUFFICIENT` | 缺少发布或返回所需证据 |
| `METADATA_DRIFT_BLOCKED` | 来源表 / 字段失效导致阻断 |
| `GRAPH_TIMEOUT` | 图查询超时 |
| `INFERENCE_DEGRADED` | 运行推理已按降级契约退化，但仍返回受控结果 |
| `INFERENCE_ASSERTION_UNAVAILABLE` | 当前推理快照不可读或无合格断言可用，无法形成正式推理结果 |
| `PUBLISH_BLOCKED` | 发布检查未通过 |

### 9.4 事件契约（异步任务）

| 事件名 | 触发条件 |
| --- | --- |
| `source_ingest_requested` | 接收到原始材料并建立导入任务 |
| `source_parsed` | 解析完成并产出候选资产 |
| `metadata_aligned` | 元数据对齐完成 |
| `review_requested` | 进入业务 / 技术 / 合规复核 |
| `snapshot_gray_started` | 新 `snapshot_id（快照标识）` / `inference_snapshot_id（推理快照标识）` 对开始灰度 |
| `snapshot_gray_finished` | 灰度达标并切换为全量运行可见 |
| `snapshot_published` | 快照已切换为运行可见 |
| `snapshot_rolled_back` | 快照对因灰度或运行风险回退到上一稳定版本 |
| `metadata_drift_detected` | 发现来源失效或新鲜度异常 |
| `approval_submitted` | 高敏审批已提交 |
| `approval_decided` | 审批结论已回写 |
| `export_generated` | 导出已生成并归档审计 |

### 9.4.1 事件最小载荷字段

所有异步事件最少必须包含以下字段：`event_id`、`event_name`、`occurred_at`、`trace_id`、`snapshot_id`、`inference_snapshot_id`、`operator_id`、`job_id`、`status`、`reason_code`、`asset_refs`、`source_system`。事件消费者不得依赖裸文本描述判断状态，必须以结构化字段为准。

## 10. 安全、合规与审计设计

安全设计不能只停留在“可脱敏、可审批”层面，而应落实到身份认证、角色授权、字段级控制、导出审查、操作留痕和证据可追溯六个层面。

### 10.1 认证与授权模型

1. 身份认证依赖行内统一认证平台；本系统不保存用户密码，仅保存 `operator_id（操作人标识）`、角色、组织归属与审批上下文。
2. 授权分为四层：系统级访问、场景级访问、字段级访问、导出级访问；高敏场景必须同时满足场景级和字段级授权。
3. `Scene（业务场景）`、`Plan（方案资产）`、`Contract View（契约视图）` 均可按角色绑定可见范围；运行时优先计算可见视图，再做字段返回。

### 10.2 敏感等级与默认动作

| 级别 | 典型对象 | 默认动作 |
| --- | --- | --- |
| `S0` | 无敏感业务字段 | 允许返回 |
| `S1` | 普通内部业务字段 | 允许返回并留痕 |
| `S2` | 手机号、地址、证件号、客户基础信息 | 默认掩码；导出需审批 |
| `S3` | 冻结原因、名单信息、客户经理、推荐人、管控明细 | 默认 `need_approval（需要审批）`，按角色裁剪 |
| `S4` | 密码修改日志、密码相关审计、强命中监视名单 | 默认 `deny（拒绝）` 或专审 |

### 10.3 字段级控制与契约视图

1. `Contract View（契约视图）` 是字段级安全的唯一真值来源；前端与下游不得绕过 `Contract View（契约视图）` 直接拼字段。
2. `masked_outputs` 必须携带 `mask_type（脱敏类型）`，如 `FULL_MASK`、`PARTIAL_MASK`、`HASH_ONLY`。
3. `restricted_outputs` 必须显式标注 `required_role` 或 `approval_template`，避免运行时临时判断。
4. `forbidden_outputs` 仅用于解释和申请提示，不得被普通知识包直接包含原值。

### 10.4 审批、导出与审计要求

| 阶段 | 要求 |
| --- | --- |
| 审批前 | 记录请求人、用途、字段清单、命中策略、`trace_id（追踪编号）`、`snapshot_id（快照标识）` |
| 审批中 | 记录审批人、审批结论、备注、审批模板、时效 |
| 导出时 | 记录导出任务号、文件指纹、时间、字段遮蔽方案、审批单号 |
| 事后审计 | 支持按 `operator_id（操作人标识）` / `scene_code` / `trace_id（追踪编号）` / `approval_job_id（审批任务标识）` 检索完整链路 |

### 10.5 高风险场景特别规则

1. `AUDIT_LOG` 与 `WATCHLIST_CONTROL` 场景默认不进入普通 `allow（允许）`；必须绑定专门 `Policy（策略对象）` 和审批模板。
2. 当用户只需核验而不需明细时，优先返回“是否存在 / 是否命中 / 可申请字段”，不直接返回原始高敏记录。
3. 密码修改日志、名单命中、冻结原因等字段默认禁止进入业务子图的普通展示层。

## 11. NFR、部署、容灾与运维设计

本章定义首发阶段的 `NFR（非功能需求，Non-Functional Requirements）`、环境拓扑、容灾目标、容量假设与 `Runbook（运行手册）` 基线。

### 11.1 `NFR（非功能需求，Non-Functional Requirements）` 基线

| 维度 | 要求 |
| --- | --- |
| 延迟 | `Scene Search（场景搜索）` p95 <= 3 秒；`Knowledge Package（知识包）` 生成 p95 <= 8 秒；审批提交接口 p95 <= 2 秒 |
| 可靠性 | 已发布快照对运行面可用性需达到 `HA（高可用，High Availability）`；发布失败不得影响当前运行快照 |
| 一致性 | 运行面只能看到最新稳定快照；不得出现契约与覆盖版本不一致 |
| 安全 | 未授权 `S3 / S4` 字段暴露次数 = 0；所有高敏访问均有审计链路 |
| 新鲜度 | 元数据与快照新鲜度异常应在同一发布 / 运行链路中被识别并阻断 |
| 可解释性 | `Plan（方案资产）` 选择、`Policy（策略对象）` 决策、`Coverage Declaration（覆盖声明）` 命中、`deny（拒绝）` 原因均可追溯 |

### 11.1.1 延迟指标测量口径

1. `Knowledge Package（知识包）` 生成 `p95 <= 8 秒` 的测量口径，指 `Knowledge Package API（知识包接口）` 从服务端接收完整请求到返回完整 `JSON（JavaScript对象表示法，JavaScript Object Notation）` 响应的端到端服务时延。
2. 该口径计入 `Query Rewrite（查询改写）`、`Slot Filling（槽位补齐）`、`Inference Runtime（运行推理）`、`Scene Recall（场景召回）`、`Plan Selection（方案选择）`、`Coverage Engine（覆盖引擎）`、`Policy Decision（策略决策）`、路径解析和知识包组装时间。
3. 该口径不计入前端渲染、客户端网络传输、人工审批等待、异步导出生成以及人工重试造成的等待时间。
4. 性能验收必须基于样板场景标准请求集统计，不能以空请求、缓存预热请求或单场景最优路径替代正式测量结果。

### 11.2 环境拓扑建议

| 环境 | 用途 | 要求 |
| --- | --- | --- |
| `DEV` | 开发与本地验证 | 可用匿名 / 脱敏样例，允许 `DRAFT（草稿）` 资产验证 |
| `TEST` | 集成测试 | 接口联调、回放集、失败注入、性能基线 |
| `PRE-PROD` | 上线前演练 | 并行运行、发布演练、审批联调、容灾演练 |
| `PROD` | 正式运行 | 仅允许经过发布门禁的快照可见 |

### 11.3 高可用与容灾基线

| 子系统 | 建议 | 说明 |
| --- | --- | --- |
| 关系型控制库 | 主备或高可用集群；日志备份；快照备份 | 发布与审批台账不可丢 |
| 图存储 | 主备或集群；周期性快照与恢复演练 | 运行查询与影响分析依赖 |
| 对象存储 | 版本化与跨区域 / 跨机房复制 | 原文、附件、审计文件需保全 |
| 向量索引 | 允许重建，但必须记录重建窗口与版本绑定 | 不可影响当前稳定快照可读性 |
| 事件 / 日志库 | 至少支持审计级归档和监控级保留策略分离 | 兼顾合规与成本 |

若立项阶段没有更严格基础设施目标，建议首发基线采用“控制面 `RPO（恢复点目标，Recovery Point Objective）` 30 分钟 / `RTO（恢复时间目标，Recovery Time Objective）` 4 小时，运行面优先保障当前稳定快照可读，发布与审批可延后恢复”。最终值以基础设施和合规要求确认为准。

### 11.4 容量与性能假设

1. 首发阶段以样板场景为主，读多写少；发布、审批、导入是低频管理动作，查询与数据地图浏览是高频动作。
2. 容量规划至少考虑三类峰值：工作时段查询峰值、批量导入 / 发布峰值、审批导出峰值。
3. 性能压测需覆盖 `Inference Runtime（运行推理）`、`Scene Recall（场景召回）`、`Plan Select API（方案选择接口）`、`Knowledge Package API（知识包接口）`、`Impact Analysis API（影响分析接口）` 五类主路径；不得只测单接口平均时延。

### 11.5 可观测性、告警与 `Runbook（运行手册）`

| 告警 | 可能原因 | 首要处置 |
| --- | --- | --- |
| `scene_hit_rate（场景命中率）` 日环比下降明显 | 术语漂移、索引失效或新快照问题 | 检查最近发布、索引构建和术语映射 |
| `plan_select_hit_rate（方案选择命中率）` 下降 | 覆盖、策略或来源契约配置异常 | 回查最近策略变更与来源可用性 |
| `inference_degrade_rate（推理降级率）` 升高 | 推理超时、阈值配置异常或断言索引失效 | 检查 `Inference Runtime（运行推理）` 依赖、阈值快照和降级原因编码 |
| `gray_release_diff_rate（灰度差异率）` 超阈值 | 新旧快照对决策差异异常 | 冻结灰度，执行差异分析并准备快照对回滚 |
| `metadata_freshness（元数据新鲜度）` 超阈值 | 快照或元数据平台延迟 | 阻断高风险发布，发起平台排查 |
| `publish_pass_rate（发布通过率）` 降低 | 材料质量下降或发布规则过严 | 查看被阻断门禁项与缺口任务 |
| `approval_queue_backlog（审批队列积压量）` 增长 | 专审积压 | 增加审批资源或调整场景默认策略 |
| `llm_isolation_violation（LLM 隔离违规告警）` 触发 | 临时提示疑似绕过正式推理门禁 | 立即阻断相关发布 / 运行链路，审计 `LLM（大语言模型）` 输出去向 |
| `graph_timeout` / `api_error_rate` 上升 | 图查询或服务异常 | 执行降级与恢复预案 |

## 12. 测试、验收、迁移与回退设计

方案能否上线，取决于测试和迁移闭环，而不只是设计本身是否正确。本章给出首发阶段必须具备的测试矩阵、样板回放集、并行运行方案、切换步骤和回退机制。

### 12.1 测试矩阵

| 测试层 | 范围 |
| --- | --- |
| 单元测试 | 术语归一、规则抽取、推理断言生成、阈值门禁判定、推理冲突仲裁、`Policy（策略对象）` 决策、`Coverage Declaration（覆盖声明）` 命中、契约视图裁剪 |
| 集成测试 | `Source Intake（来源接入） -> Inference Build（推理构建） -> Publish（发布）`、`Query（查询） -> Inference Runtime（运行推理） -> Knowledge Package（知识包）`、`Approval（审批） -> Export（导出）`、`LLM Hint（LLM 提示） -> 隔离校验 -> Clarification / Reject（澄清 / 拒绝）` |
| 契约测试 | 所有 API 的请求 / 响应字段、错误码、幂等语义 |
| 回放测试 | 使用工单样例和样板场景固定问题集做回放 |
| 性能测试 | 主路径 p95、并发读写、发布切换窗口 |
| 安全测试 | 越权访问、字段泄露、导出绕过、审计缺失 |
| 灰度 / 演练 | 并行运行、成对快照灰度发布、阈值变更回放、发布回滚、元数据漂移注入、图查询超时降级、`LLM（大语言模型）` 隔离违规注入 |

### 12.2 样板回放集

| 回放主题 | 验证重点 |
| --- | --- |
| 代发明细查询 | 协议号 / 客户号 / 批次号多入口；验证标识收敛、当前 / 历史 / 批次场景切分与覆盖命中 |
| 基金申赎申请记录 | 验证默认时间为申请时间、时间语义澄清、资金记录场景切换 |
| 零售客户基础查询 | 验证 `current_date - 1` 快照、新鲜度、字段裁剪 |
| 开户机构变更查询 | 验证不完整材料处理、变更轨迹场景产出 |
| 密码修改日志 | 验证 `S4` 高敏拒绝或专审链路 |
| 断卡排查工单 | 验证多标识归一、`as_of_time（截止时点）`、敏感字段降级输出 |

### 12.3 迁移与切换方案

1. 梳理存量资产：把现有文档、`SQL（结构化查询语言，Structured Query Language）` 样例、已知规则、旧场景口径映射到新资产模型。
2. 建立主键：为 `Scene（业务场景）`、`Plan（方案资产）`、`Output Contract（输出契约）`、`Contract View（契约视图）`、`Coverage Declaration（覆盖声明）`、`Policy（策略对象）` 生成稳定逻辑主键和版本主键。
3. 补齐缺口：对缺少结果字段、默认时间、覆盖边界的材料先转 `Gap Task（缺口任务）`，阻断直接发布。
4. 建立并行运行：旧口径与新知识包并行对照，记录差异和误路由案例。
5. 灰度切换：先对样板场景和白名单角色启用新的 `snapshot_id（快照标识）` / `inference_snapshot_id（推理快照标识）` 对，再逐域扩大。
6. 差异观测：灰度期间持续记录场景命中率、方案选择命中率、推理降级率、误路由率和高敏风险差异，并通过 `Impact Analysis API（影响分析接口）` 留痕。
7. 回退机制：若发布后出现误路由、泄露、大面积命中下降或推理降级异常升高，按成对快照回退到上一稳定版本。

### 12.4 上线回退触发条件

1. 样板场景覆盖误路由率显著超阈值。
2. 未授权高敏字段暴露或审计链路缺失。
3. 关键 API 持续超时且短时降级无效。
4. 灰度期间新旧快照对差异率持续超阈值且无法通过阈值治理或适用边界收敛修复。
5. 推理降级率异常升高，导致正式路径持续退化到 `template_only（仅模板路径）` 或 `clarification_only（仅澄清返回）`。
6. 发布快照与缓存 / 索引不一致导致大面积错误。

## 13. 分阶段实施路线与治理机制

实施策略遵循“先样板、再治理、后规模”的原则。首发阶段不追求全量场景覆盖，而是先在代发、财富、零售三大域打穿完整链路。

| 阶段 | 核心目标 | 交付结果 |
| --- | --- | --- |
| 阶段一：标准化与样板入库 | 统一 `Source Intake Contract（来源接入契约）`、`Scene Type（场景类型）`、核心资产主键与首批样板场景 | 可发布的样板 `Scene（业务场景）` / `Plan（方案资产）` 集合 |
| 阶段二：发布治理与数据地图 | 打通复核、发布门禁、快照切换、数据地图工作区 | 发布中心、覆盖矩阵、差异与追溯 |
| 阶段三：运行消费与审批导出 | 稳定输出 `Knowledge Package（知识包）`，对接 `PlanIR（计划中间表示，Plan Intermediate Representation）`、审批与导出 | 运行服务闭环 |
| 阶段四：规模复制 | 通过工单与材料反哺扩展更多业务域 | 常态治理与运营指标面板 |

### 13.0 当前首轮实施收口

虽然总路线按多业务域铺开，但当前首轮实施只验收 `代发 / 薪资域`。首轮交付必须同时满足三项前提：

1. 以真实关系型数据库作为运行基线，控制资产与来源材料落正式表，不再以 `H2（内存数据库）` 或临时大字段模拟主数据。
2. 在同一业务域内至少完成 2 至 3 个已发布场景，而不是只保留单个 `MVP（最小可行产品）` 样例。
3. 数据地图与运行决策台按同一 `Version Snapshot（版本快照）` 联动，确保图谱投影、覆盖解释和知识包输出读取的是同一批已发布资产。

### 13.0.1 跨场景统一实体试点闭环

“跨场景统一实体层与领域级图谱融合”是当前首个同时验证业务能力与工程治理的试点工作项。该工作项进入开发队列时，固定遵循以下收口方式：

1. 交付目标同时包含两类结果：一类是业务结果，即统一实体真源、成员归属、发布冻结、`root_type=DOMAIN` 领域级图谱和运行时复用边界落地；另一类是工程结果，即可复制到后续工作项的分层边界、任务拆分模板、机械门禁与固定角色智能体路由。
2. 开发队列固定拆为 8 个构件任务：`Task 0` 先固化测试文档骨架、边界检查、契约断言与 `Golden Path（黄金路径样例）` 烟测；`Task 1` 落控制库迁移；`Task 2` 落统一实体仓储；`Task 3` 落归并决策服务；`Task 4` 落成员归属维护服务；`Task 5` 落发布冻结服务；`Task 6` 落领域级图读服务；`Task 7` 落 `Impact Analysis（影响分析）` / `Runtime Retrieval（运行检索）` 复用适配。每个任务只允许触碰一层或相邻两层，不允许跨层顺手扩 scope（范围）。
3. `Build Agent（实现智能体）` 的执行输入必须结构化为任务卡：至少写明任务编号、所属层级、可修改目录、禁止触碰目录、输入契约、输出契约、必跑测试和 `Review Agent（评审智能体）` 关注点。没有任务卡的实现请求不得直接进入编码。
4. 首轮必须先机械化 5 类门禁：分层越界检查、设计/计划/测试文档联通性检查、统一实体契约快照检查、发布“只冻结归属不复制真源”回归检查、`Golden Path` 端到端烟测。第一轮允许先用目录约束、导入规则、关键字扫描和回归测试组合拦错，不要求一开始就建设复杂平台。
5. 半自动多智能体编排按状态文档路由：`planning（计划中）` 优先进入 `Plan Agent（计划智能体）`，`implementing（开发中）` / `fixing（修复中）` 进入 `Build Agent（实现智能体）`，`reviewing（评审验证中）` 进入 `Review Agent（评审智能体）`；只有当统一键规则、发布边界或运行时消费口径发生变化时才回到 `Design Agent（设计智能体）`。`automation（自动化巡检）` 只负责推荐角色和下一动作，不直接改状态、不直接提交业务代码。
6. 试点是否跑通，不以“代码已写完”为判据，而以 5 个闭环同时成立为准：功能闭环、边界闭环、测试闭环、协作闭环、文档闭环。缺少任一闭环，都不能视为该工作项已完成首轮治理收口。

### 13.1 首发验收口径

| 指标 | 口径 |
| --- | --- |
| 样板场景发布数 | 当前首轮先在 `代发 / 薪资域` 完成至少 2 个、目标 3 个可解释、可审计的已发布 `Scene（业务场景）` |
| `Plan（方案资产）` 选择可解释率 | 样板场景达到 100% |
| `evidence_coverage（证据覆盖率）` | 已发布关键场景 >= 95% |
| 覆盖误路由率 | 样板场景控制在 3% 以下 |
| 未授权敏感字段暴露 | 0 |
| 阻断类缺口闭环率 | 100%，全部进入任务台账并可追踪 |
| 真实库运行基线 | 首轮验收请求全部基于真实关系型控制库，不接受 `H2（内存数据库）` 回放结果代替正式验收 |

### 13.1.1 关键验收指标口径补充

1. 覆盖误路由率，指在样板回放集与正式验收样本中，本应命中既定 `Scene（业务场景）` / `Plan（方案资产）` / `Coverage Segment（覆盖分段）` 的请求，被错误路由到其他场景、其他方案、其他覆盖分段，或错误给出 `FULL / PARTIAL / GAP` 判定的样本占比。
2. 覆盖误路由率的分子为误路由样本数，分母为全部“输入充分且应可判定”的样本数；因输入缺失进入澄清、因权限不足直接拒绝、因材料缺口无法发布的样本，不计入该指标分母。
3. 首发验收按样板场景逐类统计覆盖误路由率，总体目标控制在 3% 以下；任一关键样板场景若连续两个统计窗口超过 3%，应视为需治理整改的异常信号。

### 13.2 治理 `SLA（服务级别协议，Service Level Agreement）` 与责任分工（`RACI（职责分工矩阵，Responsible / Accountable / Consulted / Informed）`）

| 流程 | R | A | C | I |
| --- | --- | --- | --- | --- |
| `Source Intake（来源接入）` 校验 | 数据治理 | 业务口径负责人 | 技术 / 合规 | 下游消费方 |
| 业务复核 | 业务口径负责人 | 数据治理 | 研发 / 合规 | 运维 |
| 技术复核与发布执行 | 平台研发 | 数据治理 | 业务 / 合规 | 运维 |
| 高敏审批 | 合规 / 授权审批人 | 业务口径负责人 | 数据治理 / 研发 | 下游消费方 |
| 监控与恢复 | 运维 / SRE | 平台研发 | 数据治理 | 业务方 |

建议首发治理 `SLA（服务级别协议，Service Level Agreement）` 基线如下：发布阻断类缺口任务必须在发布窗口内有明确责任人与状态；高敏审批队列需有工作时段内的明确处置时限；元数据新鲜度异常需在同一工作日触发阻断或降级动作；误路由与高敏暴露事件需按审计事件升级处理。

## 14. 样板场景实施附录

本附录把首批样板场景落成可以直接实施的骨架清单。每个样板必须同时回答六个问题：`Scene（业务场景）` 是什么、`Plan（方案资产）` 怎么拆、`Output Contract（输出契约）` 返回什么、`Coverage Declaration（覆盖声明）` 覆盖到哪里、`Policy（策略对象）` 如何控制、`Evidence Fragment（证据片段）` 从哪里来。

| Scene | 类型 | 主入口 | Coverage | Policy | Plan 拆分 |
| --- | --- | --- | --- | --- | --- |
| 代发明细查询 | `FACT_DETAIL` | 协议号、客户号 | 2014+ 主表 `FULL`；2004-2013 历史 `PARTIAL`；2004 前 `GAP` | 普通明细可 `allow（允许）`；全历史默认 `need_approval（需要审批）` | 当前明细 / 历史明细 |
| 代发批次结果查询 | `FACT_AGGREGATION` | 批次号 | 批次表按现有来源覆盖；不复用明细主对象 | 普通结果 `allow（允许）`；高敏字段裁剪 | 批次结果 |
| 基金申购与赎回申请记录 | `FACT_DETAIL` | 账户号、客户号、产品号 | 默认申请日期；与资金交易场景分离 | 普通 `allow（允许）`；超范围字段按契约裁剪 | 申请记录 |
| 基金资金交易 / 分红记录 | `FACT_DETAIL` | 账户号、客户号、产品号 | 资金 / 入账时间；与申请记录分场景 | 需说明时间语义；部分产品需审批 | 资金 / 分红 |
| SA 理财交易查询 | `FACT_DETAIL` | 账户号、客户号、产品号 | 2020-08 起覆盖；更早数据不承诺 | 覆盖为 `PARTIAL` 时返回说明 | SA 理财 |
| 零售客户基础信息查询 | `ENTITY_PROFILE` | 客户号、户口号、证件号 | `current_date - 1` 快照 | `S2` 默认掩码，导出审批 | 客户画像 |
| 零售户口开户机构变更查询 | `CHANGE_TRACE` | 户口号、客户号 | 按变更记录表覆盖；不完整材料先补结果字段 | 普通查询 `allow（允许）`，导出审批 | 变更轨迹 |
| 客户密码修改日志查询 | `AUDIT_LOG` | 用户号、客户号、户口号 | 按日志源覆盖 | `S4` 默认 `deny（拒绝）` 或专审 | 密码审计 |
| 断卡排查账户管控核验 | `WATCHLIST_CONTROL` | 工单号、`UID`、客户号、户口号 | 按投诉日期 `as_of_time（截止时点）` 回看 | 敏感字段降级返回或专审 | 管控核验 |

### 14.1 首批样板必须通过的检查点

1. 至少一条 `PUBLISHED（已发布）` `Plan（方案资产）` 能落到真实 `Source Contract（来源契约）`。
2. `required_outputs` 全部可兑现，且 `Policy（策略对象）` 可以生成明确字段视图。
3. `FULL / PARTIAL / GAP` 有正式解释，不允许用备注替代。
4. `Evidence Fragment（证据片段）` 至少可回到文档片段、`SQL（结构化查询语言，Structured Query Language）` 片段或人工确认之一。
5. 高敏场景必须演练审批与拒绝路径。

## 15. 知识图谱 -> 数据地图投影约定

本节定义知识图谱正式对象到前端工作台页面元素的一一映射关系。目标不是写抽象原则，而是让每个正式对象在每个主视图或面板中都有确定的页面表达。前端工作台的页面骨架和交互细节继续由 [前端界面与工作台设计](frontend-workbench-design.md) 承接，本节只约定"对象投影成什么"。

### 15.1 投影映射总表

| 正式对象 | 中文 | 业务视图 | 路径视图 | 覆盖视图 | 右侧检查面板 | 差异清单 |
| --- | --- | --- | --- | --- | --- | --- |
| `Domain（业务领域）` | 业务领域 | 左侧导航树一级节点；图谱中作为聚类区域标签 | 不独立展示，作为路径上下文中的领域标注 | 覆盖矩阵行头的领域分组标签 | 对象概况页签中的"所属领域"字段 | 不单独出现 |
| `Scene（业务场景）` | 业务场景 | 底部场景带卡片；图谱中的靛蓝主节点 | 页头左侧的"当前场景"上下文标注 | 覆盖矩阵的行标识 | 对象概况页签的第一主对象；覆盖检查页签的入口锚点 | 变更对象行，按"新增 / 修改 / 替代 / 退役"分组 |
| `Plan（方案资产）` | 方案资产 | 图谱中的深青节点；与 `Scene` 之间以控制资产关系实线连接 | 路径起点或路径归属标注 | 覆盖矩阵单元格下钻后的方案标识 | 对象概况页签中的关联方案列表 | 变更对象行，独立展示路由前置条件和来源优先级差异 |
| `Contract View（契约视图）` | 契约视图 | 图谱中作为 `Plan` 的子节点；默认折叠，点击 `Plan` 后展开 | 不独立展示 | 不独立展示 | 对象概况页签中的"输出与安全"子区块，展示字段裁剪、角色范围和审批模板 | 变更对象行，按字段级逐项对比新旧视图 |
| `Coverage Segment（覆盖分段）` | 覆盖分段 | 图谱中作为 `Plan` 的附属关系边标签（如"2014+ 完整"） | 路径视图中路径节点的时间覆盖标注 | 覆盖矩阵的单元格（绿 / 橙 / 红着色）；左侧时间轴的分段节点 | 覆盖检查页签的分段明细列表，含完整度、排除项和回退动作 | 覆盖差异行，按"新增分段 / 分段边界变更 / 分段删除"分组 |
| `Policy（策略对象）` | 策略对象 | 图谱中的深靛节点；与 `Plan` 以控制资产关系实线连接 | 不独立展示 | 不独立展示 | 风险与影响页签中的策略命中列表，展示决策三态和审批模板 | 变更对象行，展示策略规则变更和影响的角色范围 |
| `Source Contract（来源契约）` | 来源契约 | 图谱中的暖灰节点；与 `Plan` 以元数据关联细线连接 | 右侧证据列"来源契约摘要"页签的主展示对象 | 不独立展示 | 对象概况页签中的"来源约束"子区块 | 变更对象行，展示来源表 / 字段 / 快照周期差异 |
| `Canonical Entity（统一实体）` | 统一实体 | `root_type=DOMAIN` 的业务视图中作为跨场景共享锚点节点出现；与场景资产实例通过 `INSTANCE_OF / SCENE_MEMBERSHIP` 关系连接 | 路径视图中不作为默认路径节点，但可在共享来源、共享策略和共享证据解释时显示为上层锚点 | 不独立展示 | 对象概况页签中的"统一身份"子区块，展示统一键、类型、成员场景与人工决议状态 | 变更对象行，展示统一键变更、成员增减和关系拆分 / 合并差异 |
| `Inference Rule（推理规则）` | 推理规则 | 图谱中作为可选展示节点（需通过过滤器启用），青绿色 | 右侧证据列"规则说明"页签中的规则卡片 | 不独立展示 | 对象概况页签中的"关联推理规则"折叠区 | 推理资产变更分组中的规则差异行 |
| `Inference Assertion（推理结论）` | 推理结论 | 图谱中作为可选展示节点（需通过过滤器启用），青绿色；与 `Inference Rule` 以推理关系虚线连接 | 不独立展示 | 不独立展示 | 风险与影响页签中的"推理结论摘要"子区块 | 推理资产变更分组中的结论差异行，含结论类型、适用范围和复核状态 |
| `Inference Chain（推理链）` | 推理链 | 不在业务视图默认展示 | 不在路径视图默认展示 | 不独立展示 | 风险与影响页签中"推理结论摘要"的下钻详情，以步骤图展示 | 推理资产变更分组中的推理链差异行（仅摘要） |
| `Evidence Fragment（证据片段）` | 证据片段 | 图谱中的淡紫节点；与关联对象以证据关联线连接 | 右侧证据列"证据片段"页签的主展示对象 | 不独立展示 | 对象概况页签中的"证据支撑"子区块，含原文锚点和确认状态 | 证据差异行，展示新增 / 变更 / 移除的证据片段 |
| `Join Relation Object（表间关联关系对象）` | 表间关联关系 | 图谱中的浅琥珀节点或边标签；表达两张来源表之间的业务关联 | 路径图中来源表节点之间的关联边，鼠标悬停展示连接键和关联类型 | 不独立展示 | 对象概况页签中的"表间关联"折叠区，展示连接键、关联类型和业务语义 | 变更对象行，展示连接键和过滤条件差异 |
| `Version Snapshot（版本快照）` | 版本快照 | 顶部工具栏的"当前版本选择"控件；图谱节点的版本徽标 | 页头右侧的"路径版本 / 快照"切换控件 | 覆盖矩阵支持按快照横向滚动查看历史分段 | 对象概况页签底部的版本信息区，含 `snapshot_id` 和 `inference_snapshot_id` 配对 | 差异视图顶部的"左版本 vs 右版本"标题栏 |

### 15.2 投影规则补充说明

1. 上表中"不独立展示"指该对象在对应视图中不作为独立节点或独立区块出现，但可能作为其他对象的附属信息展示。
2. 推理类对象（`Inference Rule`、`Inference Assertion`、`Inference Chain`）在业务视图图谱中默认不展示，用户可通过顶部过滤器启用"推理节点"后查看。
3. 当 `Data Map Graph API` 以 `root_type=DOMAIN` 返回时，业务视图允许展示统一实体节点；当以 `root_type=SCENE` 返回时，统一实体默认不单独展开，只在详情面板中作为共享身份补充信息出现，避免单场景画布被统一层骨架抢占。
3. 所有在图谱中以节点形式展示的对象，点击后右侧检查面板必须切换到对应对象的详情页签，不跳离当前画布。
4. 差异清单中的对象分组顺序固定为：控制资产变更 -> 推理资产变更 -> 语义资产变更 -> 覆盖差异 -> 证据差异 -> 元数据关联差异。

## 16. Workbench Context Package（工作台上下文包）

工作台上下文包是前端工作台之间跨台跳转时必须携带的正式契约，不是前端交互建议。任何跨工作台跳转必须通过统一的上下文包传递焦点对象和运行上下文，目标工作台收到上下文包后按包内字段加载数据，不依赖全局状态或浏览器缓存。

### 16.1 上下文包字段定义

| 字段 | 类型 | 必填 | 语义 |
| --- | --- | --- | --- |
| `source_workbench` | `string` | 是 | 发起跳转的源工作台标识，固定枚举为 7 个一级工作台编码 |
| `target_workbench` | `string` | 是 | 跳转目标工作台标识 |
| `intent` | `string` | 是 | 跳转意图，如 `run_query`（发起查询）、`submit_approval`（提交审批）、`view_impact`（查看影响）、`replay_trace`（回放链路）、`view_node`（查看节点） |
| `scene_code` | `string` | 否 | 当前焦点业务场景编码 |
| `plan_code` | `string` | 否 | 当前焦点方案资产编码 |
| `asset_ref` | `string` | 否 | 当前焦点资产的统一引用标识（类型 + 编码） |
| `edge_id` | `string` | 否 | 当前焦点边（关系）标识，用于从图谱跳转时传递关系上下文 |
| `relation_type` | `string` | 否 | 当前焦点边的关系类型枚举值 |
| `trace_id` | `string` | 否 | 追踪编号，用于回放或审计场景 |
| `snapshot_id` | `string` | 否 | 控制资产快照标识 |
| `inference_snapshot_id` | `string` | 否 | 推理资产快照标识，必须与 `snapshot_id` 配对使用 |
| `path_id` | `string` | 否 | 当前主路径标识 |
| `candidate_path_id` | `string` | 否 | 当前预览中的候选路径标识 |
| `coverage_segment_id` | `string` | 否 | 当前焦点覆盖分段标识 |
| `evidence_refs` | `string[]` | 否 | 需要在目标工作台展示的证据片段引用列表 |
| `requested_fields` | `string[]` | 否 | 预填的请求字段列表，用于审批和导出场景 |
| `purpose` | `string` | 否 | 预填的用途说明 |
| `lock_mode` | `string` | 是 | 数据加载模式，固定枚举为 `latest` / `replay` / `frozen` |

### 16.2 `lock_mode` 三种模式语义与使用边界

| 模式 | 语义 | 使用边界 |
| --- | --- | --- |
| `latest`（最新态） | 目标工作台收到上下文包后，使用包内标识从后端接口加载当前最新已发布状态的数据。`snapshot_id` 和 `inference_snapshot_id` 仅作为参考，不强制锁定版本。 | 适用于日常浏览、发起新查询、查看当前状态等不要求历史一致性的场景。允许重算和刷新。 |
| `replay`（回放态） | 目标工作台必须按 `trace_id` 或 `snapshot_id` + `inference_snapshot_id` 加载历史时刻的数据快照，复现当时的决策上下文。不允许使用当前最新数据替代历史态。 | 适用于审计回放、告警追溯、历史决策复现。不允许重算，只允许只读查看。 |
| `frozen`（冻结态） | 目标工作台必须严格按 `snapshot_id` + `inference_snapshot_id` 加载指定版本的数据，禁止自动刷新或版本漂移。用于审批和导出等需要版本一致性保证的场景。 | 适用于审批流程、导出执行、发布影响分析。审批单关联的数据必须与提交时一致，不允许中途因版本切换导致审批依据变化。 |

### 16.3 上下文包传递原则

1. 上下文包通过 URL 参数或前端路由状态传递，不依赖浏览器 `localStorage` 或全局状态管理器。
2. 上下文包只携带标识和模式信息，不携带完整对象数据。目标工作台收到标识后必须自行从后端接口加载数据。
3. `snapshot_id` 和 `inference_snapshot_id` 必须成对传递，禁止只传其一。
4. 当 `lock_mode` 为 `replay` 或 `frozen` 时，`snapshot_id` 和 `inference_snapshot_id` 为必填。
5. 目标工作台加载失败时（如快照已归档、资产已退役），必须展示结构化错误提示，说明失败原因和替代路径，不得静默回退到 `latest` 模式。

## 17. 变更记录

### 2026-03-30（第七轮）

1. §8.1.2 补充“跨场景统一实体层与领域级图谱融合”试点闭环的六层边界：明确 `Types / Config / Repo / Service / Runtime / UI` 的职责划分，以及 `Runtime`、`Publish`、`UI`、`Service` 的越界禁止项。
2. §13.0.1 新增跨场景统一实体试点闭环：把当前工作项固定收口为 8 个构件任务、结构化任务卡、5 类机械门禁与固定角色智能体路由，作为进入开发队列的正式约束。
3. 首轮验收口径补充“功能闭环、边界闭环、测试闭环、协作闭环、文档闭环”五类完成条件，避免只以“功能写完”判断试点结束。

### 2026-03-28（第五轮）

1. §9.1 `Data Map Graph API` 关键返回字段重排：将 `node_type`、`relation_group`、`snapshot_id`、`inference_snapshot_id`、`projection_hints` 提升为返回体顶层显式口径字段（不再仅嵌套在 `nodes[]` / `edges[]` 子字段内），前端据此确认类型范围、版本基线和投影口径。
2. §9.1 `Data Map Edge Detail API` 请求字段补齐 `snapshot_id`、`inference_snapshot_id`，返回字段补齐 `edge_id`、`source_asset_ref`、`target_asset_ref`、`relation_group`、`relation_version`、`snapshot_id`、`inference_snapshot_id`；补充说明用于支撑边/关系一等公民和差异对比。
3. §9.1 `Data Map Graph API` 返回字段从 `nodes[]`、`edges[]` 粗粒度口径升级为显式子字段口径：`nodes[]` 每项含 `asset_ref`、`node_type`、`label`、`status`；`edges[]` 每项含 `edge_id`、`relation_group`、`relation_type`；同时将 `projection_hints` 纳入返回体，供前端确认实际投影口径。
4. §9.1.1 新增"投影与分组字段约定"子节，显式说明 `projection_hints`（前端按视图稳定投影）、`relation_group`（边的分组和视觉分层）、`relation_version`（边级版本追溯）三个字段的设计用途和消费方式。
5. 新增 §15 知识图谱 -> 数据地图投影约定：定义知识图谱本体节点 / 边到前端数据地图的投影映射总表与规则，`projection_hints` 由前端按当前视图传入，后端据此裁剪返回属性深度。
6. 新增 §16 Workbench Context Package（工作台上下文包）：定义前端跨工作台跳转的唯一上下文传递载体，携带 `lock_mode`（`latest` / `replay` / `frozen`）、焦点标识和快照对字段，禁止绕过上下文包直接拼 URL 参数；具体跳转链路规则和加载行为见前端主文档 §6.2。
7. `Data Map Graph API` / `Data Map Edge Detail API` 接口字段补齐：确保请求和返回的语义字段完整覆盖前端数据地图工作台的筛选、投影、版本锁定和关系分层需求。

### 2026-03-30（第六轮）

1. `Candidate Entity Graph（候选实体图谱）` 新增“导入中活图谱”事件契约：固定通过 `Import Preprocess Stream API（导入预处理事件流接口）` 回传 `start`、`stage`、`graph_patch`、`draft`、`done`、`error` 六类事件。
2. §5.1.0 补充 `graph_patch` 作为候选图增量补丁的强约束，明确补丁字段、无硬删除原则和最终完成态校准边界。
3. §6.1 材料接入与发布流程的“解析抽取”步骤新增实时事件流回传约束，要求阶段进度与活图谱在同一任务上下文内联动。
4. §9.1 新增 `Import Preprocess Stream API（导入预处理事件流接口）` 正式契约，并新增 §9.1.0 说明事件顺序、补丁幂等边界和 `graph_patch` 载荷字段。

### 2026-03-28（第四轮）

1. 补充知识图谱本体到数据地图工作台的映射缺口，修补系统设计对图谱边缘关系的支撑。
2. §9.1 接口列表新增 `Data Map Edge Detail API（数据地图边详情接口）`，以支撑前端数据地图工作台将边（关系）作为一等公民展示。
3. §9.1.1 新增 `Data Map Graph API` 补充说明，正式枚举 13+ 类 `relation_types（关系类型过滤条件）`（分为控制、推理、语义、元数据、证据五个分组），统一前后端对齐口径。
