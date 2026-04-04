# 2026-04-04 特性文档覆盖与最低完备性巡检

## 1. 巡检范围

- 主方案：`docs/architecture/system-design.md`
- 前端主方案：`docs/architecture/frontend-workbench-design.md`
- 特性文档目录：`docs/architecture/features/README.md`
- 当前交付状态：`docs/engineering/current-delivery-status.md`
- 既有计划目录：`docs/plans/`

本轮只做四件事：

1. 重新核对上轮巡检中的原子能力缺口是否仍然成立。
2. 对命中的特性文档执行最低完备性检查。
3. 只为通过门禁的特性同步产出正式实施计划草案。
4. 不修改正式设计主文档，不修改业务代码。

## 2. 原子能力映射结论

| 原子能力项 | 当前状态 | 现有归属 / 证据 | 建议归属特性文档 | 是否建议新增专题文档 | 结论 |
| --- | --- | --- | --- | --- | --- |
| `Dictionary（字典）` 一等治理资产与码值映射闭环 | 已覆盖 | `03a-dictionary-governance.md` 已单独建档并进入特性目录 | `iteration-01-knowledge-production/03a-dictionary-governance.md` | 否 | 上轮缺口已关闭 |
| `Join Relation Object（表间关联关系对象）` 建模与发布治理 | 已覆盖 | `05a-join-relation-governance.md` 已单独建档并进入特性目录 | `iteration-01-knowledge-production/05a-join-relation-governance.md` | 否 | 上轮缺口已关闭 |
| `Input Slot Schema（输入槽位模式）` 与 `Identifier Lineage（标识谱系）` 平台能力 | 部分覆盖 | `08-运行决策与知识包生成.md` 承接运行期消费；`21-断卡排查账户管控核验.md` 只覆盖高风险样板用法 | 建议新增 `iteration-02-runtime-and-governance/08b-统一输入槽位与标识谱系收敛.md` | 是 | 今天仍是平台级缺口 |
| `Time Semantic Selector（时间语义选择器）` 与跨场景澄清规则 | 部分覆盖 | `08`、`15`、`16`、`18`、`21` 分散承接；前端主方案已有统一澄清交互，但缺专题文档 | 建议新增 `iteration-02-runtime-and-governance/08c-时间语义选择与跨场景澄清.md` | 是 | 今天仍是平台级缺口 |
| `LLM（大语言模型）` 隔离、运行时降级与冲突仲裁 | 部分覆盖 | `08` 只承接运行时降级序列；`11c` 只治理 provider / model；前端主方案只定义展示层 | 建议新增 `iteration-02-runtime-and-governance/08d-LLM隔离与运行时降级治理.md` | 是 | 今天仍是平台级缺口 |
| 数据保留、归档与历史回放约束 | 已覆盖 | `11-监控审计与影响分析.md`、`11b-运维验收与上线保障.md` 已吸收 | 继续归属 `11` / `11b` | 否 | 继续按既有场景承接 |

本轮结论收口如下：

1. 今天仍成立的原子能力缺口只有 3 项：`08b`、`08c`、`08d`。
2. `Dictionary` 与 `Join Relation Object` 两项上轮缺口已被既有专题特性文档吸收。
3. 数据保留、归档与历史回放仍不建议新增专题文档，继续归属 `11` / `11b`。

## 3. 最低完备性检查结果

### 3.1 本轮命中的目标特性文档

- `docs/architecture/features/iteration-02-runtime-and-governance/11-监控审计与影响分析.md`
- `docs/architecture/features/iteration-02-runtime-and-governance/12-全局壳层、导航与跨工作台上下文跳转.md`
- `docs/architecture/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发.md`
- `docs/architecture/features/iteration-03-payroll-and-wealth/13-代发明细查询.md`
- `docs/architecture/features/iteration-03-payroll-and-wealth/14-代发批次结果查询.md`
- `docs/architecture/features/iteration-03-payroll-and-wealth/15-基金申购、赎回申请记录查询.md`
- `docs/architecture/features/iteration-03-payroll-and-wealth/16-基金资金交易与分红记录查询.md`
- `docs/architecture/features/iteration-03-payroll-and-wealth/17-SA理财交易查询.md`
- `docs/architecture/features/iteration-04-retail-and-high-risk/18-零售客户基础信息查询.md`
- `docs/architecture/features/iteration-04-retail-and-high-risk/19-零售户口开户机构变更查询.md`
- `docs/architecture/features/iteration-04-retail-and-high-risk/20-客户密码修改日志查询.md`
- `docs/architecture/features/iteration-04-retail-and-high-risk/21-断卡排查账户管控核验.md`

### 3.2 门禁结果

