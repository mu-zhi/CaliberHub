import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { apiRequest, apiSseRequest, parseJsonText } from "../api/client";
import { useAuthStore } from "../store/authStore";
import { useNavigate } from "react-router-dom";
import { CheckCircle2, Circle } from "lucide-react";
import { AccordionStepCard } from "../components/AccordionStepCard";
import { AutoGrowTextarea } from "../components/AutoGrowTextarea";
import { UiTextarea } from "../components/ui";
import { format as formatSql } from "sql-formatter";
import {
  buildStep1Summary,
  buildStep2Summary,
  buildStep3Summary,
  resolveAccordionStepState,
  toConfidenceLevelZh,
} from "./knowledge-import-utils";

function safeStringify(value) {
  try {
    return JSON.stringify(value, null, 2);
  } catch (_) {
    return "{}";
  }
}

function formatElapsedMs(value) {
  const ms = Math.max(0, Number(value || 0));
  if (ms >= 60_000) {
    return `${(ms / 1000).toFixed(1)}s`;
  }
  if (ms >= 1_000) {
    return `${(ms / 1000).toFixed(ms >= 10_000 ? 0 : 1)}s`;
  }
  return `${ms}ms`;
}

function formatDateTimeLabel(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return `${value}`;
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

function nowLocalInputValue() {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 16);
}

function detectSourceTypeByFileName(fileName) {
  const lowerName = `${fileName || ""}`.toLowerCase();
  if (lowerName.endsWith(".sql")) {
    return "FILE_SQL";
  }
  if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
    return "FILE_MD";
  }
  return "FILE_TXT";
}

const IMPORT_BEST_PRACTICE_SAMPLE = `### 场景 1：根据公司户口号查询代发协议号
- 场景描述：先定位代发协议号，再关联后续批次或明细查询。
- 口径提供人：张三/80000001
- 结果字段：合作方协议号、公司户口号、客户号、协议状态
- 注意事项：历史系统与当前系统字段命名不同，跨系统联查前需先确认客户主键。
- SQL 语句
-- Step 1：根据公司户口号查询代发协议号
SELECT PROTOCOL_NBR, CARD_NBR, CLIENT_NBR
FROM NDS_VHIS.NLJ54_AGF_PROTOCOL_PAY_CARD_T
WHERE CARD_NBR IN ('\${CORP_CARD_NBR}');

### 场景 2：根据代发协议号查询代发明细
- 场景描述：按协议号查询代发明细，支持当前主表与历史补数表双通道。
- 口径提供人：李四/80000002
- 结果字段：代发批次号、收款户口号、交易金额、代发状态代码
- 注意事项：2014 年前建议优先使用历史表；跨年查询需拆分时间段执行。
- SQL 语句
-- Step 1：查询 2014 年至今主表明细
-- 码值：AGN_STS_CD=01(成功),02(失败),03(处理中)
SELECT AGN_BCH_SEQ, EAC_NBR, TRX_AMT, AGN_STS_CD, TRX_DT
FROM PDM_VHIS.T05_AGN_DTL
WHERE TRX_DT >= DATE '2014-01-01'
  AND MCH_AGR_NBR = '\${PROTOCOL_NBR}';

-- Step 2：按需补查历史明细表
SELECT EPXTRXDAT, EPXEACNBR, EPXTRXAMT, EPXTRXSTS
FROM LGC_EAM.ETL_FE_EPHISTRXP
WHERE EPXCNVNBR = '\${PROTOCOL_NBR}';`;

const IMPORT_GHOST_PLACEHOLDER = `### 场景标题：请填写业务场景名称
- 场景描述：请描述该场景的业务目标与查询口径。
- 口径提供人：姓名/一事通
- 结果字段：字段1、字段2、字段3
- 注意事项：请填写取数边界、历史数据差异或风险提示。
- SQL 语句
-- Step 1：请描述本步骤的查询目标
SELECT ...
FROM ...
WHERE ...;

-- Step 2：如有历史表或补充查询，请继续追加`;

