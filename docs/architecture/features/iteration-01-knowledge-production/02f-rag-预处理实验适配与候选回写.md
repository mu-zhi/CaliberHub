# RAG 预处理实验适配与候选回写特性文档

> 迭代归属：迭代一知识生产链路
> 来源主文档：`system-design.md` §4.2、§4.3、§4.4.7、§5.1.0、§8.1；`knowledge-graph-concepts-and-boundaries.md` §5.5、§8、§9；`02-解析抽取与证据确认.md`、`02b-openai-responses-preprocess-adapter.md`、`02c-preprocess-structured-output-and-prompt-governance.md`、`03-资产建模与治理对象编辑.md`、`06-复核与缺口任务协同.md`
> 对应实施计划：`docs/plans/2026-04-22-rag-preprocess-experiment-adapter-implementation-plan.md`

# 一、特性概述

## 1.1 背景

当前导入预处理链路已经能够把材料标准化结果送入候选图、候选证据和人工复核链路，但外部 `RAG（检索增强生成，Retrieval-Augmented Generation）` / `GraphRAG（图检索增强生成，Graph Retrieval-Augmented Generation）` 引擎还没有正式接入边界。若直接把这类引擎接到正式解析、正式资产写库或发布链路，系统会失去“候选层隔离、人工复核、正式发布”三道门禁。

本专题只解决一个问题：如何把实验引擎正式接到导入预处理阶段，并且保证它只能生成候选实体、候选关系和候选证据，不能直接写正式 `Scene（业务场景）`、`Plan（方案资产）`、`Evidence Fragment（证据片段）` 或 `Source Contract（来源契约）`。首轮默认以 `LightRAG（开源 GraphRAG 检索框架，LightRAG）` 作为首个适配器和回放对照，但产品名只属于实验实现层，不进入正式对象命名。

## 1.2 目标

本特性用于在材料标准化完成后、正式解析抽取入库前引入统一 `Preprocess Experiment Adapter（预处理实验适配器）`，并把实验结果稳定回写到 `Candidate Entity Graph（候选实体图谱）` 与候选证据层。系统得到的不是一条新的正式写库主链路，而是一条受控的实验补候选链路。

# 二、指标

1. 门禁型指标：实验结果误写正式治理资产次数为 `0`。统计口径：分子为实验结果直接写入正式 `Scene / Plan / Evidence Fragment / Source Contract` 的次数，分母为启用 `Preprocess Experiment Adapter（预处理实验适配器）` 的导入任务数，统计窗口为首轮 `代发 / 薪资域` 回放集与真实联调任务。
2. 门禁型指标：实验候选来源追溯完整率达到 `100%`。统计口径：分母为实验适配器输出的候选实体、候选关系、候选证据总数，分子为同时带有 `import_task_id（导入任务标识）`、`reference_refs（引用定位列表）`、`adapter_name（适配器名称）` 与 `adapter_version（适配器版本）` 的候选数，统计窗口为首轮回放集与真实联调任务。
3. 效果型指标：候选证据 `hit@10（前 10 命中率，Hit at 10）` 不低于现有预处理基线。统计口径：分母为启用实验适配器的导入回放请求数，分子为真实证据片段出现在前 10 个候选证据中的请求数，统计窗口为首轮 `代发 / 薪资域` 回放集，目标值不低于当前正式预处理基线。
4. 效果型指标：人工复核页对实验候选的直接采用率达到 `60%` 以上。统计口径：分母为进入复核页的实验候选建议数，分子为未经二次手工重录、直接被采纳进入候选图或候选证据的建议数，统计窗口为首轮灰度观察期。

# 三、特性全景

## 3.1 特性全景描述

本特性只覆盖“导入预处理阶段的实验适配与候选回写”一个场景。它位于知识生产主链路内部，负责把实验引擎输出收口为候选层对象，并把调用边界、输入输出、失败回退和人工复核前隔离规则固定下来。它不替代现有正式解析抽取主链路，也不负责发布后运行检索。

## 3.2 特性图示

结构固定为“材料标准化结果 -> `Preprocess Experiment Adapter（预处理实验适配器）` -> 候选实体 / 候选关系 / 候选证据 -> `Candidate Entity Graph（候选实体图谱）` 与候选证据回写 -> 人工复核决定是否转正”。

# 四、特性说明

## 4.1 原型（非必要）

前端主承载继续沿用“知识生产台”一级工作台，当前页面入口为 `frontend/src/pages/KnowledgePage.jsx`，候选图主面板继续沿用 `frontend/src/components/knowledge/CandidateEntityGraphPanel.jsx` 与 `frontend/src/components/knowledge/ImportLiveGraphCanvas.jsx`。本专题不新增独立业务工作台。

