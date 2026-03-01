package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneReferencePO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SceneReferenceMapper extends JpaRepository<SceneReferencePO, Long> {

    List<SceneReferencePO> findBySceneIdOrderByIdAsc(Long sceneId);

    Optional<SceneReferencePO> findBySceneIdAndRefTypeAndRefId(Long sceneId, String refType, Long refId);

    List<SceneReferencePO> findByRefTypeAndRefId(String refType, Long refId);

    long countBySceneId(Long sceneId);
}

