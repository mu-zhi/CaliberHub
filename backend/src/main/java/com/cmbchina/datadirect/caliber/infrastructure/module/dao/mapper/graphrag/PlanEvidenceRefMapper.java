package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanEvidenceRefPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanEvidenceRefMapper extends JpaRepository<PlanEvidenceRefPO, Long> {

    List<PlanEvidenceRefPO> findByPlanIdIn(List<Long> planIds);

    List<PlanEvidenceRefPO> findByPlanId(Long planId);

    void deleteByPlanId(Long planId);
}
