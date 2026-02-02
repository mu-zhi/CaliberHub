package com.caliberhub.application.importdoc.schema;

import java.util.*;

/**
 * CALIBER_IMPORT_V1 JSON Schema 定义
 * 
 * 这是系统导入格式的权威定义，用于校验和生成模板。
 * 采用手写方式快速实现 P0，后续 P1 可迁移到 DTO 自动生成。
 */
public class ImportSchemaV1 {

    public static final String SCHEMA_ID = "caliber.import.v1";
    public static final String SCHEMA_VERSION = "1.0.0";
    public static final String DOC_TYPE = "CALIBER_IMPORT_V1";

    /**
     * 获取完整的 JSON Schema
     */
    public static Map<String, Object> getSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();

        // 元数据
        schema.put("schema_id", SCHEMA_ID);
        schema.put("schema_version", SCHEMA_VERSION);
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("title", DOC_TYPE);
        schema.put("type", "object");

        // 必填字段
        schema.put("required", List.of("doc_type", "schema_version", "source_type", "scenes"));

        // 顶层属性
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("doc_type", Map.of(
                "type", "string",
                "const", DOC_TYPE,
                "description", "文档类型标识，固定为 CALIBER_IMPORT_V1"));
        properties.put("schema_version", Map.of(
                "type", "string",
                "const", SCHEMA_VERSION,
                "description", "Schema 版本号"));
        properties.put("source_type", Map.of(
                "type", "string",
                "enum", List.of("PASTE_MD", "FILE_MD", "FILE_TXT", "IMAGE_OCR_TEXT"),
                "description", "输入来源类型"));
        properties.put("global", Map.of(
                "$ref", "#/$defs/Global",
                "description", "全局配置"));
        properties.put("scenes", Map.of(
                "type", "array",
                "minItems", 1,
                "items", Map.of("$ref", "#/$defs/Scene"),
                "description", "场景列表，至少包含一个场景"));
        properties.put("parse_report", Map.of(
                "$ref", "#/$defs/ParseReport",
                "description", "解析报告"));

        schema.put("properties", properties);

        // 定义 $defs
        schema.put("$defs", buildDefs());

