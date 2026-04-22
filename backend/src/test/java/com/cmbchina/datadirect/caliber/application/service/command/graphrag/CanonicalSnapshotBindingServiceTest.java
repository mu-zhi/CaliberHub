package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.application.api.dto.request.PublishSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.assembler.SceneAssembler;
import com.cmbchina.datadirect.caliber.application.service.command.AlignmentReportAppService;
import com.cmbchina.datadirect.caliber.application.service.command.CaliberDictSyncService;
import com.cmbchina.datadirect.caliber.application.service.command.SceneCommandAppService;
import com.cmbchina.datadirect.caliber.application.service.command.SceneVersionAppService;
import com.cmbchina.datadirect.caliber.application.service.command.graphrag.GraphProjectionAppService;
import com.cmbchina.datadirect.caliber.application.service.command.graphrag.SceneGraphAssetSyncService;
import com.cmbchina.datadirect.caliber.application.service.governance.SceneGovernanceGateAppService;
import com.cmbchina.datadirect.caliber.application.service.query.graphrag.ScenePublishGateAppService;
import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.domain.support.CaliberDomainSupport;
import com.cmbchina.datadirect.caliber.domain.support.SceneDomainSupport;
import com.cmbchina.datadirect.caliber.infrastructure.module.converter.CaliberDomainConverter;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneAuditLogMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneVersionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.DictionaryMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityMembershipMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityRelationMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalSnapshotMembershipMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalSnapshotRelationVisibilityMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.IdentifierLineageMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ExperimentalRetrievalIndexManifestMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.TimeSemanticSelectorMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityMembershipPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityRelationPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalSnapshotMembershipPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalSnapshotRelationVisibilityPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.DictionaryPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.ExperimentalRetrievalIndexManifestPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.IdentifierLineagePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.TimeSemanticSelectorPO;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        CanonicalEntityTestApplication.class,
        CanonicalSnapshotBindingServiceTest.TestSupportConfiguration.class
})
@ActiveProfiles("test")
@Transactional
class CanonicalSnapshotBindingServiceTest {

    @Autowired
    private SceneCommandAppService sceneCommandAppService;

    @Autowired
    private SceneMapper sceneMapper;

    @Autowired
    private CanonicalEntityMapper canonicalEntityMapper;

    @Autowired
    private CanonicalEntityMembershipMapper canonicalEntityMembershipMapper;

    @Autowired
    private CanonicalEntityRelationMapper canonicalEntityRelationMapper;

    @Autowired
    private CanonicalSnapshotMembershipMapper canonicalSnapshotMembershipMapper;

    @Autowired
    private CanonicalSnapshotRelationVisibilityMapper canonicalSnapshotRelationVisibilityMapper;

    @Autowired
    private DictionaryMapper dictionaryMapper;

    @Autowired
    private IdentifierLineageMapper identifierLineageMapper;

    @Autowired
    private TimeSemanticSelectorMapper timeSemanticSelectorMapper;

    @Autowired
    private ExperimentalRetrievalIndexManifestMapper experimentalRetrievalIndexManifestMapper;

    @Autowired
    private ExperimentalRetrievalIndexSyncService experimentalRetrievalIndexSyncService;

    @MockBean
    private CaliberDictSyncService caliberDictSyncService;

    @MockBean
    private AlignmentReportAppService alignmentReportAppService;

    @MockBean
    private SceneGraphAssetSyncService sceneGraphAssetSyncService;

    @MockBean
    private ScenePublishGateAppService scenePublishGateAppService;

    @MockBean
    private GraphProjectionAppService graphProjectionAppService;

    @MockBean
    private SceneGovernanceGateAppService sceneGovernanceGateAppService;

    @MockBean
    private SceneAuditLogMapper sceneAuditLogMapper;

