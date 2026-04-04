package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.GraphProjectionStatusDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphNodeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphResponseDTO;
import com.cmbchina.datadirect.caliber.application.service.graphrag.HashEmbeddingSupport;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.ProjectionVerificationStatus;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.ReadSource;
import com.cmbchina.datadirect.caliber.application.service.query.datamap.DataMapGraphDtoAdapter;
import com.cmbchina.datadirect.caliber.application.service.query.datamap.DataMapGraphQueryOptions;
import com.cmbchina.datadirect.caliber.application.service.query.datamap.GraphReadService;
import com.cmbchina.datadirect.caliber.application.service.query.datamap.GraphSceneBundle;
import com.cmbchina.datadirect.caliber.application.service.query.datamap.Neo4jGraphReadService;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.graphrag.GraphRuntimeProperties;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneVersionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CoverageDeclarationMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanPolicyRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PolicyMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ProjectionEventMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneVersionPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CoverageDeclarationPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EvidenceFragmentPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.OutputContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPolicyRefPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PolicyPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.ProjectionEventPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GraphProjectionAppService {

    private final ProjectionEventMapper projectionEventMapper;
    private final SceneMapper sceneMapper;
    private final SceneVersionMapper sceneVersionMapper;
    private final PlanMapper planMapper;
    private final EvidenceFragmentMapper evidenceFragmentMapper;
    private final CoverageDeclarationMapper coverageDeclarationMapper;
    private final PolicyMapper policyMapper;
    private final PlanPolicyRefMapper planPolicyRefMapper;
    private final OutputContractMapper outputContractMapper;
    private final GraphRuntimeProperties graphRuntimeProperties;
    private final GraphReadService graphReadService;
    private final DataMapGraphDtoAdapter dataMapGraphDtoAdapter;
    private final Neo4jGraphReadService neo4jGraphReadService;
    private final HashEmbeddingSupport hashEmbeddingSupport;
    private final ObjectMapper objectMapper;
    private final Optional<Driver> neo4jDriver;
    private final ProjectionValidationService projectionValidationService;

    public GraphProjectionAppService(ProjectionEventMapper projectionEventMapper,
                                     SceneMapper sceneMapper,
                                     SceneVersionMapper sceneVersionMapper,
                                     PlanMapper planMapper,
                                     EvidenceFragmentMapper evidenceFragmentMapper,
                                     CoverageDeclarationMapper coverageDeclarationMapper,
                                     PolicyMapper policyMapper,
                                     PlanPolicyRefMapper planPolicyRefMapper,
                                     OutputContractMapper outputContractMapper,
                                     GraphRuntimeProperties graphRuntimeProperties,
                                     HashEmbeddingSupport hashEmbeddingSupport,
                                     ObjectMapper objectMapper,
                                     Optional<Driver> neo4jDriver,
                                     @Lazy GraphReadService graphReadService,
                                     DataMapGraphDtoAdapter dataMapGraphDtoAdapter,
                                     Neo4jGraphReadService neo4jGraphReadService,
                                     ProjectionValidationService projectionValidationService) {
        this.projectionEventMapper = projectionEventMapper;
        this.sceneMapper = sceneMapper;
        this.sceneVersionMapper = sceneVersionMapper;
        this.planMapper = planMapper;
        this.evidenceFragmentMapper = evidenceFragmentMapper;
        this.coverageDeclarationMapper = coverageDeclarationMapper;
        this.policyMapper = policyMapper;
        this.planPolicyRefMapper = planPolicyRefMapper;
        this.outputContractMapper = outputContractMapper;
        this.graphRuntimeProperties = graphRuntimeProperties;
        this.hashEmbeddingSupport = hashEmbeddingSupport;
        this.objectMapper = objectMapper;
        this.neo4jDriver = neo4jDriver;
        this.graphReadService = graphReadService;
        this.dataMapGraphDtoAdapter = dataMapGraphDtoAdapter;
        this.neo4jGraphReadService = neo4jGraphReadService;
        this.projectionValidationService = projectionValidationService;
    }

    @Transactional
    public GraphProjectionStatusDTO refreshProjection(Long sceneId, String sceneCode, String operator) {
        Long snapshotId = resolveSnapshotId(sceneId);
        return refreshProjection(sceneId, snapshotId, sceneCode, operator);
    }

    @Transactional
    public GraphProjectionStatusDTO refreshProjection(Long sceneId, Long snapshotId, String sceneCode, String operator) {
        if (snapshotId == null) {
            snapshotId = resolveSnapshotId(sceneId);
        }

        ProjectionEventPO event = projectionEventMapper.findBySceneId(sceneId).orElseGet(ProjectionEventPO::new);
        OffsetDateTime now = OffsetDateTime.now();
        if (event.getId() == null) {
            event.setSceneId(sceneId);
            event.setSceneCode(sceneCode);
            event.setCreatedAt(now);
        }
        event.setSceneCode(sceneCode);
        event.setUpdatedAt(now);
        event.setStage("PREPARE");
        event.setStatus("PENDING");
        event.setMessage("正在准备图投影");
        event.setPayloadJson(buildPayloadJson(sceneId, snapshotId));
        projectionEventMapper.save(event);

        if (!graphRuntimeProperties.isProjectionEnabled()) {
            event.setStage("RELATIONAL_ONLY");
            event.setStatus("SKIPPED");
            event.setMessage("图投影已关闭，当前使用关系库检索运行时");
            event.setLastProjectedAt(now);
            event.setUpdatedAt(now);
            ProjectionEventPO saved = projectionEventMapper.save(event);
            if (snapshotId != null) {
                projectionValidationService.recordSkipped(sceneId, snapshotId, "图投影已关闭");
            }
            return toDTO(saved);
        }

        try {
            projectScene(sceneId, snapshotId);
            event.setStage("UPSERTED");
            event.setStatus("SUCCEEDED");
            event.setMessage("Neo4j 投影完成");
            event.setLastProjectedAt(now);
            event.setUpdatedAt(now);
            ProjectionEventPO saved = projectionEventMapper.save(event);
            if (snapshotId != null) {
                DataMapGraphResponseDTO relationalSnapshotGraph = buildRelationalSnapshotGraph(sceneId, snapshotId);
                var neo4jSnapshotGraph = neo4jGraphReadService.readGraph(
                        sceneId,
                        snapshotId,
                        DataMapGraphQueryOptions.of(snapshotId, null, null, null, null)
                );
                projectionValidationService.recordProjectionAndValidate(
                        sceneId,
                        snapshotId,
                        nodeKeys(relationalSnapshotGraph.nodes()),
                        edgeKeys(relationalSnapshotGraph.edges()),
                        nodeKeys(neo4jSnapshotGraph.nodes()),
                        edgeKeys(neo4jSnapshotGraph.edges())
                );
            }
            return toDTO(saved);
        } catch (Exception ex) {
            event.setStage("FAILED");
            event.setStatus("FAILED");
            event.setMessage(ex.getMessage());
            event.setUpdatedAt(now);
            ProjectionEventPO saved = projectionEventMapper.save(event);
            if (snapshotId != null) {
                projectionValidationService.recordFailed(sceneId, snapshotId, ex.getMessage());
            }
            return toDTO(saved);
        }
    }

    @Transactional(readOnly = true)
    public GraphProjectionStatusDTO getStatus(Long sceneId) {
        return projectionEventMapper.findBySceneId(sceneId)
                .map(this::toDTO)
                .orElseGet(() -> new GraphProjectionStatusDTO(sceneId, resolveSnapshotId(sceneId), null, "NOT_FOUND", "IDLE", "尚未触发投影", null, null, null));
    }

    @Transactional
    public GraphProjectionStatusDTO rebuildProjection(Long sceneId, String operator) {
        ScenePO scene = sceneMapper.findById(sceneId).orElse(null);
        if (scene == null) {
            return new GraphProjectionStatusDTO(sceneId, null, null, "NOT_FOUND", "IDLE", "scene not found", null, null, null);
        }
        return refreshProjection(sceneId, scene.getSceneCode(), operator);
    }

    private void projectScene(Long sceneId, Long snapshotId) throws Exception {
        Driver driver = neo4jDriver.orElseThrow(() ->
                new IllegalStateException("Neo4j driver is not configured"));
        ScenePO scene = sceneMapper.findById(sceneId).orElse(null);
        if (scene == null) {
            return;
        }
        List<PlanPO> plans = planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        List<EvidenceFragmentPO> evidences = evidenceFragmentMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        List<OutputContractPO> contracts = outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        try (var session = driver.session(SessionConfig.forDatabase(graphRuntimeProperties.getNeo4jDatabase()))) {
                session.executeWrite(tx -> {
                    tx.run("MATCH (s:Scene {sceneId:$sceneId, snapshotId:$snapshotId}) DETACH DELETE s",
                            Values.parameters(
                                    "sceneId", sceneId,
                                    "snapshotId", snapshotId
                            ));
                    tx.run("MERGE (s:Scene {sceneId:$sceneId, snapshotId:$snapshotId}) SET s.sceneCode=$sceneCode, s.sceneTitle=$sceneTitle, s.sceneType=$sceneType, s.status=$status, s.embedding=$embedding",
                            Values.parameters(
                                    "sceneId", sceneId,
                                    "snapshotId", snapshotId,
                                    "sceneCode", scene.getSceneCode(),
                                    "sceneTitle", scene.getSceneTitle(),
                                    "sceneType", scene.getSceneType(),
                                    "status", scene.getStatus() == null ? "DRAFT" : scene.getStatus().name(),
                                    "embedding", hashEmbeddingSupport.embed(scene.getSceneTitle() + " " + safe(scene.getSceneDescription()))));
                        tx.run("MATCH (s:Scene {sceneId:$sceneId, snapshotId:$snapshotId})-[r]->(n) DETACH DELETE n",
                                Values.parameters(
                                        "sceneId", sceneId,
                                    "snapshotId", snapshotId
                            ));
                    for (OutputContractPO contract : contracts) {
                        tx.run("MATCH (s:Scene {sceneId:$sceneId, snapshotId:$snapshotId}) MERGE (o:OutputContract {contractId:$contractId, snapshotId:$snapshotId}) SET o.contractCode=$contractCode, o.contractName=$contractName, o.summaryText=$summaryText MERGE (s)-[:EMITS {snapshotId:$snapshotId}]->(o)",
                                Values.parameters(
                                        "sceneId", sceneId,
                                        "snapshotId", snapshotId,
                                        "contractId", contract.getId(),
                                        "contractCode", contract.getContractCode(),
                                        "contractName", contract.getContractName(),
                                        "summaryText", safe(contract.getSummaryText())));
                    }
                    for (PlanPO plan : plans) {
                        tx.run("MATCH (s:Scene {sceneId:$sceneId, snapshotId:$snapshotId}) MERGE (p:Plan {planId:$planId, snapshotId:$snapshotId}) SET p.planCode=$planCode, p.planName=$planName, p.retrievalText=$retrievalText, p.status=$status, p.embedding=$embedding MERGE (s)-[:HAS_PLAN {snapshotId:$snapshotId}]->(p)",
                                Values.parameters(
                                        "sceneId", sceneId,
                                        "snapshotId", snapshotId,
                                        "planId", plan.getId(),
                                        "planCode", plan.getPlanCode(),
                                        "planName", plan.getPlanName(),
                                        "retrievalText", safe(plan.getRetrievalText()),
                                        "status", plan.getStatus(),
                                        "embedding", hashEmbeddingSupport.embed(safe(plan.getRetrievalText()))));
                        for (CoverageDeclarationPO coverage : coverageDeclarationMapper.findByPlanIdOrderByUpdatedAtDesc(plan.getId())) {
                        tx.run("MATCH (p:Plan {planId:$planId}) MERGE (c:Coverage {coverageId:$coverageId}) SET c.coverageCode=$coverageCode, c.coverageStatus=$coverageStatus, c.statementText=$statementText MERGE (p)-[:COVERED_BY]->(c)",
                                Values.parameters(
                                        "planId", plan.getId(),
                                        "snapshotId", snapshotId,
                                        "coverageId", coverage.getId(),
                                        "coverageCode", coverage.getCoverageCode(),
                                        "coverageStatus", coverage.getCoverageStatus(),
                                        "statementText", safe(coverage.getStatementText())));
                        }
                        for (PlanPolicyRefPO policyRef : planPolicyRefMapper.findByPlanId(plan.getId())) {
                            PolicyPO policy = policyMapper.findById(policyRef.getPolicyId()).orElse(null);
                            if (policy == null) {
                                continue;
                            }
                            tx.run("MATCH (p:Plan {planId:$planId, snapshotId:$snapshotId}) MERGE (pl:Policy {policyId:$policyId, snapshotId:$snapshotId}) SET pl.policyCode=$policyCode, pl.effectType=$effectType, pl.sensitivityLevel=$sensitivityLevel MERGE (p)-[:BOUND_BY {snapshotId:$snapshotId}]->(pl)",
                                    Values.parameters(
                                            "planId", plan.getId(),
                                            "snapshotId", snapshotId,
                                            "policyId", policy.getId(),
                                            "policyCode", policy.getPolicyCode(),
                                            "effectType", policy.getEffectType(),
                                            "sensitivityLevel", policy.getSensitivityLevel()));
                        }
                    }
                    for (EvidenceFragmentPO evidence : evidences) {
                        tx.run("MATCH (s:Scene {sceneId:$sceneId, snapshotId:$snapshotId}) MERGE (e:Evidence {evidenceId:$evidenceId, snapshotId:$snapshotId}) SET e.evidenceCode=$evidenceCode, e.title=$title, e.fragmentText=$fragmentText, e.embedding=$embedding MERGE (s)-[:BACKED_BY {snapshotId:$snapshotId}]->(e)",
                                Values.parameters(
                                        "sceneId", sceneId,
                                        "snapshotId", snapshotId,
                                        "evidenceId", evidence.getId(),
                                        "evidenceCode", evidence.getEvidenceCode(),
                                        "title", evidence.getTitle(),
                                        "fragmentText", safe(evidence.getFragmentText()),
                                        "embedding", hashEmbeddingSupport.embed(evidence.getTitle() + " " + safe(evidence.getFragmentText()))));
                    }
                    return null;
                });
            }
    }

    private DataMapGraphResponseDTO buildRelationalSnapshotGraph(Long sceneId, Long snapshotId) {
        GraphSceneBundle bundle = graphReadService.loadBundle("SCENE", sceneId, snapshotId);
        DataMapGraphResponseDTO graph = dataMapGraphDtoAdapter.buildGraph(
                "scene:" + sceneId,
                bundle,
                DataMapGraphQueryOptions.of(snapshotId, null, null, null, null)
        );
        return new DataMapGraphResponseDTO(
                graph.rootRef(),
                graph.sceneId(),
                graph.sceneName(),
                snapshotId,
                ReadSource.RELATIONAL,
                ProjectionVerificationStatus.PENDING,
                null,
                graph.nodes(),
                graph.edges()
        );
    }

    private Set<String> nodeKeys(List<DataMapGraphNodeDTO> nodes) {
        return nodes.stream().map(DataMapGraphNodeDTO::id).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> edgeKeys(List<DataMapGraphEdgeDTO> edges) {
        return edges.stream()
                .map(edge -> edge.source() + "|" + edge.relationType() + "|" + edge.target())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private GraphProjectionStatusDTO toDTO(ProjectionEventPO event) {
        return new GraphProjectionStatusDTO(
                event.getSceneId(),
                resolveProjectedSnapshotId(event),
                event.getSceneCode(),
                event.getStatus(),
                event.getStage(),
                event.getMessage(),
                event.getPayloadJson(),
                event.getLastProjectedAt(),
                event.getUpdatedAt()
        );
    }

    private String buildPayloadJson(Long sceneId, Long snapshotId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("snapshotId", snapshotId);
        payload.put("plans", planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).size());
        payload.put("evidences", evidenceFragmentMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).size());
        payload.put("contracts", outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).size());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private Long resolveProjectedSnapshotId(ProjectionEventPO event) {
        if (event == null) {
            return null;
        }
        String payloadJson = event.getPayloadJson();
        if (payloadJson != null && !payloadJson.isBlank()) {
            try {
                var payloadNode = objectMapper.readTree(payloadJson);
                Long snapshotId = payloadNode.path("snapshotId").isNumber()
                        ? payloadNode.path("snapshotId").asLong()
                        : null;
                if (snapshotId != null && snapshotId > 0) {
                    return snapshotId;
                }
            } catch (Exception ignored) {
                // keep backward compatibility with old payload format
            }
        }
        return resolveSnapshotId(event.getSceneId());
    }

    private Long resolveSnapshotId(Long sceneId) {
        if (sceneId == null) {
            return null;
        }
        return sceneVersionMapper.findTopBySceneIdOrderByVersionNoDesc(sceneId)
                .map(SceneVersionPO::getId)
                .orElse(null);
    }
}
