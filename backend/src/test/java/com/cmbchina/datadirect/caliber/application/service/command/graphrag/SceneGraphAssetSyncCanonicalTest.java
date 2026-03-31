package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceContractPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CanonicalEntityTestApplication.class)
@ActiveProfiles("test")
@Transactional
class SceneGraphAssetSyncCanonicalTest {

    @Autowired
    private SceneGraphAssetSyncService sceneGraphAssetSyncService;

    @Autowired
    private SceneMapper sceneMapper;

    @Autowired
    private SourceContractMapper sourceContractMapper;

    @Test
    void shouldMarkMissingSourceContractDeprecatedInsteadOfDelete() {
        Long sceneId = seedSceneWithSourceContract("PAYROLL-DETAIL", "PDM_VHIS.T05_AGN_DTL");

        sceneGraphAssetSyncService.syncSceneAssetsFromLegacy(sceneId, "tester");
        List<SourceContractPO> firstContracts = sourceContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        assertThat(firstContracts).hasSize(1);
        Long firstId = firstContracts.get(0).getId();

        rewriteSceneSqlVariants(sceneId, "PDM_VHIS.T99_NEW_TABLE");
        sceneGraphAssetSyncService.syncSceneAssetsFromLegacy(sceneId, "tester");

        List<SourceContractPO> contracts = sourceContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        assertThat(contracts).extracting(SourceContractPO::getStatus)
                .contains("DEPRECATED", "DRAFT");
        assertThat(contracts).extracting(SourceContractPO::getId)
                .contains(firstId);
    }

    private Long seedSceneWithSourceContract(String sceneCode, String sourceTable) {
        OffsetDateTime now = OffsetDateTime.now();
        ScenePO scene = new ScenePO();
        scene.setSceneCode(sceneCode);
        scene.setSceneTitle("代发明细查询");
        scene.setDomain("PAYROLL");
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

    private void rewriteSceneSqlVariants(Long sceneId, String sourceTable) {
        ScenePO scene = sceneMapper.findById(sceneId).orElseThrow();
        scene.setSqlVariantsJson(sqlVariantsJson(sourceTable));
        scene.setSqlBlocksJson(sqlVariantsJson(sourceTable));
        scene.setSourceTablesJson("[\"" + sourceTable + "\"]");
        scene.setUpdatedAt(OffsetDateTime.now());
        sceneMapper.save(scene);
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
