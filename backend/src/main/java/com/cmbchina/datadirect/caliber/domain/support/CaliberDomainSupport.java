package com.cmbchina.datadirect.caliber.domain.support;

import com.cmbchina.datadirect.caliber.domain.model.CaliberDomain;

import java.util.List;
import java.util.Optional;

public interface CaliberDomainSupport {

    CaliberDomain save(CaliberDomain domain);

    Optional<CaliberDomain> findById(Long id);

    List<CaliberDomain> findAllOrderBySortOrder();

    boolean existsByDomainCode(String domainCode);

    boolean existsByDomainCodeAndIdNot(String domainCode, Long id);
}
