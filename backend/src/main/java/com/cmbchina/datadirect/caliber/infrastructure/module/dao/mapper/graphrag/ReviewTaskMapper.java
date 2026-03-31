package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.ReviewTaskPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewTaskMapper extends JpaRepository<ReviewTaskPO, Long> {
    List<ReviewTaskPO> findBySceneIdOrderByUpdatedAtDesc(Long sceneId);
}
