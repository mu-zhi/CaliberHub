package com.caliberhub.application.importdoc.schema;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 导入数据校验器
 * 
 * 基于 Schema 定义校验导入的 JSON 数据。
 */
@Slf4j
@Component
public class ImportSchemaValidator {

    /**
     * 校验结果
     */
    public record ValidationResult(
            boolean valid,
            List<Violation> violations) {
        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failure(List<Violation> violations) {
            return new ValidationResult(false, violations);
        }
    }

    /**
     * 校验违规项
     */
    public record Violation(
            String path,
            String reason,
            String value) {
    }

    /**
     * 校验导入 JSON 数据
     * 
     * @param data 导入的 JSON 数据（Map 形式）
     * @return 校验结果
     */
    @SuppressWarnings("unchecked")
    public ValidationResult validate(Map<String, Object> data) {
        List<Violation> violations = new ArrayList<>();

        // 1. 校验 doc_type
        String docType = getStringValue(data, "doc_type");
        if (docType == null) {
            violations.add(new Violation("$.doc_type", "required", null));
        } else if (!ImportSchemaV1.DOC_TYPE.equals(docType)) {
            violations.add(new Violation("$.doc_type",
                    "must be '" + ImportSchemaV1.DOC_TYPE + "'", docType));
        }

        // 2. 校验 schema_version
        String schemaVersion = getStringValue(data, "schema_version");
        if (schemaVersion == null) {
            violations.add(new Violation("$.schema_version", "required", null));
        } else if (!ImportSchemaV1.SCHEMA_VERSION.equals(schemaVersion)) {
            // 允许兼容的版本
            if (!isCompatibleVersion(schemaVersion)) {
                violations.add(new Violation("$.schema_version",
                        "unsupported version, current is " + ImportSchemaV1.SCHEMA_VERSION,
                        schemaVersion));
            }
        }

        // 3. 校验 source_type
        String sourceType = getStringValue(data, "source_type");
        if (sourceType == null) {
            violations.add(new Violation("$.source_type", "required", null));
        } else {
            List<String> validSourceTypes = List.of("PASTE_MD", "FILE_MD", "FILE_TXT", "IMAGE_OCR_TEXT");
            if (!validSourceTypes.contains(sourceType)) {
                violations.add(new Violation("$.source_type",
                        "must be one of " + validSourceTypes, sourceType));
            }
        }

        // 4. 校验 scenes
        Object scenesObj = data.get("scenes");
        if (scenesObj == null) {
            violations.add(new Violation("$.scenes", "required", null));
        } else if (!(scenesObj instanceof List)) {
            violations.add(new Violation("$.scenes", "must be array", scenesObj.getClass().getSimpleName()));
        } else {
            List<Object> scenes = (List<Object>) scenesObj;
            if (scenes.isEmpty()) {
                violations.add(new Violation("$.scenes", "minItems=1", "[]"));
            } else {
                // 校验每个 scene
                for (int i = 0; i < scenes.size(); i++) {
                    Object sceneObj = scenes.get(i);
                    if (sceneObj instanceof Map) {
                        validateScene((Map<String, Object>) sceneObj, "$.scenes[" + i + "]", violations);
                    } else {
                        violations.add(new Violation("$.scenes[" + i + "]", "must be object",
                                sceneObj != null ? sceneObj.getClass().getSimpleName() : "null"));
                    }
                }
            }
        }

        if (violations.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(violations);
    }

    /**
     * 校验单个 Scene
     */
    @SuppressWarnings("unchecked")
    private void validateScene(Map<String, Object> scene, String basePath, List<Violation> violations) {
        // 必填字段：scene_title
        String sceneTitle = getStringValue(scene, "scene_title");
        if (sceneTitle == null || sceneTitle.isBlank()) {
            violations.add(new Violation(basePath + ".scene_title", "required, minLength=1",
                    sceneTitle == null ? null : "\"\""));
        }

        // inputs 必须是对象
        Object inputsObj = scene.get("inputs");
        if (inputsObj != null && !(inputsObj instanceof Map)) {
            violations.add(new Violation(basePath + ".inputs", "must be object",
                    inputsObj.getClass().getSimpleName()));
        }

        // outputs 必须是对象
        Object outputsObj = scene.get("outputs");
        if (outputsObj != null && !(outputsObj instanceof Map)) {
            violations.add(new Violation(basePath + ".outputs", "must be object",
                    outputsObj.getClass().getSimpleName()));
        }

        // sql_blocks 必须是数组
        Object sqlBlocksObj = scene.get("sql_blocks");
        if (sqlBlocksObj != null) {
            if (!(sqlBlocksObj instanceof List)) {
                violations.add(new Violation(basePath + ".sql_blocks", "must be array",
                        sqlBlocksObj.getClass().getSimpleName()));
            } else {
                List<Object> sqlBlocks = (List<Object>) sqlBlocksObj;
                for (int i = 0; i < sqlBlocks.size(); i++) {
                    Object blockObj = sqlBlocks.get(i);
                    if (blockObj instanceof Map) {
                        validateSqlBlock((Map<String, Object>) blockObj,
                                basePath + ".sql_blocks[" + i + "]", violations);
                    }
                }
            }
        }

        // quality 必须是对象
        Object qualityObj = scene.get("quality");
        if (qualityObj != null && !(qualityObj instanceof Map)) {
            violations.add(new Violation(basePath + ".quality", "must be object",
                    qualityObj.getClass().getSimpleName()));
        }

        // caveats 必须是数组
        Object caveatsObj = scene.get("caveats");
        if (caveatsObj != null) {
            if (!(caveatsObj instanceof List)) {
                violations.add(new Violation(basePath + ".caveats", "must be array",
                        caveatsObj.getClass().getSimpleName()));
            } else {
                List<Object> caveats = (List<Object>) caveatsObj;
                for (int i = 0; i < caveats.size(); i++) {
                    Object caveatObj = caveats.get(i);
                    if (caveatObj instanceof Map) {
                        validateCaveat((Map<String, Object>) caveatObj,
                                basePath + ".caveats[" + i + "]", violations);
                    }
                }
            }
        }
    }

    /**
     * 校验 SqlBlock
     */
    private void validateSqlBlock(Map<String, Object> block, String basePath, List<Violation> violations) {
        // block_id 必填
        String blockId = getStringValue(block, "block_id");
        if (blockId == null || blockId.isBlank()) {
            violations.add(new Violation(basePath + ".block_id", "required", null));
        }

        // sql 必填
        String sql = getStringValue(block, "sql");
        if (sql == null || sql.isBlank()) {
            violations.add(new Violation(basePath + ".sql", "required", null));
        }
    }

    /**
     * 校验 Caveat
     */
    private void validateCaveat(Map<String, Object> caveat, String basePath, List<Violation> violations) {
        // risk 枚举校验
        String risk = getStringValue(caveat, "risk");
        if (risk != null) {
            List<String> validRisks = List.of("LOW", "MEDIUM", "HIGH");
            if (!validRisks.contains(risk)) {
                violations.add(new Violation(basePath + ".risk",
                        "must be one of " + validRisks, risk));
            }
        }
    }

    /**
     * 检查版本兼容性
     */
    private boolean isCompatibleVersion(String version) {
        // 1.0.x 系列都兼容
        if (version == null)
            return false;
        return version.startsWith("1.0.");
    }

    /**
     * 安全获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }
}
