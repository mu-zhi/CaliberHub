# CaliberHub 领域词汇表

本词汇表定义了 CaliberHub 系统中的核心业务术语，用于业务与技术团队间的统一沟通。

| 中文 | 英文 | 缩写 | 备注 |
|------|------|------|------|
| 口径 | Caliber | - | 数据取数的业务定义和规则 |
| 业务场景 | Scene | - | 最小治理单元，对应一个具体的取数需求 |
| 业务领域 | Domain | - | 场景的分类聚合层 |
| 场景版本 | SceneVersion | - | 场景的内容快照，支持草稿和发布状态 |
| 草稿 | Draft | - | 未发布的编辑中版本 |
| 已发布 | Published | - | 已通过审核的正式版本 |
| SQL方案 | SqlBlock | - | 场景中的一段SQL代码块，可有多个 |
| 输入参数 | InputParam | - | 取数时需要提供的参数 |
| 输出字段 | OutputField | - | 取数结果返回的字段 |
| 数据来源表 | SourceTable | - | 场景SQL中涉及的数据表 |
| 敏感字段 | SensitiveField | - | 需要脱敏处理的字段 |
| 脱敏规则 | MaskRule | - | 敏感字段的脱敏方式 |
| 最后验证日期 | LastVerified | - | 口径最后被验证正确的日期 |
| 版本号 | VersionSeq | - | 发布版本的递增序号 |
| 版本标签 | VersionLabel | - | 版本的展示名称，如 v1.0 |
| 生命周期状态 | LifecycleStatus | - | 场景的整体状态：活跃/已废弃 |
| 审计日志 | AuditLog | - | 操作记录，用于追溯 |
| Lint规则 | LintRule | - | 发布前的校验规则 |
| 阻断级错误 | BlockingError | E | 必须修正才能发布的错误 |
| 警告 | Warning | W | 建议修正但不阻断发布 |
| 元数据平台 | MetadataPlatform | - | 行内的表/字段元数据管理系统 |
| RAG导出 | RagExport | - | 生成供检索增强生成使用的JSON |
| 文档JSON | DocJson | - | 场景的完整结构化导出 |
| 切块JSON | ChunksJson | - | 场景拆分后的检索块 |