        return schema;
    }

    private static Map<String, Object> buildDefs() {
        Map<String, Object> defs = new LinkedHashMap<>();

        // Global
        defs.put("Global", Map.of(
                "type", "object",
                "properties", Map.of(
                        "domain_guess", Map.of("type", List.of("string", "null"), "description", "推测的领域"),
                        "keywords", Map.of("type", "array", "items", Map.of("type", "string"), "description", "关键词"),
                        "notes", Map.of("type", "array", "items", Map.of("type", "string"), "description", "备注"))));

        // Scene
        Map<String, Object> sceneProps = new LinkedHashMap<>();
        sceneProps.put("scene_title", Map.of("type", "string", "minLength", 1, "description", "场景标题（必填）"));
        sceneProps.put("scene_code_guess", Map.of("type", "string", "description", "推测的场景编码"));
        sceneProps.put("domain_guess", Map.of("type", List.of("string", "null"), "description", "推测的领域"));
        sceneProps.put("contributors",
                Map.of("type", "array", "items", Map.of("type", "string"), "description", "贡献者列表"));
        sceneProps.put("owner_guess", Map.of("type", List.of("string", "null"), "description", "推测的负责人"));
        sceneProps.put("scene_description", Map.of("type", "string", "description", "场景描述"));
        sceneProps.put("caliber_definition", Map.of("type", "string", "description", "口径定义"));
        sceneProps.put("applicability", Map.of("type", "string", "description", "适用性说明"));
        sceneProps.put("boundaries", Map.of("type", "string", "description", "边界条件"));
        sceneProps.put("entities", Map.of("type", "array", "items", Map.of("type", "string"), "description", "涉及实体"));
        sceneProps.put("inputs", Map.of("$ref", "#/$defs/Inputs", "description", "输入参数"));
        sceneProps.put("outputs", Map.of("$ref", "#/$defs/Outputs", "description", "输出定义"));
        sceneProps.put("sql_blocks",
                Map.of("type", "array", "items", Map.of("$ref", "#/$defs/SqlBlock"), "description", "SQL 语句块列表"));
        sceneProps.put("source_tables_hint",
                Map.of("type", "array", "items", Map.of("$ref", "#/$defs/TableHint"), "description", "数据源表提示"));
        sceneProps.put("sensitive_fields_hint",
                Map.of("type", "array", "items", Map.of("$ref", "#/$defs/SensitiveHint"), "description", "敏感字段提示"));
        sceneProps.put("caveats",
                Map.of("type", "array", "items", Map.of("$ref", "#/$defs/Caveat"), "description", "注意事项/风险点"));
        sceneProps.put("unmapped_text", Map.of("type", "string", "description", "未能映射的原始文本"));
        sceneProps.put("quality", Map.of("$ref", "#/$defs/Quality", "description", "解析质量评估"));

        defs.put("Scene", Map.of(
                "type", "object",
                "required", List.of("scene_title", "inputs", "outputs", "sql_blocks", "quality", "unmapped_text"),
                "properties", sceneProps));

        // Inputs
        defs.put("Inputs", Map.of(
                "type", "object",
                "properties", Map.of(
                        "params",
                        Map.of("type", "array", "items", Map.of("$ref", "#/$defs/InputParam"), "description", "输入参数列表"),
                        "constraints", Map.of("type", "array", "items", Map.of("$ref", "#/$defs/InputConstraint"),
                                "description", "约束条件"))));

        // InputParam
        defs.put("InputParam", Map.of(
                "type", "object",
                "properties", Map.of(
                        "name_en", Map.of("type", "string", "description", "参数英文名"),
                        "name_zh", Map.of("type", "string", "description", "参数中文名"),
                        "type", Map.of("type", "string", "description", "参数类型"),
                        "required", Map.of("type", "boolean", "description", "是否必填"),
                        "example", Map.of("type", "string", "description", "示例值"),
                        "description", Map.of("type", "string", "description", "参数描述"))));

        // InputConstraint
        defs.put("InputConstraint", Map.of(
                "type", "object",
                "properties", Map.of(
                        "name", Map.of("type", "string", "description", "约束名称"),
                        "description", Map.of("type", "string", "description", "约束描述"),
                        "required", Map.of("type", "boolean", "description", "是否必须"),
                        "impact", Map.of("type", "string", "description", "不满足时的影响"))));

        // Outputs
        defs.put("Outputs", Map.of(
                "type", "object",
                "properties", Map.of(
                        "summary", Map.of("type", "string", "description", "输出概要描述"),
                        "fields", Map.of("type", "array", "items", Map.of("$ref", "#/$defs/OutputField"), "description",
                                "输出字段列表"))));

        // OutputField
        defs.put("OutputField", Map.of(
                "type", "object",
                "properties", Map.of(
                        "name", Map.of("type", "string", "description", "字段名"),
                        "type", Map.of("type", "string", "description", "字段类型"),
                        "description", Map.of("type", "string", "description", "字段描述"))));

        // SqlBlock
        defs.put("SqlBlock", Map.of(
                "type", "object",
                "required", List.of("block_id", "sql"),
                "properties", Map.of(
                        "block_id", Map.of("type", "string", "description", "SQL块唯一标识"),
                        "name", Map.of("type", "string", "description", "SQL块名称"),
                        "condition", Map.of("type", "string", "description", "适用条件"),
                        "sql", Map.of("type", "string", "description", "SQL语句"),
                        "notes", Map.of("type", "string", "description", "备注说明"))));

        // TableHint
        defs.put("TableHint", Map.of(
                "type", "object",
                "properties", Map.of(
                        "table_fullname", Map.of("type", "string", "description", "表全名 (schema.table)"),
                        "description", Map.of("type", "string", "description", "表描述"),
                        "is_key", Map.of("type", "boolean", "description", "是否关键表"))));

        // SensitiveHint
        defs.put("SensitiveHint", Map.of(
                "type", "object",
                "properties", Map.of(
                        "field_fullname", Map.of("type", "string", "description", "字段全名 (schema.table.field)"),
                        "level",
                        Map.of("type", "string", "enum", List.of("L1", "L2", "L3", "L4"), "description", "敏感级别"),
                        "remark", Map.of("type", "string", "description", "备注"))));

        // Caveat
        defs.put("Caveat", Map.of(
                "type", "object",
                "properties", Map.of(
                        "title", Map.of("type", "string", "description", "注意事项标题"),
                        "text", Map.of("type", "string", "description", "详细内容"),
                        "risk",
                        Map.of("type", "string", "enum", List.of("LOW", "MEDIUM", "HIGH"), "description", "风险等级"))));

        // Quality
        defs.put("Quality", Map.of(
                "type", "object",
                "properties", Map.of(
                        "confidence", Map.of("type", "number", "minimum", 0, "maximum", 1, "description", "解析置信度 0~1"),
                        "warnings", Map.of("type", "array", "items", Map.of("type", "string"), "description", "警告信息列表"),
                        "errors",
                        Map.of("type", "array", "items", Map.of("type", "string"), "description", "错误信息列表"))));

        // ParseReport
        defs.put("ParseReport", Map.of(
                "type", "object",
                "properties", Map.of(
                        "parser", Map.of("type", "string", "description", "解析器名称"),
                        "warnings", Map.of("type", "array", "items", Map.of("type", "string"), "description", "全局警告"),
                        "errors", Map.of("type", "array", "items", Map.of("type", "string"), "description", "全局错误"))));

        return defs;
    }
}
