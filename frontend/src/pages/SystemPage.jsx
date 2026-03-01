import { useEffect, useMemo, useState } from "react";
import { RefreshCw, WifiOff } from "lucide-react";
import { apiRequest, apiRequestWithMeta } from "../api/client";
import { UiButton, UiEmptyState } from "../components/ui";
import { useAuthStore } from "../store/authStore";

const DEFAULT_PREPROCESS_SYSTEM_PROMPT = `你是“数据直通车”口径导入链路中的专业预处理引擎。
你的输出会被后端系统严格反序列化并转换为 CALIBER_IMPORT_V2 标准资产，因此必须绝对遵守结构与取值约束。

[核心铁律]
1) 绝对的格式纯净：只输出 JSON 字符串。绝对禁止包裹 \`\`\`json 和 \`\`\` 标签，绝对禁止输出任何解释性前言或后语。你的输出必须以 "{" 开头，以 "}" 结尾。
2) 绝对的 SQL 保真：sql_segments[*].sql_raw 必须 100% 复制原始业务文档中的 SQL 字符串。绝对禁止格式化缩进、绝对禁止修复语法错误、绝对禁止删除 SQL 注释（-- 或 /* */）。
3) 绝对不编造：无法确认的信息（如适用时段、业务含义）留空或写入 unresolved，宁缺毋造。
4) 宁愿冗余不愿丢失：所有无法被解析到结构化字段中的有价值文本（如大段业务背景、特殊流程说明），必须全部原样写入 carry_over_text。

[抽取策略]
A. 场景与 SQL 关联提取（Scene & SQL Binding）：
- 按业务意图或标题切换拆分 scene_candidates。
- 当文档出现 Step（如 Step 1/2/3）或明确时间分段（如 2009-2012、2014-至今）时，必须拆分为多个 scene_candidates，不得收敛为单场景。
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
- 存在多版本口径冲突：写入 risk_notes，并把冲突说明保留在 carry_over_text。`;

const DEFAULT_PREP_SCHEMA_JSON = `{
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
}`;

const DEFAULT_PREPROCESS_USER_PROMPT_TEMPLATE = `请将以下打好行号标签的原始文档 [RAW_DOC] 抽取为符合 [PREP_SCHEMA] 定义的 JSON 对象。

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

请直接输出符合 Schema 的 JSON 结果，以 "{" 开始：`;

const PREPROCESS_SOURCE_TYPE_OPTIONS = [
  { value: "PASTE_MD", label: "粘贴文档" },
  { value: "FILE_MD", label: "Markdown 文件" },
  { value: "FILE_TXT", label: "文本文件" },
  { value: "FILE_SQL", label: "SQL 文件" },
  { value: "IMAGE_OCR_TEXT", label: "OCR 文本" },
];

const DEFAULT_TEST_RAW_TEXT = "# 测试文档\n```sql\nselect 1;\n```";
const REQUIRED_PROMPT_CORE_TOKENS = ["{{RAW_DOC}}", "{{SOURCE_TYPE}}"];
const SUPPORTED_SCHEMA_TOKENS = ["{{PREP_SCHEMA}}", "{{DYNAMIC_JSON_SCHEMA}}"];

function defaultPrompts() {
  return {
    preprocessSystemPrompt: DEFAULT_PREPROCESS_SYSTEM_PROMPT,
    preprocessUserPromptTemplate: DEFAULT_PREPROCESS_USER_PROMPT_TEMPLATE,
    prepSchemaJson: DEFAULT_PREP_SCHEMA_JSON,
    promptFingerprint: "",
    schemaValid: true,
    schemaValidationMessage: "",
    templateHasRequiredTokens: true,
    templateMissingTokens: [],
    updatedBy: "",
    updatedAt: "",
  };
}

const USER_GUIDE_SECTIONS = [
  { id: "01", title: "系统定位与愿景", summary: "理解系统定位与目标边界", url: "/user-manual/01-system-positioning.md" },
  { id: "02", title: "核心概念与设计理念", summary: "统一关键术语与资产抽象", url: "/user-manual/02-core-concepts.md" },
  { id: "03", title: "角色与权限边界", summary: "明确角色职责和操作权限", url: "/user-manual/03-role-boundary.md" },
  { id: "04", title: "主要功能上手指南", summary: "从导入到发布的操作路径", url: "/user-manual/04-getting-started.md" },
  { id: "05", title: "质量门禁与发布说明", summary: "发布前门禁与质量要求", url: "/user-manual/05-quality-gate.md" },
  { id: "06", title: "常见问题与最佳实践", summary: "高频问题与推荐做法", url: "/user-manual/06-faq-best-practices.md" },
];

function resolveTestTone(testResult) {
  if (!testResult) {
    return "pending";
  }
  if (testResult.llmEffective) {
    return "ok";
  }
  if (testResult.fallbackUsed) {
    return "warn";
  }
  return "neutral";
}

function formatConfigSource(source) {
  if (source === "PERSISTED") {
    return "已保存配置";
  }
  if (source === "DEFAULT_PROPERTIES") {
    return "系统默认配置";
  }
  return "-";
}

