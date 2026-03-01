package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.PublishSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.SceneListQuery;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneMinimumUnitCheckDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneMinimumUnitDefinitionDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.service.command.SceneCommandAppService;
import com.cmbchina.datadirect.caliber.application.service.query.SceneQueryAppService;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.SecurityOperator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scenes")
@Validated
public class SceneController {

    private final SceneCommandAppService sceneCommandAppService;
    private final SceneQueryAppService sceneQueryAppService;

    public SceneController(SceneCommandAppService sceneCommandAppService, SceneQueryAppService sceneQueryAppService) {
        this.sceneCommandAppService = sceneCommandAppService;
        this.sceneQueryAppService = sceneQueryAppService;
    }

    @PostMapping
    public ResponseEntity<SceneDTO> create(@Valid @RequestBody CreateSceneCmd cmd) {
        CreateSceneCmd payload = new CreateSceneCmd(
                cmd.sceneTitle(),
                cmd.domainId(),
                cmd.domain(),
                cmd.rawInput(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(sceneCommandAppService.create(payload));
    }

    @GetMapping
    public ResponseEntity<List<SceneDTO>> list(@RequestParam(required = false) Long domainId,
                                                @RequestParam(required = false) String domain,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(sceneQueryAppService.list(new SceneListQuery(domainId, domain, status, keyword)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SceneDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sceneQueryAppService.getById(id));
    }

    @GetMapping("/minimum-unit")
    public ResponseEntity<SceneMinimumUnitDefinitionDTO> minimumUnitDefinition() {
        return ResponseEntity.ok(sceneQueryAppService.minimumUnitDefinition());
    }

    @GetMapping("/{id}/minimum-unit-check")
    public ResponseEntity<SceneMinimumUnitCheckDTO> minimumUnitCheck(@PathVariable Long id) {
        return ResponseEntity.ok(sceneQueryAppService.checkMinimumUnit(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SceneDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateSceneCmd cmd) {
        UpdateSceneCmd payload = new UpdateSceneCmd(
                cmd.sceneTitle(),
                cmd.domainId(),
                cmd.domain(),
                cmd.sceneDescription(),
                cmd.caliberDefinition(),
                cmd.applicability(),
                cmd.boundaries(),
                cmd.inputsJson(),
                cmd.outputsJson(),
                cmd.sqlVariantsJson(),
                cmd.codeMappingsJson(),
                cmd.contributors(),
                cmd.sqlBlocksJson(),
                cmd.sourceTablesJson(),
                cmd.caveatsJson(),
                cmd.unmappedText(),
                cmd.qualityJson(),
                cmd.rawInput(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.ok(sceneCommandAppService.update(id, payload));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<SceneDTO> publish(@PathVariable Long id, @Valid @RequestBody PublishSceneCmd cmd) {
        PublishSceneCmd payload = new PublishSceneCmd(
                cmd.verifiedAt(),
                cmd.changeSummary(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.ok(sceneCommandAppService.publish(id, payload));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDraft(@PathVariable Long id) {
        sceneCommandAppService.deleteDraft(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/discard")
    public ResponseEntity<SceneDTO> discard(@PathVariable Long id) {
        return ResponseEntity.ok(sceneCommandAppService.discard(id, SecurityOperator.currentOperator("")));
    }
}
