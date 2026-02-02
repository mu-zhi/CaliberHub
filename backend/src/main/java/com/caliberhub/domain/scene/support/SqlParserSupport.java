package com.caliberhub.domain.scene.support;

import java.util.List;

/**
 * SQL 解析支持接口 - 领域层防腐层
 * 由基础设施层实现
 */
public interface SqlParserSupport {
    
    /**
     * 从 SQL 中抽取表名
     * 
     * @param sql SQL 语句
     * @return 抽取结果
     */
    SqlParseResult extractTables(String sql);
    
    /**
     * 批量抽取多个 SQL 块的表名
     * 
     * @param sqlBlocks SQL 块列表
     * @return 汇总的抽取结果
     */
    SqlParseResult extractTablesFromBlocks(List<SqlBlockInput> sqlBlocks);
    
    /**
     * SQL 块输入
     */
    record SqlBlockInput(String blockId, String sql) {}
    
    /**
     * SQL 解析结果
     */
    record SqlParseResult(
        List<TableInfo> tables,
        boolean success,
        String errorMessage,
        ParseMethod method
    ) {
        public static SqlParseResult success(List<TableInfo> tables, ParseMethod method) {
            return new SqlParseResult(tables, true, null, method);
        }
        
        public static SqlParseResult failed(String errorMessage) {
            return new SqlParseResult(List.of(), false, errorMessage, ParseMethod.NONE);
        }
        
        public static SqlParseResult partial(List<TableInfo> tables, String errorMessage, ParseMethod method) {
            return new SqlParseResult(tables, true, errorMessage, method);
        }
    }
    
    /**
     * 表信息
     */
    record TableInfo(
        String tableFullname,
        String blockId,
        String source
    ) {
        public static TableInfo extracted(String tableFullname, String blockId) {
            return new TableInfo(tableFullname, blockId, "EXTRACTED");
        }
    }
    
    /**
     * 解析方法
     */
    enum ParseMethod {
        PARSER,    // JSqlParser 解析
        FALLBACK,  // 正则 fallback
        NONE       // 未解析
    }
}
