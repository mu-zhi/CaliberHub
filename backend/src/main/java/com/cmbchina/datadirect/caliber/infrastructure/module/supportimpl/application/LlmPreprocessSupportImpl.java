package com.cmbchina.datadirect.caliber.infrastructure.module.supportimpl.application;

import com.cmbchina.datadirect.caliber.application.service.support.SqlTableParseSupport;
import com.cmbchina.datadirect.caliber.application.support.LlmPreprocessSupport;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmPromptDefaults;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmPromptFingerprint;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmPrepSchemaJsonGenerator;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmPreprocessProperties;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmProviderCapabilityRegistry;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmSecretCodec;
import com.cmbchina.datadirect.caliber.domain.model.LlmPreprocessConfig;
import com.cmbchina.datadirect.caliber.domain.support.LlmPreprocessConfigDomainSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LlmPreprocessSupportImpl implements LlmPreprocessSupport {

    private static final Pattern SQL_CODE_BLOCK_PATTERN = Pattern.compile("(?is)```sql\\s*(.*?)```");
    private static final Pattern SQL_STATEMENT_PATTERN = Pattern.compile(
            "(?is)(with\\b[\\s\\S]*?;|select\\b[\\s\\S]*?;|insert\\b[\\s\\S]*?;|update\\b[\\s\\S]*?;|delete\\b[\\s\\S]*?;)");
    private static final Pattern SQL_START_LINE_PATTERN = Pattern.compile("(?i)^\\s*(?:with|select|insert|update|delete|merge|create|alter|drop)\\b");
    private static final Pattern TABLE_HINT_PATTERN = Pattern.compile("(?i)\\b(?:from|join)\\s+([a-zA-Z0-9_.$]+)");
    private static final Pattern SQL_LINE_COMMENT_PATTERN = Pattern.compile("(?m)--\\s*(.+)$");
    private static final Pattern SQL_BLOCK_COMMENT_PATTERN = Pattern.compile("(?s)/\\*\\s*(.*?)\\s*\\*/");
    private static final Pattern STEP_HINT_PATTERN = Pattern.compile("(?im)^\\s*(?:--\\s*)?(step\\s*\\d+[^\\r\\n]*)$");
    private static final Pattern STEP_HINT_CN_PATTERN = Pattern.compile("(?im)^\\s*(?:--\\s*)?(第\\s*\\d+\\s*步[^\\r\\n]*)$");
    private static final Pattern METHOD_HINT_PATTERN = Pattern.compile("(?im)^\\s*(?:--\\s*)?(?:方法\\s*\\d+|method\\s*\\d+)[：:\\s]+([^\\r\\n]{2,100})$");
    private static final Pattern INLINE_METHOD_HINT_PATTERN = Pattern.compile("(?im)(?:方法\\s*\\d+|method\\s*\\d+)[：:\\s]+([^\\r\\n]{2,100})");
    private static final Pattern BUSINESS_COMMENT_TITLE_PATTERN = Pattern.compile("(?im)^\\s*(?:--\\s*)?([^\\r\\n]{2,80}(?:查询|明细|批次|统计|核对|校验)[^\\r\\n]{0,40})$");
    private static final Pattern PERIOD_TEXT_HINT_PATTERN = Pattern.compile("(?im)(?:数据日期|数据时间范围|时间范围)\\s*[:：]?\\s*([0-9]{4}(?:[0-9]{4})?)\\s*[-~至到]\\s*([0-9]{4}(?:[0-9]{4})?|至今|今)");
    private static final Pattern LLM_STATUS_CODE_PATTERN = Pattern.compile("status=(\\d{3})");
    private static final Pattern CODE_FIELD_PATTERN = Pattern.compile("\\b([A-Z][A-Z0-9_]*(?:_CD|_CODE|_FLAG|_TYPE))\\b");
    private static final Pattern CODE_VALUE_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])([A-Za-z0-9]{1,8})\\s*[=:：]\\s*([^,，;；\\n]{1,40})");
    private static final Pattern PERIOD_HINT_PATTERN = Pattern.compile("((?:19|20)\\d{2})\\s*(?:年|至|到|-)\\s*(((?:19|20)\\d{2})|今|当前)");
    private static final Pattern QUERY_SCENE_HEADING_PATTERN = Pattern.compile("(?m)^#{2,6}\\s*(查询[^\\r\\n]{1,80})\\s*$");
    private static final Pattern SOURCE_NAME_SCENE_PATTERN = Pattern.compile("([\\p{IsHan}A-Za-z0-9]+(?:查询|变更|明细))");
    private static final Pattern SQL_OUTPUT_ALIAS_PATTERN = Pattern.compile("`([^`]+)`$");
    private static final Pattern SQL_OUTPUT_AS_PATTERN = Pattern.compile("(?i)\\bas\\s+([\\p{IsHan}A-Za-z_][\\p{IsHan}A-Za-z0-9_]*)$");
    private static final Pattern SQL_WHERE_CONDITION_PATTERN = Pattern.compile("(?i)\\b([A-Za-z_][A-Za-z0-9_.$]*)\\s*(=|<>|!=|>=|<=|>|<|like\\b|in\\s*\\()");
    private static final Pattern SQL_CLAUSE_LINE_PATTERN = Pattern.compile("(?i)^\\s*(?:from|where|join|left\\s+join|right\\s+join|inner\\s+join|group\\s+by|order\\s+by|having|union|limit|on|set)\\b");
    private static final Pattern SQL_FIELD_ALIAS_LINE_PATTERN = Pattern.compile("(?i)^\\s*[a-z_][a-z0-9_$]*\\.[a-z_][a-z0-9_$]*(?:\\s+as\\b.*|\\s*,\\s*|)$");
    private static final Pattern SQL_INLINE_SELECT_FROM_PATTERN = Pattern.compile("(?i)\\bselect\\b[\\s\\S]*\\bfrom\\b");
    private static final Pattern DOC_LINE_PREFIX_PATTERN = Pattern.compile("^\\s*\\[(\\d{1,6})]\\s*");
    private static final Pattern DECORATION_LINE_PATTERN = Pattern.compile("^[=\\-_*#\\s]{4,}$");
    private static final String DOC_TYPE = "CALIBER_IMPORT_V2";
    private static final String SCHEMA_VERSION = "2.0.0";
    private static final String PREP_TYPE = "CALIBER_PREP_V1";
    private static final int MAX_LLM_INPUT_LINES = 10_000;
    private static final int MAX_LLM_RETRY_COUNT = 3;
    private static final long[] LLM_RETRY_BACKOFF_MILLIS = {300L, 800L, 1500L};
    private static final double RULE_FALLBACK_CONFIDENCE = 0.55D;
    private static final String FALLBACK_WARNING = "llm_preprocess_fallback_to_rule";
    private static final String PREP_INVALID_WARNING = "llm_prep_invalid_or_incomplete";
    private static final String PREP_TRUNCATED_WARNING = "llm_prep_context_truncated";
    private static final String SQL_FRAGMENT_WARNING = "sql_fragment_incomplete";
    private static final String SQL_INTEGRITY_MISMATCH_WARNING = "sql_raw_integrity_mismatch";
    private static final String SQL_INTEGRITY_FALLBACK_WARNING = "sql_raw_integrity_check_failed_fallback_rule";
    private static final String PREP_SYSTEM_PROMPT = LlmPromptDefaults.PREPROCESS_SYSTEM_PROMPT;
    private static final String PREP_SCHEMA_JSON = LlmPromptDefaults.PREP_SCHEMA_JSON;
    private static final String PREP_USER_PROMPT_TEMPLATE = LlmPromptDefaults.PREPROCESS_USER_PROMPT_TEMPLATE;
    private static final String PROMPT_SYSTEM_EMPTY_WARNING = "preprocess_system_prompt_empty_fallback_default";
    private static final String PROMPT_USER_TEMPLATE_EMPTY_WARNING = "preprocess_user_prompt_template_empty_fallback_default";
    private static final String PROMPT_USER_TEMPLATE_INVALID_WARNING = "preprocess_user_prompt_template_invalid_fallback_default";
    private static final String PROMPT_SCHEMA_EMPTY_WARNING = "prep_schema_empty_fallback_default";
    private static final String PROMPT_SCHEMA_INVALID_WARNING = "prep_schema_invalid_fallback_default";
    private static final String PROMPT_SCHEMA_DYNAMIC_FALLBACK_WARNING = "dynamic_schema_generation_failed_fallback_static";
    private static final String LLM_RATE_LIMIT_FALLBACK_WARNING = "llm_rate_limited_fallback_to_rule";
    private static final String LLM_CIRCUIT_OPEN_FALLBACK_WARNING = "llm_circuit_open_fallback_to_rule";
    private static final String LLM_CIRCUIT_OPENED_WARNING = "llm_circuit_opened_fallback_to_rule";
    private static final String TOKEN_PREP_SCHEMA = "{{PREP_SCHEMA}}";
    private static final String TOKEN_DYNAMIC_JSON_SCHEMA = "{{DYNAMIC_JSON_SCHEMA}}";
    private static final String TOKEN_RAW_DOC = "{{RAW_DOC}}";
    private static final String TOKEN_SOURCE_TYPE = "{{SOURCE_TYPE}}";

    private final ObjectMapper objectMapper;
    private final LlmPreprocessProperties llmPreprocessProperties;
    private final LlmPreprocessConfigDomainSupport llmPreprocessConfigDomainSupport;
    private final LlmSecretCodec llmSecretCodec;
    private final LlmPrepSchemaJsonGenerator llmPrepSchemaJsonGenerator;
    private final HttpClient httpClient;
    private final Object circuitLock = new Object();
    private long rateWindowStartAt = 0L;
    private int rateWindowCount = 0;
    private int consecutiveFailureCount = 0;
    private long circuitOpenUntil = 0L;

    public LlmPreprocessSupportImpl(ObjectMapper objectMapper,
                                    LlmPreprocessProperties llmPreprocessProperties,
                                    LlmPreprocessConfigDomainSupport llmPreprocessConfigDomainSupport,
                                    LlmSecretCodec llmSecretCodec,
                                    LlmPrepSchemaJsonGenerator llmPrepSchemaJsonGenerator) {
        this.objectMapper = objectMapper;
        this.llmPreprocessProperties = llmPreprocessProperties;
        this.llmPreprocessConfigDomainSupport = llmPreprocessConfigDomainSupport;
        this.llmSecretCodec = llmSecretCodec;
        this.llmPrepSchemaJsonGenerator = llmPrepSchemaJsonGenerator;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public String preprocessToCaliberImportV2(String rawText, String sourceType, String sourceName) {
        return buildRuleBasedImportJson(rawText, sourceType, sourceName, List.of());
    }

    @Override
    public String preprocessToCaliberImportV2ByLlm(String rawText, String sourceType, String sourceName) {
        RuntimeConfig runtimeConfig = resolveRuntimeConfig();
        if (!runtimeConfig.enabled()) {
            return buildRuleBasedImportJson(rawText, sourceType, sourceName, List.of());
        }
        enforceLlmInputLineLimit(rawText);
        Exception lastFailure = null;
        List<String> fallbackWarnings = new ArrayList<>();
        int maxAttempt = 1 + MAX_LLM_RETRY_COUNT;
        for (int attempt = 1; attempt <= maxAttempt; attempt++) {
            try {
                JsonNode prepRoot = callLlmPreprocess(rawText, sourceType, runtimeConfig);
                if (!isValidPrep(prepRoot)) {
                    return buildRuleBasedImportJson(rawText, sourceType, sourceName, List.of(PREP_INVALID_WARNING));
                }
                recordLlmSuccess();
                return buildImportJsonFromPrep(prepRoot, rawText, sourceType, sourceName);
            } catch (Exception ex) {
                lastFailure = ex;
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (ex instanceof LlmRateLimitException) {
                    fallbackWarnings = mergeDistinct(fallbackWarnings, List.of(LLM_RATE_LIMIT_FALLBACK_WARNING));
                    break;
                }
                if (ex instanceof LlmCircuitOpenException) {
                    fallbackWarnings = mergeDistinct(fallbackWarnings, List.of(LLM_CIRCUIT_OPEN_FALLBACK_WARNING));
                    break;
                }
                if (recordLlmFailure(runtimeConfig)) {
                    fallbackWarnings = mergeDistinct(fallbackWarnings, List.of(LLM_CIRCUIT_OPENED_WARNING));
                    break;
                }
                boolean retryable = isRetryableLlmFailure(ex);
                boolean hasNextAttempt = attempt < maxAttempt;
                if (!retryable || !hasNextAttempt) {
                    break;
                }
                if (!sleepBeforeRetry(attempt)) {
                    break;
                }
            }
        }
        if (!runtimeConfig.fallbackToRule()) {
            throw new IllegalStateException("llm preprocess failed and fallback is disabled", lastFailure);
        }
        return buildRuleBasedImportJson(
                rawText,
                sourceType,
                sourceName,
                mergeDistinct(fallbackWarnings, List.of(FALLBACK_WARNING))
        );
    }

    @Override
    public PromptPreviewResult previewPrompt(String rawText,
                                             String sourceType,
                                             String preprocessSystemPrompt,
                                             String preprocessUserPromptTemplate,
                                             String prepSchemaJson) {
        RuntimeConfig runtimeConfig = resolveRuntimeConfig();
        RuntimeConfig previewConfig = overrideRuntimeConfig(
                runtimeConfig,
                preprocessSystemPrompt,
                preprocessUserPromptTemplate,
                prepSchemaJson
        );
        PromptRenderResult promptRenderResult = renderPrompt(previewConfig, rawText, sourceType);
        String promptFingerprint = LlmPromptFingerprint.of(
                promptRenderResult.systemPrompt(),
                promptRenderResult.userPromptTemplate(),
                promptRenderResult.prepSchemaJson()
        );
        return new PromptPreviewResult(
                promptRenderResult.systemPrompt(),
                promptRenderResult.userPrompt(),
                promptFingerprint,
                promptRenderResult.normalizedSourceType(),
                countLines(rawText),
                promptRenderResult.warnings()
        );
    }

    private String buildRuleBasedImportJson(String rawText, String sourceType, String sourceName, List<String> extraWarnings) {
        List<String> sqlBlocks = extractSqlBlocks(rawText);
        List<String> sourceTables = extractSourceTables(sqlBlocks);
        List<String> warnings = buildWarnings(rawText, sqlBlocks, extraWarnings);
        boolean lowConfidence = shouldDowngradeRuleConfidence(warnings);
        List<RuleSceneCluster> sceneClusters = buildRuleSceneClusters(rawText, sqlBlocks, sourceName);
        List<Map<String, Object>> scenes = new ArrayList<>();
        for (RuleSceneCluster sceneCluster : sceneClusters) {
            scenes.add(buildSceneBlock(
                    rawText,
                    sceneCluster.sceneTitle(),
                    sceneCluster.sqlBlocks(),
                    sourceTables,
                    warnings,
                    lowConfidence
            ));
        }
        if (scenes.isEmpty()) {
            scenes.add(buildSceneBlock(
                    rawText,
                    extractSceneTitle(rawText, sourceName),
                    sqlBlocks,
                    sourceTables,
                    warnings,
                    lowConfidence
            ));
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("doc_type", DOC_TYPE);
        root.put("schema_version", SCHEMA_VERSION);
        root.put("source_type", normalizeSourceType(sourceType));
        root.put("global", buildGlobalBlock(rawText, sourceTables, sourceName));
        root.put("scenes", scenes);
        root.put("parse_report", buildParseReportBlock(warnings));
        root.put("_meta", buildMetaBlock("rule_generated"));

        return writeJson(root);
    }

    private boolean sleepBeforeRetry(int attempt) {
        int backoffIndex = Math.min(attempt - 1, LLM_RETRY_BACKOFF_MILLIS.length - 1);
        long backoffMillis = LLM_RETRY_BACKOFF_MILLIS[backoffIndex];
        try {
            Thread.sleep(backoffMillis);
            return true;
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean isRetryableLlmFailure(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (throwable instanceof HttpTimeoutException || throwable instanceof SocketTimeoutException) {
            return true;
        }
        if (throwable instanceof IOException) {
            return true;
        }
        if (throwable instanceof IllegalStateException illegalStateException) {
            Integer statusCode = resolveHttpStatusCode(illegalStateException.getMessage());
            if (statusCode != null) {
                return statusCode == 429 || statusCode >= 500;
            }
        }
        return isRetryableLlmFailure(throwable.getCause());
    }

    private Integer resolveHttpStatusCode(String message) {
        String safe = trimOrEmpty(message);
        if (safe.isEmpty()) {
            return null;
        }
        Matcher matcher = LLM_STATUS_CODE_PATTERN.matcher(safe);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private boolean shouldDowngradeRuleConfidence(List<String> warnings) {
        for (String warning : warnings) {
            String normalized = trimOrEmpty(warning);
            if (normalized.isEmpty()) {
                continue;
            }
            if (normalized.startsWith(FALLBACK_WARNING)
                    || normalized.startsWith(PREP_INVALID_WARNING)
                    || normalized.startsWith(SQL_INTEGRITY_FALLBACK_WARNING)) {
                return true;
            }
        }
        return false;
    }

    private List<RuleSceneCluster> buildRuleSceneClusters(String rawText, List<String> sqlBlocks, String sourceName) {
        List<RuleSceneCluster> clusters = new ArrayList<>();
        if (sqlBlocks.isEmpty()) {
            return clusters;
        }
        String defaultTitle = extractSceneTitle(rawText, sourceName);
        Map<String, RuleSceneClusterBuilder> byClusterKey = new LinkedHashMap<>();
        for (int i = 0; i < sqlBlocks.size(); i++) {
            String sql = sqlBlocks.get(i);
            int sqlIndex = i + 1;
            String stepHint = extractStepHint(sql);
            String periodHint = extractPeriodHint(sql);
            String clusterKey = buildClusterKey(stepHint, periodHint, sqlIndex);
            RuleSceneClusterBuilder builder = byClusterKey.get(clusterKey);
            if (builder == null) {
                String title = buildRuleSceneTitle(stepHint, periodHint, sql, sqlIndex, defaultTitle, rawText);
                builder = new RuleSceneClusterBuilder(title);
                byClusterKey.put(clusterKey, builder);
            }
            builder.sqlBlocks().add(sql);
        }
        for (RuleSceneClusterBuilder builder : byClusterKey.values()) {
            clusters.add(new RuleSceneCluster(builder.sceneTitle(), builder.sqlBlocks()));
        }
        return clusters;
    }

    private String buildClusterKey(String stepHint, String periodHint, int sqlIndex) {
        if (!stepHint.isBlank()) {
            return "step:" + stepHint + "|period:" + periodHint;
        }
        if (!periodHint.isBlank()) {
            return "period:" + periodHint;
        }
        return "single:" + sqlIndex;
    }

    private String buildRuleSceneTitle(String stepHint,
                                       String periodHint,
                                       String sql,
                                       int sqlIndex,
                                       String defaultTitle,
                                       String rawText) {
        String methodHint = extractBusinessTitleFromSql(sql);
        if (methodHint.isBlank()) {
            methodHint = extractBusinessTitleFromRawContext(rawText, sql);
        }
        if (!methodHint.isBlank()) {
            return methodHint;
        }
        if (!stepHint.isBlank() && !periodHint.isBlank()) {
            return stepHint + " · " + periodHint;
        }
        if (!stepHint.isBlank()) {
            return stepHint;
        }
        if (!periodHint.isBlank()) {
            return "时段 " + periodHint;
        }
        String primaryTable = extractSourceTables(List.of(sql)).stream().findFirst().orElse("");
        if (!primaryTable.isBlank()) {
            return primaryTable + " 查询";
        }
        if (sqlIndex == 1 && !defaultTitle.isBlank()) {
            return defaultTitle;
        }
        return "场景" + sqlIndex;
    }

    private String extractBusinessTitleFromSql(String sql) {
        String safe = safeText(sql);
        if (safe.isBlank()) {
            return "";
        }
        Matcher lineMatcher = METHOD_HINT_PATTERN.matcher(safe);
        String lineCandidate = "";
        while (lineMatcher.find()) {
            lineCandidate = cleanBusinessText(lineMatcher.group(1));
        }
        if (!lineCandidate.isBlank()) {
            return lineCandidate;
        }
        Matcher inlineMatcher = INLINE_METHOD_HINT_PATTERN.matcher(safe);
        String inlineCandidate = "";
        while (inlineMatcher.find()) {
            inlineCandidate = cleanBusinessText(inlineMatcher.group(1));
        }
        if (!inlineCandidate.isBlank()) {
            return inlineCandidate;
        }
        return "";
    }

    private String extractBusinessTitleFromRawContext(String rawText, String sql) {
        String safeRaw = safeText(rawText);
        String safeSql = safeText(sql);
        if (safeRaw.isBlank() || safeSql.isBlank()) {
            return "";
        }
        String anchor = "";
        String[] sqlLines = safeSql.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String sqlLine : sqlLines) {
            String trimmed = trimOrEmpty(sqlLine);
            if (trimmed.isBlank()) {
                continue;
            }
            if (SQL_START_LINE_PATTERN.matcher(trimmed).find()) {
                anchor = trimmed;
                break;
            }
        }
        if (anchor.isBlank()) {
            return "";
        }
        String[] rawLines = safeRaw.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        int anchorIndex = findAnchorLineIndex(rawLines, anchor);
        if (anchorIndex < 0) {
            return "";
        }
        int scanStart = Math.max(0, anchorIndex - 12);
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = scanStart; i < anchorIndex; i++) {
            contextBuilder.append(rawLines[i]).append('\n');
        }
        String methodHint = extractBusinessTitleFromSql(contextBuilder.toString());
        if (!methodHint.isBlank()) {
            return methodHint;
        }
        for (int i = anchorIndex - 1; i >= scanStart; i--) {
            String line = trimCommentLine(rawLines[i]);
            if (line.isBlank()) {
                continue;
            }
            Matcher matcher = BUSINESS_COMMENT_TITLE_PATTERN.matcher(line);
            if (matcher.find()) {
                String candidate = cleanBusinessText(matcher.group(1));
                if (!candidate.isBlank()) {
                    return candidate;
                }
            }
        }
        return "";
    }

    private int findAnchorLineIndex(String[] lines, String anchorLine) {
        String anchor = normalizeLineForLookup(anchorLine);
        if (anchor.isBlank()) {
            return -1;
        }
        if (anchor.length() > 120) {
            anchor = anchor.substring(0, 120);
        }
        for (int i = 0; i < lines.length; i++) {
            String candidate = normalizeLineForLookup(lines[i]);
            if (candidate.isBlank()) {
                continue;
            }
            if (candidate.startsWith(anchor) || anchor.startsWith(candidate)) {
                return i;
            }
        }
        return -1;
    }

    private String normalizeLineForLookup(String line) {
        String normalized = safeText(line)
                .replace('`', ' ')
                .replace('"', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        return normalized;
    }

    private String extractStepHint(String sql) {
        String safe = safeText(sql);
        Matcher stepMatcher = STEP_HINT_PATTERN.matcher(safe);
        if (stepMatcher.find()) {
            return stepMatcher.group(1).replaceAll("\\s+", " ").trim();
        }
        Matcher cnStepMatcher = STEP_HINT_CN_PATTERN.matcher(safe);
        if (cnStepMatcher.find()) {
            return cnStepMatcher.group(1).replaceAll("\\s+", " ").trim();
        }
        return "";
    }

    private String extractPeriodHint(String sql) {
        String safe = safeText(sql);
        Matcher periodTextMatcher = PERIOD_TEXT_HINT_PATTERN.matcher(safe);
        if (periodTextMatcher.find()) {
            return normalizePeriodValue(periodTextMatcher.group(1), periodTextMatcher.group(2));
        }
        Matcher periodMatcher = PERIOD_HINT_PATTERN.matcher(safe);
        if (periodMatcher.find()) {
            String start = periodMatcher.group(1);
            String end = periodMatcher.group(2);
            return normalizePeriodValue(start, end);
        }
        return "";
    }

    private String normalizePeriodValue(String rawStart, String rawEnd) {
        String start = trimOrEmpty(rawStart);
        String end = trimOrEmpty(rawEnd);
        if (start.length() >= 4) {
            start = start.substring(0, 4);
        }
        if (end.equalsIgnoreCase("今") || end.equalsIgnoreCase("至今")) {
            end = "至今";
        } else if (end.length() >= 4) {
            end = end.substring(0, 4);
        }
        if (start.isBlank() || end.isBlank()) {
            return "";
        }
        return start + "-" + end;
    }

    private String buildImportJsonFromPrep(JsonNode prepRoot, String rawText, String sourceType, String sourceName) {
        List<String> unresolved = readTextArray(prepRoot.path("unresolved"));
        SqlBlocksBuildResult sqlBlockResult = buildSqlBlocksFromPrep(prepRoot.path("sql_segments"), rawText);
        List<Map<String, Object>> sqlBlocks = sqlBlockResult.blocks();
        List<String> sourceTables = buildSourceTablesFromPrep(prepRoot.path("table_hints"), sqlBlocks);
        Map<String, Object> quality = buildQualityFromPrep(prepRoot, sqlBlocks, unresolved);

        List<Map<String, Object>> scenes = buildScenesFromPrep(prepRoot, rawText, sqlBlockResult, sourceTables, quality, unresolved, sourceName);
        if (scenes.isEmpty()) {
            return buildRuleBasedImportJson(rawText, sourceType, sourceName, List.of(PREP_INVALID_WARNING));
        }

        List<String> parseWarnings = mergeDistinct(readTextArray(prepRoot.path("quality").path("warnings")), unresolved);
        parseWarnings = mergeDistinct(parseWarnings, sqlBlockResult.warnings());
        if (prepRoot.path("doc_profile").path("is_truncated").asBoolean(false)) {
            parseWarnings = mergeDistinct(parseWarnings, List.of(PREP_TRUNCATED_WARNING));
        }
        List<String> parseErrors = readTextArray(prepRoot.path("quality").path("errors"));

        Map<String, Object> parseReport = new LinkedHashMap<>();
        parseReport.put("parser", "llm-preprocess-v2-plus-rule-v2");
        parseReport.put("warnings", parseWarnings);
        parseReport.put("errors", parseErrors);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("doc_type", DOC_TYPE);
        root.put("schema_version", SCHEMA_VERSION);
        root.put("source_type", normalizeSourceType(sourceType));
        root.put("global", buildGlobalBlockFromPrep(prepRoot, sourceTables, unresolved));
        root.put("scenes", scenes);
        root.put("parse_report", parseReport);
        root.put("_meta", buildMetaBlock("llm_enhanced"));

        return writeJson(root);
    }

    private JsonNode callLlmPreprocess(String rawText, String sourceType, RuntimeConfig runtimeConfig) throws IOException, InterruptedException {
        enforceCircuitAndRateLimit(runtimeConfig);
        String endpoint = runtimeConfig.endpoint();
        if (endpoint.isEmpty()) {
            throw new IllegalStateException("llm endpoint is empty");
        }
        String model = runtimeConfig.model();
        if (model.isEmpty()) {
            throw new IllegalStateException("llm model is empty");
        }
        PromptRenderResult promptRenderResult = renderPrompt(runtimeConfig, rawText, sourceType);
        LlmProviderCapabilityRegistry.ProviderCapability capability =
                LlmProviderCapabilityRegistry.resolve(endpoint, model);

        Map<String, Object> requestPayload = capability.supportsResponsesApi()
                ? buildResponsesRequestPayload(model, runtimeConfig, promptRenderResult)
                : buildChatCompletionsRequestPayload(model, runtimeConfig, promptRenderResult);
        String requestBody = writeJson(requestPayload);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(Math.max(5, runtimeConfig.timeoutSeconds())))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));
        String apiKey = runtimeConfig.apiKey();
        if (!apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 300) {
            String briefBody = abbreviate(trimOrEmpty(response.body()), 200);
            if (briefBody.isBlank()) {
                throw new IllegalStateException("llm request failed, status=" + response.statusCode());
            }
            throw new IllegalStateException("llm request failed, status=" + response.statusCode() + ", body=" + briefBody);
        }

        JsonNode root = objectMapper.readTree(response.body());
        String assistantContent = extractAssistantContent(root);
        JsonNode prepRoot = parsePrepJson(assistantContent);
        appendQualityWarnings(prepRoot, promptRenderResult.warnings());
        return prepRoot;
    }

    private void enforceCircuitAndRateLimit(RuntimeConfig runtimeConfig) {
        long now = System.currentTimeMillis();
        synchronized (circuitLock) {
            if (circuitOpenUntil > now) {
                throw new LlmCircuitOpenException("llm circuit is open");
            }
            if (runtimeConfig.rateLimitPerMinute() > 0) {
                if (rateWindowStartAt <= 0 || (now - rateWindowStartAt) >= 60_000L) {
                    rateWindowStartAt = now;
                    rateWindowCount = 0;
                }
                if (rateWindowCount >= runtimeConfig.rateLimitPerMinute()) {
                    throw new LlmRateLimitException("llm rate limit exceeded");
                }
                rateWindowCount++;
            }
        }
    }

    private boolean recordLlmFailure(RuntimeConfig runtimeConfig) {
        synchronized (circuitLock) {
            consecutiveFailureCount++;
            if (runtimeConfig.circuitBreakerFailureThreshold() > 0
                    && consecutiveFailureCount >= runtimeConfig.circuitBreakerFailureThreshold()) {
                circuitOpenUntil = System.currentTimeMillis() + (long) runtimeConfig.circuitBreakerOpenSeconds() * 1000L;
                consecutiveFailureCount = 0;
                return true;
            }
            return false;
        }
    }

    private void recordLlmSuccess() {
        synchronized (circuitLock) {
            consecutiveFailureCount = 0;
            circuitOpenUntil = 0L;
        }
    }

    private Map<String, String> buildMessage(String role, String content) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private Map<String, Object> buildChatCompletionsRequestPayload(String model,
                                                                   RuntimeConfig runtimeConfig,
                                                                   PromptRenderResult promptRenderResult) {
        Map<String, Object> requestPayload = new LinkedHashMap<>();
        requestPayload.put("model", model);
        requestPayload.put("temperature", runtimeConfig.temperature());
        requestPayload.put("max_tokens", runtimeConfig.maxTokens());
        requestPayload.put("enable_thinking", runtimeConfig.enableThinking());
        requestPayload.put("response_format", Map.of("type", "json_object"));
        requestPayload.put("messages", List.of(
                buildMessage("system", promptRenderResult.systemPrompt()),
                buildMessage("user", promptRenderResult.userPrompt())
        ));
        return requestPayload;
    }

    private Map<String, Object> buildResponsesRequestPayload(String model,
                                                             RuntimeConfig runtimeConfig,
                                                             PromptRenderResult promptRenderResult) {
        Map<String, Object> requestPayload = new LinkedHashMap<>();
        requestPayload.put("model", model);
        requestPayload.put("instructions", promptRenderResult.systemPrompt());
        requestPayload.put("input", promptRenderResult.userPrompt());
        requestPayload.put("temperature", runtimeConfig.temperature());
        requestPayload.put("max_output_tokens", runtimeConfig.maxTokens());
        requestPayload.put("text", Map.of("format", Map.of("type", "json_object")));
        return requestPayload;
    }

    private PromptRenderResult renderPrompt(RuntimeConfig runtimeConfig, String rawText, String sourceType) {
        List<String> warnings = new ArrayList<>();
        String systemPrompt = trimOrEmpty(runtimeConfig.preprocessSystemPrompt());
        if (systemPrompt.isEmpty()) {
            systemPrompt = PREP_SYSTEM_PROMPT;
            warnings.add(PROMPT_SYSTEM_EMPTY_WARNING);
        }

        String dynamicSchema = trimOrEmpty(llmPrepSchemaJsonGenerator.generateSchemaJson());
        if (dynamicSchema.isEmpty()) {
            dynamicSchema = PREP_SCHEMA_JSON;
            warnings.add(PROMPT_SCHEMA_DYNAMIC_FALLBACK_WARNING);
        }

        String template = trimOrEmpty(runtimeConfig.preprocessUserPromptTemplate());
        if (template.isEmpty()) {
            template = PREP_USER_PROMPT_TEMPLATE;
            warnings.add(PROMPT_USER_TEMPLATE_EMPTY_WARNING);
        } else {
            boolean hasSchemaToken = template.contains(TOKEN_PREP_SCHEMA) || template.contains(TOKEN_DYNAMIC_JSON_SCHEMA);
            if (!(hasSchemaToken && template.contains(TOKEN_RAW_DOC) && template.contains(TOKEN_SOURCE_TYPE))) {
                template = PREP_USER_PROMPT_TEMPLATE;
                warnings.add(PROMPT_USER_TEMPLATE_INVALID_WARNING);
            }
        }

        boolean needsLegacyPrepSchema = template.contains(TOKEN_PREP_SCHEMA);
        String prepSchema = dynamicSchema;
        if (needsLegacyPrepSchema) {
            prepSchema = trimOrEmpty(runtimeConfig.prepSchemaJson());
            if (prepSchema.isEmpty()) {
                prepSchema = dynamicSchema;
                warnings.add(PROMPT_SCHEMA_EMPTY_WARNING);
            } else {
                try {
                    objectMapper.readTree(prepSchema);
                } catch (Exception ex) {
                    prepSchema = dynamicSchema;
                    warnings.add(PROMPT_SCHEMA_INVALID_WARNING);
                }
            }
        }

        String schemaForFingerprint = template.contains(TOKEN_DYNAMIC_JSON_SCHEMA) ? dynamicSchema : prepSchema;
        String normalizedSourceType = normalizeSourceType(sourceType);
        String userPrompt = template
                .replace(TOKEN_DYNAMIC_JSON_SCHEMA, dynamicSchema)
                .replace(TOKEN_PREP_SCHEMA, prepSchema)
                .replace(TOKEN_RAW_DOC, buildLineAnchoredRawDoc(rawText))
                .replace(TOKEN_SOURCE_TYPE, normalizedSourceType);
        return new PromptRenderResult(systemPrompt, template, schemaForFingerprint, normalizedSourceType, userPrompt, warnings);
    }

    private RuntimeConfig overrideRuntimeConfig(RuntimeConfig baseConfig,
                                                String preprocessSystemPrompt,
                                                String preprocessUserPromptTemplate,
                                                String prepSchemaJson) {
        return new RuntimeConfig(
                baseConfig.enabled(),
                baseConfig.endpoint(),
                baseConfig.model(),
                baseConfig.apiKey(),
                baseConfig.timeoutSeconds(),
                baseConfig.temperature(),
                baseConfig.maxTokens(),
                baseConfig.enableThinking(),
                baseConfig.fallbackToRule(),
                baseConfig.rateLimitPerMinute(),
                baseConfig.circuitBreakerFailureThreshold(),
                baseConfig.circuitBreakerOpenSeconds(),
                preprocessSystemPrompt == null ? baseConfig.preprocessSystemPrompt() : preprocessSystemPrompt,
                preprocessUserPromptTemplate == null ? baseConfig.preprocessUserPromptTemplate() : preprocessUserPromptTemplate,
                prepSchemaJson == null ? baseConfig.prepSchemaJson() : prepSchemaJson
        );
    }

    private void appendQualityWarnings(JsonNode prepRoot, List<String> extraWarnings) {
        if (!(prepRoot instanceof ObjectNode prepObject) || extraWarnings == null || extraWarnings.isEmpty()) {
            return;
        }
        JsonNode qualityNode = prepObject.path("quality");
        ObjectNode qualityObject;
        if (qualityNode instanceof ObjectNode objectNode) {
            qualityObject = objectNode;
        } else {
            qualityObject = objectMapper.createObjectNode();
            prepObject.set("quality", qualityObject);
        }
        JsonNode warningNode = qualityObject.path("warnings");
        ArrayNode warningArray;
        if (warningNode instanceof ArrayNode arrayNode) {
            warningArray = arrayNode;
        } else {
            warningArray = objectMapper.createArrayNode();
            qualityObject.set("warnings", warningArray);
        }
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (JsonNode warning : warningArray) {
            if (warning.isTextual()) {
                String value = warning.asText("").trim();
                if (!value.isEmpty()) {
                    merged.add(value);
                }
            }
        }
        for (String warning : extraWarnings) {
            String value = trimOrEmpty(warning);
            if (!value.isEmpty()) {
                merged.add(value);
            }
        }
        warningArray.removeAll();
        for (String warning : merged) {
            warningArray.add(warning);
        }
    }

    private String extractAssistantContent(JsonNode llmRoot) {
        JsonNode choices = llmRoot.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode contentNode = choices.path(0).path("message").path("content");
            if (contentNode.isTextual()) {
                return contentNode.asText();
            }
            if (contentNode.isArray()) {
                StringBuilder builder = new StringBuilder();
                for (JsonNode piece : contentNode) {
                    if (piece.path("type").asText("").equalsIgnoreCase("text")) {
                        builder.append(piece.path("text").asText(""));
                    }
                }
                return builder.toString();
            }
        }

        JsonNode output = llmRoot.path("output");
        if (output.isArray() && !output.isEmpty()) {
            JsonNode content = output.path(0).path("content");
            if (content.isArray() && !content.isEmpty()) {
                String text = content.path(0).path("text").asText("");
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        throw new IllegalStateException("llm response missing assistant content");
    }

    private JsonNode parsePrepJson(String assistantContent) {
        String raw = safeText(assistantContent).trim();
        List<String> candidates = new ArrayList<>();
        if (!raw.isBlank()) {
            candidates.add(raw);
        }
        String withoutFence = stripMarkdownFence(raw);
        if (!withoutFence.isBlank()) {
            candidates.add(withoutFence);
        }
        String extractedFromRaw = extractFirstJsonObject(raw);
        if (!extractedFromRaw.isBlank()) {
            candidates.add(extractedFromRaw);
        }
        String extractedFromFence = extractFirstJsonObject(withoutFence);
        if (!extractedFromFence.isBlank()) {
            candidates.add(extractedFromFence);
        }

        LinkedHashSet<String> deduped = new LinkedHashSet<>();
        for (String candidate : candidates) {
            String normalized = safeText(candidate).trim();
            if (!normalized.isBlank()) {
                deduped.add(normalized);
            }
        }

        for (String candidate : deduped) {
            try {
                return objectMapper.readTree(candidate);
            } catch (Exception ignore) {
                // try next candidate
            }
        }
        throw new IllegalStateException("llm preprocess json parse failed");
    }

    private String stripMarkdownFence(String text) {
        String safe = safeText(text).trim();
        if (!safe.startsWith("```")) {
            return safe;
        }
        String stripped = safe.replaceFirst("^```[a-zA-Z0-9_-]*\\s*", "");
        stripped = stripped.replaceFirst("\\s*```\\s*$", "");
        return stripped.trim();
    }

    private String extractFirstJsonObject(String text) {
        String safe = safeText(text);
        int start = safe.indexOf('{');
        if (start < 0) {
            return "";
        }
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        for (int i = start; i < safe.length(); i++) {
            char ch = safe.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                depth += 1;
                continue;
            }
            if (ch == '}') {
                depth -= 1;
                if (depth == 0) {
                    return safe.substring(start, i + 1).trim();
                }
            }
        }
        return "";
    }

    private boolean isValidPrep(JsonNode prepRoot) {
        if (prepRoot == null || !prepRoot.isObject()) {
            return false;
        }
        String prepType = prepRoot.path("prep_type").asText("");
        if (!PREP_TYPE.equals(prepType)) {
            return false;
        }
        return prepRoot.path("scene_candidates").isArray()
                && prepRoot.path("sql_segments").isArray()
                && prepRoot.path("quality").isObject();
    }

    private Map<String, Object> buildGlobalBlockFromPrep(JsonNode prepRoot,
                                                         List<String> sourceTables,
                                                         List<String> unresolved) {
        Map<String, Object> global = new LinkedHashMap<>();
        String domainGuess = readText(prepRoot.path("context").path("domain_guess"));
        if (domainGuess == null) {
            domainGuess = readText(prepRoot.path("domain_guess"));
        }
        global.put("domain_guess", domainGuess);
        global.put("domain_overview_hint", buildDomainOverviewHint(prepRoot.path("normalized_text")));
        global.put("common_tables_hint", sourceTables);
        global.put("keywords", sourceTables);
        global.put("notes", unresolved);
        return global;
    }

    private List<Map<String, Object>> buildScenesFromPrep(JsonNode prepRoot,
                                                          String rawText,
                                                          SqlBlocksBuildResult sqlBlockResult,
                                                          List<String> sourceTables,
                                                          Map<String, Object> quality,
                                                          List<String> unresolved,
                                                          String sourceName) {
        List<Map<String, Object>> scenes = new ArrayList<>();
        List<Map<String, Object>> allSqlBlocks = sqlBlockResult.blocks();
        Map<String, Map<String, Object>> sqlBlocksBySegmentId = sqlBlockResult.blocksBySegmentId();
        JsonNode sceneCandidates = prepRoot.path("scene_candidates");
        if (!sceneCandidates.isArray() || sceneCandidates.isEmpty()) {
            return scenes;
        }

        for (JsonNode candidate : sceneCandidates) {
            List<Map<String, Object>> sceneSqlBlocks = resolveSceneSqlBlocks(candidate, allSqlBlocks, sqlBlocksBySegmentId);
            String title = readText(candidate.path("scene_title"));
            if (title == null) {
                title = extractSceneTitle(rawText, sourceName);
            }

            Map<String, Object> scene = new LinkedHashMap<>();
            scene.put("scene_title", title);
            scene.put("scene_code_guess", readText(candidate.path("scene_id"), ""));
            scene.put("domain_guess", null);
            scene.put("contributors", new ArrayList<>());
            scene.put("owner_guess", null);
            scene.put("source_evidence_lines", readIntArray(candidate.path("evidence_lines")));
            String sceneDescription = cleanBusinessText(readText(candidate.path("scene_description_hint"), buildSceneDescription(rawText, title)));
            scene.put("scene_description", sceneDescription);
            scene.put("caliber_definition", readText(prepRoot.path("normalized_text"), ""));
            scene.put("applicability", "");
            scene.put("boundaries", "");
            scene.put("entities", new ArrayList<>());
            scene.put("inputs", buildInputsFromPrep(prepRoot.path("field_hints"), sceneSqlBlocks));
            scene.put("outputs", buildOutputsFromPrep(prepRoot.path("field_hints"), sceneSqlBlocks));
            scene.put("sql_variants", sceneSqlBlocks);
            scene.put("code_mappings", buildCodeMappings(rawText, prepRoot.path("field_hints")));
            scene.put("caveats", buildCaveatsFromPrep(prepRoot.path("risk_notes")));
            String unmappedText = readText(prepRoot.path("carry_over_text"), "");
            if (isLowConfidenceQuality(quality)) {
                unmappedText = mergeTextBlocks(unmappedText, readText(prepRoot.path("normalized_text"), ""));
            }
            if (unresolved != null && !unresolved.isEmpty()) {
                unmappedText = mergeTextBlocks(unmappedText, String.join("\n", unresolved));
            }
            unmappedText = removeDuplicateSegment(cleanBusinessText(unmappedText), sceneDescription);
            scene.put("unmapped_text", unmappedText);
            scene.put("quality", quality);
            scenes.add(scene);
        }
        return scenes;
    }

    private List<Map<String, Object>> resolveSceneSqlBlocks(JsonNode sceneCandidate,
                                                            List<Map<String, Object>> allSqlBlocks,
                                                            Map<String, Map<String, Object>> sqlBlocksBySegmentId) {
        List<String> segmentIds = readTextArray(sceneCandidate.path("sql_segment_ids"));
        if (segmentIds.isEmpty()) {
            return allSqlBlocks;
        }
        List<Map<String, Object>> matched = new ArrayList<>();
        for (String segmentId : segmentIds) {
            Map<String, Object> block = sqlBlocksBySegmentId.get(segmentId);
            if (block != null) {
                matched.add(block);
            }
        }
        return matched.isEmpty() ? allSqlBlocks : matched;
    }

    private Map<String, Object> buildInputsFromPrep(JsonNode fieldHintsNode, List<Map<String, Object>> sqlBlocks) {
        LinkedHashMap<String, Map<String, Object>> paramsByName = new LinkedHashMap<>();
        if (fieldHintsNode.isArray()) {
            for (JsonNode fieldHint : fieldHintsNode) {
                String name = readText(fieldHint.path("field"));
                if (name == null) {
                    continue;
                }
                Map<String, Object> param = new LinkedHashMap<>();
                param.put("name_en", "");
                param.put("name_zh", name);
                param.put("type", "STRING");
                param.put("required", false);
                param.put("example", "");
                param.put("description", readText(fieldHint.path("meaning_hint"), ""));
                paramsByName.put(name, param);
            }
        }
        for (String field : extractWhereFields(sqlBlocks)) {
            if (paramsByName.containsKey(field)) {
                continue;
            }
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("name_en", "");
            param.put("name_zh", field);
            param.put("type", "STRING");
            param.put("required", false);
            param.put("example", "");
            param.put("description", "由 SQL 过滤条件识别");
            paramsByName.put(field, param);
        }

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("params", new ArrayList<>(paramsByName.values()));
        inputs.put("constraints", new ArrayList<>());
        return inputs;
    }

    private Map<String, Object> buildOutputsFromPrep(JsonNode fieldHintsNode, List<Map<String, Object>> sqlBlocks) {
        LinkedHashMap<String, Map<String, Object>> fieldsByName = new LinkedHashMap<>();
        if (fieldHintsNode.isArray()) {
            for (JsonNode fieldHint : fieldHintsNode) {
                String field = readText(fieldHint.path("field"));
                if (field == null) {
                    continue;
                }
                Map<String, Object> outputField = new LinkedHashMap<>();
                outputField.put("display_name", field);
                outputField.put("source_table", readText(fieldHint.path("table"), ""));
                outputField.put("source_field", field);
                outputField.put("sensitivity_hint", readText(fieldHint.path("meaning_hint"), ""));
                outputField.put("mask_rule_suggest", "");
                fieldsByName.put(field, outputField);
            }
        }
        for (Map<String, String> sqlField : extractSelectFields(sqlBlocks)) {
            String displayName = sqlField.getOrDefault("display_name", "");
            if (displayName.isBlank() || fieldsByName.containsKey(displayName)) {
                continue;
            }
            Map<String, Object> outputField = new LinkedHashMap<>();
            outputField.put("display_name", displayName);
            outputField.put("source_table", sqlField.getOrDefault("source_table", ""));
            outputField.put("source_field", sqlField.getOrDefault("source_field", displayName));
            outputField.put("sensitivity_hint", "");
            outputField.put("mask_rule_suggest", "");
            fieldsByName.put(displayName, outputField);
        }

        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("summary", "");
        outputs.put("fields", new ArrayList<>(fieldsByName.values()));
        return outputs;
    }

    private SqlBlocksBuildResult buildSqlBlocksFromPrep(JsonNode sqlSegmentsNode, String rawText) {
        List<Map<String, Object>> blocks = new ArrayList<>();
        Map<String, Map<String, Object>> blocksBySegmentId = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> sourceSqlBlocks = extractSqlBlocks(rawText);
        Map<String, String> sourceSqlByFingerprint = buildSqlFingerprintMap(sourceSqlBlocks);

        if (sqlSegmentsNode.isArray()) {
            int index = 1;
            for (JsonNode segment : sqlSegmentsNode) {
                String sqlRaw = readText(segment.path("sql_raw"));
                if (sqlRaw == null) {
                    continue;
                }
                String segmentId = readText(segment.path("segment_id"), "SQL_%03d".formatted(index));
                String sqlRawWithoutLineTags = stripDocLinePrefix(sqlRaw);
                String exactSql = resolveExactSqlFromSource(sqlRawWithoutLineTags, sourceSqlByFingerprint);
                if (exactSql == null) {
                    warnings.add(SQL_INTEGRITY_MISMATCH_WARNING + ":" + segmentId);
                    continue;
                }
                List<String> tables = extractSourceTables(List.of(exactSql));
                Map<String, Object> block = new LinkedHashMap<>();
                block.put("variant_name", readText(segment.path("name_hint"), "取数方案" + index));
                block.put("applicable_period", guessApplicablePeriod(segment, exactSql));
                block.put("description", readText(segment.path("name_hint"), ""));
                block.put("sql_text", exactSql);
                block.put("source_tables", tables);
                block.put("source_spans", readSourceSpans(segment.path("source_spans")));
                block.put("notes", buildSqlNote(segment));
                blocks.add(block);
                blocksBySegmentId.putIfAbsent(segmentId, block);
                index += 1;
            }
        }
        if (!blocks.isEmpty()) {
            return new SqlBlocksBuildResult(blocks, mergeDistinct(warnings, List.of()), blocksBySegmentId);
        }
        if (!sourceSqlBlocks.isEmpty()) {
            List<Map<String, Object>> fallbackBlocks = buildSqlBlockList(sourceSqlBlocks);
            for (int i = 0; i < fallbackBlocks.size(); i++) {
                blocksBySegmentId.put("SQL_%03d".formatted(i + 1), fallbackBlocks.get(i));
            }
            return new SqlBlocksBuildResult(
                    fallbackBlocks,
                    mergeDistinct(warnings, List.of(SQL_INTEGRITY_FALLBACK_WARNING)),
                    blocksBySegmentId
            );
        }
        return new SqlBlocksBuildResult(new ArrayList<>(), mergeDistinct(warnings, List.of()), blocksBySegmentId);
    }

    private String buildSqlNote(JsonNode segment) {
        List<String> warnings = readTextArray(segment.path("warnings"));
        boolean complete = segment.path("is_complete").asBoolean(true);
        if (!complete) {
            warnings = mergeDistinct(warnings, List.of(SQL_FRAGMENT_WARNING));
        }
        return String.join(" | ", warnings);
    }

    private String guessApplicablePeriod(JsonNode segment, String sqlRaw) {
        String period = readText(segment.path("applicable_period"));
        if (period != null) {
            return period;
        }
        String text = safeText(sqlRaw);
        Matcher matcher = PERIOD_HINT_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().replaceAll("\\s+", "");
        }
        return "";
    }

    private String buildDomainOverviewHint(JsonNode normalizedTextNode) {
        String normalizedText = readText(normalizedTextNode, "");
        if (normalizedText.isBlank()) {
            return "";
        }
        return normalizedText.length() > 500 ? normalizedText.substring(0, 500) : normalizedText;
    }

    private List<Map<String, Object>> buildCodeMappings(String rawText, JsonNode fieldHintsNode) {
        List<Map<String, Object>> commentMappings = buildCodeMappingsFromComments(rawText);
        List<Map<String, Object>> fieldMappings = buildCodeMappingsFromFieldHints(fieldHintsNode);
        if (commentMappings.isEmpty()) {
            return fieldMappings;
        }
        if (fieldMappings.isEmpty()) {
            return commentMappings;
        }
        return mergeCodeMappings(commentMappings, fieldMappings);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mergeCodeMappings(List<Map<String, Object>> first, List<Map<String, Object>> second) {
        Map<String, Map<String, Object>> byCode = new LinkedHashMap<>();
        for (Map<String, Object> mapping : first) {
            String code = Objects.toString(mapping.get("code"), "").trim();
            if (code.isEmpty()) {
                continue;
            }
            Map<String, Object> copied = new LinkedHashMap<>(mapping);
            Object valueObj = copied.get("mappings");
            Map<String, String> values = new LinkedHashMap<>();
            if (valueObj instanceof Map<?, ?> rawMap) {
                rawMap.forEach((key, value) -> {
                    String valueKey = Objects.toString(key, "").trim();
                    String valueText = Objects.toString(value, "").trim();
                    if (!valueKey.isEmpty() && !valueText.isEmpty()) {
                        values.put(valueKey, valueText);
                    }
                });
            }
            copied.put("mappings", values);
            byCode.put(code, copied);
        }
        for (Map<String, Object> mapping : second) {
            String code = Objects.toString(mapping.get("code"), "").trim();
            if (code.isEmpty()) {
                continue;
            }
            Map<String, Object> target = byCode.computeIfAbsent(code, key -> {
                Map<String, Object> created = new LinkedHashMap<>();
                created.put("code", key);
                created.put("description", "");
                created.put("mappings", new LinkedHashMap<String, String>());
                return created;
            });
            String currentDesc = Objects.toString(target.get("description"), "").trim();
            String newDesc = Objects.toString(mapping.get("description"), "").trim();
            if (currentDesc.isEmpty() && !newDesc.isEmpty()) {
                target.put("description", newDesc);
            }
            Map<String, String> targetValues = (Map<String, String>) target.get("mappings");
            Object mappingObj = mapping.get("mappings");
            if (mappingObj instanceof Map<?, ?> rawMap) {
                rawMap.forEach((key, value) -> {
                    String valueKey = Objects.toString(key, "").trim();
                    String valueText = Objects.toString(value, "").trim();
                    if (!valueKey.isEmpty() && !valueText.isEmpty()) {
                        targetValues.putIfAbsent(valueKey, valueText);
                    }
                });
            }
        }
        return new ArrayList<>(byCode.values());
    }

    private List<Map<String, Object>> buildCodeMappingsFromComments(String rawText) {
        List<Map<String, Object>> mappings = new ArrayList<>();
        for (String commentLine : extractSqlComments(rawText)) {
            Matcher codeFieldMatcher = CODE_FIELD_PATTERN.matcher(commentLine);
            if (!codeFieldMatcher.find()) {
                continue;
            }
            String codeField = codeFieldMatcher.group(1);
            Map<String, String> valueMappings = new LinkedHashMap<>();
            Matcher codeValueMatcher = CODE_VALUE_PATTERN.matcher(commentLine);
            while (codeValueMatcher.find()) {
                String value = safeText(codeValueMatcher.group(1)).trim();
                String meaning = safeText(codeValueMatcher.group(2)).trim();
                if (isPlausibleCodeValue(value) && !meaning.isEmpty()) {
                    valueMappings.put(value, meaning);
                }
            }
            if (valueMappings.isEmpty()) {
                continue;
            }

            Map<String, Object> mapping = new LinkedHashMap<>();
            mapping.put("code", codeField);
            mapping.put("description", cleanBusinessText(trimCommentLine(commentLine)));
            mapping.put("mappings", valueMappings);
            mappings.add(mapping);
        }
        return mappings;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildCodeMappingsFromFieldHints(JsonNode fieldHintsNode) {
        Map<String, Map<String, Object>> mappingsByCode = new LinkedHashMap<>();
        if (fieldHintsNode == null || !fieldHintsNode.isArray()) {
            return new ArrayList<>();
        }

        for (JsonNode fieldHint : fieldHintsNode) {
            String fieldName = readText(fieldHint.path("field"), "");
            String meaning = cleanBusinessText(readText(fieldHint.path("meaning_hint"), ""));
            String joined = (fieldName + " " + meaning).toUpperCase(Locale.ROOT);
            Map<String, String> extractedMappings = readStringMap(fieldHint.path("extracted_mappings"));
            boolean looksLikeCodeField = joined.contains("_CD")
                    || joined.contains("_CODE")
                    || joined.contains("_FLAG")
                    || joined.contains("_TYPE")
                    || joined.contains("码");
            if (!looksLikeCodeField && extractedMappings.isEmpty()) {
                continue;
            }
            if (extractedMappings.isEmpty() && isNoisyMeaningHint(meaning)) {
                continue;
            }
            String code = fieldName.isBlank() ? "UNKNOWN_CODE" : fieldName;
            Map<String, Object> mapping = mappingsByCode.computeIfAbsent(code, key -> {
                Map<String, Object> created = new LinkedHashMap<>();
                created.put("code", key);
                created.put("description", meaning);
                created.put("mappings", new LinkedHashMap<String, String>());
                return created;
            });
            if (Objects.toString(mapping.get("description"), "").isBlank() && !meaning.isBlank()) {
                mapping.put("description", meaning);
            }
            Map<String, String> values = (Map<String, String>) mapping.get("mappings");
            values.putAll(extractedMappings);
        }
        return new ArrayList<>(mappingsByCode.values());
    }

    private List<String> extractSqlComments(String rawText) {
        LinkedHashSet<String> comments = new LinkedHashSet<>();
        String safe = safeText(rawText);

        Matcher lineMatcher = SQL_LINE_COMMENT_PATTERN.matcher(safe);
        while (lineMatcher.find()) {
            String line = trimCommentLine(lineMatcher.group(1));
            if (!line.isEmpty()) {
                comments.add(line);
            }
        }
        Matcher blockMatcher = SQL_BLOCK_COMMENT_PATTERN.matcher(safe);
        while (blockMatcher.find()) {
            String block = safeText(blockMatcher.group(1)).replace("\r\n", "\n").replace('\r', '\n');
            String[] lines = block.split("\n");
            for (String line : lines) {
                String normalized = trimCommentLine(line);
                if (!normalized.isEmpty()) {
                    comments.add(normalized);
                }
            }
        }
        return new ArrayList<>(comments);
    }

    private String trimCommentLine(String line) {
        return safeText(line)
                .replaceFirst("^\\s*(?:--+|/\\*+|\\*+/|\\*)\\s*", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isPlausibleCodeValue(String value) {
        String safe = trimOrEmpty(value);
        if (safe.isBlank() || safe.length() > 8) {
            return false;
        }
        if (!safe.matches("[A-Za-z0-9]+")) {
            return false;
        }
        return !safe.matches("[A-Za-z]{4,}");
    }

    private List<String> buildSourceTablesFromPrep(JsonNode tableHintsNode, List<Map<String, Object>> sqlBlocks) {
        LinkedHashSet<String> tables = new LinkedHashSet<>();
        if (tableHintsNode.isArray()) {
            for (JsonNode tableHint : tableHintsNode) {
                String table = readText(tableHint.path("table"));
                if (table != null) {
                    tables.add(table);
                }
            }
        }
        for (Map<String, Object> sqlBlock : sqlBlocks) {
            Object sourceTables = sqlBlock.get("source_tables");
            if (sourceTables instanceof List<?> sourceTableList) {
                for (Object table : sourceTableList) {
                    String text = Objects.toString(table, "").trim();
                    if (!text.isEmpty()) {
                        tables.add(text);
                    }
                }
            }
        }
        if (!tables.isEmpty()) {
            return new ArrayList<>(tables);
        }
        List<String> sqlBlocksText = sqlBlocks.stream()
                .map(item -> Objects.toString(item.get("sql_text"), ""))
                .toList();
        return extractSourceTables(sqlBlocksText);
    }

    private List<Map<String, Object>> buildCaveatsFromPrep(JsonNode riskNotesNode) {
        List<Map<String, Object>> caveats = new ArrayList<>();
        if (!riskNotesNode.isArray()) {
            return caveats;
        }
        for (JsonNode riskNote : riskNotesNode) {
            String text = readText(riskNote.path("text"));
            if (text == null) {
                continue;
            }
            Map<String, Object> caveat = new LinkedHashMap<>();
            caveat.put("title", readText(riskNote.path("title"), text.length() > 20 ? text.substring(0, 20) : text));
            caveat.put("text", text);
            caveat.put("risk", normalizeRisk(readText(riskNote.path("risk"), "MEDIUM")));
            caveats.add(caveat);
        }
        return caveats;
    }

    private Map<String, Object> buildQualityFromPrep(JsonNode prepRoot,
                                                     List<Map<String, Object>> sqlBlocks,
                                                     List<String> unresolved) {
        Map<String, Object> quality = new LinkedHashMap<>();
        double confidence = prepRoot.path("quality").path("confidence").asDouble(sqlBlocks.isEmpty() ? 0.65 : 0.85);
        quality.put("confidence", Math.max(0.0, Math.min(confidence, 1.0)));
        List<String> warnings = mergeDistinct(readTextArray(prepRoot.path("quality").path("warnings")), unresolved);
        if (confidence < 0.70) {
            warnings = mergeDistinct(warnings, List.of("confidence_below_threshold"));
        }
        quality.put("warnings", warnings);
        quality.put("errors", readTextArray(prepRoot.path("quality").path("errors")));
        return quality;
    }

    private boolean isLowConfidenceQuality(Map<String, Object> quality) {
        if (quality == null) {
            return true;
        }
        Object confidenceValue = quality.get("confidence");
        if (confidenceValue instanceof Number number) {
            return number.doubleValue() < 0.70;
        }
        return true;
    }

    private String mergeTextBlocks(String first, String second) {
        String left = safeText(first).trim();
        String right = safeText(second).trim();
        if (left.isBlank()) {
            return right;
        }
        if (right.isBlank()) {
            return left;
        }
        if (left.equals(right)) {
            return left;
        }
        return left + "\n\n" + right;
    }

    private String normalizeRisk(String risk) {
        String normalized = safeText(risk).trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LOW", "MEDIUM", "HIGH" -> normalized;
            default -> "MEDIUM";
        };
    }

    private Map<String, Object> buildGlobalBlock(String rawText, List<String> sourceTables, String sourceName) {
        Map<String, Object> global = new LinkedHashMap<>();
        global.put("domain_guess", guessDomain(rawText, sourceName));
        global.put("domain_overview_hint", extractDomainOverview(rawText));
        global.put("common_tables_hint", sourceTables);
        global.put("keywords", sourceTables);
        global.put("notes", new ArrayList<>());
        return global;
    }

    private Map<String, Object> buildSceneBlock(String rawText,
                                                String sceneTitle,
                                                List<String> sqlBlocks,
                                                List<String> sourceTables,
                                                List<String> warnings,
                                                boolean lowConfidence) {
        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("scene_title", sceneTitle);
        scene.put("scene_code_guess", "");
        scene.put("domain_guess", null);
        scene.put("contributors", new ArrayList<>());
        scene.put("owner_guess", null);
        String sceneDescription = buildSceneDescription(rawText, sceneTitle);
        scene.put("scene_description", sceneDescription);
        scene.put("caliber_definition", extractCaliberDefinition(rawText, sceneTitle));
        scene.put("applicability", "");
        scene.put("boundaries", "");
        scene.put("entities", new ArrayList<>());
        scene.put("source_evidence_lines", new ArrayList<>());
        scene.put("inputs", buildInputsBlock(sqlBlocks));
        scene.put("outputs", buildOutputsBlock(sqlBlocks));
        scene.put("sql_variants", buildSqlBlockList(sqlBlocks));
        scene.put("code_mappings", buildCodeMappings(rawText, null));
        scene.put("caveats", new ArrayList<>());
        scene.put("unmapped_text", removeDuplicateSegment(extractUnmappedText(rawText, sceneTitle), sceneDescription));
        scene.put("quality", buildQualityBlock(sqlBlocks, warnings, lowConfidence));
        return scene;
    }

    private Map<String, Object> buildInputsBlock(List<String> sqlBlocks) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        List<Map<String, Object>> params = new ArrayList<>();
        for (String field : extractWhereFieldsFromSqlText(sqlBlocks)) {
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("name_en", "");
            param.put("name_zh", field);
            param.put("type", "STRING");
            param.put("required", false);
            param.put("example", "");
            param.put("description", "由 SQL 过滤条件识别");
            params.add(param);
        }
        inputs.put("params", params);
        inputs.put("constraints", new ArrayList<>());
        return inputs;
    }

    private Map<String, Object> buildOutputsBlock(List<String> sqlBlocks) {
        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("summary", "");
        List<Map<String, Object>> fields = new ArrayList<>();
        for (Map<String, String> field : extractSelectFieldsFromSqlText(sqlBlocks)) {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("display_name", field.getOrDefault("display_name", ""));
            output.put("source_table", field.getOrDefault("source_table", ""));
            output.put("source_field", field.getOrDefault("source_field", ""));
            output.put("sensitivity_hint", "");
            output.put("mask_rule_suggest", "");
            fields.add(output);
        }
        outputs.put("fields", fields);
        return outputs;
    }

    private List<Map<String, Object>> buildSqlBlockList(List<String> sqlBlocks) {
        List<Map<String, Object>> blocks = new ArrayList<>();
        for (int i = 0; i < sqlBlocks.size(); i++) {
            Map<String, Object> block = new LinkedHashMap<>();
            block.put("variant_name", "取数方案" + (i + 1));
            block.put("applicable_period", "");
            block.put("description", "");
            block.put("sql_text", sqlBlocks.get(i));
            block.put("source_tables", extractSourceTables(List.of(sqlBlocks.get(i))));
            block.put("source_spans", new ArrayList<>());
            block.put("notes", "");
            blocks.add(block);
        }
        return blocks;
    }

    private Map<String, Object> buildQualityBlock(List<String> sqlBlocks, List<String> warnings, boolean lowConfidence) {
        Map<String, Object> quality = new LinkedHashMap<>();
        if (lowConfidence) {
            quality.put("confidence", RULE_FALLBACK_CONFIDENCE);
        } else {
            quality.put("confidence", sqlBlocks.isEmpty() ? 0.65 : 0.85);
        }
        quality.put("warnings", warnings);
        quality.put("errors", new ArrayList<>());
        return quality;
    }

    private Map<String, Object> buildParseReportBlock(List<String> warnings) {
        Map<String, Object> parseReport = new LinkedHashMap<>();
        parseReport.put("parser", "rule-plus-llm-fallback-v2");
        parseReport.put("warnings", warnings);
        parseReport.put("errors", new ArrayList<>());
        return parseReport;
    }

    private Map<String, Object> buildMetaBlock(String mode) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("schema_id", "caliber.import.v2");
        meta.put("schema_version", SCHEMA_VERSION);
        meta.put("mode", mode);
        meta.put("lang", "zh");
        return meta;
    }

    private String buildSceneDescription(String rawText, String sceneTitle) {
        String section = extractSectionByHeading(rawText, sceneTitle);
        if (!section.isBlank()) {
            return abbreviate(cleanBusinessText(section), 800);
        }
        String safe = safeText(rawText);
        if (safe.isBlank()) {
            return "";
        }
        return abbreviate(cleanBusinessText(safe), 400);
    }

    private String extractCaliberDefinition(String rawText, String sceneTitle) {
        String section = extractSectionByHeading(rawText, sceneTitle);
        if (!section.isBlank()) {
            return abbreviate(section, 1500);
        }
        return "";
    }

    private String extractUnmappedText(String rawText, String sceneTitle) {
        String section = extractSectionByHeading(rawText, sceneTitle);
        String base;
        if (section.isBlank()) {
            base = safeText(rawText);
        } else {
            String safe = safeText(rawText);
            int start = safe.indexOf(section);
            if (start <= 0) {
                base = "";
            } else {
                base = safe.substring(0, start).trim();
            }
        }
        return cleanBusinessText(base);
    }

    private String extractSceneTitle(String rawText, String sourceName) {
        String titleFromSourceName = extractSceneTitleFromSourceName(sourceName);
        if (!titleFromSourceName.isBlank()) {
            return titleFromSourceName;
        }

        String queryHeading = extractQuerySceneHeading(rawText);
        if (!queryHeading.isBlank()) {
            return queryHeading;
        }

        String safe = safeText(rawText);
        if (safe.isBlank()) {
            return "未命名场景";
        }
        List<String> genericTitles = List.of("业务概述", "业务定义", "术语释义", "常见数据表", "常见业务场景");
        for (String line : safe.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String normalized = trimmed.replaceFirst("^#+\\s*", "")
                    .replaceFirst("^[\\-*\\d.)\\s]+", "")
                    .replace("：", "")
                    .trim();
            if (normalized.isEmpty()) {
                continue;
            }
            if (genericTitles.contains(normalized)) {
                continue;
            }
            return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
        }
        return "未命名场景";
    }

    private String extractSceneTitleFromSourceName(String sourceName) {
        String safe = safeText(sourceName).trim();
        if (safe.isBlank()) {
            return "";
        }
        String fileName = safe.replace('\\', '/');
        int slashIdx = fileName.lastIndexOf('/');
        if (slashIdx >= 0 && slashIdx < fileName.length() - 1) {
            fileName = fileName.substring(slashIdx + 1);
        }
        fileName = fileName.replaceFirst("\\.[A-Za-z0-9]+$", "");
        fileName = fileName.replaceAll("[_\\-]+", " ");

        Matcher matcher = SOURCE_NAME_SCENE_PATTERN.matcher(fileName);
        String selected = "";
        while (matcher.find()) {
            String candidate = safeText(matcher.group(1)).trim();
            if (candidate.length() > selected.length()) {
                selected = candidate;
            }
        }
        if (!selected.isBlank()) {
            return selected;
        }
        return "";
    }

    private String extractQuerySceneHeading(String rawText) {
        Matcher matcher = QUERY_SCENE_HEADING_PATTERN.matcher(safeText(rawText));
        while (matcher.find()) {
            String candidate = safeText(matcher.group(1)).trim();
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }

    private String extractSectionByHeading(String rawText, String heading) {
        String safe = safeText(rawText);
        if (safe.isBlank()) {
            return "";
        }
        String normalizedHeading = safeText(heading).trim();
        if (normalizedHeading.isBlank()) {
            return "";
        }
        String[] lines = safe.split("\\R");
        int startLine = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim().replaceFirst("^#+\\s*", "").trim();
            if (line.equals(normalizedHeading)) {
                startLine = i;
                break;
            }
        }
        if (startLine < 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = startLine; i < lines.length; i++) {
            String line = lines[i];
            if (i > startLine && line.trim().startsWith("### ")) {
                break;
            }
            builder.append(line).append('\n');
        }
        return builder.toString().trim();
    }

    private String guessDomain(String rawText, String sourceName) {
        String merged = (safeText(sourceName) + "\n" + safeText(rawText)).toLowerCase(Locale.ROOT);
        if (merged.contains("零售")) {
            return "零售";
        }
        if (merged.contains("对公")) {
            return "对公";
        }
        if (merged.contains("运营")) {
            return "运营";
        }
        return null;
    }

    private String extractDomainOverview(String rawText) {
        String safe = safeText(rawText);
        if (safe.isBlank()) {
            return "";
        }
        int boundary = safe.indexOf("## 常见业务场景");
        if (boundary < 0) {
            Matcher matcher = QUERY_SCENE_HEADING_PATTERN.matcher(safe);
            if (matcher.find()) {
                boundary = matcher.start();
            }
        }
        String overview = boundary > 0 ? safe.substring(0, boundary) : safe;
        return abbreviate(overview.trim(), 1500);
    }

    private List<String> extractWhereFields(List<Map<String, Object>> sqlBlocks) {
        return extractWhereFieldsFromSqlText(extractSqlTextList(sqlBlocks));
    }

    private List<String> extractWhereFieldsFromSqlText(List<String> sqlBlocks) {
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        for (String sql : sqlBlocks) {
            String whereClause = extractWhereClause(sql);
            if (whereClause.isBlank()) {
                continue;
            }
            Matcher matcher = SQL_WHERE_CONDITION_PATTERN.matcher(whereClause);
            while (matcher.find()) {
                String field = safeText(matcher.group(1)).trim();
                if (!field.isBlank()) {
                    fields.add(field);
                }
            }
        }
        return new ArrayList<>(fields);
    }

    private List<Map<String, String>> extractSelectFields(List<Map<String, Object>> sqlBlocks) {
        return extractSelectFieldsFromSqlText(extractSqlTextList(sqlBlocks));
    }

    private List<Map<String, String>> extractSelectFieldsFromSqlText(List<String> sqlBlocks) {
        List<Map<String, String>> fields = new ArrayList<>();
        LinkedHashSet<String> dedupe = new LinkedHashSet<>();
        for (String sql : sqlBlocks) {
            String sourceTable = extractSourceTables(List.of(sql)).stream().findFirst().orElse("");
            String selectBody = extractSelectBody(sql);
            if (selectBody.isBlank()) {
                continue;
            }
            for (String item : splitSqlSelectItems(selectBody)) {
                String cleaned = item.replaceAll("(?m)--.*$", "").trim();
                if (cleaned.isBlank()) {
                    continue;
                }
                String displayName = extractOutputName(cleaned);
                if (displayName.isBlank() || dedupe.contains(displayName)) {
                    continue;
                }
                dedupe.add(displayName);
                Map<String, String> output = new LinkedHashMap<>();
                output.put("display_name", abbreviate(displayName, 120));
                output.put("source_table", sourceTable);
                output.put("source_field", abbreviate(cleaned, 180));
                fields.add(output);
                if (fields.size() >= 120) {
                    return fields;
                }
            }
        }
        return fields;
    }

    private List<String> extractSqlTextList(List<Map<String, Object>> sqlBlocks) {
        List<String> sqlTexts = new ArrayList<>();
        for (Map<String, Object> sqlBlock : sqlBlocks) {
            String sqlText = Objects.toString(sqlBlock.get("sql_text"), "").trim();
            if (!sqlText.isBlank()) {
                sqlTexts.add(sqlText);
            }
        }
        return sqlTexts;
    }

    private String extractWhereClause(String sql) {
        Matcher matcher = Pattern.compile("(?is)\\bwhere\\b([\\s\\S]*?)(?:\\border\\b|\\bgroup\\b|\\blimit\\b|;|$)").matcher(safeText(sql));
        if (matcher.find()) {
            return safeText(matcher.group(1));
        }
        return "";
    }

    private String extractSelectBody(String sql) {
        Matcher matcher = Pattern.compile("(?is)\\bselect\\b([\\s\\S]*?)\\bfrom\\b").matcher(safeText(sql));
        if (matcher.find()) {
            return safeText(matcher.group(1));
        }
        return "";
    }

    private List<String> splitSqlSelectItems(String selectBody) {
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenthesesDepth = 0;
        for (int i = 0; i < selectBody.length(); i++) {
            char ch = selectBody.charAt(i);
            if (ch == '(') {
                parenthesesDepth += 1;
            } else if (ch == ')' && parenthesesDepth > 0) {
                parenthesesDepth -= 1;
            }
            if (ch == ',' && parenthesesDepth == 0) {
                items.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        if (!current.isEmpty()) {
            items.add(current.toString());
        }
        return items;
    }

    private String extractOutputName(String selectItem) {
        String normalized = safeText(selectItem).trim();
        if (normalized.isBlank()) {
            return "";
        }
        Matcher aliasMatcher = SQL_OUTPUT_ALIAS_PATTERN.matcher(normalized);
        if (aliasMatcher.find()) {
            return aliasMatcher.group(1).trim();
        }
        Matcher asMatcher = SQL_OUTPUT_AS_PATTERN.matcher(normalized);
        if (asMatcher.find()) {
            return asMatcher.group(1).trim();
        }
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < normalized.length() - 1) {
            return normalized.substring(dotIndex + 1).trim();
        }
        int spaceIndex = normalized.lastIndexOf(' ');
        if (spaceIndex >= 0 && spaceIndex < normalized.length() - 1) {
            return normalized.substring(spaceIndex + 1).trim();
        }
        return normalized;
    }

    private String cleanBusinessText(String text) {
        String safe = safeText(text);
        if (safe.isBlank()) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        for (String line : safe.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String normalized = line == null ? "" : line.trim();
            if (normalized.isBlank()) {
                continue;
            }
            normalized = normalized.replace("/*", "").replace("*/", "").trim();
            normalized = normalized.replaceAll("^[#>*\\-\\s]+", "").trim();
            if (normalized.isBlank() || DECORATION_LINE_PATTERN.matcher(normalized).matches()) {
                continue;
            }
            if (isSqlNoiseLine(normalized)) {
                continue;
            }
            lines.add(normalized);
        }
        return String.join("\n", lines).replaceAll("\\n{3,}", "\n\n").trim();
    }

    private boolean isSqlNoiseLine(String line) {
        String normalized = trimOrEmpty(line);
        if (normalized.isEmpty()) {
            return false;
        }
        if (normalized.startsWith("```")) {
            return true;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if ("SQL".equals(upper) || "SQL语句".equals(upper) || "SQL 语句".equals(upper)) {
            return true;
        }
        if (SQL_START_LINE_PATTERN.matcher(normalized).find()
                || SQL_CLAUSE_LINE_PATTERN.matcher(normalized).find()
                || SQL_FIELD_ALIAS_LINE_PATTERN.matcher(normalized).find()
                || SQL_INLINE_SELECT_FROM_PATTERN.matcher(normalized).find()) {
            return true;
        }
        return upper.contains(" AS ")
                && normalized.contains(".")
                && normalized.matches(".*[A-Za-z_][A-Za-z0-9_$]*\\.[A-Za-z_][A-Za-z0-9_$].*");
    }

    private boolean isNoisyMeaningHint(String meaning) {
        String safe = safeText(meaning).trim();
        if (safe.isBlank()) {
            return true;
        }
        if (safe.length() > 120) {
            return true;
        }
        String upper = safe.toUpperCase(Locale.ROOT);
        return upper.contains("业务场景描述")
                || upper.contains("====")
                || upper.contains("/*")
                || upper.contains("*/");
    }

    private String removeDuplicateSegment(String source, String segment) {
        String left = cleanBusinessText(source);
        String right = cleanBusinessText(segment);
        if (left.isBlank() || right.isBlank()) {
            return left;
        }
        if (left.contains(right)) {
            return left.replace(right, "").trim();
        }
        if (right.contains(left)) {
            return "";
        }
        return left;
    }

    private String abbreviate(String text, int maxLength) {
        String safe = safeText(text);
        if (safe.length() <= maxLength) {
            return safe;
        }
        return safe.substring(0, maxLength);
    }

    private List<String> extractSqlBlocks(String rawText) {
        String safe = safeText(rawText);
        if (safe.isBlank()) {
            return new ArrayList<>();
        }

        LinkedHashSet<String> sqlSet = new LinkedHashSet<>();
        Matcher codeBlockMatcher = SQL_CODE_BLOCK_PATTERN.matcher(safe);
        while (codeBlockMatcher.find()) {
            String sql = codeBlockMatcher.group(1);
            if (sql != null && !sql.isBlank()) {
                List<String> statements = splitSqlStatementsFromCodeBlock(sql);
                if (statements.isEmpty()) {
                    sqlSet.add(sql.trim());
                } else {
                    for (String statement : statements) {
                        String normalized = trimOrEmpty(statement);
                        if (!normalized.isBlank()) {
                            sqlSet.add(normalized);
                        }
                    }
                }
            }
        }
        if (!sqlSet.isEmpty()) {
            return new ArrayList<>(sqlSet);
        }

        Matcher statementMatcher = SQL_STATEMENT_PATTERN.matcher(safe);
        while (statementMatcher.find()) {
            String sql = statementMatcher.group(1);
            if (sql != null && !sql.isBlank()) {
                sqlSet.add(sql.trim());
            }
        }
        if (!sqlSet.isEmpty()) {
            return new ArrayList<>(sqlSet);
        }
        for (String statement : splitSqlStatementsFromCodeBlock(safe)) {
            String normalized = trimOrEmpty(statement);
            if (!normalized.isBlank()) {
                sqlSet.add(normalized);
            }
        }
        return new ArrayList<>(sqlSet);
    }

    private List<String> splitSqlStatementsFromCodeBlock(String blockText) {
        String safe = safeText(blockText);
        if (safe.isBlank()) {
            return new ArrayList<>();
        }
        List<String> statements = extractSqlStatementsByLineScan(safe);
        if (statements.size() > 1) {
            return statements;
        }
        LinkedHashSet<String> fallback = new LinkedHashSet<>();
        Matcher statementMatcher = SQL_STATEMENT_PATTERN.matcher(safe);
        while (statementMatcher.find()) {
            String sql = trimOrEmpty(statementMatcher.group(1));
            if (!sql.isBlank()) {
                fallback.add(sql);
            }
        }
        if (!fallback.isEmpty()) {
            return new ArrayList<>(fallback);
        }
        return statements;
    }

    private List<String> extractSqlStatementsByLineScan(String text) {
        List<String> statements = new ArrayList<>();
        List<String> pendingCommentLines = new ArrayList<>();
        StringBuilder current = null;
        String[] lines = safeText(text).replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        boolean previousBlank = true;
        for (String line : lines) {
            String trimmed = trimOrEmpty(line);
            boolean blank = trimmed.isBlank();
            boolean comment = isSqlCommentLine(trimmed);
            boolean sqlStart = SQL_START_LINE_PATTERN.matcher(trimmed).find();

            if (current == null) {
                if (blank) {
                    if (!pendingCommentLines.isEmpty()) {
                        pendingCommentLines.add("");
                    }
                    previousBlank = true;
                    continue;
                }
                if (comment) {
                    pendingCommentLines.add(line);
                    previousBlank = false;
                    continue;
                }
                if (sqlStart) {
                    current = new StringBuilder();
                    appendPendingComments(current, pendingCommentLines);
                    pendingCommentLines.clear();
                    current.append(line).append('\n');
                    if (trimmed.endsWith(";")) {
                        statements.add(trimOrEmpty(current.toString()));
                        current = null;
                    }
                    previousBlank = false;
                    continue;
                }
                pendingCommentLines.clear();
                previousBlank = false;
                continue;
            }

            if (sqlStart && previousBlank) {
                statements.add(trimOrEmpty(current.toString()));
                current = new StringBuilder();
                appendPendingComments(current, pendingCommentLines);
                pendingCommentLines.clear();
                current.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    statements.add(trimOrEmpty(current.toString()));
                    current = null;
                }
                previousBlank = false;
                continue;
            }

            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                statements.add(trimOrEmpty(current.toString()));
                current = null;
            }
            previousBlank = blank;
        }
        if (current != null && !trimOrEmpty(current.toString()).isBlank()) {
            statements.add(trimOrEmpty(current.toString()));
        }
        List<String> normalized = new ArrayList<>();
        for (String statement : statements) {
            String safeStatement = trimOrEmpty(statement);
            if (!safeStatement.isBlank() && SQL_START_LINE_PATTERN.matcher(safeStatement).find()) {
                normalized.add(safeStatement);
            }
        }
        return normalized;
    }

    private void appendPendingComments(StringBuilder builder, List<String> pendingComments) {
        if (pendingComments.isEmpty()) {
            return;
        }
        for (String pendingComment : pendingComments) {
            builder.append(pendingComment).append('\n');
        }
    }

    private boolean isSqlCommentLine(String trimmedLine) {
        return trimmedLine.startsWith("--")
                || trimmedLine.startsWith("/*")
                || trimmedLine.startsWith("*")
                || trimmedLine.startsWith("*/");
    }

    private Map<String, String> buildSqlFingerprintMap(List<String> sqlBlocks) {
        Map<String, String> byFingerprint = new LinkedHashMap<>();
        for (String sql : sqlBlocks) {
            String fingerprint = sqlFingerprint(sql);
            if (!fingerprint.isEmpty() && !byFingerprint.containsKey(fingerprint)) {
                byFingerprint.put(fingerprint, sql);
            }
        }
        return byFingerprint;
    }

    private String resolveExactSqlFromSource(String sqlRaw, Map<String, String> sourceSqlByFingerprint) {
        String fingerprint = sqlFingerprint(sqlRaw);
        if (fingerprint.isEmpty()) {
            return null;
        }
        return sourceSqlByFingerprint.get(fingerprint);
    }

    private String sqlFingerprint(String sql) {
        String normalized = safeText(sql).replaceAll("\\s+", "");
        if (normalized.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return toHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("sha-256 unavailable", ex);
        }
    }

    private String toHex(byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte item : value) {
            builder.append(Character.forDigit((item >> 4) & 0xf, 16));
            builder.append(Character.forDigit(item & 0xf, 16));
        }
        return builder.toString();
    }

    private List<String> extractSourceTables(List<String> sqlBlocks) {
        LinkedHashSet<String> tableSet = new LinkedHashSet<>();
        for (String sql : sqlBlocks) {
            SqlTableParseSupport.ParseResult parsed = SqlTableParseSupport.parse(sql);
            tableSet.addAll(parsed.sourceTables());
        }
        return new ArrayList<>(tableSet);
    }

    private List<String> buildWarnings(String rawText, List<String> sqlBlocks, List<String> extraWarnings) {
        List<String> warnings = new ArrayList<>();
        if (safeText(rawText).isBlank()) {
            warnings.add("raw_text_empty");
        }
        if (sqlBlocks.isEmpty()) {
            warnings.add("sql_not_detected");
        }
        warnings.addAll(extraWarnings);
        return mergeDistinct(warnings, List.of());
    }

    private List<String> mergeDistinct(List<String> first, List<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (String item : first) {
            if (item != null && !item.isBlank()) {
                merged.add(item.trim());
            }
        }
        for (String item : second) {
            if (item != null && !item.isBlank()) {
                merged.add(item.trim());
            }
        }
        return new ArrayList<>(merged);
    }

    private List<String> readTextArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (!node.isArray()) {
            return values;
        }
        for (JsonNode item : node) {
            if (item.isTextual()) {
                String text = item.asText("").trim();
                if (!text.isEmpty()) {
                    values.add(text);
                }
            }
        }
        return values;
    }

    private List<Integer> readIntArray(JsonNode node) {
        List<Integer> values = new ArrayList<>();
        if (!node.isArray()) {
            return values;
        }
        for (JsonNode item : node) {
            Integer value = readPositiveInt(item);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private List<Map<String, Integer>> readSourceSpans(JsonNode node) {
        List<Map<String, Integer>> spans = new ArrayList<>();
        if (!node.isArray()) {
            return spans;
        }
        for (JsonNode item : node) {
            if (!item.isObject()) {
                continue;
            }
            Integer startLine = readPositiveInt(item.path("start_line"));
            Integer endLine = readPositiveInt(item.path("end_line"));
            if (startLine == null || endLine == null) {
                continue;
            }
            if (startLine <= 0 || endLine <= 0) {
                continue;
            }
            Map<String, Integer> span = new LinkedHashMap<>();
            span.put("start_line", Math.min(startLine, endLine));
            span.put("end_line", Math.max(startLine, endLine));
            spans.add(span);
        }
        return spans;
    }

    private String readText(JsonNode node) {
        if (node == null || !node.isTextual()) {
            return null;
        }
        String value = node.asText("").trim();
        return value.isEmpty() ? null : value;
    }

    private String readText(JsonNode node, String defaultValue) {
        String value = readText(node);
        return value == null ? defaultValue : value;
    }

    private Integer readPositiveInt(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            int value = node.asInt();
            return value > 0 ? value : null;
        }
        if (node.isTextual()) {
            Matcher matcher = Pattern.compile("(\\d{1,6})").matcher(node.asText(""));
            if (matcher.find()) {
                int value = Integer.parseInt(matcher.group(1));
                return value > 0 ? value : null;
            }
        }
        return null;
    }

    private Map<String, String> readStringMap(JsonNode node) {
        Map<String, String> mappings = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return mappings;
        }
        node.fields().forEachRemaining(entry -> {
            String key = safeText(entry.getKey()).trim();
            String value = safeText(entry.getValue().asText("")).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                mappings.put(key, value);
            }
        });
        return mappings;
    }

    private void enforceLlmInputLineLimit(String rawText) {
        int lineCount = countLines(rawText);
        if (lineCount > MAX_LLM_INPUT_LINES) {
            throw new IllegalStateException("文本过长，请分段导入（当前行数：" + lineCount + "，上限：" + MAX_LLM_INPUT_LINES + "）");
        }
    }

    private int countLines(String text) {
        String safe = safeText(text);
        if (safe.isEmpty()) {
            return 0;
        }
        return safe.split("\\R", -1).length;
    }

    private String buildLineAnchoredRawDoc(String rawText) {
        String safe = safeText(rawText);
        if (safe.isBlank()) {
            return safe;
        }
        String[] lines = safe.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        int width = Math.max(3, String.valueOf(lines.length).length());
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String prefix = String.format(Locale.ROOT, "[%0" + width + "d] ", i + 1);
            builder.append(prefix).append(lines[i]);
            if (i < lines.length - 1) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private String stripDocLinePrefix(String text) {
        String safe = safeText(text);
        if (safe.isBlank()) {
            return safe;
        }
        String[] lines = safe.split("\\R", -1);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = DOC_LINE_PREFIX_PATTERN.matcher(lines[i]).replaceFirst("");
            builder.append(line);
            if (i < lines.length - 1) {
                builder.append('\n');
            }
        }
        return builder.toString().trim();
    }

    private RuntimeConfig resolveRuntimeConfig() {
        Optional<LlmPreprocessConfig> persisted = llmPreprocessConfigDomainSupport.findSingleton();
        if (persisted.isEmpty()) {
            return new RuntimeConfig(
                    llmPreprocessProperties.isEnabled(),
                    trimOrEmpty(llmPreprocessProperties.getEndpoint()),
                    trimOrEmpty(llmPreprocessProperties.getModel()),
                    trimOrEmpty(llmPreprocessProperties.getApiKey()),
                    llmPreprocessProperties.getTimeoutSeconds(),
                    llmPreprocessProperties.getTemperature(),
                    llmPreprocessProperties.getMaxTokens(),
                    llmPreprocessProperties.isEnableThinking(),
                    llmPreprocessProperties.isFallbackToRule(),
                    llmPreprocessProperties.getRateLimitPerMinute(),
                    llmPreprocessProperties.getCircuitBreakerFailureThreshold(),
                    llmPreprocessProperties.getCircuitBreakerOpenSeconds(),
                    PREP_SYSTEM_PROMPT,
                    PREP_USER_PROMPT_TEMPLATE,
                    llmPrepSchemaJsonGenerator.generateSchemaJson()
            );
        }
        LlmPreprocessConfig config = persisted.get();
        return new RuntimeConfig(
                Boolean.TRUE.equals(config.getEnabled()),
                trimOrEmpty(config.getEndpoint()),
                trimOrEmpty(config.getModel()),
                decryptKey(config.getApiKeyCiphertext()),
                config.getTimeoutSeconds() == null ? llmPreprocessProperties.getTimeoutSeconds() : config.getTimeoutSeconds(),
                config.getTemperature() == null ? llmPreprocessProperties.getTemperature() : config.getTemperature(),
                config.getMaxTokens() == null ? llmPreprocessProperties.getMaxTokens() : config.getMaxTokens(),
                config.getEnableThinking() == null ? llmPreprocessProperties.isEnableThinking() : config.getEnableThinking(),
                config.getFallbackToRule() == null ? llmPreprocessProperties.isFallbackToRule() : config.getFallbackToRule(),
                llmPreprocessProperties.getRateLimitPerMinute(),
                llmPreprocessProperties.getCircuitBreakerFailureThreshold(),
                llmPreprocessProperties.getCircuitBreakerOpenSeconds(),
                trimOrEmpty(config.getPreprocessSystemPrompt()),
                trimOrEmpty(config.getPreprocessUserPromptTemplate()),
                trimOrEmpty(config.getPrepSchemaJson())
        );
    }

    private String decryptKey(String cipher) {
        if (cipher == null || cipher.isBlank()) {
            return "";
        }
        try {
            return llmSecretCodec.decrypt(cipher);
        } catch (IllegalStateException ex) {
            return cipher;
        }
    }

    private String trimOrEmpty(String text) {
        return text == null ? "" : text.trim();
    }

    private String normalizeSourceType(String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            return "PASTE_MD";
        }
        String normalized = sourceType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PASTE_MD", "PASTE_TEXT", "PASTE_TXT", "TEXT", "PLAIN_TEXT", "MARKDOWN" -> "PASTE_MD";
            case "FILE_MD", "MARKDOWN_FILE", "FILE_MARKDOWN" -> "FILE_MD";
            case "FILE_TXT", "FILE_TEXT", "TEXT_FILE", "FILE", "UPLOAD_FILE" -> "FILE_TXT";
            case "FILE_SQL", "SQL_FILE" -> "FILE_SQL";
            case "IMAGE_OCR_TEXT", "IMAGE", "IMG", "PICTURE", "OCR_TEXT", "IMAGE_TEXT" -> "IMAGE_OCR_TEXT";
            default -> "PASTE_MD";
        };
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to build json payload", ex);
        }
    }

    private record RuntimeConfig(boolean enabled,
                                 String endpoint,
                                 String model,
                                 String apiKey,
                                 int timeoutSeconds,
                                 double temperature,
                                 int maxTokens,
                                 boolean enableThinking,
                                 boolean fallbackToRule,
                                 int rateLimitPerMinute,
                                 int circuitBreakerFailureThreshold,
                                 int circuitBreakerOpenSeconds,
                                 String preprocessSystemPrompt,
                                 String preprocessUserPromptTemplate,
                                 String prepSchemaJson) {
    }

    private record PromptRenderResult(
            String systemPrompt,
            String userPromptTemplate,
            String prepSchemaJson,
            String normalizedSourceType,
            String userPrompt,
            List<String> warnings
    ) {
    }

    private record SqlBlocksBuildResult(
            List<Map<String, Object>> blocks,
            List<String> warnings,
            Map<String, Map<String, Object>> blocksBySegmentId
    ) {
    }

    private record RuleSceneCluster(
            String sceneTitle,
            List<String> sqlBlocks
    ) {
    }

    private record RuleSceneClusterBuilder(
            String sceneTitle,
            List<String> sqlBlocks
    ) {
        private RuleSceneClusterBuilder(String sceneTitle) {
            this(sceneTitle, new ArrayList<>());
        }
    }

    private static final class LlmRateLimitException extends RuntimeException {
        private LlmRateLimitException(String message) {
            super(message);
        }
    }

    private static final class LlmCircuitOpenException extends RuntimeException {
        private LlmCircuitOpenException(String message) {
            super(message);
        }
    }
}
