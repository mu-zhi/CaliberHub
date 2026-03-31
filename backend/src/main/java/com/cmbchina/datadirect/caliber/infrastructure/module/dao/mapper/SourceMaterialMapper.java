package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SourceMaterialPO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceMaterialMapper extends JpaRepository<SourceMaterialPO, String> {
}
