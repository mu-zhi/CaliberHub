package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.PublishSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.SceneListQuery;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateSceneCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneMinimumUnitCheckDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneMinimumUnitDefinitionDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.governance.SceneGovernanceSummaryDTO;
import com.cmbchina.datadirect.caliber.application.service.command.SceneCommandAppService;
import com.cmbchina.datadirect.caliber.application.service.governance.SceneGovernanceGateAppService;
import com.cmbchina.datadirect.caliber.application.service.query.SceneQueryAppService;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.SecurityOperator;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scenes")
@Validated
@Tag(name = "场景设计", description = "场景定义、版本发布与最小单位定义")
public class SceneController {

    private final SceneCommandAppService sceneCommandAppService;
    private final SceneQueryAppService sceneQueryAppService;
    private final SceneGovernanceGateAppService sceneGovernanceGateAppService;

    public SceneController(SceneCommandAppService sceneCommandAppService,
                           SceneQueryAppService sceneQueryAppService,
                           SceneGovernanceGateAppService sceneGovernanceGateAppService) {
        this.sceneCommandAppService = sceneCommandAppService;
        this.sceneQueryAppService = sceneQueryAppService;
        this.sceneGovernanceGateAppService = sceneGovernanceGateAppService;
    }

    @PostMapping
    @Operation(summary = "创建场景", operationId = "createScene")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "场景创建成功",
                    content = @Content(schema = @Schema(implementation = SceneDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<SceneDTO> create(@Valid @RequestBody CreateSceneCmd cmd) {
        CreateSceneCmd payload = new CreateSceneCmd(
                cmd.sceneTitle(),
                cmd.domainId(),
                cmd.domain(),
                cmd.sceneType(),
                cmd.rawInput(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(sceneCommandAppService.create(payload));
    }

    @GetMapping
    @Operation(summary = "查询场景列表", operationId = "listScenes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回场景列表",
                    content = @Content(schema = @Schema(implementation = SceneDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<List<SceneDTO>> list(@RequestParam(required = false) Long domainId,
                                                @RequestParam(required = false) String domain,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(sceneQueryAppService.list(new SceneListQuery(domainId, domain, status, keyword)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询场景详情", operationId = "getSceneById")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回场景详情",
                    content = @Content(schema = @Schema(implementation = SceneDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<SceneDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sceneQueryAppService.getById(id));
    }

    @GetMapping("/{id}/governance-gaps")
    public ResponseEntity<SceneGovernanceSummaryDTO> governanceSummary(@PathVariable Long id) {
        sceneQueryAppService.getById(id);
        return ResponseEntity.ok(sceneGovernanceGateAppService.summarize(id, SceneGovernanceGateAppService.STAGE_PRE_PUBLISH));
    }

    @GetMapping("/minimum-unit")
    @Operation(summary = "查询最小单位定义", operationId = "getMinimumUnitDefinition")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回最小单位定义",
                    content = @Content(schema = @Schema(implementation = SceneMinimumUnitDefinitionDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<SceneMinimumUnitDefinitionDTO> minimumUnitDefinition() {
        return ResponseEntity.ok(sceneQueryAppService.minimumUnitDefinition());
    }

    @GetMapping("/{id}/minimum-unit-check")
    @Operation(summary = "检查场景最小单位", operationId = "checkMinimumUnit")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回场景最小单位检查结果",
                    content = @Content(schema = @Schema(implementation = SceneMinimumUnitCheckDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<SceneMinimumUnitCheckDTO> minimumUnitCheck(@PathVariable Long id) {
        return ResponseEntity.ok(sceneQueryAppService.checkMinimumUnit(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新场景", operationId = "updateScene")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "场景更新成功",
                    content = @Content(schema = @Schema(implementation = SceneDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<SceneDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateSceneCmd cmd) {
        UpdateSceneCmd payload = new UpdateSceneCmd(
                cmd.sceneTitle(),
                cmd.domainId(),
                cmd.domain(),
                cmd.sceneType(),
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
                cmd.expectedVersion(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.ok(sceneCommandAppService.update(id, payload));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布场景", operationId = "publishScene")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "场景发布成功",
                    content = @Content(schema = @Schema(implementation = SceneDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<SceneDTO> publish(@PathVariable Long id, @Valid @RequestBody PublishSceneCmd cmd) {
        PublishSceneCmd payload = new PublishSceneCmd(
                cmd.verifiedAt(),
                cmd.changeSummary(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.ok(sceneCommandAppService.publish(id, payload));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除草稿场景", operationId = "deleteDraftScene")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "场景删除成功"),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<Void> deleteDraft(@PathVariable Long id) {
        sceneCommandAppService.deleteDraft(id, SecurityOperator.currentOperator(""));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/discard")
    @Operation(summary = "废弃场景", operationId = "discardScene")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "场景废弃成功",
                    content = @Content(schema = @Schema(implementation = SceneDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<SceneDTO> discard(@PathVariable Long id) {
        return ResponseEntity.ok(sceneCommandAppService.discard(id, SecurityOperator.currentOperator("")));
    }
}
