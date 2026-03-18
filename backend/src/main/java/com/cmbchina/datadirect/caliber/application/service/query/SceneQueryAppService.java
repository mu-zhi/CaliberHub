package com.cmbchina.datadirect.caliber.application.service.query;

import com.cmbchina.datadirect.caliber.application.api.dto.request.SceneListQuery;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneMinimumUnitCheckDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneMinimumUnitDefinitionDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.assembler.SceneAssembler;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.application.service.support.SceneMinimumUnitSupport;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.domain.model.CaliberDomain;
import com.cmbchina.datadirect.caliber.domain.model.Scene;
import com.cmbchina.datadirect.caliber.domain.model.SceneQueryCondition;
import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.domain.support.CaliberDomainSupport;
import com.cmbchina.datadirect.caliber.domain.support.SceneDomainSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SceneQueryAppService {

    private final SceneDomainSupport sceneDomainSupport;
    private final CaliberDomainSupport caliberDomainSupport;
    private final SceneAssembler sceneAssembler;
    private final ObjectMapper objectMapper;

    public SceneQueryAppService(SceneDomainSupport sceneDomainSupport,
                                CaliberDomainSupport caliberDomainSupport,
                                SceneAssembler sceneAssembler,
                                ObjectMapper objectMapper) {
        this.sceneDomainSupport = sceneDomainSupport;
        this.caliberDomainSupport = caliberDomainSupport;
        this.sceneAssembler = sceneAssembler;
        this.objectMapper = objectMapper;
    }

    @Cacheable(cacheNames = "sceneById", key = "#id", unless = "#result == null || #result.status() == null || #result.status() != 'PUBLISHED'")
    public SceneDTO getById(Long id) {
        Scene scene = sceneDomainSupport.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("scene not found: " + id));
        return enrichDomainName(sceneAssembler.toDTO(scene), buildDomainNameMap());
    }

    @Cacheable(
            cacheNames = "sceneList",
            key = "T(java.lang.String).valueOf(#query.domainId()) + '|' + T(java.lang.String).valueOf(#query.domain()) + '|' + T(java.lang.String).valueOf(#query.status()) + '|' + T(java.lang.String).valueOf(#query.keyword())",
            condition = "#query != null && #query.status() != null && #query.status().equalsIgnoreCase('PUBLISHED')",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<SceneDTO> list(SceneListQuery query) {
        SceneStatus status = parseStatus(query.status());

        List<Scene> scenes = sceneDomainSupport.findByCondition(new SceneQueryCondition(
                        query.domainId(),
                        query.domain(),
                        status,
                        query.keyword()
                ));
        if (status == null) {
            scenes = scenes.stream()
                    .filter(item -> item.getStatus() != SceneStatus.DISCARDED)
                    .toList();
        }
        Map<Long, String> domainNameMap = buildDomainNameMap();
        return sceneAssembler.toDTOList(scenes).stream()
                .map(scene -> enrichDomainName(scene, domainNameMap))
                .toList();
    }

    public SceneMinimumUnitDefinitionDTO minimumUnitDefinition() {
        return SceneMinimumUnitSupport.definition();
    }

    public SceneMinimumUnitCheckDTO checkMinimumUnit(Long id) {
        Scene scene = sceneDomainSupport.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("scene not found: " + id));
        return SceneMinimumUnitSupport.check(scene, objectMapper);
    }

    private SceneStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return SceneStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("invalid status, allowed values: DRAFT, DISCARDED, PUBLISHED");
        }
    }

    private Map<Long, String> buildDomainNameMap() {
        Map<Long, String> domainNameMap = new HashMap<>();
        for (CaliberDomain domain : caliberDomainSupport.findAllOrderBySortOrder()) {
            domainNameMap.put(domain.getId(), domain.getDomainName());
        }
        return domainNameMap;
    }

    private SceneDTO enrichDomainName(SceneDTO scene, Map<Long, String> domainNameMap) {
        String domainName = scene.domainName();
        if (scene.domainId() != null && domainNameMap.containsKey(scene.domainId())) {
            domainName = domainNameMap.get(scene.domainId());
        }
        return new SceneDTO(
                scene.id(),
                scene.sceneCode(),
                scene.sceneTitle(),
                scene.domainId(),
                scene.domain(),
                domainName,
                scene.status(),
                scene.sceneDescription(),
                scene.caliberDefinition(),
                scene.applicability(),
                scene.boundaries(),
                scene.inputsJson(),
                scene.outputsJson(),
                scene.sqlVariantsJson(),
                scene.codeMappingsJson(),
                scene.contributors(),
                scene.sqlBlocksJson(),
                scene.sourceTablesJson(),
                scene.caveatsJson(),
                scene.unmappedText(),
                scene.qualityJson(),
                scene.rawInput(),
                scene.verifiedAt(),
                scene.changeSummary(),
                scene.createdBy(),
                scene.createdAt(),
                scene.updatedAt(),
                scene.publishedBy(),
                scene.publishedAt(),
                scene.rowVersion()
        );
    }
}
