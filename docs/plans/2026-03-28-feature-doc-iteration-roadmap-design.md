# 2026-03-28 特性文档拆分与迭代路线设计稿

本文根据 [`docs/architecture/system-design.md`](../architecture/system-design.md)、[`docs/architecture/frontend-workbench-design.md`](../architecture/frontend-workbench-design.md) 与 [`docs/engineering/standards/scenario-feature-doc-standard.md`](../engineering/standards/scenario-feature-doc-standard.md) 的现有结论，先把正式方案拆成可落文档的特性清单，再按依赖关系编排迭代顺序。主文档仍是正式总纲；本设计稿只负责说明“先拆什么、放到哪里、为什么这样分组”。

## 一、拆分原则

1. 每份特性文档只承载一个场景，不在同一文档里混写多个业务场景或多个治理场景。
2. 每个特性文档都必须同时覆盖四类内容：业务背景、治理对象、接口或数据边界、前端承载页面。
3. 平台能力型场景优先于样板业务场景落地；没有知识生产链路、发布链路和运行链路，样板场景无法形成稳定交付。
4. 样板业务场景按来源相近、时间语义相近、审批风险相近的原则编排到同一迭代，减少一次迭代内的异构成本。
5. 特性文档统一放在 `docs/architecture/features/`；`docs/plans/` 只保存本次路线设计稿，不再并行承载正式特性正文。

## 二、迭代路线

| 迭代 | 目标 | 主要依赖 | 包含特性文档 | 出口标准 |
| --- | --- | --- | --- | --- |
| 迭代一 | 打通知识生产主链路，先让材料能进入、能建模、能复核 | 无前置迭代，直接承接主方案第 5、6、7 章与前端知识生产台 | 01-06、02b-02f、03a、04a、05a | 已具备来源接入、证据沉淀、资产建模、推理校验、元数据对齐、复核与缺口闭环，并补齐 provider 适配、Prompt 治理、任务主线、真实服务 E2E、字典、推理资产详情、表间关联与预处理实验适配等原子治理能力 |
| 迭代二 | 打通首页分发、发布、运行、审批、数据地图与跨工作台协同 | 依赖迭代一产出的已复核资产 | 07-12、08b-08d、11c、12a、12b | 已具备首页状态分发、发布检查、灰度回滚、知识包生成、检索增强侧车、实验索引锁定、实验评测与运行观测、审批导出、图谱浏览、监控审计、provider 路由治理、跨台锁定态跳转，并补齐 `OpenAPI` 与前端消费收口 |
| 迭代三 | 先落地公司代发与财富样板域，验证历史覆盖、多时间语义和路径解释 | 依赖迭代一与迭代二的通用平台能力 | 13-17 | 已具备代发协议链路、代发明细、代发批次、基金申请、基金资金/分红、SA 理财样板能力 |
| 迭代四 | 落地零售画像与高风险场景，验证敏感分级、审批边界与审计闭环 | 依赖迭代二的审批、审计、发布、运行链路 | 18-21 | 已具备零售画像、变更轨迹、密码审计、断卡排查四类高敏或强治理样板能力 |

## 三、特性文档清单

### 3.1 迭代一：知识生产链路

