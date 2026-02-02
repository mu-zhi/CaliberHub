package com.caliberhub.application.importdoc.schema;

/**
 * 提示词片段生成器
 * 
 * 生成可直接粘贴到 Claude/GPT 提示词中的格式说明片段。
 */
public class ImportPromptSnippetGenerator {

    /**
     * 生成完整的提示词片段（包含规则和模板）
     */
    public static String generateFull(String lang) {
        StringBuilder sb = new StringBuilder();

        if ("en".equals(lang)) {
            sb.append(generateFullEnglish());
        } else {
            sb.append(generateFullChinese());
        }

        return sb.toString();
    }

    /**
     * 生成简洁版提示词片段（仅规则）
     */
    public static String generateCompact(String lang) {
        if ("en".equals(lang)) {
            return generateCompactEnglish();
        }
        return generateCompactChinese();
    }

    // ========== 中文版本 ==========

    private static String generateFullChinese() {
        return """
                ## 导入格式规范

                你需要将用户输入的文档转换为 CaliberHub 系统的标准导入格式。请严格遵循以下规则：

                ### 硬性规则

                1. **必须**使用 `CALIBER_IMPORT_V1` 作为 `doc_type`
                2. **必须**设置 `schema_version` 为 `1.0.0`
                3. **必须**至少包含一个 `scene`（场景）
                4. 每个 `scene` **必须**有 `scene_title`（场景标题）
                5. 每个 `sql_block` **必须**有 `block_id` 和 `sql` 字段
                6. `source_type` 只能是：`PASTE_MD`、`FILE_MD`、`FILE_TXT`、`IMAGE_OCR_TEXT`
                7. `risk` 字段只能是：`LOW`、`MEDIUM`、`HIGH`

                ### 输出格式模板

                ```json
                {
                  "doc_type": "CALIBER_IMPORT_V1",
                  "schema_version": "1.0.0",
                  "source_type": "PASTE_MD",
                  "global": {
                    "domain_guess": null,
                    "keywords": [],
                    "notes": []
                  },
                  "scenes": [
                    {
                      "scene_title": "场景标题（必填）",
                      "scene_description": "场景的业务描述",
                      "caliber_definition": "口径的精确定义",
                      "inputs": {
                        "params": [
                          {
                            "name_en": "param_name",
                            "name_zh": "参数名称",
                            "type": "STRING",
                            "required": true,
                            "example": "示例值",
                            "description": "参数说明"
                          }
                        ],
                        "constraints": []
                      },
                      "outputs": {
                        "summary": "输出概要",
                        "fields": []
                      },
                      "sql_blocks": [
                        {
                          "block_id": "main",
                          "name": "主查询",
                          "condition": null,
                          "sql": "SELECT * FROM table",
                          "notes": "备注"
                        }
                      ],
                      "caveats": [
                        {
                          "title": "注意事项标题",
                          "text": "详细内容",
                          "risk": "MEDIUM"
                        }
                      ],
                      "quality": {
                        "confidence": 0.85,
                        "warnings": [],
                        "errors": []
                      },
                      "unmapped_text": ""
                    }
                  ],
                  "parse_report": {
                    "parser": "claude",
                    "warnings": [],
                    "errors": []
                  }
                }
                ```

                ### 字段映射指南

                | 用户输入关键词 | 映射字段 |
                |--------------|---------|
                | 场景/用途/目的 | scene_description |
                | 口径/定义/公式 | caliber_definition |
                | 输入/参数 | inputs.params |
                | 输出/结果 | outputs |
                | SQL/查询/代码 | sql_blocks |
                | 注意/风险/问题 | caveats |

                """;
    }

    private static String generateCompactChinese() {
        return """
                请将文档转换为 CALIBER_IMPORT_V1 格式。规则：
                1. doc_type 必须是 "CALIBER_IMPORT_V1"，schema_version 必须是 "1.0.0"
                2. 至少包含一个 scene，每个 scene 必须有 scene_title
                3. sql_blocks 每项必须有 block_id 和 sql
                4. source_type: PASTE_MD|FILE_MD|FILE_TXT|IMAGE_OCR_TEXT
                5. risk: LOW|MEDIUM|HIGH
                """;
    }

    // ========== 英文版本 ==========

    private static String generateFullEnglish() {
        return """
                ## Import Format Specification

                Convert user input to CaliberHub standard import format. Follow these rules strictly:

                ### Hard Rules

                1. **MUST** use `CALIBER_IMPORT_V1` as `doc_type`
                2. **MUST** set `schema_version` to `1.0.0`
                3. **MUST** include at least one `scene`
                4. Each `scene` **MUST** have `scene_title`
                5. Each `sql_block` **MUST** have `block_id` and `sql`
                6. `source_type` can only be: `PASTE_MD`, `FILE_MD`, `FILE_TXT`, `IMAGE_OCR_TEXT`
                7. `risk` can only be: `LOW`, `MEDIUM`, `HIGH`

                ### Output Template

                ```json
                {
                  "doc_type": "CALIBER_IMPORT_V1",
                  "schema_version": "1.0.0",
                  "source_type": "PASTE_MD",
                  "scenes": [
                    {
                      "scene_title": "Scene Title (Required)",
                      "scene_description": "Business description",
                      "caliber_definition": "Precise definition",
                      "sql_blocks": [
                        {
                          "block_id": "main",
                          "name": "Main Query",
                          "sql": "SELECT * FROM table"
                        }
                      ],
                      "quality": {
                        "confidence": 0.85,
                        "warnings": [],
                        "errors": []
                      }
                    }
                  ]
                }
                ```
                """;
    }

    private static String generateCompactEnglish() {
        return """
                Convert document to CALIBER_IMPORT_V1 format. Rules:
                1. doc_type must be "CALIBER_IMPORT_V1", schema_version must be "1.0.0"
                2. At least one scene with scene_title
                3. sql_blocks items must have block_id and sql
                4. source_type: PASTE_MD|FILE_MD|FILE_TXT|IMAGE_OCR_TEXT
                5. risk: LOW|MEDIUM|HIGH
                """;
    }
}
