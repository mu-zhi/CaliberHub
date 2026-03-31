ALTER TABLE caliber_llm_preprocess_config
    ADD COLUMN prompt_version BIGINT;

ALTER TABLE caliber_llm_preprocess_config
    ADD COLUMN prompt_hash VARCHAR(64);

UPDATE caliber_llm_preprocess_config
SET prompt_version = 1
WHERE prompt_version IS NULL;
