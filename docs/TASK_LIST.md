# CaliberHub 开发任务清单

> 基于 PRD.md 第 17 章 WBS 生成，可直接开 Jira/看板

## 当前进度概览

| 阶段 | 任务 | 状态 | 已完成 |
|------|------|------|--------|
| M0 | 方案冻结与契约对齐 | ⬜ 待确认 | 0/5 |
| M1 | 后端工程初始化与数据层 | ✅ 完成 | 4/4 |
| M2 | 核心领域模型与版本/审计 | ✅ 完成 | 4/4 |
| M3 | SQL 表名抽取服务 | ✅ 完成 | 4/4 |
| M4 | 元数据适配器 | ✅ 完成 | 5/5 |
| M5 | Lint 引擎 | ✅ 完成 | 4/4 |
| M6 | 数据来源表 + 敏感字段 | ✅ 完成 | 4/4 |
| M7 | 发布导出 | ✅ 完成 | 4/4 |
| M8 | 前端工程初始化 | ✅ 完成 | 3/3 |
| M9 | 前端：场景列表 | ✅ 完成 | 3/3 |
| M10 | 前端：场景编辑器 | ✅ 完成 | 7/7 |
| M11 | 前端：场景详情 | ✅ 完成 | 4/4 |
| M12 | 集成测试与演示 | ⬜ 未开始 | 0/3 |

---

## M0. 方案冻结与契约对齐

- [ ] 冻结 P0 验收 8 条
- [ ] 冻结 DB Schema（附录 A）与导出契约（附录 B）
- [ ] 冻结 lint 规则清单（E001-E006, W001-W006）
- [ ] 冻结元数据对接返回字段
- [ ] 冻结 `scene_code` 生成规则

**验收**：研发/产品/使用方共同确认

---

## M1. 后端工程初始化与数据层

- [x] Spring Boot 工程骨架（四层架构）
- [x] SQLite 初始化脚本（schema.sql）
- [x] 种子数据（4 个默认领域）
- [x] 基础中间件（异常处理、统一返回体、X-User 提取）

---

## M2. 核心领域模型与版本/审计

- [x] Scene 聚合根（创建、废弃）
- [x] SceneVersion 实体（草稿保存、发布）
- [x] 审计日志写入（CREATE / SAVE_DRAFT / PUBLISH / DEPRECATE）
- [x] 持久化层实现（PO、Mapper、Repository）

---

## M3. SQL 表名抽取服务

- [x] JSqlParser 解析实现（`SqlParserSupportImpl`）
- [x] Fallback 正则抽取
- [x] 多 sql_blocks 汇总（`extractTablesFromBlocks`）
- [x] 接口定义（`SqlParserSupport`）

---

## M4. 元数据适配器

- [x] MetadataSupport 接口定义
- [x] Mock 客户端实现（`MetadataApiClient`）
- [x] Cache 实现（`SimpleCache` TTL）
- [x] match_status 全链路（MATCHED/NOT_FOUND/BLACKLISTED/VERIFY_FAILED）
- [x] 黑名单正则规则

---

## M5. Lint 引擎

- [x] E001-E006 阻断规则（`LintService`）
- [x] W001-W006 警告规则
- [x] LintResult 值对象
- [x] 发布门禁逻辑

---

## M6. 数据来源表 + 敏感字段

- [x] scene_version_table 写入
- [x] 手工补充/修正
- [x] scene_version_sensitive_field
- [x] has_sensitive 自动推导

---

## M7. 发布导出

- [x] doc.json 生成器（`RagExportService`）
- [x] chunks.json 生成器（按区块切块）
- [x] ExportResult 结果封装
- [x] 下载 API（前端实现）

---

## M8. 前端工程初始化

- [x] React + Vite + TypeScript（`package.json`、`vite.config.ts`）
- [x] 路由配置（`App.tsx`、`MainLayout.tsx`）
- [x] API client（`api/client.ts`、`api/index.ts`）

---

## M9. 前端：场景列表

- [x] 搜索 + 筛选（领域、状态）
- [x] last_verified 颜色高亮（绿/黄/红）
- [x] 操作按钮（查看/编辑）

---

## M10. 前端：场景编辑器

- [x] 三栏布局（`SceneEditorPage.tsx`）
- [x] 基础表单区块（1-4, 7-8）
- [x] 数据来源汇总（区块 5）
- [x] 敏感字段清单（区块 6）
- [x] 右侧辅助面板（Lint 结果、完成度）
- [x] 元数据选择抽屉
- [x] 发布确认弹窗

---

## M11. 前端：场景详情

- [x] 顶栏信息（`SceneDetailPage.tsx`）
- [x] 版本切换（下拉选择）
- [x] 导出按钮（doc.json/chunks.json）
- [x] 编辑按钮

---

## M11+. 前端额外页面

- [x] 领域管理页面（`DomainListPage.tsx`）
- [x] 系统设置页面（`SettingsPage.tsx`）

---

## M11. 前端：场景详情

- [x] 顶栏信息（`SceneDetailPage.tsx`）
- [x] 版本切换（下拉选择）
- [x] 导出按钮（doc.json/chunks.json）
- [x] 编辑按钮

---

## M12. 集成测试与演示

- [ ] 端到端冒烟脚本
- [ ] 3-5 个样例场景
- [ ] README 文档
