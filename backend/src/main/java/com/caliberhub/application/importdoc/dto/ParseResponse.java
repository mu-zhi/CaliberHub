package com.caliberhub.application.importdoc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 导入解析响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResponse {

    /**
     * 解析模式
     */
    private String mode;

    /**
     * 候选场景列表
     */
    private List<SceneCandidate> sceneCandidates;

    /**
     * 解析报告
     */
    private ParseReport parseReport;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SceneCandidate {
        private String tempId;
        private String titleGuess;
        private DraftContent draftContent;
        private ParseStats parseStats;
        private List<String> warnings;
        private List<String> errors;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DraftContent {
        private String title;
        private String domainId;
        private List<String> tags;
        private String ownerUser;
        private List<String> contributors;

        private String sceneDescription;
        private String caliberDefinition;
        private String applicability;
        private String boundaries;
        private List<String> entities;

        private InputsSection inputs;
        private OutputsSection outputs;

        private List<SqlBlockDto> sqlBlocks;
        private List<CaveatDto> caveats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputsSection {
        private List<InputParamDto> params;
        private List<InputConstraintDto> constraints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputsSection {
        private String summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputParamDto {
        private String nameEn;
        private String nameZh;
        private String type;
        private boolean required;
        private String example;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputConstraintDto {
        private String name;
        private String description;
        private boolean required;
        private String impact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SqlBlockDto {
        private String blockId;
        private String name;
        private String condition;
        private String sql;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaveatDto {
        private String title;
        private String text;
        private String risk; // LOW | MEDIUM | HIGH
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParseStats {
        private int sqlBlocks;
        private int tablesExtracted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParseReport {
        private String parser;
        private String mode;
        private List<String> global_warnings;
        private List<String> global_errors;
        private List<ParseReportScene> scenes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParseReportScene {
        private String tempId;
        private String titleGuess;
        private double confidence;
        private Map<String, String> fieldsMapped;
        private int sqlBlocksFound;
        private List<String> warnings;
        private List<String> errors;
    }
}
