package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.GapTaskPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GapTaskMapper extends JpaRepository<GapTaskPO, Long> {
    List<GapTaskPO> findBySceneIdOrderByUpdatedAtDesc(Long sceneId);
}
