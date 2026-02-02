package com.caliberhub.infrastructure.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    /**
     * 业务状态码
     */
    private String code;
    
    /**
     * 提示信息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 错误详情（仅错误时返回）
     */
    private String detail;
    
    /**
     * 追踪ID（用于日志排查）
     */
    private String traceId;
    
    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code("SUCCESS")
                .message("操作成功")
                .data(data)
                .build();
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .code("SUCCESS")
                .message("操作成功")
                .build();
    }
    
    /**
     * 成功响应（自定义消息）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code("SUCCESS")
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
    
    /**
     * 失败响应（带详情）
     */
    public static <T> ApiResponse<T> error(String code, String message, String detail) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .detail(detail)
                .build();
    }
    
    /**
     * 失败响应（带追踪ID）
     */
    public static <T> ApiResponse<T> error(String code, String message, String detail, String traceId) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .detail(detail)
                .traceId(traceId)
                .build();
    }
}
