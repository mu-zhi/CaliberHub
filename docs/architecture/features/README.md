# 特性文档目录

本目录用于承接已经从主方案拆出的正式特性文档。默认形态仍是场景级文档；当主方案中存在必须单独治理、且已在路线稿中显式拆出的原子平台能力时，也允许以“专题特性文档”形式落在本目录。这里的文档不是并行主方案，而是对 [`../system-design.md`](../system-design.md) 和 [`../frontend-workbench-design.md`](../frontend-workbench-design.md) 的可实施展开：主文档继续定义总对象、总边界、总主线；本目录负责把单一场景或单一专题能力需要的业务背景、治理对象、接口边界和前端承载落成可评审、可排期的单文档。

## 使用约束

1. 一份特性文档只写一个场景或一个经路线稿明确拆出的专题能力，不混写多个业务场景，也不混写多个专题能力。
2. 正式口径以主文档为准；如果特性文档发现总纲缺口，先回写主文档，再同步本目录。
3. 文档结构遵循 [`../../engineering/standards/scenario-feature-doc-standard.md`](../../engineering/standards/scenario-feature-doc-standard.md)。
4. 迭代总览和覆盖关系统一见 [`../../plans/2026-03-28-feature-doc-iteration-roadmap-design.md`](../../plans/2026-03-28-feature-doc-iteration-roadmap-design.md)。

## 目录分组

### 迭代一：知识生产链路

- [`01-source-intake-registration.md`](iteration-01-knowledge-production/01-source-intake-registration.md)：材料接入与来源接入契约登记
- [`02-parsing-and-evidence-confirmation.md`](iteration-01-knowledge-production/02-parsing-and-evidence-confirmation.md)：解析抽取与证据确认
- [`03-asset-modeling-and-governance-editing.md`](iteration-01-knowledge-production/03-asset-modeling-and-governance-editing.md)：资产建模与治理对象编辑
- [`03a-dictionary-governance.md`](iteration-01-knowledge-production/03a-dictionary-governance.md)：字典治理
- [`04-inference-validation-and-chain-review.md`](iteration-01-knowledge-production/04-inference-validation-and-chain-review.md)：推理校验与推理链复核
- [`04a-inference-asset-detail-and-visibility.md`](iteration-01-knowledge-production/04a-inference-asset-detail-and-visibility.md)：推理资产详情与前台可见
- [`05-metadata-alignment-and-source-contract.md`](iteration-01-knowledge-production/05-metadata-alignment-and-source-contract.md)：元数据对齐与来源契约固化
- [`05a-join-relation-governance.md`](iteration-01-knowledge-production/05a-join-relation-governance.md)：表间关联关系治理
- [`06-review-and-gap-task-collaboration.md`](iteration-01-knowledge-production/06-review-and-gap-task-collaboration.md)：复核与缺口任务协同

### 迭代二：运行与治理工作台

说明：`12a` 表示从“12 全局壳层、导航与跨工作台上下文跳转”中拆出的首页独立场景。为了保持既有编号引用稳定，首页总览保留 `12a` 并与 `12` 并列，而不重新整体改号。

- [`12a-home-overview-and-state-dispatch.md`](iteration-02-runtime-and-governance/12a-home-overview-and-state-dispatch.md)：首页总览与状态分发
- [`07-publish-check-gray-release-and-rollback.md`](iteration-02-runtime-and-governance/07-publish-check-gray-release-and-rollback.md)：发布检查、灰度发布与回滚
- [`08-runtime-decision-and-knowledge-package.md`](iteration-02-runtime-and-governance/08-runtime-decision-and-knowledge-package.md)：运行决策与知识包生成
- [`09-approval-and-export.md`](iteration-02-runtime-and-governance/09-approval-and-export.md)：审批与导出
- [`10-data-map-browsing-and-coverage-tracing.md`](iteration-02-runtime-and-governance/10-data-map-browsing-and-coverage-tracing.md)：数据地图浏览与覆盖追踪
- [`11-monitoring-audit-and-impact-analysis.md`](iteration-02-runtime-and-governance/11-monitoring-audit-and-impact-analysis.md)：监控审计与影响分析
- [`12-global-shell-navigation-and-context-handoff.md`](iteration-02-runtime-and-governance/12-global-shell-navigation-and-context-handoff.md)：全局壳层、导航与跨工作台上下文跳转
- [`12b-openapi-contract-and-frontend-consumption.md`](iteration-02-runtime-and-governance/12b-openapi-contract-and-frontend-consumption.md)：OpenAPI 契约与前端消费

### 迭代三：公司代发与财富样板域

- [`13-payroll-detail-query.md`](iteration-03-payroll-and-wealth/13-payroll-detail-query.md)：代发明细查询
- [`14-payroll-batch-result-query.md`](iteration-03-payroll-and-wealth/14-payroll-batch-result-query.md)：代发批次结果查询
- [`15-fund-subscription-redemption-application.md`](iteration-03-payroll-and-wealth/15-fund-subscription-redemption-application.md)：基金申购、赎回申请记录查询
- [`16-fund-capital-dividend-record.md`](iteration-03-payroll-and-wealth/16-fund-capital-dividend-record.md)：基金资金交易与分红记录查询
- [`17-sa-wealth-transaction-query.md`](iteration-03-payroll-and-wealth/17-sa-wealth-transaction-query.md)：SA 理财交易查询

### 迭代四：零售画像与高风险样板域

- [`18-retail-customer-basic-profile.md`](iteration-04-retail-and-high-risk/18-retail-customer-basic-profile.md)：零售客户基础信息查询
- [`19-retail-account-opening-org-change.md`](iteration-04-retail-and-high-risk/19-retail-account-opening-org-change.md)：零售户口开户机构变更查询
- [`20-customer-password-change-audit-log.md`](iteration-04-retail-and-high-risk/20-customer-password-change-audit-log.md)：客户密码修改日志查询
- [`21-watchlist-control-verification.md`](iteration-04-retail-and-high-risk/21-watchlist-control-verification.md)：断卡排查账户管控核验
