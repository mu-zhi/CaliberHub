package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.AuditEventPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventMapper extends JpaRepository<AuditEventPO, Long> {
    List<AuditEventPO> findBySceneIdOrderByCreatedAtDesc(Long sceneId);
    List<AuditEventPO> findByTraceIdOrderByCreatedAtDesc(String traceId);
}
