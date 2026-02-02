# CaliberHub 口径知识资产治理 Demo（P0）需求文档 + 任务清单

## 1. 背景与问题陈述

银行大数据部门公共支持组长期承担分行临时取数支持（工单审批→支持组写 SQL→数据湖 GaussDB 查询→交付）。10 年累计大量重复场景与口径知识，但当前口径文档存在：

* 文档自由式混杂（SQL + 中文解释 + 列表 + 备注），**难读、难检索、难治理**
* RAG 机器可用性差：切块碎片化、召回不稳定、容易引用过期口径
* 缺少版本/审计/复核闭环：更新时间≠有效验证，变更不可追溯
* 模板供给侧瓶颈：支持组难覆盖复杂业务，未来需开放给业务参与

因此需要构建一个"小而真闭环"的线上系统 Demo：**结构化编辑→lint 校验→发布→版本/审计→导出 doc.json + chunks.json**，并强制对接行内元数据平台（表/字段/敏感等级）。

---

## 2. 目标与非目标

### 2.1 目标（P0）

* 以"**业务场景 Scene**"为最小治理单元，提供结构化表单维护口径知识
* v0.1 **强保证 SQL 表名抽取**，并可视化展示匹配结果
* 对接行内元数据平台：表/字段可选择，敏感等级可回填
* 提供 lint 门禁（阻断/警告）与发布复核字段（last_verified）
* 发布生成标准产物：**doc.json + chunks.json**（页面可读 + 机器可用）
* 全流程可追溯：版本快照 + 审计日志落库（UI 可后做）

### 2.2 非目标（P0 不做）

* 不做大而全的企业级权限体系/SSO（但必须有 actor 身份）
* 不做字段级 SQL 强解析（字段抽取仅可作为提示，P0 不依赖）
* 不做"批量导出中心""向量库入库""智能问答"
* 不做模板执行引擎（仅先治理口径知识资产）

---

## 3. 术语与对象定义（必须统一口径）

* **Domain（业务领域）**：分类/聚合层，用于导航与组织场景（不作为最小治理对象）
* **Scene（业务场景）**：最小治理单元；一个可复用口径对应一个 Scene
* **SceneVersion（场景版本）**：场景内容快照（草稿/已发布），用于回溯与导出
* **SqlBlock（SQL 方案块）**：一个场景可有多段 SQL（Step1/Step2/不同系统方案）
* **SourceTables（数据来源表汇总）**：场景级从所有 SQL 块抽取并汇总的表清单（按版本绑定）
* **SensitiveField（敏感字段清单）**：从元数据字段中选择敏感字段，并填写 `mask_rule`（按版本绑定）
* **last_verified**：口径"最后验证日期"，发布必填，证明可信与可追责
* **doc.json**：结构化权威文档（可渲染/可机器读）
* **chunks.json**：面向 RAG 的分块检索单元

---

## 4. 角色与权限（P0 简化但不能没有）

> P0 可不接登录，但必须有"操作者身份"，否则审计链断。

### 4.1 用户角色（概念）

* **Editor（编辑者）**：创建/编辑草稿、运行 lint、提交发布
* **Publisher（发布者/复核者）**：发布时填写 last_verified、变更摘要（P0 可与 Editor 同人）
* **Viewer（阅读者）**：只读查看已发布版本、导出 JSON

### 4.2 身份获取（P0 实现约束）

* 后端通过 HTTP Header 获取 `X-User`（如 `zhangsan`），无则默认 `demo_user`
* 所有写操作必须记录 `actor` 到 `audit_log`（必填）

---

## 5. 范围与验收标准

## 5.1 P0 最小验收清单（8 条，必须全部满足）

| # | 验收项 | 描述 |
|---|--------|------|
| 1 | 能新建场景（草稿） | 创建新场景，自动生成草稿版本 |
| 2 | 能编辑核心字段 | 基本信息、业务定义、输入参数、多 SQL 块、注意事项 |
| 3 | SQL 块能自动抽取表名（强保证） | v0.1 必须给出抽取结果，失败时有 fallback |
| 4 | 抽取表名能去元数据平台匹配 | 成功/失败可见（MATCHED/NOT_FOUND/BLACKLISTED） |
| 5 | 场景级汇总"数据来源表" | 支持列表搜索 + lint |
| 6 | 能维护敏感字段清单并填写 mask_rule | 合规必需 |
| 7 | 发布弹窗必填 last_verified + 变更摘要 | 产生版本 + 审计日志（可追溯） |
| 8 | 发布后自动生成 doc.json + chunks.json | 能下载/复制（RAG 输出） |

---

