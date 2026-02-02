package com.caliberhub.domain.scene.valueobject;

/**
 * 表匹配状态
 */
public enum TableMatchStatus {
    /**
     * 已匹配元数据平台
     */
    MATCHED,
    
    /**
     * 未找到
     */
    NOT_FOUND,
    
    /**
     * 黑名单表
     */
    BLACKLISTED,
    
    /**
     * 校验失败
     */
    VERIFY_FAILED
}
