package com.caliberhub.infrastructure.scene.supportimpl;

import com.caliberhub.domain.scene.support.SqlParserSupport;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 解析支持实现
 * 优先使用 JSqlParser，失败时 fallback 到正则
 */
@Slf4j
@Component
public class SqlParserSupportImpl implements SqlParserSupport {
    
    // 黑名单前缀（临时表、CTE 等）
    private static final Set<String> BLACKLIST_PREFIXES = Set.of(
        "tmp_", "temp_", "cte_", "#"
    );
    
    // Fallback 正则：匹配 FROM/JOIN 后的表名
    private static final Pattern TABLE_PATTERN = Pattern.compile(
        "(?:FROM|JOIN)\\s+([`\"\\[]?[\\w]+[`\"\\]]?\\.)?([`\"\\[]?[\\w]+[`\"\\]]?)(?:\\s+(?:AS\\s+)?[\\w]+)?",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public SqlParseResult extractTables(String sql) {
        if (sql == null || sql.isBlank()) {
            return SqlParseResult.failed("SQL 为空");
        }
        
        // 清理 SQL
        String cleanedSql = cleanSql(sql);
        
        // 尝试使用 JSqlParser
        try {
            List<String> tables = parseWithJSqlParser(cleanedSql);
            List<TableInfo> tableInfos = tables.stream()
                    .map(t -> TableInfo.extracted(t, null))
                    .toList();
            return SqlParseResult.success(tableInfos, ParseMethod.PARSER);
        } catch (Exception e) {
            log.debug("JSqlParser 解析失败，尝试 fallback: {}", e.getMessage());
        }
        
        // Fallback 到正则
        try {
            List<String> tables = parseWithRegex(cleanedSql);
            if (tables.isEmpty()) {
                return SqlParseResult.failed("未能抽取到表名");
            }
            List<TableInfo> tableInfos = tables.stream()
                    .map(t -> TableInfo.extracted(t, null))
                    .toList();
            return SqlParseResult.partial(tableInfos, "使用正则 fallback 解析", ParseMethod.FALLBACK);
        } catch (Exception e) {
            log.error("正则解析也失败: ", e);
            return SqlParseResult.failed("SQL 解析失败: " + e.getMessage());
        }
    }
    
    @Override
    public SqlParseResult extractTablesFromBlocks(List<SqlBlockInput> sqlBlocks) {
        if (sqlBlocks == null || sqlBlocks.isEmpty()) {
            return SqlParseResult.failed("没有 SQL 块");
        }
        
        List<TableInfo> allTables = new ArrayList<>();
        Set<String> seenTables = new HashSet<>();
        StringBuilder errors = new StringBuilder();
        ParseMethod finalMethod = ParseMethod.PARSER;
        
        for (SqlBlockInput block : sqlBlocks) {
            SqlParseResult result = extractTables(block.sql());
            
            if (result.method() == ParseMethod.FALLBACK) {
                finalMethod = ParseMethod.FALLBACK;
            }
            
            if (!result.success() && result.tables().isEmpty()) {
                errors.append("Block ").append(block.blockId()).append(": ").append(result.errorMessage()).append("; ");
                continue;
            }
            
            for (TableInfo table : result.tables()) {
                String fullname = table.tableFullname().toLowerCase();
                if (!seenTables.contains(fullname)) {
                    seenTables.add(fullname);
                    allTables.add(TableInfo.extracted(table.tableFullname(), block.blockId()));
                }
            }
        }
        
        if (allTables.isEmpty() && errors.length() > 0) {
            return SqlParseResult.failed(errors.toString());
        }
        
        String errorMsg = errors.length() > 0 ? errors.toString() : null;
        if (errorMsg != null) {
            return SqlParseResult.partial(allTables, errorMsg, finalMethod);
        }
        return SqlParseResult.success(allTables, finalMethod);
    }
    
    /**
     * 使用 JSqlParser 解析
     */
    private List<String> parseWithJSqlParser(String sql) throws JSQLParserException {
        Statement statement = CCJSqlParserUtil.parse(sql);
        
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        List<String> tableList = tablesNamesFinder.getTableList(statement);
        
        return tableList.stream()
                .map(this::normalizeTableName)
                .filter(this::isValidTable)
                .distinct()
                .toList();
    }
    
    /**
     * 使用正则 fallback 解析
     */
    private List<String> parseWithRegex(String sql) {
        Set<String> tables = new HashSet<>();
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        
        while (matcher.find()) {
            String schema = matcher.group(1);
            String table = matcher.group(2);
            
            if (table == null || table.isBlank()) {
                continue;
            }
            
            String fullname;
            if (schema != null && !schema.isBlank()) {
                fullname = normalizeTableName(schema + table);
            } else {
                fullname = normalizeTableName(table);
            }
            
            if (isValidTable(fullname)) {
                tables.add(fullname);
            }
        }
        
        return new ArrayList<>(tables);
    }
    
    /**
     * 标准化表名
     */
    private String normalizeTableName(String tableName) {
        if (tableName == null) {
            return "";
        }
        // 去除引号、反引号、方括号
        return tableName
                .replace("`", "")
                .replace("\"", "")
                .replace("[", "")
                .replace("]", "")
                .trim()
                .toLowerCase();
    }
    
    /**
     * 清理 SQL
     */
    private String cleanSql(String sql) {
        // 移除单行注释
        String cleaned = sql.replaceAll("--.*$", "");
        // 移除多行注释
        cleaned = cleaned.replaceAll("/\\*.*?\\*/", "");
        return cleaned.trim();
    }
    
    /**
     * 检查是否为有效表名（排除临时表等）
     */
    private boolean isValidTable(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return false;
        }
        
        String lower = tableName.toLowerCase();
        for (String prefix : BLACKLIST_PREFIXES) {
            if (lower.startsWith(prefix) || lower.contains("." + prefix)) {
                return false;
            }
        }
        
        // 排除可能的子查询别名
        if (lower.matches("^[a-z]$") || lower.matches("^t\\d+$")) {
            return false;
        }
        
        return true;
    }
}
