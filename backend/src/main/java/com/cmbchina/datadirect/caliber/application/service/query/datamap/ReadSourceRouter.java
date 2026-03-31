package com.cmbchina.datadirect.caliber.application.service.query.datamap;

import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.ProjectionVerificationStatus;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.ReadSource;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.graphrag.GraphRuntimeProperties;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneVersionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SnapshotProjectionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneVersionPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SnapshotProjectionPO;
import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class ReadSourceRouter {

    private static final Logger log = LoggerFactory.getLogger(ReadSourceRouter.class);

    private final GraphRuntimeProperties graphRuntimeProperties;
    private final SnapshotProjectionMapper snapshotProjectionMapper;
    private final SceneVersionMapper sceneVersionMapper;
    private final Optional<Driver> neo4jDriver;

    public ReadSourceRouter(GraphRuntimeProperties graphRuntimeProperties,
                            SnapshotProjectionMapper snapshotProjectionMapper,
                            SceneVersionMapper sceneVersionMapper,
                            Optional<Driver> neo4jDriver) {
        this.graphRuntimeProperties = graphRuntimeProperties;
        this.snapshotProjectionMapper = snapshotProjectionMapper;
        this.sceneVersionMapper = sceneVersionMapper;
        this.neo4jDriver = neo4jDriver;
    }

    public ReadSourceDecision decide(Long sceneId, Long requestedSnapshotId) {
        if (!graphRuntimeProperties.isReadEnabled()) {
            return new ReadSourceDecision(ReadSource.RELATIONAL, requestedSnapshotId,
                    ProjectionVerificationStatus.SKIPPED, null);
        }

        if (neo4jDriver.isEmpty()) {
            return new ReadSourceDecision(ReadSource.RELATIONAL, requestedSnapshotId,
                    ProjectionVerificationStatus.SKIPPED, null);
        }

        Long snapshotId = requestedSnapshotId;
        if (snapshotId == null) {
            snapshotId = sceneVersionMapper.findTopBySceneIdOrderByVersionNoDesc(sceneId)
                    .map(SceneVersionPO::getId)
                    .orElse(null);
        }

        if (snapshotId == null) {
            return new ReadSourceDecision(ReadSource.RELATIONAL, null,
                    ProjectionVerificationStatus.SKIPPED, null);
        }

        Optional<SnapshotProjectionPO> projectionOpt = snapshotProjectionMapper
                .findBySceneIdAndSnapshotId(sceneId, snapshotId);

        if (projectionOpt.isEmpty()) {
            return new ReadSourceDecision(ReadSource.RELATIONAL, snapshotId,
                    ProjectionVerificationStatus.PENDING, null);
        }

        SnapshotProjectionPO projection = projectionOpt.get();
        String status = projection.getVerificationStatus();
        OffsetDateTime verifiedAt = projection.getVerifiedAt();

        if ("PASSED".equals(status)) {
            return new ReadSourceDecision(ReadSource.NEO4J, snapshotId,
                    ProjectionVerificationStatus.PASSED, verifiedAt);
        }

        ProjectionVerificationStatus pvs;
        try {
            pvs = ProjectionVerificationStatus.valueOf(status);
        } catch (Exception e) {
            pvs = ProjectionVerificationStatus.FAILED;
        }
        return new ReadSourceDecision(ReadSource.RELATIONAL, snapshotId, pvs, verifiedAt);
    }

    public record ReadSourceDecision(
            ReadSource readSource,
            Long snapshotId,
            ProjectionVerificationStatus verificationStatus,
            OffsetDateTime verifiedAt
    ) {}
}
