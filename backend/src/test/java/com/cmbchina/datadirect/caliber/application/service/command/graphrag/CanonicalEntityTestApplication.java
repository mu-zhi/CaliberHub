package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.application.service.graphrag.GraphAssetSupport;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityMembershipMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalEntityRelationMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalSnapshotMembershipMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CanonicalSnapshotRelationVisibilityMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ContractViewMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CoverageDeclarationMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EntityAliasMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.InputSlotSchemaMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanEvidenceRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanPolicyRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanSchemaLinkMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PolicyMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceIntakeContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = ScenePO.class)
@EnableJpaRepositories(basePackageClasses = SceneMapper.class)
class CanonicalEntityTestApplication {

    @Bean
    CanonicalKeyFactory canonicalKeyFactory() {
        return new CanonicalKeyFactory();
    }

    @Bean
    GraphAssetSupport graphAssetSupport(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new GraphAssetSupport(objectMapper);
    }

    @Bean
    CanonicalEntityResolutionService canonicalEntityResolutionService(CanonicalKeyFactory canonicalKeyFactory,
                                                                     CanonicalEntityMapper canonicalEntityMapper,
                                                                     CanonicalEntityMembershipMapper membershipMapper,
                                                                     CanonicalEntityRelationMapper relationMapper,
                                                                     SourceContractMapper sourceContractMapper,
                                                                     PolicyMapper policyMapper,
                                                                     EvidenceFragmentMapper evidenceFragmentMapper,
                                                                     OutputContractMapper outputContractMapper) {
        return new CanonicalEntityResolutionService(
                canonicalKeyFactory,
                canonicalEntityMapper,
                membershipMapper,
                relationMapper,
                sourceContractMapper,
                policyMapper,
                evidenceFragmentMapper,
                outputContractMapper
        );
    }

    @Bean
    CanonicalSnapshotBindingService canonicalSnapshotBindingService(CanonicalEntityMembershipMapper membershipMapper,
                                                                   CanonicalSnapshotMembershipMapper snapshotMembershipMapper,
                                                                   CanonicalEntityRelationMapper relationMapper,
                                                                   CanonicalSnapshotRelationVisibilityMapper snapshotRelationVisibilityMapper) {
        return new CanonicalSnapshotBindingService(membershipMapper, snapshotMembershipMapper, relationMapper, snapshotRelationVisibilityMapper);
    }

    @Bean
    SceneGraphAssetSyncService sceneGraphAssetSyncService(PlanMapper planMapper,
                                                          EvidenceFragmentMapper evidenceFragmentMapper,
                                                          CoverageDeclarationMapper coverageDeclarationMapper,
                                                          PolicyMapper policyMapper,
                                                          PlanEvidenceRefMapper planEvidenceRefMapper,
                                                          PlanPolicyRefMapper planPolicyRefMapper,
                                                          EntityAliasMapper entityAliasMapper,
                                                          PlanSchemaLinkMapper planSchemaLinkMapper,
                                                          OutputContractMapper outputContractMapper,
                                                          InputSlotSchemaMapper inputSlotSchemaMapper,
                                                          SourceIntakeContractMapper sourceIntakeContractMapper,
                                                          ContractViewMapper contractViewMapper,
                                                          SourceContractMapper sourceContractMapper,
                                                          SceneMapper sceneMapper,
                                                          GraphAssetSupport graphAssetSupport,
                                                          CanonicalEntityResolutionService canonicalEntityResolutionService) {
        return new SceneGraphAssetSyncService(
                planMapper,
                evidenceFragmentMapper,
                coverageDeclarationMapper,
                policyMapper,
                planEvidenceRefMapper,
                planPolicyRefMapper,
                entityAliasMapper,
                planSchemaLinkMapper,
                outputContractMapper,
                inputSlotSchemaMapper,
                sourceIntakeContractMapper,
                contractViewMapper,
                sourceContractMapper,
                sceneMapper,
                graphAssetSupport,
                canonicalEntityResolutionService
        );
    }
}
