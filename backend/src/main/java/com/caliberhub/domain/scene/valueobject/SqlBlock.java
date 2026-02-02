package com.caliberhub.domain.scene.valueobject;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * SQL方案块 - 值对象
 * 场景中的一段SQL代码块，可有多个
 */
@Getter
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = SqlBlock.Builder.class)
public class SqlBlock {

    /**
     * 块ID
     */
    private final String blockId;

    /**
     * 方案名称，如"Step1: 查询客户资料"
     */
    private final String name;

    /**
     * 适用条件，如"2C系统；2013年后"
     */
    private final String condition;

    /**
     * SQL正文
     */
    private final String sql;

    /**
     * 抽取到的表名列表
     */
    private final List<String> extractedTables;

    /**
     * 备注
     */
    private final String notes;

    /**
     * 创建SQL块
     */
    public static SqlBlock create(String blockId, String name, String sql) {
        return SqlBlock.builder()
                .blockId(blockId)
                .name(name)
                .sql(sql)
                .extractedTables(List.of())
                .build();
    }

    /**
     * 更新抽取的表名
     */
    public SqlBlock withExtractedTables(List<String> tables) {
        return SqlBlock.builder()
                .blockId(this.blockId)
                .name(this.name)
                .condition(this.condition)
                .sql(this.sql)
                .extractedTables(tables)
                .notes(this.notes)
                .build();
    }

    public static SqlBlock of(String blockId, String name, String condition,
            String sql, String notes) {
        return SqlBlock.builder()
                .blockId(blockId)
                .name(name)
                .condition(condition)
                .sql(sql)
                .notes(notes)
                .extractedTables(List.of())
                .build();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
    }
}
