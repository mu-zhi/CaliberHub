package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SnapshotProjectionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SnapshotProjectionPO;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.graphrag.GraphRuntimeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectionValidationServiceTest {

    private ProjectionValidationService service;
    private SnapshotProjectionMapper snapshotProjectionMapper;

    @BeforeEach
    void setUp() {
        snapshotProjectionMapper = mock(SnapshotProjectionMapper.class);
        GraphRuntimeProperties graphRuntimeProperties = new GraphRuntimeProperties();
        graphRuntimeProperties.setNeo4jDatabase("neo4j");
        service = new ProjectionValidationService(snapshotProjectionMapper, graphRuntimeProperties, Optional.of(mock(Driver.class)));
    }

    @Test
    void shouldPassWhenNodeAndEdgeSetsMatch() {
        when(snapshotProjectionMapper.save(any(SnapshotProjectionPO.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SnapshotProjectionPO result = service.recordProjectionAndValidate(
                12L,
                88L,
                Set.of("scene:12", "plan:3"),
                Set.of("scene:12|USES_PLAN|plan:3"),
                Set.of("scene:12", "plan:3"),
                Set.of("scene:12|USES_PLAN|plan:3")
        );

        assertThat(result.getVerificationStatus()).isEqualTo("PASSED");
        assertThat(result.getVerificationMessage()).contains("nodes=2").contains("edges=1");
        assertThat(result.getVerifiedAt()).isNotNull();
    }

    @Test
    void shouldFailWhenNeo4jGraphMissesExpectedRelation() {
        when(snapshotProjectionMapper.save(any(SnapshotProjectionPO.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SnapshotProjectionPO result = service.recordProjectionAndValidate(
                12L,
                88L,
                Set.of("scene:12", "plan:3"),
                Set.of("scene:12|USES_PLAN|plan:3"),
                Set.of("scene:12", "plan:3"),
                Set.of()
        );

        assertThat(result.getVerificationStatus()).isEqualTo("FAILED");
        assertThat(result.getVerificationMessage()).contains("missingEdges");
    }

    @Test
    void shouldFailWhenNeo4jGraphContainsExtraEdge() {
        when(snapshotProjectionMapper.save(any(SnapshotProjectionPO.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SnapshotProjectionPO result = service.recordProjectionAndValidate(
                12L,
                88L,
                Set.of("scene:12", "plan:3"),
                Set.of("scene:12|USES_PLAN|plan:3"),
                Set.of("scene:12", "plan:3", "policy:8"),
                Set.of("scene:12|USES_PLAN|plan:3", "plan:3|GOVERNED_BY_POLICY|policy:8")
        );

        assertThat(result.getVerificationStatus()).isEqualTo("FAILED");
        assertThat(result.getVerificationMessage()).contains("extraEdges");
    }
}
