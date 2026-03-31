---
name: frontend-design-governance
description: "用于约束数据直通车前端页面、导航、中文界面口径与设计留痕。"
---

# 前端设计治理（frontend-design-governance）

## Workflow

1. Read these baselines before editing frontend:
   - `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/architecture/frontend-workbench-design.md`
   - `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/architecture/system-design.md` (for current stage boundaries)
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
   - Keep system settings and personal collaboration in the global tools area, outside business workbench main flow.
4. Keep consistency after code changes:
   - If interface behavior changes, sync `SpringDoc（接口文档生成框架，SpringDoc OpenAPI）` / `OpenAPI（开放接口描述规范，OpenAPI Specification）` output or related supplementary notes instead of a fixed-path interface doc.
   - If frontend decision baseline changes, update `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/architecture/frontend-workbench-design.md`.
5. Return result with:
   - changed files
   - applied decisions
   - remaining gaps marked as later construction

## Review Checklist

1. Verify no page uses mixed Chinese-English labels for business-facing terms.
2. Verify no UI copy uses internal level terms such as P0/P1.
3. Verify unimplemented menu or option items have clear status labels and workaround text.
4. Verify top nav and module naming remain: 首页总览、数据地图、知识生产台、发布中心、运行决策台、审批与导出、监控与审计.
5. Verify system settings and personal collaboration remain in the global tools area rather than returning to first-level business navigation.
6. Verify data map pages follow the approved layout pattern and keep current visual style.
