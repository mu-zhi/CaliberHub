ALTER TABLE caliber_scene
    ADD COLUMN scene_type VARCHAR(32) DEFAULT 'FACT_DETAIL';

UPDATE caliber_scene
SET scene_type = COALESCE(scene_type, 'FACT_DETAIL');

ALTER TABLE caliber_coverage_declaration
    ADD COLUMN coverage_status VARCHAR(16) DEFAULT 'FULL';
ALTER TABLE caliber_coverage_declaration
    ADD COLUMN time_semantic VARCHAR(128);
ALTER TABLE caliber_coverage_declaration
    ADD COLUMN source_system VARCHAR(128);

UPDATE caliber_coverage_declaration
SET coverage_status = COALESCE(coverage_status, 'FULL');

ALTER TABLE caliber_policy
    ADD COLUMN sensitivity_level VARCHAR(8) DEFAULT 'S1';
ALTER TABLE caliber_policy
    ADD COLUMN masking_rule LONGTEXT;

UPDATE caliber_policy
SET sensitivity_level = COALESCE(sensitivity_level, 'S1');

CREATE TABLE IF NOT EXISTS caliber_output_contract (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    contract_code VARCHAR(64) NOT NULL,
    contract_name VARCHAR(200) NOT NULL,
    summary_text LONGTEXT,
    fields_json LONGTEXT,
    masking_rules_json LONGTEXT,
    usage_constraints LONGTEXT,
    time_caliber_note LONGTEXT,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_output_contract_code UNIQUE (contract_code)
);

CREATE INDEX idx_output_contract_scene_status
    ON caliber_output_contract (scene_id, status, updated_at);

CREATE TABLE IF NOT EXISTS caliber_input_slot_schema (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    slot_code VARCHAR(64) NOT NULL,
    slot_name VARCHAR(128) NOT NULL,
    slot_type VARCHAR(64) NOT NULL,
    required_flag BOOLEAN NOT NULL DEFAULT FALSE,
    identifier_candidates_json LONGTEXT,
    normalization_rule LONGTEXT,
    clarification_hint LONGTEXT,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_input_slot_code UNIQUE (slot_code)
);

CREATE INDEX idx_input_slot_scene_status
    ON caliber_input_slot_schema (scene_id, status, updated_at);

CREATE TABLE IF NOT EXISTS caliber_source_intake_contract (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    intake_code VARCHAR(64) NOT NULL,
    intake_name VARCHAR(200) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    required_fields_json LONGTEXT,
    completeness_rule LONGTEXT,
    gap_task_hint LONGTEXT,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_source_intake_code UNIQUE (intake_code)
);

CREATE INDEX idx_source_intake_scene_status
    ON caliber_source_intake_contract (scene_id, status, updated_at);
