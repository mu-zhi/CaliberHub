package com.cmbchina.datadirect.caliber.application.service.query.datamap;

import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphNodeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphResponseDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapImpactAnalysisDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapImpactAssetDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ImpactAnalysisService {

    private final GraphReadService graphReadService;
    private final GraphQueryService graphQueryService;

    public ImpactAnalysisService(GraphReadService graphReadService,
                                 GraphQueryService graphQueryService) {
        this.graphReadService = graphReadService;
        this.graphQueryService = graphQueryService;
    }

    @Transactional(readOnly = true)
    public DataMapImpactAnalysisDTO analyze(String assetRef, Long snapshotId) {
        GraphSceneBundle bundle = graphReadService.loadBundleByAssetRef(assetRef);
        DataMapGraphResponseDTO fullGraph = graphQueryService.queryGraph("SCENE", bundle.scene().id(), snapshotId, null, null, null, null);

        Map<String, DataMapGraphNodeDTO> nodeById = new LinkedHashMap<>();
        fullGraph.nodes().forEach(node -> nodeById.put(node.id(), node));
        Map<String, List<DataMapGraphEdgeDTO>> edgesByNode = new LinkedHashMap<>();
        for (DataMapGraphEdgeDTO edge : fullGraph.edges()) {
            edgesByNode.computeIfAbsent(edge.source(), key -> new ArrayList<>()).add(edge);
            edgesByNode.computeIfAbsent(edge.target(), key -> new ArrayList<>()).add(edge);
        }

        Set<String> impactedNodeIds = new LinkedHashSet<>();
        Set<String> impactedEdgeIds = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        Map<String, Integer> depthByNode = new LinkedHashMap<>();
        queue.add(assetRef);
        depthByNode.put(assetRef, 0);
        impactedNodeIds.add(assetRef);

        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            int depth = depthByNode.getOrDefault(current, 0);
            if (depth >= 2) {
                continue;
            }
            for (DataMapGraphEdgeDTO edge : edgesByNode.getOrDefault(current, List.of())) {
                impactedEdgeIds.add(edge.id());
                String next = current.equals(edge.source()) ? edge.target() : edge.source();
                if (impactedNodeIds.add(next)) {
                    depthByNode.put(next, depth + 1);
                    queue.add(next);
                }
            }
        }

        List<DataMapGraphNodeDTO> impactedNodes = fullGraph.nodes().stream()
                .filter(node -> impactedNodeIds.contains(node.id()))
                .toList();
        List<DataMapGraphEdgeDTO> impactedEdges = fullGraph.edges().stream()
                .filter(edge -> impactedEdgeIds.contains(edge.id()))
                .toList();

        DataMapGraphNodeDTO rootNode = nodeById.get(assetRef);
        List<DataMapImpactAssetDTO> affectedAssets = ImpactAnalysisSupport.buildAffectedAssets(
                assetRef, rootNode, impactedNodes, impactedEdges);

        return new DataMapImpactAnalysisDTO(
                assetRef,
                ImpactAnalysisSupport.riskLevel(rootNode, affectedAssets.size()),
                fullGraph.readSource(),
                fullGraph.projectionVerificationStatus(),
                fullGraph.projectionVerifiedAt(),
                ImpactAnalysisSupport.recommendedActions(rootNode),
                affectedAssets,
                new DataMapGraphResponseDTO(assetRef, fullGraph.sceneId(), fullGraph.sceneName(),
                        fullGraph.snapshotId(), fullGraph.readSource(), fullGraph.projectionVerificationStatus(),
                        fullGraph.projectionVerifiedAt(), impactedNodes, impactedEdges)
        );
    }
}
