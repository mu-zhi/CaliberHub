package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.PublishSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneVersionDTO;
import com.cmbchina.datadirect.caliber.application.assembler.SceneAssembler;
import com.cmbchina.datadirect.caliber.application.exception.BusinessConflictException;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.application.service.command.graphrag.GraphProjectionAppService;
import com.cmbchina.datadirect.caliber.application.service.command.graphrag.CanonicalSnapshotBindingService;
import com.cmbchina.datadirect.caliber.application.service.command.graphrag.SceneGraphAssetSyncService;
import com.cmbchina.datadirect.caliber.application.service.governance.SceneGovernanceGateAppService;
import com.cmbchina.datadirect.caliber.application.service.query.graphrag.ScenePublishGateAppService;
import com.cmbchina.datadirect.caliber.domain.model.CaliberDomain;
import com.cmbchina.datadirect.caliber.domain.model.Scene;
import com.cmbchina.datadirect.caliber.domain.model.SceneDraftUpdate;
import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.domain.support.CaliberDomainSupport;
import com.cmbchina.datadirect.caliber.domain.support.SceneDomainSupport;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneAuditLogMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneAuditLogPO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class SceneCommandAppService {

    private static final Pattern SQL_NOISE_LINE = Pattern.compile(
            "(?i)^\\s*(select\\b|from\\b|where\\b|join\\b|left\\b|right\\b|inner\\b|group\\s+by\\b|order\\s+by\\b|having\\b|limit\\b|with\\b|--\\s*step\\b|```sql|```)"
    );
    private static final String SOFT_GATE_QG102 = "QG-102:id_mapping_notes_missing";

    private final SceneDomainSupport sceneDomainSupport;
    private final CaliberDomainSupport caliberDomainSupport;
    private final SceneAssembler sceneAssembler;
    private final CaliberDictSyncService caliberDictSyncService;
    private final AlignmentReportAppService alignmentReportAppService;
    private final SceneGraphAssetSyncService sceneGraphAssetSyncService;
    private final ScenePublishGateAppService scenePublishGateAppService;
    private final SceneVersionAppService sceneVersionAppService;
    private final CanonicalSnapshotBindingService canonicalSnapshotBindingService;
    private final GraphProjectionAppService graphProjectionAppService;
    private final SceneGovernanceGateAppService sceneGovernanceGateAppService;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final SceneAuditLogMapper sceneAuditLogMapper;

    public SceneCommandAppService(SceneDomainSupport sceneDomainSupport,
                                  CaliberDomainSupport caliberDomainSupport,
                                  SceneAssembler sceneAssembler,
                                  CaliberDictSyncService caliberDictSyncService,
                                  AlignmentReportAppService alignmentReportAppService,
                                  SceneGraphAssetSyncService sceneGraphAssetSyncService,
                                  ScenePublishGateAppService scenePublishGateAppService,
                                  SceneVersionAppService sceneVersionAppService,
                                  CanonicalSnapshotBindingService canonicalSnapshotBindingService,
                                  GraphProjectionAppService graphProjectionAppService,
                                  SceneGovernanceGateAppService sceneGovernanceGateAppService,
                                  MeterRegistry meterRegistry,
                                  ObjectMapper objectMapper,
                                  SceneAuditLogMapper sceneAuditLogMapper) {
        this.sceneDomainSupport = sceneDomainSupport;
        this.caliberDomainSupport = caliberDomainSupport;
        this.sceneAssembler = sceneAssembler;
        this.caliberDictSyncService = caliberDictSyncService;
        this.alignmentReportAppService = alignmentReportAppService;
        this.sceneGraphAssetSyncService = sceneGraphAssetSyncService;
        this.scenePublishGateAppService = scenePublishGateAppService;
        this.sceneVersionAppService = sceneVersionAppService;
        this.canonicalSnapshotBindingService = canonicalSnapshotBindingService;
        this.graphProjectionAppService = graphProjectionAppService;
        this.sceneGovernanceGateAppService = sceneGovernanceGateAppService;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
        this.sceneAuditLogMapper = sceneAuditLogMapper;
    }

    @Transactional
    @CacheEvict(cacheNames = {"sceneById", "sceneList"}, allEntries = true)
    public SceneDTO create(CreateSceneCmd cmd) {
        String domainName = resolveDomainName(cmd.domainId(), cmd.domain());
        Scene scene = Scene.createDraft(cmd.sceneTitle(), cmd.domainId(), domainName, cmd.rawInput(), cmd.operator());
        scene.setSceneType(normalizeSceneType(cmd.sceneType()));
        Scene saved = saveWithConflictGuard(scene);
        sceneGraphAssetSyncService.ensureGovernanceAssets(saved.getId(), cmd.operator());
        writeSceneAudit(saved.getId(), "CREATE_DRAFT", cmd.operator(), Map.of("status", saved.getStatus().name()));
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
                normalizeSceneType(cmd.sceneType()),
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

        assertExpectedVersion(scene, cmd.expectedVersion());
        Scene saved = saveWithConflictGuard(scene);
        sceneGraphAssetSyncService.syncSceneAssetsFromLegacy(saved.getId(), cmd.operator());
        caliberDictSyncService.syncFromScene(saved.getId(), saved.getDomainId(), saved.getCodeMappingsJson());
        writeSceneAudit(saved.getId(), "UPDATE_DRAFT", cmd.operator(), Map.of("status", saved.getStatus().name()));
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
            sceneGraphAssetSyncService.ensureGovernanceAssets(scene.getId(), cmd.operator());
            scenePublishGateAppService.assertPublishable(scene);
            sceneGovernanceGateAppService.assertPublishable(scene.getId(), cmd.operator());
            alignmentReportAppService.assertPublishAllowed(scene.getId());
            List<String> softGateWarnings = collectPublishSoftGateWarnings(scene);

            scene.publish(cmd.verifiedAt(), cmd.changeSummary(), cmd.operator());
            Scene saved = saveWithConflictGuard(scene);
            sceneGraphAssetSyncService.syncAssetStatuses(saved.getId(), saved.getStatus().name(), cmd.operator());
            SceneVersionDTO snapshot = sceneVersionAppService.createPublishedSnapshot(saved.getId(), cmd.changeSummary(), cmd.operator());
            canonicalSnapshotBindingService.bindSceneSnapshot(saved.getId(), snapshot.id(), cmd.operator());
            graphProjectionAppService.refreshProjection(saved.getId(), saved.getSceneCode(), cmd.operator());
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("status", saved.getStatus().name());
            detail.put("verifiedAt", saved.getVerifiedAt());
            detail.put("publishedAt", saved.getPublishedAt());
            detail.put("snapshotId", snapshot.id());
            if (!softGateWarnings.isEmpty()) {
                detail.put("softGateWarnings", softGateWarnings);
                Counter.builder("caliber.scene.publish.soft_gate.total")
                        .tag("rule", "QG-102")
                        .register(meterRegistry)
                        .increment();
            }
            writeSceneAudit(saved.getId(), "PUBLISH", cmd.operator(), detail);
            success = true;
            return withSnapshotId(sceneAssembler.toDTO(saved), snapshot.id());
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
    public void deleteDraft(Long id, String operator) {
        Scene scene = sceneDomainSupport.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("scene not found: " + id));
        if (scene.getStatus() == SceneStatus.PUBLISHED) {
            throw new IllegalStateException("published scene cannot be deleted in MVP");
        }
        sceneDomainSupport.deleteById(id);
        writeSceneAudit(id, "DELETE_DRAFT", operator, Map.of("status", scene.getStatus().name()));
    }

    @Transactional
    @CacheEvict(cacheNames = {"sceneById", "sceneList"}, allEntries = true)
    public SceneDTO discard(Long id, String operator) {
        Scene scene = sceneDomainSupport.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("scene not found: " + id));
        scene.discard(operator);
        Scene saved = saveWithConflictGuard(scene);
        sceneGraphAssetSyncService.syncAssetStatuses(saved.getId(), saved.getStatus().name(), operator);
        graphProjectionAppService.refreshProjection(saved.getId(), saved.getSceneCode(), operator);
        writeSceneAudit(saved.getId(), "DISCARD", operator, Map.of("status", saved.getStatus().name()));
        return sceneAssembler.toDTO(saved);
    }

    private Scene saveWithConflictGuard(Scene scene) {
        try {
            return sceneDomainSupport.save(scene);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessConflictException("CAL-SC-409",
                    "scene version conflict, please refresh and retry");
        }
    }

    private void assertExpectedVersion(Scene scene, Long expectedVersion) {
        if (expectedVersion == null) {
            return;
        }
        if (!Objects.equals(scene.getRowVersion(), expectedVersion)) {
            throw new BusinessConflictException("CAL-SC-409",
                    "scene version conflict, expected=" + expectedVersion + ", actual=" + scene.getRowVersion());
        }
    }

    private void writeSceneAudit(Long sceneId, String action, String operator, Map<String, Object> detail) {
        SceneAuditLogPO audit = new SceneAuditLogPO();
        audit.setSceneId(sceneId);
        audit.setAction(action);
        audit.setOperator(normalizeOperator(operator));
        audit.setDetailJson(writeJson(detail));
        audit.setCreatedAt(OffsetDateTime.now());
        sceneAuditLogMapper.save(audit);
    }

    private String writeJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{\"serializationError\":true}";
        }
    }

    private String normalizeOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            return "system";
        }
        return operator.trim();
    }

    private String resolveDomainName(Long domainId, String domainNameFromCmd) {
        if (domainId == null) {
            return domainNameFromCmd;
        }
        CaliberDomain domain = caliberDomainSupport.findById(domainId)
                .orElseThrow(() -> new ResourceNotFoundException("domain not found: " + domainId));
        return domain.getDomainName();
    }

    private List<String> collectPublishSoftGateWarnings(Scene scene) {
        List<String> warnings = new ArrayList<>();
        if (!hasIdMappingNotes(scene.getCodeMappingsJson())) {
            warnings.add(SOFT_GATE_QG102);
        }
        return warnings;
    }

    private boolean hasIdMappingNotes(String codeMappingsJson) {
        if (codeMappingsJson == null || codeMappingsJson.isBlank()) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(codeMappingsJson);
            if (!root.isArray() || root.isEmpty()) {
                return false;
            }
            for (JsonNode group : root) {
                if (isNonBlank(group.path("id_mapping_notes").asText(null))) {
                    return true;
                }
                JsonNode mappings = group.path("mappings");
                if (!mappings.isArray()) {
                    continue;
                }
                for (JsonNode mapping : mappings) {
                    if (isNonBlank(mapping.path("id_mapping_notes").asText(null))) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isNonBlank(String text) {
        return text != null && !text.trim().isEmpty();
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

    private SceneDTO withSnapshotId(SceneDTO dto, Long snapshotId) {
        return new SceneDTO(
                dto.id(),
                dto.sceneCode(),
                dto.sceneTitle(),
                dto.domainId(),
                dto.domain(),
                dto.domainName(),
                dto.sceneType(),
                dto.status(),
                dto.sceneDescription(),
                dto.caliberDefinition(),
                dto.applicability(),
                dto.boundaries(),
                dto.inputsJson(),
                dto.outputsJson(),
                dto.sqlVariantsJson(),
                dto.codeMappingsJson(),
                dto.contributors(),
                dto.sqlBlocksJson(),
                dto.sourceTablesJson(),
                dto.caveatsJson(),
                dto.unmappedText(),
                dto.qualityJson(),
                dto.rawInput(),
                dto.verifiedAt(),
                dto.changeSummary(),
                dto.createdBy(),
                dto.createdAt(),
                dto.updatedAt(),
                dto.publishedBy(),
                dto.publishedAt(),
                dto.rowVersion(),
                snapshotId
        );
    }

    private String normalizeSceneType(String sceneType) {
        if (sceneType == null || sceneType.isBlank()) {
            return "FACT_DETAIL";
        }
        return sceneType.trim().toUpperCase();
    }
}
