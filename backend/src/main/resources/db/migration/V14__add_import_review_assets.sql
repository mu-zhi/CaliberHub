CREATE TABLE caliber_import_scene_candidate (
    candidate_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    candidate_code VARCHAR(64) NOT NULL,
    scene_index INT NOT NULL,
    scene_title VARCHAR(255) NOT NULL,
    scene_description LONGTEXT,
    candidate_payload_json LONGTEXT,
    confidence_score DOUBLE,
    confirmation_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_import_scene_candidate_code
    ON caliber_import_scene_candidate (candidate_code);

CREATE INDEX idx_import_scene_candidate_task_material
    ON caliber_import_scene_candidate (task_id, material_id, updated_at);

CREATE TABLE caliber_import_evidence_candidate (
    evidence_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    candidate_code VARCHAR(64) NOT NULL,
    scene_candidate_code VARCHAR(64),
    evidence_type VARCHAR(32) NOT NULL,
    anchor_label VARCHAR(255) NOT NULL,
    quote_text LONGTEXT,
    line_start INT,
    line_end INT,
    confirmation_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_import_evidence_candidate_code
    ON caliber_import_evidence_candidate (candidate_code);

CREATE INDEX idx_import_evidence_candidate_task_material
    ON caliber_import_evidence_candidate (task_id, material_id, updated_at);
