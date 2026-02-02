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
 * 场景持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scene")
public class ScenePO {
    
    @Id
    private String id;
    
    @Column(name = "scene_code", nullable = false, unique = true)
    private String sceneCode;
    
    @Column(nullable = false)
    private String title;
    
    @Column(name = "domain_id", nullable = false)
    private String domainId;
    
    @Column(name = "lifecycle_status", nullable = false)
    private String lifecycleStatus;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @Column(name = "created_at", nullable = false)
    private String createdAt;
    
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;
    
    @Column(name = "updated_at", nullable = false)
    private String updatedAt;
}
