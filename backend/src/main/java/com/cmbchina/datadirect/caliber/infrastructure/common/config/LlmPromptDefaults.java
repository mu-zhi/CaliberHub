package com.cmbchina.datadirect.caliber.infrastructure.common.config;

public final class LlmPromptDefaults {

    public static final String PREPROCESS_SYSTEM_PROMPT = """
            <role>
            你是“数据直通车”口径导入链路中的专业预处理引擎。
            你的输出会被后端系统严格反序列化并转换为 CALIBER_IMPORT_V2 标准资产。
            </role>

            <output_contract>
            1) 只输出一个 JSON 对象，不要输出 Markdown、代码围栏、解释、前言或后语。
            2) 输出必须以 "{" 开头，以 "}" 结尾，并且满足用户消息提供的 Schema。
            3) 若无法确认某字段，留空或写入 unresolved；不要编造。
            4) 所有无法结构化但仍有价值的信息，全部保留到 carry_over_text。
            </output_contract>

            <sql_preservation_rules>
            1) sql_segments[*].sql_raw 必须 100% 复制原始文档中的 SQL 字符串。
            2) 绝对禁止格式化缩进、修复语法错误、删除 SQL 注释（-- 或 /* */）。
            3) 行号标签只用于定位，不属于 SQL 本体；sql_raw 中不要包含行号标签。
            </sql_preservation_rules>

            <scene_binding_rules>
            1) 按业务意图或标题切换拆分 scene_candidates，但必须优先识别“同一业务问题下的多种取数方案”。
            2) 当文档出现 Step（如 Step 1/2/3）、方法1/方法2、明确时间分段（如 2009-2012、2014-至今）或历史表切换时，若它们仍服务于同一个业务场景，必须保留为同一个 scene_candidate，并把差异下沉到多个 sql_segment_ids / plan variants；不得仅因取数路径不同就拆成多个 scene_candidates。
            3) 仅当业务意图、问题对象或标题已经明显切换时，才拆成多个 scene_candidates。
            4) 给每一个抽取出的 SQL 片段分配独立 segment_id（如 SQL_001），并在对应 scene_candidates[*].sql_segment_ids 中建立关联。
            5) 同一场景横跨多张历史表（多段 SQL）时，必须关联全部对应 segment_id。
            6) scene_title 禁止直接使用表名或纯英文拼接；优先使用 SQL 上下文邻近的中文业务注释。只有在没有可用中文注释时，才允许使用“查询 [表名]”兜底。
            </scene_binding_rules>

            <line_anchor_rules>
            1) 输入 RAW_DOC 每行已附加行号标签（如 [001], [002]）。
            2) evidence_lines 和 source_spans 必须提取这些标签中的数字，禁止自行估算行号。
            </line_anchor_rules>

            <field_mapping_rules>
            1) 高度关注 SQL 注释和 CASE WHEN 中隐含的字典/码值映射（例如：020=金卡，040=金葵花卡，A=新增）。
            2) 只有在原文明确出现键值映射时，才允许填充 field_hints[*].extracted_mappings。
            3) 将提取到的码值键值对写入 extracted_mappings，并在 meaning_hint 中补充业务语义。
            4) 无法确认时必须留空，绝对禁止把无关段落拼接成伪映射。
            </field_mapping_rules>

            <text_cleanup_rules>
            1) 输出 scene_description_hint、carry_over_text 时，清理连续分隔符、多余注释边界符、无业务语义的 Markdown 装饰字符。
            2) 保留业务含义，不保留排版噪声；最终描述必须为可读中文业务句子。
            3) 文档开头的大段业务背景优先归入第一个核心场景的 scene_description_hint 或 context，不要直接丢到 unresolved。
            </text_cleanup_rules>

            <exception_rules>
            1) 发现口径文档存在歧义、缺漏或 OCR 乱码时，必须在 quality.warnings 中添加告警，并在 risk_notes 中说明问题位置。
            2) 无 SQL：doc_profile.has_sql=false，sql_segments=[]；仍输出 scene_candidates、field_hints、risk_notes。
            3) 文档被截断：doc_profile.is_truncated=true，并在 quality.errors 写 context_truncated。
            4) 多场景混杂且边界不清：在 unresolved 写明不确定边界来源。
            5) 存在多版本口径冲突：写入 risk_notes，并把冲突说明保留在 carry_over_text。
            6) applicable_period 优先识别“历史表/旧系统/2014年之前/目前主表”等时段描述。
            </exception_rules>

            <self_check>
            在输出前再次确认：
            1) 结果是单个 JSON 对象且满足 Schema。
            2) SQL 保持原文，不含行号标签。
            3) 同一业务问题下的多种取数方案没有被错误拆成多个 scene_candidates。
            4) 每个 scene_candidate 都正确关联了 sql_segment_ids。
            5) 不确定信息只出现在 unresolved、warnings、errors 或留空字段中。
            </self_check>
            """;

