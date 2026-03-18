package com.cmbchina.datadirect.caliber.infrastructure.module.supportimpl.domain;

import com.cmbchina.datadirect.caliber.domain.model.CaliberDomain;
import com.cmbchina.datadirect.caliber.domain.support.CaliberDomainSupport;
import com.cmbchina.datadirect.caliber.infrastructure.module.converter.CaliberDomainConverter;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.CaliberDomainMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.CaliberDomainPO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CaliberDomainSupportImpl implements CaliberDomainSupport {

    private final CaliberDomainMapper caliberDomainMapper;
    private final CaliberDomainConverter caliberDomainConverter;

    public CaliberDomainSupportImpl(CaliberDomainMapper caliberDomainMapper,
                                    CaliberDomainConverter caliberDomainConverter) {
        this.caliberDomainMapper = caliberDomainMapper;
        this.caliberDomainConverter = caliberDomainConverter;
    }

    @Override
    public CaliberDomain save(CaliberDomain domain) {
        CaliberDomainPO saved = caliberDomainMapper.save(caliberDomainConverter.toPO(domain));
        return caliberDomainConverter.toDomain(saved);
    }

    @Override
    public Optional<CaliberDomain> findById(Long id) {
        return caliberDomainMapper.findById(id).map(caliberDomainConverter::toDomain);
    }

    @Override
    public List<CaliberDomain> findAllOrderBySortOrder() {
        return caliberDomainMapper.findAllByOrderBySortOrderAscDomainCodeAsc().stream()
                .map(caliberDomainConverter::toDomain)
                .toList();
    }

    @Override
    public boolean existsByDomainCode(String domainCode) {
        return caliberDomainMapper.existsByDomainCode(domainCode);
    }

    @Override
    public boolean existsByDomainCodeAndIdNot(String domainCode, Long id) {
        return caliberDomainMapper.existsByDomainCodeAndIdNot(domainCode, id);
    }
}
