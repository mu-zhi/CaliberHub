package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneVersionPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SceneVersionMapper extends JpaRepository<SceneVersionPO, Long> {

    Optional<SceneVersionPO> findTopBySceneIdOrderByVersionNoDesc(Long sceneId);

    Optional<SceneVersionPO> findBySceneIdAndVersionNo(Long sceneId, Integer versionNo);

    List<SceneVersionPO> findBySceneIdOrderByVersionNoDesc(Long sceneId);
}

