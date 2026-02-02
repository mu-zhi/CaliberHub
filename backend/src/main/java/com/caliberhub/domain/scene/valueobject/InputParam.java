package com.caliberhub.domain.scene.valueobject;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

/**
 * 输入参数 - 值对象
 */
@Getter
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = InputParam.Builder.class)
public class InputParam {

    /**
     * 参数英文名
     */
    private final String name;

    /**
     * 参数中文名
     */
    private final String displayName;

    /**
     * 参数类型：string/int/date/enum
     */
    private final String type;

    /**
     * 是否必填
     */
    private final boolean required;

    /**
     * 示例值
     */
    private final String example;

    /**
     * 说明
     */
    private final String description;

    public static InputParam create(String name, String displayName, String type, boolean required) {
        return InputParam.builder()
                .name(name)
                .displayName(displayName)
                .type(type)
                .required(required)
                .build();
    }

    public static InputParam of(String name, String displayName, String type,
            boolean required, String example, String description) {
        return InputParam.builder()
                .name(name)
                .displayName(displayName)
                .type(type)
                .required(required)
                .example(example)
                .description(description)
                .build();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
    }
}