    @Test
    void shouldFreezeActiveCanonicalMembershipsIntoSnapshotWhenPublishingScene() {
        Long sceneId = seedScene();
        CanonicalEntityPO canonicalEntity = seedCanonicalEntity();
        CanonicalEntityMembershipPO activeMembership = seedMembership(sceneId, canonicalEntity.getId(), 101L, true);
        seedMembership(sceneId, canonicalEntity.getId(), 102L, false);
        seedRequiredGovernanceAssets(sceneId);

        SceneDTO published = sceneCommandAppService.publish(sceneId, new PublishSceneCmd(
                OffsetDateTime.now(),
                "首次发布",
                "publisher"
        ));

        List<CanonicalSnapshotMembershipPO> frozenMemberships =
                canonicalSnapshotMembershipMapper.findBySnapshotIdAndSceneIdOrderByUpdatedAtDesc(published.snapshotId(), sceneId);

        assertThat(published.snapshotId()).isNotNull();
        assertThat(frozenMemberships).hasSize(1);
        assertThat(frozenMemberships.get(0).getCanonicalEntityId()).isEqualTo(canonicalEntity.getId());
        assertThat(frozenMemberships.get(0).getSourceMembershipId()).isEqualTo(activeMembership.getId());
        assertThat(frozenMemberships.get(0).getSceneAssetType()).isEqualTo("SOURCE_CONTRACT");
        assertThat(frozenMemberships.get(0).getSceneAssetId()).isEqualTo(101L);
    }

    @Test
    void shouldFreezeVisibleCanonicalRelationsIntoSnapshotWhenPublishingScene() {
        Long sceneId = seedScene();
        CanonicalEntityPO outputEntity = seedCanonicalEntity("OUTPUT_CONTRACT", "OUT::PAYROLL::DETAIL", "代发明细输出");
        CanonicalEntityPO evidenceEntity = seedCanonicalEntity("EVIDENCE", "EVD::DOC::mvp#sql", "代发明细证据");
        seedMembership(sceneId, outputEntity.getId(), 201L, true, "OUTPUT_CONTRACT");
        seedMembership(sceneId, evidenceEntity.getId(), 202L, true, "EVIDENCE");
        CanonicalEntityRelationPO relation = seedRelation(outputEntity.getId(), evidenceEntity.getId(), "SUPPORTED_BY");
        seedRequiredGovernanceAssets(sceneId);

        SceneDTO published = sceneCommandAppService.publish(sceneId, new PublishSceneCmd(
                OffsetDateTime.now(),
                "冻结统一关系",
                "publisher"
        ));

        List<CanonicalSnapshotRelationVisibilityPO> frozenRelations =
                canonicalSnapshotRelationVisibilityMapper.findBySnapshotIdAndSceneIdOrderByUpdatedAtDesc(published.snapshotId(), sceneId);

        assertThat(published.snapshotId()).isNotNull();
        assertThat(frozenRelations).hasSize(1);
        assertThat(frozenRelations.get(0).getCanonicalRelationId()).isEqualTo(relation.getId());
        assertThat(frozenRelations.get(0).getSourceCanonicalEntityId()).isEqualTo(outputEntity.getId());
        assertThat(frozenRelations.get(0).getTargetCanonicalEntityId()).isEqualTo(evidenceEntity.getId());
        assertThat(frozenRelations.get(0).getRelationType()).isEqualTo("SUPPORTED_BY");
    }

    @Test
    void shouldPersistPublishedSnapshotIndexManifestWhenPublishingScene() {
        Long sceneId = seedScene();
        seedRequiredGovernanceAssets(sceneId);

        SceneDTO published = sceneCommandAppService.publish(sceneId, new PublishSceneCmd(
                OffsetDateTime.now(),
                "冻结检索索引版本",
                "publisher"
        ));

        ExperimentalRetrievalIndexManifestPO manifest = experimentalRetrievalIndexManifestMapper
                .findBySceneIdAndSnapshotId(sceneId, published.snapshotId())
                .orElseThrow();

        assertThat(manifest.getSnapshotId()).isEqualTo(published.snapshotId());
        assertThat(manifest.getSourceStatus()).isEqualTo("PUBLISHED");
        assertThat(manifest.getDraftLeakCount()).isZero();
        assertThat(experimentalRetrievalIndexSyncService.resolveLockedIndexVersion(sceneId, published.snapshotId()))
                .contains(manifest.getIndexVersion());
        assertThat(experimentalRetrievalIndexSyncService.resolveLockedIndexVersion(sceneId, published.snapshotId() + 1))
                .isEmpty();
    }

