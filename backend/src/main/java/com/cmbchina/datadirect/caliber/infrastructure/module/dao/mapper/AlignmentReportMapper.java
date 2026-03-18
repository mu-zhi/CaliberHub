package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.AlignmentReportPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlignmentReportMapper extends JpaRepository<AlignmentReportPO, Long> {

    Optional<AlignmentReportPO> findTopBySceneIdOrderByCheckedAtDesc(Long sceneId);

    List<AlignmentReportPO> findBySceneIdOrderByCheckedAtDesc(Long sceneId);
}

