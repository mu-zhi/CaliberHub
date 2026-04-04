package com.cmbchina.datadirect.caliber.application.service.query.datamap;

import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphNodeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphResponseDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapNodeDetailDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.ProjectionVerificationStatus;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.ReadSource;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.ContractViewDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.CoverageDeclarationDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.EvidenceFragmentDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.OutputContractDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.PlanDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.PolicyDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.SourceContractDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.SourceIntakeContractDTO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityRelationPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalSnapshotMembershipPO;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DataMapGraphDtoAdapter {

    public DataMapGraphResponseDTO buildGraph(String rootRef,
                                              GraphSceneBundle bundle,
                                              DataMapGraphQueryOptions options) {
        Map<String, DataMapGraphNodeDTO> nodeMap = new LinkedHashMap<>();
        Map<String, DataMapGraphEdgeDTO> edgeMap = new LinkedHashMap<>();

        SceneDTO scene = bundle.scene();
        String sceneRef = assetRef("scene", scene.id());
        String domainRef = null;
        if (scene.domainId() != null && scene.domainId() > 0) {
            domainRef = assetRef("domain", scene.domainId());
            nodeMap.put(domainRef, domainNode(scene));
            addEdge(edgeMap, edge(domainRef, "BELONGS_TO_DOMAIN", sceneRef, null, null, null, false, null, Map.of("domainId", scene.domainId())));
        }
        nodeMap.put(sceneRef, sceneNode(scene, bundle.evidences().size()));

        Map<Long, OutputContractDTO> contractsById = bundle.outputContracts().stream().collect(Collectors.toMap(OutputContractDTO::id, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, SourceIntakeContractDTO> intakeById = bundle.sourceIntakeContracts().stream().collect(Collectors.toMap(SourceIntakeContractDTO::id, item -> item, (left, right) -> left, LinkedHashMap::new));

        for (PlanDTO plan : bundle.plans()) {
            String planRef = assetRef("plan", plan.id());
            nodeMap.put(planRef, planNode(scene, plan));
            addEdge(edgeMap, edge(sceneRef, "USES_PLAN", planRef, round(plan.confidenceScore()), traceRef(plan.planCode(), "USES_PLAN"), plan.planCode(), false, plan.applicablePeriod(), Map.of("defaultTimeSemantic", safeText(plan.defaultTimeSemantic()))));

            String pathRef = null;
            if (!bundle.sourceContracts().stream().filter(item -> Objects.equals(item.planId(), plan.id())).toList().isEmpty()
                    || !safeText(plan.sourceTablesJson()).isBlank()
                    || !safeText(plan.sqlText()).isBlank()) {
                pathRef = assetRef("path-template", plan.id());
                nodeMap.put(pathRef, pathTemplateNode(scene, plan, bundle.sourceContracts()));
                addEdge(edgeMap, edge(planRef, "RESOLVES_TO_PATH", pathRef, round(plan.confidenceScore()), traceRef(plan.planCode(), "RESOLVES_TO_PATH"), plan.planCode(), false, null, Map.of("pathScope", "PLAN")));
            }

            for (OutputContractDTO contract : bundle.outputContracts()) {
                String contractRef = assetRef("output-contract", contract.id());
                nodeMap.put(contractRef, outputContractNode(scene, contract));
                addEdge(edgeMap, edge(planRef, "REQUIRES_CONTRACT", contractRef, null, traceRef(contract.contractCode(), "REQUIRES_CONTRACT"), contract.contractCode(), false, contract.timeCaliberNote(), Map.of()));
            }

            for (CoverageDeclarationDTO coverage : bundle.coverages().stream().filter(item -> Objects.equals(item.planId(), plan.id())).toList()) {
                String coverageRef = assetRef("coverage-declaration", coverage.id());
                nodeMap.put(coverageRef, coverageNode(scene, coverage));
                addEdge(edgeMap, edge(planRef, "HAS_COVERAGE", coverageRef, null, traceRef(coverage.coverageCode(), "HAS_COVERAGE"), coverage.coverageCode(), false, coverage.statementText(), Map.of("coverageStatus", safeText(coverage.coverageStatus()))));
            }

            for (Long evidenceId : plan.evidenceIds()) {
                bundle.evidences().stream()
                        .filter(item -> Objects.equals(item.id(), evidenceId))
                        .findFirst()
                        .ifPresent(evidence -> {
                            String evidenceRef = assetRef("evidence-fragment", evidence.id());
                            nodeMap.put(evidenceRef, evidenceNode(scene, evidence));
                            addEdge(edgeMap, edge(planRef, "SUPPORTED_BY_EVIDENCE", evidenceRef, round(evidence.confidenceScore()), traceRef(evidence.evidenceCode(), "SUPPORTED_BY_EVIDENCE"), evidence.sourceRef(), false, evidence.sourceAnchor(), Map.of("sourceType", safeText(evidence.sourceType()))));
                        });
            }

            for (Long policyId : plan.policyIds()) {
                bundle.policies().stream()
                        .filter(item -> Objects.equals(item.id(), policyId))
                        .findFirst()
                        .ifPresent(policy -> {
                            String policyRef = assetRef("policy", policy.id());
                            nodeMap.put(policyRef, policyNode(scene, policy));
                            addEdge(edgeMap, edge(planRef, "GOVERNED_BY_POLICY", policyRef, null, traceRef(policy.policyCode(), "GOVERNED_BY_POLICY"), policy.policyCode(), true, policy.conditionText(), Map.of("effectType", safeText(policy.effectType()))));
                        });
            }

            for (ContractViewDTO view : bundle.contractViews().stream().filter(item -> Objects.equals(item.planId(), plan.id())).toList()) {
                String viewRef = assetRef("contract-view", view.id());
                nodeMap.put(viewRef, contractViewNode(scene, view));
                addEdge(edgeMap, edge(planRef, "USES_PLAN", viewRef, null, traceRef(view.viewCode(), "USES_PLAN"), view.viewCode(), false, view.roleScope(), Map.of("roleScope", safeText(view.roleScope()))));
                if (view.outputContractId() != null && contractsById.containsKey(view.outputContractId())) {
                    String contractRef = assetRef("output-contract", view.outputContractId());
                    nodeMap.put(contractRef, outputContractNode(scene, contractsById.get(view.outputContractId())));
                    addEdge(edgeMap, edge(contractRef, "DERIVED_FROM", viewRef, null, traceRef(view.viewCode(), "DERIVED_FROM"), view.viewCode(), false, view.approvalTemplate(), Map.of("approvalTemplate", safeText(view.approvalTemplate()))));
                }
            }

            List<SourceContractDTO> sourceContracts = bundle.sourceContracts().stream()
                    .filter(item -> Objects.equals(item.planId(), plan.id()))
                    .toList();
            for (SourceContractDTO sourceContract : sourceContracts) {
                String sourceContractRef = assetRef("source-contract", sourceContract.id());
                nodeMap.put(sourceContractRef, sourceContractNode(scene, sourceContract));
                if (pathRef != null) {
                    addEdge(edgeMap, edge(pathRef, "MAPS_TO_TABLE", sourceContractRef, null, traceRef(sourceContract.sourceContractCode(), "MAPS_TO_TABLE"), sourceContract.physicalTable(), false, sourceContract.notes(), Map.of("physicalTable", safeText(sourceContract.physicalTable()))));
                } else {
                    addEdge(edgeMap, edge(planRef, "MAPS_TO_TABLE", sourceContractRef, null, traceRef(sourceContract.sourceContractCode(), "MAPS_TO_TABLE"), sourceContract.physicalTable(), false, sourceContract.notes(), Map.of("physicalTable", safeText(sourceContract.physicalTable()))));
                }
                if (sourceContract.intakeContractId() != null && intakeById.containsKey(sourceContract.intakeContractId())) {
                    SourceIntakeContractDTO intake = intakeById.get(sourceContract.intakeContractId());
                    String intakeRef = assetRef("source-intake-contract", intake.id());
                    nodeMap.put(intakeRef, sourceIntakeNode(scene, intake));
                    addEdge(edgeMap, edge(intakeRef, "DERIVED_FROM", sourceContractRef, null, traceRef(intake.intakeCode(), "DERIVED_FROM"), intake.intakeCode(), false, intake.materialSourceNote(), Map.of("sourceType", safeText(intake.sourceType()))));
                }
            }
        }

        for (PolicyDTO policy : bundle.policies()) {
            String policyRef = assetRef("policy", policy.id());
            nodeMap.putIfAbsent(policyRef, policyNode(scene, policy));
            if (policy.planIds().isEmpty()) {
                addEdge(edgeMap, edge(sceneRef, "GOVERNED_BY_POLICY", policyRef, null, traceRef(policy.policyCode(), "GOVERNED_BY_POLICY"), policy.policyCode(), true, policy.conditionText(), Map.of("scopeType", safeText(policy.scopeType()))));
            }
        }

        for (EvidenceFragmentDTO evidence : bundle.evidences()) {
            String evidenceRef = assetRef("evidence-fragment", evidence.id());
            nodeMap.putIfAbsent(evidenceRef, evidenceNode(scene, evidence));
            if (evidence.planIds().isEmpty()) {
                addEdge(edgeMap, edge(sceneRef, "SUPPORTED_BY_EVIDENCE", evidenceRef, round(evidence.confidenceScore()), traceRef(evidence.evidenceCode(), "SUPPORTED_BY_EVIDENCE"), evidence.sourceRef(), false, evidence.sourceAnchor(), Map.of("sourceType", safeText(evidence.sourceType()))));
            }
        }

        for (SourceIntakeContractDTO intake : bundle.sourceIntakeContracts()) {
            String intakeRef = assetRef("source-intake-contract", intake.id());
            nodeMap.putIfAbsent(intakeRef, sourceIntakeNode(scene, intake));
            addEdge(edgeMap, edge(sceneRef, "DERIVED_FROM", intakeRef, null, traceRef(intake.intakeCode(), "DERIVED_FROM"), intake.intakeCode(), false, intake.materialSourceNote(), Map.of("sourceType", safeText(intake.sourceType()))));
        }

        appendDomainMembershipEdges(rootRef, sceneRef, nodeMap, edgeMap);
        appendInstanceOfEdges(rootRef, bundle, nodeMap, edgeMap, options.snapshotId());
        appendCanonicalRelationEdges(rootRef, bundle, nodeMap, edgeMap, options.snapshotId());
        appendSnapshotNodes(nodeMap, edgeMap, List.copyOf(nodeMap.values()));
        return filterGraph(rootRef, scene.id(), scene.sceneTitle(), nodeMap, edgeMap, options);
    }

    public DataMapNodeDetailDTO buildNodeDetail(String assetRef, GraphSceneBundle bundle) {
        DataMapGraphResponseDTO graph = buildGraph(assetRef, bundle, DataMapGraphQueryOptions.of(null, null, null, null, null));
        DataMapGraphNodeDTO node = graph.nodes().stream()
                .filter(item -> assetRef.equals(item.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("asset not found in graph: " + assetRef));
        Map<String, Object> attributes = buildAttributes(assetRef, bundle, node);
        return new DataMapNodeDetailDTO(assetRef, node, attributes);
    }

    private DataMapGraphResponseDTO filterGraph(String rootRef,
                                                Long sceneId,
                                                String sceneName,
                                                Map<String, DataMapGraphNodeDTO> nodeMap,
                                                Map<String, DataMapGraphEdgeDTO> edgeMap,
                                                DataMapGraphQueryOptions options) {
        String rootNodeId = rootRef == null || rootRef.isBlank() ? assetRef("scene", sceneId) : rootRef;
        Set<String> allowedNodeIds = nodeMap.values().stream()
                .filter(node -> matchesNode(node, options))
                .map(DataMapGraphNodeDTO::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        allowedNodeIds.add(assetRef("scene", sceneId));
        if (nodeMap.containsKey(rootNodeId)) {
            allowedNodeIds.add(rootNodeId);
        }

        List<DataMapGraphEdgeDTO> edges = edgeMap.values().stream()
                .filter(edge -> allowedNodeIds.contains(edge.source()) && allowedNodeIds.contains(edge.target()))
                .filter(edge -> matchesEdge(edge, options))
                .toList();

        Set<String> connectedNodeIds = new LinkedHashSet<>();
        edges.forEach(edge -> {
            connectedNodeIds.add(edge.source());
            connectedNodeIds.add(edge.target());
        });
        connectedNodeIds.add(assetRef("scene", sceneId));
        connectedNodeIds.add(rootNodeId);

        List<DataMapGraphNodeDTO> nodes = nodeMap.values().stream()
                .filter(node -> allowedNodeIds.contains(node.id()))
                .filter(node -> connectedNodeIds.contains(node.id()) || "DOMAIN".equalsIgnoreCase(node.objectType()))
                .filter(node -> !"DOMAIN".equalsIgnoreCase(node.objectType()) || edges.stream().anyMatch(edge -> edge.source().equals(node.id()) || edge.target().equals(node.id())))
                .toList();

        return new DataMapGraphResponseDTO(rootNodeId, sceneId, sceneName, null,
                ReadSource.RELATIONAL, ProjectionVerificationStatus.SKIPPED, null,
                nodes, edges);
    }

    private boolean matchesNode(DataMapGraphNodeDTO node, DataMapGraphQueryOptions options) {
        if (node == null) {
            return false;
        }
        if (!options.objectTypes().isEmpty() && !options.objectTypes().contains(node.objectType().toUpperCase(Locale.ROOT))) {
            return false;
        }
        if (!options.statuses().isEmpty()) {
            String status = safeText(node.status()).toUpperCase(Locale.ROOT);
            if (status.isBlank() || !options.statuses().contains(status)) {
                return false;
            }
        }
        if (options.snapshotId() != null && node.snapshotId() != null && !Objects.equals(options.snapshotId(), node.snapshotId())) {
            return false;
        }
        if (options.snapshotId() != null && node.snapshotId() == null && !Set.of("SCENE", "DOMAIN", "VERSION_SNAPSHOT").contains(node.objectType())) {
            return false;
        }
        if (!options.sensitivityScopes().isEmpty()) {
            String scope = safeText(node.sensitivityScope()).toUpperCase(Locale.ROOT);
            if (scope.isBlank() || !options.sensitivityScopes().contains(scope)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesEdge(DataMapGraphEdgeDTO edge, DataMapGraphQueryOptions options) {
        if (edge == null) {
            return false;
        }
        if (!options.relationTypes().isEmpty()) {
            String relationType = safeText(edge.relationType()).toUpperCase(Locale.ROOT);
            return !relationType.isBlank() && options.relationTypes().contains(relationType);
        }
        return true;
    }

    private Map<String, Object> buildAttributes(String assetRef,
                                                GraphSceneBundle bundle,
                                                DataMapGraphNodeDTO node) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        putAttribute(attributes, "object_type", node.objectType());
        putAttribute(attributes, "object_code", node.objectCode());
        putAttribute(attributes, "object_name", node.objectName());
        putAttribute(attributes, "status", node.status());
        putAttribute(attributes, "snapshot_id", node.snapshotId());
        putAttribute(attributes, "domain_code", node.domainCode());
        putAttribute(attributes, "owner", node.owner());
        putAttribute(attributes, "sensitivity_scope", node.sensitivityScope());
        putAttribute(attributes, "time_semantic", node.timeSemantic());
        putAttribute(attributes, "evidence_count", node.evidenceCount());
        putAttribute(attributes, "last_reviewed_at", node.lastReviewedAt() == null ? null : node.lastReviewedAt().toString());

        ResolvedAssetRef resolved = parseAssetRef(assetRef);
        switch (resolved.objectType()) {
            case "SCENE" -> {
                SceneDTO scene = bundle.scene();
                putAttribute(attributes, "scene_type", scene.sceneType());
                putAttribute(attributes, "scene_description", scene.sceneDescription());
                putAttribute(attributes, "applicability", scene.applicability());
                putAttribute(attributes, "boundaries", scene.boundaries());
                putAttribute(attributes, "published_by", scene.publishedBy());
                putAttribute(attributes, "published_at", scene.publishedAt() == null ? null : scene.publishedAt().toString());
            }
            case "PLAN" -> bundle.plans().stream().filter(item -> Objects.equals(item.id(), resolved.numericId())).findFirst().ifPresent(plan -> {
                putAttribute(attributes, "applicable_period", plan.applicablePeriod());
                putAttribute(attributes, "default_time_semantic", plan.defaultTimeSemantic());
                putAttribute(attributes, "confidence", round(plan.confidenceScore()));
                putAttribute(attributes, "source_tables_json", plan.sourceTablesJson());
                putAttribute(attributes, "notes", plan.notes());
            });
            case "OUTPUT_CONTRACT" -> bundle.outputContracts().stream().filter(item -> Objects.equals(item.id(), resolved.numericId())).findFirst().ifPresent(contract -> {
                putAttribute(attributes, "summary_text", contract.summaryText());
                putAttribute(attributes, "time_caliber_note", contract.timeCaliberNote());
                putAttribute(attributes, "usage_constraints", contract.usageConstraints());
                putAttribute(attributes, "fields_json", contract.fieldsJson());
            });
            case "CONTRACT_VIEW" -> bundle.contractViews().stream().filter(item -> Objects.equals(item.id(), resolved.numericId())).findFirst().ifPresent(view -> {
                putAttribute(attributes, "role_scope", view.roleScope());
                putAttribute(attributes, "approval_template", view.approvalTemplate());
                putAttribute(attributes, "visible_fields_json", view.visibleFieldsJson());
                putAttribute(attributes, "masked_fields_json", view.maskedFieldsJson());
                putAttribute(attributes, "restricted_fields_json", view.restrictedFieldsJson());
                putAttribute(attributes, "forbidden_fields_json", view.forbiddenFieldsJson());
            });
            case "COVERAGE_DECLARATION" -> bundle.coverages().stream().filter(item -> Objects.equals(item.id(), resolved.numericId())).findFirst().ifPresent(coverage -> {
                putAttribute(attributes, "coverage_type", coverage.coverageType());
                putAttribute(attributes, "coverage_status", coverage.coverageStatus());
                putAttribute(attributes, "statement_text", coverage.statementText());
                putAttribute(attributes, "applicable_period", coverage.applicablePeriod());
                putAttribute(attributes, "source_system", coverage.sourceSystem());
                putAttribute(attributes, "gap_text", coverage.gapText());
            });
            case "POLICY" -> bundle.policies().stream().filter(item -> Objects.equals(item.id(), resolved.numericId())).findFirst().ifPresent(policy -> {
                putAttribute(attributes, "scope_type", policy.scopeType());
                putAttribute(attributes, "scope_ref_id", policy.scopeRefId());
                putAttribute(attributes, "effect_type", policy.effectType());
                putAttribute(attributes, "condition_text", policy.conditionText());
                putAttribute(attributes, "source_type", policy.sourceType());
                putAttribute(attributes, "masking_rule", policy.maskingRule());
            });
            case "EVIDENCE_FRAGMENT" -> bundle.evidences().stream().filter(item -> Objects.equals(item.id(), resolved.numericId())).findFirst().ifPresent(evidence -> {
                putAttribute(attributes, "source_anchor", evidence.sourceAnchor());
                putAttribute(attributes, "source_type", evidence.sourceType());
                putAttribute(attributes, "source_ref", evidence.sourceRef());
                putAttribute(attributes, "confidence", round(evidence.confidenceScore()));
                putAttribute(attributes, "fragment_text", evidence.fragmentText());
            });
            case "SOURCE_CONTRACT" -> bundle.sourceContracts().stream().filter(item -> Objects.equals(item.id(), resolved.numericId())).findFirst().ifPresent(contract -> {
                putAttribute(attributes, "physical_table", contract.physicalTable());
                putAttribute(attributes, "source_role", contract.sourceRole());
                putAttribute(attributes, "identifier_type", contract.identifierType());
                putAttribute(attributes, "output_identifier_type", contract.outputIdentifierType());
                putAttribute(attributes, "source_system", contract.sourceSystem());
                putAttribute(attributes, "completeness_level", contract.completenessLevel());
                putAttribute(attributes, "effective_from", contract.startDate() == null ? null : contract.startDate().toString());
                putAttribute(attributes, "effective_to", contract.endDate() == null ? null : contract.endDate().toString());
                putAttribute(attributes, "material_source_note", contract.materialSourceNote());
            });
            case "SOURCE_INTAKE_CONTRACT" -> bundle.sourceIntakeContracts().stream().filter(item -> Objects.equals(item.id(), resolved.numericId())).findFirst().ifPresent(contract -> {
                putAttribute(attributes, "source_type", contract.sourceType());
                putAttribute(attributes, "required_fields_json", contract.requiredFieldsJson());
                putAttribute(attributes, "completeness_rule", contract.completenessRule());
                putAttribute(attributes, "gap_task_hint", contract.gapTaskHint());
                putAttribute(attributes, "known_coverage_json", contract.knownCoverageJson());
                putAttribute(attributes, "material_source_note", contract.materialSourceNote());
            });
            case "PATH_TEMPLATE" -> bundle.plans().stream().filter(item -> Objects.equals(item.id(), resolved.numericId())).findFirst().ifPresent(plan -> {
                putAttribute(attributes, "plan_code", plan.planCode());
                putAttribute(attributes, "plan_name", plan.planName());
                putAttribute(attributes, "source_contract_codes", bundle.sourceContracts().stream()
                        .filter(item -> Objects.equals(item.planId(), plan.id()))
                        .map(SourceContractDTO::sourceContractCode)
                        .toList());
                putAttribute(attributes, "source_tables_json", plan.sourceTablesJson());
            });
            case "VERSION_SNAPSHOT" -> {
                List<DataMapGraphNodeDTO> snapshotAssets = buildGraph(assetRef, bundle, DataMapGraphQueryOptions.of(null, null, null, null, null)).nodes().stream()
                        .filter(item -> Objects.equals(item.snapshotId(), resolved.numericId()))
                        .toList();
                putAttribute(attributes, "snapshot_asset_count", snapshotAssets.size());
                putAttribute(attributes, "snapshot_asset_types", snapshotAssets.stream().map(DataMapGraphNodeDTO::objectType).distinct().toList());
            }
            case "DOMAIN" -> {
                putAttribute(attributes, "domain_id", bundle.scene().domainId());
                putAttribute(attributes, "domain_name", bundle.scene().domainName());
                putAttribute(attributes, "scene_count", 1);
            }
            default -> {
            }
        }
        return attributes;
    }

    private void appendSnapshotNodes(Map<String, DataMapGraphNodeDTO> nodeMap,
                                     Map<String, DataMapGraphEdgeDTO> edgeMap,
                                     List<DataMapGraphNodeDTO> currentNodes) {
        Map<Long, DataMapGraphNodeDTO> snapshotNodes = new LinkedHashMap<>();
        for (DataMapGraphNodeDTO node : currentNodes) {
            if (node.snapshotId() == null || "VERSION_SNAPSHOT".equalsIgnoreCase(node.objectType())) {
                continue;
            }
            DataMapGraphNodeDTO snapshotNode = snapshotNodes.computeIfAbsent(node.snapshotId(),
                    snapshotId -> versionSnapshotNode(snapshotId, currentNodes));
            nodeMap.put(snapshotNode.id(), snapshotNode);
            addEdge(edgeMap, edge(node.id(), "PUBLISHED_IN_SNAPSHOT", snapshotNode.id(), null, traceRef(snapshotNode.objectCode(), "PUBLISHED_IN_SNAPSHOT"), snapshotNode.objectCode(), false, null, Map.of()));
        }
    }

    private void appendDomainMembershipEdges(String rootRef,
                                             String sceneRef,
                                             Map<String, DataMapGraphNodeDTO> nodeMap,
                                             Map<String, DataMapGraphEdgeDTO> edgeMap) {
        if (rootRef == null || !rootRef.startsWith("domain:")) {
            return;
        }
        for (DataMapGraphNodeDTO node : nodeMap.values()) {
            if (node == null) {
                continue;
            }
            if (node.id().equals(sceneRef) || "DOMAIN".equalsIgnoreCase(node.objectType()) || "VERSION_SNAPSHOT".equalsIgnoreCase(node.objectType())) {
                continue;
            }
            addEdge(edgeMap, edge(
                    sceneRef,
                    "SCENE_MEMBERSHIP",
                    node.id(),
                    null,
                    traceRef(node.objectCode(), "SCENE_MEMBERSHIP"),
                    node.id(),
                    false,
                    null,
                    Map.of("relationGroup", "control")));
        }
    }

    private void appendInstanceOfEdges(String rootRef,
                                       GraphSceneBundle bundle,
                                       Map<String, DataMapGraphNodeDTO> nodeMap,
                                       Map<String, DataMapGraphEdgeDTO> edgeMap,
                                       Long snapshotId) {
        if (rootRef == null || !rootRef.startsWith("domain:")) {
            return;
        }
        if (bundle.canonicalSnapshotMemberships() == null || bundle.canonicalSnapshotMemberships().isEmpty()) {
            return;
        }
        Map<Long, CanonicalEntityPO> canonicalEntityMap = bundle.canonicalEntities().stream()
                .collect(Collectors.toMap(CanonicalEntityPO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        for (CanonicalSnapshotMembershipPO membership : bundle.canonicalSnapshotMemberships()) {
            if (membership == null) {
                continue;
            }
            CanonicalEntityPO canonicalEntity = canonicalEntityMap.get(membership.getCanonicalEntityId());
            if (canonicalEntity == null) {
                continue;
            }
            String sceneAssetRef = sceneAssetRef(membership.getSceneAssetType(), membership.getSceneAssetId());
            if (!nodeMap.containsKey(sceneAssetRef)) {
                continue;
            }
            DataMapGraphNodeDTO canonicalNode = canonicalEntityNode(canonicalEntity, snapshotId);
            nodeMap.put(canonicalNode.id(), canonicalNode);
            addEdge(edgeMap, edge(
                    sceneAssetRef,
                    "INSTANCE_OF",
                    canonicalNode.id(),
                    null,
                    traceRef(canonicalEntity.getCanonicalKey(), "INSTANCE_OF"),
                    canonicalEntity.getCanonicalKey(),
                    false,
                    null,
                    Map.of(
                            "canonicalEntityId", canonicalEntity.getId(),
                            "sceneAssetType", membership.getSceneAssetType(),
                            "sceneAssetId", membership.getSceneAssetId()
                    )));
        }
    }

    private void appendCanonicalRelationEdges(String rootRef,
                                              GraphSceneBundle bundle,
                                              Map<String, DataMapGraphNodeDTO> nodeMap,
                                              Map<String, DataMapGraphEdgeDTO> edgeMap,
                                              Long snapshotId) {
        if (rootRef == null || !rootRef.startsWith("domain:")) {
            return;
        }
        if (bundle.canonicalRelations() == null || bundle.canonicalRelations().isEmpty()) {
            return;
        }
        Map<Long, CanonicalEntityPO> canonicalEntityMap = bundle.canonicalEntities().stream()
                .collect(Collectors.toMap(CanonicalEntityPO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        for (CanonicalEntityRelationPO relation : bundle.canonicalRelations()) {
            if (relation == null) {
                continue;
            }
            String sourceRef = canonicalAssetRef(relation.getSourceCanonicalEntityId());
            String targetRef = canonicalAssetRef(relation.getTargetCanonicalEntityId());
            CanonicalEntityPO sourceEntity = canonicalEntityMap.get(relation.getSourceCanonicalEntityId());
            CanonicalEntityPO targetEntity = canonicalEntityMap.get(relation.getTargetCanonicalEntityId());
            if (sourceEntity != null) {
                nodeMap.putIfAbsent(sourceRef, canonicalEntityNode(sourceEntity, snapshotId));
            }
            if (targetEntity != null) {
                nodeMap.putIfAbsent(targetRef, canonicalEntityNode(targetEntity, snapshotId));
            }
            if (!nodeMap.containsKey(sourceRef) || !nodeMap.containsKey(targetRef)) {
                continue;
            }
            addEdge(edgeMap, edge(
                    sourceRef,
                    relation.getRelationType(),
                    targetRef,
                    null,
                    traceRef(relation.getRelationLabel(), relation.getRelationType()),
                    relation.getRelationLabel(),
                    false,
                    null,
                    Map.of("canonicalRelationId", relation.getId())));
        }
    }

    private DataMapGraphNodeDTO domainNode(SceneDTO scene) {
        Map<String, Object> meta = new LinkedHashMap<>();
        putAttribute(meta, "domainId", scene.domainId());
        putAttribute(meta, "domainName", scene.domainName());
        return new DataMapGraphNodeDTO(
                assetRef("domain", scene.domainId()),
                safeText(scene.domainName()).isBlank() ? safeText(scene.domain()) : safeText(scene.domainName()),
                "DOMAIN",
                safeText(scene.domain()),
                safeText(scene.domainName()).isBlank() ? safeText(scene.domain()) : safeText(scene.domainName()),
                "ACTIVE",
                null,
                safeText(scene.domain()),
                "",
                "",
                "",
                0,
                scene.updatedAt(),
                "业务领域根节点",
                meta
        );
    }

    private DataMapGraphNodeDTO sceneNode(SceneDTO scene, int evidenceCount) {
        Map<String, Object> meta = new LinkedHashMap<>();
        putAttribute(meta, "sceneType", scene.sceneType());
        putAttribute(meta, "publishedBy", scene.publishedBy());
        return new DataMapGraphNodeDTO(
                assetRef("scene", scene.id()),
                scene.sceneTitle(),
                "SCENE",
                scene.sceneCode(),
                scene.sceneTitle(),
                scene.status(),
                null,
                safeText(scene.domain()),
                safeText(scene.createdBy()),
                "",
                "",
                evidenceCount,
                scene.verifiedAt() == null ? scene.updatedAt() : scene.verifiedAt(),
                safeText(scene.sceneDescription()),
                meta
        );
    }

    private DataMapGraphNodeDTO planNode(SceneDTO scene, PlanDTO plan) {
        Map<String, Object> meta = new LinkedHashMap<>();
        putAttribute(meta, "sceneId", plan.sceneId());
        putAttribute(meta, "policyIds", plan.policyIds());
        return new DataMapGraphNodeDTO(
                assetRef("plan", plan.id()),
                plan.planName(),
                "PLAN",
                plan.planCode(),
                plan.planName(),
                plan.status(),
                null,
                safeText(scene.domain()),
                "",
                "",
                safeText(plan.defaultTimeSemantic()),
                plan.evidenceIds() == null ? 0 : plan.evidenceIds().size(),
                plan.updatedAt(),
                firstNonBlank(plan.notes(), plan.applicablePeriod(), "方案资产"),
                meta
        );
    }

    private DataMapGraphNodeDTO outputContractNode(SceneDTO scene, OutputContractDTO contract) {
        Map<String, Object> meta = new LinkedHashMap<>();
        putAttribute(meta, "sceneId", contract.sceneId());
        return new DataMapGraphNodeDTO(
                assetRef("output-contract", contract.id()),
                contract.contractName(),
                "OUTPUT_CONTRACT",
                contract.contractCode(),
                contract.contractName(),
                contract.status(),
                null,
                safeText(scene.domain()),
                "",
                "",
                safeText(contract.timeCaliberNote()),
                0,
                contract.updatedAt(),
                firstNonBlank(contract.summaryText(), contract.usageConstraints(), "输出契约"),
                meta
        );
    }

    private DataMapGraphNodeDTO contractViewNode(SceneDTO scene, ContractViewDTO view) {
        Map<String, Object> meta = new LinkedHashMap<>();
        putAttribute(meta, "roleScope", view.roleScope());
        putAttribute(meta, "planId", view.planId());
        putAttribute(meta, "outputContractId", view.outputContractId());
        putAttribute(meta, "versionTag", view.versionTag());
        return new DataMapGraphNodeDTO(
                assetRef("contract-view", view.id()),
                view.viewName(),
                "CONTRACT_VIEW",
                view.viewCode(),
                view.viewName(),
                view.status(),
                view.snapshotId(),
                safeText(scene.domain()),
                "",
                safeText(view.roleScope()),
                "",
                0,
                view.updatedAt(),
                firstNonBlank(view.approvalTemplate(), view.roleScope(), "契约视图"),
                meta
        );
    }

    private DataMapGraphNodeDTO coverageNode(SceneDTO scene, CoverageDeclarationDTO coverage) {
        Map<String, Object> meta = new LinkedHashMap<>();
        putAttribute(meta, "coverageStatus", coverage.coverageStatus());
        putAttribute(meta, "planId", coverage.planId());
        return new DataMapGraphNodeDTO(
                assetRef("coverage-declaration", coverage.id()),
                coverage.coverageTitle(),
                "COVERAGE_DECLARATION",
                coverage.coverageCode(),
                coverage.coverageTitle(),
                coverage.status(),
                null,
                safeText(scene.domain()),
                "",
                "",
                safeText(coverage.timeSemantic()),
                0,
                coverage.updatedAt(),
                firstNonBlank(coverage.statementText(), coverage.gapText(), "覆盖声明"),
                meta
        );
    }

    private DataMapGraphNodeDTO policyNode(SceneDTO scene, PolicyDTO policy) {
        Map<String, Object> meta = new LinkedHashMap<>();
        putAttribute(meta, "scopeType", policy.scopeType());
        putAttribute(meta, "scopeRefId", policy.scopeRefId());
        return new DataMapGraphNodeDTO(
                assetRef("policy", policy.id()),
                policy.policyName(),
                "POLICY",
                policy.policyCode(),
                policy.policyName(),
                policy.status(),
                null,
                safeText(scene.domain()),
                "",
                safeText(policy.sensitivityLevel()),
                "",
                0,
                policy.updatedAt(),
                firstNonBlank(policy.conditionText(), policy.effectType(), "策略对象"),
                meta
        );
    }

    private DataMapGraphNodeDTO evidenceNode(SceneDTO scene, EvidenceFragmentDTO evidence) {
        Map<String, Object> meta = new LinkedHashMap<>();
        putAttribute(meta, "planIds", evidence.planIds());
        putAttribute(meta, "sourceAnchor", evidence.sourceAnchor());
        return new DataMapGraphNodeDTO(
                assetRef("evidence-fragment", evidence.id()),
                evidence.title(),
                "EVIDENCE_FRAGMENT",
                evidence.evidenceCode(),
                evidence.title(),
                evidence.status(),
                null,
                safeText(scene.domain()),
                "",
                "",
                "",
                evidence.planIds() == null ? 0 : evidence.planIds().size(),
                evidence.updatedAt(),
                firstNonBlank(evidence.sourceAnchor(), shorten(evidence.fragmentText()), "证据片段"),
                meta
        );
    }

    private DataMapGraphNodeDTO sourceContractNode(SceneDTO scene, SourceContractDTO contract) {
        Map<String, Object> meta = new LinkedHashMap<>();
        putAttribute(meta, "physicalTable", contract.physicalTable());
        putAttribute(meta, "sourceRole", contract.sourceRole());
        putAttribute(meta, "planId", contract.planId());
        putAttribute(meta, "versionTag", contract.versionTag());
        return new DataMapGraphNodeDTO(
                assetRef("source-contract", contract.id()),
                firstNonBlank(contract.sourceName(), contract.physicalTable(), contract.sourceContractCode()),
                "SOURCE_CONTRACT",
                contract.sourceContractCode(),
                firstNonBlank(contract.sourceName(), contract.physicalTable(), contract.sourceContractCode()),
                contract.status(),
                contract.snapshotId(),
                safeText(scene.domain()),
                "",
                safeText(contract.sensitivityLevel()),
                safeText(contract.timeSemantic()),
                0,
                contract.updatedAt(),
                firstNonBlank(contract.physicalTable(), contract.notes(), "来源契约"),
                meta
        );
    }

    private DataMapGraphNodeDTO sourceIntakeNode(SceneDTO scene, SourceIntakeContractDTO contract) {
        Map<String, Object> meta = new LinkedHashMap<>();
        putAttribute(meta, "sourceType", contract.sourceType());
        putAttribute(meta, "versionTag", contract.versionTag());
        return new DataMapGraphNodeDTO(
                assetRef("source-intake-contract", contract.id()),
                contract.intakeName(),
                "SOURCE_INTAKE_CONTRACT",
                contract.intakeCode(),
                contract.intakeName(),
                contract.status(),
                contract.snapshotId(),
                safeText(scene.domain()),
                "",
                safeText(contract.sensitivityLevel()),
                safeText(contract.defaultTimeSemantic()),
                0,
                contract.updatedAt(),
                firstNonBlank(contract.gapTaskHint(), contract.materialSourceNote(), "来源接入契约"),
                meta
        );
    }

    private DataMapGraphNodeDTO pathTemplateNode(SceneDTO scene,
                                                 PlanDTO plan,
                                                 List<SourceContractDTO> sourceContracts) {
        List<String> contractCodes = sourceContracts.stream()
                .filter(item -> Objects.equals(item.planId(), plan.id()))
                .map(SourceContractDTO::sourceContractCode)
                .toList();
        Long snapshotId = sourceContracts.stream()
                .filter(item -> Objects.equals(item.planId(), plan.id()) && item.snapshotId() != null)
                .map(SourceContractDTO::snapshotId)
                .findFirst()
                .orElse(null);
        Map<String, Object> meta = new LinkedHashMap<>();
        putAttribute(meta, "planId", plan.id());
        putAttribute(meta, "sourceContractCodes", contractCodes);
        putAttribute(meta, "sourceTablesJson", plan.sourceTablesJson());
        return new DataMapGraphNodeDTO(
                assetRef("path-template", plan.id()),
                plan.planName() + " 路径模板",
                "PATH_TEMPLATE",
                plan.planCode() + "-PATH",
                plan.planName() + " 路径模板",
                plan.status(),
                snapshotId,
                safeText(scene.domain()),
                "",
                "",
                safeText(plan.defaultTimeSemantic()),
                plan.evidenceIds() == null ? 0 : plan.evidenceIds().size(),
                plan.updatedAt(),
                firstNonBlank(plan.applicablePeriod(), plan.sourceTablesJson(), "路径模板"),
                meta
        );
    }

    private DataMapGraphNodeDTO versionSnapshotNode(Long snapshotId, List<DataMapGraphNodeDTO> currentNodes) {
        List<DataMapGraphNodeDTO> sameSnapshotNodes = currentNodes.stream()
                .filter(item -> Objects.equals(item.snapshotId(), snapshotId))
                .toList();
        String versionTag = sameSnapshotNodes.stream()
                .map(item -> item.meta() == null ? null : item.meta().get("versionTag"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(text -> !text.isBlank())
                .findFirst()
                .orElse("snapshot-" + snapshotId);
        Map<String, Object> meta = new LinkedHashMap<>();
        putAttribute(meta, "assetTypes", sameSnapshotNodes.stream().map(DataMapGraphNodeDTO::objectType).distinct().toList());
        putAttribute(meta, "assetCount", sameSnapshotNodes.size());
        return new DataMapGraphNodeDTO(
                assetRef("version-snapshot", snapshotId),
                "版本快照 " + snapshotId,
                "VERSION_SNAPSHOT",
                versionTag,
                "版本快照 " + snapshotId,
                "PUBLISHED",
                snapshotId,
                "",
                "",
                "",
                "",
                sameSnapshotNodes.size(),
                sameSnapshotNodes.stream().map(DataMapGraphNodeDTO::lastReviewedAt).filter(Objects::nonNull).findFirst().orElse(null),
                "快照级发布锚点",
                meta
        );
    }

    private DataMapGraphNodeDTO canonicalEntityNode(CanonicalEntityPO canonicalEntity, Long snapshotId) {
        Map<String, Object> meta = new LinkedHashMap<>();
        putAttribute(meta, "canonicalEntityId", canonicalEntity.getId());
        putAttribute(meta, "canonicalKey", canonicalEntity.getCanonicalKey());
        putAttribute(meta, "resolutionStatus", canonicalEntity.getResolutionStatus());
        return new DataMapGraphNodeDTO(
                canonicalAssetRef(canonicalEntity.getId()),
                canonicalEntity.getDisplayName(),
                "CANONICAL_ENTITY",
                canonicalEntity.getCanonicalKey(),
                canonicalEntity.getDisplayName(),
                canonicalEntity.getLifecycleStatus(),
                snapshotId,
                "",
                "",
                "",
                "",
                0,
                canonicalEntity.getUpdatedAt(),
                firstNonBlank(canonicalEntity.getDisplayName(), canonicalEntity.getCanonicalKey(), "统一实体"),
                meta
        );
    }

    private DataMapGraphEdgeDTO edge(String source,
                                     String relationType,
                                     String target,
                                     Double confidence,
                                     String traceId,
                                     String sourceRef,
                                     boolean policyHit,
                                     String coverageExplanation,
                                     Map<String, Object> meta) {
        return new DataMapGraphEdgeDTO(
                source + ">" + relationType + ">" + target,
                relationType,
                resolveRelationGroup(relationType),
                source,
                target,
                relationType,
                confidence,
                traceId,
                sourceRef,
                null,
                null,
                policyHit,
                coverageExplanation,
                meta == null ? Map.of() : meta
        );
    }

    private String resolveRelationGroup(String relationType) {
        String normalized = safeText(relationType).toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SCENE_MEMBERSHIP", "INSTANCE_OF", "APPLIES_POLICY" -> "control";
            case "MAPS_TO_SOURCE" -> "metadata";
            case "SUPPORTED_BY" -> "evidence";
            default -> "";
        };
    }

    private void addEdge(Map<String, DataMapGraphEdgeDTO> edgeMap, DataMapGraphEdgeDTO edge) {
        edgeMap.putIfAbsent(edge.id(), edge);
    }

    private String assetRef(String prefix, Long id) {
        return prefix + ":" + id;
    }

    private String sceneAssetRef(String sceneAssetType, Long id) {
        String normalizedType = safeText(sceneAssetType).trim().toUpperCase(Locale.ROOT);
        if ("EVIDENCE".equals(normalizedType)) {
            return assetRef("evidence-fragment", id);
        }
        return assetRef(
                safeText(sceneAssetType)
                        .trim()
                        .replace('_', '-')
                        .toLowerCase(Locale.ROOT),
                id
        );
    }

    private String canonicalAssetRef(Long canonicalEntityId) {
        return assetRef("canonical-entity", canonicalEntityId);
    }

    private ResolvedAssetRef parseAssetRef(String assetRef) {
        int separator = assetRef.indexOf(':');
        String prefix = separator <= 0 ? assetRef : assetRef.substring(0, separator);
        Long id = Long.valueOf(separator <= 0 ? "-1" : assetRef.substring(separator + 1));
        String objectType = prefix
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        return new ResolvedAssetRef(objectType, id);
    }

    private void putAttribute(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || key.isBlank() || value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        if (value instanceof Collection<?> collection && collection.isEmpty()) {
            return;
        }
        target.put(key, value);
    }

    private Double round(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100.0d) / 100.0d;
    }

    private String traceRef(String code, String suffix) {
        if (code == null || code.isBlank()) {
            return "trace_datamap_" + suffix.toLowerCase(Locale.ROOT);
        }
        return "trace_datamap_" + code.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_") + "_" + suffix.toLowerCase(Locale.ROOT);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String text = safeText(value);
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private String shorten(String text) {
        String safe = safeText(text);
        if (safe.length() <= 120) {
            return safe;
        }
        return safe.substring(0, 117) + "...";
    }

}
