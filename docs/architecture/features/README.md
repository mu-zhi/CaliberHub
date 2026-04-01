# 特性文档目录

本目录用于承接已经从主方案拆出的正式特性文档。默认形态仍是场景级文档；当主方案中存在必须单独治理、且已在路线稿中显式拆出的原子平台能力时，也允许以“专题特性文档”形式落在本目录。这里的文档不是并行主方案，而是对 [`../system-design.md`](../system-design.md) 和 [`../frontend-workbench-design.md`](../frontend-workbench-design.md) 的可实施展开：主文档继续定义总对象、总边界、总主线；本目录负责把单一场景或单一专题能力需要的业务背景、治理对象、接口边界和前端承载落成可评审、可排期的单文档。

## 使用约束

1. 一份特性文档只写一个场景或一个经路线稿明确拆出的专题能力，不混写多个业务场景，也不混写多个专题能力。
2. 正式口径以主文档为准；如果特性文档发现总纲缺口，先回写主文档，再同步本目录。
3. 文档结构遵循 [`../../engineering/standards/scenario-feature-doc-standard.md`](../../engineering/standards/scenario-feature-doc-standard.md)。
4. 迭代总览和覆盖关系统一见 [`../../plans/2026-03-28-feature-doc-iteration-roadmap-design.md`](../../plans/2026-03-28-feature-doc-iteration-roadmap-design.md)。

## 目录分组

### 迭代一：知识生产链路

- [`01-材料接入与来源接入契约登记.md`](iteration-01-knowledge-production/01-材料接入与来源接入契约登记.md)：材料接入与来源接入契约登记
- [`02-解析抽取与证据确认.md`](iteration-01-knowledge-production/02-解析抽取与证据确认.md)：解析抽取与证据确认
- [`02b-openai-responses-preprocess-adapter.md`](iteration-01-knowledge-production/02b-openai-responses-preprocess-adapter.md)：OpenAI Responses 预处理适配
- [`02c-preprocess-structured-output-and-prompt-governance.md`](iteration-01-knowledge-production/02c-preprocess-structured-output-and-prompt-governance.md)：预处理结构化输出与 Prompt 治理
- [`03-资产建模与治理对象编辑.md`](iteration-01-knowledge-production/03-资产建模与治理对象编辑.md)：资产建模与治理对象编辑
- [`03a-dictionary-governance.md`](iteration-01-knowledge-production/03a-dictionary-governance.md)：字典治理
- [`04-推理校验与推理链复核.md`](iteration-01-knowledge-production/04-推理校验与推理链复核.md)：推理校验与推理链复核
- [`04a-inference-asset-detail-and-visibility.md`](iteration-01-knowledge-production/04a-inference-asset-detail-and-visibility.md)：推理资产详情与前台可见
- [`05-元数据对齐与来源契约固化.md`](iteration-01-knowledge-production/05-元数据对齐与来源契约固化.md)：元数据对齐与来源契约固化
- [`05a-join-relation-governance.md`](iteration-01-knowledge-production/05a-join-relation-governance.md)：表间关联关系治理
- [`06-复核与缺口任务协同.md`](iteration-01-knowledge-production/06-复核与缺口任务协同.md)：复核与缺口任务协同

### 迭代二：运行与治理工作台

说明：`12a` 表示从“12 全局壳层、导航与跨工作台上下文跳转”中拆出的首页独立场景。为了保持既有编号引用稳定，首页总览保留 `12a` 并与 `12` 并列，而不重新整体改号。

- [`12a-首页总览与状态分发.md`](iteration-02-runtime-and-governance/12a-首页总览与状态分发.md)：首页总览与状态分发
- [`07-发布检查、灰度发布与回滚.md`](iteration-02-runtime-and-governance/07-发布检查、灰度发布与回滚.md)：发布检查、灰度发布与回滚
- [`08-运行决策与知识包生成.md`](iteration-02-runtime-and-governance/08-运行决策与知识包生成.md)：运行决策与知识包生成
- [`09-审批与导出.md`](iteration-02-runtime-and-governance/09-审批与导出.md)：审批与导出
- [`10-数据地图浏览与覆盖追踪.md`](iteration-02-runtime-and-governance/10-数据地图浏览与覆盖追踪.md)：数据地图浏览与覆盖追踪
- [`11-监控审计与影响分析.md`](iteration-02-runtime-and-governance/11-监控审计与影响分析.md)：监控审计与影响分析
- [`11c-llm-provider-routing-and-model-governance.md`](iteration-02-runtime-and-governance/11c-llm-provider-routing-and-model-governance.md)：LLM Provider 路由与模型治理
- [`12-全局壳层、导航与跨工作台上下文跳转.md`](iteration-02-runtime-and-governance/12-全局壳层、导航与跨工作台上下文跳转.md)：全局壳层、导航与跨工作台上下文跳转
- [`12b-openapi-contract-and-frontend-consumption.md`](iteration-02-runtime-and-governance/12b-openapi-contract-and-frontend-consumption.md)：OpenAPI 契约与前端消费

### 迭代三：公司代发与财富样板域

- [`13-代发明细查询.md`](iteration-03-payroll-and-wealth/13-代发明细查询.md)：代发明细查询
- [`14-代发批次结果查询.md`](iteration-03-payroll-and-wealth/14-代发批次结果查询.md)：代发批次结果查询
- [`15-基金申购、赎回申请记录查询.md`](iteration-03-payroll-and-wealth/15-基金申购、赎回申请记录查询.md)：基金申购、赎回申请记录查询
- [`16-基金资金交易与分红记录查询.md`](iteration-03-payroll-and-wealth/16-基金资金交易与分红记录查询.md)：基金资金交易与分红记录查询
- [`17-SA理财交易查询.md`](iteration-03-payroll-and-wealth/17-SA理财交易查询.md)：SA 理财交易查询

### 迭代四：零售画像与高风险样板域

- [`18-零售客户基础信息查询.md`](iteration-04-retail-and-high-risk/18-零售客户基础信息查询.md)：零售客户基础信息查询
- [`19-零售户口开户机构变更查询.md`](iteration-04-retail-and-high-risk/19-零售户口开户机构变更查询.md)：零售户口开户机构变更查询
- [`20-客户密码修改日志查询.md`](iteration-04-retail-and-high-risk/20-客户密码修改日志查询.md)：客户密码修改日志查询
- [`21-断卡排查账户管控核验.md`](iteration-04-retail-and-high-risk/21-断卡排查账户管控核验.md)：断卡排查账户管控核验