| 特性文档 | 结果 | 缺失原因 | 本轮动作 / 建议下一步 |
| --- | --- | --- | --- |
| `11-监控审计与影响分析.md` | 通过 | 正文已满足指标口径、通用补充项与存储落位边界，本轮仅缺 `对应实施计划` 头注 | 本轮新增 `2026-04-04-monitoring-audit-impact-analysis-implementation-plan.md` 并回填头注 |
| `12-全局壳层、导航与跨工作台上下文跳转.md` | 通过 | 正文已满足指标口径、通用补充项与存储落位边界，本轮仅缺 `对应实施计划` 头注 | 本轮新增 `2026-04-04-global-shell-navigation-context-implementation-plan.md` 并回填头注 |
| `12a-首页总览与状态分发.md` | 通过 | 正文已满足指标口径、通用补充项与存储落位边界，本轮仅缺 `对应实施计划` 头注 | 本轮新增 `2026-04-04-home-overview-status-dispatch-implementation-plan.md` 并回填头注 |
| `13-代发明细查询.md` | 通过 | 已补显式 `通用补充项`、逐指标分子 / 分母 / 统计窗口与存储落位边界；沿用既有实施计划 | 继续沿用 `2026-03-29-payroll-runtime-knowledge-package-implementation-plan.md` |
| `14-代发批次结果查询.md` | 通过 | 已补显式 `通用补充项`、逐指标分子 / 分母 / 统计窗口与存储落位边界；沿用既有实施计划 | 继续沿用 `2026-03-29-payroll-runtime-knowledge-package-implementation-plan.md` |
| `15-基金申购、赎回申请记录查询.md` | 通过 | 本轮补齐 `对应实施计划` 头注、显式 `通用补充项`、逐指标统计口径与存储落位边界 | 本轮新增 `2026-04-04-fund-subscription-redemption-application-query-implementation-plan.md` |
| `16-基金资金交易与分红记录查询.md` | 通过 | 本轮补齐 `对应实施计划` 头注、显式 `通用补充项`、逐指标统计口径与存储落位边界 | 本轮新增 `2026-04-04-fund-cash-transaction-dividend-query-implementation-plan.md` |
| `17-SA理财交易查询.md` | 通过 | 本轮补齐 `对应实施计划` 头注、显式 `通用补充项`、逐指标统计口径与存储落位边界 | 本轮新增 `2026-04-04-sa-wealth-transaction-query-implementation-plan.md` |
| `18-零售客户基础信息查询.md` | 通过 | 本轮补齐 `对应实施计划` 头注、显式 `通用补充项`、逐指标统计口径与存储落位边界 | 本轮新增 `2026-04-04-retail-customer-profile-query-implementation-plan.md` |
| `19-零售户口开户机构变更查询.md` | 通过 | 本轮补齐 `对应实施计划` 头注、显式 `通用补充项`、逐指标统计口径与存储落位边界 | 本轮新增 `2026-04-04-retail-account-opening-branch-change-query-implementation-plan.md` |
| `20-客户密码修改日志查询.md` | 通过 | 本轮补齐 `对应实施计划` 头注、显式 `通用补充项`、逐指标统计口径与存储落位边界 | 本轮新增 `2026-04-04-password-change-audit-query-implementation-plan.md` |
| `21-断卡排查账户管控核验.md` | 通过 | 本轮补齐 `对应实施计划` 头注、显式 `通用补充项`、逐指标统计口径与存储落位边界 | 本轮新增 `2026-04-04-account-control-verification-for-card-risk-investigation-implementation-plan.md` |

## 4. 本轮新增实施计划

本轮为通过最低完备性门禁的特性文档新增 / 确认正式计划如下：

1. `docs/plans/2026-04-04-monitoring-audit-impact-analysis-implementation-plan.md`
2. `docs/plans/2026-04-04-global-shell-navigation-context-implementation-plan.md`
3. `docs/plans/2026-04-04-home-overview-status-dispatch-implementation-plan.md`
4. `docs/plans/2026-04-04-fund-subscription-redemption-application-query-implementation-plan.md`
5. `docs/plans/2026-04-04-fund-cash-transaction-dividend-query-implementation-plan.md`
6. `docs/plans/2026-04-04-sa-wealth-transaction-query-implementation-plan.md`
7. `docs/plans/2026-04-04-retail-customer-profile-query-implementation-plan.md`
8. `docs/plans/2026-04-04-retail-account-opening-branch-change-query-implementation-plan.md`
9. `docs/plans/2026-04-04-password-change-audit-query-implementation-plan.md`
10. `docs/plans/2026-04-04-account-control-verification-for-card-risk-investigation-implementation-plan.md`

## 5. 待补齐项清单

### 5.1 未通过门禁的特性文档

本轮未新增未通过门禁的场景级特性文档；`13-21` 已完成最低完备性补齐。

### 5.2 平台级专题缺口

- `08b-统一输入槽位与标识谱系收敛`
- `08c-时间语义选择与跨场景澄清`
- `08d-LLM隔离与运行时降级治理`

## 6. 建议下一步

1. 下一轮优先新增 `08b`、`08c`、`08d` 三份运行与治理平台专题特性文档，再进入门禁与计划产出。
2. `13-21` 已进入“可按计划实现”的状态，后续自动化可直接选择其中一批进入实现或测试报告阶段。
3. 若继续跑本自动化，下一轮重点观察两件事：`08b/08c/08d` 是否已正式建档，`13-21` 是否进入实现 / 测试验收链路。
