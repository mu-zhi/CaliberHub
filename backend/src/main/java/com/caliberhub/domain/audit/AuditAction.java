package com.caliberhub.domain.audit;

/**
 * 审计操作类型
 */
public enum AuditAction {
    
    /**
     * 创建场景
     */
    CREATE_SCENE("创建场景"),
    
    /**
     * 保存草稿
     */
    SAVE_DRAFT("保存草稿"),
    
    /**
     * 运行 Lint
     */
    RUN_LINT("运行校验"),
    
    /**
     * 发布版本
     */
    PUBLISH("发布版本"),
    
    /**
     * 废弃场景
     */
    DEPRECATE("废弃场景"),
    
    /**
     * 重新激活
     */
    ACTIVATE("重新激活"),
    
    /**
     * 导出
     */
    EXPORT("导出");
    
    private final String description;
    
    AuditAction(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