## 4.2 功能说明

### 4.2.1 特性场景1

通用补充项：
- 主对象：`Source Material（来源材料）`、`Import Task（导入任务）`、`Preprocess Experiment Adapter（预处理实验适配器）`、`Candidate Entity Graph（候选实体图谱）`、`Candidate Evidence Fragment（候选证据片段）`、`Experiment Run Record（实验运行记录）`。
- 默认时间语义：本场景不解释业务查询时间；候选结果只绑定导入任务执行时点、材料版本与引用位置，不重写业务默认时间语义。
- 覆盖范围和缺口：首轮只覆盖 `代发 / 薪资域` 材料的文本、`PDF（便携式文档格式，Portable Document Format）`、`Office（办公文档格式，Office Documents）` 附件和截图型输入的实验候选回写；不覆盖正式资产转正、不覆盖自动发布、不覆盖发布后运行召回。
- 策略或审批边界：实验适配器只能由受控后端服务调用，不能由前端直连；实验结果进入候选图后仍需走人工复核与正式发布门禁，不存在“实验命中即正式生效”路径。
- 前端入口、详情页和跳转链路：入口为“知识生产台 -> 导入任务主线页”；候选图详情与候选证据详情继续在现有右侧检查区与候选证据面板查看；不新增跨工作台直跳。
- 接口或数据边界：输入为材料标准化结果、附件引用、模态白名单和 `trace_id（追踪编号）`；输出为候选实体、候选关系、候选证据、引用定位和实验元数据；不输出正式 `Scene（业务场景）`、正式 `Plan（方案资产）`、正式 `Evidence Fragment（证据片段）`。
- 存储落位或持久化边界：候选节点、候选边和候选证据继续落导入任务级候选图与候选证据表；实验运行记录落任务级台账；若引擎需要图索引或向量索引，只能使用 `task_id（任务标识）` 级隔离命名空间，不能并入已发布主图或运行时快照索引。

1. `Preprocess Experiment Adapter（预处理实验适配器）` 的调用时点固定在“材料标准化完成后、正式解析抽取入库前”。它的职责是补候选，而不是替代正式解析服务。
2. 统一输入契约至少包含 `import_task_id（导入任务标识）`、`material_refs（材料引用列表）`、`normalized_chunks（标准化文本分块列表）`、`attachment_refs（附件引用列表）`、`allowed_modality_scope（允许模态范围）` 与 `trace_id（追踪编号）`。实现层可以按引擎需要扩展字段，但不能删减这组项目级最小输入。
3. 统一输出契约至少包含 `candidate_entities（候选实体列表）`、`candidate_relations（候选关系列表）`、`candidate_evidence（候选证据列表）`、`reference_refs（引用定位列表）`、`adapter_name（适配器名称）`、`adapter_version（适配器版本）` 与 `warnings（警告列表）`。任何引擎专有字段都必须包在适配器元数据里，不允许直接污染候选图正式字段。
4. 多模态输入边界固定为“先做材料标准化，再决定是否进入实验适配器”。`PDF`、`Office`、截图、表格和图片型材料可以进入实验链路，但首轮不把 `OCR（光学字符识别，Optical Character Recognition）` 修复、图片增强和表格重建当成正式交付能力；相关结果只能作为候选引用，不作为正式证据。
5. 首轮默认以 `LightRAG（开源 GraphRAG 检索框架，LightRAG）` 作为首个适配器和回放基线，但系统对外暴露的是统一 `Preprocess Experiment Adapter（预处理实验适配器）`，而不是某个开源产品名。后续更换引擎时，特性边界与候选回写契约不变。
6. 实验结果回写时必须保持“候选层优先、正式层隔离”。候选实体回写到 `Candidate Entity Graph（候选实体图谱）`，候选证据回写到候选证据层，实验运行记录回写到任务级台账；正式 `Scene / Plan / Evidence Fragment / Source Contract` 禁止直接写入。
7. 前端在知识生产台中必须能区分“正式候选”和“实验补候选”。候选图与候选证据详情至少要展示适配器名称、版本、引用来源和警告摘要，避免人工复核把实验建议误读为已经确认的正式事实。
8. 失败回退规则固定为：实验适配器超时、失败、返回空集或模态不支持时，系统继续沿用现有正式预处理链路，不阻断导入任务。失败信息进入实验运行记录和任务详情提示，但不把失败当成正式导入错误。
9. 本场景不负责 provider 路由治理，不负责正式 Prompt 治理，不负责候选对象的人工确认结论，也不负责发布后的运行检索；这些职责分别由相邻的 `02b`、`02c`、`03`、`06` 和 `08b / 08c / 08d` 专题承接。
