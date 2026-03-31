package com.cmbchina.datadirect.caliber.application.service.query.datamap;

import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.ProjectionVerificationStatus;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.ReadSource;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.graphrag.GraphRuntimeProperties;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneVersionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SnapshotProjectionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneVersionPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SnapshotProjectionPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReadSourceRouterTest {

    private GraphRuntimeProperties properties;
    private SnapshotProjectionMapper snapshotProjectionMapper;
    private SceneVersionMapper sceneVersionMapper;

    /**
     * We use a non-null sentinel to represent "driver is present" without actually
     * mocking the Driver interface (Mockito cannot mock neo4j Driver on Java 25+).
     * The router never calls driver methods — it only checks Optional.isEmpty().
     */
    private static final Optional<Driver> DRIVER_PRESENT;

    static {
        // Create a real (but unusable) driver pointing to a dummy address.
        // It won't be used for actual connections in these tests.
        Driver dummy;
        try {
            dummy = GraphDatabase.driver("bolt://localhost:0", AuthTokens.none());
        } catch (Exception e) {
            dummy = null;
        }
        DRIVER_PRESENT = Optional.ofNullable(dummy);
    }

    @BeforeEach
    void setUp() {
        properties = new GraphRuntimeProperties();
        snapshotProjectionMapper = mock(SnapshotProjectionMapper.class);
        sceneVersionMapper = mock(SceneVersionMapper.class);
    }

    @Test
    void shouldReturnRelationalWhenReadDisabled() {
        properties.setReadEnabled(false);
        ReadSourceRouter router = new ReadSourceRouter(properties, snapshotProjectionMapper, sceneVersionMapper, DRIVER_PRESENT);

        ReadSourceRouter.ReadSourceDecision decision = router.decide(1L, null);

        assertThat(decision.readSource()).isEqualTo(ReadSource.RELATIONAL);
        assertThat(decision.verificationStatus()).isEqualTo(ProjectionVerificationStatus.SKIPPED);
    }

    @Test
    void shouldReturnRelationalWhenDriverAbsent() {
        properties.setReadEnabled(true);
        ReadSourceRouter router = new ReadSourceRouter(properties, snapshotProjectionMapper, sceneVersionMapper, Optional.empty());

        ReadSourceRouter.ReadSourceDecision decision = router.decide(1L, 10L);

        assertThat(decision.readSource()).isEqualTo(ReadSource.RELATIONAL);
        assertThat(decision.verificationStatus()).isEqualTo(ProjectionVerificationStatus.SKIPPED);
    }

    @Test
    void shouldReturnRelationalWhenNoPublishedSnapshot() {
        properties.setReadEnabled(true);
        when(sceneVersionMapper.findTopBySceneIdOrderByVersionNoDesc(1L)).thenReturn(Optional.empty());
        ReadSourceRouter router = new ReadSourceRouter(properties, snapshotProjectionMapper, sceneVersionMapper, DRIVER_PRESENT);

        ReadSourceRouter.ReadSourceDecision decision = router.decide(1L, null);

        assertThat(decision.readSource()).isEqualTo(ReadSource.RELATIONAL);
        assertThat(decision.verificationStatus()).isEqualTo(ProjectionVerificationStatus.SKIPPED);
        assertThat(decision.snapshotId()).isNull();
    }

    @Test
    void shouldReturnNeo4jWhenVerificationPassed() {
        properties.setReadEnabled(true);
        SnapshotProjectionPO po = new SnapshotProjectionPO();
        po.setVerificationStatus("PASSED");
        po.setVerifiedAt(OffsetDateTime.now());
        when(snapshotProjectionMapper.findBySceneIdAndSnapshotId(1L, 10L)).thenReturn(Optional.of(po));
        ReadSourceRouter router = new ReadSourceRouter(properties, snapshotProjectionMapper, sceneVersionMapper, DRIVER_PRESENT);

        ReadSourceRouter.ReadSourceDecision decision = router.decide(1L, 10L);

        assertThat(decision.readSource()).isEqualTo(ReadSource.NEO4J);
        assertThat(decision.verificationStatus()).isEqualTo(ProjectionVerificationStatus.PASSED);
        assertThat(decision.snapshotId()).isEqualTo(10L);
    }

    @Test
    void shouldReturnRelationalWhenVerificationFailed() {
        properties.setReadEnabled(true);
        SnapshotProjectionPO po = new SnapshotProjectionPO();
        po.setVerificationStatus("FAILED");
        po.setVerifiedAt(OffsetDateTime.now());
        when(snapshotProjectionMapper.findBySceneIdAndSnapshotId(1L, 10L)).thenReturn(Optional.of(po));
        ReadSourceRouter router = new ReadSourceRouter(properties, snapshotProjectionMapper, sceneVersionMapper, DRIVER_PRESENT);

        ReadSourceRouter.ReadSourceDecision decision = router.decide(1L, 10L);

        assertThat(decision.readSource()).isEqualTo(ReadSource.RELATIONAL);
        assertThat(decision.verificationStatus()).isEqualTo(ProjectionVerificationStatus.FAILED);
    }

    @Test
    void shouldReturnRelationalWhenNoProjectionRecord() {
        properties.setReadEnabled(true);
        when(snapshotProjectionMapper.findBySceneIdAndSnapshotId(1L, 10L)).thenReturn(Optional.empty());
        ReadSourceRouter router = new ReadSourceRouter(properties, snapshotProjectionMapper, sceneVersionMapper, DRIVER_PRESENT);

        ReadSourceRouter.ReadSourceDecision decision = router.decide(1L, 10L);

        assertThat(decision.readSource()).isEqualTo(ReadSource.RELATIONAL);
        assertThat(decision.verificationStatus()).isEqualTo(ProjectionVerificationStatus.PENDING);
    }

    @Test
    void shouldResolveSnapshotIdWhenNotProvided() {
        properties.setReadEnabled(true);
        SceneVersionPO version = new SceneVersionPO();
        version.setId(42L);
        when(sceneVersionMapper.findTopBySceneIdOrderByVersionNoDesc(1L)).thenReturn(Optional.of(version));
        SnapshotProjectionPO po = new SnapshotProjectionPO();
        po.setVerificationStatus("PASSED");
        po.setVerifiedAt(OffsetDateTime.now());
        when(snapshotProjectionMapper.findBySceneIdAndSnapshotId(1L, 42L)).thenReturn(Optional.of(po));
        ReadSourceRouter router = new ReadSourceRouter(properties, snapshotProjectionMapper, sceneVersionMapper, DRIVER_PRESENT);

        ReadSourceRouter.ReadSourceDecision decision = router.decide(1L, null);

        assertThat(decision.readSource()).isEqualTo(ReadSource.NEO4J);
        assertThat(decision.snapshotId()).isEqualTo(42L);
    }
}
