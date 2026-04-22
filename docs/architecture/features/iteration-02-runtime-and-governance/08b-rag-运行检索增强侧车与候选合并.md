# RAG 运行检索增强侧车与候选合并特性文档

> 迭代归属：迭代二运行与治理工作台
> 来源主文档：`system-design.md` §4.3、§6.2、§6.2.2、§6.2.3、§8.1、§8.1.2；`knowledge-graph-concepts-and-boundaries.md` §5.2、§5.3、§5.5、§8；`08-运行决策与知识包生成.md`、`10b-跨场景统一实体层与领域级图谱融合.md`、`11-监控审计与影响分析.md`、`11b-运维验收与上线保障.md`
> 对应实施计划：`docs/plans/2026-04-22-rag-runtime-retrieval-sidecar-implementation-plan.md`

# 一、特性概述

## 1.1 背景

当前运行主线已经具备 `Query Rewrite（查询改写）`、`Inference Runtime（运行推理）`、`Scene Recall（场景召回）`、`Plan Selection（方案选择）`、`Coverage Engine（覆盖引擎）`、`Policy Decision（策略决策）` 和 `Knowledge Package（知识包）` 输出能力，但“图结构召回 + 向量召回 + 引用返回”的实验能力尚未被正式纳入边界。若直接把外部检索引擎接成新的主检索器，系统会出现两类风险：一是正式决策链被黑盒分数挤占，二是快照版本、证据范围和策略门禁难以保持一致。

本专题只解决一个问题：如何把 `RAG（检索增强生成，Retrieval-Augmented Generation）` / `GraphRAG（图检索增强生成，Graph Retrieval-Augmented Generation）` 能力接成运行时的侧车服务，为正式场景召回和证据召回补候选，而不让实验链路直接产出正式命中结果。首轮默认以 `LightRAG（开源 GraphRAG 检索框架，LightRAG）` 作为首个适配器与回放基线。

## 1.2 目标

本特性用于在 `Query Rewrite（查询改写）` / `Slot Filling（槽位补齐）` 之后、正式 `Scene Recall（场景召回）` 排序前引入统一 `Retrieval Experiment Adapter（运行检索实验适配器）`，并把候选场景、统一实体、证据引用和得分分解结果安全并入正式运行主线。系统得到的是“检索增强侧车”，不是“新的正式决策器”。

# 二、指标

1. 门禁型指标：`Policy false allow（策略误放行）` 次数为 `0`。统计口径：分子为启用检索实验侧车后出现的高敏字段误放行请求数，分母为启用实验适配器的运行请求数，统计窗口为首轮 `代发 / 薪资域` 回放集与灰度流量。
2. 门禁型指标：`snapshot mismatch（快照错配）` 次数为 `0`。统计口径：分子为实验适配器读取到与请求 `snapshot_id（快照标识）` 不一致索引版本的请求数，分母为启用实验适配器的运行请求数，统计窗口为首轮灰度期。
3. 效果型指标：`scene hit@5（前 5 命中率，Hit at 5）` 不低于现有正式召回基线。统计口径：分母为启用实验适配器的样板回放请求数，分子为真实目标 `Scene（业务场景）` 出现在前 5 个候选场景中的请求数，统计窗口为首轮 `代发 / 薪资域` 回放集。
4. 效果型指标：证据 `precision@10（前 10 精确率，Precision at 10）` 不低于现有正式证据召回基线。统计口径：分母为启用实验适配器的样板回放请求数乘以 10，分子为前 10 个实验候选证据中与真实答案一致的证据数，统计窗口为首轮回放集。

# 三、特性全景

## 3.1 特性全景描述

本特性只覆盖“运行时检索增强侧车与候选合并”一个场景。它位于正式运行主线内部，负责把实验引擎提供的候选场景、统一实体和证据引用并入 `Scene Recall（场景召回）` 与知识包调试块，但不改变正式策略门禁、覆盖判定和知识包最终决策。

## 3.2 特性图示

结构固定为“用户问题 -> `Query Rewrite（查询改写）` / `Slot Filling（槽位补齐）` -> `Retrieval Experiment Adapter（运行检索实验适配器）` -> 正式 `Scene Recall（场景召回）` 合并排序 -> `Plan Selection（方案选择）` / `Coverage Engine（覆盖引擎）` / `Policy Decision（策略决策）` -> `Knowledge Package（知识包）`”。

# 四、特性说明

## 4.1 原型（非必要）

前端主承载继续沿用“运行决策台”一级工作台，当前页面入口为 `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`。实验侧车的操作化观测继续落“监控审计与影响分析”工作台，不新增业务工作台页面。

## 4.2 功能说明

### 4.2.1 特性场景1

