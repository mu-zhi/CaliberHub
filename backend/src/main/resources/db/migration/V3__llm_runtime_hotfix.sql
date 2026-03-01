-- Ensure runtime configuration aligns with current default model and stable timeout baseline.
UPDATE caliber_llm_preprocess_config
SET model = 'qwen3-max'
WHERE LOWER(TRIM(model)) = 'qwen3.5-plus';

UPDATE caliber_llm_preprocess_config
SET timeout_seconds = 90
WHERE timeout_seconds IS NOT NULL
  AND timeout_seconds < 60;
