package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.ProjectionEventPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectionEventMapper extends JpaRepository<ProjectionEventPO, Long> {

    Optional<ProjectionEventPO> findBySceneId(Long sceneId);
}
