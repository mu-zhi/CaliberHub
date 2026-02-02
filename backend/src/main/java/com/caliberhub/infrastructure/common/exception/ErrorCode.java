package com.caliberhub.infrastructure.common.exception;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {
    
    // 通用错误 (1xxx)
    SUCCESS("0000", "操作成功"),
    SYSTEM_ERROR("1000", "系统内部错误"),
    PARAM_INVALID("1001", "参数校验失败"),
    NOT_FOUND("1002", "资源不存在"),
    UNAUTHORIZED("1003", "未授权访问"),
    FORBIDDEN("1004", "无权限操作"),
    
    // 场景相关错误 (2xxx)
    SCENE_NOT_FOUND("2001", "场景不存在"),
    SCENE_ALREADY_EXISTS("2002", "场景编码已存在"),
    SCENE_DEPRECATED("2003", "场景已废弃，不可操作"),
    SCENE_NO_DRAFT("2004", "没有可编辑的草稿"),
    SCENE_CANNOT_PUBLISH("2005", "场景不满足发布条件"),
    
    // 版本相关错误 (3xxx)
    VERSION_NOT_FOUND("3001", "版本不存在"),
    VERSION_ALREADY_PUBLISHED("3002", "版本已发布，不可修改"),
    VERSION_LINT_FAILED("3003", "Lint 校验未通过，不能发布"),
    VERSION_MISSING_VERIFIED("3004", "缺少最后验证日期"),
    VERSION_MISSING_SUMMARY("3005", "缺少变更摘要"),
    
    // Lint 相关错误 (4xxx)
    LINT_ERROR("4001", "存在阻断级错误"),
    
    // 元数据相关错误 (5xxx)
    METADATA_API_ERROR("5001", "元数据平台接口异常"),
    METADATA_TABLE_NOT_FOUND("5002", "表在元数据平台未找到"),
    METADATA_TABLE_BLACKLISTED("5003", "表命中黑名单"),
    
    // 导出相关错误 (6xxx)
    EXPORT_NOT_FOUND("6001", "导出产物不存在"),
    EXPORT_GENERATE_FAILED("6002", "导出生成失败");
    
    private final String code;
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