function evaluatePromptDraft(prompt) {
  const template = `${prompt?.preprocessUserPromptTemplate || ""}`;
  const schemaText = `${prompt?.prepSchemaJson || ""}`;
  const missingTokens = REQUIRED_PROMPT_CORE_TOKENS.filter((token) => !template.includes(token));
  const hasSchemaToken = SUPPORTED_SCHEMA_TOKENS.some((token) => template.includes(token));
  if (!hasSchemaToken) {
    missingTokens.push("{{PREP_SCHEMA}}|{{DYNAMIC_JSON_SCHEMA}}");
  }
  let schemaValid = true;
  let schemaValidationMessage = "Schema JSON 可解析";
  try {
    JSON.parse(schemaText);
  } catch (error) {
    schemaValid = false;
    schemaValidationMessage = error?.message || "Schema JSON 解析失败";
  }
  const systemPromptLength = `${prompt?.preprocessSystemPrompt || ""}`.trim().length;
  return {
    missingTokens,
    schemaValid,
    schemaValidationMessage,
    systemPromptLength,
    templateHasRequiredTokens: missingTokens.length === 0,
    hasEnoughSystemPrompt: systemPromptLength >= 80,
  };
}

function normalizeGuideLoadError(error) {
  const message = `${error?.message || ""}`.trim();
  if (!message || /failed to fetch|networkerror|load failed|timeout/i.test(message)) {
    return "网络似乎走神了，无法获取内容";
  }
  return "网络似乎走神了，无法获取内容";
}

function renderInlineMarkdown(text, keyPrefix) {
  const source = `${text || ""}`;
  if (!source) {
    return "";
  }
  const pattern = /(\*\*[^*]+\*\*|`[^`]+`)/g;
  const nodes = [];
  let cursor = 0;
  let tokenIndex = 0;
  let matched = false;
  let match = pattern.exec(source);
  while (match) {
    matched = true;
    if (match.index > cursor) {
      nodes.push(<span key={`${keyPrefix}-text-${tokenIndex}`}>{source.slice(cursor, match.index)}</span>);
      tokenIndex += 1;
    }
    const token = match[0];
    if (token.startsWith("**") && token.endsWith("**")) {
      nodes.push(<strong key={`${keyPrefix}-bold-${tokenIndex}`}>{token.slice(2, -2)}</strong>);
    } else {
      nodes.push(<code key={`${keyPrefix}-code-${tokenIndex}`}>{token.slice(1, -1)}</code>);
    }
    tokenIndex += 1;
    cursor = match.index + token.length;
    match = pattern.exec(source);
  }
  if (cursor < source.length) {
    nodes.push(<span key={`${keyPrefix}-tail-${tokenIndex}`}>{source.slice(cursor)}</span>);
  }
  if (!matched) {
    return source;
  }
  return nodes;
}

function renderGuideMarkdown(markdownText) {
  const lines = `${markdownText || ""}`.replace(/\r\n?/g, "\n").split("\n");
  const blocks = [];
  let lineIndex = 0;
  let blockIndex = 0;

  while (lineIndex < lines.length) {
    const current = lines[lineIndex].trimEnd();
    const trimmed = current.trim();
    if (!trimmed) {
      lineIndex += 1;
      continue;
    }

    if (trimmed === "---") {
      blocks.push(<hr key={`guide-hr-${blockIndex}`} />);
      blockIndex += 1;
      lineIndex += 1;
      continue;
    }

    if (trimmed.startsWith("```")) {
      lineIndex += 1;
      const codeLines = [];
      while (lineIndex < lines.length && !lines[lineIndex].trim().startsWith("```")) {
        codeLines.push(lines[lineIndex]);
        lineIndex += 1;
      }
      if (lineIndex < lines.length) {
        lineIndex += 1;
      }
      blocks.push(
        <pre key={`guide-code-${blockIndex}`} className="guide-code-block">
          <code>{codeLines.join("\n")}</code>
        </pre>,
      );
      blockIndex += 1;
      continue;
    }

    const heading = trimmed.match(/^(#{1,3})\s+(.+)$/);
    if (heading) {
      const level = heading[1].length;
      const headingText = heading[2].trim();
      if (level === 1) {
        blocks.push(<h1 key={`guide-h1-${blockIndex}`}>{renderInlineMarkdown(headingText, `guide-h1-${blockIndex}`)}</h1>);
      } else if (level === 2) {
        blocks.push(<h2 key={`guide-h2-${blockIndex}`}>{renderInlineMarkdown(headingText, `guide-h2-${blockIndex}`)}</h2>);
      } else {
        blocks.push(<h3 key={`guide-h3-${blockIndex}`}>{renderInlineMarkdown(headingText, `guide-h3-${blockIndex}`)}</h3>);
      }
      blockIndex += 1;
      lineIndex += 1;
      continue;
    }

    if (/^[-*]\s+/.test(trimmed)) {
      const items = [];
      while (lineIndex < lines.length) {
        const text = lines[lineIndex].trim();
        if (!/^[-*]\s+/.test(text)) {
          break;
        }
        items.push(text.replace(/^[-*]\s+/, "").trim());
        lineIndex += 1;
      }
      blocks.push(
        <ul key={`guide-ul-${blockIndex}`}>
          {items.map((item, index) => (
            <li key={`guide-ul-${blockIndex}-${index}`}>
              {renderInlineMarkdown(item, `guide-ul-${blockIndex}-${index}`)}
            </li>
          ))}
        </ul>,
      );
      blockIndex += 1;
      continue;
    }

    if (/^\d+\.\s+/.test(trimmed)) {
      const items = [];
      while (lineIndex < lines.length) {
        const text = lines[lineIndex].trim();
        if (!/^\d+\.\s+/.test(text)) {
          break;
        }
        items.push(text.replace(/^\d+\.\s+/, "").trim());
        lineIndex += 1;
      }
      blocks.push(
        <ol key={`guide-ol-${blockIndex}`}>
          {items.map((item, index) => (
            <li key={`guide-ol-${blockIndex}-${index}`}>
              {renderInlineMarkdown(item, `guide-ol-${blockIndex}-${index}`)}
            </li>
          ))}
        </ol>,
      );
      blockIndex += 1;
      continue;
    }

    const paragraphLines = [trimmed];
    lineIndex += 1;
    while (lineIndex < lines.length) {
      const nextTrimmed = lines[lineIndex].trim();
      if (!nextTrimmed
        || nextTrimmed === "---"
        || nextTrimmed.startsWith("```")
        || /^#{1,3}\s+/.test(nextTrimmed)
        || /^[-*]\s+/.test(nextTrimmed)
        || /^\d+\.\s+/.test(nextTrimmed)) {
        break;
      }
      paragraphLines.push(nextTrimmed);
      lineIndex += 1;
    }
    blocks.push(
      <p key={`guide-p-${blockIndex}`}>
        {renderInlineMarkdown(paragraphLines.join(" "), `guide-p-${blockIndex}`)}
      </p>,
    );
    blockIndex += 1;
  }

  if (blocks.length === 0) {
    return [<p key="guide-empty">当前章节暂无内容。</p>];
  }
  return blocks;
}

