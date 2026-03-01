package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.CaliberDictPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaliberDictMapper extends JpaRepository<CaliberDictPO, Long> {

    Optional<CaliberDictPO> findByDomainScopeAndCodeAndValueCode(String domainScope, String code, String valueCode);
}
