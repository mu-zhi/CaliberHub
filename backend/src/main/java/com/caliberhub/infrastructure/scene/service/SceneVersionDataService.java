package com.caliberhub.infrastructure.scene.service;

import com.caliberhub.domain.scene.valueobject.SensitiveField;
import com.caliberhub.domain.scene.valueobject.SourceTable;
import com.caliberhub.infrastructure.scene.dao.mapper.SceneVersionSensitiveFieldMapper;
import com.caliberhub.infrastructure.scene.dao.mapper.SceneVersionTableMapper;
import com.caliberhub.infrastructure.scene.dao.po.SceneVersionSensitiveFieldPO;
import com.caliberhub.infrastructure.scene.dao.po.SceneVersionTablePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 场景版本数据写入服务
 * 负责将数据来源表和敏感字段写入数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneVersionDataService {

    private final SceneVersionTableMapper tableMapper;
    private final SceneVersionSensitiveFieldMapper fieldMapper;

    /**
     * 保存数据来源表
     */
    @Transactional
    public void saveSourceTables(String versionId, List<SourceTable> sourceTables) {
        if (sourceTables == null || sourceTables.isEmpty()) {
            log.info("No source tables to save for version: {}", versionId);
            return;
        }

        // 先删除旧数据
        tableMapper.deleteByVersionId(versionId);

        // 保存新数据
        List<SceneVersionTablePO> poList = sourceTables.stream()
                .map(table -> SceneVersionTablePO.builder()
                        .id(UUID.randomUUID().toString())
                        .versionId(versionId)
                        .tableFullname(table.getTableFullname())
                        .metadataTableId(table.getMetadataTableId())
                        .matchStatus(table.getMatchStatus().name())
                        .isKey(table.isKeyTable() ? 1 : 0)
                        .usageType(table.getUsageType())
                        .partitionField(table.getPartitionField())
                        .source(table.getSource())
                        .description(table.getDescription())
                        .extraJson("{}")
                        .build())
                .toList();

        tableMapper.saveAll(poList);
        log.info("Saved {} source tables for version: {}", poList.size(), versionId);
    }

    /**
     * 保存敏感字段
     */
    @Transactional
    public void saveSensitiveFields(String versionId, List<SensitiveField> sensitiveFields) {
        if (sensitiveFields == null || sensitiveFields.isEmpty()) {
            log.info("No sensitive fields to save for version: {}", versionId);
            return;
        }

        // 先删除旧数据
        fieldMapper.deleteByVersionId(versionId);

        // 保存新数据
        List<SceneVersionSensitiveFieldPO> poList = sensitiveFields.stream()
                .map(field -> SceneVersionSensitiveFieldPO.builder()
                        .id(UUID.randomUUID().toString())
                        .versionId(versionId)
                        .tableFullname(field.getTableName())
                        .fieldName(field.getFieldName())
                        .fieldFullname(field.getFieldFullname())
                        .metadataFieldId(field.getMetadataFieldId())
                        .sensitivityLevel(field.getSensitivityLevel())
                        .maskRule(field.getMaskRule())
                        .remarks(field.getRemarks())
                        .source(field.getSource() != null ? field.getSource() : "MANUAL")
                        .build())
                .toList();

        fieldMapper.saveAll(poList);
        log.info("Saved {} sensitive fields for version: {}", poList.size(), versionId);
    }

    /**
     * 查询数据来源表
     */
    public List<SceneVersionTablePO> getSourceTables(String versionId) {
        return tableMapper.findByVersionId(versionId);
    }

    /**
     * 查询敏感字段
     */
    public List<SceneVersionSensitiveFieldPO> getSensitiveFields(String versionId) {
        return fieldMapper.findByVersionId(versionId);
    }
}
