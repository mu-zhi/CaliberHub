# 数据直通车 · 详细设计文档（LLD）

> **目标**：将总体设计（HLD）与 MVP 方案落成可开发、可联调、可验收的实现级设计基线。  
> **范围**：覆盖 M1（口径治理 MVP）的模块拆分、数据结构、接口契约、发布门禁、异常降级、测试与上线验收；包含 M2/M3 预留点但不展开实现。  
> **读者**：后端工程师、前端工程师、测试工程师、技术负责人。  
> **当前阶段定位**：M1 实施与收口文档（Single Source of Truth）。

---

## 1. 设计输入与边界

设计输入：

1. `01-设计文档/03-数据直通车-MVP定义与交付计划.md`
2. `01-设计文档/04-数据直通车-当前阶段架构设计.md`
3. `01-设计文档/05-数据直通车-知识梳理服务方案.md`
4. `01-设计文档/06-数据直通车-CALIBER系统总体设计方案.md`
5. `01-设计文档/08-数据直通车-统一术语表.md`
6. `02-开发文档/01-数据直通车-导入模板定义.json`
7. `02-开发文档/04-数据直通车-接口文档.md`

M1 边界（强约束）：

1. 不做 SQL 执行。
2. 不做审批流程实现。
3. 不做黑盒 NL2SQL（自然语言转 SQL）。
4. 手动录入是主路径，LLM（大语言模型）仅做导入加速器。

---

## 2. 实现总览

M1 落地模块：

1. 场景管理模块：Scene/Domain 的增删改查、发布、弃用、审计。
2. 导入预处理模块：文档分块、LLM/规则抽取、质量评分、草稿落库。
3. 导入任务模块：导入流程步骤机、恢复能力、任务状态流转。
4. SQL 解析增强模块：提取来源表/字段占位符，失败可降级。
5. 搜索模块：标题/描述/SQL 全文检索。
6. 系统管理模块：LLM 配置、提示词配置、运行态测试。
7. 数据地图查询模块：米勒列节点查询、场景血缘查询。

技术形态：

- 后端：Spring Boot 单体。
- 数据库：MySQL（当前实现采用 CLOB/JSON 字符串列）。
- 接口路径：当前 `/api/...`，治理目标 `/api/v1/...`。

---

## 3. 逻辑模型与数据库设计

## 3.1 核心实体

1. `Domain`：业务域容器，承载域级背景知识。
2. `Scene`：最小治理单元，承载口径与取数方案。
3. `Data Retrieval Plan`（取数方案）：`Scene.sqlVariants[]` 子结构。
4. `ImportTask`：导入过程状态机与恢复载体。
5. `LlmPreprocessConfig`：模型参数与提示词配置。

## 3.2 表结构（M1）

- `caliber_domain`
- `caliber_scene`
- `caliber_import_task`
- `caliber_llm_preprocess_config`

说明：`caliber_dict` 已通过迁移引入，当前主要作为 M2 过渡能力，不作为 M1 主流程依赖。

## 3.3 关键字段约束

### 3.3.1 `caliber_scene`

1. `scene_code`：全局唯一。
2. `status`：`DRAFT` / `DISCARDED` / `PUBLISHED`。
3. `sql_variants_json`：M1 取数方案主字段。
4. `quality_json`：质量评分输出载体。
5. `verified_at` + `change_summary`：发布审计必填。

### 3.3.2 `caliber_import_task`

1. 主键：`task_id`。
2. 步骤：`current_step`（1-4）。
3. 状态：`RUNNING/QUALITY_REVIEWING/SCENE_REVIEWING/PUBLISHING/COMPLETED/FAILED`。
4. 结果：`preprocess_result_json` 存放最近导入结果快照。

---

## 4. JSON 契约设计

## 4.1 导入输入契约：`CALIBER_IMPORT_V2`

顶层关键字段：

- `doc_type`
- `schema_version`
- `source_type`
- `global`
- `scenes[]`
- `parse_report`
- `_meta`

`scenes[]` 关键字段：

