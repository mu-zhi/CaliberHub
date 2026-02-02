package com.caliberhub.infrastructure.scene.dao.mapper;

import com.caliberhub.infrastructure.scene.dao.po.ScenePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 场景 Mapper
 */
@Repository
public interface SceneMapper extends JpaRepository<ScenePO, String>, JpaSpecificationExecutor<ScenePO> {
    
    Optional<ScenePO> findBySceneCode(String sceneCode);
    
    List<ScenePO> findByDomainId(String domainId);
    
    List<ScenePO> findByLifecycleStatus(String lifecycleStatus);
    
    boolean existsBySceneCode(String sceneCode);
}
