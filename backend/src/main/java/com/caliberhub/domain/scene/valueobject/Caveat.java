package com.caliberhub.domain.scene.valueobject;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

/**
 * 注意事项/坑点 - 值对象
 */
@Getter
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = Caveat.Builder.class)
public class Caveat {

    /**
     * 注意事项ID
     */
    private final String id;

    /**
     * 标题
     */
    private final String title;

    /**
     * 风险等级：HIGH/MEDIUM/LOW
     */
    private final String risk;

    /**
     * 详细说明
     */
    private final String text;

    public static Caveat create(String id, String title, String text) {
        return Caveat.builder()
                .id(id)
                .title(title)
                .text(text)
                .risk("MEDIUM")
                .build();
    }

    public static Caveat of(String id, String title, String risk, String text) {
        return Caveat.builder()
                .id(id)
                .title(title)
                .risk(risk != null ? risk : "MEDIUM")
                .text(text)
                .build();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
    }
}
