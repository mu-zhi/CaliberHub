package com.caliberhub.application.scene.api.dto.request;

import com.caliberhub.domain.scene.valueobject.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 保存草稿命令
 */
@Data
public class SaveDraftCmd {
    
    @NotBlank(message = "标题不能为空")
    private String title;
    
    private String ownerUser;
    
    private List<String> tags;
    
    private List<String> contributors;
    
    // 内容字段
    private String sceneDescription;
    
    private String caliberDefinition;
    
    private List<InputParamDto> inputParams;
    
    private String constraintsDescription;
    
    private String outputSummary;
    
    private List<SqlBlockDto> sqlBlocks;
    
    private List<CaveatDto> caveats;
    
    @Data
    public static class InputParamDto {
        private String name;
        private String displayName;
        private String type;
        private boolean required;
        private String example;
        private String description;
    }
    
    @Data
    public static class SqlBlockDto {
        private String blockId;
        private String name;
        private String condition;
        private String sql;
        private String notes;
    }
    
    @Data
    public static class CaveatDto {
        private String id;
        private String title;
        private String risk;
        private String text;
    }
}
