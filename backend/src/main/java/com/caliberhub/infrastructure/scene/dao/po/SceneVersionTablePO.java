package com.caliberhub.infrastructure.scene.dao.po;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 场景版本-数据来源表 PO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scene_version_table")
public class SceneVersionTablePO {

    @Id
    private String id;

    @Column(name = "version_id", nullable = false)
    private String versionId;

    @Column(name = "table_fullname", nullable = false)
    private String tableFullname;

    @Column(name = "metadata_table_id")
    private String metadataTableId;

    @Column(name = "match_status", nullable = false)
    private String matchStatus;

    @Column(name = "is_key", nullable = false)
    private Integer isKey;

    @Column(name = "usage_type")
    private String usageType;

    @Column(name = "partition_field")
    private String partitionField;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "description")
    private String description;

    @Column(name = "sensitivity_summary")
    private String sensitivitySummary;

    @Column(name = "notes")
    private String notes;

    @Column(name = "extra_json", nullable = false)
    private String extraJson;
}
