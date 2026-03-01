package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.PlanIrAuditPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanIrAuditMapper extends JpaRepository<PlanIrAuditPO, Long> {

    List<PlanIrAuditPO> findBySceneIdOrderByCreatedAtDesc(Long sceneId);
}

