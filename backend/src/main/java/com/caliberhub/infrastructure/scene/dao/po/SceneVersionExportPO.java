package com.caliberhub.infrastructure.scene.dao.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scene_version_export")
public class SceneVersionExportPO {

    @Id
    @Column(name = "version_id")
    private String versionId;

    @Column(name = "doc_json", nullable = false, columnDefinition = "TEXT")
    private String docJson;

    @Column(name = "chunks_json", nullable = false, columnDefinition = "TEXT")
    private String chunksJson;

    @Column(name = "chunk_count", nullable = false)
    private Integer chunkCount;

    @Column(name = "generated_at", nullable = false)
    private String generatedAt;

    @Column(name = "generated_by", nullable = false)
    private String generatedBy;

    @Column(name = "hash")
    private String hash;
}
