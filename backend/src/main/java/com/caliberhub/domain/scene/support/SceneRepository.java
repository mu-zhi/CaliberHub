package com.caliberhub.domain.scene.support;

import com.caliberhub.domain.scene.model.Scene;

import java.util.List;
import java.util.Optional;

/**
 * 场景仓储接口 - 领域层防腐层
 * 由基础设施层实现
 */
public interface SceneRepository {
    
    /**
     * 根据ID查询场景
     */
    Optional<Scene> findById(String id);
    
    /**
     * 根据场景编码查询
     */
    Optional<Scene> findBySceneCode(String sceneCode);
    
    /**
     * 根据领域查询场景列表
     */
    List<Scene> findByDomainId(String domainId);
    
    /**
     * 查询所有活跃场景
     */
    List<Scene> findAllActive();
    
    /**
     * 保存场景（含版本）
     */
    void save(Scene scene);
    
    /**
     * 删除场景
     */
    void delete(String id);
    
    /**
     * 检查场景编码是否存在
     */
    boolean existsBySceneCode(String sceneCode);
}
