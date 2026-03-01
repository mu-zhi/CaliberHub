---
name: frontend-design-governance
description: "Capture and enforce 数据直通车 frontend design decisions with Chinese UX conventions. Use when requests mention 前端, 页面, 界面, UI, 交互, 导航, 样式, 图谱, 组件, 或前端评审/改造. Keep implementation aligned with the approved layout and always sync decision traces to project docs."
---

# Frontend Design Governance

## Workflow

1. Read these baselines before editing frontend:
   - `/Users/rlc/Code/数据直通车/02-开发文档/05-数据直通车-前端设计留痕与执行约束.md`
   - `/Users/rlc/Code/数据直通车/03-其他文档/前端设计审查报告.md`
   - `/Users/rlc/Code/数据直通车/01-设计文档/03-数据直通车-MVP定义与交付计划.md` (for current stage boundaries)
2. Extract change deltas from user request into five buckets:
   - 导航与路由
   - 页面布局与交互
   - 文案与术语
   - 能力状态标注
   - 数据地图图谱展示
3. Apply hard constraints during implementation:
   - Keep top navigation labels in Chinese business wording.
   - Use single-column step flow for primary workflows; avoid parallel primary task columns.
   - Edit draft forms instead of exposing raw JSON editing as primary path.
   - Show explanatory text in low-attention style; prioritize task controls.
   - Mark only unimplemented capabilities with clear fallback guidance.
   - Keep system management outside business workbench main flow.
4. Keep consistency after code changes:
   - If interface behavior changes, update `/Users/rlc/Code/数据直通车/02-开发文档/04-数据直通车-接口文档.md`.
   - If frontend decision baseline changes, update `/Users/rlc/Code/数据直通车/02-开发文档/05-数据直通车-前端设计留痕与执行约束.md`.
5. Return result with:
   - changed files
   - applied decisions
   - remaining gaps marked as later construction

## Review Checklist

1. Verify no page uses mixed Chinese-English labels for business-facing terms.
2. Verify no UI copy uses internal level terms such as P0/P1.
3. Verify unimplemented menu or option items have clear status labels and workaround text.
4. Verify top nav and module naming remain: 首页、口径治理、数据地图、个人中心.
5. Verify data map pages follow the approved layout pattern and keep current visual style.
