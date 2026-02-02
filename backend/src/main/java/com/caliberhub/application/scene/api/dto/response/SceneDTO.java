package com.caliberhub.application.scene.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场景 DTO
 */
@Data
@Builder
public class SceneDTO {
    
    private String id;
    private String sceneCode;
    private String title;
    private String domainId;
    private String domainKey;
    private String domainName;
    private String lifecycleStatus;
    
    // 当前版本摘要
    private String currentVersionId;
    private String currentVersionLabel;
    private String versionStatus;
    private boolean hasSensitive;
    private LocalDateTime lastVerifiedAt;
    private String verifiedBy;
    
    private String ownerUser;
    private List<String> tags;
    
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
