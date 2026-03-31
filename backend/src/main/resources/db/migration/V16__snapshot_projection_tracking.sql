-- Snapshot-level projection tracking for Neo4j Phase 1 read model
CREATE TABLE IF NOT EXISTS caliber_snapshot_projection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    snapshot_id BIGINT NOT NULL,
    projection_status VARCHAR(20) NOT NULL,
    verification_status VARCHAR(20) NOT NULL,
    verification_message LONGTEXT,
    node_count INT,
    edge_count INT,
    projected_at TIMESTAMP NULL,
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_snapshot_projection UNIQUE (scene_id, snapshot_id)
);

CREATE INDEX idx_snapshot_proj_scene_status
    ON caliber_snapshot_projection (scene_id, verification_status, updated_at);
