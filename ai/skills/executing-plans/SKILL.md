---
name: executing-plans
description: "用于用户明确要求批量执行实施计划时，按批次推进任务并设置检查点，避免一次性跑完整个计划后再集中补救。"
---

# 批量执行计划（executing-plans）

## Use when

- 用户明确要求批量执行
- 用户要求独立会话一次跑完整个计划
- 任务之间耦合较低，适合按批次交付

## Workflow

1. 按实施计划将任务分成若干批次。
2. 每个批次内部仍执行 `test-driven-development`。
3. 每个批次结束后做一次：
   - 规格符合性检查
   - 代码质量检查
   - 服务验活
4. 发现风险累积时，暂停后续批次，先修复当前问题。
5. 每个批次更新 `docs/engineering/current-delivery-status.md` 的：
   - `当前状态`
   - `最新完成`
   - `下一动作`
   - `最后更新时间`
6. 最后一批任务完成、状态文档同步后，触发 `code-reviewing` 做整体代码检视；检视通过后进入 `feature-test-report`，不允许直接进入分支收尾。

## Model routing

批量推进开发计划与逐任务推进共用同一模型约定：

1. 主模型：`gpt-5.2-codex + high`
2. 高风险升级：`gpt-5.2-codex + xhigh`
3. 备选模型：`qwen3-coder-plus`

若主模型额度不足、provider 不可用或长时间超时，可切到本地备选：

```bash
bash scripts/claude-coder.sh
```

## Guardrails

1. 批量执行不等于跳过门禁。
2. 测试、审查、服务验活必须按批次做，不允许全部堆到最后。
3. 自动化只允许更新当前工作项的事实字段，不自动改 `来源设计`、`来源计划` 与 `退出条件`。
