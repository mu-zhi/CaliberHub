CREATE TABLE caliber_source_material (
    material_id VARCHAR(64) PRIMARY KEY,
    source_type VARCHAR(32),
    source_name VARCHAR(255),
    raw_text TEXT,
    text_fingerprint VARCHAR(128) NOT NULL,
    operator VARCHAR(64),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_source_material_source_type_updated
    ON caliber_source_material (source_type, updated_at);

CREATE INDEX idx_source_material_fingerprint
    ON caliber_source_material (text_fingerprint);

ALTER TABLE caliber_import_task
    ADD COLUMN material_id VARCHAR(64);

CREATE INDEX idx_import_task_material_id
    ON caliber_import_task (material_id);

ALTER TABLE caliber_import_task
    ADD CONSTRAINT fk_import_task_material
    FOREIGN KEY (material_id) REFERENCES caliber_source_material (material_id);
