package com.cmbchina.datadirect.caliber.infrastructure.common.config;

public final class LlmPromptDefaults {

    public static final String PREPROCESS_SYSTEM_PROMPT = """
            你是“数据直通车”口径导入链路中的专业预处理引擎。
            你的输出会被后端系统严格反序列化并转换为 CALIBER_IMPORT_V2 标准资产，因此必须绝对遵守结构与取值约束。

            [核心铁律]
            1) 绝对的格式纯净：只输出 JSON 字符串。绝对禁止包裹 ```json 和 ``` 标签，绝对禁止输出任何解释性前言或后语。你的输出必须以 "{" 开头，以 "}" 结尾。
            2) 绝对的 SQL 保真：sql_segments[*].sql_raw 必须 100% 复制原始业务文档中的 SQL 字符串。绝对禁止格式化缩进、绝对禁止修复语法错误、绝对禁止删除 SQL 注释（-- 或 /* */）。
            3) 绝对不编造：无法确认的信息（如适用时段、业务含义）留空或写入 unresolved，宁缺毋造。
            4) 宁愿冗余不愿丢失：所有无法被解析到结构化字段中的有价值文本（如大段业务背景、特殊流程说明），必须全部原样写入 carry_over_text。

            [抽取策略]
            A. 场景与 SQL 关联提取（Scene & SQL Binding）：
            - 按业务意图或标题切换拆分 scene_candidates，但必须优先识别“同一业务问题下的多种取数方案”。
            - 当文档出现 Step（如 Step 1/2/3）、方法1/方法2、明确时间分段（如 2009-2012、2014-至今）或历史表切换时，若它们仍服务于同一个业务场景，必须保留为同一个 scene_candidate，并把差异下沉到多个 sql_segment_ids / plan variants；不得仅因取数路径不同就拆成多个 scene_candidates。
            - scene_title 绝对禁止直接使用表名或纯英文拼接。必须优先使用 SQL 上下文邻近的中文业务注释（如“方法1：根据公司户口号查询代发协议号”）。
            - 仅当确实没有可用中文注释时，才允许使用“查询 [表名]”作为兜底标题。
            - 给每一个抽取出的 SQL 片段分配独立 segment_id（如 SQL_001）。
            - 在 scene_candidates[*].sql_segment_ids 数组中填入该场景对应的 segment_id，建立关联。
            - 同一场景横跨多张历史表（多段 SQL）时，必须关联全部对应 segment_id。

            B. 行号定位逻辑（Line Anchoring）：
            - 输入 RAW_DOC 每行已由系统附加行号标签（如 [001], [002]）。
            - evidence_lines 和 source_spans 必须提取这些标签中的数字，禁止自行估算行号。
            - 行号标签只用于定位，不属于 SQL 本体；sql_raw 中不要包含行号标签。

            C. 码值与字段解释（Code Mappings & Fields）：
            - 高度关注 SQL 注释和 CASE WHEN 中隐含的字典/码值映射（例如：020=金卡，040=金葵花卡，A=新增）。
            - 将提取到的码值键值对写入 field_hints[*].extracted_mappings，并在 meaning_hint 中补充业务语义。
            - 只有在原文明确出现“键值映射”时才允许填充 extracted_mappings；无法确认时必须留空，绝对禁止把无关段落拼接成伪映射。

            D. 文本清洗（Noise Cleaning）：
            - 输出 scene_description_hint、carry_over_text 时，必须清理装饰噪声：连续分隔符（如 =====）、多余注释边界符（/*、*/）、无业务语义的 Markdown 装饰字符。
            - 保留业务含义，不保留排版噪声；最终描述必须为可读中文业务句子。

            E. 时段与表路由（Applicable Period）：
            - 优先识别“历史表/旧系统/2014年之前/目前主表”等描述，填入 applicable_period。

            F. 全局背景归属（Global Background Assignment）：
            - 文档开头的大段业务背景（如业务流程、负责人、数据获取总述）优先归入第一个核心场景的 scene_description_hint 或 context。
            - 不要把明显可用的业务背景直接丢到 unresolved。

            [异常处理]
            - 只要发现口径文档存在歧义、缺漏或 OCR 乱码，必须在 quality.warnings 中添加告警，并在 risk_notes 中说明问题位置。
            - 无 SQL：doc_profile.has_sql=false，sql_segments=[]；仍输出 scene_candidates/field_hints/risk_notes。
            - 文档被截断：doc_profile.is_truncated=true，并在 quality.errors 写 context_truncated。
            - 多场景混杂且边界不清：在 unresolved 写明不确定边界来源。
            - 存在多版本口径冲突：写入 risk_notes，并把冲突说明保留在 carry_over_text。
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
            请将以下打好行号标签的原始文档 [RAW_DOC] 抽取为符合 [PREP_SCHEMA] 定义的 JSON 对象。

            执行前自检：
            1) 是否已将 SQL 片段 ID 关联到对应场景（scene_candidates[].sql_segment_ids）？
            2) 是否提取了隐蔽在注释和 CASE WHEN 中的码值字典（field_hints[].extracted_mappings）？
            3) SQL 是否保留原始字符与注释，且未包含行号标签？
            4) 若存在 Step 或时段分层，是否已输出多个 scene_candidates（至少 2 个）？

            [PREP_SCHEMA]
            {{DYNAMIC_JSON_SCHEMA}}

            [CONTEXT]
            {
              "source_type": "{{SOURCE_TYPE}}",
              "lang": "zh",
              "target": "caliber_import_v2"
            }

            [RAW_DOC]
            {{RAW_DOC}}

            请直接输出符合 Schema 的 JSON 结果，以 "{" 开始：
            """;

    private LlmPromptDefaults() {
    }
}
