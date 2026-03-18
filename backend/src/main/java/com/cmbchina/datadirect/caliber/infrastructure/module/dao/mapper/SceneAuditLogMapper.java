package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneAuditLogPO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SceneAuditLogMapper extends JpaRepository<SceneAuditLogPO, Long> {
}
