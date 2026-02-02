package com.caliberhub.infrastructure.common.exception;

/**
 * 资源不存在异常
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " 不存在: " + identifier);
    }
}
