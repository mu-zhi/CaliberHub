ALTER TABLE caliber_llm_preprocess_config
    ADD COLUMN IF NOT EXISTS prompt_version BIGINT;

ALTER TABLE caliber_llm_preprocess_config
    ADD COLUMN IF NOT EXISTS prompt_hash VARCHAR(64);

UPDATE caliber_llm_preprocess_config
SET prompt_version = 1
WHERE prompt_version IS NULL;
