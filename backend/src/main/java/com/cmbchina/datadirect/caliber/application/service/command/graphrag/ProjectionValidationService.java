package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.common.config.graphrag.GraphRuntimeProperties;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SnapshotProjectionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SnapshotProjectionPO;
import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class ProjectionValidationService {

    private static final Logger log = LoggerFactory.getLogger(ProjectionValidationService.class);

    private final SnapshotProjectionMapper snapshotProjectionMapper;
    private final GraphRuntimeProperties graphRuntimeProperties;
    private final Optional<Driver> neo4jDriver;

    public ProjectionValidationService(SnapshotProjectionMapper snapshotProjectionMapper,
                                       GraphRuntimeProperties graphRuntimeProperties,
                                       Optional<Driver> neo4jDriver) {
        this.snapshotProjectionMapper = snapshotProjectionMapper;
        this.graphRuntimeProperties = graphRuntimeProperties;
        this.neo4jDriver = neo4jDriver;
    }

    public SnapshotProjectionPO recordProjectionAndValidate(Long sceneId, Long snapshotId,
                                                            Set<String> expectedNodeKeys,
                                                            Set<String> expectedEdgeKeys,
                                                            Set<String> actualNodeKeys,
                                                            Set<String> actualEdgeKeys) {
        OffsetDateTime now = OffsetDateTime.now();
        SnapshotProjectionPO po = snapshotProjectionMapper
                .findBySceneIdAndSnapshotId(sceneId, snapshotId)
                .orElseGet(() -> {
                    return freshProjection(sceneId, snapshotId, now);
                });

        po.setProjectionStatus("SUCCEEDED");
        po.setNodeCount(actualNodeKeys.size());
        po.setEdgeCount(actualEdgeKeys.size());
        po.setProjectedAt(now);
        po.setUpdatedAt(now);

        if (neo4jDriver.isEmpty()) {
            po.setVerificationStatus("SKIPPED");
            po.setVerificationMessage("Neo4j driver not configured");
            po.setVerifiedAt(now);
            return snapshotProjectionMapper.save(po);
        }

        try {
            Set<String> missingNodes = new LinkedHashSet<>(expectedNodeKeys);
            missingNodes.removeAll(actualNodeKeys);
            Set<String> extraNodes = new LinkedHashSet<>(actualNodeKeys);
            extraNodes.removeAll(expectedNodeKeys);

            Set<String> missingEdges = new LinkedHashSet<>(expectedEdgeKeys);
            missingEdges.removeAll(actualEdgeKeys);
            Set<String> extraEdges = new LinkedHashSet<>(actualEdgeKeys);
            extraEdges.removeAll(expectedEdgeKeys);

            if (missingNodes.isEmpty() && extraNodes.isEmpty()
                    && missingEdges.isEmpty() && extraEdges.isEmpty()) {
                po.setVerificationStatus("PASSED");
                po.setVerificationMessage("Projection verified: nodes=%d, edges=%d"
                        .formatted(actualNodeKeys.size(), actualEdgeKeys.size()));
            } else {
                po.setVerificationStatus("FAILED");
                po.setVerificationMessage("missingNodes=%s, extraNodes=%s, missingEdges=%s, extraEdges=%s"
                        .formatted(missingNodes, extraNodes, missingEdges, extraEdges));
            }
            po.setVerifiedAt(now);
        } catch (Exception ex) {
            log.warn("Projection validation failed for scene={} snapshot={}: {}", sceneId, snapshotId, ex.getMessage());
            po.setVerificationStatus("FAILED");
            po.setVerificationMessage("Validation error: " + ex.getMessage());
            po.setVerifiedAt(now);
        }

        return snapshotProjectionMapper.save(po);
    }

    public SnapshotProjectionPO recordSkipped(Long sceneId, Long snapshotId, String reason) {
        OffsetDateTime now = OffsetDateTime.now();
        SnapshotProjectionPO po = snapshotProjectionMapper
                .findBySceneIdAndSnapshotId(sceneId, snapshotId)
                .orElseGet(() -> freshProjection(sceneId, snapshotId, now));
        po.setProjectionStatus("SKIPPED");
        po.setVerificationStatus("SKIPPED");
        po.setVerificationMessage(reason);
        po.setUpdatedAt(now);
        po.setVerifiedAt(now);
        return snapshotProjectionMapper.save(po);
    }

    public SnapshotProjectionPO recordFailed(Long sceneId, Long snapshotId, String errorMessage) {
        OffsetDateTime now = OffsetDateTime.now();
        SnapshotProjectionPO po = snapshotProjectionMapper
                .findBySceneIdAndSnapshotId(sceneId, snapshotId)
                .orElseGet(() -> freshProjection(sceneId, snapshotId, now));
        po.setProjectionStatus("FAILED");
        po.setVerificationStatus("FAILED");
        po.setVerificationMessage(errorMessage);
        po.setUpdatedAt(now);
        po.setVerifiedAt(now);
        return snapshotProjectionMapper.save(po);
    }

    private SnapshotProjectionPO freshProjection(Long sceneId, Long snapshotId, OffsetDateTime now) {
        SnapshotProjectionPO fresh = new SnapshotProjectionPO();
        fresh.setSceneId(sceneId);
        fresh.setSnapshotId(snapshotId);
        fresh.setCreatedAt(now);
        return fresh;
    }

    private int[] countNodesAndEdges(Driver driver, Long sceneId, Long snapshotId) {
        try (var session = driver.session(SessionConfig.forDatabase(graphRuntimeProperties.getNeo4jDatabase()))) {
            int nodeCount = session.executeRead(tx -> {
                var result = tx.run(
                        "MATCH (n {sceneId: $sceneId, snapshotId: $snapshotId}) RETURN count(n) as cnt",
                        Values.parameters("sceneId", sceneId, "snapshotId", snapshotId));
                return result.single().get("cnt").asInt();
            });
            int edgeCount = session.executeRead(tx -> {
                var result = tx.run(
                        "MATCH (n {sceneId: $sceneId, snapshotId: $snapshotId})-[r]->() RETURN count(r) as cnt",
                        Values.parameters("sceneId", sceneId, "snapshotId", snapshotId));
                return result.single().get("cnt").asInt();
            });
            return new int[]{nodeCount, edgeCount};
        }
    }
}
