package com.caliberhub.domain.importdoc.service;

import com.caliberhub.application.importdoc.dto.ParseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown解析器
 * 将自由式口径文档解析为结构化的场景候选
 * 
 * 版本: import.rule.v0.1
 */
@Slf4j
@Service
public class MarkdownParser {

    public static final String PARSER_VERSION = "import.rule.v0.1";

    // 正则模式
    private static final Pattern H2_PATTERN = Pattern.compile("^##\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern FENCED_SQL_PATTERN = Pattern.compile("```(?:sql|SQL)\\s*\\n([\\s\\S]*?)```",
            Pattern.MULTILINE);
    private static final Pattern BLOCK_COMMENT_PATTERN = Pattern.compile("/\\*([\\s\\S]*?)\\*/");
    private static final Pattern LINE_COMMENT_PATTERN = Pattern.compile("^--\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern KEY_VALUE_PATTERN = Pattern
            .compile("^\\s*-\\s*(场景描述|业务背景|业务概述|口径来源|口径提供人|注意事项|常见坑|结果字段|输出字段)[:：]\\s*(.*)$", Pattern.MULTILINE);
    private static final Pattern CAVEAT_PATTERN = Pattern.compile("(不能用|不要用|仅展示近|需确认|没数据|会导致)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "(=\\s*''|=\\s*\"\"|\\.\\.\\.\\s*=|IN\\s*\\(\\s*\\)|=\\s*'99999'|=\\s*null)", Pattern.CASE_INSENSITIVE);

    /**
     * 解析文档
     * 
     * @param rawText 原始文本
     * @param mode    解析模式: split_by_h2 | single_scene
     * @return 解析响应
     */
    public ParseResponse parse(String rawText, String mode) {
        List<String> globalWarnings = new ArrayList<>();
        List<String> globalErrors = new ArrayList<>();

        // 检查原文大小
        if (rawText == null || rawText.isBlank()) {
            globalErrors.add("E_EMPTY_INPUT");
            return buildEmptyResponse(mode, globalWarnings, globalErrors);
        }

        // 超过1MB截断
        if (rawText.length() > 1_000_000) {
            rawText = rawText.substring(0, 1_000_000);
            globalWarnings.add("W_SOURCE_TRUNCATED");
        }

        // 根据模式切分
        List<TextSegment> segments;
        if ("split_by_h2".equals(mode)) {
            segments = splitByH2(rawText);
            if (segments.isEmpty()) {
                // 没有找到 ## 标题，回退到single_scene
                segments = List.of(new TextSegment(null, rawText));
                globalWarnings.add("W_NO_H2_FOUND");
            }
        } else {
            segments = List.of(new TextSegment(null, rawText));
        }

        // 解析每个segment
        List<ParseResponse.SceneCandidate> candidates = new ArrayList<>();
        List<ParseResponse.ParseReportScene> reportScenes = new ArrayList<>();

        int tempIdCounter = 1;
        for (TextSegment segment : segments) {
            String tempId = "tmp-" + tempIdCounter++;
            ParsedScene parsed = parseSegment(segment, tempId);
            candidates.add(parsed.candidate);
            reportScenes.add(parsed.reportScene);
        }

        // 构建解析报告
        ParseResponse.ParseReport report = ParseResponse.ParseReport.builder()
                .parser(PARSER_VERSION)
                .mode(mode)
                .global_warnings(globalWarnings)
                .global_errors(globalErrors)
                .scenes(reportScenes)
                .build();

        return ParseResponse.builder()
                .mode(mode)
                .sceneCandidates(candidates)
                .parseReport(report)
                .build();
    }

    /**
     * 按 ## 标题切分文档
     */
    private List<TextSegment> splitByH2(String text) {
        List<TextSegment> segments = new ArrayList<>();
        Matcher matcher = H2_PATTERN.matcher(text);

        List<int[]> matches = new ArrayList<>();
        List<String> titles = new ArrayList<>();

        while (matcher.find()) {
            matches.add(new int[] { matcher.start(), matcher.end() });
            titles.add(matcher.group(1).trim());
        }

        if (matches.isEmpty()) {
            return segments;
        }

        for (int i = 0; i < matches.size(); i++) {
            int start = matches.get(i)[1]; // 标题行结束后
            int end = (i + 1 < matches.size()) ? matches.get(i + 1)[0] : text.length();
            String content = text.substring(start, end).trim();
            segments.add(new TextSegment(titles.get(i), content));
        }

        return segments;
    }

    /**
     * 解析单个文档片段
     */
    private ParsedScene parseSegment(TextSegment segment, String tempId) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Map<String, String> fieldsMapped = new HashMap<>();

        String title = segment.title != null ? segment.title : "";
        String content = segment.content;

        // 1. 提取 fenced SQL code blocks
        List<ParseResponse.SqlBlockDto> sqlBlocks = new ArrayList<>();
        Matcher fencedMatcher = FENCED_SQL_PATTERN.matcher(content);
        List<String> fencedSqls = new ArrayList<>();

        while (fencedMatcher.find()) {
            fencedSqls.add(fencedMatcher.group(1).trim());
        }

        // 移除 fenced blocks 后的内容
        String contentWithoutFenced = FENCED_SQL_PATTERN.matcher(content).replaceAll("");

        // 2. 如果没有 fenced SQL，尝试按分号拆分独立SELECT（GT-01规则）
        if (fencedSqls.isEmpty()) {
            sqlBlocks = extractSqlBySemicolon(contentWithoutFenced, warnings);
        } else {
            // 有 fenced SQL，每个作为一个 block
            int blockCounter = 1;
            for (String sql : fencedSqls) {
                String blockId = "b" + blockCounter;
                String blockName = findSqlBlockName(content, sql, blockCounter);

                // 检查占位符
                if (PLACEHOLDER_PATTERN.matcher(sql).find()) {
                    warnings.add("W_HAS_PLACEHOLDER");
                }

                sqlBlocks.add(ParseResponse.SqlBlockDto.builder()
                        .blockId(blockId)
                        .name(blockName)
                        .sql(sql)
                        .build());
                blockCounter++;
            }
        }

        // 3. 提取块注释作为描述
        StringBuilder descriptionBuilder = new StringBuilder();
        Matcher blockCommentMatcher = BLOCK_COMMENT_PATTERN.matcher(content);
        while (blockCommentMatcher.find()) {
            String comment = blockCommentMatcher.group(1).trim();
            if (!comment.isEmpty()) {
                descriptionBuilder.append(comment).append("\n");
            }
        }

        // 4. 提取键值对字段
        String sceneDescription = descriptionBuilder.toString().trim();
        String caliberSource = "";
        List<ParseResponse.CaveatDto> caveats = new ArrayList<>();

        Matcher kvMatcher = KEY_VALUE_PATTERN.matcher(contentWithoutFenced);
        while (kvMatcher.find()) {
            String key = kvMatcher.group(1);
            String value = kvMatcher.group(2).trim();

            switch (key) {
                case "场景描述":
                case "业务背景":
                case "业务概述":
                    if (!sceneDescription.isEmpty())
                        sceneDescription += "\n";
                    sceneDescription += value;
                    break;
                case "口径来源":
                case "口径提供人":
                    caliberSource = value;
                    break;
                case "注意事项":
                case "常见坑":
                    caveats.add(ParseResponse.CaveatDto.builder()
                            .title(key)
                            .text(value)
                            .risk("MEDIUM")
                            .build());
                    break;
            }
        }

        // 5. 从文本中提取 caveat（关键词匹配）
        extractCaveatsFromText(content, caveats);

        // 6. 未映射的文本追加到描述
        String unmappedText = extractUnmappedText(contentWithoutFenced, sceneDescription);
        if (!unmappedText.isEmpty() && sceneDescription.isEmpty()) {
            sceneDescription = unmappedText;
            warnings.add("W_UNMAPPED_TEXT_TO_DESC");
        }

        // 7. 如果标题为空，尝试从描述中提取
        if (title.isEmpty() && !sceneDescription.isEmpty()) {
            String[] lines = sceneDescription.split("\n");
            if (lines.length > 0 && lines[0].length() < 50) {
                title = lines[0].trim();
            }
        }

        // 8. 检查是否缺少领域
        warnings.add("W_NO_DOMAIN");

        // 9. 统计表名抽取（简化：从SQL中匹配FROM/JOIN后的表名）
        Set<String> tables = extractTablesFromSql(sqlBlocks);

        // 构建字段映射置信度
        fieldsMapped.put("sceneDescription", sceneDescription.isEmpty() ? "NONE" : "MEDIUM");
        fieldsMapped.put("caliberDefinition", "NONE");
        fieldsMapped.put("contributors", caliberSource.isEmpty() ? "NONE" : "LOW");
        fieldsMapped.put("caveats", caveats.isEmpty() ? "NONE" : "MEDIUM");
        fieldsMapped.put("sqlBlocks", sqlBlocks.isEmpty() ? "NONE" : "HIGH");

        // 构建 draftContent
        ParseResponse.DraftContent draftContent = ParseResponse.DraftContent.builder()
                .title(title)
                .sceneDescription(sceneDescription)
                .contributors(caliberSource.isEmpty() ? List.of() : List.of(caliberSource))
                .sqlBlocks(sqlBlocks)
                .caveats(caveats)
                .inputs(ParseResponse.InputsSection.builder()
                        .params(List.of())
                        .constraints(List.of())
                        .build())
                .outputs(ParseResponse.OutputsSection.builder()
                        .summary("")
                        .build())
                .tags(List.of())
                .entities(List.of())
                .build();

        // 构建候选
        ParseResponse.SceneCandidate candidate = ParseResponse.SceneCandidate.builder()
                .tempId(tempId)
                .titleGuess(title)
                .draftContent(draftContent)
                .parseStats(ParseResponse.ParseStats.builder()
                        .sqlBlocks(sqlBlocks.size())
                        .tablesExtracted(tables.size())
                        .build())
                .warnings(warnings)
                .errors(errors)
                .build();

        // 构建报告场景
        ParseResponse.ParseReportScene reportScene = ParseResponse.ParseReportScene.builder()
                .tempId(tempId)
                .titleGuess(title)
                .confidence(calculateConfidence(sqlBlocks.size(), !sceneDescription.isEmpty()))
                .fieldsMapped(fieldsMapped)
                .sqlBlocksFound(sqlBlocks.size())
                .warnings(warnings)
                .errors(errors)
                .build();

        return new ParsedScene(candidate, reportScene);
    }

    /**
     * 按分号拆分独立SELECT语句（GT-01核心规则）
     * 注意：WITH链条保持完整（GT-02）
     */
    private List<ParseResponse.SqlBlockDto> extractSqlBySemicolon(String text, List<String> warnings) {
        List<ParseResponse.SqlBlockDto> blocks = new ArrayList<>();

        // 先移除块注释
        String textWithoutBlockComments = BLOCK_COMMENT_PATTERN.matcher(text).replaceAll("");

        // 按分号分割
        String[] statements = textWithoutBlockComments.split(";");

        int blockCounter = 1;
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty())
                continue;

            // 检查是否包含 SELECT 或 WITH（SQL语句标志）
            boolean hasSelect = trimmed.toUpperCase().contains("SELECT");
            boolean hasWith = trimmed.toUpperCase().startsWith("WITH");

            if (hasSelect || hasWith) {
                String blockId = "b" + blockCounter;

                // 检查占位符
                if (PLACEHOLDER_PATTERN.matcher(trimmed).find()) {
                    warnings.add("W_HAS_PLACEHOLDER");
                }

                // 尝试从前面的行注释提取名称
                String blockName = "SQL块" + blockCounter;
                Matcher lineCommentMatcher = LINE_COMMENT_PATTERN.matcher(stmt);
                if (lineCommentMatcher.find()) {
                    String comment = lineCommentMatcher.group(1).trim();
                    if (comment.length() < 30) {
                        blockName = comment;
                    }
                }

                blocks.add(ParseResponse.SqlBlockDto.builder()
                        .blockId(blockId)
                        .name(blockName)
                        .sql(trimmed)
                        .build());

                blockCounter++;
            }
        }

