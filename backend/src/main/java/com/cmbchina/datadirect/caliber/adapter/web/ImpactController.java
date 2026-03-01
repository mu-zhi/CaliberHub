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
@RequestMapping("/api/impact")
@Validated
public class ImpactController {

    private final ImpactQueryAppService impactQueryAppService;

    public ImpactController(ImpactQueryAppService impactQueryAppService) {
        this.impactQueryAppService = impactQueryAppService;
    }

    @GetMapping("/scenes/{sceneId}")
    public ResponseEntity<SceneImpactDTO> sceneImpact(@PathVariable Long sceneId) {
        return ResponseEntity.ok(impactQueryAppService.sceneImpact(sceneId));
    }
}
