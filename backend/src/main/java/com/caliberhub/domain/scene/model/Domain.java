package com.caliberhub.domain.scene.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 业务领域 - 实体
 */
@Getter
@Builder
public class Domain {
    
    private final String id;
    private final String domainKey;
    private String name;
    private String description;
    
    private final String createdBy;
    private final LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    
    /**
     * 创建领域
     */
    public static Domain create(String domainKey, String name, String description, String operator) {
        LocalDateTime now = LocalDateTime.now();
        return Domain.builder()
                .id(UUID.randomUUID().toString())
                .domainKey(domainKey)
                .name(name)
                .description(description)
                .createdBy(operator)
                .createdAt(now)
                .updatedBy(operator)
                .updatedAt(now)
                .build();
    }
    
    /**
     * 更新领域信息
     */
    public void update(String name, String description, String operator) {
        this.name = name;
        this.description = description;
        this.updatedBy = operator;
        this.updatedAt = LocalDateTime.now();
    }
}