## 6. 页面清单与信息架构（P0）

### 6.1 P0 页面（3 页 + 2 组件）

| 页面/组件 | 路由 | 描述 |
|-----------|------|------|
| 场景库列表 | `/scenes` | 入口，找得到、知道哪个可信 |
| 场景编辑器 | `/scenes/:id/edit` | 表单维护 + lint + 表名抽取 + 发布导出 |
| 场景详情 | `/scenes/:id` | 人读主页面 + 版本下拉 + 导出按钮 |
| 元数据选择抽屉 | 组件 | 搜索表 → 选择表/字段 |
| 发布确认弹窗 | 组件 | last_verified + 变更摘要 +（可选）导出预览 |

### 6.2 P0 后端必须具备但 UI 可延后的能力

* 版本管理：save→draft_version；publish→published_version（`/versions` 页面可 P1）
* 审计日志：每次操作写 `audit_log`（`/audit` 页面可 P2）
* RAG 导出：发布时自动生成 doc.json + chunks.json（批量导出中心可 P1）

---

## 7. 页面与交互详细需求（P0）

## 7.1 页面 A：场景库列表 `/scenes`

### 列表字段（必须）

| 字段 | 描述 |
|------|------|
| 场景标题 | 场景名称 |
| 所属领域 | Domain |
| 状态 | 草稿/已发布/已废弃 |
| last_verified | 过期高亮 |
| 敏感标记 | 🔒含敏感字段/无 |
| 当前版本号 | v1.0 等 |
| 最近更新人/时间 | 可选但建议 |
| 操作 | 查看 / 编辑 / 废弃 |

### last_verified 高亮规则（必须）

| 状态 | 颜色 | 条件 |
|------|------|------|
| ✅ 有效 | 绿色 | ≤ 90 天 |
| ⚠️ 即将过期 | 黄色 | 90~365 天 |
| ❌ 已过期 | 红色 | > 365 天 |

### 筛选器（必须）

* 业务领域
* 状态
* 是否过期（仅看已过期）
* 是否含敏感字段

### 操作规则（必须）

| 状态 | 可操作 |
|------|--------|
| 草稿 | 编辑 / 废弃 |
| 已发布 | 查看 / 编辑（创建新草稿版本）/ 废弃 |
| 已废弃 | 仅查看（可恢复作为 P1） |

> 约束：不提供硬删除；统一软删除（废弃）保留审计可追溯。

---

## 7.2 页面 B：场景编辑器 `/scenes/:id/edit`（核心）

### 布局（必须）

三栏布局：

* 左：章节目录锚点
* 中：表单卡片（区块）
* 右：辅助面板（Lint 结果 / 表名抽取结果 / 敏感提醒 / 完成度）

### 表单区块（P0：10 个，其中 5、6、9 为关键）

| # | 区块 | 优先级 |
|---|------|--------|
| 1 | 基本信息 | P0 |
| 2 | 口径定义 | P0 |
| 3 | 输入与限制 | P0 |
| 4 | 输出字段（仅摘要） | P0 |
| 5 | **数据来源（表）汇总** | P0 关键 |
| 6 | **敏感字段与脱敏规则** | P0 关键 |
| 7 | SQL 方案（多块） | P0 |
| 8 | 注意事项/常见坑 | P0 |
| 9 | **复核信息（发布弹窗）** | P0 关键 |
| 10 | Lint 结果（右侧面板） | P0 |

### 区块 5：数据来源（表）汇总（P0）

**字段列**

| 字段 | 描述 |
|------|------|
| 表名 | `schema.table` |
| 匹配状态 | ✅MATCHED / ❌NOT_FOUND / 🚫BLACKLISTED / ⚠️VERIFY_FAILED |
| 是否关键表 | 默认抽取到的勾上 |
| 用途 | FACT/DIM/LOG/INTERMEDIATE 或自由文本 |
| 分区字段 | 可选 |
| 来源 | EXTRACTED/MANUAL |

**交互**

* 从所有 SQL 块抽取表名 → 调元数据平台匹配 → 汇总展示
* 提供"一键导入"把抽取结果写入表汇总
* 允许手工新增表（MANUAL）用于补漏
* 允许对"未匹配表"进行人工修正绑定

**门禁**

* 未匹配/黑名单/校验失败：阻断发布（可保存草稿）

### 区块 6：敏感字段与脱敏规则（P0）

**字段列**

| 字段 | 描述 |
|------|------|
| 字段 | 从已选表字段中选 |
| 敏感等级 | 系统回填 |
| mask_rule | 必填，枚举 |
| 备注 | 可选 |

**门禁**

