package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityMembershipMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PolicyMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityMembershipPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EvidenceFragmentPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.OutputContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PolicyPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceContractPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CanonicalEntityTestApplication.class)
@ActiveProfiles("test")
@Transactional
class CanonicalEntityResolutionServiceTest {

    @Autowired
    private CanonicalEntityResolutionService resolutionService;

    @Autowired
    private SceneGraphAssetSyncService sceneGraphAssetSyncService;

    @Autowired
    private CanonicalEntityMapper canonicalEntityMapper;

    @Autowired
    private CanonicalEntityMembershipMapper membershipMapper;

    @Autowired
    private SceneMapper sceneMapper;

    @Autowired
    private SourceContractMapper sourceContractMapper;

    @Autowired
    private PolicyMapper policyMapper;

    @Autowired
    private EvidenceFragmentMapper evidenceFragmentMapper;

    @Autowired
    private OutputContractMapper outputContractMapper;

    @Test
    void shouldAutoMergeStableKeysAcrossScenesForAllSupportedAssetTypes() {
        Long sceneA = seedScene("PAYROLL_A", "PAYROLL");
        Long sceneB = seedScene("PAYROLL_B", "PAYROLL");

        seedSourceContract(sceneA, "PAYROLL", "T05_AGN_DTL");
        seedSourceContract(sceneB, "PAYROLL", "T05_AGN_DTL");
        seedPolicy(sceneA, "payroll::masking::default");
        seedPolicy(sceneB, "payroll::masking::default");
        seedEvidence(sceneA, "sql_fragment", "payroll_detail", "line_10");
        seedEvidence(sceneB, "sql_fragment", "payroll_detail", "line_10");
        seedOutputContract(sceneA, "payroll::output::primary");
        seedOutputContract(sceneB, "payroll::output::primary");

        resolutionService.resolveScene(sceneA, "tester");
        resolutionService.resolveScene(sceneB, "tester");

        Map<String, CanonicalEntityPO> entitiesByType = canonicalEntityMapper.findAll().stream()
                .collect(Collectors.toMap(CanonicalEntityPO::getEntityType, Function.identity()));

        assertThat(entitiesByType).hasSize(4);
        assertThat(entitiesByType.get("SOURCE_CONTRACT").getCanonicalKey()).isEqualTo("SRC::PAYROLL::T05_AGN_DTL");
        assertThat(entitiesByType.get("POLICY").getCanonicalKey()).isEqualTo("PLC::PAYROLL::MASKING::DEFAULT");
        assertThat(entitiesByType.get("EVIDENCE").getCanonicalKey()).isEqualTo("EVD::SQL_FRAGMENT::PAYROLL_DETAIL::LINE_10");
        assertThat(entitiesByType.get("OUTPUT_CONTRACT").getCanonicalKey()).isEqualTo("OUT::PAYROLL::OUTPUT::PRIMARY");
        assertThat(entitiesByType.values()).allMatch(entity -> "ACTIVE".equals(entity.getResolutionStatus()));
        assertThat(membershipMapper.findAll())
                .hasSize(8)
                .allMatch(CanonicalEntityMembershipPO::isActiveFlag)
                .allMatch(membership -> "canonical_key".equals(membership.getMatchBasis()));
    }

    @Test
    void shouldKeepAssetsWithoutStableKeysInNeedsReview() {
        Long sceneId = seedScene("PAYROLL_REVIEW", "PAYROLL");

        seedSourceContract(sceneId, null, null);
        seedPolicy(sceneId, "   ");
        seedEvidence(sceneId, "sql_fragment", "payroll_detail", null);
        seedOutputContract(sceneId, null);

        resolutionService.resolveScene(sceneId, "tester");

        List<CanonicalEntityPO> entities = canonicalEntityMapper.findAll();
        assertThat(entities).hasSize(4);
        assertThat(entities).extracting(CanonicalEntityPO::getResolutionStatus)
                .containsOnly("NEEDS_REVIEW");
        assertThat(membershipMapper.findAll())
                .hasSize(4)
                .allMatch(membership -> !membership.isManualOverride())
                .allMatch(membership -> "missing_key".equals(membership.getMatchBasis()));
    }

