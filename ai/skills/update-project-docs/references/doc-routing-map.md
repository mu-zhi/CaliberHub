# 数据直通车文档路由映射

## 1. 变更类型 -> 目标文档

| 变更类型 | 必更新文档 | 说明 |
| --- | --- | --- |
| 统一方案主线（全链路/PlanIR/样例/门禁） | `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/architecture/system-design.md` | 当前主线方案入口 |
| 项目定位、业务目标、范围调整 | `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/architecture/system-design.md` | 统一写入主文前部章节 |
| 系统模块、流程、架构边界调整 | `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/architecture/system-design.md` | 统一写入主方案运行链路与治理章节 |
| 方案对象定义、运行主线、接口契约、治理边界调整 | `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/architecture/system-design.md` | 不写入前端主文档 |
| 前端页面结构、导航、交互、状态表达、中文界面口径调整 | `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/architecture/frontend-workbench-design.md` | 不回写主方案；前端相关留痕统一收口 |
| 治理先行重设方案（禁引 HLD 场景） | `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/architecture/system-design.md` | 治理主线合并到主方案 |
| 知识梳理服务能力、输入输出、流程调整 | `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/architecture/system-design.md` | 统一写入主方案与运行链路 |
| 术语新增或定义修改 | `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/glossary.md` | 作为术语唯一标准源 |
| 架构规范、命名规范、编码守护规则调整 | `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/engineering/standards/*.md` | 维护开发约束与守护规则 |
| 协作协议、共享触发语、完成证据格式调整 | `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/engineering/collaboration-workflow.md` + `/Users/rlc/LingChao_Ren/1.2、数据直通车/AGENTS.md` | 共享协作入口、方案/执行协议与收尾格式需双写保持一致 |
| 业界调研、竞品分析、外部参考补充 | `/Users/rlc/LingChao_Ren/1.2、数据直通车/research/**/*.md` | 仅放外部参考信息 |

## 2. 每次“更新项目文档”的必做动作

1. 先提炼本次方案增量（新增、调整、下线），再映射文档。
2. 对受影响文档完成内容更新，不创建重复版本文件。
3. 若同一需求同时影响主方案和前端页面，必须拆分更新 `system-design.md` 与 `frontend-workbench-design.md`，不能混写到单一文档。
4. 扫描 `docs`、`research`、`archive` 全量 `.md` 文件做一致性检查。
5. 如发生文档新增、下线、改名、迁移，回写 `/Users/rlc/LingChao_Ren/1.2、数据直通车/README.md` 导航和目录树；如协作入口或共享触发语变化，同步回写 `AGENTS.md`。
6. 设计主入口固定为 `docs/architecture/system-design.md`；前端页面与工作台设计主入口固定为 `docs/architecture/frontend-workbench-design.md`，两者分工明确，不互为并行总方案入口。
7. 输出同步报告：已更新文件、核心变更、已检查未改动文件。

## 3. 一致性检查清单

- 术语是否以“统一术语表”为准
- 英文术语每次出现是否附中文解释（代码常量、接口路径、论文标题除外）
- 是否存在“最终版/修改版/xxx版”等过程后缀
- 战略口径是否保持一致（数据直通车2.0 / 口径治理 / 知识梳理服务）
- 是否存在过时链接或迁移历史描述
