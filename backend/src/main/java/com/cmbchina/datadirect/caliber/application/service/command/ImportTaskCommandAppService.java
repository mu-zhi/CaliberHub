package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.PreprocessImportCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportTaskDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.PreprocessResultDTO;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportTaskMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportTaskPO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class ImportTaskCommandAppService {

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_QUALITY_REVIEWING = "QUALITY_REVIEWING";
    private static final String STATUS_SCENE_REVIEWING = "SCENE_REVIEWING";
    private static final String STATUS_PUBLISHING = "PUBLISHING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    private final ImportTaskMapper importTaskMapper;
    private final ObjectMapper objectMapper;

    public ImportTaskCommandAppService(ImportTaskMapper importTaskMapper, ObjectMapper objectMapper) {
        this.importTaskMapper = importTaskMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportTaskDTO start(String taskId, PreprocessImportCmd cmd) {
        String normalizedTaskId = normalizeTaskId(taskId);
        OffsetDateTime now = OffsetDateTime.now();
        ImportTaskPO po = importTaskMapper.findById(normalizedTaskId).orElseGet(ImportTaskPO::new);
        if (po.getTaskId() == null || po.getTaskId().isBlank()) {
            po.setTaskId(normalizedTaskId);
            po.setCreatedAt(now);
        }
        po.setStatus(STATUS_RUNNING);
        po.setCurrentStep(1);
        po.setSourceType(trimToNull(cmd.sourceType()));
        po.setSourceName(trimToNull(cmd.sourceName()));
        po.setOperator(trimToNull(cmd.operator()));
        po.setRawText(cmd.rawText());
        po.setPreprocessResultJson(null);
        po.setQualityConfirmed(false);
        po.setCompareConfirmed(false);
        po.setErrorMessage(null);
        po.setCompletedAt(null);
        po.setUpdatedAt(now);
        return toDTO(importTaskMapper.save(po));
    }

    @Transactional
    public ImportTaskDTO markQualityReviewReady(String taskId, PreprocessResultDTO result) {
        ImportTaskPO po = requireTask(taskId);
        po.setStatus(STATUS_QUALITY_REVIEWING);
        po.setCurrentStep(Math.max(2, defaultStep(po.getCurrentStep())));
        po.setPreprocessResultJson(writeResultJson(result));
        po.setQualityConfirmed(false);
        po.setCompareConfirmed(false);
        po.setErrorMessage(null);
        po.setUpdatedAt(OffsetDateTime.now());
        return toDTO(importTaskMapper.save(po));
    }

    @Transactional
    public ImportTaskDTO confirmQuality(String taskId, String operator) {
        ImportTaskPO po = requireTask(taskId);
        po.setQualityConfirmed(true);
        po.setStatus(STATUS_SCENE_REVIEWING);
        po.setCurrentStep(Math.max(3, defaultStep(po.getCurrentStep())));
        po.setOperator(mergeOperator(po.getOperator(), operator));
        po.setUpdatedAt(OffsetDateTime.now());
        return toDTO(importTaskMapper.save(po));
    }

    @Transactional
    public ImportTaskDTO confirmCompare(String taskId, String operator) {
        ImportTaskPO po = requireTask(taskId);
        po.setCompareConfirmed(true);
        po.setStatus(STATUS_PUBLISHING);
        po.setCurrentStep(Math.max(4, defaultStep(po.getCurrentStep())));
        po.setOperator(mergeOperator(po.getOperator(), operator));
        po.setUpdatedAt(OffsetDateTime.now());
        return toDTO(importTaskMapper.save(po));
    }

    @Transactional
    public ImportTaskDTO rewindToStep(String taskId, int step, String operator) {
        if (step < 1 || step > 4) {
            throw new DomainValidationException("step must be between 1 and 4");
        }
        ImportTaskPO po = requireTask(taskId);
        po.setOperator(mergeOperator(po.getOperator(), operator));
        po.setUpdatedAt(OffsetDateTime.now());
        if (step <= 1) {
            po.setStatus(STATUS_RUNNING);
            po.setCurrentStep(1);
            po.setPreprocessResultJson(null);
            po.setQualityConfirmed(false);
            po.setCompareConfirmed(false);
            po.setErrorMessage(null);
            po.setCompletedAt(null);
        } else if (step == 2) {
            po.setStatus(STATUS_QUALITY_REVIEWING);
            po.setCurrentStep(2);
            po.setQualityConfirmed(false);
            po.setCompareConfirmed(false);
            po.setCompletedAt(null);
        } else if (step == 3) {
            po.setStatus(STATUS_SCENE_REVIEWING);
            po.setCurrentStep(3);
            po.setQualityConfirmed(true);
            po.setCompareConfirmed(false);
            po.setCompletedAt(null);
        } else {
            po.setStatus(STATUS_PUBLISHING);
            po.setCurrentStep(4);
            po.setQualityConfirmed(true);
            po.setCompareConfirmed(true);
            po.setCompletedAt(null);
        }
        return toDTO(importTaskMapper.save(po));
    }

    @Transactional
    public ImportTaskDTO complete(String taskId, String operator) {
        ImportTaskPO po = requireTask(taskId);
        po.setStatus(STATUS_COMPLETED);
        po.setCurrentStep(4);
        po.setQualityConfirmed(true);
        po.setCompareConfirmed(true);
        po.setOperator(mergeOperator(po.getOperator(), operator));
        OffsetDateTime now = OffsetDateTime.now();
        po.setUpdatedAt(now);
        po.setCompletedAt(now);
        return toDTO(importTaskMapper.save(po));
    }

    @Transactional
    public void markFailed(String taskId, String errorMessage) {
        if (taskId == null || taskId.isBlank()) {
            return;
        }
        Optional<ImportTaskPO> optional = importTaskMapper.findById(taskId.trim());
        if (optional.isEmpty()) {
            return;
        }
        ImportTaskPO po = optional.get();
        po.setStatus(STATUS_FAILED);
        po.setErrorMessage(trimToNull(errorMessage));
        po.setUpdatedAt(OffsetDateTime.now());
        po.setCurrentStep(Math.max(1, defaultStep(po.getCurrentStep())));
        importTaskMapper.save(po);
    }

    private ImportTaskPO requireTask(String taskId) {
        String normalizedTaskId = normalizeTaskId(taskId);
        return importTaskMapper.findById(normalizedTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("import task not found: " + normalizedTaskId));
    }

    private int defaultStep(Integer step) {
        return step == null || step <= 0 ? 1 : step;
    }

    private String writeResultJson(PreprocessResultDTO result) {
        if (result == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeTaskId(String taskId) {
        String normalized = taskId == null ? "" : taskId.trim();
        if (normalized.isBlank()) {
            throw new DomainValidationException("taskId must not be blank");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String mergeOperator(String currentOperator, String nextOperator) {
        String normalizedNext = trimToNull(nextOperator);
        if (normalizedNext != null) {
            return normalizedNext;
        }
        return trimToNull(currentOperator);
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
}
