# 数据直通车

数据直通车是一个文档优先、代码可运行的治理型项目仓库。根目录采用 GitHub 同类项目常见的英文域名分区：项目正文收口到 `docs/`，AI 协作资产收口到 `ai/`，外部研究资料收口到 `research/`，第三方快照、历史归档、工具运行物和生成物各自独立分区。

---

## Root Layout

| 目录 / 文件 | 作用 | 是否正文真源 |
| --- | --- | --- |
| `README.md` | 仓库总入口、快速启动、目录导航 | 是 |
| `AGENTS.md` | 共享协作规则入口 | 是 |
| `docs/` | 当前有效项目文档 | 是 |
| `ai/` | AI 代理、技能、规则、上下文、项目级代理资产 | 否 |
| `research/` | 原始材料、最佳实践、业界调研、论文附件 | 否 |
| `third_party/` | 外部项目快照 | 否 |
| `archive/` | 历史资料、待迁移残留 | 否 |
| `tooling/` | 本地工具运行目录与缓存 | 否 |
| `generated/` | 生成物与中间输出 | 否 |
| `frontend/` | 前端代码与前端构建配置 | 代码域 |
| `backend/` | 后端代码与后端构建配置 | 代码域 |
| `scripts/` | 脚本与自动化入口 | 代码域 |
| `.claude/` `.superpowers/` `.mcp.json` `.vscode/` `.idea/` | 工具与 IDE / 运行时约定目录 | 否 |

---

## Canonical Docs

1. [Docs Home](docs/README.md)
2. [Glossary](docs/glossary.md)
3. [System Design](docs/architecture/system-design.md)
4. [Knowledge Graph Concepts and Boundaries](docs/architecture/knowledge-graph-concepts-and-boundaries.md)
5. [Frontend Workbench Design](docs/architecture/frontend-workbench-design.md)
6. [Feature Docs Index](docs/architecture/features/README.md)
7. [Collaboration Workflow](docs/engineering/collaboration-workflow.md)
8. [Development Manual](docs/engineering/development-manual.md)
9. [Current Delivery Status](docs/engineering/current-delivery-status.md)
10. [ChatGPT Browser Bridge Capability](docs/engineering/chatgpt-browser-bridge-capability.md)
11. [Plans README](docs/plans/README.md)
12. [Research README](research/README.md)
13. [AI README](ai/README.md)

---

## Local Development

### Backend

```bash
bash scripts/start_backend.sh
```

默认地址：`http://127.0.0.1:8082`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://127.0.0.1:5174`

后端启动前提：`scripts/start_backend.sh` 会读取仓库根目录 `.env.local`，至少需要提供 `CALIBER_JWT_SECRET` 与本地联调用账号口令环境变量。

### Recommended Verification

```bash
bash scripts/run_system_test_flow.sh
```

NFR 门禁：

```bash
bash scripts/run_nfr_acceptance_gate.sh
```

---

## Manual Source Rules

1. 用户手册的人工维护源头固定为 `docs/user-guide/`。
2. `frontend/public/user-manual/` 与 `backend/src/main/resources/static/user-manual/` 视为发布镜像路径，不作为并行编辑入口。
3. `generated/`、`frontend/dist/`、`backend/target/` 等目录中的文档或产物不纳入人工维护真源。
4. 如用户手册正文发生变更，应先修改 `docs/user-guide/`，再执行同步。

---

## Current Directory Structure

```text
数据直通车/
├── AGENTS.md
├── README.md
├── ai/
│   ├── README.md
│   ├── agents/
│   ├── contexts/
│   ├── hooks/
│   ├── project/
│   ├── rules/
│   └── skills/
├── archive/
│   ├── README.md
│   └── pending-migration/
├── docs/
│   ├── README.md
│   ├── glossary.md
│   ├── architecture/
│   │   └── features/
│   ├── engineering/
│   ├── plans/
│   └── user-guide/
├── generated/
│   ├── README.md
│   ├── dist/
│   └── output/
├── research/
│   ├── README.md
│   ├── best-practices/
│   ├── industry-research/
│   ├── papers/
│   └── source-materials/
├── third_party/
│   ├── README.md
│   └── mirofish/
├── tooling/
│   ├── README.md
│   ├── claude-2-config/
│   ├── codex-tools/
│   └── playwright-cli/
├── backend/
├── frontend/
└── scripts/
```

---

## 项目文档同步更新规范（12条，生效版）

1. 项目当前有效正文域固定为 `docs/`；外部参考固定为 `research/`；AI 协作资产固定为 `ai/`。
2. 根目录一级分区统一采用英文域名风格，不再新增 `其他文档`、`待整理`、`杂项` 一类兜底目录。
3. `docs/` 是唯一项目正文真源；`research/`、`third_party/`、`archive/`、`tooling/`、`generated/` 不得冒充正式方案入口。
4. 一个主题只保留一份主文档，禁止并行维护多个主版本。
5. 禁止使用“最终版”“修改版”“新版”等过程性后缀。
6. 英文术语、缩写、变量名、常量名、指标名在文档中每次出现都必须附中文解释。
7. 术语以 [docs/glossary.md](docs/glossary.md) 为唯一标准；新增术语时，必须先补术语表，再写入其他文档。
8. 文档路由固定为：系统对象、运行主线、接口契约、治理边界写入 [docs/architecture/system-design.md](docs/architecture/system-design.md)；前端页面结构、导航、交互、状态表达、状态机展示、中文界面口径写入 [docs/architecture/frontend-workbench-design.md](docs/architecture/frontend-workbench-design.md)；已从主方案拆出的场景级特性文档写入 [docs/architecture/features/README.md](docs/architecture/features/README.md) 所在目录；开发规范、协作协议、实施约束写入 `docs/engineering/`；用户手册写入 `docs/user-guide/`；外部样例与调研写入 `research/`。
9. 战略口径固定：`数据直通车2.0` 是远期蓝图，`口径治理` 是基础，`知识梳理服务` 是口径治理的大模型场景。
10. 改名、迁移、新增、下线文档时，必须同步修复链接，并更新 `README.md`、`AGENTS.md`、`docs/README.md` 与相关域 README。
11. AI 相关代理、技能、规则、上下文、项目级代理资产统一维护在 `ai/`；项目正文不得混入这些实现资产。
12. 主文档中不保留“旧目录迁移过程”“历史演进整理中”等过程性叙述；历史资料如需保留，统一下沉到 `archive/`。

## 接口文档维护规范（强制）

1. 当前仓库未单独维护固定路径的接口文档文件，接口变更优先通过 `SpringDoc（接口文档生成框架，SpringDoc OpenAPI）` / `OpenAPI（开放接口描述规范，OpenAPI Specification）` 方式保持可追溯。
2. 如后续恢复独立接口文档，必须正式落到 `docs/engineering/`，并同步更新本 README 导航。
3. 后端接口变更时，路径、方法、参数、返回、错误码必须同步更新对应接口说明或自动生成配置。
4. 控制器代码变更与接口说明不一致时，不允许提测。
5. 接口说明仍是前后端联调与测试验收的协作基线。