        return blocks;
    }

    /**
     * 为SQL块寻找名称（从前5行的注释中）
     */
    private String findSqlBlockName(String content, String sql, int blockNum) {
        int sqlIndex = content.indexOf(sql);
        if (sqlIndex > 0) {
            // 取SQL前面的内容
            String before = content.substring(Math.max(0, sqlIndex - 200), sqlIndex);
            String[] lines = before.split("\n");

            // 从后往前找注释行
            for (int i = lines.length - 1; i >= Math.max(0, lines.length - 5); i--) {
                String line = lines[i].trim();
                if (line.startsWith("--") || line.startsWith("脚本") || line.contains("：")) {
                    String name = line.replaceAll("^--\\s*", "").replaceAll("^脚本\\d*[：:]\\s*", "").trim();
                    if (!name.isEmpty() && name.length() < 30) {
                        return name;
                    }
                }
            }
        }
        return "SQL块" + blockNum;
    }

    /**
     * 从文本中提取caveats
     */
    private void extractCaveatsFromText(String content, List<ParseResponse.CaveatDto> caveats) {
        String[] sentences = content.split("[。；;\\n]");
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() > 10 && CAVEAT_PATTERN.matcher(trimmed).find()) {
                // 避免重复
                boolean exists = caveats.stream().anyMatch(c -> c.getText().contains(trimmed));
                if (!exists && caveats.size() < 5) {
                    String risk = trimmed.contains("不能") || trimmed.contains("没数据") ? "HIGH" : "MEDIUM";
                    caveats.add(ParseResponse.CaveatDto.builder()
                            .title("注意事项")
                            .text(trimmed)
                            .risk(risk)
                            .build());
                }
            }
        }
    }

    /**
     * 提取未映射的文本
     */
    private String extractUnmappedText(String content, String alreadyMapped) {
        // 简化：移除已知模式后的剩余文本
        String remaining = content;
        remaining = KEY_VALUE_PATTERN.matcher(remaining).replaceAll("");
        remaining = LINE_COMMENT_PATTERN.matcher(remaining).replaceAll("");
        remaining = BLOCK_COMMENT_PATTERN.matcher(remaining).replaceAll("");
        remaining = remaining.replaceAll("\\s+", " ").trim();

        if (remaining.length() > 500) {
            remaining = remaining.substring(0, 500) + "...";
        }

        return remaining;
    }

    /**
     * 从SQL中简单抽取表名
     */
    private Set<String> extractTablesFromSql(List<ParseResponse.SqlBlockDto> sqlBlocks) {
        Set<String> tables = new HashSet<>();
        Pattern tablePattern = Pattern.compile("(?:FROM|JOIN)\\s+([a-zA-Z_][a-zA-Z0-9_]*\\.?[a-zA-Z0-9_]*)",
                Pattern.CASE_INSENSITIVE);

        for (ParseResponse.SqlBlockDto block : sqlBlocks) {
            Matcher matcher = tablePattern.matcher(block.getSql());
            while (matcher.find()) {
                tables.add(matcher.group(1));
            }
        }
        return tables;
    }

    /**
     * 计算置信度
     */
    private double calculateConfidence(int sqlBlockCount, boolean hasDescription) {
        double score = 0.0;
        if (sqlBlockCount > 0)
            score += 0.5;
        if (sqlBlockCount > 2)
            score += 0.2;
        if (hasDescription)
            score += 0.3;
        return Math.min(1.0, score);
    }

    private ParseResponse buildEmptyResponse(String mode, List<String> warnings, List<String> errors) {
        return ParseResponse.builder()
                .mode(mode)
                .sceneCandidates(List.of())
                .parseReport(ParseResponse.ParseReport.builder()
                        .parser(PARSER_VERSION)
                        .mode(mode)
                        .global_warnings(warnings)
                        .global_errors(errors)
                        .scenes(List.of())
                        .build())
                .build();
    }

    // 内部类
    private record TextSegment(String title, String content) {
    }

    private record ParsedScene(ParseResponse.SceneCandidate candidate, ParseResponse.ParseReportScene reportScene) {
    }
}
