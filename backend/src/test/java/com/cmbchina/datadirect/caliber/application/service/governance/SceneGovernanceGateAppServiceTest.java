package com.cmbchina.datadirect.caliber.application.service.governance;

import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.DictionaryMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.GapTaskMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.IdentifierLineageMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.TimeSemanticSelectorMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.DictionaryPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.GapTaskPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.IdentifierLineagePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.TimeSemanticSelectorPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SceneGovernanceGateAppServiceTest {

    @Mock
    private DictionaryMapper dictionaryMapper;

    @Mock
    private IdentifierLineageMapper identifierLineageMapper;

    @Mock
    private TimeSemanticSelectorMapper timeSemanticSelectorMapper;

    @Mock
    private GapTaskMapper gapTaskMapper;

    private SceneGovernanceGateAppService service;

    @BeforeEach
    void setUp() {
        service = new SceneGovernanceGateAppService(
                dictionaryMapper,
                identifierLineageMapper,
                timeSemanticSelectorMapper,
                gapTaskMapper
        );
    }

    @Test
    void shouldCreateOpenGovernanceGapsWhenThreeRequiredAssetsAreMissing() {
        when(dictionaryMapper.findBySceneIdOrderByUpdatedAtDesc(21L)).thenReturn(List.of());
        when(identifierLineageMapper.findBySceneIdOrderByUpdatedAtDesc(21L)).thenReturn(List.of());
        when(timeSemanticSelectorMapper.findBySceneIdOrderByUpdatedAtDesc(21L)).thenReturn(List.of());
        when(gapTaskMapper.findBySceneIdOrderByUpdatedAtDesc(21L)).thenReturn(List.of());
        when(gapTaskMapper.findByTaskCode(any())).thenReturn(Optional.empty());

        var summary = service.evaluateAndSync(21L, SceneGovernanceGateAppService.STAGE_IMPORT_CONFIRM, "tester");

        assertThat(summary.publishReady()).isFalse();
        assertThat(summary.failedRules()).hasSize(3);
        ArgumentCaptor<GapTaskPO> captor = ArgumentCaptor.forClass(GapTaskPO.class);
        verify(gapTaskMapper, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(task -> "OPEN".equals(task.getStatus()));
    }

    @Test
    void shouldPassWhenGovernanceAssetsAlreadyExistAndNoBlockingGapRemains() {
        when(dictionaryMapper.findBySceneIdOrderByUpdatedAtDesc(22L)).thenReturn(List.of(activeDictionary(22L)));
        when(identifierLineageMapper.findBySceneIdOrderByUpdatedAtDesc(22L)).thenReturn(List.of(activeLineage(22L)));
        when(timeSemanticSelectorMapper.findBySceneIdOrderByUpdatedAtDesc(22L)).thenReturn(List.of(activeTimeSemantic(22L)));
        when(gapTaskMapper.findBySceneIdOrderByUpdatedAtDesc(22L)).thenReturn(List.of());

        var summary = service.summarize(22L, SceneGovernanceGateAppService.STAGE_PRE_PUBLISH);

        assertThat(summary.publishReady()).isTrue();
        assertThat(summary.failedRules()).isEmpty();
        assertThat(summary.openBlockingGaps()).isEmpty();
        assertThat(summary.summary()).contains("已通过");
    }

    @Test
    void shouldRejectPublishWhenGovernanceSummaryHasBlockingFailures() {
        when(dictionaryMapper.findBySceneIdOrderByUpdatedAtDesc(23L)).thenReturn(List.of());
        when(identifierLineageMapper.findBySceneIdOrderByUpdatedAtDesc(23L)).thenReturn(List.of());
        when(timeSemanticSelectorMapper.findBySceneIdOrderByUpdatedAtDesc(23L)).thenReturn(List.of());
        when(gapTaskMapper.findBySceneIdOrderByUpdatedAtDesc(23L)).thenReturn(List.of());
        when(gapTaskMapper.findByTaskCode(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assertPublishable(23L, "reviewer"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("治理规则未通过")
                .hasMessageContaining("字典治理对象");
    }

    private DictionaryPO activeDictionary(Long sceneId) {
        DictionaryPO po = new DictionaryPO();
        po.setSceneId(sceneId);
        po.setStatus("ACTIVE");
        po.setReleaseStatus("PUBLISHED");
        return po;
    }

    private IdentifierLineagePO activeLineage(Long sceneId) {
        IdentifierLineagePO po = new IdentifierLineagePO();
        po.setSceneId(sceneId);
        po.setStatus("ACTIVE");
        po.setConfirmationStatus("CONFIRMED");
        return po;
    }

    private TimeSemanticSelectorPO activeTimeSemantic(Long sceneId) {
        TimeSemanticSelectorPO po = new TimeSemanticSelectorPO();
        po.setSceneId(sceneId);
        po.setStatus("ACTIVE");
        po.setDefaultSemantic("交易日期");
        return po;
    }
}
