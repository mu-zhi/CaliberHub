CREATE TABLE caliber_import_candidate_graph_node (
    node_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    graph_id VARCHAR(128) NOT NULL,
    node_code VARCHAR(96) NOT NULL,
    scene_candidate_code VARCHAR(64),
    node_type VARCHAR(48) NOT NULL,
    node_label VARCHAR(255) NOT NULL,
    canonical_node_code VARCHAR(96),
    review_status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    confidence_score DOUBLE,
    payload_json LONGTEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_candidate_graph_node_code
    ON caliber_import_candidate_graph_node (node_code);

CREATE INDEX idx_candidate_graph_node_task_graph
    ON caliber_import_candidate_graph_node (task_id, graph_id, review_status, updated_at);

CREATE TABLE caliber_import_candidate_graph_edge (
    edge_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    graph_id VARCHAR(128) NOT NULL,
    edge_code VARCHAR(96) NOT NULL,
    scene_candidate_code VARCHAR(64),
    edge_type VARCHAR(48) NOT NULL,
    source_node_code VARCHAR(96) NOT NULL,
    target_node_code VARCHAR(96) NOT NULL,
    edge_label VARCHAR(255),
    review_status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    confidence_score DOUBLE,
    payload_json LONGTEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_candidate_graph_edge_code
    ON caliber_import_candidate_graph_edge (edge_code);

CREATE INDEX idx_candidate_graph_edge_task_graph
    ON caliber_import_candidate_graph_edge (task_id, graph_id, review_status, updated_at);

CREATE TABLE caliber_import_candidate_review_event (
    event_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    graph_id VARCHAR(128) NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_code VARCHAR(96) NOT NULL,
    action_type VARCHAR(16) NOT NULL,
    before_status VARCHAR(32),
    after_status VARCHAR(32),
    operator VARCHAR(64),
    reason_text VARCHAR(255),
    payload_json LONGTEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_candidate_review_event_task_graph
    ON caliber_import_candidate_review_event (task_id, graph_id, created_at);