    private Long seedScene() {
        OffsetDateTime now = OffsetDateTime.now();
        ScenePO scene = new ScenePO();
        scene.setSceneCode("SCN-FREEZE-0001");
        scene.setSceneTitle("冻结成员归属");
        scene.setDomain("零售金融");
        scene.setSceneType("FACT_DETAIL");
        scene.setStatus(SceneStatus.DRAFT);
        scene.setSceneDescription("用于验证发布时冻结 canonical memberships");
        scene.setCaliberDefinition("definition");
        scene.setApplicability("2024-至今");
        scene.setBoundaries("boundaries");
        scene.setInputsJson("{\"params\":[]}");
        scene.setOutputsJson("{\"summary\":\"summary\",\"fields\":[]}");
        scene.setSqlVariantsJson("[]");
        scene.setSqlBlocksJson("[]");
        scene.setCodeMappingsJson("[]");
        scene.setContributors("tester");
        scene.setSourceTablesJson("[]");
        scene.setCaveatsJson("[]");
        scene.setUnmappedText("");
        scene.setQualityJson("{}");
        scene.setRawInput("raw");
        scene.setCreatedBy("tester");
        scene.setCreatedAt(now);
        scene.setUpdatedAt(now);
        return sceneMapper.save(scene).getId();
    }

    private CanonicalEntityPO seedCanonicalEntity() {
        return seedCanonicalEntity("SOURCE_CONTRACT", "SRC::PAYROLL::T05_AGN_DTL", "T05_AGN_DTL");
    }

    private CanonicalEntityPO seedCanonicalEntity(String entityType, String canonicalKey, String displayName) {
        OffsetDateTime now = OffsetDateTime.now();
        CanonicalEntityPO entity = new CanonicalEntityPO();
        entity.setEntityType(entityType);
        entity.setCanonicalKey(canonicalKey);
        entity.setDisplayName(displayName);
        entity.setResolutionStatus("ACTIVE");
        entity.setLifecycleStatus("ACTIVE");
        entity.setProfileJson("{\"canonicalKey\":\"" + canonicalKey + "\"}");
        entity.setCreatedBy("tester");
        entity.setCreatedAt(now);
        entity.setUpdatedBy("tester");
        entity.setUpdatedAt(now);
        return canonicalEntityMapper.save(entity);
    }

    private CanonicalEntityMembershipPO seedMembership(Long sceneId, Long canonicalEntityId, Long sceneAssetId, boolean active) {
        return seedMembership(sceneId, canonicalEntityId, sceneAssetId, active, "SOURCE_CONTRACT");
    }

    private CanonicalEntityMembershipPO seedMembership(Long sceneId,
                                                       Long canonicalEntityId,
                                                       Long sceneAssetId,
                                                       boolean active,
                                                       String sceneAssetType) {
        OffsetDateTime now = OffsetDateTime.now();
        CanonicalEntityMembershipPO membership = new CanonicalEntityMembershipPO();
        membership.setCanonicalEntityId(canonicalEntityId);
        membership.setSceneAssetType(sceneAssetType);
        membership.setSceneAssetId(sceneAssetId);
        membership.setSceneId(sceneId);
        membership.setMatchBasis("canonical_key");
        membership.setConfidenceScore(1.0d);
        membership.setManualOverride(false);
        membership.setActiveFlag(active);
        membership.setCreatedBy("tester");
        membership.setCreatedAt(now);
        membership.setUpdatedBy("tester");
        membership.setUpdatedAt(now);
        return canonicalEntityMembershipMapper.save(membership);
    }

    private CanonicalEntityRelationPO seedRelation(Long sourceCanonicalEntityId,
                                                   Long targetCanonicalEntityId,
                                                   String relationType) {
        OffsetDateTime now = OffsetDateTime.now();
        CanonicalEntityRelationPO relation = new CanonicalEntityRelationPO();
        relation.setSourceCanonicalEntityId(sourceCanonicalEntityId);
        relation.setTargetCanonicalEntityId(targetCanonicalEntityId);
        relation.setRelationType(relationType);
        relation.setRelationLabel(relationType);
        relation.setRelationPayloadJson(null);
        relation.setVisibleInSnapshotBinding(true);
        relation.setCreatedBy("tester");
        relation.setCreatedAt(now);
        relation.setUpdatedBy("tester");
        relation.setUpdatedAt(now);
        return canonicalEntityRelationMapper.save(relation);
    }

