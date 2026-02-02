package com.caliberhub.application.scene.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 创建场景命令
 */
@Data
public class CreateSceneCmd {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String domainId;

    private String ownerUser;

    private List<String> tags;

    private String sceneDescription;

    private String caliberDefinition;
}
