package com.cmbchina.datadirect.caliber.application.service.query.datamap;

import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphNodeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphResponseDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.ProjectionVerificationStatus;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.ReadSource;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.OffsetDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphQueryServiceTest {

    @Mock
    private GraphReadService graphReadService;

    @Mock
    private DataMapGraphDtoAdapter dataMapGraphDtoAdapter;

    @Mock
    private ReadSourceRouter readSourceRouter;

    @Mock
    private Neo4jGraphReadService neo4jGraphReadService;

    private GraphQueryService service;

    @BeforeEach
    void setUp() {
        service = new GraphQueryService(
                graphReadService,
                dataMapGraphDtoAdapter,
                readSourceRouter,
                neo4jGraphReadService
        );
    }

    @Test
    void shouldUseNeo4jWhenProjectionVerificationPassed() {
        when(graphReadService.resolveSceneId("SCENE", 12L)).thenReturn(12L);
        when(readSourceRouter.decide(12L, 88L)).thenReturn(new ReadSourceRouter.ReadSourceDecision(
                ReadSource.NEO4J,
                88L,
                ProjectionVerificationStatus.PASSED,
                OffsetDateTime.parse("2026-03-30T10:15:00+08:00")
        ));
        when(neo4jGraphReadService.readGraph(eq(12L), eq(88L), any(DataMapGraphQueryOptions.class)))
                .thenReturn(new Neo4jGraphResult(
                        "scene:12",
                        12L,
                        "代发样板场景",
                        List.of(),
                        List.of()
                ));

        DataMapGraphResponseDTO result = service.queryGraph("SCENE", 12L, 88L, null, null, null, null);

        assertThat(result.readSource()).isEqualTo(ReadSource.NEO4J);
        assertThat(result.snapshotId()).isEqualTo(88L);
        assertThat(result.projectionVerificationStatus()).isEqualTo(ProjectionVerificationStatus.PASSED);
        assertThat(result.nodes()).isEmpty();
        assertThat(result.edges()).isEmpty();
    }

    @Test
    void shouldFallbackToRelationalWhenNeo4jThrowsAfterPassedDecision() {
        when(graphReadService.resolveSceneId("SCENE", 12L)).thenReturn(12L);
        when(readSourceRouter.decide(12L, 88L)).thenReturn(new ReadSourceRouter.ReadSourceDecision(
                ReadSource.NEO4J,
                88L,
                ProjectionVerificationStatus.PASSED,
                OffsetDateTime.parse("2026-03-30T10:15:00+08:00")
        ));
        when(neo4jGraphReadService.readGraph(eq(12L), eq(88L), any(DataMapGraphQueryOptions.class)))
                .thenThrow(new IllegalStateException("Neo4j unavailable"));

        GraphSceneBundle relationalBundle = relationalBundle();
        when(graphReadService.loadBundle("SCENE", 12L)).thenReturn(relationalBundle);
        when(dataMapGraphDtoAdapter.buildGraph(
                "scene:12",
                relationalBundle,
                DataMapGraphQueryOptions.of(88L, null, null, null, null)
        )).thenReturn(new DataMapGraphResponseDTO(
                "scene:12",
                12L,
                "代发样板场景",
                88L,
                ReadSource.RELATIONAL,
                ProjectionVerificationStatus.PASSED,
                OffsetDateTime.parse("2026-03-30T10:15:00+08:00"),
                List.of(new DataMapGraphNodeDTO(
                        "scene:12",
                        "代发样板场景",
                        "SCENE",
                        "SCN_PAYROLL_SAMPLE",
                        "代发样板场景",
                        "PUBLISHED",
                        88L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )),
                List.of()
        ));

        DataMapGraphResponseDTO result = service.queryGraph("SCENE", 12L, 88L, null, null, null, null);

        assertThat(result.readSource()).isEqualTo(ReadSource.RELATIONAL);
        assertThat(result.projectionVerificationStatus()).isEqualTo(ProjectionVerificationStatus.PASSED);
        assertThat(result.snapshotId()).isEqualTo(88L);
        assertThat(result.nodes()).hasSize(1);
        assertThat(result.nodes().get(0).id()).isEqualTo("scene:12");
    }

    private GraphSceneBundle relationalBundle() {
        SceneDTO scene = new SceneDTO(
                12L,
                "SCN_PAYROLL_SAMPLE",
                "代发样板场景",
                3L,
                "代发域",
                "代发域",
                "FACT_DETAIL",
                "PUBLISHED",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        return new GraphSceneBundle(
                scene,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