通用补充项：
- 主对象：`Knowledge Package Query（知识包查询请求）`、`Retrieval Experiment Adapter（运行检索实验适配器）`、`Scene Recall Service（场景召回服务）`、`Canonical Entity（统一实体）`、`Evidence Fragment（证据片段）`、`Knowledge Package（知识包）`、`Experiment Reference Block（实验引用块）`。
- 默认时间语义：业务时间语义仍由现有 `Query Rewrite（查询改写）`、`Inference Runtime（运行推理）` 与 `Coverage Declaration（覆盖声明）` 解释；实验适配器不得重写业务默认时间，只能消费已经收敛的槽位。
- 覆盖范围和缺口：首轮只覆盖 `代发 / 薪资域` 已发布场景的候选场景召回、统一实体扩召回和证据引用补充；不覆盖跨业务域一次性混合召回，不覆盖最终 `Plan（方案资产）` 命中裁决。
- 策略或审批边界：实验适配器不直接输出 `allow（允许）`、`need_approval（需要审批）`、`deny（拒绝）`；一切正式可见字段、审批要求和导出边界继续由 `Policy Decision（策略决策）` 控制。
- 前端入口、详情页和跳转链路：入口为“运行决策台”；实验引用调试块放在知识包详情的调试区域；观测结果进入“监控审计与影响分析”工作台；不新增业务深页。
- 接口或数据边界：输入为 `query_text（原始问题文本）`、`structured_slots（结构化槽位）`、`domain_scope（业务域范围）`、`snapshot_id（快照标识）`、`allowed_evidence_scope（允许证据范围）` 与 `trace_id（追踪编号）`；输出为候选场景、候选实体、候选证据、引用定位和分数组成；不输出正式知识包。
- 存储落位或持久化边界：实验适配器只读已发布快照索引；运行结果只写实验运行记录、审计事件和知识包调试块，不回写正式 `Scene`、`Plan`、`Coverage Declaration` 或 `Policy`。

1. `Retrieval Experiment Adapter（运行检索实验适配器）` 的调用位置固定在 `Query Rewrite（查询改写）` / `Slot Filling（槽位补齐）` 之后、正式 `Scene Recall（场景召回）` 排序前。它只能补候选，不能替换现有正式召回器。
2. 统一输入契约至少包含 `query_text（原始问题文本）`、`structured_slots（结构化槽位）`、`domain_scope（业务域范围）`、`snapshot_id（快照标识）`、`allowed_evidence_scope（允许证据范围）` 与 `trace_id（追踪编号）`。引擎专有参数必须收口在适配器配置层，不允许透传到业务请求契约。
3. 统一输出契约至少包含 `candidate_scenes（候选场景列表）`、`candidate_entities（候选实体列表）`、`candidate_evidence（候选证据列表）`、`reference_refs（引用定位列表）`、`score_breakdown（分数组成）` 与 `adapter_metadata（适配器元数据）`。任何实验结果都不能直接变成正式 `scene_code（场景编码）`、正式 `plan_code（方案编码）` 或正式 `decision（决策结论）`。
4. 候选合并规则固定为“正式规则优先、实验候选补充”。现有正式召回结果仍是排序骨架；实验候选只允许用于扩召回、补引用和补统一实体解释，不能压掉已经由正式规则命中的明确候选。
5. 实验候选若命中统一实体层，只能作为 `Scene Recall（场景召回）` 的缩域与扩召回输入，不得直接跳过 `Plan Selection（方案选择）`、`Coverage Engine（覆盖引擎）` 或 `Policy Decision（策略决策）`。最终运行边界仍然固定在正式 `scene_id + snapshot_id + plan_id` 集合。
6. `Knowledge Package（知识包）` 中允许增加实验引用调试块，用来展示适配器名称、版本、候选引用与分数组成；该调试块默认只服务内部调试和回放验证，不构成业务用户可见的正式解释口径。
7. 失败回退规则固定为：适配器超时、失败、返回空集或索引版本不可读时，系统直接退回现有正式召回路径；失败只进入实验运行记录和监控事件，不改变知识包的正式决策结果。
8. 首轮默认以 `LightRAG（开源 GraphRAG 检索框架，LightRAG）` 作为首个适配器与回放基线，但系统对内对外暴露的都是统一 `Retrieval Experiment Adapter（运行检索实验适配器）`。后续替换引擎时，候选合并、快照锁定和审计字段不随引擎名漂移。
9. 本场景不负责实验索引构建与版本锁定，不负责离线评测与 `Shadow Mode（影子模式）`，也不负责知识生产阶段的实验候选回写；这些职责分别由 `08c`、`08d` 和 `02f` 专题承接。
