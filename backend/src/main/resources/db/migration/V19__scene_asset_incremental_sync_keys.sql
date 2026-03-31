ALTER TABLE caliber_source_contract
    ADD COLUMN normalized_physical_table VARCHAR(255);

UPDATE caliber_source_contract
SET normalized_physical_table = UPPER(TRIM(physical_table))
WHERE normalized_physical_table IS NULL;

CREATE INDEX idx_source_contract_scene_norm_table
    ON caliber_source_contract (scene_id, normalized_physical_table, updated_at);

ALTER TABLE caliber_policy
    ADD COLUMN policy_semantic_key VARCHAR(255);

UPDATE caliber_policy
SET policy_semantic_key = policy_code
WHERE policy_semantic_key IS NULL;

CREATE INDEX idx_policy_scope_semantic_key
    ON caliber_policy (scope_type, scope_ref_id, policy_semantic_key, updated_at);

ALTER TABLE caliber_evidence_fragment
    ADD COLUMN origin_type VARCHAR(64);

ALTER TABLE caliber_evidence_fragment
    ADD COLUMN origin_ref VARCHAR(500);

ALTER TABLE caliber_evidence_fragment
    ADD COLUMN origin_locator VARCHAR(500);

UPDATE caliber_evidence_fragment
SET origin_type = COALESCE(origin_type, source_type),
    origin_ref = COALESCE(origin_ref, source_ref),
    origin_locator = COALESCE(origin_locator, source_anchor)
WHERE origin_type IS NULL
   OR origin_ref IS NULL
   OR origin_locator IS NULL;

CREATE INDEX idx_evidence_scene_origin_key
    ON caliber_evidence_fragment (scene_id, origin_type, origin_ref, updated_at);

ALTER TABLE caliber_output_contract
    ADD COLUMN contract_semantic_key VARCHAR(255);

UPDATE caliber_output_contract
SET contract_semantic_key = contract_code
WHERE contract_semantic_key IS NULL;

CREATE INDEX idx_output_contract_scene_semantic_key
    ON caliber_output_contract (scene_id, contract_semantic_key, updated_at);
