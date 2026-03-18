package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.FetchLlmModelListCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.PreviewLlmPromptCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.TestLlmPreprocessConfigCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateLlmPreprocessConfigCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateLlmPromptConfigCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.LlmModelListResultDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.LlmPreprocessConfigDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.LlmPromptPreviewDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.LlmPromptConfigDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.TestLlmPreprocessConfigResultDTO;
import com.cmbchina.datadirect.caliber.application.service.query.LlmPreprocessConfigQueryAppService;
import com.cmbchina.datadirect.caliber.application.support.LlmPreprocessSupport;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.domain.model.LlmPreprocessConfig;
import com.cmbchina.datadirect.caliber.domain.support.LlmPreprocessConfigDomainSupport;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmPromptDefaults;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmPromptFingerprint;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmPrepSchemaJsonGenerator;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmPreprocessProperties;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmSecretCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class LlmPreprocessConfigCommandAppService {

    private final LlmPreprocessConfigDomainSupport llmPreprocessConfigDomainSupport;
    private final LlmPreprocessConfigQueryAppService llmPreprocessConfigQueryAppService;
    private final LlmPreprocessProperties llmPreprocessProperties;
    private final LlmSecretCodec llmSecretCodec;
    private final LlmPreprocessSupport llmPreprocessSupport;
    private final LlmPrepSchemaJsonGenerator llmPrepSchemaJsonGenerator;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public LlmPreprocessConfigCommandAppService(LlmPreprocessConfigDomainSupport llmPreprocessConfigDomainSupport,
                                                LlmPreprocessConfigQueryAppService llmPreprocessConfigQueryAppService,
                                                LlmPreprocessProperties llmPreprocessProperties,
                                                LlmSecretCodec llmSecretCodec,
                                                LlmPreprocessSupport llmPreprocessSupport,
                                                LlmPrepSchemaJsonGenerator llmPrepSchemaJsonGenerator,
                                                ObjectMapper objectMapper) {
        this.llmPreprocessConfigDomainSupport = llmPreprocessConfigDomainSupport;
        this.llmPreprocessConfigQueryAppService = llmPreprocessConfigQueryAppService;
        this.llmPreprocessProperties = llmPreprocessProperties;
        this.llmSecretCodec = llmSecretCodec;
        this.llmPreprocessSupport = llmPreprocessSupport;
        this.llmPrepSchemaJsonGenerator = llmPrepSchemaJsonGenerator;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Transactional
    public LlmPreprocessConfigDTO update(UpdateLlmPreprocessConfigCmd cmd) {
        validate(cmd);

        LlmPreprocessConfig existing = llmPreprocessConfigDomainSupport.findSingleton()
                .orElseGet(this::defaultConfig);
        String finalCipher = resolveApiKeyCipher(existing, cmd);

        LlmPreprocessConfig config = LlmPreprocessConfig.builder()
                .id(1L)
                .enabled(cmd.enabled())
                .endpoint(trimOrEmpty(cmd.endpoint()))
                .apiKeyCiphertext(finalCipher)
                .model(trimOrEmpty(cmd.model()))
                .timeoutSeconds(cmd.timeoutSeconds())
                .temperature(cmd.temperature())
                .maxTokens(cmd.maxTokens())
                .enableThinking(resolveEnableThinking(existing, cmd.enableThinking()))
                .fallbackToRule(cmd.fallbackToRule())
                .preprocessSystemPrompt(existing.getPreprocessSystemPrompt())
                .preprocessUserPromptTemplate(existing.getPreprocessUserPromptTemplate())
                .prepSchemaJson(existing.getPrepSchemaJson())
                .promptVersion(resolvePromptVersion(existing.getPromptVersion()))
                .promptHash(trimOrEmpty(existing.getPromptHash()))
                .updatedBy(trimOrEmpty(cmd.operator()))
                .updatedAt(OffsetDateTime.now())
                .build();
        llmPreprocessConfigDomainSupport.save(config);
        return llmPreprocessConfigQueryAppService.getCurrentConfig();
    }

    @Transactional
    public LlmPromptConfigDTO updatePrompts(UpdateLlmPromptConfigCmd cmd) {
        validatePromptConfig(cmd);
        LlmPreprocessConfig existing = llmPreprocessConfigDomainSupport.findSingleton()
                .orElseGet(this::defaultConfig);
        long nextPromptVersion = resolvePromptVersion(existing.getPromptVersion()) + 1L;
        String promptHash = calculatePromptHash(
                cmd.preprocessSystemPrompt(),
                cmd.preprocessUserPromptTemplate(),
                cmd.prepSchemaJson()
        );
        LlmPreprocessConfig config = LlmPreprocessConfig.builder()
                .id(1L)
                .enabled(existing.getEnabled())
                .endpoint(trimOrEmpty(existing.getEndpoint()))
                .apiKeyCiphertext(existing.getApiKeyCiphertext())
                .model(trimOrEmpty(existing.getModel()))
                .timeoutSeconds(existing.getTimeoutSeconds())
                .temperature(existing.getTemperature())
                .maxTokens(existing.getMaxTokens())
                .enableThinking(existing.getEnableThinking())
                .fallbackToRule(existing.getFallbackToRule())
                .preprocessSystemPrompt(cmd.preprocessSystemPrompt())
                .preprocessUserPromptTemplate(cmd.preprocessUserPromptTemplate())
                .prepSchemaJson(cmd.prepSchemaJson())
                .promptVersion(nextPromptVersion)
                .promptHash(promptHash)
                .updatedBy(trimOrEmpty(cmd.operator()))
                .updatedAt(OffsetDateTime.now())
                .build();
        llmPreprocessConfigDomainSupport.save(config);
        return llmPreprocessConfigQueryAppService.getCurrentPromptConfig();
    }

    @Transactional
    public LlmPromptConfigDTO resetPrompts(String operator) {
        LlmPreprocessConfig existing = llmPreprocessConfigDomainSupport.findSingleton()
                .orElseGet(this::defaultConfig);
        String dynamicSchema = llmPrepSchemaJsonGenerator.generateSchemaJson();
        long nextPromptVersion = resolvePromptVersion(existing.getPromptVersion()) + 1L;
        String promptHash = calculatePromptHash(
                LlmPromptDefaults.PREPROCESS_SYSTEM_PROMPT,
                LlmPromptDefaults.PREPROCESS_USER_PROMPT_TEMPLATE,
                dynamicSchema
        );
        LlmPreprocessConfig config = LlmPreprocessConfig.builder()
                .id(1L)
                .enabled(existing.getEnabled())
                .endpoint(trimOrEmpty(existing.getEndpoint()))
                .apiKeyCiphertext(existing.getApiKeyCiphertext())
                .model(trimOrEmpty(existing.getModel()))
                .timeoutSeconds(existing.getTimeoutSeconds())
                .temperature(existing.getTemperature())
                .maxTokens(existing.getMaxTokens())
                .enableThinking(existing.getEnableThinking())
                .fallbackToRule(existing.getFallbackToRule())
                .preprocessSystemPrompt(LlmPromptDefaults.PREPROCESS_SYSTEM_PROMPT)
                .preprocessUserPromptTemplate(LlmPromptDefaults.PREPROCESS_USER_PROMPT_TEMPLATE)
                .prepSchemaJson(dynamicSchema)
                .promptVersion(nextPromptVersion)
                .promptHash(promptHash)
                .updatedBy(trimOrEmpty(operator))
                .updatedAt(OffsetDateTime.now())
                .build();
        llmPreprocessConfigDomainSupport.save(config);
        return llmPreprocessConfigQueryAppService.getCurrentPromptConfig();
    }

    public TestLlmPreprocessConfigResultDTO test(TestLlmPreprocessConfigCmd cmd) {
        long start = System.currentTimeMillis();
        LlmPreprocessConfig runtime = llmPreprocessConfigQueryAppService.resolveCurrentConfig();
        boolean llmEnabled = Boolean.TRUE.equals(runtime.getEnabled());
        String dynamicSchema = llmPrepSchemaJsonGenerator.generateSchemaJson();
        String effectiveSchema = trimOrEmpty(runtime.getPreprocessUserPromptTemplate()).contains("{{DYNAMIC_JSON_SCHEMA}}")
                ? dynamicSchema
                : runtime.getPrepSchemaJson();
        String promptFingerprint = LlmPromptFingerprint.of(
                runtime.getPreprocessSystemPrompt(),
                runtime.getPreprocessUserPromptTemplate(),
                effectiveSchema
        );
        try {
            String result = llmPreprocessSupport.preprocessToCaliberImportV2ByLlm(cmd.rawText(), cmd.sourceType());
            JsonNode root = objectMapper.readTree(result);
            JsonNode scenes = root.path("scenes");
            int sceneCount = scenes.isArray() ? scenes.size() : 0;
            int sqlCount = 0;
            if (sceneCount > 0 && scenes.get(0).path("sql_variants").isArray()) {
                sqlCount = scenes.get(0).path("sql_variants").size();
            }
            List<String> warnings = new ArrayList<>();
            JsonNode warningNode = root.path("parse_report").path("warnings");
            if (warningNode.isArray()) {
                for (JsonNode item : warningNode) {
                    if (item.isTextual()) {
                        warnings.add(item.asText());
                    }
                }
            }
            String mode = root.path("_meta").path("mode").asText("");
            boolean llmEffective = "llm_enhanced".equalsIgnoreCase(mode);
            boolean fallbackUsed = "rule_generated".equalsIgnoreCase(mode) && llmEnabled;
            String statusLabel = resolveStatusLabel(llmEffective, fallbackUsed, llmEnabled);
            String statusReason = resolveStatusReason(llmEffective, fallbackUsed, llmEnabled, warnings);
            return new TestLlmPreprocessConfigResultDTO(
                    true,
                    mode,
                    llmEnabled,
                    llmEffective,
                    fallbackUsed,
                    statusLabel,
                    statusReason,
                    promptFingerprint,
                    System.currentTimeMillis() - start,
                    sceneCount,
                    sqlCount,
                    warnings,
                    "测试成功"
            );
        } catch (Exception ex) {
            boolean fallbackUsed = llmEnabled;
            String statusLabel = resolveStatusLabel(false, fallbackUsed, llmEnabled);
            String statusReason = "测试执行失败: " + ex.getMessage();
            return new TestLlmPreprocessConfigResultDTO(
                    false,
                    "",
                    llmEnabled,
                    false,
                    fallbackUsed,
                    statusLabel,
                    statusReason,
                    promptFingerprint,
                    System.currentTimeMillis() - start,
                    0,
                    0,
                    List.of(),
                    "测试失败: " + ex.getMessage()
            );
        }
    }

    public LlmModelListResultDTO fetchModels(FetchLlmModelListCmd cmd) {
        ResolvedModelQueryConfig runtime = resolveModelQueryConfig(cmd);
        if (runtime.endpoint().isBlank()) {
            return new LlmModelListResultDTO(false, "", List.of(), runtime.selectedModel(), "未配置模型接口地址");
        }

        URI modelsUri;
        try {
            modelsUri = resolveModelListUri(runtime.endpoint());
        } catch (IllegalArgumentException ex) {
            return new LlmModelListResultDTO(false, "", List.of(), runtime.selectedModel(), "模型接口地址不合法: " + ex.getMessage());
        }

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(modelsUri)
                    .timeout(Duration.ofSeconds(Math.max(5, runtime.timeoutSeconds())))
                    .header("Accept", "application/json")
                    .GET();

            if (!runtime.apiKey().isBlank()) {
                builder.header("Authorization", "Bearer " + runtime.apiKey());
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 300) {
                String brief = abbreviate(trimOrEmpty(response.body()), 180);
                String message = brief.isEmpty()
                        ? "请求失败，状态码=" + response.statusCode()
                        : "请求失败，状态码=" + response.statusCode() + "，响应=" + brief;
                return new LlmModelListResultDTO(false, modelsUri.toString(), List.of(), runtime.selectedModel(), message);
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                return new LlmModelListResultDTO(false, modelsUri.toString(), List.of(), runtime.selectedModel(), "返回结构缺少 data 数组");
            }

            Set<String> modelSet = new LinkedHashSet<>();
            for (JsonNode item : data) {
                String id = trimOrEmpty(item.path("id").asText(""));
                if (!id.isBlank()) {
                    modelSet.add(id);
                }
            }

            List<String> models = modelSet.stream()
                    .sorted(Comparator.comparing(String::toLowerCase))
                    .toList();
            if (models.isEmpty()) {
                return new LlmModelListResultDTO(false, modelsUri.toString(), List.of(), runtime.selectedModel(), "未获取到模型列表");
            }

            String selected = runtime.selectedModel().isBlank() ? models.get(0) : runtime.selectedModel();
            return new LlmModelListResultDTO(true, modelsUri.toString(), models, selected, "模型列表获取成功");
        } catch (Exception ex) {
            return new LlmModelListResultDTO(false, modelsUri.toString(), List.of(), runtime.selectedModel(), "模型列表获取失败: " + ex.getMessage());
        }
    }

    public LlmPromptPreviewDTO previewPrompts(PreviewLlmPromptCmd cmd) {
        LlmPreprocessSupport.PromptPreviewResult previewResult = llmPreprocessSupport.previewPrompt(
                cmd.rawText(),
                cmd.sourceType(),
                cmd.preprocessSystemPrompt(),
                cmd.preprocessUserPromptTemplate(),
                cmd.prepSchemaJson()
        );
        return new LlmPromptPreviewDTO(
                previewResult.systemPrompt(),
                previewResult.userPrompt(),
                previewResult.promptFingerprint(),
                previewResult.normalizedSourceType(),
                previewResult.lineCount(),
                previewResult.warnings()
        );
    }

    private void validate(UpdateLlmPreprocessConfigCmd cmd) {
        if (cmd.timeoutSeconds() < 5 || cmd.timeoutSeconds() > 120) {
            throw new DomainValidationException("timeoutSeconds must be between 5 and 120");
        }
        if (cmd.temperature() < 0 || cmd.temperature() > 1) {
            throw new DomainValidationException("temperature must be between 0 and 1");
        }
        if (cmd.maxTokens() < 128 || cmd.maxTokens() > 32768) {
            throw new DomainValidationException("maxTokens must be between 128 and 32768");
        }
        if (Boolean.TRUE.equals(cmd.enabled())) {
            if (trimOrEmpty(cmd.endpoint()).isEmpty()) {
                throw new DomainValidationException("endpoint is required when llm preprocess is enabled");
            }
            if (trimOrEmpty(cmd.model()).isEmpty()) {
                throw new DomainValidationException("model is required when llm preprocess is enabled");
            }
        }
        if (trimOrEmpty(cmd.operator()).isEmpty()) {
            throw new DomainValidationException("operator must not be blank");
        }
    }

    private void validatePromptConfig(UpdateLlmPromptConfigCmd cmd) {
        if (trimOrEmpty(cmd.preprocessSystemPrompt()).isEmpty()) {
            throw new DomainValidationException("preprocessSystemPrompt must not be blank");
        }
        if (trimOrEmpty(cmd.preprocessUserPromptTemplate()).isEmpty()) {
            throw new DomainValidationException("preprocessUserPromptTemplate must not be blank");
        }
        if (trimOrEmpty(cmd.prepSchemaJson()).isEmpty()) {
            throw new DomainValidationException("prepSchemaJson must not be blank");
        }
        if (trimOrEmpty(cmd.operator()).isEmpty()) {
            throw new DomainValidationException("operator must not be blank");
        }
    }

    private String resolveStatusLabel(boolean llmEffective, boolean fallbackUsed, boolean llmEnabled) {
        if (llmEffective) {
            return "LLM生效";
        }
        if (fallbackUsed) {
            return "已回退规则";
        }
        if (!llmEnabled) {
            return "未启用";
        }
        return "未知状态";
    }

    private String resolveStatusReason(boolean llmEffective,
                                       boolean fallbackUsed,
                                       boolean llmEnabled,
                                       List<String> warnings) {
        if (llmEffective) {
            return "当前测试请求已使用大模型预处理链路。";
        }
        if (fallbackUsed) {
            if (warnings == null || warnings.isEmpty()) {
                return "大模型未生效，系统已自动回退规则抽取。";
            }
            return "大模型未生效，系统已自动回退规则抽取。警告: " + String.join("；", warnings);
        }
        if (!llmEnabled) {
            return "当前配置未启用大模型预处理，测试请求按规则抽取执行。";
        }
        return "状态判定失败，请检查配置。";
    }

    private String resolveApiKeyCipher(LlmPreprocessConfig existing, UpdateLlmPreprocessConfigCmd cmd) {
        if (Boolean.TRUE.equals(cmd.clearApiKey())) {
            return "";
        }
        String newApiKey = trimOrEmpty(cmd.apiKey());
        if (!newApiKey.isEmpty()) {
            return llmSecretCodec.encrypt(newApiKey);
        }
        return existing.getApiKeyCiphertext();
    }

    private ResolvedModelQueryConfig resolveModelQueryConfig(FetchLlmModelListCmd cmd) {
        Optional<LlmPreprocessConfig> persisted = llmPreprocessConfigDomainSupport.findSingleton();
        LlmPreprocessConfig config = persisted.orElseGet(this::defaultConfig);

        String endpoint = firstNonBlank(
                cmd == null ? "" : trimOrEmpty(cmd.endpoint()),
                trimOrEmpty(config.getEndpoint()),
                trimOrEmpty(llmPreprocessProperties.getEndpoint())
        );
        String apiKey = firstNonBlank(
                cmd == null ? "" : trimOrEmpty(cmd.apiKey()),
                decryptCipher(config.getApiKeyCiphertext()),
                trimOrEmpty(llmPreprocessProperties.getApiKey())
        );
        int timeoutSeconds = resolveTimeoutSeconds(
                cmd == null ? null : cmd.timeoutSeconds(),
                config.getTimeoutSeconds(),
                llmPreprocessProperties.getTimeoutSeconds()
        );
        String selectedModel = firstNonBlank(
                trimOrEmpty(config.getModel()),
                trimOrEmpty(llmPreprocessProperties.getModel())
        );
        return new ResolvedModelQueryConfig(endpoint, apiKey, timeoutSeconds, selectedModel);
    }

    private URI resolveModelListUri(String endpoint) {
        String raw = trimOrEmpty(endpoint);
        if (raw.isBlank()) {
            throw new IllegalArgumentException("endpoint is blank");
        }
        URI origin = URI.create(raw);
        if (origin.getScheme() == null || origin.getHost() == null) {
            throw new IllegalArgumentException("endpoint must include scheme and host");
        }

        String path = trimOrEmpty(origin.getPath());
        String targetPath;
        if (path.isBlank() || "/".equals(path)) {
            targetPath = "/v1/models";
        } else if (path.endsWith("/models")) {
            targetPath = path;
        } else if (path.contains("/v1")) {
            int idx = path.indexOf("/v1");
            targetPath = path.substring(0, idx + 3) + "/models";
        } else {
            String normalized = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
            targetPath = normalized + "/v1/models";
        }

        try {
            return new URI(origin.getScheme(), origin.getUserInfo(), origin.getHost(), origin.getPort(), targetPath, null, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("endpoint path is invalid");
        }
    }

    private int resolveTimeoutSeconds(Integer primary, Integer secondary, int fallback) {
        if (primary != null && primary > 0) {
            return primary;
        }
        if (secondary != null && secondary > 0) {
            return secondary;
        }
        return fallback > 0 ? fallback : 35;
    }

    private String decryptCipher(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            return "";
        }
        try {
            return llmSecretCodec.decrypt(cipherText);
        } catch (IllegalStateException ex) {
            return trimOrEmpty(cipherText);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String abbreviate(String text, int maxLen) {
        String safe = trimOrEmpty(text);
        if (safe.length() <= maxLen) {
            return safe;
        }
        return safe.substring(0, Math.max(0, maxLen - 1)) + "…";
    }

    private LlmPreprocessConfig defaultConfig() {
        String dynamicSchema = llmPrepSchemaJsonGenerator.generateSchemaJson();
        return LlmPreprocessConfig.builder()
                .id(1L)
                .enabled(llmPreprocessProperties.isEnabled())
                .endpoint(trimOrEmpty(llmPreprocessProperties.getEndpoint()))
                .apiKeyCiphertext(llmSecretCodec.encrypt(llmPreprocessProperties.getApiKey()))
                .model(trimOrEmpty(llmPreprocessProperties.getModel()))
                .timeoutSeconds(llmPreprocessProperties.getTimeoutSeconds())
                .temperature(llmPreprocessProperties.getTemperature())
                .maxTokens(llmPreprocessProperties.getMaxTokens())
                .enableThinking(llmPreprocessProperties.isEnableThinking())
                .fallbackToRule(llmPreprocessProperties.isFallbackToRule())
                .preprocessSystemPrompt(LlmPromptDefaults.PREPROCESS_SYSTEM_PROMPT)
                .preprocessUserPromptTemplate(LlmPromptDefaults.PREPROCESS_USER_PROMPT_TEMPLATE)
                .prepSchemaJson(dynamicSchema)
                .promptVersion(1L)
                .promptHash(calculatePromptHash(
                        LlmPromptDefaults.PREPROCESS_SYSTEM_PROMPT,
                        LlmPromptDefaults.PREPROCESS_USER_PROMPT_TEMPLATE,
                        dynamicSchema
                ))
                .updatedBy("system-default")
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private String trimOrEmpty(String text) {
        return text == null ? "" : text.trim();
    }

    private long resolvePromptVersion(Long promptVersion) {
        if (promptVersion == null || promptVersion < 1) {
            return 1L;
        }
        return promptVersion;
    }

    private String calculatePromptHash(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (parts != null) {
                for (String part : parts) {
                    digest.update(trimOrEmpty(part).getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) 0x1F);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            return "";
        }
    }

    private Boolean resolveEnableThinking(LlmPreprocessConfig existing, Boolean requested) {
        if (requested != null) {
            return requested;
        }
        if (existing.getEnableThinking() != null) {
            return existing.getEnableThinking();
        }
        return llmPreprocessProperties.isEnableThinking();
    }

    private record ResolvedModelQueryConfig(
            String endpoint,
            String apiKey,
            int timeoutSeconds,
            String selectedModel
    ) {
    }
}