* 选择了敏感字段但缺 mask_rule：阻断发布
* `has_sensitive` 由该清单自动推导（不允许手动改）

### 右侧辅助面板（P0）

* Lint 结果：错误/警告列表，点击跳转定位字段路径
* 表名抽取结果：✅/❌/🚫/⚠️ 状态可见
* 敏感字段提醒：显示"需要填 mask_rule 的字段"
* 完成度：按区块统计必填项完成度

---

## 7.3 发布确认弹窗（P0 必做）

**触发**：点击"提交发布"

**必填**：

* last_verified 日期
* 验证人（默认当前 `X-User`）
* 变更摘要（change_summary）

**推荐**：

* 验证说明（工单号/测试用例/对账方式）

**发布后必须产生**：

* 新 `PUBLISHED` 版本快照（不可变）
* audit_log 记录（action=PUBLISH）
* `scene_version_export` 生成 doc.json + chunks.json

---

## 7.4 页面 C：场景详情 `/scenes/:id`（只读）

**顶部信息（必须）**：

* 面包屑：Domain > Scene Title
* 状态：已发布/草稿/已废弃
* 验证信息：last_verified + verified_by
* 敏感标记：🔒含敏感字段
* 关键表 chips（可复制）
* 右上按钮：编辑 / 版本下拉 / 导出下拉（doc.json、chunks.json）

**版本下拉（P0 必做）**

* 至少支持切换：当前发布版本（+可能的草稿）
* 详情展示内容应随版本切换而切换（历史回放能力证明）

**导出（P0 必做）**

* 下载 doc.json
* 下载 chunks.json
* （可选）复制到剪贴板

---

## 8. 工作流与状态机（后端必须固化）

### 8.1 生命周期（Scene 层）

```
ACTIVE ←→ DEPRECATED
         ↑
      lifecycle_status
```

* ACTIVE：正常使用
* DEPRECATED：废弃（软删除），仅可查看（不参与检索/默认隐藏）

### 8.2 版本状态（SceneVersion 层）

```
DRAFT → PUBLISHED
  ↑         ↓
  └─ 复制生成新草稿 ─┘
```

* DRAFT：当前可编辑草稿（同一 scene 仅一个 current draft）
* PUBLISHED：已发布快照（同一 scene 仅一个 current published）

### 8.3 编辑与发布规则（必须）

* 编辑已发布版本 = **复制当前发布版本生成新草稿**（不允许直接改 published）
* 发布草稿：
  * lint 必须通过（无 Error）
  * 生成新 published_version（version_seq 递增）
  * 旧 published_version.is_current=0，新 published_version.is_current=1
  * 生成导出物并固化
  * 写 audit_log

---

## 9. Lint 引擎（P0 必须实现）

### 9.1 严重性统一

* **Error（阻断）**：合规/可信度硬门槛
* **Warning（警告）**：质量建议，不阻断发布

### 9.2 P0 规则清单

#### Error（阻断）

| 规则ID | 描述 |
|--------|------|
| E001 | 缺必填项（标题/领域/负责人/场景描述/口径定义/至少一段 SQL） |
| E002 | SQL 抽取到的表名在元数据平台 **NOT_FOUND**（未匹配） |
| E003 | SQL 抽取到的表命中 **BLACKLISTED** |
| E004 | 已选择的敏感字段缺 mask_rule |
| E005 | 发布时缺 last_verified/verified_by |
| E006 | 发布时缺变更摘要 |

#### Warning（警告）

| 规则ID | 描述 |
|--------|------|
| W001 | SQL 使用 SELECT * |
| W002 | SQL 没有分区/日期条件（性能风险） |
| W003 | 注意事项为空 |
| W004 | last_verified 距今 > 365 天（提醒复核） |
| W005 | 涉及敏感字段但无合规说明 |
| W006 | 已选表中存在敏感字段，但敏感字段清单为空（仅提醒） |

### 9.3 Lint 输出结构

```json
{
  "passed": true,
  "errors": [{"id": "E001", "message": "...", "path": "title", "block_id": null}],
  "warnings": [{"id": "W001", "message": "...", "path": "sql_blocks[0].sql", "block_id": "blk-001"}]
}
```

---

## 10. SQL 表名抽取服务（v0.1 强保证）

### 10.1 强保证的定义（写进验收）

* 系统必须给出"抽取结果列表"（可为空，但必须明确说明解析失败原因）
* 解析失败时必须 fallback，并允许用户在"数据来源汇总"手工补充/修正
* 抽取范围：FROM/JOIN 后出现的表名 token（支持 schema.table）
* 不要求字段级解析

### 10.2 实现策略（推荐）

