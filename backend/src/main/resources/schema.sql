-- CaliberHub Database Schema v0.1
-- SQLite DDL

PRAGMA foreign_keys = ON;

-- ==============
-- 1) Domain（业务领域字典）
-- ==============
CREATE TABLE IF NOT EXISTS domain (
    id            TEXT PRIMARY KEY,
    domain_key    TEXT NOT NULL UNIQUE,
    name          TEXT NOT NULL,
    description   TEXT,
    
    created_by    TEXT NOT NULL,
    created_at    TEXT NOT NULL,
    updated_by    TEXT NOT NULL,
    updated_at    TEXT NOT NULL
);

-- ==============
-- 2) Scene（口径知识资产：最小治理单元）
-- ==============
CREATE TABLE IF NOT EXISTS scene (
    id               TEXT PRIMARY KEY,
    scene_code       TEXT NOT NULL UNIQUE,
    title            TEXT NOT NULL,
    domain_id        TEXT NOT NULL REFERENCES domain(id) ON DELETE RESTRICT,
    
    lifecycle_status TEXT NOT NULL CHECK (lifecycle_status IN ('ACTIVE','DEPRECATED')),
    
    deprecated_at    TEXT,
    deprecated_by    TEXT,
    deprecate_reason TEXT,
    
    created_by       TEXT NOT NULL,
    created_at       TEXT NOT NULL,
    updated_by       TEXT NOT NULL,
    updated_at       TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_scene_domain ON scene(domain_id);
CREATE INDEX IF NOT EXISTS ix_scene_updated ON scene(updated_at);

-- ==============
-- 3) SceneVersion（草稿/发布版本快照）
-- ==============
CREATE TABLE IF NOT EXISTS scene_version (
    id              TEXT PRIMARY KEY,
    scene_id        TEXT NOT NULL REFERENCES scene(id) ON DELETE CASCADE,
    domain_id       TEXT NOT NULL REFERENCES domain(id) ON DELETE RESTRICT,
    
    status          TEXT NOT NULL CHECK (status IN ('DRAFT','PUBLISHED')),
    is_current      INTEGER NOT NULL DEFAULT 1 CHECK (is_current IN (0,1)),
    
    version_seq     INTEGER NOT NULL,
    version_label   TEXT NOT NULL,
    
    title           TEXT NOT NULL,
    tags_json       TEXT NOT NULL DEFAULT '[]',
    owner_user      TEXT NOT NULL,
    contributors_json TEXT NOT NULL DEFAULT '[]',
    
    has_sensitive   INTEGER NOT NULL DEFAULT 0 CHECK (has_sensitive IN (0,1)),
    last_verified_at TEXT,
    verified_by     TEXT,
    verify_evidence TEXT,
    change_summary  TEXT,
    
    published_by    TEXT,
    published_at    TEXT,
    
    content_json    TEXT NOT NULL,
    
    lint_passed     INTEGER NOT NULL DEFAULT 0,
    error_count     INTEGER NOT NULL DEFAULT 0,
    warning_count   INTEGER NOT NULL DEFAULT 0,
    lint_json       TEXT NOT NULL DEFAULT '{}',
    
    created_by      TEXT NOT NULL,
    created_at      TEXT NOT NULL,
    updated_by      TEXT NOT NULL,
    updated_at      TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_scene_current_draft
ON scene_version(scene_id)
WHERE status='DRAFT' AND is_current=1;

CREATE UNIQUE INDEX IF NOT EXISTS ux_scene_current_published
ON scene_version(scene_id)
WHERE status='PUBLISHED' AND is_current=1;

CREATE UNIQUE INDEX IF NOT EXISTS ux_scene_published_seq
ON scene_version(scene_id, version_seq)
WHERE status='PUBLISHED';

CREATE INDEX IF NOT EXISTS ix_scene_version_scene_status
ON scene_version(scene_id, status, is_current);

CREATE INDEX IF NOT EXISTS ix_scene_version_last_verified
ON scene_version(last_verified_at);

-- ==============
-- 4) SceneVersionTable（场景级：数据来源表汇总）
-- ==============
CREATE TABLE IF NOT EXISTS scene_version_table (
    id              TEXT PRIMARY KEY,
    version_id      TEXT NOT NULL REFERENCES scene_version(id) ON DELETE CASCADE,
    
    table_fullname  TEXT NOT NULL,
    metadata_table_id TEXT,
    
    match_status    TEXT NOT NULL CHECK (match_status IN ('MATCHED','NOT_FOUND','BLACKLISTED','VERIFY_FAILED')),
    is_key          INTEGER NOT NULL DEFAULT 1 CHECK (is_key IN (0,1)),
    usage_type      TEXT,
    partition_field TEXT,
    source          TEXT NOT NULL CHECK (source IN ('EXTRACTED','MANUAL')),
    
    description     TEXT,
    sensitivity_summary TEXT,
    notes           TEXT,
    extra_json      TEXT NOT NULL DEFAULT '{}',
    
    UNIQUE(version_id, table_fullname)
);