    @Test
    void shouldKeepCanonicalEntityActiveWhenAtLeastOneMembershipIsStillActive() {
        Long activeSceneId = seedScene("PAYROLL_ACTIVE", "PAYROLL");
        Long deprecatedSceneId = seedScene("PAYROLL_DEPRECATED", "PAYROLL");

        seedPolicy(activeSceneId, "payroll::masking::default", "DRAFT");
        seedPolicy(deprecatedSceneId, "payroll::masking::default", "DEPRECATED");

        resolutionService.resolveScene(activeSceneId, "tester");
        resolutionService.resolveScene(deprecatedSceneId, "tester");

        CanonicalEntityPO entity = canonicalEntityMapper.findAll().stream()
                .filter(item -> "POLICY".equals(item.getEntityType()))
                .findFirst()
                .orElseThrow();

        assertThat(entity.getLifecycleStatus()).isEqualTo("ACTIVE");
        assertThat(membershipMapper.findByCanonicalEntityIdAndActiveFlagTrueOrderByUpdatedAtDesc(entity.getId()))
                .hasSize(1);
    }

    @Test
    void shouldResolveCanonicalEntitiesWhenSyncingSceneAssetsFromLegacy() {
        Long sceneId = seedLegacyScene("PAYROLL_SYNC", "PAYROLL", "PDM_VHIS.T05_AGN_DTL");

        sceneGraphAssetSyncService.syncSceneAssetsFromLegacy(sceneId, "tester");

        assertThat(canonicalEntityMapper.findAll())
                .extracting(CanonicalEntityPO::getEntityType)
                .containsExactlyInAnyOrder("SOURCE_CONTRACT", "POLICY", "EVIDENCE", "OUTPUT_CONTRACT");
        assertThat(membershipMapper.findAll())
                .hasSize(4)
                .allMatch(CanonicalEntityMembershipPO::isActiveFlag);
    }

    @Test
    void shouldDeactivateStaleMembershipsAfterRepeatedLegacySync() {
        Long sceneId = seedLegacyScene("PAYROLL_SYNC_REPEAT", "PAYROLL", "PDM_VHIS.T05_AGN_DTL");

        sceneGraphAssetSyncService.syncSceneAssetsFromLegacy(sceneId, "tester");
        sceneGraphAssetSyncService.syncSceneAssetsFromLegacy(sceneId, "tester");

        List<CanonicalEntityMembershipPO> memberships = membershipMapper.findAll();

        assertThat(memberships).hasSize(7);
        assertThat(memberships.stream().filter(CanonicalEntityMembershipPO::isActiveFlag)).hasSize(4);
        assertThat(memberships.stream()
                .filter(item -> "POLICY".equals(item.getSceneAssetType()) && item.isActiveFlag()))
                .hasSize(1);
        assertThat(memberships.stream()
                .filter(item -> "EVIDENCE".equals(item.getSceneAssetType()) && item.isActiveFlag()))
                .hasSize(1);
        assertThat(memberships.stream()
                .filter(item -> "OUTPUT_CONTRACT".equals(item.getSceneAssetType()) && item.isActiveFlag()))
                .hasSize(1);
    }

