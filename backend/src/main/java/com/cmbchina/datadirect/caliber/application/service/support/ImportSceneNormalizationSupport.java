package com.cmbchina.datadirect.caliber.application.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class ImportSceneNormalizationSupport {

    private static final Pattern METHOD_PREFIX = Pattern.compile("^(?:SQL\\s*)?(?:方法|方案|路径)\\s*[0-9一二三四五六七八九十]+\\s*[：:、.．\\-)]*\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern STEP_PREFIX = Pattern.compile("^(?:STEP|步骤|第)\\s*[0-9一二三四五六七八九十]+\\s*(?:步|阶段)?\\s*[：:、.．\\-)]*\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERIC_VARIANT = Pattern.compile("^(?:默认方案|取数方案\\d*|方案\\d*|SQL方案\\d*)$", Pattern.CASE_INSENSITIVE);

    private ImportSceneNormalizationSupport() {
    }

    public static List<JsonNode> normalize(ObjectMapper objectMapper, List<JsonNode> scenes) {
        if (objectMapper == null || scenes == null || scenes.isEmpty()) {
            return List.of();
        }
        List<ObjectNode> normalized = new ArrayList<>();
        for (JsonNode rawScene : scenes) {
            if (rawScene == null || !rawScene.isObject()) {
                continue;
            }
            ObjectNode candidate = ((ObjectNode) rawScene).deepCopy();
            prepareScene(candidate);

            ObjectNode merged = findMergeTarget(normalized, candidate);
            if (merged == null) {
                normalized.add(candidate);
            } else {
                mergeSceneInto(merged, candidate);
            }
        }
        return new ArrayList<>(normalized);
    }

    private static ObjectNode findMergeTarget(List<ObjectNode> normalized, ObjectNode candidate) {
        for (ObjectNode existing : normalized) {
            if (shouldMerge(existing, candidate)) {
                return existing;
            }
        }
        return null;
    }

    private static boolean shouldMerge(ObjectNode left, ObjectNode right) {
        String leftTitle = canonicalSceneTitle(readText(left.path("scene_title")));
        String rightTitle = canonicalSceneTitle(readText(right.path("scene_title")));
        if (leftTitle.isBlank() || rightTitle.isBlank() || !leftTitle.equals(rightTitle)) {
            return false;
        }
        Set<String> leftOutputs = outputSignature(left);
        Set<String> rightOutputs = outputSignature(right);
        return leftOutputs.isEmpty() || rightOutputs.isEmpty() || !disjoint(leftOutputs, rightOutputs);
    }

    private static boolean disjoint(Set<String> left, Set<String> right) {
        for (String item : left) {
            if (right.contains(item)) {
                return false;
            }
        }
        return true;
    }

    private static void prepareScene(ObjectNode scene) {
        String originalTitle = readText(scene.path("scene_title"));
        String canonicalTitle = canonicalSceneTitle(originalTitle);
        if (!canonicalTitle.isBlank()) {
            scene.put("scene_title", canonicalTitle);
        }

        String sceneApplicability = readText(scene.path("applicability"));
        ArrayNode variants = ensureArray(scene, "sql_variants");
        for (int i = 0; i < variants.size(); i++) {
            JsonNode variantNode = variants.get(i);
            if (!(variantNode instanceof ObjectNode variant)) {
                continue;
            }
            if (!sceneApplicability.isBlank() && readText(variant.path("applicable_period")).isBlank()) {
                variant.put("applicable_period", sceneApplicability);
            }
            if (!originalTitle.isBlank()
                    && !canonicalTitle.equals(originalTitle)
                    && looksGenericVariantName(readText(variant.path("variant_name")))) {
                variant.put("variant_name", canonicalTitle);
            }
            if (readText(variant.path("variant_name")).isBlank()) {
                variant.put("variant_name", "取数方案" + (i + 1));
            }
        }

        enrichDuplicateVariantNames(variants);
    }

    private static void enrichDuplicateVariantNames(ArrayNode variants) {
        Map<String, Integer> seen = new java.util.LinkedHashMap<>();

        for (JsonNode item : variants) {
            if (!(item instanceof ObjectNode variant)) {
                continue;
            }

            String baseName = readText(variant.path("variant_name"));
            if (baseName.isBlank()) {
                continue;
            }

            int occurrence = seen.getOrDefault(baseName, 0) + 1;
            seen.put(baseName, occurrence);

            if (occurrence > 1) {
                String suffix = firstNonBlank(
                        normalizePeriodLabel(readText(variant.path("applicable_period"))),
                        firstSourceTable(variant),
                        "方案" + occurrence
                );
                variant.put("variant_name", baseName + "（" + suffix + "）");
            }
        }
    }

    private static String firstSourceTable(JsonNode variant) {
        ArrayNode tables = ensureArray((ObjectNode) variant, "source_tables");
        if (tables.isEmpty()) {
            return "";
        }
        return tables.get(0).asText("").trim();
    }

    private static String normalizePeriodLabel(String period) {
        String normalized = period == null ? "" : period.trim();
        if (normalized.contains("历史表")) {
            return "历史表";
        }
        if (normalized.contains("当前表") || normalized.contains("当前主表")) {
            return "当前表";
        }
        return normalized;
    }

    private static void mergeSceneInto(ObjectNode target, ObjectNode incoming) {
        preferLongerText(target, incoming, "scene_description");
        preferLongerText(target, incoming, "caliber_definition");
        mergeTextField(target, incoming, "applicability");
        mergeTextField(target, incoming, "boundaries");
        mergeTextField(target, incoming, "unmapped_text");

        mergeNumberArray(target, incoming, "source_evidence_lines");
        mergeTextArray(target, incoming, "contributors");
        mergeObjectArray(target, incoming, "code_mappings", ImportSceneNormalizationSupport::codeMappingKey);
        mergeObjectArray(target, incoming, "caveats", ImportSceneNormalizationSupport::simpleObjectKey);
        mergeFieldArray(target, incoming, "inputs", "params");
        mergeFieldArray(target, incoming, "outputs", "fields");
        mergeObjectArray(target, incoming, "sql_variants", ImportSceneNormalizationSupport::sqlVariantKey);
        enrichDuplicateVariantNames(ensureArray(target, "sql_variants"));
        mergeQuality(target, incoming);
    }

    private static void mergeFieldArray(ObjectNode target, ObjectNode incoming, String parentField, String arrayField) {
        ObjectNode targetParent = ensureObject(target, parentField);
        ObjectNode incomingParent = ensureObject(incoming, parentField);
        ArrayNode targetArray = ensureArray(targetParent, arrayField);
        ArrayNode incomingArray = ensureArray(incomingParent, arrayField);
        Map<String, JsonNode> dedup = new LinkedHashMap<>();
        targetArray.forEach(node -> dedup.put(fieldKey(node), node));
        incomingArray.forEach(node -> dedup.putIfAbsent(fieldKey(node), node));
        targetParent.set(arrayField, toArray(targetArray.arrayNode(), dedup.values()));
    }

    private static void mergeObjectArray(ObjectNode target,
                                         ObjectNode incoming,
                                         String arrayField,
                                         java.util.function.Function<JsonNode, String> keyFn) {
        ArrayNode targetArray = ensureArray(target, arrayField);
        ArrayNode incomingArray = ensureArray(incoming, arrayField);
        Map<String, JsonNode> dedup = new LinkedHashMap<>();
        targetArray.forEach(node -> dedup.put(keyFn.apply(node), node));
        incomingArray.forEach(node -> dedup.putIfAbsent(keyFn.apply(node), node));
        target.set(arrayField, toArray(targetArray.arrayNode(), dedup.values()));
    }

    private static void mergeNumberArray(ObjectNode target, ObjectNode incoming, String field) {
        LinkedHashSet<Integer> merged = new LinkedHashSet<>();
        readIntArray(target.path(field)).forEach(merged::add);
        readIntArray(incoming.path(field)).forEach(merged::add);
        ArrayNode array = target.arrayNode();
        merged.forEach(array::add);
        target.set(field, array);
    }

    private static void mergeTextArray(ObjectNode target, ObjectNode incoming, String field) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        readTextArray(target.path(field)).forEach(merged::add);
        readTextArray(incoming.path(field)).forEach(merged::add);
        ArrayNode array = target.arrayNode();
        merged.forEach(array::add);
        target.set(field, array);
    }

    private static void mergeQuality(ObjectNode target, ObjectNode incoming) {
        ObjectNode targetQuality = ensureObject(target, "quality");
        ObjectNode incomingQuality = ensureObject(incoming, "quality");
        double confidence = Math.max(targetQuality.path("confidence").asDouble(0.0), incomingQuality.path("confidence").asDouble(0.0));
        targetQuality.put("confidence", confidence);
        mergeTextArray(targetQuality, incomingQuality, "warnings");
        mergeTextArray(targetQuality, incomingQuality, "errors");
    }

    private static void preferLongerText(ObjectNode target, ObjectNode incoming, String field) {
        String left = readText(target.path(field));
        String right = readText(incoming.path(field));
        if (left.isBlank() || (!right.isBlank() && right.length() > left.length())) {
            target.put(field, right);
        }
    }

    private static void mergeTextField(ObjectNode target, ObjectNode incoming, String field) {
        String left = readText(target.path(field));
        String right = readText(incoming.path(field));
        if (left.isBlank()) {
            target.put(field, right);
            return;
        }
        if (right.isBlank() || left.contains(right)) {
            return;
        }
        if (right.contains(left)) {
            target.put(field, right);
            return;
        }
        target.put(field, left + "\n" + right);
    }

    private static Set<String> outputSignature(ObjectNode scene) {
        Set<String> fields = new LinkedHashSet<>();
        ensureArray(ensureObject(scene, "outputs"), "fields").forEach(node -> {
            String key = fieldKey(node);
            if (!key.isBlank()) {
                fields.add(key);
            }
        });
        return fields;
    }

    private static String fieldKey(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "";
        }
        String key = normalizeKey(firstNonBlank(
                readText(node.path("name")),
                readText(node.path("name_zh")),
                readText(node.path("display_name")),
                readText(node.path("label")),
                readText(node.path("source_field"))
        ));
        return key == null ? "" : key;
    }

    private static String codeMappingKey(JsonNode node) {
        return normalizeKey(node == null ? "" : node.toString());
    }

    private static String simpleObjectKey(JsonNode node) {
        return normalizeKey(node == null ? "" : node.toString());
    }

    private static String sqlVariantKey(JsonNode node) {
        if (node == null || !node.isObject()) {
            return "";
        }
        String sql = normalizeKey(readText(node.path("sql_text")));
        if (!sql.isBlank()) {
            return sql;
        }
        String name = normalizeKey(readText(node.path("variant_name")));
        String period = normalizeKey(readText(node.path("applicable_period")));
        String tables = normalizeKey(String.join("|", readTextArray(node.path("source_tables"))));
        return name + "|" + period + "|" + tables;
    }

    private static String canonicalSceneTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        normalized = METHOD_PREFIX.matcher(normalized).replaceFirst("");
        normalized = STEP_PREFIX.matcher(normalized).replaceFirst("");
        return normalized.trim();
    }

    private static boolean looksGenericVariantName(String name) {
        String normalized = name == null ? "" : name.trim();
        return normalized.isBlank() || GENERIC_VARIANT.matcher(normalized).matches();
    }

    private static ObjectNode ensureObject(ObjectNode node, String field) {
        JsonNode child = node.path(field);
        if (child instanceof ObjectNode objectNode) {
            return objectNode;
        }
        ObjectNode created = node.objectNode();
        node.set(field, created);
        return created;
    }

    private static ArrayNode ensureArray(ObjectNode node, String field) {
        JsonNode child = node.path(field);
        if (child instanceof ArrayNode arrayNode) {
            return arrayNode;
        }
        ArrayNode created = node.arrayNode();
        node.set(field, created);
        return created;
    }

    private static ArrayNode toArray(ArrayNode template, Iterable<JsonNode> nodes) {
        ArrayNode result = template.arrayNode();
        for (JsonNode node : nodes) {
            result.add(node.deepCopy());
        }
        return result;
    }

    private static List<String> readTextArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                String text = readText(item);
                if (!text.isBlank()) {
                    values.add(text);
                }
            });
        }
        return values;
    }

    private static List<Integer> readIntArray(JsonNode node) {
        List<Integer> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                if (item.canConvertToInt()) {
                    values.add(item.asInt());
                }
            });
        }
        return values;
    }

    private static String readText(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "";
        }
        return node.asText("").trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}
