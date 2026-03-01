package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.CaliberDomainPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaliberDomainMapper extends JpaRepository<CaliberDomainPO, Long> {

    List<CaliberDomainPO> findAllByOrderBySortOrderAscDomainCodeAsc();

    boolean existsByDomainCode(String domainCode);

    boolean existsByDomainCodeAndIdNot(String domainCode, Long id);
}
