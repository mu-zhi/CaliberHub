package com.cmbchina.datadirect.caliber.application.service.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlTableParseSupport {

    private static final Pattern SOURCE_TABLE_FALLBACK = Pattern.compile("(?i)\\b(?:from|join|using)\\s+([a-zA-Z0-9_.$`\"]+)");
    private static final Pattern TARGET_TABLE_FALLBACK = Pattern.compile("(?i)\\b(?:insert\\s+into|merge\\s+into|update\\s+(?!set\\b))\\s+([a-zA-Z0-9_.$`\"]+)");
    private static final Set<String> CLAUSE_BREAKERS = Set.of(
            "where", "group", "order", "having", "limit", "union", "intersect", "except", "qualify",
            "window", "connect", "minus", "on", "set"
    );
    private static final Set<String> JOIN_MODIFIERS = Set.of(
            "left", "right", "inner", "outer", "full", "cross", "natural", "straight_join", "lateral"
    );

    private SqlTableParseSupport() {
    }

    public static ParseResult parse(String sql) {
        if (sql == null || sql.isBlank()) {
            return new ParseResult(List.of(), List.of(), List.of());
        }
        String normalized = stripCommentsAndStrings(sql);
        List<String> tokens = tokenize(normalized);

        LinkedHashSet<String> sourceTables = new LinkedHashSet<>();
        LinkedHashSet<String> targetTables = new LinkedHashSet<>();
        List<String> errors = new ArrayList<>();

        int balance = 0;
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if ("(".equals(token)) {
                balance++;
                continue;
            }
            if (")".equals(token)) {
                balance--;
                continue;
            }
            String lower = lower(token);
            if ("from".equals(lower)) {
                i = parseFromTables(tokens, i + 1, sourceTables);
                continue;
            }
            if ("join".equals(lower)) {
                i = parseSingleTable(tokens, i + 1, sourceTables);
                continue;
            }
            if ("using".equals(lower)) {
                i = parseSingleTable(tokens, i + 1, sourceTables);
                continue;
            }
            if ("update".equals(lower)) {
                i = parseSingleTable(tokens, i + 1, targetTables);
                continue;
            }
            if ("insert".equals(lower)) {
                int cursor = i + 1;
                if (cursor < tokens.size() && "into".equals(lower(tokens.get(cursor)))) {
                    cursor++;
                }
                i = parseSingleTable(tokens, cursor, targetTables);
                continue;
            }
            if ("merge".equals(lower)) {
                int cursor = i + 1;
                if (cursor < tokens.size() && "into".equals(lower(tokens.get(cursor)))) {
                    cursor++;
                }
                i = parseSingleTable(tokens, cursor, targetTables);
            }
        }

        if (balance != 0) {
            errors.add("sql_parentheses_unbalanced");
        }

        fallbackExtract(normalized, SOURCE_TABLE_FALLBACK, sourceTables);
        fallbackExtract(normalized, TARGET_TABLE_FALLBACK, targetTables);
        return new ParseResult(List.copyOf(sourceTables), List.copyOf(targetTables), List.copyOf(errors));
    }

    private static int parseFromTables(List<String> tokens, int start, Set<String> output) {
        int cursor = start;
        while (cursor < tokens.size()) {
            String token = lower(tokens.get(cursor));
            if (CLAUSE_BREAKERS.contains(token) || "join".equals(token)) {
                return cursor - 1;
            }
            if (",".equals(tokens.get(cursor))) {
                cursor++;
                continue;
            }
            if ("(".equals(tokens.get(cursor))) {
                cursor = skipParentheses(tokens, cursor);
                continue;
            }
            cursor = parseSingleTable(tokens, cursor, output) + 1;
            if (cursor >= tokens.size()) {
                return tokens.size() - 1;
            }
            if (!",".equals(tokens.get(cursor))) {
                return cursor - 1;
            }
        }
        return tokens.size() - 1;
    }

    private static int parseSingleTable(List<String> tokens, int start, Set<String> output) {
        int cursor = start;
        while (cursor < tokens.size() && JOIN_MODIFIERS.contains(lower(tokens.get(cursor)))) {
            cursor++;
        }
        if (cursor >= tokens.size()) {
            return tokens.size() - 1;
        }
        String token = tokens.get(cursor);
        if ("(".equals(token)) {
            return skipParentheses(tokens, cursor);
        }
        if (isKeyword(token)) {
            return cursor;
        }
        String table = normalizeIdentifier(token);
        if (!table.isEmpty()) {
            output.add(table);
        }
        return cursor;
    }

    private static int skipParentheses(List<String> tokens, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < tokens.size(); i++) {
            if ("(".equals(tokens.get(i))) {
                depth++;
            } else if (")".equals(tokens.get(i))) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return tokens.size() - 1;
    }

    private static void fallbackExtract(String sql, Pattern pattern, Set<String> output) {
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            String table = normalizeIdentifier(matcher.group(1));
            if (!table.isEmpty()) {
                output.add(table);
            }
        }
    }

    private static String stripCommentsAndStrings(String sql) {
        String noBlock = sql.replaceAll("(?s)/\\*.*?\\*/", " ");
        String noLine = noBlock.replaceAll("(?m)--[^\\n\\r]*", " ");
        return noLine.replaceAll("'([^'\\\\]|\\\\.)*'", " ");
    }

    private static List<String> tokenize(String sql) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '$' || ch == '.' || ch == '`' || ch == '"') {
                current.append(ch);
                continue;
            }
            flush(current, tokens);
            if (ch == '(' || ch == ')' || ch == ',') {
                tokens.add(String.valueOf(ch));
            }
        }
        flush(current, tokens);
        return tokens;
    }

    private static void flush(StringBuilder current, List<String> tokens) {
        if (current.length() == 0) {
            return;
        }
        tokens.add(current.toString());
        current.setLength(0);
    }

    private static String normalizeIdentifier(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.trim();
        while (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        if (normalized.startsWith("(")) {
            return "";
        }
        normalized = normalized.replace("`", "").replace("\"", "");
        if (normalized.isBlank() || isKeyword(normalized)) {
            return "";
        }
        return normalized;
    }

    private static boolean isKeyword(String token) {
        String lower = lower(token);
        return CLAUSE_BREAKERS.contains(lower)
                || JOIN_MODIFIERS.contains(lower)
                || "join".equals(lower)
                || "as".equals(lower)
                || "select".equals(lower)
                || "from".equals(lower)
                || "update".equals(lower)
                || "insert".equals(lower)
                || "into".equals(lower)
                || "merge".equals(lower)
                || "using".equals(lower)
                || "with".equals(lower);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public record ParseResult(
            List<String> sourceTables,
            List<String> targetTables,
            List<String> parseErrors
    ) {
    }
}
