package com.caliberhub.application.importdoc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 导入提交响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitResponse {

    /**
     * 创建的场景列表
     */
    private List<CreatedScene> createdScenes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatedScene {
        private String sceneCode;
        private String draftVersionId;
    }
}
