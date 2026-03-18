package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateAlignmentReportCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.AlignmentReportDTO;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.AlignmentReportMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.AlignmentReportPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class AlignmentReportAppService {

    private final SceneMapper sceneMapper;
    private final AlignmentReportMapper alignmentReportMapper;
    private final ObjectMapper objectMapper;

    public AlignmentReportAppService(SceneMapper sceneMapper,
                                     AlignmentReportMapper alignmentReportMapper,
                                     ObjectMapper objectMapper) {
        this.sceneMapper = sceneMapper;
        this.alignmentReportMapper = alignmentReportMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AlignmentReportDTO create(Long sceneId, CreateAlignmentReportCmd cmd) {
        sceneMapper.findById(sceneId).orElseThrow(() -> new ResourceNotFoundException("scene not found: " + sceneId));
        String status = normalizeStatus(cmd.status());
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("tables", safeList(cmd.tables()));
        report.put("columns", safeList(cmd.columns()));
        report.put("policies", safeList(cmd.policies()));
        AlignmentReportPO po = new AlignmentReportPO();
        po.setSceneId(sceneId);
        po.setStatus(status);
        po.setMessage(cmd.message());
        po.setReportJson(writeJson(report));
        po.setCheckedBy(cmd.operator());
        po.setCheckedAt(OffsetDateTime.now());
        return toDTO(alignmentReportMapper.save(po));
    }

    public AlignmentReportDTO latest(Long sceneId) {
        AlignmentReportPO po = alignmentReportMapper.findTopBySceneIdOrderByCheckedAtDesc(sceneId)
                .orElseThrow(() -> new ResourceNotFoundException("alignment report not found: " + sceneId));
        return toDTO(po);
    }

    public Optional<AlignmentReportDTO> latestOptional(Long sceneId) {
        return alignmentReportMapper.findTopBySceneIdOrderByCheckedAtDesc(sceneId).map(this::toDTO);
    }

    public void assertPublishAllowed(Long sceneId) {
        AlignmentReportPO po = alignmentReportMapper.findTopBySceneIdOrderByCheckedAtDesc(sceneId).orElse(null);
        if (po == null) {
            return;
        }
        if (!"PASS".equalsIgnoreCase(po.getStatus())) {
            throw new DomainValidationException("发布失败，对齐未通过：" + (po.getMessage() == null ? "请先处理对齐问题" : po.getMessage()));
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new DomainValidationException("status must not be blank");
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!"PASS".equals(value) && !"WARN".equals(value) && !"FAIL".equals(value)) {
            throw new DomainValidationException("status must be PASS/WARN/FAIL");
        }
        return value;
    }

    private List<String> safeList(List<String> items) {
        return items == null ? List.of() : items.stream().filter(item -> item != null && !item.isBlank()).toList();
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("serialize alignment report failed", ex);
        }
    }

    private AlignmentReportDTO toDTO(AlignmentReportPO po) {
        return new AlignmentReportDTO(
                po.getId(),
                po.getSceneId(),
                po.getStatus(),
                po.getMessage(),
                po.getReportJson(),
                po.getCheckedBy(),
                po.getCheckedAt()
        );
    }
}

