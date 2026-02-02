package com.caliberhub.infrastructure.scene.dao.po;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 场景版本-敏感字段 PO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scene_version_sensitive_field")
public class SceneVersionSensitiveFieldPO {

    @Id
    private String id;

    @Column(name = "version_id", nullable = false)
    private String versionId;

    @Column(name = "table_fullname")
    private String tableFullname;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "field_fullname", nullable = false)
    private String fieldFullname;

    @Column(name = "metadata_field_id")
    private String metadataFieldId;

    @Column(name = "sensitivity_level", nullable = false)
    private String sensitivityLevel;

    @Column(name = "mask_rule", nullable = false)
    private String maskRule;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "source", nullable = false)
    private String source;
}
