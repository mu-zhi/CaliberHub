package com.caliberhub.infrastructure.scene.dao.mapper;

import com.caliberhub.infrastructure.scene.dao.po.SceneVersionTablePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 场景版本-数据来源表 Mapper
 */
@Repository
public interface SceneVersionTableMapper extends JpaRepository<SceneVersionTablePO, String> {

    /**
     * 根据版本ID查询所有数据来源表
     */
    List<SceneVersionTablePO> findByVersionId(String versionId);

    /**
     * 删除某版本的所有数据来源表记录
     */
    @Modifying
    @Query("DELETE FROM SceneVersionTablePO t WHERE t.versionId = :versionId")
    void deleteByVersionId(String versionId);

    /**
     * 根据表全名查询
     */
    List<SceneVersionTablePO> findByTableFullname(String tableFullname);

    /**
     * 根据匹配状态查询
     */
    List<SceneVersionTablePO> findByMatchStatus(String matchStatus);
}
