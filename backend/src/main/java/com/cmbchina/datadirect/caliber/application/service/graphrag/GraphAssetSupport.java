package com.cmbchina.datadirect.caliber.application.service.graphrag;

import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class GraphAssetSupport {

    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");
    private final ObjectMapper objectMapper;

    public GraphAssetSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(field + " must not be blank");
        }
        return value.trim();
    }

    public String normalizeStatus(String status, String fallback) {
        String value = status == null || status.isBlank() ? fallback : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("DRAFT", "PUBLISHED", "DISCARDED", "INACTIVE", "ACTIVE").contains(value)) {
            throw new DomainValidationException("invalid status: " + value);
        }
        return value;
    }

    public String normalizeSceneType(String sceneType) {
        String value = sceneType == null || sceneType.isBlank() ? "FACT_DETAIL" : sceneType.trim().toUpperCase(Locale.ROOT);
        if (!List.of("FACT_DETAIL", "FACT_AGGREGATION", "ENTITY_PROFILE", "CHANGE_TRACE", "AUDIT_LOG",
                "WATCHLIST_CONTROL", "CROSS_SOURCE_RECON").contains(value)) {
            throw new DomainValidationException("invalid sceneType: " + value);
        }
        return value;
    }

    public String normalizeScopeType(String scopeType) {
        String value = requireText(scopeType, "scopeType").toUpperCase(Locale.ROOT);
        if (!List.of("GLOBAL", "DOMAIN", "SCENE", "PLAN").contains(value)) {
            throw new DomainValidationException("scopeType must be GLOBAL/DOMAIN/SCENE/PLAN");
        }
        return value;
    }

    public String normalizeEffectType(String effectType) {
        String value = requireText(effectType, "effectType").toUpperCase(Locale.ROOT);
        if (!List.of("ALLOW", "REQUIRE_APPROVAL", "DENY").contains(value)) {
            throw new DomainValidationException("effectType must be ALLOW/REQUIRE_APPROVAL/DENY");
        }
        return value;
    }

    public String normalizeCoverageType(String coverageType) {
        String value = requireText(coverageType, "coverageType").toUpperCase(Locale.ROOT);
        if (!List.of("PERIOD_TABLE", "LEGACY", "MANUAL").contains(value)) {
            throw new DomainValidationException("coverageType must be PERIOD_TABLE/LEGACY/MANUAL");
        }
        return value;
    }

    public String normalizeCoverageStatus(String coverageStatus) {
        String value = coverageStatus == null || coverageStatus.isBlank() ? "FULL" : coverageStatus.trim().toUpperCase(Locale.ROOT);
        if (!List.of("FULL", "PARTIAL", "GAP").contains(value)) {
            throw new DomainValidationException("coverageStatus must be FULL/PARTIAL/GAP");
        }
        return value;
    }

    public String normalizeLinkRole(String role) {
        String value = requireText(role, "linkRole").toUpperCase(Locale.ROOT);
        if (!List.of("FILTER", "OUTPUT", "JOIN", "TIME").contains(value)) {
            throw new DomainValidationException("linkRole must be FILTER/OUTPUT/JOIN/TIME");
        }
        return value;
    }

    public String normalizeSensitivityLevel(String sensitivityLevel) {
        String value = sensitivityLevel == null || sensitivityLevel.isBlank()
                ? "S1"
                : sensitivityLevel.trim().toUpperCase(Locale.ROOT);
        if (!List.of("S0", "S1", "S2", "S3", "S4").contains(value)) {
            throw new DomainValidationException("sensitivityLevel must be S0/S1/S2/S3/S4");
        }
        return value;
    }

    public String generateCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    public ArrayNode parseArray(String jsonText) {
        try {
            JsonNode node = objectMapper.readTree(jsonText == null || jsonText.isBlank() ? "[]" : jsonText);
            if (node != null && node.isArray()) {
                return (ArrayNode) node;
            }
        } catch (Exception ignore) {
            // ignore
        }
        return objectMapper.createArrayNode();
    }

    public JsonNode parseJson(String text, String fallback) {
        try {
            return objectMapper.readTree(text == null || text.isBlank() ? fallback : text);
        } catch (Exception ignore) {
            try {
                return objectMapper.readTree(fallback);
            } catch (Exception inner) {
                throw new IllegalStateException("invalid fallback json", inner);
            }
        }
    }

    public String writeJson(Object payload, String fallback) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ignore) {
            return fallback;
        }
    }

    public List<String> parseStringList(String jsonText) {
        JsonNode node = parseJson(jsonText, "[]");
        if (!node.isArray()) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        node.forEach(item -> {
            String text = safeText(item);
            if (!text.isBlank()) {
                values.add(text);
            }
        });
        return new ArrayList<>(values);
    }

    public String normalizeAlias(String text) {
        String safe = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        return NORMALIZE_PATTERN.matcher(safe).replaceAll(" ").trim();
    }

    public String safeText(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "";
        }
        return node.asText("").trim();
    }

    public LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDate.parse(text.trim());
    }
}