    private Long seedScene(String sceneCode, String domain) {
        OffsetDateTime now = OffsetDateTime.now();
        ScenePO scene = new ScenePO();
        scene.setSceneCode(sceneCode);
        scene.setSceneTitle(sceneCode + " title");
        scene.setDomain(domain);
        scene.setSceneType("FACT_DETAIL");
        scene.setStatus(SceneStatus.DRAFT);
        scene.setSceneDescription("desc");
        scene.setCaliberDefinition("definition");
        scene.setApplicability("2024-至今");
        scene.setBoundaries("boundaries");
        scene.setInputsJson("{\"params\":[]}");
        scene.setOutputsJson("{\"summary\":\"summary\",\"fields\":[\"f1\"]}");
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

    private Long seedLegacyScene(String sceneCode, String domain, String sourceTable) {
        OffsetDateTime now = OffsetDateTime.now();
        ScenePO scene = new ScenePO();
        scene.setSceneCode(sceneCode);
        scene.setSceneTitle("代发明细查询");
        scene.setDomain(domain);
        scene.setSceneType("FACT_DETAIL");
        scene.setStatus(SceneStatus.DRAFT);
        scene.setSceneDescription("按协议号查询代发明细");
        scene.setCaliberDefinition("按协议号过滤并返回代发明细");
        scene.setApplicability("2014-至今");
        scene.setBoundaries("仅支持已入湖的代发明细");
        scene.setInputsJson("""
                {"params":[{"name":"PROTOCOL_NBR","type":"TEXT","required":true,"identifiers":["PROTOCOL_NBR"]}]}
                """);
        scene.setOutputsJson("""
                {"summary":"返回代发明细","fields":["协议号","交易日期","金额"]}
                """);
        scene.setSqlVariantsJson(sqlVariantsJson(sourceTable));
        scene.setSqlBlocksJson(sqlVariantsJson(sourceTable));
        scene.setCodeMappingsJson("[]");
        scene.setContributors("tester");
        scene.setSourceTablesJson("[\"" + sourceTable + "\"]");
        scene.setCaveatsJson("[]");
        scene.setUnmappedText("");
        scene.setQualityJson("{}");
        scene.setRawInput("SELECT * FROM " + sourceTable + " WHERE MCH_AGR_NBR = '${PROTOCOL_NBR}'");
        scene.setCreatedBy("tester");
        scene.setCreatedAt(now);
        scene.setUpdatedAt(now);
        return sceneMapper.save(scene).getId();
    }

    private void seedSourceContract(Long sceneId, String sourceSystem, String normalizedPhysicalTable) {
        OffsetDateTime now = OffsetDateTime.now();
        SourceContractPO contract = new SourceContractPO();
        contract.setSceneId(sceneId);
        contract.setSourceContractCode("SRC-" + sceneId + "-" + System.nanoTime());
        contract.setSourceName("source contract");
        contract.setPhysicalTable(normalizedPhysicalTable == null ? "UNKNOWN" : normalizedPhysicalTable);
        contract.setNormalizedPhysicalTable(normalizedPhysicalTable);
        contract.setSourceRole("PRIMARY_QUERY");
        contract.setSourceSystem(sourceSystem);
        contract.setStatus("DRAFT");
        contract.setCreatedBy("tester");
        contract.setCreatedAt(now);
        contract.setUpdatedBy("tester");
        contract.setUpdatedAt(now);
        sourceContractMapper.save(contract);
    }

    private void seedPolicy(Long sceneId, String semanticKey) {
        seedPolicy(sceneId, semanticKey, "DRAFT");
    }

    private void seedPolicy(Long sceneId, String semanticKey, String status) {
        OffsetDateTime now = OffsetDateTime.now();
        PolicyPO policy = new PolicyPO();
        policy.setPolicyCode("PLC-" + sceneId + "-" + System.nanoTime());
        policy.setPolicyName("policy");
        policy.setPolicySemanticKey(semanticKey);
        policy.setScopeType("SCENE");
        policy.setScopeRefId(sceneId);
        policy.setEffectType("ALLOW");
        policy.setStatus(status);
        policy.setCreatedBy("tester");
        policy.setCreatedAt(now);
        policy.setUpdatedBy("tester");
        policy.setUpdatedAt(now);
        policyMapper.save(policy);
    }

    private void seedEvidence(Long sceneId, String originType, String originRef, String originLocator) {
        OffsetDateTime now = OffsetDateTime.now();
        EvidenceFragmentPO evidence = new EvidenceFragmentPO();
        evidence.setSceneId(sceneId);
        evidence.setEvidenceCode("EVD-" + sceneId + "-" + System.nanoTime());
        evidence.setTitle("evidence");
        evidence.setFragmentText("fragment");
        evidence.setSourceAnchor(sceneId.toString());
        evidence.setSourceType(originType);
        evidence.setSourceRef(originRef);
        evidence.setOriginType(originType);
        evidence.setOriginRef(originRef);
        evidence.setOriginLocator(originLocator);
        evidence.setConfidenceScore(0.8d);
        evidence.setStatus("DRAFT");
        evidence.setCreatedBy("tester");
        evidence.setCreatedAt(now);
        evidence.setUpdatedBy("tester");
        evidence.setUpdatedAt(now);
        evidenceFragmentMapper.save(evidence);
    }

    private void seedOutputContract(Long sceneId, String semanticKey) {
        OffsetDateTime now = OffsetDateTime.now();
        OutputContractPO contract = new OutputContractPO();
        contract.setSceneId(sceneId);
        contract.setContractCode("OUT-" + sceneId + "-" + System.nanoTime());
        contract.setContractName("output contract");
        contract.setContractSemanticKey(semanticKey);
        contract.setSummaryText("summary");
        contract.setFieldsJson("[]");
        contract.setMaskingRulesJson("[]");
        contract.setUsageConstraints("");
        contract.setTimeCaliberNote("");
        contract.setStatus("DRAFT");
        contract.setCreatedBy("tester");
        contract.setCreatedAt(now);
        contract.setUpdatedBy("tester");
        contract.setUpdatedAt(now);
        outputContractMapper.save(contract);
    }

    private String sqlVariantsJson(String sourceTable) {
        return """
                [{
                  "variant_name":"主方案",
                  "applicable_period":"2014-至今",
                  "sql_text":"SELECT * FROM %s WHERE MCH_AGR_NBR = '${PROTOCOL_NBR}'",
                  "source_tables":["%s"]
                }]
                """.formatted(sourceTable, sourceTable);
    }

}
