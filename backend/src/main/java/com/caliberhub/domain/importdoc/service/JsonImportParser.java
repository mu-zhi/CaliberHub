package com.caliberhub.domain.importdoc.service;

import com.caliberhub.application.importdoc.dto.ParseResponse;
import com.caliberhub.application.importdoc.schema.ImportSchemaValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * CALIBER_IMPORT_V1 JSON 导入解析器。
 *
 * 将结构化 JSON 直接映射为 ParseResponse，若输入不是 JSON 则返回 null 以便上层回退到 Markdown 解析。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonImportParser {

    private final ObjectMapper objectMapper;
    private final ImportSchemaValidator importSchemaValidator;

    private static final Pattern TABLE_PATTERN = Pattern.compile("(?:FROM|JOIN)\\s+([a-zA-Z_][a-zA-Z0-9_.]*)",
            Pattern.CASE_INSENSITIVE);

    /**
     * 尝试解析 JSON；若输入不是 JSON 或解析失败返回 null。
     */
    @SuppressWarnings("unchecked")
    public ParseResponse tryParse(String rawText) {
        if (rawText == null) {
            return null;
        }
        String trimmed = rawText.trim();
        if (!trimmed.startsWith("{")) {
            return null; // 明显不是 JSON
        }

        Map<String, Object> data;
        try {
            data = objectMapper.readValue(trimmed, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            // 不是合法 JSON，交给 Markdown 解析
            log.debug("Raw text is not valid JSON, fallback to markdown parser", e);
            return null;
        }

        // Schema 校验
        ImportSchemaValidator.ValidationResult validationResult = importSchemaValidator.validate(data);
        List<String> globalWarnings = new ArrayList<>();
        List<String> globalErrors = new ArrayList<>();
        if (!validationResult.valid()) {
            validationResult.violations().forEach(v ->
                    globalErrors.add(v.path() + ": " + v.reason() + (v.value() != null ? " (" + v.value() + ")" : ""))
            );
        }

        // 追加已有 parse_report 中的警告/错误
        Map<String, Object> parseReport = map(data.get("parse_report"));
        if (parseReport != null) {
            globalWarnings.addAll(listOfString(parseReport.get("warnings")));
            globalErrors.addAll(listOfString(parseReport.get("errors")));
        }

        Object scenesObj = data.get("scenes");
        if (!(scenesObj instanceof List<?> scenes)) {
            globalErrors.add("$.scenes must be array");
            return buildResponse(List.of(), globalWarnings, globalErrors);
        }

        List<ParseResponse.SceneCandidate> candidates = new ArrayList<>();
        int idx = 1;
        for (Object sceneObj : scenes) {
            if (!(sceneObj instanceof Map<?, ?> sceneMap)) {
                globalWarnings.add("$.scenes[" + (idx - 1) + "] skipped: not object");
                continue;
            }
            candidates.add(mapScene((Map<String, Object>) sceneMap, "json-" + idx));
            idx++;
        }

        return buildResponse(candidates, globalWarnings, globalErrors);
    }

    private ParseResponse buildResponse(List<ParseResponse.SceneCandidate> candidates,
                                        List<String> globalWarnings,
                                        List<String> globalErrors) {
        List<ParseResponse.ParseReportScene> reportScenes = candidates.stream()
                .map(c -> ParseResponse.ParseReportScene.builder()
                        .tempId(c.getTempId())
                        .titleGuess(c.getTitleGuess())
                        .confidence(estimateConfidence(c))
                        .fieldsMapped(Map.of())
                        .sqlBlocksFound(c.getDraftContent() != null && c.getDraftContent().getSqlBlocks() != null
                                ? c.getDraftContent().getSqlBlocks().size() : 0)
                        .warnings(c.getWarnings())
                        .errors(c.getErrors())
                        .build())
                .collect(Collectors.toList());

        ParseResponse.ParseReport report = ParseResponse.ParseReport.builder()
                .parser("json.import.v1")
                .mode("json_v1")
                .global_warnings(globalWarnings)
                .global_errors(globalErrors)
                .scenes(reportScenes)
                .build();

        return ParseResponse.builder()
                .mode("json_v1")
                .sceneCandidates(candidates)
                .parseReport(report)
                .build();
    }

    @SuppressWarnings("unchecked")
    private ParseResponse.SceneCandidate mapScene(Map<String, Object> s, String tempId) {
        String title = str(s, "scene_title");
        String sceneDescription = str(s, "scene_description");
        String caliberDefinition = str(s, "caliber_definition");
        String applicability = str(s, "applicability");
        String boundaries = str(s, "boundaries");
        List<String> entities = listOfString(s.get("entities"));
        List<String> contributors = listOfString(s.get("contributors"));
        List<String> tags = listOfString(s.get("keywords"));

        // Inputs
        Map<String, Object> inputs = map(s.get("inputs"));
        List<ParseResponse.InputParamDto> inputParams = new ArrayList<>();
        if (inputs != null) {
            Object paramsObj = inputs.get("params");
            if (paramsObj instanceof List<?> params) {
                for (Object pObj : params) {
                    Map<String, Object> p = map(pObj);
                    if (p == null) continue;
                    inputParams.add(ParseResponse.InputParamDto.builder()
                            .nameEn(str(p, "name_en"))
                            .nameZh(strOrFallback(p, "name_zh", "name"))
                            .type(str(p, "type"))
                            .required(bool(p, "required"))
                            .example(str(p, "example"))
                            .description(str(p, "description"))
                            .build());
                }
            }
        }

        // Outputs
        Map<String, Object> outputs = map(s.get("outputs"));
        String outputSummary = outputs != null ? str(outputs, "summary") : "";

        // SQL blocks
        List<ParseResponse.SqlBlockDto> sqlBlocks = new ArrayList<>();
        Object sqlBlocksObj = s.get("sql_blocks");
        if (sqlBlocksObj instanceof List<?> blocks) {
            int counter = 1;
            for (Object bObj : blocks) {
                Map<String, Object> b = map(bObj);
                if (b == null) continue;
                String blockId = strOrFallback(b, "block_id", "b" + counter);
                sqlBlocks.add(ParseResponse.SqlBlockDto.builder()
                        .blockId(blockId)
                        .name(strOrFallback(b, "name", "SQL块" + counter))
                        .condition(str(b, "condition"))
                        .sql(str(b, "sql"))
                        .notes(str(b, "notes"))
                        .build());
                counter++;
            }
        }

        // Caveats
        List<ParseResponse.CaveatDto> caveats = new ArrayList<>();
        Object caveatsObj = s.get("caveats");
        if (caveatsObj instanceof List<?> cvs) {
            for (Object cObj : cvs) {
                Map<String, Object> c = map(cObj);
                if (c == null) continue;
                caveats.add(ParseResponse.CaveatDto.builder()
                        .title(strOrFallback(c, "title", "注意事项"))
                        .text(str(c, "text"))
                        .risk(strOrFallback(c, "risk", str(c, "level")))
                        .build());
            }
        }

        // Quality
        Map<String, Object> quality = map(s.get("quality"));
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        if (quality != null) {
            warnings.addAll(listOfString(quality.get("warnings")));
            errors.addAll(listOfString(quality.get("errors")));
        }

        int tablesExtracted = extractTables(sqlBlocks).size();

        ParseResponse.DraftContent draftContent = ParseResponse.DraftContent.builder()
                .title(title)
                .contributors(contributors)
                .tags(tags)
                .sceneDescription(sceneDescription)
                .caliberDefinition(caliberDefinition)
                .applicability(applicability)
                .boundaries(boundaries)
                .entities(entities)
                .inputs(ParseResponse.InputsSection.builder()
                        .params(inputParams)
                        .constraints(List.of())
                        .build())
                .outputs(ParseResponse.OutputsSection.builder()
                        .summary(outputSummary)
                        .build())
                .sqlBlocks(sqlBlocks)
                .caveats(caveats)
                .build();

        return ParseResponse.SceneCandidate.builder()
                .tempId(tempId)
                .titleGuess(title)
                .draftContent(draftContent)
                .parseStats(ParseResponse.ParseStats.builder()
                        .sqlBlocks(sqlBlocks.size())
                        .tablesExtracted(tablesExtracted)
                        .build())
                .warnings(warnings)
                .errors(errors)
                .build();
    }

    private double estimateConfidence(ParseResponse.SceneCandidate c) {
        double score = 0.2; // base
        if (c.getTitleGuess() != null && !c.getTitleGuess().isBlank()) score += 0.3;
        if (c.getDraftContent() != null && c.getDraftContent().getSceneDescription() != null
                && !c.getDraftContent().getSceneDescription().isBlank()) score += 0.2;
        if (c.getDraftContent() != null && c.getDraftContent().getSqlBlocks() != null
                && !c.getDraftContent().getSqlBlocks().isEmpty()) score += 0.3;
        return Math.min(1.0, score);
    }

    private Set<String> extractTables(List<ParseResponse.SqlBlockDto> sqlBlocks) {
        Set<String> tables = new HashSet<>();
        if (sqlBlocks == null) return tables;
        for (ParseResponse.SqlBlockDto block : sqlBlocks) {
            String sql = block.getSql();
            if (sql == null) continue;
            Matcher matcher = TABLE_PATTERN.matcher(sql);
            while (matcher.find()) {
                tables.add(matcher.group(1));
            }
        }
        return tables;
    }

    private Map<String, Object> map(Object obj) {
        if (obj instanceof Map<?, ?> m) {
            Map<String, Object> casted = new LinkedHashMap<>();
            m.forEach((k, v) -> casted.put(String.valueOf(k), v));
            return casted;
        }
        return null;
    }

    private String str(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object v = map.get(key);
        return v instanceof String ? (String) v : null;
    }

    private String strOrFallback(Map<String, Object> map, String key, String fallback) {
        String value = str(map, key);
        return value != null && !value.isBlank() ? value : fallback;
    }

    private boolean bool(Map<String, Object> map, String key) {
        if (map == null) return false;
        Object v = map.get(key);
        if (v instanceof Boolean b) return b;
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<String> listOfString(Object obj) {
        if (!(obj instanceof List<?> list)) return List.of();
        List<String> result = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof String s) {
                result.add(s);
            }
        }
        return result;
    }
}
