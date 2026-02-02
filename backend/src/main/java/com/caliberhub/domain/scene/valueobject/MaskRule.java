package com.caliberhub.domain.scene.valueobject;

/**
 * 脱敏规则
 */
public enum MaskRule {
    /**
     * 全掩码
     */
    FULL_MASK,
    
    /**
     * 保留后四位
     */
    KEEP_LAST4,
    
    /**
     * 哈希
     */
    HASH,
    
    /**
     * 不可展示
     */
    NO_DISPLAY,
    
    /**
     * 仅审批后可见
     */
    APPROVAL_ONLY
}
