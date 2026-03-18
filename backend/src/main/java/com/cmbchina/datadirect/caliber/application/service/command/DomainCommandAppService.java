package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateDomainCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateDomainCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.DomainDTO;
import com.cmbchina.datadirect.caliber.application.assembler.DomainAssembler;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.domain.model.CaliberDomain;
import com.cmbchina.datadirect.caliber.domain.support.CaliberDomainSupport;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class DomainCommandAppService {

    private final CaliberDomainSupport caliberDomainSupport;
    private final DomainAssembler domainAssembler;

    public DomainCommandAppService(CaliberDomainSupport caliberDomainSupport, DomainAssembler domainAssembler) {
        this.caliberDomainSupport = caliberDomainSupport;
        this.domainAssembler = domainAssembler;
    }

    @Transactional
    @CacheEvict(cacheNames = {"sceneById", "sceneList"}, allEntries = true)
    public DomainDTO create(CreateDomainCmd cmd) {
        String normalizedCode = normalizeCode(cmd.domainCode());
        if (caliberDomainSupport.existsByDomainCode(normalizedCode)) {
            throw new DomainValidationException("domainCode already exists: " + normalizedCode);
        }

        CaliberDomain domain = CaliberDomain.create(
                normalizedCode,
                cmd.domainName(),
                cmd.domainOverview(),
                cmd.commonTables(),
                cmd.contacts(),
                cmd.sortOrder(),
                cmd.operator()
        );
        CaliberDomain saved = caliberDomainSupport.save(domain);
        return domainAssembler.toDTO(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"sceneById", "sceneList"}, allEntries = true)
    public DomainDTO update(Long id, UpdateDomainCmd cmd) {
        CaliberDomain existing = caliberDomainSupport.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("domain not found: " + id));

        String normalizedCode = normalizeCode(cmd.domainCode());
        if (caliberDomainSupport.existsByDomainCodeAndIdNot(normalizedCode, id)) {
            throw new DomainValidationException("domainCode already exists: " + normalizedCode);
        }

        existing.update(
                normalizedCode,
                cmd.domainName(),
                cmd.domainOverview(),
                cmd.commonTables(),
                cmd.contacts(),
                cmd.sortOrder(),
                cmd.operator()
        );
        CaliberDomain saved = caliberDomainSupport.save(existing);
        return domainAssembler.toDTO(saved);
    }

    private String normalizeCode(String domainCode) {
        return domainCode == null ? "" : domainCode.trim().toUpperCase(Locale.ROOT);
    }
}
