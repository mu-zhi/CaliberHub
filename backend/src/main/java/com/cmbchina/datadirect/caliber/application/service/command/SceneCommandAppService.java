package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.PublishSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneMinimumUnitCheckDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.assembler.SceneAssembler;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.application.service.support.SceneMinimumUnitSupport;
import com.cmbchina.datadirect.caliber.domain.model.CaliberDomain;
import com.cmbchina.datadirect.caliber.domain.model.Scene;
import com.cmbchina.datadirect.caliber.domain.model.SceneDraftUpdate;
import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.domain.support.CaliberDomainSupport;
import com.cmbchina.datadirect.caliber.domain.support.SceneDomainSupport;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class SceneCommandAppService {

    private static final Pattern SQL_NOISE_LINE = Pattern.compile(
            "(?i)^\\s*(select\\b|from\\b|where\\b|join\\b|left\\b|right\\b|inner\\b|group\\s+by\\b|order\\s+by\\b|having\\b|limit\\b|with\\b|--\\s*step\\b|```sql|```)"
    );

    private final SceneDomainSupport sceneDomainSupport;
    private final CaliberDomainSupport caliberDomainSupport;
    private final SceneAssembler sceneAssembler;
    private final CaliberDictSyncService caliberDictSyncService;
    private final AlignmentReportAppService alignmentReportAppService;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    public SceneCommandAppService(SceneDomainSupport sceneDomainSupport,
                                  CaliberDomainSupport caliberDomainSupport,
                                  SceneAssembler sceneAssembler,
                                  CaliberDictSyncService caliberDictSyncService,
                                  AlignmentReportAppService alignmentReportAppService,
                                  MeterRegistry meterRegistry,
                                  ObjectMapper objectMapper) {
        this.sceneDomainSupport = sceneDomainSupport;
        this.caliberDomainSupport = caliberDomainSupport;
        this.sceneAssembler = sceneAssembler;
        this.caliberDictSyncService = caliberDictSyncService;
        this.alignmentReportAppService = alignmentReportAppService;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @CacheEvict(cacheNames = {"sceneById", "sceneList"}, allEntries = true)
    public SceneDTO create(CreateSceneCmd cmd) {
        String domainName = resolveDomainName(cmd.domainId(), cmd.domain());
        Scene scene = Scene.createDraft(cmd.sceneTitle(), cmd.domainId(), domainName, cmd.rawInput(), cmd.operator());
        Scene saved = sceneDomainSupport.save(scene);
        return sceneAssembler.toDTO(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"sceneById", "sceneList"}, allEntries = true)
    public SceneDTO update(Long id, UpdateSceneCmd cmd) {
        Scene scene = sceneDomainSupport.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("scene not found: " + id));
        String domainName = resolveDomainName(cmd.domainId(), cmd.domain());

        scene.updateDraft(new SceneDraftUpdate(
                cmd.sceneTitle(),
                cmd.domainId(),
                domainName,
                sanitizeSceneDescription(cmd.sceneDescription()),
                cmd.caliberDefinition(),
                cmd.applicability(),
                cmd.boundaries(),
                cmd.inputsJson(),
                cmd.outputsJson(),
                cmd.sqlVariantsJson(),
                cmd.codeMappingsJson(),
                cmd.contributors(),
                cmd.sqlBlocksJson(),
                cmd.sourceTablesJson(),
                cmd.caveatsJson(),
                cmd.unmappedText(),
                cmd.qualityJson(),
                cmd.rawInput()
        ), cmd.operator());

        Scene saved = sceneDomainSupport.save(scene);
        caliberDictSyncService.syncFromScene(saved.getId(), saved.getDomainId(), saved.getCodeMappingsJson());
        return sceneAssembler.toDTO(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"sceneById", "sceneList"}, allEntries = true)
    public SceneDTO publish(Long id, PublishSceneCmd cmd) {
        Timer.Sample sample = Timer.start(meterRegistry);
        boolean success = false;
        try {
            Scene scene = sceneDomainSupport.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("scene not found: " + id));
            assertPublishableMinimumUnit(scene);
            alignmentReportAppService.assertPublishAllowed(scene.getId());

            scene.publish(cmd.verifiedAt(), cmd.changeSummary(), cmd.operator());
            Scene saved = sceneDomainSupport.save(scene);
            success = true;
            return sceneAssembler.toDTO(saved);
        } finally {
            Counter.builder("caliber.scene.publish.total")
                    .tag("success", String.valueOf(success))
                    .register(meterRegistry)
                    .increment();
            sample.stop(Timer.builder("caliber.scene.publish.latency")
                    .tag("success", String.valueOf(success))
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    @Transactional
    @CacheEvict(cacheNames = {"sceneById", "sceneList"}, allEntries = true)
    public void deleteDraft(Long id) {
        Scene scene = sceneDomainSupport.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("scene not found: " + id));
        if (scene.getStatus() == SceneStatus.PUBLISHED) {
            throw new IllegalStateException("published scene cannot be deleted in MVP");
        }
        sceneDomainSupport.deleteById(id);
    }

    @Transactional
    @CacheEvict(cacheNames = {"sceneById", "sceneList"}, allEntries = true)
    public SceneDTO discard(Long id, String operator) {
        Scene scene = sceneDomainSupport.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("scene not found: " + id));
        scene.discard(operator);
        Scene saved = sceneDomainSupport.save(scene);
        return sceneAssembler.toDTO(saved);
    }

    private String resolveDomainName(Long domainId, String domainNameFromCmd) {
        if (domainId == null) {
            return domainNameFromCmd;
        }
        CaliberDomain domain = caliberDomainSupport.findById(domainId)
                .orElseThrow(() -> new ResourceNotFoundException("domain not found: " + domainId));
        return domain.getDomainName();
    }

    private void assertPublishableMinimumUnit(Scene scene) {
        SceneMinimumUnitCheckDTO check = SceneMinimumUnitSupport.check(scene, objectMapper);
        if (Boolean.TRUE.equals(check.publishReady())) {
            return;
        }
        String message = check.items().stream()
                .filter(item -> !Boolean.TRUE.equals(item.passed()))
                .map(item -> item.name() + "：" + item.message())
                .reduce((a, b) -> a + "；" + b)
                .orElse("最小单元校验未通过");
        throw new DomainValidationException("发布失败，最小单元不完整：" + message);
    }

    private String sanitizeSceneDescription(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String[] lines = text.split("\\R", -1);
        StringBuilder cleaned = new StringBuilder();
        for (String line : lines) {
            String current = line == null ? "" : line;
            if (SQL_NOISE_LINE.matcher(current).find()) {
                continue;
            }
            cleaned.append(current).append('\n');
        }
        String normalized = cleaned.toString()
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        if (!normalized.isBlank()) {
            return normalized;
        }
        return text.trim();
    }
}
