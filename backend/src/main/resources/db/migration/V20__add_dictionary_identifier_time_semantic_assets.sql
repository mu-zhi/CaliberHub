CREATE TABLE IF NOT EXISTS caliber_dictionary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    plan_id BIGINT,
    dict_code VARCHAR(64) NOT NULL,
    dict_name VARCHAR(200) NOT NULL,
    dict_category VARCHAR(64),
    dict_version VARCHAR(64),
    release_status VARCHAR(32),
    entries_json LONGTEXT,
    referenced_by_json LONGTEXT,
    snapshot_id BIGINT,
    version_tag VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_dictionary_code UNIQUE (dict_code)
);

CREATE INDEX idx_dictionary_scene_status
    ON caliber_dictionary (scene_id, status, updated_at);
CREATE INDEX idx_dictionary_plan_status
    ON caliber_dictionary (plan_id, status, updated_at);

CREATE TABLE IF NOT EXISTS caliber_identifier_lineage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    plan_id BIGINT,
    lineage_code VARCHAR(64) NOT NULL,
    lineage_name VARCHAR(200) NOT NULL,
    identifier_type VARCHAR(64),
    source_identifier_type VARCHAR(64),
    target_identifier_type VARCHAR(64),
    mapping_rules_json LONGTEXT,
    evidence_refs_json LONGTEXT,
    confirmation_status VARCHAR(32),
    snapshot_id BIGINT,
    version_tag VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_identifier_lineage_code UNIQUE (lineage_code)
);

CREATE INDEX idx_identifier_lineage_scene_status
    ON caliber_identifier_lineage (scene_id, status, updated_at);
CREATE INDEX idx_identifier_lineage_plan_status
    ON caliber_identifier_lineage (plan_id, status, updated_at);

CREATE TABLE IF NOT EXISTS caliber_time_semantic_selector (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    plan_id BIGINT,
    selector_code VARCHAR(64) NOT NULL,
    selector_name VARCHAR(200) NOT NULL,
    default_semantic VARCHAR(128),
    candidate_semantics_json LONGTEXT,
    clarification_terms_json LONGTEXT,
    priority_rules_json LONGTEXT,
    must_clarify_flag BOOLEAN NOT NULL DEFAULT FALSE,
    snapshot_id BIGINT,
    version_tag VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_time_semantic_selector_code UNIQUE (selector_code)
);

CREATE INDEX idx_time_semantic_selector_scene_status
    ON caliber_time_semantic_selector (scene_id, status, updated_at);
CREATE INDEX idx_time_semantic_selector_plan_status
    ON caliber_time_semantic_selector (plan_id, status, updated_at);

