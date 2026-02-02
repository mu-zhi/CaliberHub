package com.caliberhub.domain.scene.valueobject;

/**
 * 敏感等级
 */
public enum SensitivityLevel {
    /**
     * 个人身份信息
     */
    PII,
    
    /**
     * 机密
     */
    CONFIDENTIAL,
    
    /**
     * 内部
     */
    INTERNAL,
    
    /**
     * 公开
     */
    PUBLIC
}
