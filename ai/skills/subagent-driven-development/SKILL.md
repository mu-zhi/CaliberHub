---
name: subagent-driven-development
description: "用于按计划逐任务推进实现，默认每次只推进一个小任务，并在任务之间执行规格符合性检查和代码质量检查。"
---

# 子代理驱动开发（subagent-driven-development）

## Use when

- 用户说“按方案实现”
- 设计与实施计划均已确认
- 任务适合逐项推进而不是一次性批量落地

## Workflow

1. 读取特性文档与实施计划，锁定当前要做的单个任务。
2. 先执行 `test-driven-development`，明确失败测试。
3. 完成当前任务的最小实现。
4. 任务完成后执行两段检查：
   - 规格符合性检查
   - 代码质量检查（触发 `requesting-code-review`）
5. 关闭当前任务问题后，才进入下一任务。
6. 每完成一批任务同步 `docs/engineering/current-delivery-status.md` 的：
   - `当前状态`
   - `最新完成`
   - `下一动作`
   - `最后更新时间`
7. 全部任务完成、状态文档同步后，触发 `code-reviewing` 做整体代码检视；检视通过后进入 `feature-test-report`，不允许直接进入分支收尾。

## Model routing

推进开发计划固定采用以下模型约定：

1. 主模型：`gpt-5.2-codex + high`
2. 高风险升级：`gpt-5.2-codex + xhigh`
3. 备选模型：`qwen3-coder-plus`

满足以下任一条件时，主模型从 `high` 升到 `xhigh`：

1. 跨 `5` 个以上文件
2. 同一问题两轮修复仍失败
3. 涉及公共接口、数据契约或核心状态机
4. 需要先读大量上下文再改动

若需要切到本地 `Claude Code` 备选链路，统一走：

```bash
bash scripts/claude-coder.sh
```

## Guardrails

1. 不跳过计划直接实现。
2. 不并行推进多个未关闭问题的任务。
3. 若中途发现目标变化，退回 `brainstorming`。
4. 自动化只允许更新当前工作项的事实字段，不自动改 `来源设计`、`来源计划` 与 `退出条件`。
