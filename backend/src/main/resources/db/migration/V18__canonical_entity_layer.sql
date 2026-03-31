CREATE TABLE caliber_canonical_entity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(64) NOT NULL,
    canonical_key VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    resolution_status VARCHAR(32) NOT NULL,
    lifecycle_status VARCHAR(32) NOT NULL,
    profile_json LONGTEXT,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_canonical_entity_type_key UNIQUE (entity_type, canonical_key)
);

CREATE INDEX idx_canonical_entity_type_resolution
    ON caliber_canonical_entity (entity_type, resolution_status, updated_at);

CREATE TABLE caliber_canonical_entity_membership (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    canonical_entity_id BIGINT NOT NULL,
    scene_asset_type VARCHAR(64) NOT NULL,
    scene_asset_id BIGINT NOT NULL,
    scene_id BIGINT NOT NULL,
    match_basis VARCHAR(128) NOT NULL,
    confidence_score DOUBLE,
    manual_override BOOLEAN NOT NULL DEFAULT FALSE,
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_canonical_membership_asset UNIQUE (scene_asset_type, scene_asset_id)
);

CREATE INDEX idx_canonical_membership_entity_active
    ON caliber_canonical_entity_membership (canonical_entity_id, active_flag, updated_at);

CREATE TABLE caliber_canonical_entity_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_canonical_entity_id BIGINT NOT NULL,
    target_canonical_entity_id BIGINT NOT NULL,
    relation_type VARCHAR(64) NOT NULL,
    relation_label VARCHAR(255),
    relation_payload_json LONGTEXT,
    visible_in_snapshot_binding BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_canonical_relation UNIQUE (
        source_canonical_entity_id,
        target_canonical_entity_id,
        relation_type
    )
);

CREATE INDEX idx_canonical_relation_source_type
    ON caliber_canonical_entity_relation (source_canonical_entity_id, relation_type, updated_at);

CREATE TABLE caliber_canonical_resolution_audit (
    audit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(64) NOT NULL,
    scene_asset_type VARCHAR(64) NOT NULL,
    scene_asset_id BIGINT NOT NULL,
    scene_id BIGINT NOT NULL,
    canonical_entity_id BIGINT,
    suggested_canonical_key VARCHAR(255),
    decision VARCHAR(32) NOT NULL,
    match_basis VARCHAR(128),
    confidence_score DOUBLE,
    resolution_rule_version VARCHAR(64),
    decision_reason LONGTEXT,
    manual_override BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_canonical_audit_asset
    ON caliber_canonical_resolution_audit (scene_asset_type, scene_asset_id, updated_at);

CREATE TABLE caliber_canonical_snapshot_membership (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    snapshot_id BIGINT NOT NULL,
    scene_id BIGINT NOT NULL,
    canonical_entity_id BIGINT NOT NULL,
    scene_asset_type VARCHAR(64) NOT NULL,
    scene_asset_id BIGINT NOT NULL,
    source_membership_id BIGINT,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_canonical_snapshot_membership UNIQUE (snapshot_id, scene_asset_type, scene_asset_id)
);

CREATE INDEX idx_canonical_snapshot_membership_scene
    ON caliber_canonical_snapshot_membership (snapshot_id, scene_id, updated_at);