    private void seedRequiredGovernanceAssets(Long sceneId) {
        OffsetDateTime now = OffsetDateTime.now();

        DictionaryPO dictionary = new DictionaryPO();
        dictionary.setSceneId(sceneId);
        dictionary.setDictCode("DICT-" + sceneId + "-MAIN");
        dictionary.setDictName("客户状态字典");
        dictionary.setDictCategory("ENUM");
        dictionary.setDictVersion("v1");
        dictionary.setReleaseStatus("PUBLISHED");
        dictionary.setEntriesJson("[{\"code\":\"01\",\"name\":\"正常\"}]");
        dictionary.setReferencedByJson("[\"output_contract:main\"]");
        stamp(dictionary, now);
        dictionaryMapper.save(dictionary);

        IdentifierLineagePO lineage = new IdentifierLineagePO();
        lineage.setSceneId(sceneId);
        lineage.setLineageCode("LIN-" + sceneId + "-MAIN");
        lineage.setLineageName("客户号映射链");
        lineage.setIdentifierType("CUST_ID");
        lineage.setSourceIdentifierType("SRC_CUST_ID");
        lineage.setTargetIdentifierType("CUST_ID");
        lineage.setMappingRulesJson("[{\"rule\":\"trim\"}]");
        lineage.setEvidenceRefsJson("[\"evidence:integration-test\"]");
        lineage.setConfirmationStatus("CONFIRMED");
        stamp(lineage, now);
        identifierLineageMapper.save(lineage);

        TimeSemanticSelectorPO selector = new TimeSemanticSelectorPO();
        selector.setSceneId(sceneId);
        selector.setSelectorCode("TIME-" + sceneId + "-MAIN");
        selector.setSelectorName("时间语义选择器");
        selector.setDefaultSemantic("交易日期");
        selector.setCandidateSemanticsJson("[\"交易日期\",\"记账日期\"]");
        selector.setClarificationTermsJson("[\"当日\",\"最近\"]");
        selector.setPriorityRulesJson("[{\"priority\":1,\"semantic\":\"交易日期\"}]");
        selector.setMustClarifyFlag(false);
        stamp(selector, now);
        timeSemanticSelectorMapper.save(selector);
    }

    private void stamp(com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.AbstractSnapshotGraphAuditablePO po,
                       OffsetDateTime now) {
        po.setStatus("ACTIVE");
        po.setSnapshotId(null);
        po.setCreatedBy("tester");
        po.setCreatedAt(now);
        po.setUpdatedBy("tester");
        po.setUpdatedAt(now);
    }

    @TestConfiguration
    static class TestSupportConfiguration {

