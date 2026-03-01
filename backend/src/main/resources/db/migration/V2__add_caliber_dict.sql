CREATE TABLE IF NOT EXISTS caliber_dict (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    domain_scope VARCHAR(64) NOT NULL,
    domain_id BIGINT,
    code VARCHAR(128) NOT NULL,
    value_code VARCHAR(128) NOT NULL,
    value_name VARCHAR(500),
    description CLOB,
    last_scene_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_dict_scope_code_value UNIQUE (domain_scope, code, value_code),
    INDEX idx_dict_scope_code (domain_scope, code),
    INDEX idx_dict_updated_at (updated_at)
);
