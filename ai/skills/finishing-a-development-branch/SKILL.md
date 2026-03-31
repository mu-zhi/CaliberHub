---
name: finishing-a-development-branch
description: "用于全部验证通过后的分支收尾，统一收口为本地合并、推送建 PR、保留分支或丢弃工作四个选项。"
---

# 开发分支收尾（finishing-a-development-branch）

## Preconditions

- 关键测试已通过
- `feature-test-report` 已完成，且不存在未关闭的 `P0/P1` 缺陷
- 服务验活已完成
- `docs/engineering/current-delivery-status.md` 已同步最终状态

## Workflow

1. 汇总四项完成证据：
   - 改动摘要
   - 执行命令
   - 服务状态
   - 剩余风险
2. 仅在验证通过后给出四个标准选项：
   - 本地合并回基线分支
   - 推送并创建 PR
   - 保留当前分支，稍后处理
   - 丢弃本次工作
3. 未通过测试时，不进入分支收尾选项。