const IMPORT_STANDARD_HINT_GROUPS = [
  {
    key: "required",
    title: "必备段落",
    items: [
      {
        key: "title",
        label: "场景标题",
        hint: "例如：### 场景标题：根据公司户口号查询代发协议号",
        pattern: /(场景标题|场景\s*\d+[:：]|###\s*场景)/i,
      },
      {
        key: "description",
        label: "场景描述",
        hint: "例如：- 场景描述：先定位协议号，再查询批次或明细",
        pattern: /(场景描述[:：]|业务场景[:：])/i,
      },
      {
        key: "contributors",
        label: "口径提供人",
        hint: "例如：- 口径提供人：张三/80000001",
        pattern: /(口径提供人[:：]|负责人[:：])/i,
      },
      {
        key: "outputs",
        label: "结果字段",
        hint: "例如：- 结果字段：代发批次号、交易金额、代发状态",
        pattern: /(结果字段[:：]|输出字段[:：]|明细字段[:：])/i,
      },
      {
        key: "caveats",
        label: "注意事项",
        hint: "例如：- 注意事项：跨年查询需拆分时间段",
        pattern: /(注意事项[:：]|限制字段[:：]|边界说明[:：])/i,
      },
      {
        key: "sql",
        label: "SQL 语句",
        hint: "例如：- SQL 语句 + 至少一段 SELECT",
        pattern: /(SQL\s*语句|SELECT\s+)/i,
      },
    ],
  },
  {
    key: "enhanced",
    title: "建议增强",
    items: [
      {
        key: "step_comment",
        label: "Step 注释",
        hint: "例如：-- Step 1：根据公司户口号查询代发协议号",
        pattern: /(--\s*(Step|步骤)\s*\d+\s*[:：])/i,
      },
      {
        key: "mapping",
        label: "码值映射",
        hint: "例如：-- 码值：01=成功，02=失败",
        pattern: /([A-Za-z0-9_]{1,}\s*[:：]?\s*[0-9A-Za-z]{1,}\s*=\s*[^,，\n]+)/,
      },
      {
        key: "period",
        label: "时段/边界",
        hint: "例如：2014 年至今、2009-2013 历史表",
        pattern: /(20\d{2}\s*[-~至到]+\s*20\d{2}|20\d{2}\s*年.*至今|历史表|边界|不连续)/i,
      },
    ],
  },
];

const SOURCE_TYPE_OPTIONS = [
  {
    value: "PASTE_MD",
    label: "粘贴文本",
    requiresFile: false,
    uploadEnabled: true,
    uploadAccept: ".sql,.txt,.md,text/plain,text/markdown",
    dropzoneTitle: "将文件拖拽到这里，或点击上传",
    dropzoneSuffix: ".sql / .txt / .md（上传后自动识别来源类型）",
    rawLabel: "粘贴内容",
    placeholder: IMPORT_GHOST_PLACEHOLDER,
    modeHint: "当前为粘贴内容模式，请直接粘贴口径文档文本。",
  },
  {
    value: "FILE_MD",
    label: "Markdown 文件",
    requiresFile: true,
    uploadEnabled: true,
    uploadAccept: ".md,.markdown,text/markdown",
    dropzoneTitle: "请上传 Markdown 文件",
    dropzoneSuffix: ".md / .markdown",
    rawLabel: "导入文档内容",
    placeholder: "请先上传 Markdown 文件，系统会自动填充内容。",
    modeHint: "当前为 Markdown 文件模式，请上传 .md / .markdown 文件。",
  },
  {
    value: "FILE_TXT",
    label: "文本文件",
    requiresFile: true,
    uploadEnabled: true,
    uploadAccept: ".txt,text/plain",
    dropzoneTitle: "请上传文本文件",
    dropzoneSuffix: ".txt",
    rawLabel: "导入文档内容",
    placeholder: "请先上传 .txt 文件，系统会自动填充内容。",
    modeHint: "当前为文本文件模式，请上传 .txt 文件。",
  },
  {
    value: "FILE_SQL",
    label: "SQL 文件",
    requiresFile: true,
    uploadEnabled: true,
    uploadAccept: ".sql,text/plain",
    dropzoneTitle: "请上传 SQL 文件",
    dropzoneSuffix: ".sql",
    rawLabel: "导入 SQL 内容",
    placeholder: "请先上传 .sql 文件，系统会自动填充内容。",
    modeHint: "当前为 SQL 文件模式，请上传 .sql 文件。",
  },
  {
    value: "IMAGE_OCR_TEXT",
    label: "图片 OCR 文本（后续建设）",
    requiresFile: false,
    uploadEnabled: false,
    uploadAccept: "",
    dropzoneTitle: "图片直传后续建设",
    dropzoneSuffix: "当前请先粘贴 OCR 识别后的文本",
    rawLabel: "OCR 文本",
    placeholder: "请粘贴 OCR 识别后的文本内容…",
    modeHint: "图片直传后续建设，当前请先在外部完成 OCR 后粘贴文本。",
  },
];

const PREPROCESS_MODE_OPTIONS = [
  {
    value: "RULE_ONLY",
    label: "直接导入（不走大模型）",
    hint: "标准模板优先，速度最快，适合结构化口径文档。",
  },
  {
    value: "LLM_ONLY",
    label: "AI 预处理后导入",
    hint: "适合 Word/Excel/邮件等非结构化文本，耗时更长。",
  },
];

const SOURCE_TYPE_CONFIG_MAP = SOURCE_TYPE_OPTIONS.reduce((acc, item) => {
  acc[item.value] = item;
  return acc;
}, {});

function getSourceTypeConfig(type) {
  return SOURCE_TYPE_CONFIG_MAP[type] || SOURCE_TYPE_CONFIG_MAP.PASTE_MD;
}

function parsePreprocessResponse(response) {
  const importRoot = parseJsonText(response?.caliberImportJson, {});
  const scenes = Array.isArray(response?.scenes) && response.scenes.length > 0
    ? response.scenes
    : (Array.isArray(importRoot?.scenes) ? importRoot.scenes : []);
  const warnings = Array.isArray(response?.warnings) && response.warnings.length > 0
    ? response.warnings
    : (Array.isArray(importRoot?.parse_report?.warnings) ? importRoot.parse_report.warnings : []);
  const confidenceScore = Number(response?.confidenceScore ?? response?.quality?.confidence ?? scenes[0]?.quality?.confidence ?? 0);
  const confidenceLevel = `${response?.confidenceLevel || ""}`.trim() || (
    confidenceScore >= 0.85 ? "HIGH" : (confidenceScore >= 0.7 ? "MEDIUM" : "LOW")
  );
  const lowConfidence = typeof response?.lowConfidence === "boolean"
    ? response.lowConfidence
    : confidenceScore < 0.7;
  const batchUnmappedText = normalizeBatchUnmappedText(response, importRoot, scenes);
  return {
    mode: response?.mode || importRoot?._meta?.mode || "-",
    scenes,
    quality: response?.quality || scenes[0]?.quality || importRoot?.quality || null,
    warnings: warnings.filter((item) => `${item || ""}`.trim()),
    confidenceScore,
    confidenceLevel,
    lowConfidence,
    batchUnmappedText,
  };
}

function readArray(value) {
  return Array.isArray(value) ? value : [];
}

function readPositiveIntList(value) {
  return readArray(value)
    .map((item) => Number(item))
    .filter((item) => Number.isInteger(item) && item > 0);
}

function normalizeLineText(value) {
  return `${value || ""}`.replace(/\s+/g, " ").trim().toLowerCase();
}

function inferVariantSpan(rawLines, sqlText) {
  const sqlLines = `${sqlText || ""}`.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
  if (sqlLines.length === 0 || rawLines.length === 0) {
    return null;
  }
  const firstSqlLine = normalizeLineText(sqlLines[0]).slice(0, 42);
  if (!firstSqlLine) {
    return null;
  }
  const startIndex = rawLines.findIndex((line) => normalizeLineText(line).includes(firstSqlLine));
  if (startIndex < 0) {
    return null;
  }
  const spanLength = Math.max(1, Math.min(sqlLines.length, 120));
  return {
    start_line: startIndex + 1,
    end_line: Math.min(rawLines.length, startIndex + spanLength),
  };
}

function normalizeVariantSpans(rawLines, variant) {
  const spans = readArray(variant?.source_spans)
    .map((item) => ({
      start_line: Number(item?.start_line),
      end_line: Number(item?.end_line),
    }))
    .filter((item) => Number.isInteger(item.start_line) && Number.isInteger(item.end_line) && item.start_line > 0 && item.end_line > 0)
    .map((item) => ({
      start_line: Math.min(item.start_line, item.end_line),
      end_line: Math.max(item.start_line, item.end_line),
    }));
  if (spans.length > 0) {
    return spans;
  }
  const inferred = inferVariantSpan(rawLines, variant?.sql_text);
  return inferred ? [inferred] : [];
}

function collectHighlightLines(rawText, scene) {
  const lines = `${rawText || ""}`.split(/\r?\n/);
  const highlights = new Set();
  const evidenceLines = readPositiveIntList(scene?.source_evidence_lines || scene?.evidence_lines);
  evidenceLines.forEach((lineNo) => highlights.add(lineNo));

  readArray(scene?.sql_variants).forEach((variant) => {
    normalizeVariantSpans(lines, variant).forEach((span) => {
      for (let lineNo = span.start_line; lineNo <= span.end_line; lineNo += 1) {
        highlights.add(lineNo);
      }
    });
  });
  return highlights;
}

function formatSpanText(spans) {
  if (!Array.isArray(spans) || spans.length === 0) {
    return "未提供";
  }
  return spans.map((span) => `${span.start_line}-${span.end_line}`).join("，");
}

function resolveDomainIdByGuess(scene, domains) {
  const guess = `${scene?.domain_guess || ""}`.trim();
  if (!guess) {
    return "";
  }
  const matched = domains.find((item) => {
    const name = `${item.domainName || ""}`;
    const code = `${item.domainCode || ""}`.toUpperCase();
    return name.includes(guess) || code === guess.toUpperCase();
  });
  return matched ? `${matched.id}` : "";
}

function normalizeDomainOption(item) {
  const id = Number(item?.id || 0);
  if (!id) {
    return null;
  }
  const domainCode = `${item?.domainCode ?? item?.code ?? ""}`.trim();
  const domainName = `${item?.domainName ?? item?.name ?? ""}`.trim();
  const sortOrder = Number(item?.sortOrder ?? 0);
  return {
    ...item,
    id,
    domainCode,
    domainName,
    sortOrder: Number.isFinite(sortOrder) ? sortOrder : 0,
  };
}

const SCENE_DESC_SQL_START_PATTERN = /^(?:WITH|SELECT|INSERT|UPDATE|DELETE|MERGE|CREATE|ALTER|DROP)\b/i;
const SCENE_DESC_SQL_CLAUSE_PATTERN = /^(?:FROM|WHERE|JOIN|LEFT\s+JOIN|RIGHT\s+JOIN|INNER\s+JOIN|GROUP\s+BY|ORDER\s+BY|HAVING|UNION|LIMIT|ON|SET)\b/i;
const SCENE_DESC_SQL_FIELD_PATTERN = /^[A-Za-z_][A-Za-z0-9_$]*\.[A-Za-z_][A-Za-z0-9_$]*(?:\s+AS\b.*|.*,\s*|)$/i;

function looksLikeSqlDescriptionLine(line) {
  const normalized = `${line || ""}`.trim();
  if (!normalized) {
    return false;
  }
  const upper = normalized.toUpperCase();
  if (normalized.startsWith("```")) {
    return true;
  }
  if (upper === "SQL" || upper === "SQL语句" || upper === "SQL 语句") {
    return true;
  }
  if (SCENE_DESC_SQL_START_PATTERN.test(normalized) || SCENE_DESC_SQL_CLAUSE_PATTERN.test(normalized)) {
    return true;
  }
  if (SCENE_DESC_SQL_FIELD_PATTERN.test(normalized)) {
    return true;
  }
  if (upper.includes("SELECT ") && upper.includes(" FROM ")) {
    return true;
  }
  return false;
}

function sanitizeSceneDescription(text) {
  const safe = `${text || ""}`.replace(/\r\n/g, "\n").replace(/\r/g, "\n");
  if (!safe.trim()) {
    return "";
  }
  const cleaned = safe
    .split("\n")
    .map((line) => `${line || ""}`.trim())
    .filter((line) => line !== "" && !looksLikeSqlDescriptionLine(line));
  return cleaned.join("\n").replace(/\n{3,}/g, "\n\n").trim();
}

function buildFormByScene(scene, previousForm, domains) {
  const hasSceneDescription = scene && Object.prototype.hasOwnProperty.call(scene, "scene_description");
  const cleanedSceneDescription = sanitizeSceneDescription(scene?.scene_description);
  const nextForm = {
    ...previousForm,
    sceneTitle: scene?.scene_title || previousForm.sceneTitle,
    sceneDescription: hasSceneDescription ? cleanedSceneDescription : previousForm.sceneDescription,
    caliberDefinition: scene?.caliber_definition || previousForm.caliberDefinition,
    inputsJson: safeStringify(scene?.inputs || { params: [], constraints: [] }),
    outputsJson: safeStringify(scene?.outputs || { summary: "", fields: [] }),
    sqlVariantsJson: safeStringify(scene?.sql_variants || []),
    codeMappingsJson: safeStringify(scene?.code_mappings || []),
    caveatsJson: safeStringify(scene?.caveats || []),
    qualityJson: safeStringify(scene?.quality || {}),
  };
  const matchedDomainId = resolveDomainIdByGuess(scene, domains);
  if (matchedDomainId) {
    nextForm.domainId = matchedDomainId;
  }
  return nextForm;
}

function parseJsonArrayOrEmpty(text) {
  const parsed = parseJsonText(text, []);
  return Array.isArray(parsed) ? parsed : [];
}

function clonePlainObject(value) {
  return value && typeof value === "object" && !Array.isArray(value) ? { ...value } : {};
}

function parseSourceTablesText(value) {
  const values = Array.isArray(value)
    ? value
    : `${value || ""}`.split(/[，,\n\r]/);
  return Array.from(new Set(values
    .map((item) => `${item || ""}`.trim())
    .filter((item) => item !== "")));
}

function parseApplicablePeriod(text) {
  const raw = `${text || ""}`.trim();
  if (!raw) {
    return {
      parseable: true,
      startDate: "",
      endDate: "",
      toPresent: false,
      rawText: "",
    };
  }
  const rangeMatch = raw.match(/^(\d{4}-\d{2}-\d{2})\s*(?:~|～|-|—|–|至|到)\s*(\d{4}-\d{2}-\d{2}|至今)$/);
  if (rangeMatch) {
    const endPart = rangeMatch[2];
    const toPresent = endPart === "至今";
    return {
      parseable: true,
      startDate: rangeMatch[1],
      endDate: toPresent ? "" : endPart,
      toPresent,
      rawText: raw,
    };
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
    return {
      parseable: true,
      startDate: raw,
      endDate: "",
      toPresent: false,
      rawText: raw,
    };
  }
  return {
    parseable: false,
    startDate: "",
    endDate: "",
    toPresent: false,
    rawText: raw,
  };
}

function composeApplicablePeriod({ startDate, endDate, toPresent }) {
  const start = `${startDate || ""}`.trim();
  const end = `${endDate || ""}`.trim();
  if (start && toPresent) {
    return `${start} ~ 至今`;
  }
  if (start && end) {
    return `${start} ~ ${end}`;
  }
  if (start) {
    return start;
  }
  if (end) {
    return end;
  }
  return "";
}

function tryFormatSqlText(sqlText, warnings, variantLabel) {
  const rawSql = `${sqlText || ""}`.trim();
  if (!rawSql) {
    return "";
  }
  try {
    return formatSql(rawSql, {
      language: "sql",
      keywordCase: "upper",
      tabWidth: 2,
      linesBetweenQueries: 1,
    }).trim();
  } catch (_) {
    warnings.push(`${variantLabel} SQL 格式化失败，已保留原文。`);
    return rawSql;
  }
}

function normalizeBatchUnmappedText(response, importRoot, scenes) {
  const topNotes = `${response?.global?.notes || ""}`.trim();
  if (topNotes) {
    return topNotes;
  }
  const innerNotes = `${importRoot?.global?.notes || ""}`.trim();
  if (innerNotes) {
    return innerNotes;
  }
  const merged = Array.from(new Set(
    readArray(scenes)
      .map((scene) => `${scene?.unmapped_text || ""}`.trim())
      .filter((item) => item !== ""),
  ));
  return merged.join("\n\n");
}

const IMPORT_STAGE_ORDER = {
  split: 1,
  extract: 2,
  merge: 3,
  normalize: 4,
  draft_persist: 5,
  finalize: 6,
};

function toSourceTablesText(value) {
  return readArray(value)
    .map((item) => `${item || ""}`.trim())
    .filter((item) => item !== "")
    .join(", ");
}

const EMPTY_FORM = {
  sceneTitle: "",
  domainId: "",
  sceneDescription: "",
  caliberDefinition: "",
  inputsJson: "{\n  \"params\": [],\n  \"constraints\": []\n}",
  outputsJson: "{\n  \"summary\": \"\",\n  \"fields\": []\n}",
  sqlVariantsJson: "[]",
  codeMappingsJson: "[]",
  caveatsJson: "[]",
  qualityJson: "{}",
};

const IMPORT_TASK_STORAGE_KEY = "dd.import.activeTaskId";

function clampStep(step) {
  const value = Number(step || 1);
  if (!Number.isFinite(value)) {
    return 1;
  }
  return Math.max(1, Math.min(4, Math.trunc(value)));
}

function resolveStepByTask(task) {
  if (!task || typeof task !== "object") {
    return 1;
  }
  const directStep = clampStep(task.currentStep);
  const normalizedStatus = `${task.status || ""}`.trim().toUpperCase();
  if (normalizedStatus === "COMPLETED") {
    return 4;
  }
  if (normalizedStatus === "PUBLISHING") {
    return 4;
  }
  if (normalizedStatus === "SCENE_REVIEWING") {
    return 3;
  }
  if (normalizedStatus === "QUALITY_REVIEWING") {
    return 2;
  }
  if (normalizedStatus === "RUNNING") {
    return 1;
  }
  if (normalizedStatus === "FAILED") {
    return Math.max(1, Math.min(2, directStep));
  }
  return directStep;
}

export function KnowledgePage({ preset = "import" }) {
  const navigate = useNavigate();
  const token = useAuthStore((state) => state.token);
  const username = useAuthStore((state) => state.username);
  const [domains, setDomains] = useState([]);
  const [domainLoading, setDomainLoading] = useState(false);
  const [domainLoadError, setDomainLoadError] = useState("");

  const [sourceType, setSourceType] = useState("PASTE_MD");
  const [preprocessMode, setPreprocessMode] = useState("RULE_ONLY");
  const [sourceName, setSourceName] = useState("");
  const [rawText, setRawText] = useState("");
  const [fileMeta, setFileMeta] = useState(null);
  const [preprocessMeta, setPreprocessMeta] = useState("");
  const [preprocessJson, setPreprocessJson] = useState("");
  const [preprocessWarnings, setPreprocessWarnings] = useState([]);
  const [preprocessScenes, setPreprocessScenes] = useState([]);
  const [batchUnmappedText, setBatchUnmappedText] = useState("");
  const [sceneDrafts, setSceneDrafts] = useState([]);
  const [sceneForms, setSceneForms] = useState({});
  const [selectedSceneIndex, setSelectedSceneIndex] = useState(0);
  const [sceneId, setSceneId] = useState(null);
  const [sceneStatus, setSceneStatus] = useState("DRAFT");
  const [importStageTimings, setImportStageTimings] = useState([]);
  const [importPercent, setImportPercent] = useState(0);
  const [importStageText, setImportStageText] = useState("待导入");
  const [importElapsedMs, setImportElapsedMs] = useState(0);
  const [importBatchId, setImportBatchId] = useState("");
  const [importTaskStatus, setImportTaskStatus] = useState("");
  const [recentImportTasks, setRecentImportTasks] = useState([]);
  const [taskListLoading, setTaskListLoading] = useState(false);
  const [sceneQueueKeyword, setSceneQueueKeyword] = useState("");
  const [showImportDone, setShowImportDone] = useState(false);
  const [activeStep, setActiveStep] = useState(1);
  const [qualityConfirmed, setQualityConfirmed] = useState(false);
  const [compareConfirmed, setCompareConfirmed] = useState(false);
  const [lowConfidenceAckMap, setLowConfidenceAckMap] = useState({});
  const [publishVerifiedAt, setPublishVerifiedAt] = useState(nowLocalInputValue());
  const [publishSummary, setPublishSummary] = useState("");
  const [minimumUnitCheck, setMinimumUnitCheck] = useState(null);
  const [minimumUnitLoading, setMinimumUnitLoading] = useState(false);
  const [qualityMsg, setQualityMsg] = useState("待导入");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [activeVariantIndex, setActiveVariantIndex] = useState(-1);
  const [dropActive, setDropActive] = useState(false);
  const lineRefMap = useRef(new Map());
  const stepCardRefMap = useRef(new Map());
  const fileInputRef = useRef(null);
  const sourceTypeConfig = useMemo(() => getSourceTypeConfig(sourceType), [sourceType]);
  const isUploadSourceType = sourceTypeConfig.requiresFile;
  const isPasteSourceType = !sourceTypeConfig.requiresFile;
  const sourceFileReady = useMemo(
    () => !sourceTypeConfig.requiresFile || Boolean(fileMeta),
    [sourceTypeConfig.requiresFile, fileMeta],
  );
  const importGuideGroups = useMemo(() => {
    const text = `${rawText || ""}`;
    return IMPORT_STANDARD_HINT_GROUPS.map((group) => ({
      ...group,
      items: group.items.map((item) => ({
        ...item,
        matched: text ? item.pattern.test(text) : false,
      })),
    }));
  }, [rawText]);

  const [form, setForm] = useState(EMPTY_FORM);

  const loadDomains = useCallback(async () => {
    setDomainLoading(true);
    setDomainLoadError("");
    try {
      // 场景绑定业务领域仅使用全量业务领域主数据
      const result = await apiRequest("/domains", { token });
      const nextDomains = Array.isArray(result) ? result.map((item) => normalizeDomainOption(item)).filter(Boolean) : [];
      setDomains(nextDomains);
      if (nextDomains.length === 0) {
        setDomainLoadError("暂无业务领域，请先在业务领域管理维护。");
      }
      return nextDomains;
    } catch (err) {
      setDomains([]);
      setDomainLoadError(err?.message || "全量业务领域加载失败，请检查 /api/domains 接口。");
      return [];
    } finally {
      setDomainLoading(false);
    }
  }, [token]);

  const loadRecentImportTasks = useCallback(async () => {
    if (preset !== "import") {
      return [];
    }
    setTaskListLoading(true);
    try {
      const tasks = await apiRequest("/import/tasks", {
        token,
        query: {
          limit: 12,
        },
      });
      const normalized = Array.isArray(tasks) ? tasks : [];
      setRecentImportTasks(normalized);
      return normalized;
    } catch (_) {
      setRecentImportTasks([]);
      return [];
    } finally {
      setTaskListLoading(false);
    }
  }, [preset, token]);

  const loadMinimumUnitCheck = useCallback(async (sceneIdInput) => {
    const id = Number(sceneIdInput || 0);
    if (!id) {
      setMinimumUnitCheck(null);
      return null;
    }
    setMinimumUnitLoading(true);
    try {
      const result = await apiRequest(`/scenes/${id}/minimum-unit-check`, { token });
      setMinimumUnitCheck(result || null);
      return result;
    } catch (_) {
      setMinimumUnitCheck(null);
      return null;
    } finally {
      setMinimumUnitLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadDomains();
    if (preset === "import") {
      loadRecentImportTasks();
    }
    if (preset === "manual") {
      setRawText("");
      setPreprocessMeta("手动创建模式：请直接填写草稿内容。");
      setSceneDrafts([]);
      setSceneForms({});
      setBatchUnmappedText("");
    }
  }, [preset, loadDomains, loadRecentImportTasks]);

  useEffect(() => {
    if (activeStep === 4) {
      loadDomains();
    }
    if (activeStep === 1 && preset === "import") {
      loadRecentImportTasks();
    }
  }, [activeStep, loadDomains, loadRecentImportTasks, preset]);

  useEffect(() => {
    if (preset !== "import" || typeof window === "undefined") {
      return;
    }
    const query = new URLSearchParams(window.location.search);
    const queryTaskId = `${query.get("taskId") || ""}`.trim();
    const cachedTaskId = `${window.localStorage.getItem(IMPORT_TASK_STORAGE_KEY) || ""}`.trim();
    const candidate = queryTaskId || cachedTaskId;
    if (candidate) {
      restoreImportTask(candidate);
    }
    // restoreImportTask intentionally keeps latest in-render state;
    // adding it as dependency will repeatedly refetch task on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [preset]);

  useEffect(() => {
    if (!hasUnsavedChanges) {
      return undefined;
    }
    const handleBeforeUnload = (event) => {
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [hasUnsavedChanges]);

  useEffect(() => {
    setActiveVariantIndex(-1);
  }, [selectedSceneIndex]);

  useEffect(() => {
    if (preprocessScenes.length === 0) {
      return;
    }
    setSceneForms((prev) => ({ ...prev, [selectedSceneIndex]: form }));
  }, [form, preprocessScenes.length, selectedSceneIndex]);

  useEffect(() => {
    const currentDraft = sceneDrafts[selectedSceneIndex];
    setSceneId(currentDraft?.sceneId || null);
    setSceneStatus(currentDraft?.status || "DRAFT");
  }, [sceneDrafts, selectedSceneIndex]);

  useEffect(() => {
    const draft = sceneDrafts[selectedSceneIndex];
    const currentSceneId = draft?.sceneId || sceneId;
    if (!currentSceneId) {
      setMinimumUnitCheck(null);
      return;
    }
    loadMinimumUnitCheck(currentSceneId);
  }, [sceneDrafts, selectedSceneIndex, sceneId, loadMinimumUnitCheck]);

  useEffect(() => {
    const target = stepCardRefMap.current.get(activeStep);
    if (!target) {
      return;
    }
    const top = window.scrollY + target.getBoundingClientRect().top - 18;
    window.scrollTo({
      top: Math.max(0, top),
      behavior: "smooth",
    });
  }, [activeStep]);

  function updateFormField(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
    setHasUnsavedChanges(true);
  }

  async function handleReloadDomains() {
    const nextDomains = await loadDomains();
    if (nextDomains.length > 0) {
      setPreprocessMeta(`业务领域刷新完成，共 ${nextDomains.length} 个可选项。`);
      setError("");
      return;
    }
    setError("当前无可选业务领域，请点击\"去业务领域管理\"先创建业务领域。");
  }

  async function quickCreateDefaultDomain() {
    setDomainLoading(true);
    setError("");
    try {
      const payload = {
        domainCode: "UNCLASSIFIED",
        domainName: "未分类业务领域",
        domainOverview: "用于导入阶段临时归集未明确归属的业务场景，发布前建议调整到正式业务领域。",
        commonTables: "",
        contacts: "",
        sortOrder: 9999,
        operator: "",
      };
      const created = await apiRequest("/domains", {
        method: "POST",
        token,
        body: payload,
      });
      const nextDomains = await loadDomains();
      const createdId = Number(created?.id || 0);
      if (createdId > 0) {
        setForm((prev) => ({ ...prev, domainId: `${createdId}` }));
      } else if (nextDomains.length > 0) {
        setForm((prev) => ({ ...prev, domainId: `${nextDomains[0].id}` }));
      }
      setPreprocessMeta("已创建默认业务领域，可继续保存当前场景。");
    } catch (err) {
      setError(err.message || "创建默认业务领域失败，请前往业务领域管理手动创建。");
    } finally {
      setDomainLoading(false);
    }
  }

  function goDomainManagement() {
    setPreprocessMeta("正在跳转业务领域管理…");
    navigate("/knowledge/domains");
  }

  function validateSelectedDomainOrThrow(formData = form) {
    const selected = `${formData?.domainId || ""}`.trim();
    if (!selected) {
      throw new Error("请先选择业务领域后再保存当前场景");
    }
    const selectedId = Number(selected);
    const matched = domains.find((item) => Number(item?.id || 0) === selectedId);
    if (!matched) {
      throw new Error("当前业务领域无效，请重载业务领域后重新选择");
    }
    return selectedId;
  }

  function mutateSqlVariants(mutator) {
    setForm((prev) => {
      const variants = parseJsonArrayOrEmpty(prev.sqlVariantsJson).map((item) => clonePlainObject(item));
      mutator(variants);
      return {
        ...prev,
        sqlVariantsJson: safeStringify(variants),
      };
    });
    setHasUnsavedChanges(true);
  }

  function mutateCodeMappings(mutator) {
    setForm((prev) => {
      const codeMappings = parseJsonArrayOrEmpty(prev.codeMappingsJson).map((item) => {
        const block = clonePlainObject(item);
        block.mappings = readArray(block.mappings).map((mapping) => clonePlainObject(mapping));
        return block;
      });
      mutator(codeMappings);
      return {
        ...prev,
        codeMappingsJson: safeStringify(codeMappings),
      };
    });
    setHasUnsavedChanges(true);
  }

  function addSqlVariant() {
    mutateSqlVariants((variants) => {
      variants.push({
        variant_name: `取数方案${variants.length + 1}`,
        applicable_period: "",
        sql_text: "",
        source_tables: [],
        notes: "",
      });
    });
  }

  function removeSqlVariant(index) {
    mutateSqlVariants((variants) => {
      if (index < 0 || index >= variants.length) {
        return;
      }
      variants.splice(index, 1);
    });
  }

  function updateSqlVariantField(index, field, value) {
    mutateSqlVariants((variants) => {
      const current = clonePlainObject(variants[index]);
      if (field === "source_tables") {
        current.source_tables = parseSourceTablesText(value);
      } else {
        current[field] = value;
      }
      variants[index] = current;
    });
  }

  function updateSqlVariantPeriod(index, patch = {}) {
    mutateSqlVariants((variants) => {
      const current = clonePlainObject(variants[index]);
      const parsed = parseApplicablePeriod(current?.applicable_period || current?.applicablePeriod);
      const nextPeriod = {
        startDate: parsed.startDate,
        endDate: parsed.endDate,
        toPresent: parsed.toPresent,
        ...patch,
      };
      if (nextPeriod.toPresent) {
        nextPeriod.endDate = "";
      }
      current.applicable_period = composeApplicablePeriod(nextPeriod);
      variants[index] = current;
    });
  }

  function addCodeMappingGroup() {
    mutateCodeMappings((codeMappings) => {
      codeMappings.push({
        code: "",
        description: "",
        mappings: [{ value_code: "", value_name: "" }],
      });
    });
  }

  function removeCodeMappingGroup(index) {
    mutateCodeMappings((codeMappings) => {
      if (index < 0 || index >= codeMappings.length) {
        return;
      }
      codeMappings.splice(index, 1);
    });
  }

  function updateCodeMappingField(index, field, value) {
    mutateCodeMappings((codeMappings) => {
      const current = clonePlainObject(codeMappings[index]);
      current.mappings = readArray(current.mappings).map((mapping) => clonePlainObject(mapping));
      current[field] = value;
      codeMappings[index] = current;
    });
  }

  function addCodeMappingValue(index) {
    mutateCodeMappings((codeMappings) => {
      const current = clonePlainObject(codeMappings[index]);
      current.mappings = readArray(current.mappings).map((mapping) => clonePlainObject(mapping));
      current.mappings.push({ value_code: "", value_name: "" });
      codeMappings[index] = current;
    });
  }

  function removeCodeMappingValue(groupIndex, mappingIndex) {
    mutateCodeMappings((codeMappings) => {
      const current = clonePlainObject(codeMappings[groupIndex]);
      const mappings = readArray(current.mappings).map((mapping) => clonePlainObject(mapping));
      if (mappingIndex < 0 || mappingIndex >= mappings.length) {
        return;
      }
      mappings.splice(mappingIndex, 1);
      current.mappings = mappings;
      codeMappings[groupIndex] = current;
    });
  }

  function updateCodeMappingValue(groupIndex, mappingIndex, field, value) {
    mutateCodeMappings((codeMappings) => {
      const current = clonePlainObject(codeMappings[groupIndex]);
      const mappings = readArray(current.mappings).map((mapping) => clonePlainObject(mapping));
      const nextItem = clonePlainObject(mappings[mappingIndex]);
      nextItem[field] = value;
      mappings[mappingIndex] = nextItem;
      current.mappings = mappings;
      codeMappings[groupIndex] = current;
    });
  }

  function persistTaskId(taskId) {
    const normalized = `${taskId || ""}`.trim();
    setImportBatchId(normalized);
    if (typeof window === "undefined") {
      return;
    }
    try {
      if (normalized) {
        window.localStorage.setItem(IMPORT_TASK_STORAGE_KEY, normalized);
      } else {
        window.localStorage.removeItem(IMPORT_TASK_STORAGE_KEY);
      }
    } catch (_) {
      // ignore storage error
    }
  }

  function applyPreprocessPayload(response, options = {}) {
    const { keepActiveStep = false } = options;
    setPreprocessJson(safeStringify(response));
    const parsed = parsePreprocessResponse(response);
    const sceneCount = parsed.scenes.length;
    const scene = sceneCount > 0 ? parsed.scenes[0] : null;
    const nextSceneForms = {};
    parsed.scenes.forEach((item, index) => {
      nextSceneForms[index] = buildFormByScene(item, EMPTY_FORM, domains);
    });
    setPreprocessScenes(parsed.scenes);
    setSceneForms(nextSceneForms);
    setSelectedSceneIndex(0);
    setForm(nextSceneForms[0] || EMPTY_FORM);
    setBatchUnmappedText(parsed.batchUnmappedText || "");

    const qualityConfidence = Number(parsed.confidenceScore ?? parsed.quality?.confidence ?? 0);
    setPreprocessWarnings(parsed.warnings);
    const draftsFromResponse = Array.isArray(response?.sceneDrafts) ? response.sceneDrafts : [];
    const normalizedDrafts = parsed.scenes.map((item, index) => {
      const draft = draftsFromResponse[index] || {};
      const confidence = Number(draft.confidenceScore ?? item?.quality?.confidence ?? qualityConfidence ?? 0);
      return {
        sceneIndex: index,
        sceneTitle: draft.sceneTitle || item.scene_title || `场景${index + 1}`,
        sceneId: draft.sceneId || null,
        status: draft.status || "DRAFT",
        confidenceScore: confidence,
        lowConfidence: typeof draft.lowConfidence === "boolean" ? draft.lowConfidence : confidence < 0.7,
        warnings: Array.isArray(draft.warnings) ? draft.warnings : [],
      };
    });
    setSceneDrafts(normalizedDrafts);
    if (Array.isArray(response?.stageTimings) && response.stageTimings.length > 0) {
      setImportStageTimings(response.stageTimings);
    }
    setImportElapsedMs(Number(response?.totalElapsedMs || importElapsedMs || 0));
    if (`${response?.importBatchId || ""}`.trim()) {
      persistTaskId(response.importBatchId);
    }
    setImportPercent(100);
    setImportStageText("导入与草稿生成完成");
    setQualityMsg(scene
      ? `共识别 ${sceneCount} 个场景，当前应用第 1 个；置信度 ${(qualityConfidence * 100).toFixed(0)}%（${parsed.confidenceLevel}）`
      : "抽取完成，但未识别到可用场景");
    if (scene) {
      setPreprocessMeta(`导入完成 · 模式 ${parsed.mode}${sceneCount > 1 ? ` · 共识别 ${sceneCount} 个场景` : ""}`);
    }

    if (!scene) {
      setSceneId(null);
      setSceneStatus("DRAFT");
      setPreprocessScenes([]);
      setSceneDrafts([]);
      setSceneForms({});
      setBatchUnmappedText("");
      setForm(EMPTY_FORM);
      setSelectedSceneIndex(0);
      if (!keepActiveStep) {
        setActiveStep(1);
      }
      if (parsed.warnings.length > 0) {
        setError(`未识别到场景：${parsed.warnings.join("；")}`);
      }
      return {
        parsed,
        hasScene: false,
      };
    }

    if (parsed.lowConfidence) {
      setError("检测到低置信度场景，请逐个复核后再发布。");
    }
    if (!keepActiveStep) {
      setActiveStep(2);
    }
    setHasUnsavedChanges(false);
    return {
      parsed,
      hasScene: true,
    };
  }

  async function restoreImportTask(taskId) {
    const normalizedTaskId = `${taskId || ""}`.trim();
    if (!normalizedTaskId || preset !== "import") {
      return;
    }
    try {
      const task = await apiRequest(`/import/tasks/${encodeURIComponent(normalizedTaskId)}`, { token });
      persistTaskId(task?.taskId || normalizedTaskId);
      setImportTaskStatus(`${task?.status || ""}`.toUpperCase());
      if (`${task?.sourceType || ""}`.trim()) {
        setSourceType(task.sourceType);
      }
      setSourceName(task?.sourceName || "");
      if (`${task?.rawText || ""}`.trim()) {
        setRawText(task.rawText);
      }
      setQualityConfirmed(Boolean(task?.qualityConfirmed));
      setCompareConfirmed(Boolean(task?.compareConfirmed));
      const nextStep = resolveStepByTask(task);
      setActiveStep(nextStep);
      if (task?.preprocessResult) {
        applyPreprocessPayload(task.preprocessResult, { keepActiveStep: true });
      }
      setPreprocessMeta(`已恢复导入任务 ${normalizedTaskId.slice(0, 8)} · 当前步骤 ${nextStep}`);
      if (`${task?.status || ""}`.toUpperCase() === "FAILED" && `${task?.errorMessage || ""}`.trim()) {
        setError(task.errorMessage);
      }
    } catch (_) {
      persistTaskId("");
    }
  }

  function handleSourceTypeChange(nextType) {
    const nextConfig = getSourceTypeConfig(nextType);
    setSourceType(nextType);
    setError("");
    setDropActive(false);
    setHasUnsavedChanges(true);
    setPreprocessMeta(nextConfig.modeHint);

    if (nextType === "PASTE_MD") {
      setFileMeta(null);
      setSourceName("");
      return;
    }

    if (!nextConfig.uploadEnabled) {
      setFileMeta(null);
      setSourceName("");
      return;
    }

    if (!fileMeta?.name) {
      return;
    }
    const detectedType = detectSourceTypeByFileName(fileMeta.name);
    if (nextType !== "PASTE_MD" && detectedType !== nextType) {
      setFileMeta(null);
      setSourceName("");
    }
  }

  function handlePreprocessModeChange(nextMode) {
    const normalized = `${nextMode || ""}`.trim().toUpperCase();
    if (normalized !== "RULE_ONLY" && normalized !== "LLM_ONLY") {
      return;
    }
    setPreprocessMode(normalized);
    setHasUnsavedChanges(true);
    if (normalized === "RULE_ONLY") {
      setPreprocessMeta("当前为直接导入模式：按标准模板直接生成草稿，不调用大模型。");
    } else {
      setPreprocessMeta("当前为 AI 预处理模式：先做大模型抽取，再生成草稿。");
    }
  }

  async function applySourceFile(file) {
    if (!file) {
      return false;
    }
    if (!sourceTypeConfig.uploadEnabled) {
      setError(sourceTypeConfig.modeHint);
      setPreprocessMeta(sourceTypeConfig.modeHint);
      return false;
    }
    try {
      const text = await file.text();
      const detectedType = detectSourceTypeByFileName(file.name);
      const detectedConfig = getSourceTypeConfig(detectedType);
      const switched = detectedType !== sourceType;
      setSourceName(file.name || "");
      if (switched) {
        setSourceType(detectedType);
      }
      setRawText(text);
      setFileMeta({
        name: file.name || "未命名文件",
        size: file.size || 0,
        charCount: text.length,
        sourceType: detectedType,
      });
      setError("");
      setHasUnsavedChanges(true);
      setPreprocessMeta(switched
        ? `已根据文件后缀切换为“${detectedConfig.label}” · 已加载文件：${file.name}（${text.length} 字符）`
        : `已加载文件：${file.name}（${text.length} 字符）`);
      return true;
    } catch (_) {
      setError("文件读取失败，请重试");
      return false;
    }
  }

  async function handleFileUpload(event) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    try {
      await applySourceFile(file);
    } finally {
      event.target.value = "";
    }
  }

  function handleDropZoneClick() {
    if (!sourceTypeConfig.uploadEnabled) {
      setError(sourceTypeConfig.modeHint);
      setPreprocessMeta(sourceTypeConfig.modeHint);
      return;
    }
    fileInputRef.current?.click();
  }

  async function handleDrop(event) {
    event.preventDefault();
    event.stopPropagation();
    setDropActive(false);
    if (!sourceTypeConfig.uploadEnabled) {
      setError(sourceTypeConfig.modeHint);
      setPreprocessMeta(sourceTypeConfig.modeHint);
      return;
    }
    const file = event.dataTransfer?.files?.[0];
    if (!file) {
      return;
    }
    await applySourceFile(file);
  }

  function handleDragOver(event) {
    event.preventDefault();
    event.stopPropagation();
    if (!sourceTypeConfig.uploadEnabled) {
      return;
    }
    if (!dropActive) {
      setDropActive(true);
    }
  }

  function handleDragLeave(event) {
    event.preventDefault();
    event.stopPropagation();
    if (!sourceTypeConfig.uploadEnabled) {
      return;
    }
    if (dropActive) {
      setDropActive(false);
    }
  }

  function fillBestPracticeSample() {
    setSourceType("PASTE_MD");
    setPreprocessMode("RULE_ONLY");
    setSourceName("最佳实践样例");
    setRawText(IMPORT_BEST_PRACTICE_SAMPLE);
    setFileMeta(null);
    setError("");
    setDropActive(false);
    setHasUnsavedChanges(true);
    setPreprocessMeta("已填入最佳实践样例，可直接点击“导入并生成草稿”体验标准效果。");
  }

  function normalizeScenePayload(formData = form) {
    const notices = [];
    const normalizedVariants = parseJsonArrayOrEmpty(formData.sqlVariantsJson).map((item, index) => {
      const variant = clonePlainObject(item);
      const variantName = `${variant?.variant_name || variant?.variantName || ""}`.trim() || `取数方案${index + 1}`;
      const rawPeriod = `${variant?.applicable_period || variant?.applicablePeriod || ""}`.trim();
      const parsedPeriod = parseApplicablePeriod(rawPeriod);
      const applicablePeriod = parsedPeriod.parseable
        ? composeApplicablePeriod(parsedPeriod)
        : parsedPeriod.rawText;
      return {
        variant_name: variantName,
        applicable_period: applicablePeriod,
        sql_text: tryFormatSqlText(variant?.sql_text || variant?.sqlText, notices, variantName),
        source_tables: parseSourceTablesText(variant?.source_tables || variant?.sourceTables || variant?.source_tables_text),
        notes: `${variant?.notes || ""}`.trim(),
      };
    });
    const normalizedCodeMappings = parseJsonArrayOrEmpty(formData.codeMappingsJson).map((item) => {
      const group = clonePlainObject(item);
      return {
        code: `${group.code || ""}`.trim(),
        description: `${group.description || ""}`.trim(),
        mappings: readArray(group.mappings).map((mapping) => {
          const next = clonePlainObject(mapping);
          return {
            value_code: `${next.value_code ?? next.valueCode ?? ""}`.trim(),
            value_name: `${next.value_name ?? next.valueName ?? next.label ?? ""}`.trim(),
          };
        }).filter((mapping) => mapping.value_code || mapping.value_name),
      };
    }).filter((group) => group.code || group.description || group.mappings.length > 0);
    const normalizedInputsJson = safeStringify(parseJsonText(formData.inputsJson, { params: [], constraints: [] }));
    const normalizedOutputsJson = safeStringify(parseJsonText(formData.outputsJson, { summary: "", fields: [] }));
    const normalizedCaveatsJson = safeStringify(parseJsonText(formData.caveatsJson, []));
    const normalizedQualityJson = safeStringify(parseJsonText(formData.qualityJson, {}));
    const normalizedVariantsJson = safeStringify(normalizedVariants);
    const normalizedCodeMappingsJson = safeStringify(normalizedCodeMappings);
    const selectedDomainId = Number(formData.domainId || 0);
    const validDomain = domains.find((item) => Number(item?.id || 0) === selectedDomainId);
    const normalizedDomainId = validDomain ? Number(validDomain.id) : null;
    const normalizedForm = {
      ...formData,
      sceneTitle: formData.sceneTitle.trim(),
      domainId: normalizedDomainId ? `${normalizedDomainId}` : "",
      inputsJson: normalizedInputsJson,
      outputsJson: normalizedOutputsJson,
      sqlVariantsJson: normalizedVariantsJson,
      codeMappingsJson: normalizedCodeMappingsJson,
      caveatsJson: normalizedCaveatsJson,
      qualityJson: normalizedQualityJson,
    };
    const payload = {
      sceneTitle: normalizedForm.sceneTitle,
      domainId: normalizedDomainId,
      domain: "",
      sceneDescription: formData.sceneDescription,
      caliberDefinition: formData.caliberDefinition,
      applicability: "",
      boundaries: "",
      inputsJson: normalizedInputsJson,
      outputsJson: normalizedOutputsJson,
      sqlVariantsJson: normalizedVariantsJson,
      codeMappingsJson: normalizedCodeMappingsJson,
      contributors: username || "",
      sqlBlocksJson: "[]",
      sourceTablesJson: "[]",
      caveatsJson: normalizedCaveatsJson,
      unmappedText: "",
      qualityJson: normalizedQualityJson,
      rawInput: rawText,
      operator: "",
    };
    return {
      payload,
      normalizedForm,
      notices,
    };
  }

  function applyDraftUpdate(index, patch) {
    setSceneDrafts((prev) => {
      const next = [...prev];
      const fallbackScene = preprocessScenes[index];
      const base = next[index] || {
        sceneIndex: index,
        sceneTitle: fallbackScene?.scene_title || `场景${index + 1}`,
        sceneId: null,
        status: "DRAFT",
        confidenceScore: Number(fallbackScene?.quality?.confidence || 0),
        lowConfidence: Number(fallbackScene?.quality?.confidence || 0) < 0.7,
        warnings: [],
      };
      next[index] = {
        ...base,
        ...patch,
      };
      return next;
    });
  }

  function focusScene(index) {
    if (preprocessScenes.length === 0) {
      return;
    }
    const safeIndex = Math.max(0, Math.min(index, preprocessScenes.length - 1));
    setSelectedSceneIndex(safeIndex);
    const cached = sceneForms[safeIndex];
    if (cached) {
      setForm(cached);
      return;
    }
    const scene = preprocessScenes[safeIndex];
    if (scene) {
      const built = buildFormByScene(scene, EMPTY_FORM, domains);
      setForm(built);
      setSceneForms((prev) => ({ ...prev, [safeIndex]: built }));
    }
  }

  function focusNextProcessable(startIndex, drafts = sceneDrafts) {
    if (!Array.isArray(drafts) || drafts.length === 0) {
      if (preprocessScenes.length > 1) {
        focusScene((startIndex + 1) % preprocessScenes.length);
      }
      return;
    }
    for (let i = startIndex + 1; i < drafts.length; i += 1) {
      const status = `${drafts[i]?.status || ""}`.toUpperCase();
      if (status === "DRAFT") {
        focusScene(i);
        return;
      }
    }
    for (let i = 0; i < drafts.length; i += 1) {
      const status = `${drafts[i]?.status || ""}`.toUpperCase();
      if (status === "DRAFT") {
        focusScene(i);
        return;
      }
    }
  }

  async function runPreprocess() {
    if (sourceTypeConfig.requiresFile && !fileMeta) {
      setError(`当前来源类型为“${sourceTypeConfig.label}”，请先上传对应文件。`);
      setPreprocessMeta(sourceTypeConfig.modeHint);
      return;
    }
    if (!rawText.trim()) {
      setError("请先粘贴或上传口径文档");
      return;
    }
    setLoading(true);
    setError("");
    setPreprocessWarnings([]);
    setImportStageTimings([]);
    setImportPercent(0);
    setImportStageText("导入任务已提交");
    setImportElapsedMs(0);
    persistTaskId("");
    setImportTaskStatus("RUNNING");
    setShowImportDone(false);
    setActiveStep(1);
    setQualityConfirmed(false);
    setCompareConfirmed(false);
    setSceneQueueKeyword("");
    setLowConfidenceAckMap({});
    setPreprocessScenes([]);
    setSceneDrafts([]);
    setSceneForms({});
    setBatchUnmappedText("");
    setSelectedSceneIndex(0);
    setForm(EMPTY_FORM);
    const start = Date.now();
    try {
      const requestBody = {
        rawText,
        sourceType,
        sourceName: sourceName || fileMeta?.name || undefined,
        preprocessMode,
        autoCreateDrafts: preset === "import",
        operator: "",
      };
      setPreprocessMeta("导入启动 · 正在执行抽取与草稿生成…");
      const response = await apiSseRequest("/import/preprocess-stream", {
        token,
        body: requestBody,
        onEvent: (event) => {
          if (event.event === "start") {
            const detail = event.data || {};
            const taskId = `${detail.taskId || ""}`.trim();
            if (taskId) {
              persistTaskId(taskId);
            }
            return;
          }
          if (event.event === "stage") {
            const detail = event.data || {};
            const percent = Math.max(0, Math.min(100, Number(detail.percent || 0)));
            const message = `${detail.stageName || "处理中"} · ${detail.message || "-"}`;
            setImportPercent(percent);
            setImportStageText(message);
            setPreprocessMeta(`导入进行中 · ${message} · ${percent}%`);
            setImportStageTimings((prev) => {
              const stageKey = `${detail.stageKey || ""}`.trim();
              const nextItem = {
                stageKey: stageKey || `stage_${prev.length + 1}`,
                stageName: `${detail.stageName || "处理中"}`,
                elapsedMs: Number(detail.elapsedMs || 0),
                percent,
                chunkIndex: Number(detail.chunkIndex || 0),
                chunkTotal: Number(detail.chunkTotal || 0),
                message: `${detail.message || ""}`,
              };
              const existIndex = prev.findIndex((item) => item.stageKey === nextItem.stageKey);
              if (existIndex >= 0) {
                const next = [...prev];
                next[existIndex] = {
                  ...next[existIndex],
                  ...nextItem,
                };
                return next;
              }
              return [...prev, nextItem];
            });
          }
          if (event.event === "draft") {
            const detail = event.data || {};
            const sceneIndex = Number(detail.sceneIndex || 0);
            applyDraftUpdate(sceneIndex, {
              sceneIndex,
              sceneTitle: detail.sceneTitle || `场景${sceneIndex + 1}`,
              sceneId: detail.sceneId || null,
              status: detail.status || "DRAFT",
              confidenceScore: Number(detail.confidenceScore || 0),
              lowConfidence: Boolean(detail.lowConfidence),
              warnings: Array.isArray(detail.warnings) ? detail.warnings : [],
            });
          }
        },
      });
      setImportElapsedMs(Number(response?.totalElapsedMs || (Date.now() - start)));
      const normalizedTaskId = `${response?.importBatchId || ""}`.trim();
      if (normalizedTaskId) {
        persistTaskId(normalizedTaskId);
      }
      applyPreprocessPayload(response);
      setImportTaskStatus("QUALITY_REVIEWING");
      loadRecentImportTasks();
    } catch (err) {
      setError(err.message || "导入失败");
      setImportStageText("导入失败");
      setImportTaskStatus("FAILED");
      setActiveStep(1);
    } finally {
      setLoading(false);
    }
  }

  async function confirmQualityStep() {
    if (preprocessScenes.length === 0) {
      return;
    }
    try {
      if (importBatchId) {
        const task = await apiRequest(`/import/tasks/${encodeURIComponent(importBatchId)}/quality-confirm`, {
          method: "POST",
          token,
        });
        setImportTaskStatus(`${task?.status || "SCENE_REVIEWING"}`.toUpperCase());
        setActiveStep(resolveStepByTask(task));
      } else {
        setActiveStep(3);
      }
      setQualityConfirmed(true);
    } catch (err) {
      setError(err.message || "质检确认失败");
    }
  }

  async function confirmCompareStep() {
    const hasCurrentScene = preprocessScenes.length > 0
      && selectedSceneIndex >= 0
      && selectedSceneIndex < preprocessScenes.length;
    if (!hasCurrentScene) {
      return;
    }
    try {
      if (importBatchId) {
        const task = await apiRequest(`/import/tasks/${encodeURIComponent(importBatchId)}/compare-confirm`, {
          method: "POST",
          token,
        });
        setImportTaskStatus(`${task?.status || "PUBLISHING"}`.toUpperCase());
        setActiveStep(resolveStepByTask(task));
      } else {
        setActiveStep(4);
      }
      setCompareConfirmed(true);
    } catch (err) {
      setError(err.message || "对照确认失败");
    }
  }

  async function completeImportTask() {
    if (!importBatchId) {
      return;
    }
    try {
      const task = await apiRequest(`/import/tasks/${encodeURIComponent(importBatchId)}/complete`, {
        method: "POST",
        token,
      });
      setImportTaskStatus(`${task?.status || "COMPLETED"}`.toUpperCase());
      persistTaskId("");
      loadRecentImportTasks();
    } catch (_) {
      // ignore completion sync failure
    }
  }

  async function createDraft() {
    try {
      validateSelectedDomainOrThrow();
    } catch (err) {
      setError(err.message || "请先选择业务领域");
      return null;
    }
    setSaving(true);
    setError("");
    try {
      const response = await apiRequest("/scenes", {
        method: "POST",
        token,
        body: {
          sceneTitle: form.sceneTitle.trim() || "未命名场景",
          domainId: form.domainId ? Number(form.domainId) : null,
          domain: "",
          rawInput: rawText,
          operator: "",
        },
      });
      const createdId = response?.id || null;
      setSceneId(createdId);
      setSceneStatus(response?.status || "DRAFT");
      setPreprocessMeta(`已创建草稿 #${createdId || "-"}`);
      return createdId;
    } catch (err) {
      setError(err.message || "创建草稿失败");
      return null;
    } finally {
      setSaving(false);
    }
  }

  async function persistScenePayload(currentSceneId) {
    validateSelectedDomainOrThrow();
    const { payload, normalizedForm, notices } = normalizeScenePayload();
    setForm(normalizedForm);
    setSceneForms((prev) => ({ ...prev, [selectedSceneIndex]: normalizedForm }));
    const response = await apiRequest(`/scenes/${currentSceneId}`, {
      method: "PUT",
      token,
      body: payload,
    });
    return {
      response,
      notices,
    };
  }

  async function saveDraft() {
    const currentDraft = sceneDrafts[selectedSceneIndex] || null;
    let currentSceneId = currentDraft?.sceneId || sceneId;
    if (!currentSceneId) {
      currentSceneId = await createDraft();
      if (!currentSceneId) {
        return;
      }
      applyDraftUpdate(selectedSceneIndex, {
        sceneId: currentSceneId,
        status: "DRAFT",
      });
    }
    setSaving(true);
    setError("");
    try {
      const { response, notices } = await persistScenePayload(currentSceneId);
      setSceneId(currentSceneId);
      setSceneStatus(response?.status || "DRAFT");
      applyDraftUpdate(selectedSceneIndex, {
        sceneId: currentSceneId,
        status: response?.status || "DRAFT",
      });
      setHasUnsavedChanges(false);
      setPreprocessMeta(`草稿已保存 #${currentSceneId}${notices.length > 0 ? ` · ${notices.join("；")}` : ""}`);
      loadMinimumUnitCheck(currentSceneId);
      loadRecentImportTasks();
      focusNextProcessable(selectedSceneIndex, sceneDrafts);
    } catch (err) {
      setError(err.message || "保存草稿失败");
    } finally {
      setSaving(false);
    }
  }

  async function publish() {
    const currentDraft = sceneDrafts[selectedSceneIndex] || null;
    const currentSceneId = currentDraft?.sceneId || sceneId;
    if (!currentSceneId) {
      setError("请先保存当前场景草稿");
      return;
    }
    if (currentDraft?.lowConfidence && !lowConfidenceAckMap[selectedSceneIndex]) {
      const confirmed = window.confirm("当前场景为低置信度，确认已人工复核后再发布？");
      if (!confirmed) {
        return;
      }
      setLowConfidenceAckMap((prev) => ({ ...prev, [selectedSceneIndex]: true }));
    }
    setSaving(true);
    setError("");
    try {
      const persistResult = await persistScenePayload(currentSceneId);
      const unitCheck = await loadMinimumUnitCheck(currentSceneId);
      if (unitCheck && !unitCheck.publishReady) {
        const failed = Array.isArray(unitCheck.items)
          ? unitCheck.items.filter((item) => !item?.passed).map((item) => item?.name || item?.key).filter(Boolean)
          : [];
        throw new Error(`最小单元校验未通过：${failed.join("、") || "请补全必填结构后再发布"}`);
      }
      const response = await apiRequest(`/scenes/${currentSceneId}/publish`, {
        method: "POST",
        token,
        body: {
          verifiedAt: new Date(publishVerifiedAt).toISOString(),
          changeSummary: publishSummary.trim(),
          operator: "",
        },
      });
      setSceneStatus(response?.status || "PUBLISHED");
      setSceneId(currentSceneId);
      let nextDrafts = [];
      setSceneDrafts((prev) => {
        nextDrafts = prev.map((item, index) => (
          index === selectedSceneIndex ? { ...item, status: response?.status || "PUBLISHED" } : item
        ));
        return nextDrafts;
      });
      setHasUnsavedChanges(false);
      setPreprocessMeta(`已发布场景 #${currentSceneId}${persistResult.notices.length > 0 ? ` · ${persistResult.notices.join("；")}` : ""}`);
      setImportTaskStatus("PUBLISHING");
      const allProcessed = nextDrafts.length > 0 && nextDrafts.every((item) => {
        const status = `${item?.status || ""}`.toUpperCase();
        return status === "PUBLISHED" || status === "DISCARDED";
      });
      if (allProcessed) {
        await completeImportTask();
        setShowImportDone(true);
      } else {
        focusNextProcessable(selectedSceneIndex, nextDrafts);
      }
      loadRecentImportTasks();
    } catch (err) {
      setError(err.message || "发布失败");
    } finally {
      setSaving(false);
    }
  }

  async function discardCurrentScene() {
    const currentDraft = sceneDrafts[selectedSceneIndex] || null;
    const currentSceneId = currentDraft?.sceneId || sceneId;
    if (!currentSceneId) {
      setError("当前场景缺少草稿ID，无法弃用。");
      return;
    }
    const confirmed = window.confirm("确认弃用当前场景？弃用后将不再进入默认检索列表。");
    if (!confirmed) {
      return;
    }
    setSaving(true);
    setError("");
    try {
      const response = await apiRequest(`/scenes/${currentSceneId}/discard`, {
        method: "POST",
        token,
      });
      setSceneStatus(response?.status || "DISCARDED");
      let nextDrafts = [];
      setSceneDrafts((prev) => {
        nextDrafts = prev.map((item, index) => (
          index === selectedSceneIndex ? { ...item, status: response?.status || "DISCARDED" } : item
        ));
        return nextDrafts;
      });
      setHasUnsavedChanges(false);
      setPreprocessMeta(`已弃用场景 #${currentSceneId}`);
      setImportTaskStatus("PUBLISHING");
      const allProcessed = nextDrafts.length > 0 && nextDrafts.every((item) => {
        const status = `${item?.status || ""}`.toUpperCase();
        return status === "PUBLISHED" || status === "DISCARDED";
      });
      if (allProcessed) {
        await completeImportTask();
        setShowImportDone(true);
      } else {
        focusNextProcessable(selectedSceneIndex, nextDrafts);
      }
      loadRecentImportTasks();
    } catch (err) {
      setError(err.message || "弃用失败");
    } finally {
      setSaving(false);
    }
  }

  const isImportPreset = preset === "import";
  const currentDraft = useMemo(() => sceneDrafts[selectedSceneIndex] || null, [sceneDrafts, selectedSceneIndex]);
  const selectedScene = useMemo(() => preprocessScenes[selectedSceneIndex] || null, [preprocessScenes, selectedSceneIndex]);

  const publishReady = useMemo(() => {
    const selectedDomainId = Number(form.domainId || 0);
    const domainValid = selectedDomainId > 0 && domains.some((item) => Number(item?.id || 0) === selectedDomainId);
    return Boolean(currentDraft?.sceneId || sceneId)
      && Boolean(publishSummary.trim())
      && Boolean(publishVerifiedAt)
      && Boolean(form.sceneTitle.trim())
      && domainValid;
  }, [currentDraft?.sceneId, sceneId, publishSummary, publishVerifiedAt, form.sceneTitle, form.domainId, domains]);
  const saveReady = useMemo(() => {
    const selectedDomainId = Number(form.domainId || 0);
    return selectedDomainId > 0 && domains.some((item) => Number(item?.id || 0) === selectedDomainId);
  }, [form.domainId, domains]);

  const queueItems = useMemo(
    () => preprocessScenes.map((scene, index) => {
      const draft = sceneDrafts[index] || {};
      const confidence = Number(draft.confidenceScore ?? scene?.quality?.confidence ?? 0);
      return {
        index,
        sceneTitle: `${draft.sceneTitle || scene?.scene_title || `场景${index + 1}`}`,
        sceneId: draft.sceneId || null,
        status: `${draft.status || "DRAFT"}`.toUpperCase(),
        confidenceScore: confidence,
        lowConfidence: typeof draft.lowConfidence === "boolean" ? draft.lowConfidence : confidence < 0.7,
        warnings: Array.isArray(draft.warnings) ? draft.warnings : [],
      };
    }),
    [preprocessScenes, sceneDrafts],
  );
  const filteredQueueItems = useMemo(() => {
    const keyword = `${sceneQueueKeyword || ""}`.trim().toLowerCase();
    if (!keyword) {
      return queueItems;
    }
    return queueItems.filter((item) => item.sceneTitle.toLowerCase().includes(keyword));
  }, [queueItems, sceneQueueKeyword]);
  const queueStats = useMemo(() => {
    let draftCount = 0;
    let publishedCount = 0;
    let discardedCount = 0;
    let lowConfidenceCount = 0;
    queueItems.forEach((item) => {
      if (item.status === "PUBLISHED") {
        publishedCount += 1;
      } else if (item.status === "DISCARDED") {
        discardedCount += 1;
      } else {
        draftCount += 1;
      }
      if (item.lowConfidence) {
        lowConfidenceCount += 1;
      }
    });
    return {
      draftCount,
      publishedCount,
      discardedCount,
      lowConfidenceCount,
    };
  }, [queueItems]);
  const domainOptions = useMemo(() => [...domains].sort((a, b) => {
    const sortA = Number(a?.sortOrder ?? 0);
    const sortB = Number(b?.sortOrder ?? 0);
    if (sortA !== sortB) {
      return sortA - sortB;
    }
    return `${a?.domainName || ""}`.localeCompare(`${b?.domainName || ""}`, "zh-CN");
  }), [domains]);
  const domainSelectDisabled = domainLoading || domainOptions.length === 0;
  const processedSceneCount = useMemo(
    () => queueStats.publishedCount + queueStats.discardedCount,
    [queueStats.publishedCount, queueStats.discardedCount],
  );
  const sourceSummaryText = useMemo(
    () => buildStep1Summary(sourceTypeConfig.label, rawText),
    [sourceTypeConfig.label, rawText],
  );
  const qualitySummaryText = useMemo(() => {
    const confidence = Number(currentDraft?.confidenceScore ?? selectedScene?.quality?.confidence ?? 0);
    return buildStep2Summary(preprocessScenes.length, confidence);
  }, [currentDraft?.confidenceScore, selectedScene, preprocessScenes.length]);
  const compareSummaryText = useMemo(() => {
    const count = compareConfirmed ? preprocessScenes.length : processedSceneCount;
    return buildStep3Summary(count);
  }, [compareConfirmed, preprocessScenes.length, processedSceneCount]);
  const publishSummaryText = useMemo(() => {
    if (!currentDraft) {
      return "✓ 04 场景编辑与发布 | 待发布 0 个";
    }
    const statusText = currentDraft.status === "PUBLISHED" ? "已发布" : (currentDraft.status === "DISCARDED" ? "已弃用" : "草稿");
    return `✓ 04 场景编辑与发布 | 待发布 ${queueStats.draftCount} 个（当前${currentDraft.sceneId ? `#${currentDraft.sceneId}` : "未入库"}·${statusText}）`;
  }, [currentDraft, queueStats.draftCount]);
  const step1HeaderText = activeStep === 1
    ? "支持粘贴与文件导入，导入后将预创建全部场景草稿"
    : sourceSummaryText;
  const step2HeaderText = activeStep === 2
    ? "基于抽取结果快速判断可发布性"
    : qualitySummaryText;
  const step3HeaderText = activeStep === 3
    ? "在场景队列中逐个核对证据与结构化结果"
    : compareSummaryText;
  const step4HeaderText = activeStep === 4
    ? "完成字段编辑后保存、发布或弃用当前场景"
    : publishSummaryText;
  const getStepState = (stepNo) => resolveAccordionStepState(stepNo, activeStep);
  const canEditStep = (stepNo) => getStepState(stepNo) === "collapsed";
  function bindStepCardRef(stepNo) {
    return (node) => {
      if (node) {
        stepCardRefMap.current.set(stepNo, node);
      } else {
        stepCardRefMap.current.delete(stepNo);
      }
    };
  }
  const openStep = async (stepNo) => {
    if (stepNo <= 0 || stepNo > 4) {
      return;
    }
    if (getStepState(stepNo) === "locked") {
      return;
    }
    if (stepNo === 1 && activeStep > 1 && (preprocessScenes.length > 0 || qualityConfirmed || compareConfirmed)) {
      const confirmed = window.confirm("修改源文件将清空当前抽取与对照结果，是否继续？");
      if (!confirmed) {
        return;
      }
      if (importBatchId) {
        try {
          await apiRequest(`/import/tasks/${encodeURIComponent(importBatchId)}/rewind/1`, {
            method: "POST",
            token,
          });
          setImportTaskStatus("RUNNING");
        } catch (err) {
          setError(err.message || "回退任务状态失败");
          return;
        }
      }
      setQualityConfirmed(false);
      setCompareConfirmed(false);
      setPreprocessJson("");
      setPreprocessWarnings([]);
      setPreprocessScenes([]);
      setSceneDrafts([]);
      setSceneForms({});
      setBatchUnmappedText("");
      setSelectedSceneIndex(0);
      setSceneId(null);
      setSceneStatus("DRAFT");
      setForm(EMPTY_FORM);
      setImportStageTimings([]);
      setImportPercent(0);
      setImportStageText("待导入");
      setImportElapsedMs(0);
      setShowImportDone(false);
    }
    setActiveStep(stepNo);
  };
  const qualityVerdict = useMemo(() => {
    if (!selectedScene) {
      return qualityMsg;
    }
    const currentNo = selectedSceneIndex + 1;
    const confidence = Number(currentDraft?.confidenceScore ?? selectedScene?.quality?.confidence ?? 0);
    const level = toConfidenceLevelZh(confidence);
    return `共识别 ${preprocessScenes.length} 个场景，当前应用第 ${currentNo} 个；置信度 ${(confidence * 100).toFixed(0)}%（${level}）`;
  }, [selectedScene, qualityMsg, selectedSceneIndex, currentDraft?.confidenceScore, preprocessScenes.length]);
  const stageTimingRows = useMemo(
    () => [...importStageTimings].sort((a, b) => {
      const aOrder = IMPORT_STAGE_ORDER[`${a?.stageKey || ""}`] || 99;
      const bOrder = IMPORT_STAGE_ORDER[`${b?.stageKey || ""}`] || 99;
      if (aOrder !== bOrder) {
        return aOrder - bOrder;
      }
      return Number(a?.chunkIndex || 0) - Number(b?.chunkIndex || 0);
    }),
    [importStageTimings],
  );
  const totalElapsed = useMemo(() => {
    if (importElapsedMs > 0) {
      return importElapsedMs;
    }
    return stageTimingRows.reduce((sum, item) => sum + Number(item?.elapsedMs || 0), 0);
  }, [importElapsedMs, stageTimingRows]);
  const rawLines = useMemo(() => `${rawText || ""}`.split(/\r?\n/), [rawText]);
  const highlightedLineSet = useMemo(
    () => collectHighlightLines(rawText, selectedScene),
    [rawText, selectedScene],
  );
  const selectedSceneVariants = useMemo(
    () => readArray(selectedScene?.sql_variants).map((variant) => ({
      ...variant,
      source_spans: normalizeVariantSpans(rawLines, variant),
    })),
    [selectedScene, rawLines],
  );
  const sqlVariantRows = useMemo(
    () => parseJsonArrayOrEmpty(form.sqlVariantsJson).map((item) => ({
      variant_name: `${item?.variant_name || item?.variantName || ""}`,
      applicable_period: `${item?.applicable_period || item?.applicablePeriod || ""}`,
      sql_text: `${item?.sql_text || item?.sqlText || ""}`,
      source_tables_text: toSourceTablesText(item?.source_tables || item?.sourceTables),
      notes: `${item?.notes || ""}`,
      period_state: parseApplicablePeriod(item?.applicable_period || item?.applicablePeriod),
    })),
    [form.sqlVariantsJson],
  );
  const codeMappingRows = useMemo(
    () => parseJsonArrayOrEmpty(form.codeMappingsJson).map((item) => ({
      code: `${item?.code || ""}`,
      description: `${item?.description || ""}`,
      mappings: readArray(item?.mappings).map((mapping) => ({
        value_code: `${mapping?.value_code ?? mapping?.valueCode ?? ""}`,
        value_name: `${mapping?.value_name ?? mapping?.valueName ?? mapping?.label ?? ""}`,
      })),
    })),
    [form.codeMappingsJson],
  );
  const activeVariantLineSet = useMemo(() => {
    if (activeVariantIndex < 0 || activeVariantIndex >= selectedSceneVariants.length) {
      return new Set();
    }
    const lineSet = new Set();
    readArray(selectedSceneVariants[activeVariantIndex]?.source_spans).forEach((span) => {
      const start = Number(span?.start_line || 0);
      const end = Number(span?.end_line || 0);
      if (start <= 0 || end <= 0) {
        return;
      }
      for (let lineNo = Math.min(start, end); lineNo <= Math.max(start, end); lineNo += 1) {
        lineSet.add(lineNo);
      }
    });
    return lineSet;
  }, [activeVariantIndex, selectedSceneVariants]);

  function focusVariant(index) {
    setActiveVariantIndex(index);
    const spans = readArray(selectedSceneVariants[index]?.source_spans);
    if (spans.length === 0) {
      return;
    }
    const firstLine = Number(spans[0]?.start_line || 0);
    const target = lineRefMap.current.get(firstLine);
    if (target) {
      target.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  }

  return (
    <div className="layout">
      {showImportDone ? (
        <div className="import-done-overlay" role="dialog" aria-modal="true" aria-labelledby="importDoneTitle">
          <div className="import-done-card">
            <h3 id="importDoneTitle">本批次场景已处理完成</h3>
            <p>已完成发布/弃用处理。下一步可进入数据地图继续查看和复用场景。</p>
            <div className="actions">
              <button className="btn btn-primary" type="button" onClick={() => navigate("/assets/scenes")}>
                去数据地图查看
              </button>
              <button className="btn btn-ghost" type="button" onClick={() => setShowImportDone(false)}>
                继续留在当前页
              </button>
            </div>
          </div>
        </div>
      ) : null}

      <AccordionStepCard
        ref={bindStepCardRef(1)}
        stepNo={1}
        title="01 导入并生成草稿"
        state={getStepState(1)}
        summaryText={step1HeaderText}
        showEdit={canEditStep(1)}
        onEdit={() => openStep(1)}
      >
            <div className="row form-row">
              <label id="sourceTypeLabel">导入内容类型</label>
              <div className="import-type-picker" role="radiogroup" aria-labelledby="sourceTypeLabel">
                {SOURCE_TYPE_OPTIONS.map((item) => {
                  const active = item.value === sourceType;
                  return (
                    <button
                      key={item.value}
                      type="button"
                      role="radio"
                      aria-checked={active}
                      className={`import-type-chip ${active ? "is-active" : ""} ${item.uploadEnabled ? "" : "is-future"}`}
                      onClick={() => handleSourceTypeChange(item.value)}
                    >
                      <strong>{item.label}</strong>
                      <small>{item.requiresFile ? "上传文件" : "粘贴内容"}</small>
                    </button>
                  );
                })}
              </div>
            </div>
            <div className="row form-row">
              <label id="preprocessModeLabel">导入策略</label>
              <div className="import-preprocess-mode" role="radiogroup" aria-labelledby="preprocessModeLabel">
                {PREPROCESS_MODE_OPTIONS.map((item) => {
                  const active = item.value === preprocessMode;
                  return (
                    <button
                      key={item.value}
                      type="button"
                      role="radio"
                      aria-checked={active}
                      className={`import-mode-chip ${active ? "is-active" : ""}`}
                      onClick={() => handlePreprocessModeChange(item.value)}
                    >
                      <strong>{item.label}</strong>
                      <small>{item.hint}</small>
                    </button>
                  );
                })}
              </div>
            </div>
            <div className="import-step1-grid">
              <div className="import-input-panel">
                {isUploadSourceType ? (
                  <div className="row form-row">
                    <label htmlFor="sourceUpload">上传文件</label>
                    <input
                      id="sourceUpload"
                      name="sourceUpload"
                      autoComplete="off"
                      type="file"
                      accept={sourceTypeConfig.uploadAccept || undefined}
                      onChange={handleFileUpload}
                      ref={fileInputRef}
                      className="visually-hidden"
                      disabled={!sourceTypeConfig.uploadEnabled}
                    />
                    <div
                      className={`import-dropzone ${dropActive ? "is-dragover" : ""} ${!sourceTypeConfig.uploadEnabled ? "is-disabled" : ""}`}
                      role="button"
                      tabIndex={sourceTypeConfig.uploadEnabled ? 0 : -1}
                      aria-disabled={!sourceTypeConfig.uploadEnabled}
                      aria-label={sourceTypeConfig.uploadEnabled ? "拖拽或点击上传口径文档文件" : sourceTypeConfig.modeHint}
                      onClick={handleDropZoneClick}
                      onKeyDown={(event) => {
                        if (!sourceTypeConfig.uploadEnabled) {
                          return;
                        }
                        if (event.key === "Enter" || event.key === " ") {
                          event.preventDefault();
                          handleDropZoneClick();
                        }
                      }}
                      onDrop={handleDrop}
                      onDragOver={handleDragOver}
                      onDragLeave={handleDragLeave}
                    >
                      <span className="import-dropzone-icon" aria-hidden="true">☁</span>
                      <p>{sourceTypeConfig.dropzoneTitle}</p>
                      <small>{sourceTypeConfig.dropzoneSuffix}</small>
                    </div>
                  </div>
                ) : null}
                {isPasteSourceType ? (
                  <>
                    <label className="textarea-label" htmlFor="rawText">粘贴内容</label>
                    <UiTextarea
                      id="rawText"
                      name="rawText"
                      autoComplete="off"
                      className="import-raw-input-wrap"
                      textareaClassName="import-raw-input"
                      value={rawText}
                      onChange={(event) => {
                        setRawText(event.target.value);
                        setHasUnsavedChanges(true);
                      }}
                      placeholder={sourceTypeConfig.placeholder}
                    />
                  </>
                ) : null}
                <p className="field-hint import-linkage-note">{sourceTypeConfig.modeHint}</p>
                {fileMeta ? (
                  <p className="subtle-note">已加载文件：{fileMeta.name} · {fileMeta.size} 字节 · {fileMeta.charCount} 字符</p>
                ) : null}
                {!sourceFileReady ? (
                  <p className="subtle-note">当前模式要求先上传文件后才能导入。</p>
                ) : null}
              </div>
              <aside className="import-standard-guide">
                <p className="import-guide-title">标准文档格式</p>
                {importGuideGroups.map((group) => (
                  <section key={group.key} className="import-guide-section">
                    <p className="import-guide-subtitle">{group.title}</p>
                    <ul>
                      {group.items.map((item) => (
                        <li key={item.key} className={item.matched ? "is-hit" : ""}>
                          <strong>
                            <span className="import-guide-icon" aria-hidden="true">
                              {item.matched ? <CheckCircle2 size={14} strokeWidth={2.1} /> : <Circle size={14} strokeWidth={1.9} />}
                            </span>
                            <span>{item.label}</span>
                          </strong>
                          <span>{item.hint}</span>
                        </li>
                      ))}
                    </ul>
                  </section>
                ))}
              </aside>
            </div>
            <div className="actions">
              <button className="btn btn-primary" type="button" onClick={runPreprocess} disabled={loading || !sourceFileReady}>
                {loading ? "处理中…" : "导入并生成草稿"}
              </button>
              <button className="btn btn-ghost" type="button" onClick={fillBestPracticeSample} disabled={loading}>
                填入最佳实践样例
              </button>
              <button
                className="btn btn-ghost"
                type="button"
                onClick={() => {
                  setRawText("");
                  setSourceName("");
                  setFileMeta(null);
                  setError("");
                  persistTaskId("");
                  setImportTaskStatus("");
                  setQualityConfirmed(false);
                  setCompareConfirmed(false);
                  setPreprocessMeta(sourceTypeConfig.modeHint);
                  setHasUnsavedChanges(true);
                  setBatchUnmappedText("");
                }}
              >
                清空输入
              </button>
            </div>
            <p className="meta" role="status" aria-live="polite">{preprocessMeta}</p>
            {preprocessWarnings.length > 0 ? (
              <p className="subtle-note" role="status" aria-live="polite">抽取提示：{preprocessWarnings.join("；")}</p>
            ) : null}
            {error ? <p className="danger-text" role="status" aria-live="polite">{error}</p> : null}

            <div className="import-stage-strip" role="status" aria-live="polite">
              <div className="import-stage-head">
                <strong>导入执行明细</strong>
                <span>
                  总耗时 {formatElapsedMs(totalElapsed)}
                  {importBatchId ? ` · 批次 ${importBatchId.slice(0, 8)}` : ""}
                  {importTaskStatus ? ` · 状态 ${importTaskStatus}` : ""}
                </span>
              </div>
              <div className="import-stage-progress">
                <i style={{ width: `${Math.max(0, Math.min(100, importPercent))}%` }} />
              </div>
              <p className="subtle-note">当前阶段：{importStageText || "待导入"}</p>
              {stageTimingRows.length > 0 ? (
                <ul className="import-stage-list">
                  {stageTimingRows.map((stage) => (
                    <li key={`${stage.stageKey}-${stage.chunkIndex || 0}`}>
                      <span>{stage.stageName || stage.stageKey}</span>
                      <span>{formatElapsedMs(stage.elapsedMs)}</span>
                      <span>{Math.max(0, Math.min(100, Number(stage.percent || 0)))}%</span>
                    </li>
                  ))}
                </ul>
              ) : null}
            </div>
            {isImportPreset ? (
              <div className="import-task-board">
                <div className="import-task-board-head">
                  <strong>导入任务队列（草稿生命周期）</strong>
                  <button className="btn btn-ghost btn-xs" type="button" onClick={loadRecentImportTasks} disabled={taskListLoading}>
                    {taskListLoading ? "刷新中…" : "刷新队列"}
                  </button>
                </div>
                {recentImportTasks.length === 0 ? (
                  <p className="subtle-note">暂无导入任务记录，可先执行一次导入。</p>
                ) : (
                  <ul className="import-task-list">
                    {recentImportTasks.map((task) => {
                      const taskStatus = `${task?.status || ""}`.toUpperCase();
                      const draftTotal = Number(task?.draftTotal || 0);
                      const draftCount = Number(task?.draftCount || 0);
                      const publishedCount = Number(task?.publishedCount || 0);
                      const discardedCount = Number(task?.discardedCount || 0);
                      const resumable = Boolean(task?.resumable);
                      return (
                        <li key={task.taskId} className="import-task-item">
                          <div className="import-task-item-main">
                            <strong>{task.sourceName || "未命名导入任务"}</strong>
                            <p className="subtle-note">
                              批次 {`${task.taskId || ""}`.slice(0, 8)}
                              {` · 状态 ${taskStatus || "-"}`}
                              {` · 步骤 ${task.currentStep || "-"}`}
                              {` · 更新于 ${formatDateTimeLabel(task.updatedAt)}`}
                            </p>
                            <p className="subtle-note">
                              草稿总数 {draftTotal} · 待处理 {draftCount} · 已发布 {publishedCount} · 已弃用 {discardedCount}
                            </p>
                          </div>
                          <div className="import-task-item-actions">
                            <button
                              className="btn btn-ghost btn-xs"
                              type="button"
                              onClick={() => restoreImportTask(task.taskId)}
                              disabled={!task.taskId || !resumable}
                            >
                              {resumable ? "恢复处理" : "已结束"}
                            </button>
                          </div>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </div>
            ) : null}
      </AccordionStepCard>

      <AccordionStepCard
        ref={bindStepCardRef(2)}
        stepNo={2}
        title="02 抽取质量判断"
        state={getStepState(2)}
        summaryText={step2HeaderText}
        showEdit={canEditStep(2)}
        onEdit={() => openStep(2)}
      >
            <div className={`score-board ${queueItems.length === 0 ? "empty" : ""}`}>
              <div className="score-ring">
                <span>{queueItems.length === 0 ? "--" : queueItems.length}</span>
                <small>识别场景</small>
              </div>
              <div className="quality-board-main">
                <p className="verdict">{qualityVerdict}</p>
                <p className="subtle-note">
                  待处理 {queueStats.draftCount} · 已发布 {queueStats.publishedCount} · 已弃用 {queueStats.discardedCount}
                  {queueStats.lowConfidenceCount > 0 ? ` · 低置信度 ${queueStats.lowConfidenceCount}` : ""}
                </p>
              </div>
            </div>
            <details className="field-section-collapse">
              <summary>批次未识别内容</summary>
              <div className="row form-row">
                <AutoGrowTextarea
                  id="batchUnmappedText"
                  name="batchUnmappedText"
                  autoComplete="off"
                  className="mini"
                  value={batchUnmappedText}
                  onChange={(event) => setBatchUnmappedText(event.target.value)}
                  placeholder="当前批次无未识别内容。"
                  minRows={4}
                  maxRows={10}
                />
                <p className="field-hint">该内容仅用于当前页面临时校对，不绑定单个场景，也不会随保存/发布写入。</p>
              </div>
            </details>
            <div className="actions">
              <button
                className="btn btn-primary"
                type="button"
                disabled={queueItems.length === 0}
                onClick={confirmQualityStep}
              >
                确认质检，进入对照
              </button>
            </div>
      </AccordionStepCard>

      <AccordionStepCard
        ref={bindStepCardRef(3)}
        stepNo={3}
        title="03 抽取结果与原文对照"
        state={getStepState(3)}
        summaryText={step3HeaderText}
        showEdit={canEditStep(3)}
        onEdit={() => openStep(3)}
      >
            <div className="import-master-detail">
              <div className="import-scene-queue">
                <div className="panel-head">
                  <h2>场景队列</h2>
                  <p>支持批量处理与快速切换</p>
                </div>
                <div className="row form-row">
                  <label htmlFor="sceneQueueKeyword">筛选场景</label>
                  <input
                    id="sceneQueueKeyword"
                    name="sceneQueueKeyword"
                    autoComplete="off"
                    value={sceneQueueKeyword}
                    onChange={(event) => setSceneQueueKeyword(event.target.value)}
                    placeholder="按场景标题筛选"
                  />
                </div>
                <div className="scene-queue-list">
                  {filteredQueueItems.length === 0 ? (
                    <p className="muted">暂无场景</p>
                  ) : filteredQueueItems.map((item) => (
                    <button
                      type="button"
                      key={`scene-queue-${item.index}`}
                      className={`scene-queue-item ${item.index === selectedSceneIndex ? "is-active" : ""}`}
                      onClick={() => focusScene(item.index)}
                    >
                      <div className="scene-queue-item-head">
                        <strong>{item.index + 1}. {item.sceneTitle}</strong>
                        <span className={`scene-status-badge is-${item.status.toLowerCase()}`}>
                          {item.status === "PUBLISHED" ? "已发布" : (item.status === "DISCARDED" ? "已弃用" : "草稿")}
                        </span>
                      </div>
                      <p className="subtle-note">
                        {item.sceneId ? `#${item.sceneId}` : "待入库"}
                        {item.lowConfidence ? " · 低置信度" : ""}
                        {item.confidenceScore > 0 ? ` · ${(item.confidenceScore * 100).toFixed(0)}%` : ""}
                      </p>
                    </button>
                  ))}
                </div>
              </div>

              <div className="import-scene-detail">
                <div className="result-summary">
                  <div className="summary-item">
                    <span>识别场景</span>
                    <strong>{preprocessScenes.length}</strong>
                  </div>
                  <div className="summary-item">
                    <span>当前场景</span>
                    <strong>{selectedScene ? (selectedScene.scene_title || "未命名") : "--"}</strong>
                  </div>
                  <div className="summary-item">
                    <span>取数方案</span>
                    <strong>{selectedSceneVariants.length}</strong>
                  </div>
                  <div className="summary-item">
                    <span>高亮行数</span>
                    <strong>{highlightedLineSet.size}</strong>
                  </div>
                </div>

                <div className="compare-wrap">
                  <div className="compare-panel">
                    <h3>原文定位预览</h3>
                    <div className="compare-raw compare-raw-lines">
                      {rawLines.length === 0 ? (
                        <p className="muted">暂无原文</p>
                      ) : (
                        <ol className="compare-lines">
                          {rawLines.map((line, idx) => {
                            const lineNo = idx + 1;
                            const isHit = highlightedLineSet.has(lineNo);
                            const isVariantHit = activeVariantLineSet.has(lineNo);
                            return (
                              <li
                                key={lineNo}
                                ref={(node) => {
                                  if (node) {
                                    lineRefMap.current.set(lineNo, node);
                                  } else {
                                    lineRefMap.current.delete(lineNo);
                                  }
                                }}
                                className={`compare-line ${isHit ? "is-hit" : ""} ${isVariantHit ? "is-variant-hit" : ""}`}
                              >
                                <span className="compare-line-no">{lineNo}</span>
                                <span className="compare-line-text">{line || " "}</span>
                              </li>
                            );
                          })}
                        </ol>
                      )}
                    </div>
                    <p className="subtle-note">高亮来自场景证据行和 SQL 来源行段；若无行段则按 SQL 文本自动定位。</p>
                  </div>
                  <div className="compare-panel">
                    <h3>结构化预览</h3>
                    {selectedScene ? (
                      <div className="compare-kv">
                        <div className="compare-kv-item"><strong>场景标题</strong><span>{selectedScene.scene_title || "-"}</span></div>
                        <div className="compare-kv-item"><strong>场景描述</strong><span>{selectedScene.scene_description || "-"}</span></div>
                        <div className="compare-kv-item"><strong>输入参数数</strong><span>{readArray(selectedScene?.inputs?.params).length}</span></div>
                        <div className="compare-kv-item"><strong>输出字段数</strong><span>{readArray(selectedScene?.outputs?.fields).length}</span></div>
                        <div className="compare-kv-item"><strong>证据行</strong><span>{readPositiveIntList(selectedScene?.source_evidence_lines || selectedScene?.evidence_lines).join("，") || "未提供"}</span></div>
                      </div>
                    ) : (
                      <p className="muted">尚未产生可对照的结构化场景。</p>
                    )}
                    <div className="sql-variant-list">
                      {selectedSceneVariants.map((variant, index) => (
                        <article
                          className={`variant-card ${activeVariantIndex === index ? "is-active" : ""}`}
                          key={`${variant.variant_name || "variant"}-${index}`}
                          role="button"
                          tabIndex={0}
                          onClick={() => focusVariant(index)}
                          onKeyDown={(event) => {
                            if (event.key === "Enter" || event.key === " ") {
                              event.preventDefault();
                              focusVariant(index);
                            }
                          }}
                        >
                          <div className="variant-head">
                            <h4>{variant.variant_name || `取数方案${index + 1}`}</h4>
                            <span className="subtle-note">行段：{formatSpanText(variant.source_spans)}</span>
                          </div>
                          <p className="subtle-note">
                            适用时段：{variant.applicable_period || "未标注"} · 来源表：{readArray(variant.source_tables).join("，") || "未标注"}
                          </p>
                        </article>
                      ))}
                    </div>
                  </div>
                </div>
                <details className="debug-panel">
                  <summary>查看完整结构化 JSON</summary>
                  <pre className="json-output">{preprocessJson || "{ }"}</pre>
                </details>
              </div>
            </div>
            <div className="actions">
              <button
                className="btn btn-primary"
                type="button"
                disabled={!qualityConfirmed || !selectedScene}
                onClick={confirmCompareStep}
              >
                确认对照，进入发布
              </button>
            </div>
      </AccordionStepCard>

      <AccordionStepCard
        ref={bindStepCardRef(4)}
        stepNo={4}
        title="04 场景编辑与发布"
        state={getStepState(4)}
        summaryText={step4HeaderText}
        showEdit={canEditStep(4)}
        onEdit={() => openStep(4)}
      >
            <div className="row form-row">
              <label htmlFor="sceneTitle">场景标题</label>
              <input
                id="sceneTitle"
                name="sceneTitle"
                autoComplete="off"
                value={form.sceneTitle}
                onChange={(event) => updateFormField("sceneTitle", event.target.value)}
              />
            </div>
          <div className="row form-row">
            <label htmlFor="sceneDomain">业务领域</label>
            <div className="domain-inline-row">
              <select
                id="sceneDomain"
                name="sceneDomain"
                autoComplete="off"
                value={form.domainId}
                onChange={(event) => updateFormField("domainId", event.target.value)}
                disabled={domainSelectDisabled}
                className="domain-select-compact"
              >
                <option value="">{domainLoading ? "业务领域加载中…" : "请选择业务领域"}</option>
                {domainOptions.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.domainName || item.domainCode || `业务领域#${item.id}`}
                  </option>
                ))}
              </select>
              <div className="inline-actions">
                <button className="btn btn-ghost" type="button" onClick={handleReloadDomains} disabled={domainLoading}>
                  {domainLoading ? "重载中…" : "重载业务领域"}
                </button>
                {domainOptions.length === 0 ? (
                  <button className="btn btn-ghost" type="button" onClick={quickCreateDefaultDomain} disabled={domainLoading}>
                    快速创建默认业务领域
                  </button>
                ) : null}
                {domainOptions.length === 0 ? (
                  <button className="btn btn-ghost" type="button" onClick={goDomainManagement}>
                    去业务领域管理
                  </button>
                ) : null}
              </div>
            </div>
            {domainLoadError ? <p className="field-hint">{domainLoadError}</p> : null}
            {!domainLoadError && !saveReady ? <p className="field-hint">保存和发布前请先选择业务领域。</p> : null}
          </div>

          <label className="textarea-label" htmlFor="sceneDescription">场景描述</label>
          <AutoGrowTextarea
            id="sceneDescription"
            name="sceneDescription"
            autoComplete="off"
            className="mini"
            value={form.sceneDescription}
            onChange={(event) => updateFormField("sceneDescription", event.target.value)}
            minRows={5}
            maxRows={16}
          />
          <label className="textarea-label" htmlFor="caliberDefinition">口径定义</label>
          <AutoGrowTextarea
            id="caliberDefinition"
            name="caliberDefinition"
            autoComplete="off"
            className="mini"
            value={form.caliberDefinition}
            onChange={(event) => updateFormField("caliberDefinition", event.target.value)}
            minRows={4}
            maxRows={14}
          />
          <details className="field-section-collapse">
            <summary>输入参数（默认折叠）</summary>
            <AutoGrowTextarea
              id="inputsJson"
              name="inputsJson"
              autoComplete="off"
              className="mini"
              value={form.inputsJson}
              onChange={(event) => updateFormField("inputsJson", event.target.value)}
              minRows={4}
              maxRows={16}
            />
          </details>
          <details className="field-section-collapse">
            <summary>输出字段（默认折叠）</summary>
            <AutoGrowTextarea
              id="outputsJson"
              name="outputsJson"
              autoComplete="off"
              className="mini"
              value={form.outputsJson}
              onChange={(event) => updateFormField("outputsJson", event.target.value)}
              minRows={4}
              maxRows={16}
            />
          </details>
          <label className="textarea-label">取数方案</label>
          <p className="field-hint">一个场景可有多个取数方案；请按适用时段和来源表选择。</p>
          <div className="json-editor-list">
            {sqlVariantRows.length === 0 ? (
              <p className="muted">暂无取数方案，请新增后补充内容。</p>
            ) : sqlVariantRows.map((variant, index) => (
              <article key={`variant-editor-${index + 1}`} className="json-editor-card">
                <div className="json-editor-head">
                  <h4>{variant.variant_name || `取数方案${index + 1}`}</h4>
                  <button
                    className="btn btn-ghost"
                    type="button"
                    onClick={() => removeSqlVariant(index)}
                  >
                    删除
                  </button>
                </div>
                <div className="json-editor-grid">
                  <div className="inline-field">
                    <label htmlFor={`variantName-${index + 1}`}>取数方案名称</label>
                    <input
                      id={`variantName-${index + 1}`}
                      name={`variantName-${index + 1}`}
                      autoComplete="off"
                      value={variant.variant_name}
                      onChange={(event) => updateSqlVariantField(index, "variant_name", event.target.value)}
                      placeholder={`取数方案${index + 1}`}
                    />
                  </div>
                  <div className="inline-field variant-period-field">
                    <label>适用时段</label>
                    <div className="date-range-inputs">
                      <input
                        id={`variantPeriodStart-${index + 1}`}
                        name={`variantPeriodStart-${index + 1}`}
                        autoComplete="off"
                        type="date"
                        value={variant.period_state.startDate}
                        onChange={(event) => updateSqlVariantPeriod(index, { startDate: event.target.value })}
                      />
                      <span className="date-range-sep">至</span>
                      <input
                        id={`variantPeriodEnd-${index + 1}`}
                        name={`variantPeriodEnd-${index + 1}`}
                        autoComplete="off"
                        type="date"
                        value={variant.period_state.endDate}
                        disabled={variant.period_state.toPresent}
                        onChange={(event) => updateSqlVariantPeriod(index, { endDate: event.target.value, toPresent: false })}
                      />
                      <label className="date-range-now">
                        <input
                          id={`variantPeriodNow-${index + 1}`}
                          name={`variantPeriodNow-${index + 1}`}
                          autoComplete="off"
                          type="checkbox"
                          checked={variant.period_state.toPresent}
                          onChange={(event) => updateSqlVariantPeriod(index, { toPresent: event.target.checked })}
                        />
                        至今
                      </label>
                    </div>
                    {!variant.period_state.parseable && variant.applicable_period ? (
                      <p className="field-hint">原始时段文本：{variant.applicable_period}（修改日期后将覆盖）</p>
                    ) : null}
                  </div>
                </div>
                <div className="inline-field">
                  <label htmlFor={`variantSources-${index + 1}`}>来源表（逗号分隔）</label>
                  <input
                    id={`variantSources-${index + 1}`}
                    name={`variantSources-${index + 1}`}
                    autoComplete="off"
                    value={variant.source_tables_text}
                    onChange={(event) => updateSqlVariantField(index, "source_tables", event.target.value)}
                    placeholder="例如：T_CUST_INFO, T_CUST_INFO_BAK"
                  />
                </div>
                <div className="inline-field">
                  <label htmlFor={`variantNotes-${index + 1}`}>备注</label>
                  <input
                    id={`variantNotes-${index + 1}`}
                    name={`variantNotes-${index + 1}`}
                    autoComplete="off"
                    value={variant.notes}
                    onChange={(event) => updateSqlVariantField(index, "notes", event.target.value)}
                    placeholder="可选：版本差异说明"
                  />
                </div>
                <div className="inline-field">
                  <label htmlFor={`variantSql-${index + 1}`}>SQL 原文</label>
                  <AutoGrowTextarea
                    id={`variantSql-${index + 1}`}
                    name={`variantSql-${index + 1}`}
                    autoComplete="off"
                    className="mini"
                    value={variant.sql_text}
                    onChange={(event) => updateSqlVariantField(index, "sql_text", event.target.value)}
                    placeholder="粘贴该取数方案 SQL 原文"
                    minRows={5}
                    maxRows={20}
                  />
                </div>
              </article>
            ))}
          </div>
          <div className="actions">
            <button className="btn btn-ghost" type="button" onClick={addSqlVariant}>
              新增取数方案
            </button>
          </div>
          <details className="debug-panel">
            <summary>查看取数方案 JSON</summary>
            <pre className="json-output">{form.sqlVariantsJson || "[]"}</pre>
          </details>
          <label className="textarea-label">码值说明</label>
          <p className="field-hint">先录入码值字段，再逐条补充取值映射。</p>
          <div className="json-editor-list">
            {codeMappingRows.length === 0 ? (
              <p className="muted">暂无码值说明，请新增后补充映射。</p>
            ) : codeMappingRows.map((group, groupIndex) => (
              <article key={`code-mapping-${groupIndex + 1}`} className="json-editor-card">
                <div className="json-editor-head">
                  <h4>{group.code || `码值组${groupIndex + 1}`}</h4>
                  <button
                    className="btn btn-ghost"
                    type="button"
                    onClick={() => removeCodeMappingGroup(groupIndex)}
                  >
                    删除组
                  </button>
                </div>
                <div className="json-editor-grid">
                  <div className="inline-field">
                    <label htmlFor={`mappingCode-${groupIndex + 1}`}>字段编码</label>
                    <input
                      id={`mappingCode-${groupIndex + 1}`}
                      name={`mappingCode-${groupIndex + 1}`}
                      autoComplete="off"
                      value={group.code}
                      onChange={(event) => updateCodeMappingField(groupIndex, "code", event.target.value)}
                      placeholder="例如：CARD_GRD_CD"
                    />
                  </div>
                  <div className="inline-field">
                    <label htmlFor={`mappingDesc-${groupIndex + 1}`}>字段说明</label>
                    <input
                      id={`mappingDesc-${groupIndex + 1}`}
                      name={`mappingDesc-${groupIndex + 1}`}
                      autoComplete="off"
                      value={group.description}
                      onChange={(event) => updateCodeMappingField(groupIndex, "description", event.target.value)}
                      placeholder="例如：客户卡等级"
                    />
                  </div>
                </div>
                <div className="mapping-list">
                  {group.mappings.length === 0 ? (
                    <p className="muted">暂无取值，请新增码值取值。</p>
                  ) : group.mappings.map((mapping, mappingIndex) => (
                    <div key={`mapping-${groupIndex + 1}-${mappingIndex + 1}`} className="mapping-row">
                      <input
                        id={`mappingValueCode-${groupIndex + 1}-${mappingIndex + 1}`}
                        name={`mappingValueCode-${groupIndex + 1}-${mappingIndex + 1}`}
                        autoComplete="off"
                        value={mapping.value_code}
                        onChange={(event) => updateCodeMappingValue(groupIndex, mappingIndex, "value_code", event.target.value)}
                        placeholder="取值编码，如 P1"
                      />
                      <input
                        id={`mappingValueName-${groupIndex + 1}-${mappingIndex + 1}`}
                        name={`mappingValueName-${groupIndex + 1}-${mappingIndex + 1}`}
                        autoComplete="off"
                        value={mapping.value_name}
                        onChange={(event) => updateCodeMappingValue(groupIndex, mappingIndex, "value_name", event.target.value)}
                        placeholder="取值名称，如 支付密码修改"
                      />
                      <button
                        className="btn btn-ghost"
                        type="button"
                        onClick={() => removeCodeMappingValue(groupIndex, mappingIndex)}
                      >
                        删除
                      </button>
                    </div>
                  ))}
                </div>
                <div className="actions">
                  <button className="btn btn-ghost" type="button" onClick={() => addCodeMappingValue(groupIndex)}>
                    新增码值取值
                  </button>
                </div>
              </article>
            ))}
          </div>
          <div className="actions">
            <button className="btn btn-ghost" type="button" onClick={addCodeMappingGroup}>
              新增码值组
            </button>
          </div>
          <details className="debug-panel">
            <summary>查看码值说明 JSON</summary>
            <pre className="json-output">{form.codeMappingsJson || "[]"}</pre>
          </details>
          <label className="textarea-label" htmlFor="caveatsJson">风险与注意事项</label>
          <AutoGrowTextarea
            id="caveatsJson"
            name="caveatsJson"
            autoComplete="off"
            className="mini"
            value={form.caveatsJson}
            onChange={(event) => updateFormField("caveatsJson", event.target.value)}
            minRows={4}
            maxRows={14}
          />
          <label className="textarea-label" htmlFor="qualityJson">质量备注</label>
          <AutoGrowTextarea
            id="qualityJson"
            name="qualityJson"
            autoComplete="off"
            className="mini"
            value={form.qualityJson}
            onChange={(event) => updateFormField("qualityJson", event.target.value)}
            minRows={3}
            maxRows={10}
          />

          <div className="publish-check-panel">
            <div className="publish-check-head">
              <strong>最小单元校验</strong>
              <button
                className="btn btn-ghost"
                type="button"
                onClick={() => loadMinimumUnitCheck(currentDraft?.sceneId || sceneId)}
                disabled={minimumUnitLoading || !(currentDraft?.sceneId || sceneId)}
              >
                {minimumUnitLoading ? "校验中…" : "重新校验"}
              </button>
            </div>
            {minimumUnitCheck && Array.isArray(minimumUnitCheck.items) ? (
              <div className="publish-checklist">
                {minimumUnitCheck.items.map((item) => (
                  <div
                    key={item.key}
                    className={`publish-check-item ${item.passed ? "is-ok" : "is-warn"}`}
                  >
                    <span className="publish-check-dot" aria-hidden="true">{item.passed ? "✓" : "!"}</span>
                    <span>{item.name}：{item.message}</span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="subtle-note">保存场景后会自动生成最小单元校验结果。</p>
            )}
          </div>

          <div className="publish-bar">
            <label htmlFor="publishVerifiedAt">验证时间</label>
            <input
              id="publishVerifiedAt"
              name="publishVerifiedAt"
              autoComplete="off"
              type="datetime-local"
              value={publishVerifiedAt}
              onChange={(event) => {
                setPublishVerifiedAt(event.target.value);
                setHasUnsavedChanges(true);
              }}
            />
            <label htmlFor="publishSummary">变更摘要</label>
            <input
              id="publishSummary"
              name="publishSummary"
              autoComplete="off"
              value={publishSummary}
              onChange={(event) => {
                setPublishSummary(event.target.value);
                setHasUnsavedChanges(true);
              }}
              placeholder="发布前必须填写… 例如：补充风险说明"
            />
          </div>

          <div className="actions">
            {isImportPreset ? null : (
              <button className="btn btn-primary" type="button" onClick={createDraft} disabled={saving}>手动创建草稿</button>
            )}
            <button className="btn btn-secondary" type="button" onClick={saveDraft} disabled={saving || !saveReady}>保存当前场景</button>
            <button className="btn btn-accent" type="button" onClick={publish} disabled={saving || !publishReady}>发布当前场景</button>
            {isImportPreset ? (
              <button className="btn btn-ghost" type="button" onClick={discardCurrentScene} disabled={saving || !(currentDraft?.sceneId || sceneId)}>
                弃用当前场景
              </button>
            ) : null}
          </div>
          <p className="meta">
            当前场景：{sceneId ? `#${sceneId}` : "未创建"} · 状态：{sceneStatus}
            {currentDraft?.lowConfidence ? " · 低置信度（发布前请确认）" : ""}
          </p>
      </AccordionStepCard>
    </div>
  );
}
