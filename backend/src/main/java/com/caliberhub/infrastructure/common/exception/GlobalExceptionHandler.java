package com.caliberhub.infrastructure.common.exception;

import com.caliberhub.infrastructure.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.warn("[{}] 业务异常: {}", traceId, e.getMessage());
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("BIZ_ERROR", e.getMessage(), null, traceId));
    }
    
    /**
     * 参数校验异常处理（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        
        log.warn("[{}] 参数校验失败: {}", traceId, detail);
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(
                        ErrorCode.PARAM_INVALID.getCode(),
                        ErrorCode.PARAM_INVALID.getMessage(),
                        detail,
                        traceId));
    }
    
    /**
     * 绑定异常处理
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(
            BindException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        String detail = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        
        log.warn("[{}] 参数绑定失败: {}", traceId, detail);
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(
                        ErrorCode.PARAM_INVALID.getCode(),
                        ErrorCode.PARAM_INVALID.getMessage(),
                        detail,
                        traceId));
    }
    
    /**
     * 资源不存在异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.warn("[{}] 资源不存在: {}", traceId, e.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        ErrorCode.NOT_FOUND.getCode(),
                        e.getMessage(),
                        null,
                        traceId));
    }
    
    /**
     * 未知异常处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception e, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.error("[{}] 系统异常: ", traceId, e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        ErrorCode.SYSTEM_ERROR.getCode(),
                        ErrorCode.SYSTEM_ERROR.getMessage(),
                        e.getMessage(),
                        traceId));
    }
    
    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