export function SystemPage({ view = "llm" }) {
  const role = useAuthStore((state) => state.role);
  const isAdmin = useMemo(() => `${role || ""}`.toLowerCase() === "admin", [role]);

  const [config, setConfig] = useState({
    enabled: true,
    endpoint: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
    model: "qwen3-max",
    timeoutSeconds: 35,
    temperature: 0,
    maxTokens: 4096,
    enableThinking: false,
    fallbackToRule: true,
    apiKey: "",
    clearApiKey: false,
  });
  const [modelRows, setModelRows] = useState([]);
  const [llmMeta, setLlmMeta] = useState("");
  const [apiKeySummary, setApiKeySummary] = useState("未保存密钥");
  const [configSnapshot, setConfigSnapshot] = useState({
    configSource: "-",
    endpointHost: "-",
    fallbackStrategy: "-",
    updatedBy: "-",
    updatedAt: "-",
  });
  const [testResult, setTestResult] = useState(null);
  const [testRaw, setTestRaw] = useState("{}");
  const [testDraft, setTestDraft] = useState({
    rawText: DEFAULT_TEST_RAW_TEXT,
    sourceType: "PASTE_MD",
  });
  const [testHistory, setTestHistory] = useState([]);
  const [prompt, setPrompt] = useState(defaultPrompts());
  const [promptMeta, setPromptMeta] = useState("");
  const [promptPreviewDraft, setPromptPreviewDraft] = useState({
    rawText: DEFAULT_TEST_RAW_TEXT,
    sourceType: "PASTE_MD",
  });
  const [promptPreview, setPromptPreview] = useState(null);
  const [promptPreviewMeta, setPromptPreviewMeta] = useState("");
  const [activeGuideId, setActiveGuideId] = useState(USER_GUIDE_SECTIONS[0]?.id || "");
  const [guideMarkdown, setGuideMarkdown] = useState("");
  const [guideLoading, setGuideLoading] = useState(false);
  const [guideError, setGuideError] = useState("");
  const [guideReloadTick, setGuideReloadTick] = useState(0);

  const readonly = useMemo(() => !isAdmin, [isAdmin]);
  const testTone = useMemo(() => resolveTestTone(testResult), [testResult]);
  const activeGuideIndex = useMemo(
    () => USER_GUIDE_SECTIONS.findIndex((item) => item.id === activeGuideId),
    [activeGuideId],
  );
  const activeGuide = activeGuideIndex >= 0 ? USER_GUIDE_SECTIONS[activeGuideIndex] : USER_GUIDE_SECTIONS[0];
  const promptDraftHealth = useMemo(() => evaluatePromptDraft(prompt), [prompt]);

  useEffect(() => {
    if (view === "llm") {
      loadConfig();
    }
    if (view === "prompts") {
      loadPrompts();
    }
  }, [view]);

  useEffect(() => {
    if (view !== "guide" || !activeGuide) {
      return undefined;
    }
    let cancelled = false;
    async function loadGuide() {
      setGuideLoading(true);
      setGuideError("");
      try {
        const response = await fetch(activeGuide.url, { cache: "no-store" });
        if (!response.ok) {
          throw new Error(`手册加载失败（${response.status}）`);
        }
        const text = await response.text();
        if (cancelled) {
          return;
        }
        setGuideMarkdown(text);
      } catch (error) {
        if (cancelled) {
          return;
        }
        setGuideMarkdown("");
        setGuideError(normalizeGuideLoadError(error));
      } finally {
        if (!cancelled) {
          setGuideLoading(false);
        }
      }
    }
    loadGuide();
    return () => {
      cancelled = true;
    };
  }, [view, activeGuide, guideReloadTick]);

  function switchGuide(offset) {
    if (activeGuideIndex < 0) {
      return;
    }
    const nextIndex = activeGuideIndex + offset;
    if (nextIndex < 0 || nextIndex >= USER_GUIDE_SECTIONS.length) {
      return;
    }
    setActiveGuideId(USER_GUIDE_SECTIONS[nextIndex].id);
  }

  async function loadConfig() {
    try {
      const data = await apiRequest("/system/llm-preprocess-config");
      setConfig({
        enabled: data.enabled !== false,
        endpoint: data.endpoint || "",
        model: data.model || "",
        timeoutSeconds: Number(data.timeoutSeconds ?? 35),
        temperature: Number(data.temperature ?? 0),
        maxTokens: Number(data.maxTokens ?? 4096),
        enableThinking: data.enableThinking === true,
        fallbackToRule: data.fallbackToRule !== false,
        apiKey: "",
        clearApiKey: false,
      });
      setApiKeySummary(data.hasApiKey ? `已保存密钥：${data.maskedApiKey || "***"}` : "未保存密钥");
      setConfigSnapshot({
        configSource: formatConfigSource(data.configSource),
        endpointHost: data.endpointHost || "-",
        fallbackStrategy: data.fallbackStrategy || "-",
        updatedBy: data.updatedBy || "-",
        updatedAt: data.updatedAt || "-",
      });
      setLlmMeta(`最后更新：${data.updatedAt || "-"} · 操作人：${data.updatedBy || "-"}`);
    } catch (error) {
      setLlmMeta(error.message || "加载失败");
    }
  }

  async function saveConfig() {
    try {
      const payload = {
        enabled: !!config.enabled,
        endpoint: `${config.endpoint || ""}`.trim(),
        model: `${config.model || ""}`.trim(),
        timeoutSeconds: Number(config.timeoutSeconds),
        temperature: Number(config.temperature),
        maxTokens: Number(config.maxTokens),
        enableThinking: !!config.enableThinking,
        fallbackToRule: !!config.fallbackToRule,
        apiKey: config.apiKey || "",
        clearApiKey: !!config.clearApiKey,
      };
      const data = await apiRequest("/system/llm-preprocess-config", {
        method: "PUT",
        body: payload,
      });
      setConfig((prev) => ({ ...prev, apiKey: "", clearApiKey: false }));
      setApiKeySummary(data.hasApiKey ? `已保存密钥：${data.maskedApiKey || "***"}` : "未保存密钥");
      setConfigSnapshot({
        configSource: formatConfigSource(data.configSource),
        endpointHost: data.endpointHost || "-",
        fallbackStrategy: data.fallbackStrategy || "-",
        updatedBy: data.updatedBy || "-",
        updatedAt: data.updatedAt || "-",
      });
      setLlmMeta(`保存成功：${data.updatedAt || "-"}`);
    } catch (error) {
      setLlmMeta(error.message || "保存失败");
    }
  }

  async function testConfig() {
    try {
      const { data, meta } = await apiRequestWithMeta("/system/llm-preprocess-config/test", {
        method: "POST",
        body: {
          rawText: testDraft.rawText,
          sourceType: testDraft.sourceType,
        },
      });
      const testedAt = new Date().toLocaleString("zh-CN", { hour12: false });
      const result = {
        ...data,
        requestId: meta.requestId || "",
        testedAt,
      };
      setTestResult(result);
      setTestRaw(JSON.stringify(result, null, 2));
      setTestHistory((prev) => [
        {
          testedAt,
          statusLabel: result.statusLabel || "-",
          mode: result.mode || "-",
          latencyMs: Number(result.latencyMs || 0),
          sceneCount: Number(result.sceneCount || 0),
          requestId: result.requestId || "-",
        },
        ...prev,
      ].slice(0, 5));
    } catch (error) {
      const fallback = {
        success: false,
        statusLabel: "测试失败",
        statusReason: error.message || "测试失败",
        message: error.message || "测试失败",
        requestId: error.requestId || "",
        testedAt: new Date().toLocaleString("zh-CN", { hour12: false }),
      };
      setTestResult(fallback);
      setTestRaw(JSON.stringify(fallback, null, 2));
      setTestHistory((prev) => [
        {
          testedAt: fallback.testedAt,
          statusLabel: fallback.statusLabel,
          mode: "-",
          latencyMs: 0,
          sceneCount: 0,
          requestId: fallback.requestId || "-",
        },
        ...prev,
      ].slice(0, 5));
    }
  }

  async function fetchModels() {
    try {
      const result = await apiRequest("/system/llm-preprocess-config/models", {
        method: "POST",
        body: {
          endpoint: config.endpoint,
          apiKey: config.apiKey,
          timeoutSeconds: Number(config.timeoutSeconds),
        },
      });
      setModelRows(Array.isArray(result?.models) ? result.models : []);
      if (result?.selectedModel) {
        setConfig((prev) => ({ ...prev, model: result.selectedModel }));
      }
      setLlmMeta(result?.message || "模型列表获取完成");
    } catch (error) {
      setLlmMeta(error.message || "模型列表获取失败");
    }
  }

  async function loadPrompts() {
    try {
      const data = await apiRequest("/system/llm-preprocess-config/prompts");
      const defaults = defaultPrompts();
      setPrompt({
        preprocessSystemPrompt: data.preprocessSystemPrompt || defaults.preprocessSystemPrompt,
        preprocessUserPromptTemplate: data.preprocessUserPromptTemplate || defaults.preprocessUserPromptTemplate,
        prepSchemaJson: data.prepSchemaJson || defaults.prepSchemaJson,
        promptFingerprint: data.promptFingerprint || "",
        schemaValid: data.schemaValid !== false,
        schemaValidationMessage: data.schemaValidationMessage || "",
        templateHasRequiredTokens: data.templateHasRequiredTokens !== false,
        templateMissingTokens: Array.isArray(data.templateMissingTokens) ? data.templateMissingTokens : [],
        updatedBy: data.updatedBy || "",
        updatedAt: data.updatedAt || "",
      });
      setPromptMeta(`最后更新：${data.updatedAt || "-"} · 操作人：${data.updatedBy || "-"}`);
      setPromptPreview(null);
      setPromptPreviewMeta("");
    } catch (error) {
      setPrompt(defaultPrompts());
      setPromptMeta(error.message || "加载提示词失败，已回退系统默认模板");
    }
  }

  async function savePrompt() {
    try {
      const data = await apiRequest("/system/llm-preprocess-config/prompts", {
        method: "PUT",
        body: {
          preprocessSystemPrompt: prompt.preprocessSystemPrompt,
          preprocessUserPromptTemplate: prompt.preprocessUserPromptTemplate,
          prepSchemaJson: prompt.prepSchemaJson,
        },
      });
      setPrompt((prev) => ({
        ...prev,
        promptFingerprint: data.promptFingerprint || prev.promptFingerprint,
        schemaValid: data.schemaValid !== false,
        schemaValidationMessage: data.schemaValidationMessage || prev.schemaValidationMessage,
        templateHasRequiredTokens: data.templateHasRequiredTokens !== false,
        templateMissingTokens: Array.isArray(data.templateMissingTokens) ? data.templateMissingTokens : prev.templateMissingTokens,
        updatedBy: data.updatedBy || prev.updatedBy,
        updatedAt: data.updatedAt || prev.updatedAt,
      }));
      setPromptMeta("提示词模板已保存，并立即生效于后端运行时抽取");
    } catch (error) {
      setPromptMeta(error.message || "提示词保存失败");
    }
  }

  async function resetPrompt() {
    try {
      const data = await apiRequest("/system/llm-preprocess-config/prompts/reset", {
        method: "POST",
        body: {},
      });
      setPrompt({
        preprocessSystemPrompt: data.preprocessSystemPrompt || "",
        preprocessUserPromptTemplate: data.preprocessUserPromptTemplate || "",
        prepSchemaJson: data.prepSchemaJson || "",
        promptFingerprint: data.promptFingerprint || "",
        schemaValid: data.schemaValid !== false,
        schemaValidationMessage: data.schemaValidationMessage || "",
        templateHasRequiredTokens: data.templateHasRequiredTokens !== false,
        templateMissingTokens: Array.isArray(data.templateMissingTokens) ? data.templateMissingTokens : [],
        updatedBy: data.updatedBy || "",
        updatedAt: data.updatedAt || "",
      });
      setPromptMeta("提示词模板已恢复默认，并立即生效于后端运行时抽取");
      setPromptPreview(null);
      setPromptPreviewMeta("");
    } catch (error) {
      setPromptMeta(error.message || "恢复默认失败");
    }
  }

  async function previewPromptDraft() {
    try {
      const { data, meta } = await apiRequestWithMeta("/system/llm-preprocess-config/prompts/preview", {
        method: "POST",
        body: {
          rawText: promptPreviewDraft.rawText,
          sourceType: promptPreviewDraft.sourceType,
          preprocessSystemPrompt: prompt.preprocessSystemPrompt,
          preprocessUserPromptTemplate: prompt.preprocessUserPromptTemplate,
          prepSchemaJson: prompt.prepSchemaJson,
        },
      });
      setPromptPreview(data);
      setPromptPreviewMeta(`预览完成 · RequestId: ${meta.requestId || "-"}`);
    } catch (error) {
      setPromptPreview(null);
      setPromptPreviewMeta(error.message || "预览失败");
    }
  }

  if (view === "guide") {
    return (
      <section className="panel">
        <div className="panel-head">
          <h2>系统手册</h2>
          <p>面向业务与治理用户的在线阅读版，支持章节切换与连续阅读。</p>
        </div>
        <div className="panel-block">
          <h3>系统定位</h3>
          <p className="subtle-note">当前阶段以口径治理和数据地图为核心能力。</p>
          <ul className="plain-list">
            <li>业务场景是最小治理单元，支持多取数方案表达历史与口径差异。</li>
            <li>口径治理主流程为导入、修订、发布，强调“信息不丢失”。</li>
            <li>数据地图用于消费与理解治理成果，支持关系浏览与路径分析。</li>
          </ul>
        </div>
        <div className="guide-reader">
          <aside className="guide-catalog" aria-label="手册目录">
            <h3>章节目录</h3>
            <div className="guide-catalog-list">
              {USER_GUIDE_SECTIONS.map((item) => (
                <button
                  key={item.id}
                  className={`guide-catalog-item ${item.id === activeGuideId ? "is-active" : ""}`}
                  type="button"
                  onClick={() => setActiveGuideId(item.id)}
                >
                  <strong>{item.id}. {item.title}</strong>
                  <span>{item.summary}</span>
                </button>
              ))}
            </div>
          </aside>
          <article className="guide-content panel-block">
            <div className="guide-content-head">
              <h3>{activeGuide ? `${activeGuide.id}. ${activeGuide.title}` : "用户手册"}</h3>
              <p className="subtle-note">系统内置手册 · 第 {activeGuideIndex + 1} / {USER_GUIDE_SECTIONS.length} 章</p>
            </div>
            {guideLoading ? <p className="meta" role="status" aria-live="polite">手册加载中…</p> : null}
            {!guideLoading && guideError ? (
              <UiEmptyState
                className="system-guide-empty"
                icon={<WifiOff size={20} strokeWidth={1.9} />}
                title={guideError}
                description="请检查网络后重试，系统会保留当前章节位置。"
                action={(
                  <UiButton
                    variant="ghost"
                    size="sm"
                    icon={<RefreshCw size={14} />}
                    onClick={() => setGuideReloadTick((value) => value + 1)}
                  >
                    刷新
                  </UiButton>
                )}
              />
            ) : null}
            {!guideLoading && !guideError ? (
              <div className="guide-markdown">
                {renderGuideMarkdown(guideMarkdown)}
              </div>
            ) : null}
            <div className="actions">
              <button
                className="btn btn-ghost"
                type="button"
                onClick={() => switchGuide(-1)}
                disabled={guideLoading || activeGuideIndex <= 0}
              >
                上一章
              </button>
              <button
                className="btn btn-primary"
                type="button"
                onClick={() => switchGuide(1)}
                disabled={guideLoading || activeGuideIndex < 0 || activeGuideIndex >= USER_GUIDE_SECTIONS.length - 1}
              >
                下一章
              </button>
            </div>
          </article>
        </div>
      </section>
    );
  }

  if (view === "prompts") {
    return (
      <section className="panel">
        <div className="panel-head">
          <h2>预处理提示词</h2>
          <p>维护后端运行时预处理提示词，保存后立即生效</p>
        </div>
        {readonly ? <p className="danger-text" role="status" aria-live="polite">当前为只读模式，仅系统管理员可修改配置。</p> : null}
        <div className="panel-block prompt-quality-panel">
          <h3>发布前质量检查</h3>
          <div className="prompt-check-list">
            <div className={`prompt-check-item ${promptDraftHealth.templateHasRequiredTokens ? "is-ok" : "is-warn"}`}>
              <strong>模板占位符完整性</strong>
              <span>
                {promptDraftHealth.templateHasRequiredTokens
                  ? "已包含 {{RAW_DOC}} / {{SOURCE_TYPE}}，并使用 {{DYNAMIC_JSON_SCHEMA}} 或 {{PREP_SCHEMA}}"
                  : `缺失占位符：${promptDraftHealth.missingTokens.join("、")}`}
              </span>
            </div>
            <div className={`prompt-check-item ${promptDraftHealth.schemaValid ? "is-ok" : "is-warn"}`}>
              <strong>Schema 可解析性</strong>
              <span>{promptDraftHealth.schemaValid ? "Schema JSON 结构可解析" : promptDraftHealth.schemaValidationMessage}</span>
            </div>
            <div className={`prompt-check-item ${promptDraftHealth.hasEnoughSystemPrompt ? "is-ok" : "is-warn"}`}>
              <strong>系统提示词长度</strong>
              <span>
                {promptDraftHealth.hasEnoughSystemPrompt
                  ? `当前 ${promptDraftHealth.systemPromptLength} 字，长度充足`
                  : `当前 ${promptDraftHealth.systemPromptLength} 字，建议补充到 80 字以上`}
              </span>
            </div>
          </div>
        </div>
        <label className="textarea-label" htmlFor="preprocessSystemPrompt">系统提示词</label>
        <textarea
          id="preprocessSystemPrompt"
          name="preprocessSystemPrompt"
          autoComplete="off"
          className="mini"
          value={prompt.preprocessSystemPrompt}
          onChange={(event) => setPrompt((prev) => ({ ...prev, preprocessSystemPrompt: event.target.value }))}
          disabled={readonly}
        />
        <label className="textarea-label" htmlFor="preprocessUserPromptTemplate">用户提示词模板</label>
        <textarea
          id="preprocessUserPromptTemplate"
          name="preprocessUserPromptTemplate"
          autoComplete="off"
          className="mini"
          value={prompt.preprocessUserPromptTemplate}
          onChange={(event) => setPrompt((prev) => ({ ...prev, preprocessUserPromptTemplate: event.target.value }))}
          disabled={readonly}
        />
        <p className="field-hint">模板占位符：{"{{DYNAMIC_JSON_SCHEMA}}"}（推荐）/{"{{PREP_SCHEMA}}"}（兼容）、{"{{RAW_DOC}}"}、{"{{SOURCE_TYPE}}"}</p>
        <label className="textarea-label" htmlFor="prepSchemaJson">预处理 Schema</label>
        <textarea
          id="prepSchemaJson"
          name="prepSchemaJson"
          autoComplete="off"
          className="code"
          value={prompt.prepSchemaJson}
          onChange={(event) => setPrompt((prev) => ({ ...prev, prepSchemaJson: event.target.value }))}
          disabled={readonly}
        />
        <p className="subtle-note">提示词指纹：{prompt.promptFingerprint || "-"}</p>
        <p className="subtle-note">后端校验：{prompt.schemaValid ? "Schema 通过" : (prompt.schemaValidationMessage || "Schema 未通过")} · 占位符：{prompt.templateHasRequiredTokens ? "完整" : "不完整"}</p>
        <div className="actions">
          <button className="btn btn-primary" type="button" onClick={savePrompt} disabled={readonly}>保存提示词</button>
          <button className="btn btn-ghost" type="button" onClick={resetPrompt} disabled={readonly}>恢复默认</button>
          <button className="btn btn-ghost" type="button" onClick={loadPrompts}>重新加载</button>
        </div>
        <p className="meta" role="status" aria-live="polite">{promptMeta}</p>

        <div className="panel-block prompt-preview-panel">
          <h3>渲染预览（不保存）</h3>
          <p className="subtle-note">基于当前编辑中的草稿进行渲染预览，不会覆盖线上配置。</p>
          <div className="row form-row">
            <label htmlFor="promptPreviewSourceType">预览来源类型</label>
            <select
              id="promptPreviewSourceType"
              name="promptPreviewSourceType"
              autoComplete="off"
              value={promptPreviewDraft.sourceType}
              onChange={(event) => setPromptPreviewDraft((prev) => ({ ...prev, sourceType: event.target.value }))}
            >
              {PREPROCESS_SOURCE_TYPE_OPTIONS.map((item) => (
                <option key={item.value} value={item.value}>{item.label}</option>
              ))}
            </select>
          </div>
          <label className="textarea-label" htmlFor="promptPreviewRawText">预览原文</label>
          <textarea
            id="promptPreviewRawText"
            name="promptPreviewRawText"
            autoComplete="off"
            className="mini"
            value={promptPreviewDraft.rawText}
            onChange={(event) => setPromptPreviewDraft((prev) => ({ ...prev, rawText: event.target.value }))}
          />
          <div className="actions">
            <button className="btn btn-secondary" type="button" onClick={previewPromptDraft}>渲染预览</button>
          </div>
          <p className="meta" role="status" aria-live="polite">{promptPreviewMeta}</p>
          {promptPreview ? (
            <>
              <div className="prompt-preview-summary">
                <span>来源类型：{promptPreview.normalizedSourceType || "-"}</span>
                <span>行数：{Number(promptPreview.lineCount || 0)}</span>
                <span>指纹：{promptPreview.promptFingerprint || "-"}</span>
              </div>
              {Array.isArray(promptPreview.warnings) && promptPreview.warnings.length > 0 ? (
                <p className="subtle-note">预览告警：{promptPreview.warnings.join("；")}</p>
              ) : null}
              <details className="debug-panel">
                <summary>查看渲染后的用户提示词</summary>
                <pre className="result-json">{promptPreview.userPrompt || "-"}</pre>
              </details>
              <details className="debug-panel">
                <summary>查看渲染后的系统提示词</summary>
                <pre className="result-json">{promptPreview.systemPrompt || "-"}</pre>
              </details>
            </>
          ) : null}
        </div>
      </section>
    );
  }

  return (
    <section className="panel system-settings">
      <div className="panel-head">
        <h2>大模型配置</h2>
        <p>维护 LLM 接入参数（非管理员为只读）</p>
      </div>
      {readonly ? <p className="danger-text" role="status" aria-live="polite">当前为只读模式，仅系统管理员可修改配置。</p> : null}
      <div className="kv-grid llm-overview-grid">
        <div className="kv-card">
          <span>配置来源</span>
          <strong>{configSnapshot.configSource}</strong>
        </div>
        <div className="kv-card">
          <span>回退链路</span>
          <strong>{configSnapshot.fallbackStrategy}</strong>
        </div>
        <div className="kv-card">
          <span>Endpoint 主机</span>
          <strong>{configSnapshot.endpointHost}</strong>
        </div>
        <div className="kv-card">
          <span>已保存密钥</span>
          <strong>{apiKeySummary.includes("已保存") ? "已配置" : "未配置"}</strong>
        </div>
      </div>
      <div className="settings-card">
        <div className="settings-row">
          <label htmlFor="llmEnabled" className="settings-label">启用大模型预处理</label>
          <label className="ios-switch" htmlFor="llmEnabled">
            <input
              id="llmEnabled"
              name="llmEnabled"
              autoComplete="off"
              type="checkbox"
              checked={!!config.enabled}
              onChange={(event) => setConfig((prev) => ({ ...prev, enabled: event.target.checked }))}
              disabled={readonly}
            />
            <span className="ios-switch-ui" aria-hidden="true" />
          </label>
        </div>

        <div className="settings-row">
          <label htmlFor="llmEnableThinking" className="settings-label">启用模型思考</label>
          <label className="ios-switch" htmlFor="llmEnableThinking">
            <input
              id="llmEnableThinking"
              name="llmEnableThinking"
              autoComplete="off"
              type="checkbox"
              checked={!!config.enableThinking}
              onChange={(event) => setConfig((prev) => ({ ...prev, enableThinking: event.target.checked }))}
              disabled={readonly}
            />
            <span className="ios-switch-ui" aria-hidden="true" />
          </label>
        </div>

        <div className="settings-row">
          <label htmlFor="llmFallbackToRule" className="settings-label">失败回退规则抽取</label>
          <label className="ios-switch" htmlFor="llmFallbackToRule">
            <input
              id="llmFallbackToRule"
              name="llmFallbackToRule"
              autoComplete="off"
              type="checkbox"
              checked={!!config.fallbackToRule}
              onChange={(event) => setConfig((prev) => ({ ...prev, fallbackToRule: event.target.checked }))}
              disabled={readonly}
            />
            <span className="ios-switch-ui" aria-hidden="true" />
          </label>
        </div>

        <div className="settings-row">
          <label htmlFor="llmEndpoint" className="settings-label">接口地址</label>
          <input
            id="llmEndpoint"
            name="llmEndpoint"
            autoComplete="off"
            value={config.endpoint || ""}
            onChange={(event) => setConfig((prev) => ({ ...prev, endpoint: event.target.value }))}
            disabled={readonly}
            placeholder="https://api.example.com/v1/chat/completions"
            className="settings-input"
          />
        </div>

        <div className="settings-row settings-row-multi">
          <label htmlFor="llmModel" className="settings-label">模型名称</label>
          <div className="settings-control-stack">
            <input
              id="llmModel"
              name="llmModel"
              autoComplete="off"
              value={config.model || ""}
              onChange={(event) => setConfig((prev) => ({ ...prev, model: event.target.value }))}
              disabled={readonly}
              className="settings-input"
            />
            <div className="llm-model-tools">
              <button className="btn btn-ghost" type="button" onClick={fetchModels} disabled={readonly}>获取模型列表</button>
              <select
                id="llmModelSelect"
                name="llmModelSelect"
                autoComplete="off"
                value={config.model || ""}
                onChange={(event) => setConfig((prev) => ({ ...prev, model: event.target.value }))}
                disabled={readonly}
                className="settings-input"
              >
                <option value="">从已获取列表选择模型</option>
                {modelRows.map((item) => <option key={item} value={item}>{item}</option>)}
              </select>
            </div>
          </div>
        </div>

        <div className="settings-row">
          <label htmlFor="llmTimeoutSeconds" className="settings-label">超时（秒）</label>
          <input
            id="llmTimeoutSeconds"
            name="llmTimeoutSeconds"
            autoComplete="off"
            type="number"
            value={config.timeoutSeconds ?? 35}
            onChange={(event) => setConfig((prev) => ({ ...prev, timeoutSeconds: Number(event.target.value) }))}
            disabled={readonly}
            className="settings-input"
          />
        </div>

        <div className="settings-row">
          <label htmlFor="llmTemperature" className="settings-label">温度（0-1）</label>
          <input
            id="llmTemperature"
            name="llmTemperature"
            autoComplete="off"
            type="number"
            min="0"
            max="1"
            step="0.1"
            value={config.temperature ?? 0}
            onChange={(event) => setConfig((prev) => ({ ...prev, temperature: Number(event.target.value) }))}
            disabled={readonly}
            className="settings-input"
          />
        </div>

        <div className="settings-row">
          <label htmlFor="llmMaxTokens" className="settings-label">最大令牌数</label>
          <input
            id="llmMaxTokens"
            name="llmMaxTokens"
            autoComplete="off"
            type="number"
            min="128"
            max="32768"
            value={config.maxTokens ?? 4096}
            onChange={(event) => setConfig((prev) => ({ ...prev, maxTokens: Number(event.target.value) }))}
            disabled={readonly}
            className="settings-input"
          />
        </div>

        <div className="settings-row">
          <label htmlFor="llmApiKey" className="settings-label">API 密钥</label>
          <input
            id="llmApiKey"
            name="llmApiKey"
            autoComplete="off"
            type="password"
            value={config.apiKey || ""}
            onChange={(event) => setConfig((prev) => ({ ...prev, apiKey: event.target.value }))}
            placeholder="留空表示不修改"
            disabled={readonly}
            className="settings-input"
          />
        </div>

        <div className="settings-row">
          <span className="settings-label">已保存密钥</span>
          <span className="settings-value">{apiKeySummary}</span>
        </div>

        <div className="settings-row">
          <label htmlFor="llmClearApiKey" className="settings-label">清空已保存密钥</label>
          <label className="ios-switch" htmlFor="llmClearApiKey">
            <input
              id="llmClearApiKey"
              name="llmClearApiKey"
              autoComplete="off"
              type="checkbox"
              checked={!!config.clearApiKey}
              onChange={(event) => setConfig((prev) => ({ ...prev, clearApiKey: event.target.checked }))}
              disabled={readonly}
            />
            <span className="ios-switch-ui" aria-hidden="true" />
          </label>
        </div>
      </div>

      <div className="panel-block llm-test-inputs">
        <h3>连通性测试样本</h3>
        <p className="subtle-note">用于验证实际链路是否走 LLM 或回退规则，本操作不创建草稿。</p>
        <div className="row form-row">
          <label htmlFor="llmTestSourceType">来源类型</label>
          <select
            id="llmTestSourceType"
            name="llmTestSourceType"
            autoComplete="off"
            value={testDraft.sourceType}
            onChange={(event) => setTestDraft((prev) => ({ ...prev, sourceType: event.target.value }))}
          >
            {PREPROCESS_SOURCE_TYPE_OPTIONS.map((item) => (
              <option key={item.value} value={item.value}>{item.label}</option>
            ))}
          </select>
        </div>
        <label className="textarea-label" htmlFor="llmTestRawText">测试原文</label>
        <textarea
          id="llmTestRawText"
          name="llmTestRawText"
          autoComplete="off"
          className="mini"
          value={testDraft.rawText}
          onChange={(event) => setTestDraft((prev) => ({ ...prev, rawText: event.target.value }))}
        />
      </div>
      <div className="actions">
        <button className="btn btn-ghost" type="button" onClick={loadConfig}>加载配置</button>
        <button className="btn btn-primary" type="button" onClick={saveConfig} disabled={readonly}>保存配置</button>
        <button className="btn btn-secondary" type="button" onClick={testConfig}>测试抽取</button>
      </div>
      {testResult ? (
        <div className={`llm-test-status llm-test-status-${testTone}`}>
          <p className="llm-test-status-title">{testResult.statusLabel || "测试结果"}</p>
          <p className="llm-test-status-text">{testResult.statusReason || testResult.message || "-"}</p>
          <div className="llm-test-status-meta">
            <span>模式：{testResult.mode || "-"}</span>
            <span>场景数：{Number(testResult.sceneCount || 0)}</span>
            <span>取数方案数：{Number(testResult.sqlCount || 0)}</span>
            <span>耗时：{Number(testResult.latencyMs || 0)}ms</span>
            <span>请求号：{testResult.requestId || "-"}</span>
          </div>
          <p className="subtle-note">测试时间：{testResult.testedAt || "-"}</p>
          <p className="subtle-note">提示词指纹：{testResult.promptFingerprint || "-"}</p>
          {Array.isArray(testResult.warnings) && testResult.warnings.length > 0 ? (
            <p className="subtle-note">链路告警：{testResult.warnings.join("；")}</p>
          ) : null}
          {!testResult.llmEffective ? (
            <p className="subtle-note">当前不是 LLM 生效态，请优先检查启用开关、接口地址、模型和密钥配置。</p>
          ) : null}
        </div>
      ) : null}
      {testHistory.length > 0 ? (
        <div className="panel-block llm-test-history">
          <h3>最近测试记录</h3>
          <ul className="plain-list">
            {testHistory.map((item, index) => (
              <li key={`${item.testedAt}-${index}`}>
                {item.testedAt} · {item.statusLabel} · 模式 {item.mode} · 场景 {item.sceneCount} · {item.latencyMs}ms · 请求号 {item.requestId}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
      <p className="meta" role="status" aria-live="polite">{llmMeta}</p>
      <details className="debug-panel">
        <summary>查看测试返回详情</summary>
        <pre className="result-json">{testRaw}</pre>
      </details>
    </section>
  );
}
