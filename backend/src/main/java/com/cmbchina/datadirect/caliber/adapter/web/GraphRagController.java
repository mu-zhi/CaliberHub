package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.GraphQueryCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.KnowledgePackageQueryCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.ProjectionRebuildCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.GraphProjectionStatusDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.GraphQueryResultDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.KnowledgePackageDTO;
import com.cmbchina.datadirect.caliber.application.service.command.graphrag.GraphProjectionAppService;
import com.cmbchina.datadirect.caliber.application.service.query.graphrag.GraphRagQueryAppService;
import com.cmbchina.datadirect.caliber.application.service.query.graphrag.KnowledgePackageQueryAppService;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.SecurityOperator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "图谱检索与知识包", description = "知识包查询、场景检索与图谱投影管理")
public class GraphRagController {

    private final KnowledgePackageQueryAppService knowledgePackageQueryAppService;
    private final GraphRagQueryAppService graphRagQueryAppService;
    private final GraphProjectionAppService graphProjectionAppService;

    public GraphRagController(KnowledgePackageQueryAppService knowledgePackageQueryAppService,
                              GraphRagQueryAppService graphRagQueryAppService,
                              GraphProjectionAppService graphProjectionAppService) {
        this.knowledgePackageQueryAppService = knowledgePackageQueryAppService;
        this.graphRagQueryAppService = graphRagQueryAppService;
        this.graphProjectionAppService = graphProjectionAppService;
    }

    @PostMapping("/graphrag/query")
    @Operation(summary = "检索知识包", operationId = "queryKnowledgePackage")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回知识包查询结果",
                    content = @Content(schema = @Schema(implementation = KnowledgePackageDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<KnowledgePackageDTO> query(@RequestBody KnowledgePackageQueryCmd cmd) {
        return ResponseEntity.ok(knowledgePackageQueryAppService.query(withOperator(cmd)));
    }

    @PostMapping("/scene-search")
    @Operation(summary = "场景检索", operationId = "sceneSearch")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回场景检索结果",
                    content = @Content(schema = @Schema(implementation = GraphQueryResultDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<GraphQueryResultDTO> sceneSearch(@RequestBody GraphQueryCmd cmd) {
        GraphQueryCmd payload = new GraphQueryCmd(cmd.queryText(), cmd.mode() == null ? "GLOBAL" : cmd.mode(), cmd.domainId(), cmd.sceneId(), cmd.slotHintsJson(), SecurityOperator.currentOperator(cmd.operator()));
        return ResponseEntity.ok(graphRagQueryAppService.query(payload));
    }

    @PostMapping("/plan-select")
    @Operation(summary = "方案选择", operationId = "planSelect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回方案选择后查询结果",
                    content = @Content(schema = @Schema(implementation = GraphQueryResultDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<GraphQueryResultDTO> planSelect(@RequestBody GraphQueryCmd cmd) {
        GraphQueryCmd payload = new GraphQueryCmd(cmd.queryText(), cmd.mode() == null ? "LOCAL" : cmd.mode(), cmd.domainId(), cmd.sceneId(), cmd.slotHintsJson(), SecurityOperator.currentOperator(cmd.operator()));
        return ResponseEntity.ok(graphRagQueryAppService.query(payload));
    }

    @GetMapping("/graphrag/projection/{sceneId}")
    @Operation(summary = "查询投影状态", operationId = "queryProjectionStatus")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回投影状态",
                    content = @Content(schema = @Schema(implementation = GraphProjectionStatusDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<GraphProjectionStatusDTO> projectionStatus(@PathVariable Long sceneId) {
        return ResponseEntity.ok(graphProjectionAppService.getStatus(sceneId));
    }

    @PostMapping("/graphrag/rebuild/{sceneId}")
    @Operation(summary = "重建投影", operationId = "rebuildProjection")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "重建投影完成，返回最新状态",
                    content = @Content(schema = @Schema(implementation = GraphProjectionStatusDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<GraphProjectionStatusDTO> rebuild(@PathVariable Long sceneId, @RequestBody(required = false) ProjectionRebuildCmd cmd) {
        String operator = cmd == null ? SecurityOperator.currentOperator("") : SecurityOperator.currentOperator(cmd.operator());
        return ResponseEntity.ok(graphProjectionAppService.rebuildProjection(sceneId, operator));
    }

    private GraphQueryCmd withOperator(GraphQueryCmd cmd) {
        return new GraphQueryCmd(cmd.queryText(), cmd.mode(), cmd.domainId(), cmd.sceneId(), cmd.slotHintsJson(), SecurityOperator.currentOperator(cmd.operator()));
    }

    private KnowledgePackageQueryCmd withOperator(KnowledgePackageQueryCmd cmd) {
        return new KnowledgePackageQueryCmd(
                cmd.queryText(),
                cmd.snapshotId(),
                cmd.selectedSceneId(),
                cmd.selectedPlanId(),
                cmd.slotHintsJson(),
                cmd.identifierType(),
                cmd.identifierValue(),
                cmd.dateFrom(),
                cmd.dateTo(),
                cmd.requestedFields(),
                cmd.purpose(),
                SecurityOperator.currentOperator(cmd.operator())
        );
    }
}
