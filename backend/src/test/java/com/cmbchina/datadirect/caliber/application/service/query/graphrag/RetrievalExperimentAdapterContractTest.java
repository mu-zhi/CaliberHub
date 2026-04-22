package com.cmbchina.datadirect.caliber.application.service.query.graphrag;

import com.cmbchina.datadirect.caliber.application.support.RetrievalExperimentSupport;
import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneVersionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.InputSlotSchemaMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneVersionPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EvidenceFragmentPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.InputSlotSchemaPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.supportimpl.application.LightRagRetrievalExperimentSupportImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrievalExperimentAdapterContractTest {

    @Mock
    private SceneMapper sceneMapper;

    @Mock
    private SceneVersionMapper sceneVersionMapper;

    @Mock
    private InputSlotSchemaMapper inputSlotSchemaMapper;

    @Mock
    private EvidenceFragmentMapper evidenceFragmentMapper;

    private RetrievalExperimentSupport retrievalExperimentSupport;

    @BeforeEach
    void setUp() {
        retrievalExperimentSupport = new LightRagRetrievalExperimentSupportImpl(
                sceneMapper,
                sceneVersionMapper,
                inputSlotSchemaMapper,
                evidenceFragmentMapper,
                new ObjectMapper()
        );
    }

    @Test
    void shouldReturnCandidateScenesAndReferencesWithoutFormalDecision() {
        ScenePO scene = publishedScene(1L, "SCN_PAYROLL_DETAIL", "代发明细查询", 10L);
        SceneVersionPO snapshot = publishedSnapshot(42L, scene.getId(), "v1");
        InputSlotSchemaPO protocolSlot = inputSlot(scene.getId(), "PROTOCOL_NBR", "协议号", "[\"PROTOCOL_NBR\"]");
        EvidenceFragmentPO evidence = evidence(scene.getId(), "EV_PAYROLL_001", "代发交易说明", "按协议号查询代发明细", "§3.2");

        when(sceneMapper.findAll()).thenReturn(List.of(scene));
        when(sceneVersionMapper.findTopBySceneIdOrderByVersionNoDesc(scene.getId())).thenReturn(Optional.of(snapshot));
        when(inputSlotSchemaMapper.findAll()).thenReturn(List.of(protocolSlot));
        when(evidenceFragmentMapper.findAll()).thenReturn(List.of(evidence));

        RetrievalExperimentSupport.RetrievalExperimentResult result = retrievalExperimentSupport.retrieve(
                new RetrievalExperimentSupport.RetrievalExperimentRequest(
                        "trace-001",
                        "按协议号查询代发明细",
                        Map.of(
                                "identifierType", "PROTOCOL_NBR",
                                "identifierValue", "AGR-2024-0001",
                                "requestedFields", "协议号,金额"
                        ),
                        10L,
                        null,
                        42L,
                        List.of("协议号", "金额"),
                        "published_evidence_only",
                        "support"
                )
        );

        assertThat(result.candidateScenes()).isNotEmpty();
        assertThat(result.decision()).isNull();
        assertThat(result.referenceRefs()).isNotEmpty();
        assertThat(result.adapterName()).isEqualTo("LightRAG");
        assertThat(result.adapterMetadata()).containsEntry("requestedSnapshotId", "42");
    }

    @Test
    void shouldFallbackToFormalPathWhenAdapterCannotReadPublishedAssets() {
        when(sceneMapper.findAll()).thenThrow(new IllegalStateException("read failed"));

        RetrievalExperimentSupport.RetrievalExperimentResult result = retrievalExperimentSupport.retrieve(
                new RetrievalExperimentSupport.RetrievalExperimentRequest(
                        "trace-002",
                        "按协议号查询代发明细",
                        Map.of("identifierType", "PROTOCOL_NBR"),
                        10L,
                        null,
                        42L,
                        List.of("协议号"),
                        "published_evidence_only",
                        "support"
                )
        );

        assertThat(result.fallbackToFormal()).isTrue();
        assertThat(result.status()).isEqualTo("FALLBACK_FORMAL");
        assertThat(result.candidateScenes()).isEmpty();
        assertThat(result.referenceRefs()).isEmpty();
        assertThat(result.decision()).isNull();
    }

    private ScenePO publishedScene(Long id, String sceneCode, String title, Long domainId) {
        ScenePO scene = new ScenePO();
        scene.setId(id);
        scene.setSceneCode(sceneCode);
        scene.setSceneTitle(title);
        scene.setSceneDescription(title + " 场景");
        scene.setSceneType("FACT_DETAIL");
        scene.setDomainId(domainId);
        scene.setStatus(SceneStatus.PUBLISHED);
        scene.setUpdatedAt(OffsetDateTime.now());
        return scene;
    }

    private SceneVersionPO publishedSnapshot(Long id, Long sceneId, String versionTag) {
        SceneVersionPO snapshot = new SceneVersionPO();
        snapshot.setId(id);
        snapshot.setSceneId(sceneId);
        snapshot.setVersionNo(1);
        snapshot.setVersionTag(versionTag);
        snapshot.setPublishStatus("PUBLISHED");
        snapshot.setPublishedAt(OffsetDateTime.now());
        snapshot.setCreatedBy("support");
        snapshot.setCreatedAt(OffsetDateTime.now());
        snapshot.setSnapshotJson("{}");
        return snapshot;
    }

    private InputSlotSchemaPO inputSlot(Long sceneId, String slotCode, String slotName, String identifiersJson) {
        InputSlotSchemaPO slot = new InputSlotSchemaPO();
        slot.setSceneId(sceneId);
        slot.setSlotCode(slotCode);
        slot.setSlotName(slotName);
        slot.setSlotType("STRING");
        slot.setIdentifierCandidatesJson(identifiersJson);
        return slot;
    }

    private EvidenceFragmentPO evidence(Long sceneId, String evidenceCode, String title, String fragmentText, String sourceAnchor) {
        EvidenceFragmentPO evidence = new EvidenceFragmentPO();
        evidence.setSceneId(sceneId);
        evidence.setEvidenceCode(evidenceCode);
        evidence.setTitle(title);
        evidence.setFragmentText(fragmentText);
        evidence.setSourceAnchor(sourceAnchor);
        evidence.setConfidenceScore(0.91d);
        return evidence;
    }
}
