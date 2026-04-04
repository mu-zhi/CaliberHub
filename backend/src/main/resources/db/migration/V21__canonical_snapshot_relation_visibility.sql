CREATE TABLE caliber_canonical_snapshot_relation_visibility (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    snapshot_id BIGINT NOT NULL,
    scene_id BIGINT NOT NULL,
    canonical_relation_id BIGINT NOT NULL,
    source_canonical_entity_id BIGINT NOT NULL,
    target_canonical_entity_id BIGINT NOT NULL,
    relation_type VARCHAR(64) NOT NULL,
    source_relation_id BIGINT,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_canonical_snapshot_relation_visibility UNIQUE (snapshot_id, canonical_relation_id)
);

CREATE INDEX idx_canonical_snapshot_relation_visibility_scene
    ON caliber_canonical_snapshot_relation_visibility (snapshot_id, scene_id, relation_type, updated_at);
