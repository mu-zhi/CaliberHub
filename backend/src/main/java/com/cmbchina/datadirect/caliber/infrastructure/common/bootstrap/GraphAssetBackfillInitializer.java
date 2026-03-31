package com.cmbchina.datadirect.caliber.infrastructure.common.bootstrap;

import com.cmbchina.datadirect.caliber.application.service.command.graphrag.SceneGraphAssetSyncService;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class GraphAssetBackfillInitializer implements ApplicationRunner {

    private final SceneMapper sceneMapper;
    private final PlanMapper planMapper;
    private final SceneGraphAssetSyncService sceneGraphAssetSyncService;

    public GraphAssetBackfillInitializer(SceneMapper sceneMapper,
                                         PlanMapper planMapper,
                                         SceneGraphAssetSyncService sceneGraphAssetSyncService) {
        this.sceneMapper = sceneMapper;
        this.planMapper = planMapper;
        this.sceneGraphAssetSyncService = sceneGraphAssetSyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!planMapper.findAll().isEmpty()) {
            return;
        }
        sceneMapper.findAll().forEach(scene -> {
            if ((scene.getSqlVariantsJson() != null && !scene.getSqlVariantsJson().isBlank())
                    || (scene.getSqlBlocksJson() != null && !scene.getSqlBlocksJson().isBlank())) {
                sceneGraphAssetSyncService.syncSceneAssetsFromLegacy(scene, "system");
            }
        });
    }
}
