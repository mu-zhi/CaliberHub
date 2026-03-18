CREATE TABLE IF NOT EXISTS caliber_scene_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    operator VARCHAR(64) NOT NULL,
    detail_json CLOB,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_scene_audit_scene_created
    ON caliber_scene_audit_log (scene_id, created_at);
