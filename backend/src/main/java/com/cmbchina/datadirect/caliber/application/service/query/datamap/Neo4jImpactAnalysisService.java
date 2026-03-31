package com.cmbchina.datadirect.caliber.application.service.query.datamap;

import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphNodeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphResponseDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapImpactAnalysisDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapImpactAssetDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.ProjectionVerificationStatus;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.ReadSource;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class Neo4jImpactAnalysisService {

    private final Neo4jGraphReadService neo4jGraphReadService;

    public Neo4jImpactAnalysisService(Neo4jGraphReadService neo4jGraphReadService) {
        this.neo4jGraphReadService = neo4jGraphReadService;
    }

    public DataMapImpactAnalysisDTO analyzeFromGraph(String assetRef, Long sceneId, Long snapshotId,
                                                      ProjectionVerificationStatus verificationStatus,
                                                      OffsetDateTime verifiedAt) {
        Neo4jGraphResult fullGraph = neo4jGraphReadService.readGraph(sceneId, snapshotId, null);

        List<DataMapGraphNodeDTO> allNodes = fullGraph.nodes();
        List<DataMapGraphEdgeDTO> allEdges = fullGraph.edges();

        DataMapGraphNodeDTO rootNode = allNodes.stream()
                .filter(n -> assetRef.equals(n.id()))
                .findFirst()
                .orElse(null);

        // BFS up to depth 2 (same logic as relational path)
        java.util.Set<String> impactedNodeIds = new java.util.LinkedHashSet<>();
        java.util.Set<String> impactedEdgeIds = new java.util.LinkedHashSet<>();
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
        java.util.Map<String, Integer> depthByNode = new java.util.LinkedHashMap<>();

        queue.add(assetRef);
        depthByNode.put(assetRef, 0);
        impactedNodeIds.add(assetRef);

        java.util.Map<String, List<DataMapGraphEdgeDTO>> edgesByNode = new java.util.LinkedHashMap<>();
        for (DataMapGraphEdgeDTO edge : allEdges) {
            edgesByNode.computeIfAbsent(edge.source(), k -> new java.util.ArrayList<>()).add(edge);
            edgesByNode.computeIfAbsent(edge.target(), k -> new java.util.ArrayList<>()).add(edge);
        }

        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            int depth = depthByNode.getOrDefault(current, 0);
            if (depth >= 2) continue;
            for (DataMapGraphEdgeDTO edge : edgesByNode.getOrDefault(current, List.of())) {
                impactedEdgeIds.add(edge.id());
                String next = current.equals(edge.source()) ? edge.target() : edge.source();
                if (impactedNodeIds.add(next)) {
                    depthByNode.put(next, depth + 1);
                    queue.add(next);
                }
            }
        }

        List<DataMapGraphNodeDTO> impactedNodes = allNodes.stream()
                .filter(n -> impactedNodeIds.contains(n.id()))
                .toList();
        List<DataMapGraphEdgeDTO> impactedEdges = allEdges.stream()
                .filter(e -> impactedEdgeIds.contains(e.id()))
                .toList();

        List<DataMapImpactAssetDTO> affectedAssets = ImpactAnalysisSupport.buildAffectedAssets(
                assetRef, rootNode, impactedNodes, impactedEdges);

        return new DataMapImpactAnalysisDTO(
                assetRef,
                ImpactAnalysisSupport.riskLevel(rootNode, affectedAssets.size()),
                ReadSource.NEO4J,
                verificationStatus,
                verifiedAt,
                ImpactAnalysisSupport.recommendedActions(rootNode),
                affectedAssets,
                new DataMapGraphResponseDTO(assetRef, sceneId, fullGraph.sceneName(),
                        snapshotId, ReadSource.NEO4J, verificationStatus, verifiedAt,
                        impactedNodes, impactedEdges)
        );
    }
}
