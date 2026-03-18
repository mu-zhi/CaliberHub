package com.cmbchina.datadirect.caliber.application.service.query;

import com.cmbchina.datadirect.caliber.application.api.dto.response.LlmPreprocessConfigDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.LlmPromptConfigDTO;
import com.cmbchina.datadirect.caliber.domain.model.LlmPreprocessConfig;
import com.cmbchina.datadirect.caliber.domain.support.LlmPreprocessConfigDomainSupport;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmPromptDefaults;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmPromptFingerprint;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmPrepSchemaJsonGenerator;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmPreprocessProperties;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmSecretCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class LlmPreprocessConfigQueryAppService {

    private static final List<String> REQUIRED_TEMPLATE_CORE_TOKENS = List.of("{{RAW_DOC}}", "{{SOURCE_TYPE}}");
    private static final List<String> SCHEMA_TEMPLATE_TOKENS = List.of("{{PREP_SCHEMA}}", "{{DYNAMIC_JSON_SCHEMA}}");

    private final LlmPreprocessConfigDomainSupport llmPreprocessConfigDomainSupport;
    private final LlmPreprocessProperties llmPreprocessProperties;
    private final LlmSecretCodec llmSecretCodec;
    private final ObjectMapper objectMapper;
    private final LlmPrepSchemaJsonGenerator llmPrepSchemaJsonGenerator;

    public LlmPreprocessConfigQueryAppService(LlmPreprocessConfigDomainSupport llmPreprocessConfigDomainSupport,
                                              LlmPreprocessProperties llmPreprocessProperties,
                                              LlmSecretCodec llmSecretCodec,
                                              ObjectMapper objectMapper,
                                              LlmPrepSchemaJsonGenerator llmPrepSchemaJsonGenerator) {
        this.llmPreprocessConfigDomainSupport = llmPreprocessConfigDomainSupport;
        this.llmPreprocessProperties = llmPreprocessProperties;
        this.llmSecretCodec = llmSecretCodec;
        this.objectMapper = objectMapper;
        this.llmPrepSchemaJsonGenerator = llmPrepSchemaJsonGenerator;
    }

    public LlmPreprocessConfigDTO getCurrentConfig() {
        Optional<LlmPreprocessConfig> persisted = llmPreprocessConfigDomainSupport.findSingleton();
        LlmPreprocessConfig config = persisted.orElseGet(this::defaultConfig);

        String plainApiKey = decrypt(config.getApiKeyCiphertext());
        return new LlmPreprocessConfigDTO(
                config.getEnabled(),
                config.getEndpoint(),
                config.getModel(),
                config.getTimeoutSeconds(),
                config.getTemperature(),
                config.getMaxTokens(),
                config.getEnableThinking(),
                config.getFallbackToRule(),
                !plainApiKey.isBlank(),
                maskApiKey(plainApiKey),
                persisted.isPresent() ? "PERSISTED" : "DEFAULT_PROPERTIES",
                extractEndpointHost(config.getEndpoint()),
                Boolean.TRUE.equals(config.getFallbackToRule()) ? "LLM -> RULE" : "LLM ONLY",
                config.getUpdatedBy(),
                config.getUpdatedAt()
        );
    }

    public LlmPromptConfigDTO getCurrentPromptConfig() {
        LlmPreprocessConfig config = resolveCurrentConfig();
        String dynamicSchema = llmPrepSchemaJsonGenerator.generateSchemaJson();
        String systemPrompt = firstNonBlank(config.getPreprocessSystemPrompt(), LlmPromptDefaults.PREPROCESS_SYSTEM_PROMPT);
        String userTemplate = firstNonBlank(config.getPreprocessUserPromptTemplate(), LlmPromptDefaults.PREPROCESS_USER_PROMPT_TEMPLATE);
        boolean useDynamicSchema = userTemplate.contains("{{DYNAMIC_JSON_SCHEMA}}");
        String prepSchema = useDynamicSchema ? dynamicSchema : firstNonBlank(config.getPrepSchemaJson(), dynamicSchema);
        String promptHash = firstNonBlank(config.getPromptHash(), calculatePromptHash(systemPrompt, userTemplate, prepSchema));
        PromptConfigValidation validation = validatePromptConfig(userTemplate, prepSchema);
        return new LlmPromptConfigDTO(
                systemPrompt,
                userTemplate,
                prepSchema,
                resolvePromptVersion(config.getPromptVersion()),
                promptHash,
                LlmPromptFingerprint.of(systemPrompt, userTemplate, prepSchema),
                validation.schemaValid(),
                validation.schemaValidationMessage(),
                validation.templateHasRequiredTokens(),
                validation.templateMissingTokens(),
                config.getUpdatedBy(),
                config.getUpdatedAt()
        );
    }

    public LlmPreprocessConfig resolveCurrentConfig() {
        return llmPreprocessConfigDomainSupport.findSingleton()
                .orElseGet(this::defaultConfig);
    }

    private LlmPreprocessConfig defaultConfig() {
        String dynamicSchema = llmPrepSchemaJsonGenerator.generateSchemaJson();
        return LlmPreprocessConfig.builder()
                .id(1L)
                .enabled(llmPreprocessProperties.isEnabled())
                .endpoint(llmPreprocessProperties.getEndpoint())
                .apiKeyCiphertext(llmSecretCodec.encrypt(llmPreprocessProperties.getApiKey()))
                .model(llmPreprocessProperties.getModel())
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
                .updatedAt(null)
                .build();
    }

    private String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            return "";
        }
        try {
            return llmSecretCodec.decrypt(cipherText);
        } catch (IllegalStateException ex) {
            return cipherText;
        }
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        if (apiKey.length() <= 6) {
            return "***";
        }
        String prefix = apiKey.substring(0, 3);
        String suffix = apiKey.substring(apiKey.length() - 3);
        return prefix + "***" + suffix;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private String extractEndpointHost(String endpoint) {
        String safe = trimOrEmpty(endpoint);
        if (safe.isBlank()) {
            return "-";
        }
        try {
            URI uri = URI.create(safe);
            String host = trimOrEmpty(uri.getHost());
            return host.isBlank() ? "-" : host;
        } catch (IllegalArgumentException ex) {
            return "-";
        }
    }

    private PromptConfigValidation validatePromptConfig(String userTemplate, String prepSchema) {
        String template = trimOrEmpty(userTemplate);
        List<String> missingTokens = new ArrayList<>();
        for (String token : REQUIRED_TEMPLATE_CORE_TOKENS) {
            if (!template.contains(token)) {
                missingTokens.add(token);
            }
        }
        boolean hasSchemaToken = false;
        for (String token : SCHEMA_TEMPLATE_TOKENS) {
            if (template.contains(token)) {
                hasSchemaToken = true;
                break;
            }
        }
        if (!hasSchemaToken) {
            missingTokens.add("{{PREP_SCHEMA}}|{{DYNAMIC_JSON_SCHEMA}}");
        }
        boolean schemaValid = true;
        String schemaValidationMessage = "Schema JSON 可解析";
        try {
            objectMapper.readTree(trimOrEmpty(prepSchema));
        } catch (Exception ex) {
            schemaValid = false;
            schemaValidationMessage = "Schema JSON 解析失败: " + trimOrEmpty(ex.getMessage());
        }
        return new PromptConfigValidation(schemaValid, schemaValidationMessage, missingTokens.isEmpty(), missingTokens);
    }

    private String trimOrEmpty(String text) {
        return text == null ? "" : text.trim();
    }

    private Long resolvePromptVersion(Long promptVersion) {
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

    private record PromptConfigValidation(
            boolean schemaValid,
            String schemaValidationMessage,
            boolean templateHasRequiredTokens,
            List<String> templateMissingTokens
    ) {
    }
}
