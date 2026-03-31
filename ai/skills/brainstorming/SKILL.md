---
name: brainstorming
description: "用于新需求、行为变更、复杂修复或实现路径不清时做方案澄清、选项比较和设计确认；确认前不进入实现。"
---

# 方案脑暴（brainstorming）

## Use when

- 用户提到“做方案”“方案设计”“需求收敛”“实现路径选择”
- 任务涉及新功能、行为变更、跨模块改动或复杂修复
- 需求边界、成功标准或治理影响尚不清晰

## Workflow

1. 先收敛四件事：
   - 目标与成功标准
   - 业务边界与不做什么
   - 风险与约束
   - 需要回写的正式文档路径
2. 至少给出 2 至 3 种方案，明确取舍。
3. 未经用户确认，不进入计划拆解或实现。
4. 设计确认后，按项目文档路由回写：
   - 前端结构、导航、交互：`docs/architecture/frontend-workbench-design.md`
   - 系统对象、运行主线、契约边界：`docs/architecture/system-design.md`
   - 场景级展开：`docs/architecture/features/`
5. 同步 `docs/engineering/current-delivery-status.md` 的来源设计、当前状态与下一动作。

## Output

Return:

1. 需求理解
2. 方案选项与权衡
3. 推荐方案
4. 应回写的正式文档
5. 下一步是写计划还是继续补方案
