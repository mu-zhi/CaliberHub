package com.cmbchina.datadirect.caliber.domain.support;

import com.cmbchina.datadirect.caliber.domain.model.Scene;
import com.cmbchina.datadirect.caliber.domain.model.SceneQueryCondition;

import java.util.List;
import java.util.Optional;

public interface SceneDomainSupport {

    Scene save(Scene scene);

    Optional<Scene> findById(Long id);

    List<Scene> findByCondition(SceneQueryCondition condition);

    void deleteById(Long id);
}
