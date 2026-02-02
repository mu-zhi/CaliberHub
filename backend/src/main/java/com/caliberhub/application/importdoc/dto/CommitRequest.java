package com.caliberhub.application.importdoc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 导入提交请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitRequest {

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 原始文本
     */
    private String rawText;

    /**
     * 解析模式
     */
    private String mode;

    /**
     * 选中的候选场景tempId列表
     */
    private List<String> selectedTempIds;

    /**
     * 默认领域ID（可选）
     */
    private String defaultDomainId;
}