CREATE INDEX IF NOT EXISTS ix_version_table_fullname ON scene_version_table(table_fullname);
CREATE INDEX IF NOT EXISTS ix_version_table_version ON scene_version_table(version_id);
CREATE INDEX IF NOT EXISTS ix_version_table_match_status ON scene_version_table(match_status);

-- ==============
-- 5) SceneVersionSensitiveField（敏感字段+脱敏规则）
-- ==============
CREATE TABLE IF NOT EXISTS scene_version_sensitive_field (
    id               TEXT PRIMARY KEY,
    version_id       TEXT NOT NULL REFERENCES scene_version(id) ON DELETE CASCADE,
    
    table_fullname   TEXT,
    field_name       TEXT NOT NULL,
    field_fullname   TEXT NOT NULL,
    metadata_field_id TEXT,
    
    sensitivity_level TEXT NOT NULL,
    mask_rule        TEXT NOT NULL,
    remarks          TEXT,
    source           TEXT NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL','SUGGESTED')),
    
    UNIQUE(version_id, field_fullname)
);

CREATE INDEX IF NOT EXISTS ix_sensitive_field_version ON scene_version_sensitive_field(version_id);
CREATE INDEX IF NOT EXISTS ix_sensitive_field_fullname ON scene_version_sensitive_field(field_fullname);

-- ==============
-- 6) AuditLog（审计日志）
-- ==============
CREATE TABLE IF NOT EXISTS audit_log (
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

CREATE INDEX IF NOT EXISTS ix_audit_scene_time ON audit_log(scene_id, occurred_at);
CREATE INDEX IF NOT EXISTS ix_audit_action_time ON audit_log(action, occurred_at);

-- ==============
-- 7) 元数据平台缓存
-- ==============
CREATE TABLE IF NOT EXISTS metadata_table_cache (
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

CREATE INDEX IF NOT EXISTS ix_metadata_cache_expires ON metadata_table_cache(expires_at);

-- ==============
-- 8) 发布导出产物固化
-- ==============
CREATE TABLE IF NOT EXISTS scene_version_export (
    version_id   TEXT PRIMARY KEY REFERENCES scene_version(id) ON DELETE CASCADE,
    doc_json     TEXT NOT NULL,
    chunks_json  TEXT NOT NULL,
    chunk_count  INTEGER NOT NULL,
    generated_at TEXT NOT NULL,
    generated_by TEXT NOT NULL,
    hash         TEXT
);

-- ==============
-- 初始化种子数据：默认领域
-- ==============
INSERT OR IGNORE INTO domain (id, domain_key, name, description, created_by, created_at, updated_by, updated_at)
VALUES 
    ('d0000000-0000-0000-0000-000000000001', 'RETAIL_CIF', '零售客户', '零售个人客户相关的取数场景', 'system', datetime('now'), 'system', datetime('now')),
    ('d0000000-0000-0000-0000-000000000002', 'RETAIL_TXN', '零售交易', '零售交易流水相关的取数场景', 'system', datetime('now'), 'system', datetime('now')),
    ('d0000000-0000-0000-0000-000000000003', 'CORP_CIF', '对公客户', '对公企业客户相关的取数场景', 'system', datetime('now'), 'system', datetime('now')),
    ('d0000000-0000-0000-0000-000000000004', 'UNCATEGORIZED', '未归类', '尚未归类的取数场景', 'system', datetime('now'), 'system', datetime('now'));
