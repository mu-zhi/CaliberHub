package com.caliberhub.application.importdoc.schema;

import java.util.*;

/**
 * 导入模板生成器
 * 
 * 根据 Schema 生成各种模式的 JSON 模板。
 */
public class ImportTemplateGenerator {

    /**
     * 生成空模板（结构完整，值为空/默认）
     */
    public static Map<String, Object> generateEmpty() {
        Map<String, Object> template = new LinkedHashMap<>();

        template.put("doc_type", ImportSchemaV1.DOC_TYPE);
        template.put("schema_version", ImportSchemaV1.SCHEMA_VERSION);
        template.put("source_type", "PASTE_MD");

        // global
        Map<String, Object> global = new LinkedHashMap<>();
        global.put("domain_guess", null);
        global.put("keywords", List.of());
        global.put("notes", List.of());
        template.put("global", global);

        // scenes - 包含一个空场景模板
        template.put("scenes", List.of(createEmptyScene()));

        // parse_report
        Map<String, Object> parseReport = new LinkedHashMap<>();
        parseReport.put("parser", "unknown");
        parseReport.put("warnings", List.of());
        parseReport.put("errors", List.of());
        template.put("parse_report", parseReport);

        return template;
    }

    /**
     * 生成带 SQL 示例的模板
     */
    public static Map<String, Object> generateExampleSql() {
        Map<String, Object> template = generateEmpty();

        // 替换 scenes 为带 SQL 示例的场景
        Map<String, Object> scene = createEmptyScene();
        scene.put("scene_title", "用户订单统计场景");
        scene.put("scene_description", "统计指定时间范围内的用户订单数量和金额");
        scene.put("caliber_definition", "订单金额 = SUM(order_amount)，仅统计已支付订单");

        // 输入参数示例
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("params", List.of(
                createInputParam("start_date", "开始日期", "DATE", true, "2024-01-01", "统计开始日期"),
                createInputParam("end_date", "结束日期", "DATE", true, "2024-01-31", "统计结束日期"),
                createInputParam("user_id", "用户ID", "STRING", false, "U001", "可选，指定用户")));
        inputs.put("constraints", List.of(
                createConstraint("时间范围限制", "开始日期不能大于结束日期", true, "查询失败")));
        scene.put("inputs", inputs);

        // 输出示例
        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("summary", "返回用户维度的订单统计数据");
        outputs.put("fields", List.of(
                createOutputField("user_id", "STRING", "用户ID"),
                createOutputField("order_count", "BIGINT", "订单数量"),
                createOutputField("total_amount", "DECIMAL", "订单总金额")));
        scene.put("outputs", outputs);

        // SQL 示例
        scene.put("sql_blocks", List.of(
                createSqlBlock("main", "主查询", null,
                        "SELECT\n  user_id,\n  COUNT(*) AS order_count,\n  SUM(order_amount) AS total_amount\nFROM dw.order_fact\nWHERE pay_status = 'PAID'\n  AND order_date BETWEEN :start_date AND :end_date\nGROUP BY user_id",
                        "仅统计已支付订单"),
                createSqlBlock("with_user_filter", "带用户过滤", "当指定 user_id 时使用",
                        "SELECT\n  user_id,\n  COUNT(*) AS order_count,\n  SUM(order_amount) AS total_amount\nFROM dw.order_fact\nWHERE pay_status = 'PAID'\n  AND order_date BETWEEN :start_date AND :end_date\n  AND user_id = :user_id\nGROUP BY user_id",
                        null)));

        // 数据源表提示
        scene.put("source_tables_hint", List.of(
                createTableHint("dw.order_fact", "订单事实表", true)));

        // 注意事项
        scene.put("caveats", List.of(
                createCaveat("数据延迟", "订单数据 T+1 更新，当日数据不完整", "MEDIUM")));

        // 质量
        Map<String, Object> quality = new LinkedHashMap<>();
        quality.put("confidence", 0.95);
        quality.put("warnings", List.of());
        quality.put("errors", List.of());
        scene.put("quality", quality);

        template.put("scenes", List.of(scene));
        return template;
    }