    public static final String PREP_SCHEMA_JSON = """
            {
              "prep_type": "CALIBER_PREP_V1",
              "schema_version": "1.2.0",
              "source_type": "PASTE_MD|FILE_MD|FILE_TXT|FILE_SQL|IMAGE_OCR_TEXT",
              "doc_profile": {
                "language": "zh|en|mixed|unknown",
                "has_sql": true,
                "estimated_scene_count": 1,
                "ocr_noise_level": "LOW|MEDIUM|HIGH",
                "is_truncated": false
              },
              "context": {
                "domain_guess": "string",
                "document_title": "string"
              },
              "normalized_text": "string",
              "scene_candidates": [
                {
                  "scene_id": "S001",
                  "scene_title": "string",
                  "scene_description_hint": "string",
                  "sql_segment_ids": ["SQL_001"],
                  "confidence": 0.0,
                  "evidence_lines": [1, 2, 3]
                }
              ],
              "sql_segments": [
                {
                  "segment_id": "SQL_001",
                  "name_hint": "string",
                  "applicable_period": "string",
                  "sql_raw": "string",
                  "sql_type": "SELECT|WITH|INSERT|UPDATE|DELETE|DDL|UNKNOWN",
                  "is_complete": true,
                  "source_spans": [{"start_line": 1, "end_line": 20}],
                  "warnings": []
                }
              ],
              "table_hints": [
                {
                  "table": "schema.table",
                  "from_segment_id": "SQL_001",
                  "confidence": 0.0
                }
              ],
              "field_hints": [
                {
                  "field": "field_name",
                  "table": "schema.table",
                  "meaning_hint": "string",
                  "extracted_mappings": {"code_value": "code_description"},
                  "confidence": 0.0
                }
              ],
              "risk_notes": [
                {
                  "title": "string",
                  "text": "string",
                  "risk": "LOW|MEDIUM|HIGH",
                  "evidence_lines": [10, 11]
                }
              ],
              "carry_over_text": "string",
              "unresolved": ["string"],
              "quality": {
                "confidence": 0.0,
                "warnings": ["string"],
                "errors": ["string"]
              }
            }
            """;

    public static final String PREPROCESS_USER_PROMPT_TEMPLATE = """
            <task>
            请将以下带行号标签的原始文档抽取为一个且仅一个符合 PREP_SCHEMA 的 JSON 对象。
            保持当前业务行为：如果 Step、方法、时段或历史表切换仍在回答同一个业务问题，不要仅因取数路径不同拆成多个 scene_candidates；应把差异下沉到多个 sql_segment_ids / plan variants。
            </task>

            <completion_criteria>
            1) 已将 SQL 片段 ID 关联到对应场景（scene_candidates[].sql_segment_ids）。
            2) 已提取注释和 CASE WHEN 中明确出现的码值字典（field_hints[].extracted_mappings）。
            3) SQL 保留原始字符与注释，且不包含行号标签。
            4) 只有在业务意图真实切换时才拆分多个 scene_candidates。
            5) 最终回答只包含 JSON，不包含解释性文本。
            </completion_criteria>

            <json_schema>
            {{DYNAMIC_JSON_SCHEMA}}
            </json_schema>

            <context>
            {
              "source_type": "{{SOURCE_TYPE}}",
              "lang": "zh",
              "target": "caliber_import_v2"
            }
            </context>

            <raw_doc>
            {{RAW_DOC}}
            </raw_doc>

            现在返回最终 JSON。只返回 JSON。
            """;

    private LlmPromptDefaults() {
    }
}
