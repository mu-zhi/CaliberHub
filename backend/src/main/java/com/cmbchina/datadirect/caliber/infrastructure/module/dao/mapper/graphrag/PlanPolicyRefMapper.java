package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPolicyRefPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanPolicyRefMapper extends JpaRepository<PlanPolicyRefPO, Long> {

    List<PlanPolicyRefPO> findByPlanIdIn(List<Long> planIds);

    List<PlanPolicyRefPO> findByPlanId(Long planId);

    void deleteByPlanId(Long planId);
}
