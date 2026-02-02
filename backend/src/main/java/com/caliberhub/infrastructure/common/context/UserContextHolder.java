package com.caliberhub.infrastructure.common.context;

/**
 * 用户上下文持有器
 * 用于在请求处理过程中传递当前用户信息
 */
public class UserContextHolder {
    
    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();
    
    private static final String DEFAULT_USER = "demo_user";
    
    /**
     * 设置当前用户
     */
    public static void setCurrentUser(String user) {
        CURRENT_USER.set(user);
    }
    
    /**
     * 获取当前用户
     */
    public static String getCurrentUser() {
        String user = CURRENT_USER.get();
        return user != null ? user : DEFAULT_USER;
    }
    
    /**
     * 清除当前用户
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
