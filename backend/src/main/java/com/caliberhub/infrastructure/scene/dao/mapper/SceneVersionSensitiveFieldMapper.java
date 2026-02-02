package com.caliberhub.infrastructure.scene.dao.mapper;

import com.caliberhub.infrastructure.scene.dao.po.SceneVersionSensitiveFieldPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 场景版本-敏感字段 Mapper
 */
@Repository
public interface SceneVersionSensitiveFieldMapper extends JpaRepository<SceneVersionSensitiveFieldPO, String> {

    /**
     * 根据版本ID查询所有敏感字段
     */
    List<SceneVersionSensitiveFieldPO> findByVersionId(String versionId);

    /**
     * 删除某版本的所有敏感字段记录
     */
    @Modifying
    @Query("DELETE FROM SceneVersionSensitiveFieldPO f WHERE f.versionId = :versionId")
    void deleteByVersionId(String versionId);

    /**
     * 根据字段全名查询
     */
    List<SceneVersionSensitiveFieldPO> findByFieldFullname(String fieldFullname);

    /**
     * 根据敏感级别查询
     */
    List<SceneVersionSensitiveFieldPO> findBySensitivityLevel(String sensitivityLevel);
}
