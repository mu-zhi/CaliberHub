package com.cmbchina.datadirect.caliber.application.service.query;

import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneImpactDTO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneReferenceMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ServiceSpecMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImpactQueryAppService {

    private final SceneReferenceMapper sceneReferenceMapper;
    private final ServiceSpecMapper serviceSpecMapper;

    public ImpactQueryAppService(SceneReferenceMapper sceneReferenceMapper,
                                 ServiceSpecMapper serviceSpecMapper) {
        this.sceneReferenceMapper = sceneReferenceMapper;
        this.serviceSpecMapper = serviceSpecMapper;
    }

    public SceneImpactDTO sceneImpact(Long sceneId) {
        long referenceCount = sceneReferenceMapper.countBySceneId(sceneId);
        long specCount = serviceSpecMapper.countBySceneId(sceneId);
        List<String> specCodes = serviceSpecMapper.findBySceneIdOrderBySpecVersionDesc(sceneId)
                .stream()
                .map(item -> item.getSpecCode() + "#" + item.getSpecVersion())
                .distinct()
                .toList();
        return new SceneImpactDTO(sceneId, referenceCount, specCount, specCodes);
    }
}