- `scene_title`
- `scene_description`
- `inputs`
- `outputs`
- `sql_variants[]`
- `code_mappings[]`
- `caveats[]`
- `quality`

`sql_variants[]` 关键字段：

- `variant_name`
- `applicable_period`
- `sql_text`
- `source_tables[]`
- `notes`

M3 预留字段：

- `id_mapping_notes`
- `outputs.fields[].sensitivity_hint`
- `outputs.fields[].mask_rule_suggest`

## 4.2 预处理响应 DTO：`PreprocessResultDTO`

```json
{
  "mode": "llm_enhanced|rule_generated",
  "global": {},
  "scenes": [],
  "quality": {
    "score": 0,
    "hard_missing": [],
    "soft_missing": [],
    "warnings": [],
    "parse_errors": []
  },
  "blocks": [],
  "evidenceMap": {},
  "warnings": [],
  "confidenceScore": 0.0,
  "confidenceLevel": "HIGH|MEDIUM|LOW",
  "lowConfidence": false,
  "totalElapsedMs": 0,
  "stageTimings": [],
  "sceneDrafts": [],
  "importBatchId": "uuid"
}
```

字段约束说明：

1. `blocks + evidenceMap + quality` 为对照视图硬契约。
2. `evidenceMap` 使用驼峰命名；设计文档中的 `evidence_map` 为概念术语。
3. `confidenceScore < 0.70` 必须设置 `lowConfidence=true`。
4. 低置信度策略：允许预创建草稿，但前端必须强提示并要求人工复核后再进入发布动作。

---

## 5. 模块详细设计

## 5.1 场景管理模块

职责：

1. Scene/Domain CRUD。
2. 发布与弃用状态机。
3. 发布前最小单元校验。
4. 操作审计（创建、更新、发布、弃用）。

发布状态机：

1. `DRAFT -> PUBLISHED`：允许。
2. `DRAFT -> DISCARDED`：允许。
3. `PUBLISHED -> DISCARDED`：默认不允许（返回状态冲突）。
4. 删除仅允许 `DRAFT`。

## 5.2 导入预处理模块

流程：

1. 接收 `rawText/sourceType/sourceName/preprocessMode`。
2. 文本规范化与分块。
3. 策略分支：`RULE_ONLY` / `LLM_ONLY` / `AUTO`。
4. 抽取结果归一到 `CALIBER_IMPORT_V2`。
5. SQL 保真校验与解析增强。
6. 质量评分卡生成。
7. 返回 `PreprocessResultDTO`，可选预创建草稿。

## 5.3 导入任务模块

职责：

1. 记录导入任务生命周期。
2. 支持步骤确认与回退。
3. 支持刷新恢复（按 `taskId` 拉取状态）。

步骤定义：

1. Step1：导入并生成草稿。
2. Step2：质量确认。
3. Step3：结果对照确认。
4. Step4：发布处理。

## 5.4 SQL 解析增强模块

输出结构：

```json
{
  "source_tables": ["schema.table"],
  "selected_columns": ["schema.table.col"],
  "filter_columns": ["schema.table.col"],
  "join_pairs": [{"left": "a.id", "right": "b.id"}],
  "placeholders": ["agreement_id", "start_date", "end_date"],
  "parse_errors": []
}
```

策略：

1. Regex（正则表达式）兜底 + AST（语法树）尽力。
2. 解析失败不阻断草稿保存。
3. 无法提取 `source_tables` 时，触发发布硬门禁。

## 5.5 搜索模块

实现策略：

1. M1 使用 MySQL FULLTEXT/LIKE。
2. 统一检索字段为 `search_text`（拼接标题/描述/SQL/码值/注意事项）。
3. 场景保存与发布时刷新索引字段。

## 5.6 系统管理模块

子能力：

1. 大模型配置读写与测试。
2. 模型列表拉取。
3. 提示词模板读写与恢复默认。

提示词占位符规则：

1. 必须包含 `{{RAW_DOC}}`、`{{SOURCE_TYPE}}`。
2. Schema 占位符二选一：`{{DYNAMIC_JSON_SCHEMA}}`（推荐）或 `{{PREP_SCHEMA}}`（兼容）。