        @Bean
        SceneDomainSupport sceneDomainSupport(SceneMapper sceneMapper) {
            return new SceneDomainSupport() {
                @Override
                public com.cmbchina.datadirect.caliber.domain.model.Scene save(com.cmbchina.datadirect.caliber.domain.model.Scene scene) {
                    ScenePO po = new ScenePO();
                    po.setId(scene.getId());
                    po.setSceneCode(scene.getSceneCode());
                    po.setSceneTitle(scene.getSceneTitle());
                    po.setDomainId(scene.getDomainId());
                    po.setDomain(scene.getDomain());
                    po.setSceneType(scene.getSceneType());
                    po.setStatus(scene.getStatus());
                    po.setSceneDescription(scene.getSceneDescription());
                    po.setCaliberDefinition(scene.getCaliberDefinition());
                    po.setApplicability(scene.getApplicability());
                    po.setBoundaries(scene.getBoundaries());
                    po.setInputsJson(scene.getInputsJson());
                    po.setOutputsJson(scene.getOutputsJson());
                    po.setSqlVariantsJson(scene.getSqlVariantsJson());
                    po.setCodeMappingsJson(scene.getCodeMappingsJson());
                    po.setContributors(scene.getContributors());
                    po.setSqlBlocksJson(scene.getSqlBlocksJson());
                    po.setSourceTablesJson(scene.getSourceTablesJson());
                    po.setCaveatsJson(scene.getCaveatsJson());
                    po.setUnmappedText(scene.getUnmappedText());
                    po.setQualityJson(scene.getQualityJson());
                    po.setRawInput(scene.getRawInput());
                    po.setVerifiedAt(scene.getVerifiedAt());
                    po.setChangeSummary(scene.getChangeSummary());
                    po.setCreatedBy(scene.getCreatedBy());
                    po.setCreatedAt(scene.getCreatedAt());
                    po.setUpdatedAt(scene.getUpdatedAt());
                    po.setPublishedBy(scene.getPublishedBy());
                    po.setPublishedAt(scene.getPublishedAt());
                    po.setRowVersion(scene.getRowVersion());
                    ScenePO saved = sceneMapper.save(po);
                    return toScene(saved);
                }

                @Override
                public java.util.Optional<com.cmbchina.datadirect.caliber.domain.model.Scene> findById(Long id) {
                    return sceneMapper.findById(id).map(this::toScene);
                }

                @Override
                public java.util.List<com.cmbchina.datadirect.caliber.domain.model.Scene> findByCondition(com.cmbchina.datadirect.caliber.domain.model.SceneQueryCondition condition) {
                    return sceneMapper.findByCondition(condition.domainId(), condition.domain(), condition.status(), condition.keyword())
                            .stream()
                            .map(this::toScene)
                            .toList();
                }

                @Override
                public void deleteById(Long id) {
                    sceneMapper.deleteById(id);
                }

                private com.cmbchina.datadirect.caliber.domain.model.Scene toScene(ScenePO po) {
                    return com.cmbchina.datadirect.caliber.domain.model.Scene.builder()
                            .id(po.getId())
                            .sceneCode(po.getSceneCode())
                            .sceneTitle(po.getSceneTitle())
                            .domainId(po.getDomainId())
                            .domain(po.getDomain())
                            .sceneType(po.getSceneType())
                            .status(po.getStatus())
                            .sceneDescription(po.getSceneDescription())
                            .caliberDefinition(po.getCaliberDefinition())
                            .applicability(po.getApplicability())
                            .boundaries(po.getBoundaries())
                            .inputsJson(po.getInputsJson())
                            .outputsJson(po.getOutputsJson())
                            .sqlVariantsJson(po.getSqlVariantsJson())
                            .codeMappingsJson(po.getCodeMappingsJson())
                            .contributors(po.getContributors())
                            .sqlBlocksJson(po.getSqlBlocksJson())
                            .sourceTablesJson(po.getSourceTablesJson())
                            .caveatsJson(po.getCaveatsJson())
                            .unmappedText(po.getUnmappedText())
                            .qualityJson(po.getQualityJson())
                            .rawInput(po.getRawInput())
                            .verifiedAt(po.getVerifiedAt())
                            .changeSummary(po.getChangeSummary())
                            .createdBy(po.getCreatedBy())
                            .createdAt(po.getCreatedAt())
                            .updatedAt(po.getUpdatedAt())
                            .publishedBy(po.getPublishedBy())
                            .publishedAt(po.getPublishedAt())
                            .rowVersion(po.getRowVersion())
                            .build();
                }
            };
        }

        @Bean
        CaliberDomainSupport caliberDomainSupport() {
            return new CaliberDomainSupport() {
                @Override
                public com.cmbchina.datadirect.caliber.domain.model.CaliberDomain save(com.cmbchina.datadirect.caliber.domain.model.CaliberDomain domain) {
                    return domain;
                }

                @Override
                public java.util.Optional<com.cmbchina.datadirect.caliber.domain.model.CaliberDomain> findById(Long id) {
                    return java.util.Optional.empty();
                }

                @Override
                public java.util.List<com.cmbchina.datadirect.caliber.domain.model.CaliberDomain> findAllOrderBySortOrder() {
                    return java.util.List.of();
                }

                @Override
                public boolean existsByDomainCode(String domainCode) {
                    return false;
                }

                @Override
                public boolean existsByDomainCodeAndIdNot(String domainCode, Long id) {
                    return false;
                }
            };
        }

