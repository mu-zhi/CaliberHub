package com.caliberhub.domain.scene.valueobject;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

/**
 * 敏感字段 - 值对象
 */
@Getter
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = SensitiveField.Builder.class)
public class SensitiveField {

    /**
     * 表全名（schema.table）
     */
    private final String tableFullname;

    /**
     * 表名（简称）
     */
    private final String tableName;

    /**
     * 字段名
     */
    private final String fieldName;

    /**
     * 字段全名（schema.table.field）
     */
    private final String fieldFullname;

    /**
     * 元数据平台字段ID
     */
    private final String metadataFieldId;

    /**
     * 敏感等级
     */
    private final SensitivityLevel sensitivityLevel;

    /**
     * 脱敏规则
     */
    private final MaskRule maskRule;

    /**
     * 备注
     */
    private final String remarks;

    /**
     * 来源：MANUAL/SUGGESTED
     */
    private final String source;

    /**
     * 获取敏感等级字符串
     */
    public String getSensitivityLevel() {
        return sensitivityLevel != null ? sensitivityLevel.name() : null;
    }

    /**
     * 获取脱敏规则字符串
     */
    public String getMaskRule() {
        return maskRule != null ? maskRule.name() : null;
    }

    public static SensitiveField create(String fieldFullname, SensitivityLevel level, MaskRule rule) {
        String[] parts = fieldFullname.split("\\.");
        String tableName = parts.length >= 2 ? parts[0] + "." + parts[1] : "";
        String fieldNameOnly = parts.length >= 3 ? parts[2] : fieldFullname;

        return SensitiveField.builder()
                .fieldFullname(fieldFullname)
                .tableFullname(tableName)
                .tableName(parts.length >= 2 ? parts[1] : "")
                .fieldName(fieldNameOnly)
                .sensitivityLevel(level)
                .maskRule(rule)
                .source("MANUAL")
                .build();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
    }
}
