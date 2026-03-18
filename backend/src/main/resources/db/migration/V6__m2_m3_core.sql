CREATE TABLE IF NOT EXISTS caliber_semantic_view (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    view_code VARCHAR(64) NOT NULL,
    view_name VARCHAR(200) NOT NULL,
    domain_id BIGINT,
    description CLOB,
    field_definitions_json CLOB,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_semantic_view_code UNIQUE (view_code)
);

CREATE INDEX IF NOT EXISTS idx_semantic_view_domain_id
    ON caliber_semantic_view (domain_id);

CREATE TABLE IF NOT EXISTS caliber_scene_reference (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    ref_type VARCHAR(32) NOT NULL,
    ref_id BIGINT NOT NULL,
    strategy VARCHAR(16) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_scene_reference UNIQUE (scene_id, ref_type, ref_id)
);

CREATE INDEX IF NOT EXISTS idx_scene_reference_ref
    ON caliber_scene_reference (ref_type, ref_id);

CREATE TABLE IF NOT EXISTS caliber_scene_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    snapshot_json CLOB NOT NULL,
    change_summary CLOB,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_scene_version UNIQUE (scene_id, version_no)
);

CREATE INDEX IF NOT EXISTS idx_scene_version_scene_created
    ON caliber_scene_version (scene_id, created_at);

CREATE TABLE IF NOT EXISTS caliber_alignment_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    report_json CLOB,
    message CLOB,
    checked_by VARCHAR(64) NOT NULL,
    checked_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_alignment_scene_checked
    ON caliber_alignment_report (scene_id, checked_at);

CREATE TABLE IF NOT EXISTS caliber_service_spec (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    spec_code VARCHAR(64) NOT NULL,
    spec_version INT NOT NULL,
    spec_json CLOB NOT NULL,
    exported_by VARCHAR(64) NOT NULL,
    exported_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_service_spec_version UNIQUE (spec_code, spec_version)
);

CREATE INDEX IF NOT EXISTS idx_service_spec_scene
    ON caliber_service_spec (scene_id, exported_at);

CREATE TABLE IF NOT EXISTS caliber_plan_ir_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    query_text CLOB NOT NULL,
    scene_id BIGINT,
    decision VARCHAR(16) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    plan_json CLOB NOT NULL,
    created_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_plan_ir_scene_created
    ON caliber_plan_ir_audit (scene_id, created_at);

CREATE TABLE IF NOT EXISTS caliber_execution_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_audit_id BIGINT NOT NULL,
    scene_id BIGINT,
    success BOOLEAN NOT NULL,
    reason CLOB,
    selected_plan VARCHAR(128),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_feedback_scene_created
    ON caliber_execution_feedback (scene_id, created_at);