* 优先 SQL Parser（如 JSqlParser）
* 失败 fallback：正则/状态机扫描 FROM/JOIN token
* 过滤：
  * 忽略子查询 `(select ...) alias`
  * 忽略 CTE 名称（WITH t AS (...)）
  * 去除引号/反引号/方括号
  * 规范化大小写

### 10.3 抽取结果字段

| 字段 | 描述 |
|------|------|
| table_fullname | 表全名 |
| source | EXTRACTED |
| match_status | 待匹配（由元数据匹配服务填充） |
| block_id | 来自哪个 SQL 块（可写在 extra_json） |

---

## 11. 元数据平台对接（P0）

### 11.1 必须具备的能力

* 表搜索（关键字 → 返回候选表列表）
* 表详情（schema.table → 返回表 ID、描述、字段列表、字段敏感等级）
* 字段搜索/筛选（在已选表内选字段）

### 11.2 缓存与降级（必须写进实现）

* `metadata_table_cache` 表缓存表/字段 JSON，带 expires_at
* 匹配状态区分：

| 状态 | 描述 |
|------|------|
| MATCHED | 正常匹配 |
| NOT_FOUND | 确实不存在/无法匹配（阻断发布） |
| BLACKLISTED | 命中黑名单（阻断发布） |
| VERIFY_FAILED | 元数据 API 超时/失败（允许保存草稿，但发布阻断） |

### 11.3 黑白名单

* P0 最少支持：黑名单前缀/正则（如 TMP_、个人库）
* 黑名单命中直接 `match_status=BLACKLISTED`

---

## 12. 导出契约（P0：doc.json + chunks.json）

> P0 的导出目标不是"好看"，是**稳定、可追溯、可过滤、可引用**。

### 12.1 doc.json（权威结构化文档）

**必须包含**：

* 稳定 doc_id：`scene_code`
* scene 信息：title、domain、tags
* version 信息：version_id/version_seq/version_label/published_at/change_summary
* governance：owner、contributors、last_verified、has_sensitive、sensitive_fields(mask_rule)
* data_sources：tables（含 match_status）
* content：表单结构化内容（scene_description、definition、inputs、sql_blocks、caveats…）
* lint：lint 结果留痕（可选但建议）

### 12.2 chunks.json（RAG 分块）

* 按区块切：description/definition/inputs/tables/sensitive/sql_blocks/caveats
* 每个 chunk 必须带：
  * chunk_id（建议可重建：doc_id::version_label::section::seq）
  * section、content_type（text/sql）、text、metadata（domain/tags/tables/has_sensitive/last_verified/source_path）

### 12.3 生成时机与落库

* 发布时生成，并写入 `scene_version_export`：
  * doc_json（TEXT）
  * chunks_json（TEXT）
  * chunk_count
  * generated_at/by
* 查看页导出按钮直接读取该表（避免每次现算导致不一致）

---

## 13. 数据模型（P0）——研发落库契约

### 必须表

| 表名 | 描述 |
|------|------|
| domain | 业务领域字典 |
| scene | 场景主表（含稳定 `scene_code`） |
| scene_version | 草稿/发布快照 |
| scene_version_table | 按版本绑定的数据来源表 |
| scene_version_sensitive_field | 按版本绑定的敏感字段 |
| audit_log | append-only 审计日志 |
| metadata_table_cache | 缓存与降级 |
| scene_version_export | 发布导出固化 |

---

## 14. API 契约（P0 最小接口清单）

### 14.1 场景与版本

| Method | Path | 描述 |
|--------|------|------|
| GET | /api/domains | 查询领域列表 |
| GET | /api/scenes | 查询场景列表（支持 filter） |
| POST | /api/scenes | 新建场景 + 创建草稿 |
| GET | /api/scenes/{sceneCode} | 获取场景基本信息 + 当前版本摘要 |
| GET | /api/scenes/{sceneCode}/draft | 获取当前草稿（无则复制发布版本生成） |
| PUT | /api/scenes/{sceneCode}/draft | 保存草稿 |
| POST | /api/scenes/{sceneCode}/lint | 运行 lint |
| POST | /api/scenes/{sceneCode}/publish | 发布 |
| POST | /api/scenes/{sceneCode}/deprecate | 废弃 |
| GET | /api/scenes/{sceneCode}/versions | 查询历史版本 |
| GET | /api/scenes/{sceneCode}/versions/{versionId} | 查询指定版本 |

### 14.2 元数据

| Method | Path | 描述 |
|--------|------|------|
| GET | /api/metadata/tables/search | 表搜索 |
| GET | /api/metadata/tables/{schema}.{table} | 表详情 + 字段列表 |
| GET | /api/metadata/fields/search | 字段搜索（可选） |

