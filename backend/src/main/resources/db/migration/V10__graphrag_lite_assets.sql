CREATE TABLE IF NOT EXISTS caliber_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    plan_code VARCHAR(64) NOT NULL,
    plan_name VARCHAR(200) NOT NULL,
    applicable_period VARCHAR(255),
    default_time_semantic LONGTEXT,
    source_tables_json LONGTEXT,
    notes LONGTEXT,
    retrieval_text LONGTEXT,
    sql_text LONGTEXT,
    confidence_score DOUBLE,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_plan_code UNIQUE (plan_code)
);

CREATE INDEX idx_plan_scene_status
    ON caliber_plan (scene_id, status, updated_at);

CREATE TABLE IF NOT EXISTS caliber_evidence_fragment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    evidence_code VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    fragment_text LONGTEXT NOT NULL,
    source_anchor VARCHAR(500),
    source_type VARCHAR(64),
    source_ref VARCHAR(500),
    confidence_score DOUBLE,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_evidence_code UNIQUE (evidence_code)
);

CREATE INDEX idx_evidence_scene_status
    ON caliber_evidence_fragment (scene_id, status, updated_at);

CREATE TABLE IF NOT EXISTS caliber_coverage_declaration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    coverage_code VARCHAR(64) NOT NULL,
    coverage_title VARCHAR(200) NOT NULL,
    coverage_type VARCHAR(32) NOT NULL,
    statement_text LONGTEXT NOT NULL,
    applicable_period VARCHAR(255),
    source_tables_json LONGTEXT,
    gap_text LONGTEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL,
    start_date DATE,
    end_date DATE,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_coverage_code UNIQUE (coverage_code)
);

CREATE INDEX idx_coverage_plan_status
    ON caliber_coverage_declaration (plan_id, status, updated_at);

CREATE TABLE IF NOT EXISTS caliber_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    policy_code VARCHAR(64) NOT NULL,
    policy_name VARCHAR(200) NOT NULL,
    scope_type VARCHAR(20) NOT NULL,
    scope_ref_id BIGINT,
    effect_type VARCHAR(32) NOT NULL,
    condition_text LONGTEXT,
    source_type VARCHAR(32),
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_policy_code UNIQUE (policy_code)
);

CREATE INDEX idx_policy_scope_status
    ON caliber_policy (scope_type, scope_ref_id, status, updated_at);

CREATE TABLE IF NOT EXISTS caliber_plan_evidence_ref (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    evidence_id BIGINT NOT NULL,
    relation_type VARCHAR(32) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_plan_evidence_ref UNIQUE (plan_id, evidence_id)
);

CREATE INDEX idx_plan_evidence_plan
    ON caliber_plan_evidence_ref (plan_id);

CREATE TABLE IF NOT EXISTS caliber_plan_policy_ref (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    policy_id BIGINT NOT NULL,
    relation_type VARCHAR(32) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_plan_policy_ref UNIQUE (plan_id, policy_id)
);

CREATE INDEX idx_plan_policy_plan
    ON caliber_plan_policy_ref (plan_id);

CREATE TABLE IF NOT EXISTS caliber_entity_alias (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT,
    plan_id BIGINT,
    alias_code VARCHAR(64) NOT NULL,
    alias_text VARCHAR(255) NOT NULL,
    normalized_text VARCHAR(255) NOT NULL,
    alias_type VARCHAR(32) NOT NULL,
    source VARCHAR(64),
    confidence_score DOUBLE,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_alias_code UNIQUE (alias_code)
);

CREATE INDEX idx_entity_alias_norm
    ON caliber_entity_alias (normalized_text, status, updated_at);

CREATE TABLE IF NOT EXISTS caliber_plan_schema_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    table_name VARCHAR(255) NOT NULL,
    column_name VARCHAR(255),
    link_role VARCHAR(32) NOT NULL,
    evidence_id BIGINT,
    confidence_score DOUBLE,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_schema_link_plan
    ON caliber_plan_schema_link (plan_id, status, updated_at);

CREATE TABLE IF NOT EXISTS caliber_projection_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    scene_code VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    stage VARCHAR(32) NOT NULL,
    message LONGTEXT,
    payload_json LONGTEXT,
    last_projected_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_projection_scene UNIQUE (scene_id)
);

CREATE INDEX idx_projection_status_updated
    ON caliber_projection_event (status, updated_at);
