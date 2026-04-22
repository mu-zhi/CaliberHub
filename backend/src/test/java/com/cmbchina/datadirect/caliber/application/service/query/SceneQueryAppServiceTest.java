package com.cmbchina.datadirect.caliber.application.service.query;

import com.cmbchina.datadirect.caliber.application.api.dto.request.SceneListQuery;
import com.cmbchina.datadirect.caliber.application.assembler.SceneAssembler;
import com.cmbchina.datadirect.caliber.application.support.RetrievalExperimentSupport;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.domain.support.CaliberDomainSupport;
import com.cmbchina.datadirect.caliber.domain.support.SceneDomainSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SceneQueryAppServiceTest {

    @Mock
    private SceneDomainSupport sceneDomainSupport;

    @Mock
    private CaliberDomainSupport caliberDomainSupport;

    @Mock
    private SceneAssembler sceneAssembler;

    private SceneQueryAppService sceneQueryAppService;

    @BeforeEach
    void setUp() {
        sceneQueryAppService = new SceneQueryAppService(
                sceneDomainSupport,
                caliberDomainSupport,
                sceneAssembler,
                new ObjectMapper()
        );
    }

    @Test
    void shouldRejectInvalidStatus() {
        SceneListQuery query = new SceneListQuery(null, null, "INVALID", "客户");

        assertThatThrownBy(() -> sceneQueryAppService.list(query))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("allowed values");
    }

    @Test
    void shouldTreatExperimentCandidatesAsHintsInsteadOfFormalDecisions() {
        RetrievalExperimentSupport.RetrievalExperimentResult result = new RetrievalExperimentSupport.RetrievalExperimentResult(
                "LightRAG",
                "contract-test",
                "COMPLETED",
                false,
                null,
                null,
                List.of(new RetrievalExperimentSupport.SceneCandidate(1L, "SCN_PAYROLL_DETAIL", "代发明细查询", 42L, 0.82d, "slot+evidence")),
                List.of(),
                List.of(),
                List.of("§3.2"),
                List.of(),
                Map.of("requestedSnapshotId", "42")
        );

        assertThat(result.decision()).isNull();
        assertThat(result.candidateScenes()).hasSize(1);
        assertThat(result.referenceRefs()).containsExactly("§3.2");
    }
}