### 14.3 导出

| Method | Path | 描述 |
|--------|------|------|
| GET | /api/scenes/{sceneCode}/export/doc | 下载 doc.json |
| GET | /api/scenes/{sceneCode}/export/chunks | 下载 chunks.json |

### 14.4 通用约定

* 请求 header：`X-User: zhangsan`
* 错误返回统一：`{code, message, detail?, traceId?}`

---

## 15. 非功能需求（P0 最小但必须）

| 项目 | 要求 |
|------|------|
| 一致性 | 发布导出物与发布版本一一对应（version_id 绑定） |
| 可追溯 | 任何写操作必须有 audit_log |
| 安全 | 禁止文档里出现真实敏感数据样例 |
| 可用性 | 元数据平台不可用时允许保存草稿，但发布阻断 |
| 可维护性 | lint 规则以配置/注册方式实现，便于增量扩展 |

---

## 16. 迁移策略（P0 的现实落地）

* 首批只迁移 Top 高频场景
* 迁移方式：
  * 人工复制+结构化录入（P0 默认）
  * "旧文档粘贴导入"作为 P1（能省大量人力，但不阻挡 P0）
* 迁移成功标准：能通过 lint + 发布 + 导出 + 可回放版本

---

# 17. P0 任务清单（WBS）

---

## 17.0 方案冻结与契约对齐（必须先完成）

* [ ] 冻结 P0 验收 8 条（本 PRD）
* [ ] 冻结 DB Schema（附录 A）与导出契约（附录 B）
* [ ] 冻结 lint 规则清单（E/W）与错误码规范
* [ ] 冻结元数据对接返回字段（table_id/fields/sensitivity）
* [ ] 冻结 `scene_code` 规则（生成/唯一性/是否允许修改）

**验收**：研发/产品/使用方共同确认"数据契约"和"门禁策略"。

---

## 17.1 后端工程初始化与数据层

* [x] Spring Boot 工程骨架（分层：controller/app/domain/infra）
* [x] SQLite 初始化与迁移脚本（执行附录 A DDL）
* [x] 种子数据：domain 初始化（至少 3 个领域用于演示）
* [ ] 基础中间件：统一异常处理、统一返回体、`X-User` 提取

**验收**：能启动服务并完成健康检查；数据库表可自动创建。

---

## 17.2 核心领域模型与版本/审计流水线（P0 必须落库）

* [x] Scene 聚合根：创建、废弃（软删除）
* [x] SceneVersion：草稿保存、发布生成、current 标记切换
* [ ] 审计日志：CREATE_SCENE / SAVE_DRAFT / RUN_LINT / PUBLISH / DEPRECATE 写入 audit_log
* [ ] published 版本不可变（服务层约束 + 可选 DB trigger）

**验收**：对同一 scene 连续发布 2 次，能在库中看到两条 published_version 且回放一致；审计记录完整。

---

## 17.3 SQL 表名抽取服务（v0.1 强保证）

* [ ] 实现 parser 优先 + fallback 抽取
* [ ] 支持多 sql_blocks 分别抽取并汇总
* [ ] 输出标准：抽取到的表名数组 + 抽取状态（成功/部分/失败原因）
* [ ] 单元测试用例覆盖：JOIN/子查询/CTE/注释/换行/大小写

**验收**：P0 验收项 #3 达成（至少能抽出样本文档的主表）。

---

## 17.4 元数据适配器 + 缓存 + 匹配状态（P0）

* [ ] MetadataAdapter 接口（search/getDetail）
* [ ] Mock 客户端（先跑通 UI/后端联调）
* [ ] Cache：metadata_table_cache（TTL/过期策略）
* [ ] match_status：MATCHED/NOT_FOUND/BLACKLISTED/VERIFY_FAILED 全链路贯通
* [ ] 黑名单规则（配置化）

**验收**：P0 验收项 #4 达成（匹配成功/失败可见）；API 超时时展示 VERIFY_FAILED。

---

## 17.5 Lint 引擎（P0）

* [ ] 实现规则 E001~E006、W001~W006（按本 PRD）
* [ ] Lint 输出落库：scene_version.lint_json
* [ ] 发布门禁：lint 有 error 不能发布
* [ ] 单元测试：每条规则至少 1 正例/1 反例

**验收**：P0 验收项 #2/#7 的门禁真实生效。

---

## 17.6 数据来源表汇总 + 敏感字段清单（按版本绑定）

