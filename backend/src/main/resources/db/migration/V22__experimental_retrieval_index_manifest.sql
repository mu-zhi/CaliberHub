CREATE TABLE IF NOT EXISTS caliber_experimental_retrieval_index_manifest (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    snapshot_id BIGINT NOT NULL,
    scene_code VARCHAR(128) NOT NULL,
    version_tag VARCHAR(128),
    index_version VARCHAR(128) NOT NULL,
    fallback_index_version VARCHAR(128),
    source_status VARCHAR(32) NOT NULL,
    manifest_status VARCHAR(32) NOT NULL,
    draft_leak_count INT NOT NULL DEFAULT 0,
    summary_json LONGTEXT,
    failure_reason LONGTEXT,
    created_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_exp_retrieval_manifest_scene_snapshot UNIQUE (scene_id, snapshot_id)
);

CREATE INDEX idx_exp_retrieval_manifest_scene_snapshot
    ON caliber_experimental_retrieval_index_manifest (scene_id, snapshot_id);

CREATE INDEX idx_exp_retrieval_manifest_status
    ON caliber_experimental_retrieval_index_manifest (manifest_status, updated_at);
