package com.caliberhub.application.scene.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场景版本 DTO
 */
@Data
@Builder
public class SceneVersionDTO {
    
    private String id;
    private String sceneId;
    private String sceneCode;
    private String status;
    private boolean isCurrent;
    
    private int versionSeq;
    private String versionLabel;
    
    private String title;
    private List<String> tags;
    private String ownerUser;
    private List<String> contributors;
    
    private boolean hasSensitive;
    private LocalDateTime lastVerifiedAt;
    private String verifiedBy;
    private String verifyEvidence;
    private String changeSummary;
    
    private LocalDateTime publishedAt;
    private String publishedBy;
    
    // 内容
    private SceneVersionContentDTO content;
    
    // Lint 结果
    private LintResultDTO lintResult;
    
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    
    @Data
    @Builder
    public static class SceneVersionContentDTO {
        private String sceneDescription;
        private String caliberDefinition;
        private List<InputParamDTO> inputParams;
        private String constraintsDescription;
        private String outputSummary;
        private List<SqlBlockDTO> sqlBlocks;
        private List<SourceTableDTO> sourceTables;
        private List<SensitiveFieldDTO> sensitiveFields;
        private List<CaveatDTO> caveats;
    }
    
    @Data
    @Builder
    public static class InputParamDTO {
        private String name;
        private String displayName;
        private String type;
        private boolean required;
        private String example;
        private String description;
    }
    
    @Data
    @Builder
    public static class SqlBlockDTO {
        private String blockId;
        private String name;
        private String condition;
        private String sql;
        private String notes;
        private List<String> extractedTables;
    }
    
    @Data
    @Builder
    public static class SourceTableDTO {
        private String tableFullname;
        private String metadataTableId;
        private String matchStatus;
        private boolean isKey;
        private String usageType;
        private String partitionField;
        private String source;
        private String description;
    }
    
    @Data
    @Builder
    public static class SensitiveFieldDTO {
        private String fieldFullname;
        private String tableName;
        private String fieldName;
        private String sensitivityLevel;
        private String maskRule;
        private String remarks;
    }
    
    @Data
    @Builder
    public static class CaveatDTO {
        private String id;
        private String title;
        private String risk;
        private String text;
    }
    
    @Data
    @Builder
    public static class LintResultDTO {
        private boolean passed;
        private int errorCount;
        private int warningCount;
        private List<LintIssueDTO> errors;
        private List<LintIssueDTO> warnings;
    }
    
    @Data
    @Builder
    public static class LintIssueDTO {
        private String id;
        private String message;
        private String path;
        private String blockId;
    }
}
