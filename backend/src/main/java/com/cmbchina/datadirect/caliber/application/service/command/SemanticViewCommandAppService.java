package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateSemanticViewCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateSemanticViewCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SemanticViewDTO;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneReferenceMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SemanticViewMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SemanticViewPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class SemanticViewCommandAppService {

    private final SemanticViewMapper semanticViewMapper;
    private final SceneReferenceMapper sceneReferenceMapper;

    public SemanticViewCommandAppService(SemanticViewMapper semanticViewMapper,
                                         SceneReferenceMapper sceneReferenceMapper) {
        this.semanticViewMapper = semanticViewMapper;
        this.sceneReferenceMapper = sceneReferenceMapper;
    }

    @Transactional
    public SemanticViewDTO create(CreateSemanticViewCmd cmd) {
        String normalizedCode = normalizeCode(cmd.viewCode());
        if (semanticViewMapper.existsByViewCode(normalizedCode)) {
            throw new DomainValidationException("viewCode already exists: " + normalizedCode);
        }
        OffsetDateTime now = OffsetDateTime.now();
        SemanticViewPO po = new SemanticViewPO();
        po.setViewCode(normalizedCode);
        po.setViewName(cmd.viewName().trim());
        po.setDomainId(cmd.domainId());
        po.setDescription(cmd.description());
        po.setFieldDefinitionsJson(cmd.fieldDefinitionsJson());
        po.setCreatedBy(cmd.operator());
        po.setUpdatedBy(cmd.operator());
        po.setCreatedAt(now);
        po.setUpdatedAt(now);
        return toDTO(semanticViewMapper.save(po));
    }

    @Transactional
    public SemanticViewDTO update(Long id, UpdateSemanticViewCmd cmd) {
        SemanticViewPO po = semanticViewMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("semantic view not found: " + id));
        po.setViewName(cmd.viewName().trim());
        po.setDomainId(cmd.domainId());
        po.setDescription(cmd.description());
        po.setFieldDefinitionsJson(cmd.fieldDefinitionsJson());
        po.setUpdatedBy(cmd.operator());
        po.setUpdatedAt(OffsetDateTime.now());
        return toDTO(semanticViewMapper.save(po));
    }

    @Transactional
    public void delete(Long id) {
        if (!semanticViewMapper.existsById(id)) {
            throw new ResourceNotFoundException("semantic view not found: " + id);
        }
        if (!sceneReferenceMapper.findByRefTypeAndRefId("SEMANTIC_VIEW", id).isEmpty()) {
            throw new DomainValidationException("semantic view is referenced by scene(s), cannot delete");
        }
        semanticViewMapper.deleteById(id);
    }

    private String normalizeCode(String viewCode) {
        if (viewCode == null || viewCode.isBlank()) {
            throw new DomainValidationException("viewCode must not be blank");
        }
        return viewCode.trim().toUpperCase(Locale.ROOT);
    }

    private SemanticViewDTO toDTO(SemanticViewPO po) {
        return new SemanticViewDTO(
                po.getId(),
                po.getViewCode(),
                po.getViewName(),
                po.getDomainId(),
                po.getDescription(),
                po.getFieldDefinitionsJson(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }
}
