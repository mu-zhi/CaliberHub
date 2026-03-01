package com.cmbchina.datadirect.caliber.infrastructure.module.supportimpl.domain;

import com.cmbchina.datadirect.caliber.domain.model.Scene;
import com.cmbchina.datadirect.caliber.domain.model.SceneQueryCondition;
import com.cmbchina.datadirect.caliber.domain.support.SceneDomainSupport;
import com.cmbchina.datadirect.caliber.infrastructure.module.converter.SceneConverter;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SceneDomainSupportImpl implements SceneDomainSupport {

    private final SceneMapper sceneMapper;
    private final SceneConverter sceneConverter;

    public SceneDomainSupportImpl(SceneMapper sceneMapper, SceneConverter sceneConverter) {
        this.sceneMapper = sceneMapper;
        this.sceneConverter = sceneConverter;
    }

    @Override
    public Scene save(Scene scene) {
        ScenePO saved = sceneMapper.save(sceneConverter.toPO(scene));
        return sceneConverter.toDomain(saved);
    }

    @Override
    public Optional<Scene> findById(Long id) {
        return sceneMapper.findById(id).map(sceneConverter::toDomain);
    }

    @Override
    public List<Scene> findByCondition(SceneQueryCondition condition) {
        return sceneMapper.findByCondition(condition.domainId(), condition.domain(), condition.status(), condition.keyword())
                .stream()
                .map(sceneConverter::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        sceneMapper.deleteById(id);
    }
}
