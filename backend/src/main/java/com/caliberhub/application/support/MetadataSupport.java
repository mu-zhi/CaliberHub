package com.caliberhub.application.support;

import java.util.List;

/**
 * 元数据支持接口 - 应用层防腐层
 */
public interface MetadataSupport {
    
    /**
     * 搜索表
     */
    List<TableSearchResult> searchTables(String keyword);
    
    /**
     * 获取表详情
     */
    TableDetail getTableDetail(String tableFullname);
    
    /**
     * 匹配表
     */
    MatchResult matchTable(String tableFullname);
    
    /**
     * 表搜索结果
     */
    record TableSearchResult(
        String tableFullname,
        String metadataTableId,
        String description,
        boolean hasSensitiveFields
    ) {}
    
    /**
     * 表详情
     */
    record TableDetail(
        String tableFullname,
        String metadataTableId,
        String schemaName,
        String tableName,
        String description,
        String sensitivitySummary,
        List<FieldInfo> fields
    ) {}
    
    /**
     * 字段信息
     */
    record FieldInfo(
        String fieldName,
        String fieldFullname,
        String metadataFieldId,
        String dataType,
        String description,
        String sensitivityLevel,
        boolean isSensitive
    ) {}
    
    /**
     * 匹配结果
     */
    record MatchResult(
        String tableFullname,
        String metadataTableId,
        MatchStatus status,
        String description,
        String message
    ) {
        public static MatchResult matched(String tableFullname, String metadataTableId, String description) {
            return new MatchResult(tableFullname, metadataTableId, MatchStatus.MATCHED, description, null);
        }
        
        public static MatchResult notFound(String tableFullname) {
            return new MatchResult(tableFullname, null, MatchStatus.NOT_FOUND, null, "表在元数据平台未找到");
        }
        
        public static MatchResult blacklisted(String tableFullname, String reason) {
            return new MatchResult(tableFullname, null, MatchStatus.BLACKLISTED, null, reason);
        }
        
        public static MatchResult verifyFailed(String tableFullname, String error) {
            return new MatchResult(tableFullname, null, MatchStatus.VERIFY_FAILED, null, error);
        }
    }
    
    /**
     * 匹配状态
     */
    enum MatchStatus {
        MATCHED,
        NOT_FOUND,
        BLACKLISTED,
        VERIFY_FAILED
    }
}