        @Bean
        SceneAssembler sceneAssembler() {
            return new SceneAssembler() {
                @Override
                public com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO toDTO(com.cmbchina.datadirect.caliber.domain.model.Scene scene) {
                    return new com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO(
                            scene.getId(),
                            scene.getSceneCode(),
                            scene.getSceneTitle(),
                            scene.getDomainId(),
                            scene.getDomain(),
                            scene.getDomain(),
                            scene.getSceneType(),
                            scene.getStatus() == null ? null : scene.getStatus().name(),
                            scene.getSceneDescription(),
                            scene.getCaliberDefinition(),
                            scene.getApplicability(),
                            scene.getBoundaries(),
                            scene.getInputsJson(),
                            scene.getOutputsJson(),
                            scene.getSqlVariantsJson(),
                            scene.getCodeMappingsJson(),
                            scene.getContributors(),
                            scene.getSqlBlocksJson(),
                            scene.getSourceTablesJson(),
                            scene.getCaveatsJson(),
                            scene.getUnmappedText(),
                            scene.getQualityJson(),
                            scene.getRawInput(),
                            scene.getVerifiedAt(),
                            scene.getChangeSummary(),
                            scene.getCreatedBy(),
                            scene.getCreatedAt(),
                            scene.getUpdatedAt(),
                            scene.getPublishedBy(),
                            scene.getPublishedAt(),
                            scene.getRowVersion(),
                            null
                    );
                }

                @Override
                public java.util.List<com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO> toDTOList(java.util.List<com.cmbchina.datadirect.caliber.domain.model.Scene> scenes) {
                    return scenes.stream().map(this::toDTO).toList();
                }
            };
        }

        @Bean
        @Primary
        SceneVersionAppService testSceneVersionAppService(SceneMapper sceneMapper,
                                                          SceneVersionMapper sceneVersionMapper,
                                                          com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper planMapper,
                                                          com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper outputContractMapper,
                                                          com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CoverageDeclarationMapper coverageDeclarationMapper,
                                                          com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PolicyMapper policyMapper,
                                                          com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanPolicyRefMapper planPolicyRefMapper,
                                                          com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceIntakeContractMapper sourceIntakeContractMapper,
                                                          com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper evidenceFragmentMapper,
                                                          com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.InputSlotSchemaMapper inputSlotSchemaMapper,
                                                          com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ContractViewMapper contractViewMapper,
                                                          com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceContractMapper sourceContractMapper,
                                                          com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
            return new SceneVersionAppService(
                    sceneMapper,
                    sceneVersionMapper,
                    planMapper,
                    outputContractMapper,
                    coverageDeclarationMapper,
                    policyMapper,
                    planPolicyRefMapper,
                    sourceIntakeContractMapper,
                    evidenceFragmentMapper,
                    inputSlotSchemaMapper,
                    contractViewMapper,
                    sourceContractMapper,
                    objectMapper
            );
        }

        @Bean
        ExperimentalRetrievalIndexSyncService experimentalRetrievalIndexSyncService(
                ExperimentalRetrievalIndexManifestMapper experimentalRetrievalIndexManifestMapper,
                SceneVersionMapper sceneVersionMapper,
                com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
            return new ExperimentalRetrievalIndexSyncService(
                    experimentalRetrievalIndexManifestMapper,
                    sceneVersionMapper,
                    objectMapper
            );
        }

        @Bean
        @Primary
        SceneCommandAppService sceneCommandAppService(SceneDomainSupport sceneDomainSupport,
                                                      CaliberDomainSupport caliberDomainSupport,
                                                      SceneAssembler sceneAssembler,
                                                      CaliberDictSyncService caliberDictSyncService,
                                                      AlignmentReportAppService alignmentReportAppService,
                                                      SceneGraphAssetSyncService sceneGraphAssetSyncService,
                                                      ScenePublishGateAppService scenePublishGateAppService,
                                                      SceneVersionAppService sceneVersionAppService,
                                                      CanonicalSnapshotBindingService canonicalSnapshotBindingService,
                                                      GraphProjectionAppService graphProjectionAppService,
                                                      SceneGovernanceGateAppService sceneGovernanceGateAppService,
                                                      MeterRegistry meterRegistry,
                                                      com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                                                      SceneAuditLogMapper sceneAuditLogMapper) {
            return new SceneCommandAppService(
                    sceneDomainSupport,
                    caliberDomainSupport,
                    sceneAssembler,
                    caliberDictSyncService,
                    alignmentReportAppService,
                    sceneGraphAssetSyncService,
                    scenePublishGateAppService,
                    sceneVersionAppService,
                    canonicalSnapshotBindingService,
                    graphProjectionAppService,
                    sceneGovernanceGateAppService,
                    meterRegistry,
                    objectMapper,
                    sceneAuditLogMapper
            );
        }

        @Bean
        @Primary
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
