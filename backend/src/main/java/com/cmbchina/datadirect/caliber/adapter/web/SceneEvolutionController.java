package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.AddSceneReferenceCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateSceneVersionCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDiffDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneReferenceDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneVersionDTO;
import com.cmbchina.datadirect.caliber.application.service.command.SceneReferenceAppService;
import com.cmbchina.datadirect.caliber.application.service.command.SceneVersionAppService;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.SecurityOperator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scenes")
@Validated
public class SceneEvolutionController {

    private final SceneReferenceAppService sceneReferenceAppService;
    private final SceneVersionAppService sceneVersionAppService;

    public SceneEvolutionController(SceneReferenceAppService sceneReferenceAppService,
                                    SceneVersionAppService sceneVersionAppService) {
        this.sceneReferenceAppService = sceneReferenceAppService;
        this.sceneVersionAppService = sceneVersionAppService;
    }

    @PostMapping("/{id}/references")
    public ResponseEntity<SceneReferenceDTO> addReference(@PathVariable Long id, @Valid @RequestBody AddSceneReferenceCmd cmd) {
        AddSceneReferenceCmd payload = new AddSceneReferenceCmd(
                cmd.refType(),
                cmd.refId(),
                cmd.strategy(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.ok(sceneReferenceAppService.add(id, payload));
    }

    @GetMapping("/{id}/references")
    public ResponseEntity<List<SceneReferenceDTO>> listReferences(@PathVariable Long id) {
        return ResponseEntity.ok(sceneReferenceAppService.list(id));
    }

    @PostMapping("/{id}/versions")
    public ResponseEntity<SceneVersionDTO> createVersion(@PathVariable Long id, @Valid @RequestBody CreateSceneVersionCmd cmd) {
        CreateSceneVersionCmd payload = new CreateSceneVersionCmd(
                cmd.changeSummary(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(sceneVersionAppService.create(id, payload));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<SceneVersionDTO>> listVersions(@PathVariable Long id) {
        return ResponseEntity.ok(sceneVersionAppService.list(id));
    }

    @GetMapping("/{id}/diff")
    public ResponseEntity<SceneDiffDTO> diff(@PathVariable Long id,
                                             @RequestParam Integer from,
                                             @RequestParam Integer to) {
        return ResponseEntity.ok(sceneVersionAppService.diff(id, from, to));
    }
}