---

## 6. 发布门禁与质量规则

## 6.1 硬门禁（阻断发布）

| 规则ID | 规则 | 结果 |
| --- | --- | --- |
| QG-001 | 可执行场景但 `sql_variants` 为空 | 阻断 |
| QG-002 | 任一 `sql_variants[].applicable_period` 为空 | 阻断 |
| QG-003 | 任一 `sql_variants[].source_tables` 为空 | 阻断 |
| QG-004 | `verified_at` 为空 | 阻断 |
| QG-005 | `change_summary` 为空 | 阻断 |

## 6.2 软门禁（告警不阻断）

| 规则ID | 规则 | 结果 |
| --- | --- | --- |
| QG-101 | `caveats` 为空 | 告警 |
| QG-102 | `id_mapping_notes` 为空 | 告警 |
| QG-103 | `code_mappings` 为空 | 告警 |

## 6.3 质量评分结构

- `score`：0-100。
- `hard_missing[]`：硬缺失项。
- `soft_missing[]`：建议补齐项。
- `warnings[]`：通用告警。
- `parse_errors[]`：SQL 解析异常。

---

## 7. 接口实现契约（M1）

主接口分组：

1. 导入：`/api/import/*`
2. 场景：`/api/scenes*`
3. 业务域：`/api/domains*`
4. 数据地图查询：`/api/assets/*`
5. 系统管理：`/api/system/*`

关键契约：

1. 当前兼容路径 `/api/...`。
2. 新治理路径按 `/api/v1/...` 演进。
3. 冲突错误码预留：`CAL-SC-409`、`CAL-SS-409`。

## 7.1 接口-功能-交互对齐矩阵

| 功能 | 后端接口 | 前端交互 | 依赖关系 |
| --- | --- | --- | --- |
| 手动创建场景 | `POST /api/scenes` | 场景创建页提交草稿 | 依赖 Domain 列表接口返回可选业务域 |
| 编辑场景 | `PUT /api/scenes/{id}` | 场景编辑页保存 | 与乐观锁/冲突错误码策略联动 |
| 发布场景 | `POST /api/scenes/{id}/publish` | 发布弹窗填写 `verified_at/change_summary` | 依赖发布门禁与最小单元校验 |
| 对照导入 | `POST /api/import/preprocess` | 左原文右结构化对照视图 | 依赖 `blocks/evidenceMap/quality` 完整返回 |
| 导入流程恢复 | `GET /api/import/tasks/{taskId}` | 页面刷新后恢复步骤与草稿上下文 | 依赖导入任务状态机 |
| 搜索场景 | `GET /api/scenes?keyword=` | 列表检索框实时/手动查询 | 依赖 `search_text` 更新策略 |
| 业务域管理 | `GET/POST/PUT /api/domains` | 业务域管理页与发布业务域下拉 | 依赖 `domain_code` 唯一性校验 |
| 预处理配置 | `GET/PUT /api/system/llm-preprocess-config` | 系统管理配置页 | 依赖角色写权限与配置校验 |
| 提示词管理 | `GET/PUT /api/system/llm-preprocess-config/prompts` | 系统管理提示词页 | 依赖占位符校验规则 |

---

## 8. 安全、审计与可观测

## 8.1 安全

1. 写接口鉴权由 `caliber.security.require-write-auth` 控制。
2. 认证开启后，后端从安全上下文注入操作人。
3. 支持限流错误 `RATE_LIMIT_EXCEEDED`（HTTP 429）。

## 8.2 审计

1. 导入、更新、发布、弃用必须记录审计字段。
2. 发布审计最小集：`verified_at`、`change_summary`。

## 8.3 可观测

1. 导入耗时：`totalElapsedMs` + `stageTimings[]`。
2. LLM 调用：`request_id/model_id/prompt_version/token/latency/error_code`。
3. 导入批次：`importBatchId` 可追踪端到端链路。

---

## 9. 异常与降级设计

## 9.1 LLM 不可用

