package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ExecutionFeedbackPO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionFeedbackMapper extends JpaRepository<ExecutionFeedbackPO, Long> {

    long countBySceneId(Long sceneId);

    long countBySceneIdAndSuccessTrue(Long sceneId);
}
