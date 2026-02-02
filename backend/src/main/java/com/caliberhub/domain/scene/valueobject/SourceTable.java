package com.caliberhub.domain.scene.valueobject;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

/**
 * 数据来源表 - 值对象
 */
@Getter
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = SourceTable.Builder.class)
public class SourceTable {
    
    /**
     * 表全名（schema.table）
     */
    private final String tableFullname;
    
    /**
     * 元数据平台表ID
     */
    private final String metadataTableId;
    
    /**
     * 匹配状态
     */
    private final TableMatchStatus matchStatus;
    
    /**
     * 是否关键表
     */
    private final boolean keyTable;
    
    /**
     * 用途：FACT/DIM/LOG/INTERMEDIATE
     */
    private final String usageType;
    
    /**
     * 分区字段
     */
    private final String partitionField;
    
    /**
     * 来源：EXTRACTED/MANUAL
     */
    private final String source;
    
    /**
     * 表描述
     */
    private final String description;
    
    /**
     * 从SQL抽取创建
     */
    public static SourceTable fromExtraction(String tableFullname) {
        return SourceTable.builder()
                .tableFullname(tableFullname)
                .matchStatus(TableMatchStatus.NOT_FOUND)
                .keyTable(true)
                .source("EXTRACTED")
                .build();
    }
    
    /**
     * 匹配成功后更新
     */
    public SourceTable withMatched(String metadataTableId, String description) {
        return SourceTable.builder()
                .tableFullname(this.tableFullname)
                .metadataTableId(metadataTableId)
                .matchStatus(TableMatchStatus.MATCHED)
                .keyTable(this.keyTable)
                .usageType(this.usageType)
                .partitionField(this.partitionField)
                .source(this.source)
                .description(description)
                .build();
    }
    
    /**
     * 标记为黑名单
     */
    public SourceTable markAsBlacklisted() {
        return SourceTable.builder()
                .tableFullname(this.tableFullname)
                .matchStatus(TableMatchStatus.BLACKLISTED)
                .keyTable(this.keyTable)
                .source(this.source)
                .build();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
    }
}
