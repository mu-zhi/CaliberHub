package com.cmbchina.datadirect.caliber.application.service.query;

import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportTaskLifecycleDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportTaskDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportTaskMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportTaskPO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ImportTaskQueryAppService {

    private final ImportTaskMapper importTaskMapper;
    private final ObjectMapper objectMapper;
    private final SceneQueryAppService sceneQueryAppService;

    public ImportTaskQueryAppService(ImportTaskMapper importTaskMapper,
                                     ObjectMapper objectMapper,
                                     SceneQueryAppService sceneQueryAppService) {
        this.importTaskMapper = importTaskMapper;
        this.objectMapper = objectMapper;
        this.sceneQueryAppService = sceneQueryAppService;
    }

    public ImportTaskDTO getByTaskId(String taskId) {
        String normalizedTaskId = taskId == null ? "" : taskId.trim();
        if (normalizedTaskId.isBlank()) {
            throw new ResourceNotFoundException("import task not found: " + taskId);
        }
        ImportTaskPO po = importTaskMapper.findById(normalizedTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("import task not found: " + normalizedTaskId));
        return toDTO(po);
    }

    public List<ImportTaskLifecycleDTO> listRecent(String status, String operator, Integer limit) {
        String normalizedStatus = normalizeStatus(status);
        String normalizedOperator = normalizeOperator(operator);
        int pageSize = normalizeLimit(limit);
        List<ImportTaskPO> tasks = importTaskMapper.findRecent(
                normalizedStatus,
                normalizedOperator,
                org.springframework.data.domain.PageRequest.of(0, pageSize)
        );
        return tasks.stream().map(this::toLifecycle).toList();
    }

    public List<SceneDTO> listTaskScenes(String taskId) {
        ImportTaskPO po = requireTask(taskId);
        List<Long> sceneIds = extractSceneIds(po.getPreprocessResultJson());
        List<SceneDTO> scenes = new ArrayList<>();
        for (Long sceneId : sceneIds) {
            try {
                scenes.add(sceneQueryAppService.getById(sceneId));
            } catch (ResourceNotFoundException ignore) {
                // ignore missing scene
            }
        }
        return scenes;
    }

    private ImportTaskPO requireTask(String taskId) {
        String normalizedTaskId = taskId == null ? "" : taskId.trim();
        if (normalizedTaskId.isBlank()) {
            throw new ResourceNotFoundException("import task not found: " + taskId);
        }
        return importTaskMapper.findById(normalizedTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("import task not found: " + normalizedTaskId));
    }

    private ImportTaskDTO toDTO(ImportTaskPO po) {
        JsonNode preprocessResult = null;
        if (po.getPreprocessResultJson() != null && !po.getPreprocessResultJson().isBlank()) {
            try {
                preprocessResult = objectMapper.readTree(po.getPreprocessResultJson());
            } catch (Exception ignore) {
                // ignore parse failure
            }
        }
        String status = po.getStatus() == null ? null : po.getStatus().toUpperCase(Locale.ROOT);
        return new ImportTaskDTO(
                po.getTaskId(),
                po.getMaterialId(),
                status,
                po.getCurrentStep(),
                po.getSourceType(),
                po.getSourceName(),
                po.getOperator(),
                po.getRawText(),
                Boolean.TRUE.equals(po.getQualityConfirmed()),
                Boolean.TRUE.equals(po.getCompareConfirmed()),
                preprocessResult,
                po.getErrorMessage(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getCompletedAt()
        );
    }

    private ImportTaskLifecycleDTO toLifecycle(ImportTaskPO po) {
        List<Long> sceneIds = extractSceneIds(po.getPreprocessResultJson());
        int draftCount = 0;
        int publishedCount = 0;
        int discardedCount = 0;
        for (Long sceneId : sceneIds) {
            try {
                SceneDTO scene = sceneQueryAppService.getById(sceneId);
                String status = normalizeStatus(scene.status());
                if ("PUBLISHED".equals(status)) {
                    publishedCount += 1;
                } else if ("DISCARDED".equals(status)) {
                    discardedCount += 1;
                } else {
                    draftCount += 1;
                }
            } catch (ResourceNotFoundException ignore) {
                draftCount += 1;
            }
        }
        int draftTotal = sceneIds.size();
        String status = normalizeStatus(po.getStatus());
        boolean resumable = !"COMPLETED".equals(status) && !"FAILED".equals(status);
        return new ImportTaskLifecycleDTO(
                po.getTaskId(),
                status,
                po.getCurrentStep(),
                po.getSourceType(),
                po.getSourceName(),
                po.getOperator(),
                po.getErrorMessage(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getCompletedAt(),
                sceneIds,
                draftTotal,
                draftCount,
                publishedCount,
                discardedCount,
                resumable
        );
    }

    private List<Long> extractSceneIds(String preprocessResultJson) {
        if (preprocessResultJson == null || preprocessResultJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(preprocessResultJson);
            JsonNode sceneDraftsNode = root.path("sceneDrafts");
            if (!sceneDraftsNode.isArray()) {
                return List.of();
            }
            Set<Long> ids = new LinkedHashSet<>();
            sceneDraftsNode.forEach(item -> {
                long sceneId = item.path("sceneId").asLong(0L);
                if (sceneId > 0) {
                    ids.add(sceneId);
                }
            });
            return new ArrayList<>(ids);
        } catch (Exception ignore) {
            return List.of();
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        int value = limit;
        if (value < 1) {
            return 20;
        }
        return Math.min(value, 100);
    }

    private String normalizeOperator(String operator) {
        if (operator == null) {
            return null;
        }
        String normalized = operator.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }
}
