package com.cmbchina.datadirect.caliber.application.service.query.graphrag;

import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.GraphQueryCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.GraphEntityLinkDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.GraphPathEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.GraphPathGraphDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.GraphPathNodeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.GraphPlanCandidateDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.GraphQueryResultDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.GraphSchemaLinkDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.GraphScoreBreakdownDTO;
import com.cmbchina.datadirect.caliber.application.service.graphrag.GraphAssetSupport;
import com.cmbchina.datadirect.caliber.application.service.graphrag.HashEmbeddingSupport;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.graphrag.GraphRuntimeProperties;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CoverageDeclarationMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EntityAliasMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.InputSlotSchemaMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanEvidenceRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanPolicyRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanSchemaLinkMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PolicyMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CoverageDeclarationPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EntityAliasPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EvidenceFragmentPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.InputSlotSchemaPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.OutputContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanEvidenceRefPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPolicyRefPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanSchemaLinkPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PolicyPO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class GraphRagQueryAppService {

    private final SceneMapper sceneMapper;
    private final PlanMapper planMapper;
    private final EvidenceFragmentMapper evidenceFragmentMapper;
    private final CoverageDeclarationMapper coverageDeclarationMapper;
    private final PolicyMapper policyMapper;
    private final OutputContractMapper outputContractMapper;
    private final InputSlotSchemaMapper inputSlotSchemaMapper;
    private final EntityAliasMapper entityAliasMapper;
    private final PlanEvidenceRefMapper planEvidenceRefMapper;
    private final PlanPolicyRefMapper planPolicyRefMapper;
    private final PlanSchemaLinkMapper planSchemaLinkMapper;
    private final GraphAssetSupport graphAssetSupport;
    private final HashEmbeddingSupport hashEmbeddingSupport;
    private final GraphRuntimeProperties graphRuntimeProperties;

    public GraphRagQueryAppService(SceneMapper sceneMapper,
                                   PlanMapper planMapper,
                                   EvidenceFragmentMapper evidenceFragmentMapper,
                                   CoverageDeclarationMapper coverageDeclarationMapper,
                                   PolicyMapper policyMapper,
                                   OutputContractMapper outputContractMapper,
                                   InputSlotSchemaMapper inputSlotSchemaMapper,
                                   EntityAliasMapper entityAliasMapper,
                                   PlanEvidenceRefMapper planEvidenceRefMapper,
                                   PlanPolicyRefMapper planPolicyRefMapper,
                                   PlanSchemaLinkMapper planSchemaLinkMapper,
                                   GraphAssetSupport graphAssetSupport,
                                   HashEmbeddingSupport hashEmbeddingSupport,
                                   GraphRuntimeProperties graphRuntimeProperties) {
        this.sceneMapper = sceneMapper;
        this.planMapper = planMapper;
        this.evidenceFragmentMapper = evidenceFragmentMapper;
        this.coverageDeclarationMapper = coverageDeclarationMapper;
        this.policyMapper = policyMapper;
        this.outputContractMapper = outputContractMapper;
        this.inputSlotSchemaMapper = inputSlotSchemaMapper;
        this.entityAliasMapper = entityAliasMapper;
        this.planEvidenceRefMapper = planEvidenceRefMapper;
        this.planPolicyRefMapper = planPolicyRefMapper;
        this.planSchemaLinkMapper = planSchemaLinkMapper;
        this.graphAssetSupport = graphAssetSupport;
        this.hashEmbeddingSupport = hashEmbeddingSupport;
        this.graphRuntimeProperties = graphRuntimeProperties;
    }

    public GraphQueryResultDTO query(GraphQueryCmd cmd) {
        String queryText = cmd.queryText() == null ? "" : cmd.queryText().trim();
        String mode = normalizeMode(cmd.mode());
        String normalizedQuery = graphAssetSupport.normalizeAlias(queryText);
        List<Double> queryVector = hashEmbeddingSupport.embed(queryText);

        Map<Long, ScenePO> sceneById = new HashMap<>();
        sceneMapper.findAll().forEach(scene -> sceneById.put(scene.getId(), scene));
        List<PlanPO> publishedPlans = planMapper.findAll().stream()
                .filter(plan -> "PUBLISHED".equalsIgnoreCase(plan.getStatus()))
                .filter(plan -> cmd.sceneId() == null || cmd.sceneId().equals(plan.getSceneId()))
                .filter(plan -> cmd.domainId() == null || sceneMatchesDomain(sceneById.get(plan.getSceneId()), cmd.domainId()))
                .sorted(Comparator.comparing(PlanPO::getUpdatedAt).reversed())
                .toList();

        Map<Long, List<PlanEvidenceRefPO>> evidenceRefsByPlan = new HashMap<>();
        planEvidenceRefMapper.findByPlanIdIn(publishedPlans.stream().map(PlanPO::getId).toList())
                .forEach(ref -> evidenceRefsByPlan.computeIfAbsent(ref.getPlanId(), key -> new ArrayList<>()).add(ref));
        Map<Long, EvidenceFragmentPO> evidenceById = new HashMap<>();
        evidenceFragmentMapper.findAll().forEach(evidence -> evidenceById.put(evidence.getId(), evidence));
        Map<Long, List<PlanPolicyRefPO>> policyRefsByPlan = new HashMap<>();
        planPolicyRefMapper.findByPlanIdIn(publishedPlans.stream().map(PlanPO::getId).toList())
                .forEach(ref -> policyRefsByPlan.computeIfAbsent(ref.getPlanId(), key -> new ArrayList<>()).add(ref));
        Map<Long, PolicyPO> policyById = new HashMap<>();
        policyMapper.findAll().forEach(policy -> policyById.put(policy.getId(), policy));

        List<EntityAliasPO> aliases = entityAliasMapper.findAll().stream()
                .filter(alias -> "PUBLISHED".equalsIgnoreCase(alias.getStatus()) || "ACTIVE".equalsIgnoreCase(alias.getStatus()))
                .toList();
        List<GraphEntityLinkDTO> entityLinks = aliases.stream()
                .map(alias -> {
                    double score = aliasScore(normalizedQuery, alias.getNormalizedText());
                    return score <= 0d ? null : new GraphEntityLinkDTO(alias.getAliasText(), alias.getAliasType(), alias.getSceneId(), alias.getPlanId(), round(score));
                })
                .filter(item -> item != null)
                .sorted(Comparator.comparing(GraphEntityLinkDTO::score).reversed())
                .limit(8)
                .toList();

        List<GraphPlanCandidateDTO> candidates = new ArrayList<>();
        for (PlanPO plan : publishedPlans) {
            ScenePO scene = sceneById.get(plan.getSceneId());
            List<PlanSchemaLinkPO> schemaLinks = loadSchemaLinks(plan.getId());
            List<CoverageDeclarationPO> coverages = coverageDeclarationMapper.findByPlanIdOrderByUpdatedAtDesc(plan.getId());
            List<EvidenceFragmentPO> evidences = resolveEvidence(plan.getId(), evidenceRefsByPlan, evidenceById);
            List<PolicyPO> policies = resolvePolicies(plan, policyRefsByPlan, policyById, sceneById);
            List<OutputContractPO> contracts = outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(plan.getSceneId());

            double entityScore = entityLinks.stream()
                    .filter(link -> plan.getId().equals(link.planId()) || plan.getSceneId().equals(link.sceneId()))
                    .mapToDouble(GraphEntityLinkDTO::score)
                    .max().orElse(0d);
            double schemaScore = scoreSchemaLinks(normalizedQuery, schemaLinks, mode);
            double pathScore = scorePath(queryText, scene, plan, mode);
            double evidenceScore = scoreEvidence(queryText, evidences);
            double vectorScore = graphRuntimeProperties.isVectorEnabled() ? scoreVector(queryVector, plan, evidences) : 0d;
            double finalScore = round(entityScore * 0.30d + schemaScore * 0.25d + pathScore * 0.20d + evidenceScore * 0.15d + vectorScore * 0.10d);

            GateDecision gateDecision = evaluateGate(plan, coverages, policies, contracts);
            GraphScoreBreakdownDTO breakdown = new GraphScoreBreakdownDTO(round(entityScore), round(schemaScore), round(pathScore), round(evidenceScore), round(vectorScore), finalScore);
            candidates.add(new GraphPlanCandidateDTO(
                    plan.getSceneId(),
                    scene == null ? null : scene.getSceneCode(),
                    scene == null ? null : scene.getSceneTitle(),
                    plan.getId(),
                    plan.getPlanCode(),
                    plan.getPlanName(),
                    gateDecision.gateStatus(),
                    gateDecision.decision(),
                    graphAssetSupport.parseStringList(plan.getSourceTablesJson()),
                    evidences.stream().map(EvidenceFragmentPO::getTitle).limit(3).toList(),
                    breakdown
            ));
        }

        candidates = candidates.stream()
                .sorted(Comparator
                        .comparing((GraphPlanCandidateDTO item) -> gateRank(item.decision()))
                        .thenComparing(item -> item.breakdown().finalScore(), Comparator.reverseOrder()))
                .limit(6)
                .toList();

        List<GraphSchemaLinkDTO> schemaLinkDTOs = candidates.stream()
                .flatMap(candidate -> loadSchemaLinks(candidate.planId()).stream()
                        .map(link -> new GraphSchemaLinkDTO(link.getPlanId(), link.getTableName(), link.getColumnName(), link.getLinkRole(), round(schemaMatch(normalizedQuery, link)))))
                .filter(link -> link.score() > 0d)
                .limit(12)
                .toList();

        List<String> slotResolutions = resolveSlots(cmd, candidates, sceneById, queryText);
        List<String> outputContracts = candidates.stream()
                .map(GraphPlanCandidateDTO::sceneId)
                .distinct()
                .flatMap(sceneId -> outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).stream())
                .map(contract -> contract.getContractName() + (contract.getSummaryText() == null || contract.getSummaryText().isBlank() ? "" : "：" + contract.getSummaryText()))
                .limit(4)
                .toList();
        List<String> reasons = buildReasons(candidates, entityLinks, schemaLinkDTOs);
        GraphPathGraphDTO pathGraph = buildPathGraph(candidates, evidenceById, evidenceRefsByPlan);

        GraphPlanCandidateDTO top = candidates.isEmpty() ? null : candidates.get(0);
        String decision = top == null ? "deny" : top.decision();
        String riskLevel = top == null ? "HIGH" : toRiskLevel(top.decision());
        return new GraphQueryResultDTO(mode, decision, riskLevel, reasons, slotResolutions, outputContracts, entityLinks, schemaLinkDTOs, candidates, pathGraph);
    }

    private List<PlanSchemaLinkPO> loadSchemaLinks(Long planId) {
        List<PlanSchemaLinkPO> links = new ArrayList<>();
        links.addAll(planSchemaLinkMapper.findByPlanIdAndStatus(planId, "PUBLISHED"));
        links.addAll(planSchemaLinkMapper.findByPlanIdAndStatus(planId, "ACTIVE"));
        return links;
    }

    private List<EvidenceFragmentPO> resolveEvidence(Long planId,
                                                     Map<Long, List<PlanEvidenceRefPO>> evidenceRefsByPlan,
                                                     Map<Long, EvidenceFragmentPO> evidenceById) {
        return evidenceRefsByPlan.getOrDefault(planId, List.of()).stream()
                .map(ref -> evidenceById.get(ref.getEvidenceId()))
                .filter(item -> item != null)
                .toList();
    }

    private List<PolicyPO> resolvePolicies(PlanPO plan,
                                           Map<Long, List<PlanPolicyRefPO>> policyRefsByPlan,
                                           Map<Long, PolicyPO> policyById,
                                           Map<Long, ScenePO> sceneById) {
        List<PolicyPO> policies = new ArrayList<>();
        for (PlanPolicyRefPO ref : policyRefsByPlan.getOrDefault(plan.getId(), List.of())) {
            PolicyPO policy = policyById.get(ref.getPolicyId());
            if (policy != null) {
                policies.add(policy);
            }
        }
        ScenePO scene = sceneById.get(plan.getSceneId());
        policyById.values().stream()
                .filter(policy -> "SCENE".equalsIgnoreCase(policy.getScopeType()) && plan.getSceneId().equals(policy.getScopeRefId()))
                .forEach(policies::add);
        if (scene != null) {
            policyById.values().stream()
                    .filter(policy -> "DOMAIN".equalsIgnoreCase(policy.getScopeType()) && scene.getDomainId() != null && scene.getDomainId().equals(policy.getScopeRefId()))
                    .forEach(policies::add);
        }
        policyById.values().stream().filter(policy -> "GLOBAL".equalsIgnoreCase(policy.getScopeType())).forEach(policies::add);
        return policies;
    }

    private double aliasScore(String query, String alias) {
        if (query == null || query.isBlank() || alias == null || alias.isBlank()) {
            return 0d;
        }
        if (query.equals(alias)) {
            return 1d;
        }
        if (query.contains(alias) || alias.contains(query)) {
            return 0.78d;
        }
        int overlap = 0;
        for (String token : query.split(" ")) {
            if (!token.isBlank() && alias.contains(token)) {
                overlap += 1;
            }
        }
        return overlap <= 0 ? 0d : Math.min(0.72d, overlap * 0.18d);
    }

    private double scoreSchemaLinks(String normalizedQuery, List<PlanSchemaLinkPO> links, String mode) {
        double score = links.stream().mapToDouble(link -> schemaMatch(normalizedQuery, link)).max().orElse(0d);
        if ("LOCAL".equals(mode)) {
            score = Math.min(1d, score + 0.08d);
        }
        return score;
    }

    private double schemaMatch(String normalizedQuery, PlanSchemaLinkPO link) {
        String table = graphAssetSupport.normalizeAlias(link.getTableName());
        String column = graphAssetSupport.normalizeAlias(link.getColumnName());
        if (!table.isBlank() && normalizedQuery.contains(table)) {
            return 0.88d;
        }
        if (!column.isBlank() && normalizedQuery.contains(column)) {
            return 0.76d;
        }
        return 0d;
    }

    private double scorePath(String queryText, ScenePO scene, PlanPO plan, String mode) {
        String corpus = String.join(" ", List.of(
                safe(scene == null ? null : scene.getSceneTitle()),
                safe(scene == null ? null : scene.getSceneDescription()),
                safe(plan.getPlanName()),
                safe(plan.getRetrievalText()),
                safe(plan.getApplicablePeriod())
        )).toLowerCase(Locale.ROOT);
        double score = textOverlapScore(queryText, corpus);
        if ("GLOBAL".equals(mode)) {
            score = Math.min(1d, score + 0.05d);
        }
        return score;
    }

    private double scoreEvidence(String queryText, List<EvidenceFragmentPO> evidences) {
        return evidences.stream()
                .mapToDouble(evidence -> textOverlapScore(queryText, evidence.getTitle() + " " + safe(evidence.getFragmentText())))
                .max().orElse(0d);
    }

    private double scoreVector(List<Double> queryVector, PlanPO plan, List<EvidenceFragmentPO> evidences) {
        List<Double> planVector = hashEmbeddingSupport.embed(plan.getRetrievalText());
        double planScore = hashEmbeddingSupport.cosine(queryVector, planVector);
        double evidenceScore = evidences.stream()
                .mapToDouble(evidence -> hashEmbeddingSupport.cosine(queryVector, hashEmbeddingSupport.embed(evidence.getTitle() + " " + safe(evidence.getFragmentText()))))
                .max().orElse(0d);
        return Math.max(planScore, evidenceScore);
    }

    private double textOverlapScore(String queryText, String corpus) {
        String normalizedQuery = queryText == null ? "" : queryText.toLowerCase(Locale.ROOT);
        if (normalizedQuery.isBlank() || corpus == null || corpus.isBlank()) {
            return 0d;
        }
        double score = 0d;
        for (String token : normalizedQuery.split("[^\\p{IsAlphabetic}\\p{IsDigit}_]+")) {
            if (token == null || token.isBlank()) {
                continue;
            }
            if (corpus.contains(token)) {
                score += 0.16d;
            }
        }
        if (corpus.contains(normalizedQuery)) {
            score += 0.24d;
        }
        return Math.min(1d, score);
    }

    private GateDecision evaluateGate(PlanPO plan,
                                      List<CoverageDeclarationPO> coverages,
                                      List<PolicyPO> policies,
                                      List<OutputContractPO> outputContracts) {
        if (outputContracts.isEmpty()) {
            return new GateDecision("HARD_DENY", "deny");
        }
        String coverageStatus = coverages.stream().map(CoverageDeclarationPO::getCoverageStatus).findFirst().orElse("GAP");
        if ("GAP".equalsIgnoreCase(coverageStatus)) {
            return new GateDecision("HARD_DENY", "deny");
        }
        boolean hasDeny = policies.stream().anyMatch(policy -> "DENY".equalsIgnoreCase(policy.getEffectType()) || "S4".equalsIgnoreCase(policy.getSensitivityLevel()));
        if (hasDeny) {
            return new GateDecision("HARD_DENY", "deny");
        }
        boolean hasApproval = policies.stream().anyMatch(policy -> "REQUIRE_APPROVAL".equalsIgnoreCase(policy.getEffectType()) || "S3".equalsIgnoreCase(policy.getSensitivityLevel()));
        if (hasApproval || "PARTIAL".equalsIgnoreCase(coverageStatus)) {
            return new GateDecision("SOFT_REVIEW", "need_approval");
        }
        return new GateDecision("PASS", "allow");
    }

    private List<String> resolveSlots(GraphQueryCmd cmd,
                                      List<GraphPlanCandidateDTO> candidates,
                                      Map<Long, ScenePO> sceneById,
                                      String queryText) {
        Set<String> values = new LinkedHashSet<>();
        if (cmd.slotHintsJson() != null && !cmd.slotHintsJson().isBlank()) {
            graphAssetSupport.parseStringList(cmd.slotHintsJson()).forEach(values::add);
        }
        for (GraphPlanCandidateDTO candidate : candidates) {
            for (InputSlotSchemaPO slot : inputSlotSchemaMapper.findBySceneIdOrderByUpdatedAtDesc(candidate.sceneId())) {
                if (queryText.contains(slot.getSlotName()) || graphAssetSupport.parseStringList(slot.getIdentifierCandidatesJson()).stream().anyMatch(queryText::contains)) {
                    values.add(slot.getSlotName() + "=" + slot.getSlotType());
                }
            }
            ScenePO scene = sceneById.get(candidate.sceneId());
            if (scene != null && values.size() >= 4) {
                break;
            }
        }
        return values.stream().limit(6).toList();
    }

    private List<String> buildReasons(List<GraphPlanCandidateDTO> candidates,
                                      List<GraphEntityLinkDTO> entityLinks,
                                      List<GraphSchemaLinkDTO> schemaLinks) {
        List<String> reasons = new ArrayList<>();
        if (!entityLinks.isEmpty()) {
            reasons.add("命中实体别名：" + entityLinks.get(0).aliasText());
        }
        if (!schemaLinks.isEmpty()) {
            reasons.add("命中模式链接：" + schemaLinks.get(0).tableName() + (schemaLinks.get(0).columnName() == null ? "" : "." + schemaLinks.get(0).columnName()));
        }
        if (!candidates.isEmpty()) {
            reasons.add("候选方案：" + candidates.get(0).planName() + "（" + candidates.get(0).decision() + "）");
        }
        if (reasons.isEmpty()) {
            reasons.add("未命中已发布资产");
        }
        return reasons;
    }

    private GraphPathGraphDTO buildPathGraph(List<GraphPlanCandidateDTO> candidates,
                                             Map<Long, EvidenceFragmentPO> evidenceById,
                                             Map<Long, List<PlanEvidenceRefPO>> evidenceRefsByPlan) {
        if (candidates.isEmpty()) {
            return new GraphPathGraphDTO(List.of(), List.of());
        }
        GraphPlanCandidateDTO top = candidates.get(0);
        List<GraphPathNodeDTO> nodes = new ArrayList<>();
        List<GraphPathEdgeDTO> edges = new ArrayList<>();
        nodes.add(new GraphPathNodeDTO("scene:" + top.sceneId(), top.sceneTitle(), "SCENE"));
        nodes.add(new GraphPathNodeDTO("plan:" + top.planId(), top.planName(), "PLAN"));
        edges.add(new GraphPathEdgeDTO("scene:" + top.sceneId(), "plan:" + top.planId(), "HAS_PLAN"));
        for (PlanEvidenceRefPO ref : evidenceRefsByPlan.getOrDefault(top.planId(), List.of())) {
            EvidenceFragmentPO evidence = evidenceById.get(ref.getEvidenceId());
            if (evidence == null) {
                continue;
            }
            String evidenceNodeId = "evidence:" + evidence.getId();
            nodes.add(new GraphPathNodeDTO(evidenceNodeId, evidence.getTitle(), "EVIDENCE"));
            edges.add(new GraphPathEdgeDTO("plan:" + top.planId(), evidenceNodeId, "BACKED_BY"));
        }
        return new GraphPathGraphDTO(nodes, edges);
    }

    private int gateRank(String decision) {
        if ("allow".equalsIgnoreCase(decision)) {
            return 0;
        }
        if ("need_approval".equalsIgnoreCase(decision)) {
            return 1;
        }
        return 2;
    }

    private boolean sceneMatchesDomain(ScenePO scene, Long domainId) {
        return scene != null && domainId != null && domainId.equals(scene.getDomainId());
    }

    private String normalizeMode(String mode) {
        String value = mode == null || mode.isBlank() ? "HYBRID" : mode.trim().toUpperCase(Locale.ROOT);
        if (!List.of("LOCAL", "GLOBAL", "HYBRID").contains(value)) {
            return "HYBRID";
        }
        return value;
    }

    private String toRiskLevel(String decision) {
        if ("allow".equalsIgnoreCase(decision)) {
            return "LOW";
        }
        if ("need_approval".equalsIgnoreCase(decision)) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    private double round(double value) {
        return Math.round(value * 1000d) / 1000d;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record GateDecision(String gateStatus, String decision) {
    }
}
