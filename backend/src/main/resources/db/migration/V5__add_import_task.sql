CREATE TABLE IF NOT EXISTS caliber_import_task (
    task_id VARCHAR(64) PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    current_step INT NOT NULL,
    source_type VARCHAR(32),
    source_name VARCHAR(255),
    operator VARCHAR(64),
    raw_text CLOB,
    preprocess_result_json CLOB,
    quality_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    compare_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    error_message CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_import_task_status_updated
    ON caliber_import_task (status, updated_at);

CREATE INDEX IF NOT EXISTS idx_import_task_updated_at
    ON caliber_import_task (updated_at);
