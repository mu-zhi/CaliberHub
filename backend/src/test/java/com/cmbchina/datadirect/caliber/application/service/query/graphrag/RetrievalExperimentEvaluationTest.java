package com.cmbchina.datadirect.caliber.application.service.query.graphrag;

import com.cmbchina.datadirect.caliber.application.service.command.graphrag.GraphAuditEventAppService;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.graphrag.GraphRuntimeProperties;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.AuditEventMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.AuditEventPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RetrievalExperimentEvaluationTest {

    @Mock
    private AuditEventMapper auditEventMapper;

    private GraphRuntimeProperties properties;
    private RetrievalExperimentEvaluationService service;

    @BeforeEach
    void setUp() {
        properties = new GraphRuntimeProperties();
        properties.setShadowModeEnabled(true);
        properties.setGrayReleaseEnabled(true);
        properties.setGrayReleaseScope("domain:payroll");
        properties.setRetrievalAdapterName("LightRAG");
        properties.setRetrievalIndexVersion("snapshot-20260422");
        properties.setSceneHitAt5Threshold(0.75d);
        properties.setEvidencePrecisionAt10Threshold(0.60d);
        properties.setP95LatencyBudgetMs(8000L);
        properties.setRequiredObservationFields(6);
        service = new RetrievalExperimentEvaluationService(properties);
    }

    @Test
    void shouldCalculateGrayReadyReportWhenThresholdsPass() {
        RetrievalExperimentEvaluationService.EvaluationReport report = service.evaluate(List.of(
                new RetrievalExperimentEvaluationService.ExperimentReplaySample(
                        "trace-01",
                        101L,
                        "LightRAG",
                        "snapshot-20260422",
                        true,
                        8,
                        10,
                        false,
                        true,
                        1200L,
                        1800L,
                        6,
                        6,
                        6,
                        9
                ),
                new RetrievalExperimentEvaluationService.ExperimentReplaySample(
                        "trace-02",
                        101L,
                        "LightRAG",
                        "snapshot-20260422",
                        true,
                        7,
                        10,
                        false,
                        true,
                        1500L,
                        2200L,
                        6,
                        6,
                        5,
                        8
                ),
                new RetrievalExperimentEvaluationService.ExperimentReplaySample(
                        "trace-03",
                        101L,
                        "LightRAG",
                        "snapshot-20260422",
                        true,
                        6,
                        10,
                        false,
                        true,
                        1700L,
                        2400L,
                        6,
                        6,
                        7,
                        10
                )
        ));

        assertThat(report.summary().gateDecision()).isEqualTo("GRAY_READY");
        assertThat(report.summary().shadowModeEnabled()).isTrue();
        assertThat(report.summary().grayReleaseScope()).isEqualTo("domain:payroll");
        assertThat(report.metrics().sceneHitAt5()).isEqualTo(1.0d);
        assertThat(report.metrics().evidencePrecisionAt10()).isEqualTo(0.7d);
        assertThat(report.metrics().policyFalseAllowCount()).isZero();
        assertThat(report.metrics().snapshotMismatchCount()).isZero();
        assertThat(report.metrics().observedFieldCompleteness()).isEqualTo(1.0d);
        assertThat(report.summary().rollbackRecommendation()).isEqualTo("KEEP_SHADOW_AND_OPEN_GRAY");
    }

    @Test
    void shouldRequireRollbackWhenFalseAllowOrSnapshotMismatchDetected() {
        RetrievalExperimentEvaluationService.EvaluationReport report = service.evaluate(List.of(
                new RetrievalExperimentEvaluationService.ExperimentReplaySample(
                        "trace-unsafe",
                        101L,
                        "LightRAG",
                        "snapshot-20260422",
                        false,
                        3,
                        10,
                        true,
                        false,
                        2500L,
                        5200L,
                        4,
                        6,
                        2,
                        5
                ),
                new RetrievalExperimentEvaluationService.ExperimentReplaySample(
                        "trace-mismatch",
                        101L,
                        "LightRAG",
                        "snapshot-20260422",
                        true,
                        4,
                        10,
                        false,
                        true,
                        2800L,
                        6100L,
                        5,
                        6,
                        3,
                        6
                )
        ));

        assertThat(report.summary().gateDecision()).isEqualTo("ROLLBACK_REQUIRED");
        assertThat(report.summary().rollbackRecommendation()).isEqualTo("DISABLE_EXPERIMENT_ADAPTER");
        assertThat(report.summary().blockers())
                .contains("POLICY_FALSE_ALLOW", "SNAPSHOT_MISMATCH", "OBSERVATION_FIELDS_MISSING");
        assertThat(report.metrics().policyFalseAllowCount()).isEqualTo(1);
        assertThat(report.metrics().snapshotMismatchCount()).isEqualTo(1);
        assertThat(report.metrics().observedFieldCompleteness()).isLessThan(1.0d);
    }

    @Test
    void shouldPersistRetrievalExperimentAuditFields() {
        GraphAuditEventAppService auditService = new GraphAuditEventAppService(auditEventMapper, new ObjectMapper());

        auditService.recordRetrievalExperiment(
                12L,
                "trace-runtime-08d",
                88L,
                "ops-admin",
                "job-08d",
                new GraphAuditEventAppService.RetrievalExperimentAuditPayload(
                        "LightRAG",
                        "snapshot-20260422",
                        1850L,
                        6,
                        9,
                        true,
                        "domain:payroll",
                        true,
                        "KEEP_SHADOW_AND_OPEN_GRAY"
                )
        );

        ArgumentCaptor<AuditEventPO> captor = ArgumentCaptor.forClass(AuditEventPO.class);
        verify(auditEventMapper).save(captor.capture());
        AuditEventPO saved = captor.getValue();

        assertThat(saved.getEventName()).isEqualTo("RUNTIME_RETRIEVAL_EXPERIMENT");
        assertThat(saved.getTraceId()).isEqualTo("trace-runtime-08d");
        assertThat(saved.getSnapshotId()).isEqualTo(88L);
        assertThat(saved.getAdapterName()).isEqualTo("LightRAG");
        assertThat(saved.getIndexVersion()).isEqualTo("snapshot-20260422");
        assertThat(saved.getLatencyMs()).isEqualTo(1850L);
        assertThat(saved.getReferenceCount()).isEqualTo(6);
        assertThat(saved.getCandidateCount()).isEqualTo(9);
        assertThat(saved.getShadowModeEnabled()).isTrue();
        assertThat(saved.getGrayReleaseScope()).isEqualTo("domain:payroll");
        assertThat(saved.getFalseAllowRisk()).isTrue();
        assertThat(saved.getRollbackRecommendation()).isEqualTo("KEEP_SHADOW_AND_OPEN_GRAY");
    }
}
