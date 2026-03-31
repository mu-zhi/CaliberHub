package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateDomainCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateDomainCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.DomainBootstrapResultDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.DomainDTO;
import com.cmbchina.datadirect.caliber.application.assembler.DomainAssembler;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.domain.model.CaliberDomain;
import com.cmbchina.datadirect.caliber.domain.support.CaliberDomainSupport;
import com.cmbchina.datadirect.caliber.infrastructure.module.supportimpl.application.BusinessCategoryTreeProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DomainCommandAppService {

    private final CaliberDomainSupport caliberDomainSupport;
    private final DomainAssembler domainAssembler;
    private final BusinessCategoryTreeProvider businessCategoryTreeProvider;

    public DomainCommandAppService(CaliberDomainSupport caliberDomainSupport,
                                   DomainAssembler domainAssembler,
                                   BusinessCategoryTreeProvider businessCategoryTreeProvider) {
        this.caliberDomainSupport = caliberDomainSupport;
        this.domainAssembler = domainAssembler;
        this.businessCategoryTreeProvider = businessCategoryTreeProvider;
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

    @Transactional
    @CacheEvict(cacheNames = {"sceneById", "sceneList"}, allEntries = true)
    public DomainBootstrapResultDTO bootstrapFromBusinessCategories(String operator) {
        List<CaliberDomain> existingDomains = caliberDomainSupport.findAllOrderBySortOrder();
        Set<String> existingCodes = new HashSet<>();
        Set<String> existingNames = new HashSet<>();
        for (CaliberDomain domain : existingDomains) {
            existingCodes.add(normalizeCode(domain.getDomainCode()));
            existingNames.add(normalizeName(domain.getDomainName()));
        }

        int createdCount = 0;
        List<BusinessCategoryTreeProvider.TopicNode> roots = businessCategoryTreeProvider.roots();
        if (roots.isEmpty()) {
            createdCount += ensureFallbackDomain(existingCodes, existingNames, operator);
        } else {
            for (int index = 0; index < roots.size(); index += 1) {
                BusinessCategoryTreeProvider.TopicNode root = roots.get(index);
                String domainCode = buildBootstrapDomainCode(root);
                String domainName = normalizeName(root.name());
                if (existingCodes.contains(domainCode) || existingNames.contains(domainName)) {
                    continue;
                }
                caliberDomainSupport.save(CaliberDomain.create(
                        domainCode,
                        domainName,
                        "从业务场景分类一级目录初始化的默认业务领域。",
                        "",
                        "",
                        resolveBootstrapSortOrder(root, index),
                        normalizeOperator(operator)
                ));
                existingCodes.add(domainCode);
                existingNames.add(domainName);
                createdCount += 1;
            }
        }

        List<DomainDTO> domains = domainAssembler.toDTOList(caliberDomainSupport.findAllOrderBySortOrder());
        return new DomainBootstrapResultDTO(createdCount, domains.size(), domains);
    }

    private String normalizeCode(String domainCode) {
        return domainCode == null ? "" : domainCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String domainName) {
        return domainName == null ? "" : domainName.trim();
    }

    private String normalizeOperator(String operator) {
        String normalized = normalizeName(operator);
        return normalized.isEmpty() ? "system" : normalized;
    }

    private String buildBootstrapDomainCode(BusinessCategoryTreeProvider.TopicNode root) {
        String rawCode = normalizeName(root.code());
        if (rawCode.isEmpty()) {
            rawCode = normalizeName(root.id());
        }
        String sanitized = rawCode.replaceAll("[^0-9A-Za-z]+", "_").replaceAll("_+", "_");
        sanitized = sanitized.replaceAll("^_+|_+$", "");
        if (sanitized.isEmpty()) {
            sanitized = "ROOT";
        }
        return normalizeCode("CATEGORY_" + sanitized);
    }

    private int resolveBootstrapSortOrder(BusinessCategoryTreeProvider.TopicNode root, int index) {
        String code = normalizeName(root.code());
        if (code.matches("\\d+")) {
            return Integer.parseInt(code) * 10;
        }
        return (index + 1) * 10;
    }

    private int ensureFallbackDomain(Set<String> existingCodes, Set<String> existingNames, String operator) {
        String fallbackCode = "UNCLASSIFIED";
        String fallbackName = "未分类业务领域";
        if (existingCodes.contains(fallbackCode) || existingNames.contains(fallbackName)) {
            return 0;
        }
        caliberDomainSupport.save(CaliberDomain.create(
                fallbackCode,
                fallbackName,
                "系统启动时自动创建的默认业务领域，建议在业务领域管理中维护正式业务领域后再迁移场景。",
                "",
                "",
                9999,
                normalizeOperator(operator)
        ));
        existingCodes.add(fallbackCode);
        existingNames.add(fallbackName);
        return 1;
    }
}