    /**
     * 生成规则口径示例的模板
     */
    public static Map<String, Object> generateExampleRule() {
        Map<String, Object> template = generateEmpty();

        Map<String, Object> scene = createEmptyScene();
        scene.put("scene_title", "活跃用户判定规则");
        scene.put("scene_description", "定义什么样的用户算作'活跃用户'");
        scene.put("caliber_definition", "活跃用户：最近30天内有登录行为或下单行为的用户");
        scene.put("applicability", "适用于所有用户运营分析场景");
        scene.put("boundaries", "不包含内部测试账号");
        scene.put("entities", List.of("用户", "登录行为", "订单"));

        // 无 SQL，纯规则定义
        scene.put("sql_blocks", List.of());

        // 注意事项
        scene.put("caveats", List.of(
                createCaveat("口径变更历史", "2023年前使用7天作为活跃判定周期", "LOW"),
                createCaveat("节假日影响", "节假日期间活跃定义可能临时调整", "MEDIUM")));

        Map<String, Object> quality = new LinkedHashMap<>();
        quality.put("confidence", 0.85);
        quality.put("warnings", List.of("未提供具体SQL实现"));
        quality.put("errors", List.of());
        scene.put("quality", quality);

        template.put("scenes", List.of(scene));
        return template;
    }

    /**
     * 根据 mode 获取模板
     */
    public static Map<String, Object> generate(String mode) {
        return switch (mode) {
            case "example_sql" -> generateExampleSql();
            case "example_rule" -> generateExampleRule();
            default -> generateEmpty();
        };
    }

    // ========== 辅助方法 ==========

    private static Map<String, Object> createEmptyScene() {
        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("scene_title", "（必填）场景标题");
        scene.put("scene_code_guess", "");
        scene.put("domain_guess", null);
        scene.put("contributors", List.of());
        scene.put("owner_guess", null);
        scene.put("scene_description", "");
        scene.put("caliber_definition", "");
        scene.put("applicability", "");
        scene.put("boundaries", "");
        scene.put("entities", List.of());

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("params", List.of());
        inputs.put("constraints", List.of());
        scene.put("inputs", inputs);

        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("summary", "");
        outputs.put("fields", List.of());
        scene.put("outputs", outputs);

        scene.put("sql_blocks", List.of());
        scene.put("source_tables_hint", List.of());
        scene.put("sensitive_fields_hint", List.of());
        scene.put("caveats", List.of());
        scene.put("unmapped_text", "");

        Map<String, Object> quality = new LinkedHashMap<>();
        quality.put("confidence", 0.0);
        quality.put("warnings", List.of());
        quality.put("errors", List.of());
        scene.put("quality", quality);

        return scene;
    }

    private static Map<String, Object> createInputParam(String nameEn, String nameZh, String type,
            boolean required, String example, String description) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("name_en", nameEn);
        param.put("name_zh", nameZh);
        param.put("type", type);
        param.put("required", required);
        param.put("example", example);
        param.put("description", description);
        return param;
    }

    private static Map<String, Object> createConstraint(String name, String description,
            boolean required, String impact) {
        Map<String, Object> constraint = new LinkedHashMap<>();
        constraint.put("name", name);
        constraint.put("description", description);
        constraint.put("required", required);
        constraint.put("impact", impact);
        return constraint;
    }

    private static Map<String, Object> createOutputField(String name, String type, String description) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", name);
        field.put("type", type);
        field.put("description", description);
        return field;
    }

    private static Map<String, Object> createSqlBlock(String blockId, String name, String condition,
            String sql, String notes) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("block_id", blockId);
        block.put("name", name);
        block.put("condition", condition);
        block.put("sql", sql);
        block.put("notes", notes);
        return block;
    }

    private static Map<String, Object> createTableHint(String tableFullname, String description, boolean isKey) {
        Map<String, Object> hint = new LinkedHashMap<>();
        hint.put("table_fullname", tableFullname);
        hint.put("description", description);
        hint.put("is_key", isKey);
        return hint;
    }

    private static Map<String, Object> createCaveat(String title, String text, String risk) {
        Map<String, Object> caveat = new LinkedHashMap<>();
        caveat.put("title", title);
        caveat.put("text", text);
        caveat.put("risk", risk);
        return caveat;
    }
}