* [ ] 保存草稿时：从 sql_blocks 抽表 → 写入 scene_version_table（EXTRACTED）
* [ ] 支持手工补充/修正表（MANUAL）
* [ ] 敏感字段清单：scene_version_sensitive_field（mask_rule 必填）
* [ ] `has_sensitive` 自动推导

**验收**：P0 验收项 #5/#6 达成；历史版本的表清单与敏感规则可回放。

---

## 17.7 发布导出（doc.json + chunks.json）并固化

* [ ] doc.json 生成器（映射表单字段）
* [ ] chunks.json 生成器（按区块切块，每 sql_block 一块，每 caveat 一块）
* [ ] 导出产物写入 scene_version_export（version_id 绑定）
* [ ] 导出下载 API（默认当前发布版本）

**验收**：P0 验收项 #8 达成；不同版本导出的 doc/chunks 不同且可追溯。

---

## 17.8 前端工程初始化与路由

* [ ] React + Vite 初始化
* [ ] 路由：/scenes、/scenes/:id/edit、/scenes/:id
* [ ] API client（带 X-User header）

**验收**：可访问三页骨架，能调用后端列表接口。

---

## 17.9 前端：场景库列表页 `/scenes`

* [ ] 搜索+筛选：domain/status/overdue/hasSensitive
* [ ] last_verified 颜色高亮（绿/黄/红）
* [ ] 操作：查看/编辑/废弃（无硬删除）

**验收**：列表页能体现"可信与风险"，并正确过滤。

---

## 17.10 前端：场景编辑器 `/scenes/:id/edit`（最大块）

* [ ] 三栏布局 + 左侧目录锚点
* [ ] 区块 1/2/3/7/8 基础表单
* [ ] 区块 5：数据来源汇总（展示 match_status、关键表勾选、一键导入、手工新增）
* [ ] 区块 6：敏感字段清单（从元数据选择字段、mask_rule 下拉）
* [ ] 右侧辅助面板：lint / 表名抽取 / 敏感提醒 / 完成度
* [ ] 元数据选择抽屉（搜索表/字段）
* [ ] 保存草稿、运行校验、提交发布按钮联动

**验收**：P0 验收项 #2~#7 在编辑器端到端跑通。

---

## 17.11 前端：场景详情页 `/scenes/:id`

* [ ] 顶栏：状态/验证/敏感标记/关键表 chips
* [ ] 版本下拉切换展示（至少支持 current published + current draft）
* [ ] 导出按钮（doc.json/chunks.json）
* [ ] 编辑按钮：进入 edit（已发布则创建新草稿）

**验收**：可回放历史版本内容，导出可用。

---

## 17.12 集成测试与演示数据

* [ ] 端到端冒烟脚本：新建→编辑→lint→发布→导出→切版本
* [ ] 准备 3~5 个真实风格的样例场景（脱敏后的）
* [ ] README：本地启动、演示流程、字段解释、lint 规则说明

**验收**：按 P0 8 条验收清单逐条演示通过。

---

# 附录 A：SQLite DDL v0.1

