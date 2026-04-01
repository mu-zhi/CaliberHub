# Docs

`docs/` 是数据直通车当前有效项目文档的唯一正文域。

## 目录说明

- `glossary.md`：统一术语表。
- `architecture/`：系统设计、知识图谱概念边界、前端工作台设计、特性文档等正式设计基线。
- `engineering/`：开发手册、协作工作流、当前交付状态、开发规范、实施门禁、工程约束与工程协作能力说明。
- `plans/`：方案过程稿与实施计划，不替代正式主文档。
- `user-guide/`：用户手册和面向使用者的操作文档。
- `assets/`：图示、附件等文档资源。

## 正文路由

1. 系统对象、运行主线、接口契约、治理边界：写入 `architecture/system-design.md`。
2. 主方案旁的独立概念支撑文档，例如知识图谱概念与边界：写入 `architecture/` 同级目录。
3. 前端页面结构、导航、交互、状态表达、状态机展示、中文界面口径：写入 `architecture/frontend-workbench-design.md`。
4. 已从主方案拆出的场景级正式特性文档：写入 `architecture/features/`，并保持与两份主文档一致。
5. 当前开发进度、下个阶段工作与任务接力入口：写入 `engineering/current-delivery-status.md`。
6. 团队开发流程、模型路由、项目级工具入口与维护规则：写入 `engineering/development-manual.md`。
7. 协作协议、开发规范、工程门禁与工程协作能力说明：写入 `engineering/`。
8. 用户手册与操作指南：写入 `user-guide/`。
9. 过程稿与实施计划：写入 `plans/`。