1. `AUTO` 模式：回退规则抽取。
2. `LLM_ONLY` 模式：返回失败并提示人工切换。
3. 始终保证手动录入路径可用。

## 9.1.1 低置信度处理

1. `lowConfidence=true` 时允许创建/保留草稿。
2. 导入页必须显著提示“需人工复核”并限制直接发布。
3. 复核动作通过对照视图确认后，才允许进入发布校验。

## 9.2 SQL 解析失败

1. 允许保存草稿。
2. 写入 `quality.parse_errors[]`。
3. 如 `source_tables` 缺失，发布时阻断。

## 9.3 大文档超限

1. 超过 10000 行直接拒绝预处理。
2. 返回明确提示“请分段导入”。

## 9.4 并发编辑冲突

1. 建议采用 `row_version` 乐观锁。
2. 版本不匹配返回 `CAL-SC-409`。

---

## 10. 性能与容量基线（M1）

1. `POST /api/import/preprocess`：P95 <= 30s。
2. 列表/详情/搜索：P95 <= 2s。
3. 基线输入：单文档建议 <= 60k 字符。
4. 场景规模：<= 1000 时使用 MySQL 检索。

---

## 11. 测试设计

## 11.1 单元测试

1. Scene 发布门禁规则测试。
2. 导入质量评分计算测试。
3. SQL 解析成功/失败分支测试。

## 11.2 集成测试

1. 导入接口：普通/低置信度/超长文本/LLM 失败回退。
2. 场景接口：创建、更新、发布、弃用、删除。
3. 业务域接口：CRUD。
4. 导入任务接口：状态恢复、步骤确认、回退。

## 11.3 端到端验收（Go/No-Go）

1. 手动创建 Domain/Scene 并保存草稿成功。
2. 可编辑多取数方案与码值说明。
3. 发布必须校验 `verified_at/change_summary`。
4. 导入结果必须返回 `blocks/evidenceMap/quality`。
5. 对照视图支持“接受/编辑/待确认”并记录变更轨迹。
6. LLM 失败时可降级，不阻断主流程。
7. 接口文档与实现一致。

---

## 12. M2/M3 前置预埋

1. 接口版本迁移预案：`/api -> /api/v1`。
2. 结构预埋：`id_mapping_notes`、`alignment_report_id`。
3. 对齐与外部依赖：元数据平台/OA/数据安全/LLM SLA（服务等级协议）参数走配置化。
4. Service Spec 版本冲突语义：`CAL-SS-409`。

---

## 13. 变更控制

1. 任何接口字段变更必须同步 `02-开发文档/04-数据直通车-接口文档.md`。
2. 任何门禁规则变更必须同步本文件与 `02-开发文档/03-数据直通车-开发任务清单.md`。
3. 任何术语变更必须同步 `01-设计文档/08-数据直通车-统一术语表.md`。

---

## 14. M2 详细设计（引用与版本）

## 14.1 目标与边界

目标：

1. 将 Scene 中的共享语义拆分为可复用资产（Dictionary、Semantic View）。
2. 建立引用关系与版本链，支持差异对比和影响判断。
3. 保持 M1 兼容，不破坏既有 `/api/...` 调用链路。

边界：

1. 不做执行侧集成。
2. 不做元数据平台强对齐门禁（延后至 M3）。

## 14.2 新增实体与关系

新增实体：

1. `semantic_view`：语义视图主表。
2. `dictionary`：码值字典主表（由 `code_mappings` 演进）。
3. `asset_version`：资产版本快照表。
4. `asset_reference`：引用关系表（Scene->Dict、Scene->SemanticView）。

关系约束：

1. `scene` 可引用多个 `dictionary` 与 `semantic_view`。
2. `asset_version` 对 `scene/dictionary/semantic_view` 通用。
3. `asset_reference` 需记录 `ref_policy`：`LOCKED/COMPATIBLE/LATEST`。

## 14.3 模块扩展

1. 版本管理模块：生成版本、回放版本、差异比对。
2. 引用管理模块：创建/更新引用，校验引用策略。
3. 共享语义模块：Dict/SV CRUD 与 Scene 联动渲染。

