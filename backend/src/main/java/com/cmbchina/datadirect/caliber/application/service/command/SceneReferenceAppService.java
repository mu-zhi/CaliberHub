package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.AddSceneReferenceCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneReferenceDTO;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.CaliberDictMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneReferenceMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SemanticViewMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneReferencePO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class SceneReferenceAppService {

    private final SceneMapper sceneMapper;
    private final SceneReferenceMapper sceneReferenceMapper;
    private final SemanticViewMapper semanticViewMapper;
    private final CaliberDictMapper caliberDictMapper;

    public SceneReferenceAppService(SceneMapper sceneMapper,
                                    SceneReferenceMapper sceneReferenceMapper,
                                    SemanticViewMapper semanticViewMapper,
                                    CaliberDictMapper caliberDictMapper) {
        this.sceneMapper = sceneMapper;
        this.sceneReferenceMapper = sceneReferenceMapper;
        this.semanticViewMapper = semanticViewMapper;
        this.caliberDictMapper = caliberDictMapper;
    }

    @Transactional
    public SceneReferenceDTO add(Long sceneId, AddSceneReferenceCmd cmd) {
        sceneMapper.findById(sceneId)
                .orElseThrow(() -> new ResourceNotFoundException("scene not found: " + sceneId));
        String refType = normalizeRefType(cmd.refType());
        String strategy = normalizeStrategy(cmd.strategy());
        validateRefExists(refType, cmd.refId());

        SceneReferencePO existing = sceneReferenceMapper.findBySceneIdAndRefTypeAndRefId(sceneId, refType, cmd.refId())
                .orElse(null);
        if (existing != null) {
            if (!strategy.equals(existing.getStrategy())) {
                existing.setStrategy(strategy);
                return toDTO(sceneReferenceMapper.save(existing));
            }
            return toDTO(existing);
        }

        SceneReferencePO po = new SceneReferencePO();
        po.setSceneId(sceneId);
        po.setRefType(refType);
        po.setRefId(cmd.refId());
        po.setStrategy(strategy);
        po.setCreatedBy(cmd.operator());
        po.setCreatedAt(OffsetDateTime.now());
        return toDTO(sceneReferenceMapper.save(po));
    }

    public List<SceneReferenceDTO> list(Long sceneId) {
        return sceneReferenceMapper.findBySceneIdOrderByIdAsc(sceneId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public long countBySceneId(Long sceneId) {
        return sceneReferenceMapper.countBySceneId(sceneId);
    }

    private void validateRefExists(String refType, Long refId) {
        boolean exists;
        if ("SEMANTIC_VIEW".equals(refType)) {
            exists = semanticViewMapper.existsById(refId);
        } else {
            exists = caliberDictMapper.existsById(refId);
        }
        if (!exists) {
            throw new ResourceNotFoundException("reference not found: " + refType + "#" + refId);
        }
    }

    private String normalizeRefType(String refType) {
        if (refType == null || refType.isBlank()) {
            throw new DomainValidationException("refType must not be blank");
        }
        String value = refType.trim().toUpperCase(Locale.ROOT);
        if (!"SEMANTIC_VIEW".equals(value) && !"DICTIONARY".equals(value)) {
            throw new DomainValidationException("refType must be SEMANTIC_VIEW or DICTIONARY");
        }
        return value;
    }

    private String normalizeStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            throw new DomainValidationException("strategy must not be blank");
        }
        String value = strategy.trim().toUpperCase(Locale.ROOT);
        if (!"LOCKED".equals(value) && !"COMPATIBLE".equals(value) && !"LATEST".equals(value)) {
            throw new DomainValidationException("strategy must be LOCKED/COMPATIBLE/LATEST");
        }
        return value;
    }

    private SceneReferenceDTO toDTO(SceneReferencePO po) {
        return new SceneReferenceDTO(
                po.getId(),
                po.getSceneId(),
                po.getRefType(),
                po.getRefId(),
                po.getStrategy(),
                po.getCreatedBy(),
                po.getCreatedAt()
        );
    }
}

