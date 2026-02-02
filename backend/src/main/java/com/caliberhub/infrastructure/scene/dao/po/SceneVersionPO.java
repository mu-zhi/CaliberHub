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
 * 场景版本持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scene_version")
public class SceneVersionPO {
    
    @Id
    private String id;
    
    @Column(name = "scene_id", nullable = false)
    private String sceneId;
    
    @Column(name = "domain_id", nullable = false)
    private String domainId;
    
    @Column(nullable = false)
    private String status;
    
    @Column(name = "is_current", nullable = false)
    private Integer isCurrent;
    
    @Column(name = "version_seq", nullable = false)
    private Integer versionSeq;
    
    @Column(name = "version_label", nullable = false)
    private String versionLabel;
    
    @Column(nullable = false)
    private String title;
    
    @Column(name = "tags_json", nullable = false)
    private String tagsJson;
    
    @Column(name = "owner_user", nullable = false)
    private String ownerUser;
    
    @Column(name = "contributors_json", nullable = false)
    private String contributorsJson;
    
    @Column(name = "has_sensitive", nullable = false)
    private Integer hasSensitive;
    
    @Column(name = "last_verified_at")
    private String lastVerifiedAt;
    
    @Column(name = "verified_by")
    private String verifiedBy;
    
    @Column(name = "verify_evidence")
    private String verifyEvidence;
    
    @Column(name = "change_summary")
    private String changeSummary;
    
    @Column(name = "published_by")
    private String publishedBy;
    
    @Column(name = "published_at")
    private String publishedAt;
    
    @Column(name = "content_json", nullable = false)
    private String contentJson;
    
    @Column(name = "lint_json", nullable = false)
    private String lintJson;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @Column(name = "created_at", nullable = false)
    private String createdAt;
    
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;
    
    @Column(name = "updated_at", nullable = false)
    private String updatedAt;
}
