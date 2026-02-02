package com.caliberhub.infrastructure.scene.dao.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务领域持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "domain")
public class DomainPO {
    
    @Id
    private String id;
    
    @Column(name = "domain_key", nullable = false, unique = true)
    private String domainKey;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @Column(name = "created_at", nullable = false)
    private String createdAt;
    
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;
    
    @Column(name = "updated_at", nullable = false)
    private String updatedAt;
}
