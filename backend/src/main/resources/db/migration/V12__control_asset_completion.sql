ALTER TABLE caliber_scene_version
    ADD COLUMN version_tag VARCHAR(64);
ALTER TABLE caliber_scene_version
    ADD COLUMN publish_status VARCHAR(20);
ALTER TABLE caliber_scene_version
    ADD COLUMN published_by VARCHAR(64);
ALTER TABLE caliber_scene_version
    ADD COLUMN published_at TIMESTAMP;
ALTER TABLE caliber_scene_version
    ADD COLUMN snapshot_summary_json LONGTEXT;

ALTER TABLE caliber_plan
    ADD COLUMN snapshot_id BIGINT;
ALTER TABLE caliber_plan
    ADD COLUMN version_tag VARCHAR(64);

ALTER TABLE caliber_output_contract
    ADD COLUMN snapshot_id BIGINT;
ALTER TABLE caliber_output_contract
    ADD COLUMN version_tag VARCHAR(64);

ALTER TABLE caliber_coverage_declaration
    ADD COLUMN snapshot_id BIGINT;
ALTER TABLE caliber_coverage_declaration
    ADD COLUMN version_tag VARCHAR(64);

ALTER TABLE caliber_policy
    ADD COLUMN snapshot_id BIGINT;
ALTER TABLE caliber_policy
    ADD COLUMN version_tag VARCHAR(64);

ALTER TABLE caliber_source_intake_contract
    ADD COLUMN snapshot_id BIGINT;
ALTER TABLE caliber_source_intake_contract
    ADD COLUMN version_tag VARCHAR(64);
ALTER TABLE caliber_source_intake_contract
    ADD COLUMN source_table_hints_json LONGTEXT;
ALTER TABLE caliber_source_intake_contract
    ADD COLUMN known_coverage_json LONGTEXT;
ALTER TABLE caliber_source_intake_contract
    ADD COLUMN sensitivity_level VARCHAR(8) DEFAULT 'S1';
ALTER TABLE caliber_source_intake_contract
    ADD COLUMN default_time_semantic VARCHAR(128);
ALTER TABLE caliber_source_intake_contract
    ADD COLUMN material_source_note LONGTEXT;

UPDATE caliber_source_intake_contract
SET sensitivity_level = COALESCE(sensitivity_level, 'S1');

ALTER TABLE caliber_evidence_fragment
    ADD COLUMN snapshot_id BIGINT;
ALTER TABLE caliber_evidence_fragment
    ADD COLUMN version_tag VARCHAR(64);

ALTER TABLE caliber_input_slot_schema
    ADD COLUMN snapshot_id BIGINT;
ALTER TABLE caliber_input_slot_schema
    ADD COLUMN version_tag VARCHAR(64);

CREATE TABLE IF NOT EXISTS caliber_contract_view (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    plan_id BIGINT,
    output_contract_id BIGINT,
    view_code VARCHAR(64) NOT NULL,
    view_name VARCHAR(200) NOT NULL,
    role_scope VARCHAR(64) NOT NULL,
    visible_fields_json LONGTEXT,
    masked_fields_json LONGTEXT,
    restricted_fields_json LONGTEXT,
    forbidden_fields_json LONGTEXT,
    approval_template LONGTEXT,
    snapshot_id BIGINT,
    version_tag VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_contract_view_code UNIQUE (view_code)
);

CREATE INDEX idx_contract_view_scene_status
    ON caliber_contract_view (scene_id, status, updated_at);

CREATE TABLE IF NOT EXISTS caliber_source_contract (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    plan_id BIGINT,
    intake_contract_id BIGINT,
    source_contract_code VARCHAR(64) NOT NULL,
    source_name VARCHAR(200) NOT NULL,
    physical_table VARCHAR(255) NOT NULL,
    source_role VARCHAR(64) NOT NULL,
    identifier_type VARCHAR(64),
    output_identifier_type VARCHAR(64),
    source_system VARCHAR(128),
    time_semantic VARCHAR(128),
    completeness_level VARCHAR(16),
    sensitivity_level VARCHAR(8),
    start_date DATE,
    end_date DATE,
    material_source_note LONGTEXT,
    notes LONGTEXT,
    snapshot_id BIGINT,
    version_tag VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_source_contract_code UNIQUE (source_contract_code)
);

CREATE INDEX idx_source_contract_scene_status
    ON caliber_source_contract (scene_id, status, updated_at);
CREATE INDEX idx_source_contract_plan_status
    ON caliber_source_contract (plan_id, status, updated_at);

CREATE TABLE IF NOT EXISTS caliber_gap_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    task_code VARCHAR(64) NOT NULL,
    task_title VARCHAR(200) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    severity VARCHAR(16),
    detail_text LONGTEXT,
    source_ref VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_gap_task_code UNIQUE (task_code)
);

CREATE INDEX idx_gap_task_scene_status
    ON caliber_gap_task (scene_id, status, updated_at);

CREATE TABLE IF NOT EXISTS caliber_review_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    review_code VARCHAR(64) NOT NULL,
    review_title VARCHAR(200) NOT NULL,
    reviewer_role VARCHAR(64),
    review_decision VARCHAR(32),
    detail_text LONGTEXT,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_review_task_code UNIQUE (review_code)
);

CREATE INDEX idx_review_task_scene_status
    ON caliber_review_task (scene_id, status, updated_at);

CREATE TABLE IF NOT EXISTS caliber_audit_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT,
    event_name VARCHAR(128) NOT NULL,
    trace_id VARCHAR(64),
    snapshot_id BIGINT,
    operator_id VARCHAR(64),
    job_id VARCHAR(64),
    reason_code VARCHAR(64),
    payload_json LONGTEXT,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_audit_event_scene_status
    ON caliber_audit_event (scene_id, status, updated_at);
CREATE INDEX idx_audit_event_trace
    ON caliber_audit_event (trace_id, created_at);
