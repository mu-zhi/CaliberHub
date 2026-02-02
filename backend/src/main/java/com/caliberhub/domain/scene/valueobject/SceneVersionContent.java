package com.caliberhub.domain.scene.valueobject;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 场景版本内容 - 值对象
 * 包含场景表单的所有结构化内容
 */
@Getter
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = SceneVersionContent.Builder.class)
public class SceneVersionContent {

    // ===== 口径定义 =====

    /**
     * 场景描述
     */
    private final String sceneDescription;

    /**
     * 口径定义
     */
    private final String caliberDefinition;

    /**
     * 适用范围
     */
    private final String applicability;

    /**
     * 边界/不适用情况
     */
    private final String boundaries;

    /**
     * 业务实体列表
     */
    private final List<String> entities;

    // ===== 输入与限制 =====

    /**
     * 输入参数列表
     */
    private final List<InputParam> inputParams;

    /**
     * 查询约束说明
     */
    private final String constraintsDescription;

    // ===== 输出 =====

    /**
     * 输出摘要
     */
    private final String outputSummary;

    // ===== SQL方案 =====

    /**
     * SQL方案块列表
     */
    private final List<SqlBlock> sqlBlocks;

    // ===== 数据来源 =====

    /**
     * 数据来源表列表
     */
    private final List<SourceTable> sourceTables;

    // ===== 敏感字段 =====

    /**
     * 敏感字段列表
     */
    private final List<SensitiveField> sensitiveFields;

    // ===== 注意事项 =====

    /**
     * 注意事项列表
     */
    private final List<Caveat> caveats;

    /**
     * 创建空内容
     */
    public static SceneVersionContent empty() {
        return SceneVersionContent.builder()
                .inputParams(List.of())
                .sqlBlocks(List.of())
                .sourceTables(List.of())
                .sensitiveFields(List.of())
                .caveats(List.of())
                .entities(List.of())
                .build();
    }

    /**
     * 是否包含敏感字段
     */
    public boolean hasSensitiveFields() {
        return sensitiveFields != null && !sensitiveFields.isEmpty();
    }

    /**
     * 获取所有抽取的表名
     */
    public List<String> getAllExtractedTables() {
        if (sqlBlocks == null) {
            return List.of();
        }
        return sqlBlocks.stream()
                .filter(block -> block.getExtractedTables() != null)
                .flatMap(block -> block.getExtractedTables().stream())
                .distinct()
                .toList();
    }

    /**
     * 使用新的数据来源表列表创建副本
     */
    public SceneVersionContent withSourceTables(List<SourceTable> sourceTables) {
        return SceneVersionContent.builder()
                .sceneDescription(this.sceneDescription)
                .caliberDefinition(this.caliberDefinition)
                .applicability(this.applicability)
                .boundaries(this.boundaries)
                .entities(this.entities)
                .inputParams(this.inputParams)
                .constraintsDescription(this.constraintsDescription)
                .outputSummary(this.outputSummary)
                .sqlBlocks(this.sqlBlocks)
                .sourceTables(sourceTables)
                .sensitiveFields(this.sensitiveFields)
                .caveats(this.caveats)
                .build();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
    }
}