## 14.4 接口契约（M2）

1. `POST /api/scenes/{id}/versions`：创建场景版本。
2. `GET /api/scenes/{id}/versions`：查询版本列表。
3. `GET /api/scenes/{id}/diff?from=&to=`：版本差异。
4. `POST /api/assets/references`：创建引用关系。
5. `GET /api/assets/references?assetType=&assetId=`：查询引用关系。
6. `GET /api/dicts`、`POST /api/dicts`、`PUT /api/dicts/{id}`。
7. `GET /api/views`、`POST /api/views`、`PUT /api/views/{id}`。

## 14.5 门禁与一致性

1. 引用更新与版本快照必须同事务提交。
2. `LOCKED` 策略禁止自动升级引用目标版本。
3. 删除被引用资产时必须先解除引用或执行替换。

## 14.6 测试要点（M2）

1. 版本快照创建与差异对比正确性。
2. 引用策略行为测试（`LOCKED/COMPATIBLE/LATEST`）。
3. 引用循环防护与删除保护。
4. Scene 页面在引用资产缺失时的降级展示。

---

## 15. M3 详细设计（对齐与导出 + 可控取数）

## 15.1 目标与边界

目标：

1. 接入元数据平台完成表/字段对齐。
2. 从发布场景导出 Service Spec 契约。
3. 输出可审计 Plan(IR)，支持执行侧消费。

边界：

1. CALIBER 不直接执行 SQL。
2. 审批流程由 OA 承担，CALIBER 只输出审批建议字段。

## 15.2 新增实体与关系

新增实体：

1. `alignment_report`：对齐结果与告警明细。
2. `service_spec`：执行侧消费契约。
3. `derivation_rule`：参数派生规则。
4. `topic_node`：主题节点与导航索引。
5. `impact_analysis`：变更影响分析结果。

关系约束：

1. `scene` 发布后可生成多个 `service_spec` 版本。
2. `service_spec` 必须绑定 `alignment_report_id`。
3. `derivation_rule` 可被 `service_spec.input_schema` 引用。

## 15.3 模块扩展

1. 对齐模块：调用元数据平台解析 `source_tables` 与字段敏感等级。
2. 导出模块：组装 `Service Spec` 与版本控制。
3. Plan(IR) 模块：检索、路由、抽参、风险门禁、输出审计信息。
4. 影响分析模块：上游结构变更反查受影响场景与 Spec。

## 15.4 接口契约（M3）

1. `POST /api/alignment/check`：触发对齐检查并返回报告。
2. `GET /api/alignment/reports/{id}`：查询对齐报告。
3. `POST /api/service-specs/export/{sceneId}`：导出 Service Spec。
4. `GET /api/service-specs/{specCode}`：查询 Service Spec（含版本）。
5. `POST /api/nl/query`：输出 Plan(IR)（不执行）。
6. `GET /api/impact/analysis?assetType=&assetId=`：查询影响面。

## 15.5 门禁矩阵（M3）

1. `source_tables` 未对齐：阻断 Service Spec 发布。
2. 输出字段缺 `sensitivity/masking`：阻断 Service Spec 发布。
3. `S3` 字段 `masking=NONE` 且无审批凭证：`deny`。
4. Plan(IR) 参数缺失：`deny` 并返回缺失槽位。

## 15.6 Plan(IR) 输出规范

最小输出字段：

1. 命中 `scene/spec` 与版本号。
2. 命中 `plan_variant` 与路由原因。
3. 参数槽位表（值/缺失/来源证据/置信度）。
4. 风险判断（allow/need_approval/deny）与原因。
5. 审计字段（requestId/timestamp/promptVersion）。

## 15.7 测试要点（M3）

1. 对齐失败重试与阻断行为（429/5xx/404/409）。
2. Service Spec 版本冲突（`CAL-SS-409`）。
3. Plan(IR) 参数抽取准确率与拒绝策略正确性。
4. 审计可追溯性（可回放、可解释）。
