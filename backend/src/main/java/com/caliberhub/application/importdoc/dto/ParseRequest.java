package com.caliberhub.application.importdoc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入解析请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseRequest {

    /**
     * 来源类型: PASTE_MD | FILE_MD | FILE_TXT
     */
    private String sourceType;

    /**
     * 原始文本
     */
    private String rawText;

    /**
     * 解析模式: split_by_h2 | single_scene
     */
    private String mode;
}
