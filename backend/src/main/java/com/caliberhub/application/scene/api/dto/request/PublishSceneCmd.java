package com.caliberhub.application.scene.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 发布场景命令
 */
@Data
public class PublishSceneCmd {
    
    @NotNull(message = "最后验证日期不能为空")
    private LocalDate lastVerifiedAt;
    
    @NotBlank(message = "验证人不能为空")
    private String verifiedBy;
    
    private String verifyEvidence;
    
    @NotBlank(message = "变更摘要不能为空")
    private String changeSummary;
}
