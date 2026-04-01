package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.ExportServiceSpecCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ServiceSpecDTO;
import com.cmbchina.datadirect.caliber.application.exception.BusinessConflictException;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ServiceSpecMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ServiceSpecPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServiceSpecAppService {

    private final SceneMapper sceneMapper;
    private final ServiceSpecMapper serviceSpecMapper;
    private final ObjectMapper objectMapper;

    public ServiceSpecAppService(SceneMapper sceneMapper,
                                 ServiceSpecMapper serviceSpecMapper,
                                 ObjectMapper objectMapper) {
        this.sceneMapper = sceneMapper;
        this.serviceSpecMapper = serviceSpecMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ServiceSpecDTO export(Long sceneId, ExportServiceSpecCmd cmd) {
        ScenePO scene = sceneMapper.findById(sceneId)
                .orElseThrow(() -> new ResourceNotFoundException("scene not found: " + sceneId));
        if (scene.getStatus() != SceneStatus.PUBLISHED) {
            throw new DomainValidationException("service spec export requires published scene");
        }
        ServiceSpecPO latest = serviceSpecMapper.findTopBySceneIdOrderBySpecVersionDesc(sceneId).orElse(null);
        int currentVersion = latest == null ? 0 : latest.getSpecVersion();
        Integer expected = cmd == null ? null : cmd.expectedVersion();
        if (expected != null && expected != currentVersion) {
            throw new BusinessConflictException("CAL-SS-409",
                    "spec version conflict, expected=" + expected + ", actual=" + currentVersion);
        }

        String specCode = latest == null ? ("SPEC-" + scene.getSceneCode()) : latest.getSpecCode();
        int nextVersion = currentVersion + 1;
        Map<String, Object> spec = buildSpec(scene, specCode, nextVersion);

        ServiceSpecPO po = new ServiceSpecPO();
        po.setSceneId(sceneId);
        po.setSpecCode(specCode);
        po.setSpecVersion(nextVersion);
        po.setSpecJson(writeJson(spec));
        po.setExportedBy(cmd == null ? "system" : cmd.operator());
        po.setExportedAt(OffsetDateTime.now());
        return toDTO(serviceSpecMapper.save(po));
    }

    public ServiceSpecDTO getByCode(String specCode, Integer version) {
        ServiceSpecPO po;
        if (version == null) {
            po = serviceSpecMapper.findTopBySpecCodeOrderBySpecVersionDesc(specCode)
                    .orElseThrow(() -> new ResourceNotFoundException("service spec not found: " + specCode));
        } else {
            po = serviceSpecMapper.findBySpecCodeAndSpecVersion(specCode, version)
                    .orElseThrow(() -> new ResourceNotFoundException("service spec not found: " + specCode + "#" + version));
        }
        return toDTO(po);
    }

    public List<ServiceSpecDTO> listRecent(Long sceneId, Integer limit) {
        int safeLimit = limit == null ? 20 : Math.min(Math.max(limit, 1), 100);
        List<ServiceSpecPO> rows;
        if (sceneId != null) {
            rows = serviceSpecMapper.findBySceneIdOrderBySpecVersionDesc(sceneId);
            if (rows.size() > safeLimit) {
                rows = rows.subList(0, safeLimit);
            }
        } else {
            rows = serviceSpecMapper.findAll(
                    PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "exportedAt"))
            ).getContent();
        }
        return rows.stream().map(this::toDTO).toList();
    }

    public long countBySceneId(Long sceneId) {
        return serviceSpecMapper.countBySceneId(sceneId);
    }

    private Map<String, Object> buildSpec(ScenePO scene, String specCode, int specVersion) {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("specCode", specCode);
        spec.put("specVersion", specVersion);
        spec.put("sceneId", scene.getId());
        spec.put("sceneCode", scene.getSceneCode());
        spec.put("sceneTitle", scene.getSceneTitle());
        spec.put("domainId", scene.getDomainId());
        spec.put("domain", scene.getDomain());
        spec.put("inputsJson", scene.getInputsJson());
        spec.put("outputsJson", scene.getOutputsJson());
        spec.put("sqlVariantsJson", scene.getSqlVariantsJson());
        spec.put("riskHint", "M3_EXPORT");
        return spec;
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("serialize service spec failed", ex);
        }
    }

    private ServiceSpecDTO toDTO(ServiceSpecPO po) {
        return new ServiceSpecDTO(
                po.getId(),
                po.getSceneId(),
                po.getSpecCode(),
                po.getSpecVersion(),
                po.getSpecJson(),
                po.getExportedBy(),
                po.getExportedAt()
        );
    }
}