```sql
PRAGMA foreign_keys = ON;

CREATE TABLE domain (
  id            TEXT PRIMARY KEY,
  domain_key    TEXT NOT NULL UNIQUE,
  name          TEXT NOT NULL,
  description   TEXT,
  created_by    TEXT NOT NULL,
  created_at    TEXT NOT NULL,
  updated_by    TEXT NOT NULL,
  updated_at    TEXT NOT NULL
);

CREATE TABLE scene (
  id               TEXT PRIMARY KEY,
  scene_code       TEXT NOT NULL UNIQUE,
  title            TEXT NOT NULL,
  domain_id        TEXT NOT NULL REFERENCES domain(id) ON DELETE RESTRICT,
  lifecycle_status TEXT NOT NULL CHECK (lifecycle_status IN ('ACTIVE','DEPRECATED')),
  created_by       TEXT NOT NULL,
  created_at       TEXT NOT NULL,
  updated_by       TEXT NOT NULL,
  updated_at       TEXT NOT NULL
);

CREATE INDEX ix_scene_domain ON scene(domain_id);
CREATE INDEX ix_scene_updated ON scene(updated_at);

CREATE TABLE scene_version (
  id               TEXT PRIMARY KEY,
  scene_id         TEXT NOT NULL REFERENCES scene(id) ON DELETE CASCADE,
  domain_id        TEXT NOT NULL REFERENCES domain(id) ON DELETE RESTRICT,
  status           TEXT NOT NULL CHECK (status IN ('DRAFT','PUBLISHED')),
  is_current       INTEGER NOT NULL DEFAULT 1 CHECK (is_current IN (0,1)),
  version_seq      INTEGER NOT NULL,
  version_label    TEXT NOT NULL,
  title            TEXT NOT NULL,
  tags_json        TEXT NOT NULL DEFAULT '[]',
  owner_user       TEXT NOT NULL,
  contributors_json TEXT NOT NULL DEFAULT '[]',
  has_sensitive    INTEGER NOT NULL DEFAULT 0 CHECK (has_sensitive IN (0,1)),
  last_verified_at TEXT,
  verified_by      TEXT,
  verify_evidence  TEXT,
  change_summary   TEXT,
  published_by     TEXT,
  published_at     TEXT,
  content_json     TEXT NOT NULL,
  lint_json        TEXT NOT NULL DEFAULT '{}',
  created_by       TEXT NOT NULL,
  created_at       TEXT NOT NULL,
  updated_by       TEXT NOT NULL,
  updated_at       TEXT NOT NULL
);

CREATE UNIQUE INDEX ux_scene_current_draft
ON scene_version(scene_id)
WHERE status='DRAFT' AND is_current=1;

CREATE UNIQUE INDEX ux_scene_current_published
ON scene_version(scene_id)
WHERE status='PUBLISHED' AND is_current=1;

CREATE UNIQUE INDEX ux_scene_published_seq
ON scene_version(scene_id, version_seq)
WHERE status='PUBLISHED';

CREATE INDEX ix_scene_version_scene_status
ON scene_version(scene_id, status, is_current);

CREATE INDEX ix_scene_version_last_verified
ON scene_version(last_verified_at);

CREATE TABLE scene_version_table (
  id                TEXT PRIMARY KEY,
  version_id        TEXT NOT NULL REFERENCES scene_version(id) ON DELETE CASCADE,
  table_fullname    TEXT NOT NULL,
  metadata_table_id TEXT,
  match_status      TEXT NOT NULL CHECK (match_status IN ('MATCHED','NOT_FOUND','BLACKLISTED','VERIFY_FAILED')),
  is_key            INTEGER NOT NULL DEFAULT 1 CHECK (is_key IN (0,1)),
  usage_type        TEXT,
  partition_field   TEXT,
  source            TEXT NOT NULL CHECK (source IN ('EXTRACTED','MANUAL')),
  notes             TEXT,
  extra_json        TEXT NOT NULL DEFAULT '{}',
  UNIQUE(version_id, table_fullname)
);

CREATE INDEX ix_version_table_fullname ON scene_version_table(table_fullname);
CREATE INDEX ix_version_table_version ON scene_version_table(version_id);
CREATE INDEX ix_version_table_match_status ON scene_version_table(match_status);

CREATE TABLE scene_version_sensitive_field (
  id                 TEXT PRIMARY KEY,
  version_id         TEXT NOT NULL REFERENCES scene_version(id) ON DELETE CASCADE,
  table_fullname     TEXT,
  field_name         TEXT NOT NULL,
  field_fullname     TEXT NOT NULL,
  metadata_field_id  TEXT,
  sensitivity_level  TEXT NOT NULL,
  mask_rule          TEXT NOT NULL,
  remarks            TEXT,
  source             TEXT NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL','SUGGESTED')),
  UNIQUE(version_id, field_fullname)
);

CREATE INDEX ix_sensitive_field_version ON scene_version_sensitive_field(version_id);
CREATE INDEX ix_sensitive_field_fullname ON scene_version_sensitive_field(field_fullname);

CREATE TABLE audit_log (
  id           TEXT PRIMARY KEY,
  scene_id     TEXT NOT NULL REFERENCES scene(id) ON DELETE CASCADE,
  version_id   TEXT REFERENCES scene_version(id) ON DELETE SET NULL,
  action       TEXT NOT NULL,
  actor        TEXT NOT NULL,
  occurred_at  TEXT NOT NULL,
  summary      TEXT,
  diff_json    TEXT NOT NULL DEFAULT '{}',
  extra_json   TEXT NOT NULL DEFAULT '{}'
);

CREATE INDEX ix_audit_scene_time ON audit_log(scene_id, occurred_at);
CREATE INDEX ix_audit_action_time ON audit_log(action, occurred_at);

CREATE TABLE metadata_table_cache (
  table_fullname      TEXT PRIMARY KEY,
  metadata_table_id   TEXT,
  schema_name         TEXT,
  table_name          TEXT,
  description         TEXT,
  sensitivity_summary TEXT,
  fields_json         TEXT NOT NULL,
  fetched_at          TEXT NOT NULL,
  expires_at          TEXT,
  extra_json          TEXT NOT NULL DEFAULT '{}'
);

CREATE INDEX ix_metadata_cache_expires ON metadata_table_cache(expires_at);

CREATE TABLE scene_version_export (
  version_id   TEXT PRIMARY KEY REFERENCES scene_version(id) ON DELETE CASCADE,
  doc_json     TEXT NOT NULL,
  chunks_json  TEXT NOT NULL,
  chunk_count  INTEGER NOT NULL,
  generated_at TEXT NOT NULL,
  generated_by TEXT NOT NULL,
  hash         TEXT
);

-- 种子数据
INSERT INTO domain (id, domain_key, name, description, created_by, created_at, updated_by, updated_at)
VALUES 
  ('d0000000-0000-0000-0000-000000000001', 'RETAIL_CIF', '零售客户', '零售个人客户相关的取数场景', 'system', datetime('now'), 'system', datetime('now')),
  ('d0000000-0000-0000-0000-000000000002', 'RETAIL_TXN', '零售交易', '零售交易流水相关的取数场景', 'system', datetime('now'), 'system', datetime('now')),
  ('d0000000-0000-0000-0000-000000000003', 'CORP_CIF', '对公客户', '对公企业客户相关的取数场景', 'system', datetime('now'), 'system', datetime('now')),
  ('d0000000-0000-0000-0000-000000000004', 'UNCATEGORIZED', '未归类', '尚未归类的取数场景', 'system', datetime('now'), 'system', datetime('now'));
```

