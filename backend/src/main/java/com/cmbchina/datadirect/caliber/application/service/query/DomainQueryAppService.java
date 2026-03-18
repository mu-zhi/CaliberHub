package com.cmbchina.datadirect.caliber.application.service.query;

import com.cmbchina.datadirect.caliber.application.api.dto.response.DomainDTO;
import com.cmbchina.datadirect.caliber.application.assembler.DomainAssembler;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.domain.model.CaliberDomain;
import com.cmbchina.datadirect.caliber.domain.support.CaliberDomainSupport;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DomainQueryAppService {

    private final CaliberDomainSupport caliberDomainSupport;
    private final DomainAssembler domainAssembler;

    public DomainQueryAppService(CaliberDomainSupport caliberDomainSupport, DomainAssembler domainAssembler) {
        this.caliberDomainSupport = caliberDomainSupport;
        this.domainAssembler = domainAssembler;
    }

    public List<DomainDTO> list() {
        List<CaliberDomain> domains = caliberDomainSupport.findAllOrderBySortOrder();
        return domainAssembler.toDTOList(domains);
    }

    public DomainDTO getById(Long id) {
        CaliberDomain domain = caliberDomainSupport.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("domain not found: " + id));
        return domainAssembler.toDTO(domain);
    }
}