| 编号 | 场景 | 文档路径 |
| --- | --- | --- |
| 01 | 材料接入与来源接入契约登记 | `docs/architecture/features/iteration-01-knowledge-production/01-材料接入与来源接入契约登记.md` |
| 02 | 解析抽取与证据确认 | `docs/architecture/features/iteration-01-knowledge-production/02-解析抽取与证据确认.md` |
| 02b | OpenAI Responses 预处理适配 | `docs/architecture/features/iteration-01-knowledge-production/02b-openai-responses-preprocess-adapter.md` |
| 02c | 预处理结构化输出与 Prompt 治理 | `docs/architecture/features/iteration-01-knowledge-production/02c-preprocess-structured-output-and-prompt-governance.md` |
| 02d | 知识生产台任务主线页收口 | `docs/architecture/features/iteration-01-knowledge-production/02d-knowledge-task-mainline-finalization.md` |
| 02e | 知识生产台真实服务 E2E 收口 | `docs/architecture/features/iteration-01-knowledge-production/02e-knowledge-production-real-service-e2e-hardening.md` |
| 02f | RAG 预处理实验适配与候选回写 | `docs/architecture/features/iteration-01-knowledge-production/02f-rag-预处理实验适配与候选回写.md` |
| 03 | 资产建模与治理对象编辑 | `docs/architecture/features/iteration-01-knowledge-production/03-资产建模与治理对象编辑.md` |
| 03a | 字典治理 | `docs/architecture/features/iteration-01-knowledge-production/03a-dictionary-governance.md` |
| 04 | 推理校验与推理链复核 | `docs/architecture/features/iteration-01-knowledge-production/04-推理校验与推理链复核.md` |
| 04a | 推理资产详情与前台可见 | `docs/architecture/features/iteration-01-knowledge-production/04a-inference-asset-detail-and-visibility.md` |
| 05 | 元数据对齐与来源契约固化 | `docs/architecture/features/iteration-01-knowledge-production/05-元数据对齐与来源契约固化.md` |
| 05a | 表间关联关系治理 | `docs/architecture/features/iteration-01-knowledge-production/05a-join-relation-governance.md` |
| 06 | 复核与缺口任务协同 | `docs/architecture/features/iteration-01-knowledge-production/06-复核与缺口任务协同.md` |

### 3.2 迭代二：运行与治理工作台

说明：`12a` 是从“12 全局壳层、导航与跨工作台上下文跳转”中拆出的首页独立场景。为了避免已经形成的编号引用整体漂移，本次路线稿保留 `12a` 作为并列编号，而不重排 07-12 的顺序。

| 编号 | 场景 | 文档路径 |
| --- | --- | --- |
| 12a | 首页总览与状态分发 | `docs/architecture/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发.md` |
| 07 | 发布检查、灰度发布与回滚 | `docs/architecture/features/iteration-02-runtime-and-governance/07-发布检查、灰度发布与回滚.md` |
| 08 | 运行决策与知识包生成 | `docs/architecture/features/iteration-02-runtime-and-governance/08-运行决策与知识包生成.md` |
| 08b | RAG 运行检索增强侧车与候选合并 | `docs/architecture/features/iteration-02-runtime-and-governance/08b-rag-运行检索增强侧车与候选合并.md` |
| 08c | 已发布快照检索索引同步与版本锁定 | `docs/architecture/features/iteration-02-runtime-and-governance/08c-已发布快照检索索引同步与版本锁定.md` |
| 08d | 检索实验评测、灰度与运行观测 | `docs/architecture/features/iteration-02-runtime-and-governance/08d-检索实验评测、灰度与运行观测.md` |
| 09 | 审批与导出 | `docs/architecture/features/iteration-02-runtime-and-governance/09-审批与导出.md` |
| 10 | 数据地图浏览与覆盖追踪 | `docs/architecture/features/iteration-02-runtime-and-governance/10-数据地图浏览与覆盖追踪.md` |
| 11 | 监控审计与影响分析 | `docs/architecture/features/iteration-02-runtime-and-governance/11-监控审计与影响分析.md` |
| 11c | LLM Provider 路由与模型治理 | `docs/architecture/features/iteration-02-runtime-and-governance/11c-llm-provider-routing-and-model-governance.md` |
| 12 | 全局壳层、导航与跨工作台上下文跳转 | `docs/architecture/features/iteration-02-runtime-and-governance/12-全局壳层、导航与跨工作台上下文跳转.md` |
| 12b | `OpenAPI` 契约与前端消费 | `docs/architecture/features/iteration-02-runtime-and-governance/12b-openapi-contract-and-frontend-consumption.md` |

### 3.3 迭代三：公司代发与财富样板域

| 编号 | 场景 | 文档路径 |
| --- | --- | --- |
| 13 | 代发明细查询 | `docs/architecture/features/iteration-03-payroll-and-wealth/13-代发明细查询.md` |
| 14 | 代发批次结果查询 | `docs/architecture/features/iteration-03-payroll-and-wealth/14-代发批次结果查询.md` |
| 15 | 基金申购、赎回申请记录查询 | `docs/architecture/features/iteration-03-payroll-and-wealth/15-基金申购、赎回申请记录查询.md` |
| 16 | 基金资金交易与分红记录查询 | `docs/architecture/features/iteration-03-payroll-and-wealth/16-基金资金交易与分红记录查询.md` |
| 17 | SA 理财交易查询 | `docs/architecture/features/iteration-03-payroll-and-wealth/17-SA理财交易查询.md` |

