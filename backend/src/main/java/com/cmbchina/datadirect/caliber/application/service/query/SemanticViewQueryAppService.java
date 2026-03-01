package com.cmbchina.datadirect.caliber.application.service.query;

import com.cmbchina.datadirect.caliber.application.api.dto.response.SemanticViewDTO;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SemanticViewMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SemanticViewPO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SemanticViewQueryAppService {

    private final SemanticViewMapper semanticViewMapper;

    public SemanticViewQueryAppService(SemanticViewMapper semanticViewMapper) {
        this.semanticViewMapper = semanticViewMapper;
    }

    public List<SemanticViewDTO> list() {
        return semanticViewMapper.findAll().stream().map(this::toDTO).toList();
    }

    public SemanticViewDTO getById(Long id) {
        SemanticViewPO po = semanticViewMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("semantic view not found: " + id));
        return toDTO(po);
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

