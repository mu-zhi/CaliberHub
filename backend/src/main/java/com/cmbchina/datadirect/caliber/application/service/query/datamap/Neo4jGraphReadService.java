package com.cmbchina.datadirect.caliber.application.service.query.datamap;

import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphNodeDTO;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.graphrag.GraphRuntimeProperties;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class Neo4jGraphReadService {

    private static final Logger log = LoggerFactory.getLogger(Neo4jGraphReadService.class);

    private final Optional<Driver> neo4jDriver;
    private final GraphRuntimeProperties graphRuntimeProperties;

    public Neo4jGraphReadService(Optional<Driver> neo4jDriver,
                                 GraphRuntimeProperties graphRuntimeProperties) {
        this.neo4jDriver = neo4jDriver;
        this.graphRuntimeProperties = graphRuntimeProperties;
    }

    public Neo4jGraphResult readGraph(Long sceneId, Long snapshotId, DataMapGraphQueryOptions options) {
        Driver driver = neo4jDriver.orElseThrow(() ->
                new IllegalStateException("Neo4j driver not available"));

        Map<String, DataMapGraphNodeDTO> nodeMap = new LinkedHashMap<>();
        Set<String> edgeIds = new LinkedHashSet<>();
        List<DataMapGraphEdgeDTO> edges = new ArrayList<>();
        String sceneName = "";
        String rootRef = "scene:" + sceneId;

        try (var session = driver.session(SessionConfig.forDatabase(graphRuntimeProperties.getNeo4jDatabase()))) {
            List<Record> records = session.executeRead(tx -> {
                var result = tx.run(
                        "MATCH (s:Scene {sceneId: $sceneId}) " +
                        "OPTIONAL MATCH (s)-[r]->(child) " +
                        "OPTIONAL MATCH (child)-[r2]->(grandchild) " +
                        "RETURN s, r, child, r2, grandchild",
                        Values.parameters("sceneId", sceneId));
                return result.list();
            });

            for (Record record : records) {
                Node sceneNode = record.get("s").asNode();
                sceneName = safeString(sceneNode, "sceneTitle");

                DataMapGraphNodeDTO sceneDto = mapNode(sceneNode, "SCENE", "scene");
                nodeMap.putIfAbsent(sceneDto.id(), sceneDto);

                if (!record.get("child").isNull()) {
                    Node child = record.get("child").asNode();
                    String childType = resolveNodeType(child);
                    DataMapGraphNodeDTO childDto = mapNode(child, childType, childType.toLowerCase(Locale.ROOT));
                    nodeMap.putIfAbsent(childDto.id(), childDto);

                    if (!record.get("r").isNull()) {
                        Relationship rel = record.get("r").asRelationship();
                        addEdge(rel, sceneDto.id(), childDto.id(), edgeIds, edges);
                    }

                    if (!record.get("grandchild").isNull()) {
                        Node grandchild = record.get("grandchild").asNode();
                        String gcType = resolveNodeType(grandchild);
                        DataMapGraphNodeDTO gcDto = mapNode(grandchild, gcType, gcType.toLowerCase(Locale.ROOT));
                        nodeMap.putIfAbsent(gcDto.id(), gcDto);

                        if (!record.get("r2").isNull()) {
                            Relationship rel2 = record.get("r2").asRelationship();
                            addEdge(rel2, childDto.id(), gcDto.id(), edgeIds, edges);
                        }
                    }
                }
            }
        }

        List<DataMapGraphNodeDTO> filteredNodes = nodeMap.values().stream()
                .filter(node -> matchesOptions(node, options))
                .toList();

        Set<String> filteredNodeIds = new LinkedHashSet<>();
        filteredNodes.forEach(n -> filteredNodeIds.add(n.id()));
        List<DataMapGraphEdgeDTO> filteredEdges = edges.stream()
                .filter(e -> filteredNodeIds.contains(e.source()) && filteredNodeIds.contains(e.target()))
                .toList();

        return new Neo4jGraphResult(rootRef, sceneId, sceneName, filteredNodes, filteredEdges);
    }

    private DataMapGraphNodeDTO mapNode(Node node, String objectType, String prefix) {
        String id = prefix + ":" + node.get(prefix + "Id").asObject();
        String label = safeString(node, prefix + "Title");
        if (label.isEmpty()) {
            label = safeString(node, prefix + "Name");
        }
        if (label.isEmpty()) {
            label = safeString(node, prefix + "Code");
        }
        String code = safeString(node, prefix + "Code");
        String status = safeString(node, "status");

        return new DataMapGraphNodeDTO(
                id, label, objectType, code, label, status,
                null, null, null, null, null,
                null, null, null, null);
    }

    private void addEdge(Relationship rel, String sourceId, String targetId,
                         Set<String> edgeIds, List<DataMapGraphEdgeDTO> edges) {
        String edgeId = sourceId + "->" + rel.type() + "->" + targetId;
        if (edgeIds.add(edgeId)) {
            edges.add(new DataMapGraphEdgeDTO(
                    edgeId, rel.type(), "", sourceId, targetId,
                    rel.type(), null, null, null, null, null, null, null, null));
        }
    }

    private String resolveNodeType(Node node) {
        for (String label : node.labels()) {
            return label.toUpperCase(Locale.ROOT);
        }
        return "UNKNOWN";
    }

    private boolean matchesOptions(DataMapGraphNodeDTO node, DataMapGraphQueryOptions options) {
        if (options == null) return true;
        if (!options.objectTypes().isEmpty()
                && !options.objectTypes().contains(node.objectType().toUpperCase(Locale.ROOT))) {
            return false;
        }
        if (!options.statuses().isEmpty()
                && node.status() != null
                && !options.statuses().contains(node.status().toUpperCase(Locale.ROOT))) {
            return false;
        }
        return true;
    }

    private String safeString(Node node, String key) {
        try {
            var value = node.get(key);
            return value.isNull() ? "" : value.asString();
        } catch (Exception e) {
            return "";
        }
    }
}
