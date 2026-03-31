package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanSchemaLinkPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanSchemaLinkMapper extends JpaRepository<PlanSchemaLinkPO, Long> {

    List<PlanSchemaLinkPO> findByPlanIdInAndStatus(List<Long> planIds, String status);

    List<PlanSchemaLinkPO> findByPlanIdAndStatus(Long planId, String status);

    void deleteByPlanId(Long planId);
}