---

# 附录 B：导出格式 v0.1（最小规范）

## B1. doc.json 必备字段清单

```json
{
  "$schema": "caliberhub.doc.v0.1",
  "doc_id": "SCE-20260128-0001",
  "scene": {
    "title": "活跃用户数统计",
    "domain": {"key": "RETAIL_CIF", "name": "零售客户"},
    "tags": ["高频", "月度"]
  },
  "version": {
    "version_id": "uuid",
    "version_seq": 1,
    "version_label": "v1.0",
    "published_at": "2026-01-28T22:00:00Z",
    "published_by": "zhangsan",
    "change_summary": "首次发布"
  },
  "governance": {
    "owner": "zhangsan",
    "contributors": ["lisi"],
    "last_verified": {
      "date": "2026-01-15",
      "by": "zhangsan",
      "evidence": "工单号 WO-12345"
    },
    "has_sensitive": true,
    "sensitive_fields": [
      {"field_fullname": "cif.t_customer.id_no", "sensitivity_level": "PII", "mask_rule": "HASH", "remarks": "身份证号"}
    ]
  },
  "data_sources": {
    "tables": [
      {"table_fullname": "cif.t_customer", "metadata_table_id": "mt-001", "match_status": "MATCHED", "is_key": true, "usage_type": "FACT"}
    ]
  },
  "content": {
    "scene_description": "...",
    "caliber_definition": "...",
    "inputs": {"params": [...], "constraints": "..."},
    "outputs": {"summary": "..."},
    "sql_blocks": [
      {"block_id": "blk-001", "name": "Step1", "condition": null, "sql": "SELECT ...", "notes": null}
    ],
    "caveats": [
      {"id": "cav-001", "title": "注意分区", "risk": "MEDIUM", "text": "..."}
    ]
  },
  "lint": {
    "passed": true,
    "errors": [],
    "warnings": [{"id": "W003", "message": "注意事项为空"}]
  }
}
```

## B2. chunks.json 必备字段清单

```json
{
  "$schema": "caliberhub.chunks.v0.1",
  "doc_id": "SCE-20260128-0001",
  "version_label": "v1.0",
  "version_id": "uuid",
  "generated_at": "2026-01-28T22:00:00Z",
  "chunk_count": 5,
  "chunks": [
    {
      "chunk_id": "SCE-20260128-0001::v1.0::definition::0",
      "section": "caliber_definition",
      "content_type": "text",
      "text": "【口径定义】活跃用户数是指...",
      "metadata": {
        "domain_key": "RETAIL_CIF",
        "tags": ["高频", "月度"],
        "tables": ["cif.t_customer"],
        "has_sensitive": true,
        "last_verified": "2026-01-15",
        "source_path": "content.caliber_definition"
      }
    },
    {
      "chunk_id": "SCE-20260128-0001::v1.0::sql_block::0",
      "section": "sql_block",
      "content_type": "sql",
      "text": "【SQL方案 Step1】\n```sql\nSELECT ...\n```",
      "metadata": {
        "domain_key": "RETAIL_CIF",
        "tables": ["cif.t_customer"],
        "has_sensitive": true,
        "last_verified": "2026-01-15",
        "source_path": "content.sql_blocks[0]",
        "block_id": "blk-001"
      }
    }
  ]
}
```
