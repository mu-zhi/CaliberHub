package com.caliberhub.infrastructure.scene.dao.mapper;

import com.caliberhub.infrastructure.scene.dao.po.SceneVersionPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 场景版本 Mapper
 */
@Repository
public interface SceneVersionMapper extends JpaRepository<SceneVersionPO, String> {
    
    /**
     * 查询当前草稿
     */
    @Query("SELECT v FROM SceneVersionPO v WHERE v.sceneId = :sceneId AND v.status = 'DRAFT' AND v.isCurrent = 1")
    Optional<SceneVersionPO> findCurrentDraft(@Param("sceneId") String sceneId);
    
    /**
     * 查询当前发布版本
     */
    @Query("SELECT v FROM SceneVersionPO v WHERE v.sceneId = :sceneId AND v.status = 'PUBLISHED' AND v.isCurrent = 1")
    Optional<SceneVersionPO> findCurrentPublished(@Param("sceneId") String sceneId);
    
    /**
     * 查询场景的所有版本
     */
    List<SceneVersionPO> findBySceneIdOrderByVersionSeqDesc(String sceneId);
    
    /**
     * 查询场景的所有发布版本
     */
    @Query("SELECT v FROM SceneVersionPO v WHERE v.sceneId = :sceneId AND v.status = 'PUBLISHED' ORDER BY v.versionSeq DESC")
    List<SceneVersionPO> findPublishedVersions(@Param("sceneId") String sceneId);
    
    /**
     * 将场景的所有发布版本标记为非当前
     */
    @Modifying
    @Query("UPDATE SceneVersionPO v SET v.isCurrent = 0 WHERE v.sceneId = :sceneId AND v.status = 'PUBLISHED'")
    void markAllPublishedNotCurrent(@Param("sceneId") String sceneId);
    
    /**
     * 删除场景的当前草稿
     */
    @Modifying
    @Query("DELETE FROM SceneVersionPO v WHERE v.sceneId = :sceneId AND v.status = 'DRAFT' AND v.isCurrent = 1")
    void deleteCurrentDraft(@Param("sceneId") String sceneId);
    
    /**
     * 获取最大版本序号
     */
    @Query("SELECT COALESCE(MAX(v.versionSeq), 0) FROM SceneVersionPO v WHERE v.sceneId = :sceneId AND v.status = 'PUBLISHED'")
    int getMaxVersionSeq(@Param("sceneId") String sceneId);
}