### 3.4 迭代四：零售画像与高风险样板域

| 编号 | 场景 | 文档路径 |
| --- | --- | --- |
| 18 | 零售客户基础信息查询 | `docs/architecture/features/iteration-04-retail-and-high-risk/18-零售客户基础信息查询.md` |
| 19 | 零售户口开户机构变更查询 | `docs/architecture/features/iteration-04-retail-and-high-risk/19-零售户口开户机构变更查询.md` |
| 20 | 客户密码修改日志查询 | `docs/architecture/features/iteration-04-retail-and-high-risk/20-客户密码修改日志查询.md` |
| 21 | 断卡排查账户管控核验 | `docs/architecture/features/iteration-04-retail-and-high-risk/21-断卡排查账户管控核验.md` |

## 四、主方案覆盖矩阵

| 主方案范围 | 对应特性文档 |
| --- | --- |
| 知识生产链路、来源接入、证据沉淀、资产建模、推理构建、元数据对齐、复核发布前门禁 | 01-07、02b-02f、03a、04a、05a |
| 运行时主线、知识包输出、覆盖判定、策略决策、审批导出、异步任务、错误码与事件 | 08-11、08b-08d、11c |
| 数据地图、发布中心、运行决策台、知识生产台、审批与导出、监控与审计、首页总览、深页设计、跨台跳转 | 01-12、12a、12b |
| 首批样板场景中的公司代发与财富管理域 | 13-17 |
| 首批样板场景中的零售画像、变更轨迹、高敏审计与管控核验域 | 18-21 |
| 全局导航、角色显隐、锁定态上下文包、版本一致性前端承载 | 10-12、12a |
| 安全分级、审批边界、审计要求、回滚与运行降级 | 07-12、18-21 |
| 数据与存储分层、缓存刷新、一致性事务与幂等规则 | 07、11、12 |
| `NFR（非功能需求，Non-Functional Requirements）`、延迟测量、可观测性与容量假设 | 11、12a、08d |
| `system-design.md` §15 图谱投影约定与前端图谱消费口径 | 10、11、08c |
| `system-design.md` §16 工作台上下文包、锁定态跳转与跨台重算边界 | 08、09、10、11、12、12a |
| `system-design.md` §9.1.3 `OpenAPI（开放接口描述规范，OpenAPI Specification）` 发布与消费规则 | 07、08、09、10、11、12、12a、12b |
| `system-design.md` §5.1.2 `Dictionary（字典）` 一等资产治理 | 03a |
| `system-design.md` §5.1.3 `Join Relation Object（表间关联关系对象）` 一等资产治理 | 05a |
| `frontend-workbench-design.md` §6.3 推理资产前台可见规则 | 04a |
| 测试矩阵、样板回放集、迁移切换与上线回退 | 07、11、13-21 |

## 五、执行顺序建议

1. 先编写迭代一和迭代二的特性文档，确保平台底座与工作台规则先成文；其中 `02f`、`08b`、`08c`、`08d` 属于当前已确认的专题缺口，应与相邻主链路文档同批维护。
2. 再按“公司代发 -> 财富管理 -> 零售画像 -> 高风险审计”的顺序落样板场景文档，避免业务口径互相抢定义。
3. 每完成一个迭代目录，就回到主文档核对一次：该迭代新增约束如果改变了总纲，必须回写 `system-design.md` 或 `frontend-workbench-design.md`，不让特性文档反向变成主真源。

## 六、实施前共性检查项

1. 每份进入实施排期的特性文档，都要补一遍存储落位确认：控制对象、图谱关系、原文附件、事件日志分别落在哪类存储，不允许实施阶段反向猜。
2. 所有前端将消费的接口，包括首页聚合类只读接口和 `BFF（前端聚合层，Backend For Frontend）` 接口，都必须先进入 `/v3/api-docs`，再进入页面实现。
3. 迭代二起所有可进入运行面的场景，都要绑定 `NFR（非功能需求，Non-Functional Requirements）` 验收条目：延迟、可用性、审计留痕、降级可解释性至少要有对应门禁。
