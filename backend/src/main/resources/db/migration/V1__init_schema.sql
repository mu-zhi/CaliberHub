CREATE TABLE caliber_domain (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    domain_code VARCHAR(64) NOT NULL,
    domain_name VARCHAR(120) NOT NULL,
    domain_overview CLOB,
    common_tables CLOB,
    contacts CLOB,
    sort_order INT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_domain_code ON caliber_domain (domain_code);
CREATE INDEX idx_domain_sort_order ON caliber_domain (sort_order);
CREATE INDEX idx_domain_updated_at ON caliber_domain (updated_at);

CREATE TABLE caliber_scene (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_code VARCHAR(64) NOT NULL,
    scene_title VARCHAR(200) NOT NULL,
    domain_id BIGINT,
    domain VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    scene_description CLOB,
    caliber_definition CLOB,
    applicability CLOB,
    boundaries CLOB,
    inputs_json CLOB,
    outputs_json CLOB,
    sql_variants_json CLOB,
    code_mappings_json CLOB,
    contributors VARCHAR(500),
    sql_blocks_json CLOB,
    source_tables_json CLOB,
    caveats_json CLOB,
    unmapped_text CLOB,
    quality_json CLOB,
    raw_input CLOB,
    verified_at TIMESTAMP NULL,
    change_summary CLOB,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    published_by VARCHAR(64),
    published_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uk_scene_code ON caliber_scene (scene_code);
CREATE INDEX idx_scene_domain_status ON caliber_scene (domain, status);
CREATE INDEX idx_scene_domain_id_status ON caliber_scene (domain_id, status);
CREATE INDEX idx_scene_updated_at ON caliber_scene (updated_at);

CREATE TABLE caliber_llm_preprocess_config (
    id BIGINT PRIMARY KEY,
    enabled BOOLEAN NOT NULL,
    endpoint VARCHAR(500),
    api_key_ciphertext CLOB,
    model VARCHAR(100),
    timeout_seconds INT,
    temperature DOUBLE,
    max_tokens INT,
    fallback_to_rule BOOLEAN NOT NULL,
    preprocess_system_prompt CLOB,
    preprocess_user_prompt_template CLOB,
    prep_schema_json CLOB,
    updated_by VARCHAR(64),
    updated_at TIMESTAMP NULL
);
