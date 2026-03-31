package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EntityAliasPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntityAliasMapper extends JpaRepository<EntityAliasPO, Long> {

    List<EntityAliasPO> findByPlanIdInAndStatus(List<Long> planIds, String status);

    List<EntityAliasPO> findBySceneIdAndStatus(Long sceneId, String status);

    List<EntityAliasPO> findByNormalizedTextAndStatus(String normalizedText, String status);

    Optional<EntityAliasPO> findByAliasCode(String aliasCode);

    void deleteByPlanId(Long planId);

    void deleteBySceneId(Long sceneId);
}
