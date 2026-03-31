package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneImpactDTO;
import com.cmbchina.datadirect.caliber.application.service.query.ImpactQueryAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/impact-analysis")
@Validated
public class ImpactAnalysisController {

    private final ImpactQueryAppService impactQueryAppService;

    public ImpactAnalysisController(ImpactQueryAppService impactQueryAppService) {
        this.impactQueryAppService = impactQueryAppService;
    }

    @GetMapping("/{sceneId}")
    public ResponseEntity<SceneImpactDTO> impact(@PathVariable Long sceneId) {
        return ResponseEntity.ok(impactQueryAppService.sceneImpact(sceneId));
    }
}
