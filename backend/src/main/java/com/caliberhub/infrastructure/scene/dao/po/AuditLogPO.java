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
 * 审计日志持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_log")
public class AuditLogPO {
    
    @Id
    private String id;
    
    @Column(name = "scene_id", nullable = false)
    private String sceneId;
    
    @Column(name = "version_id")
    private String versionId;
    
    @Column(nullable = false)
    private String action;
    
    @Column(nullable = false)
    private String actor;
    
    @Column(name = "occurred_at", nullable = false)
    private String occurredAt;
    
    private String summary;
    
    @Column(name = "diff_json", nullable = false)
    private String diffJson;
    
    @Column(name = "extra_json", nullable = false)
    private String extraJson;
}
