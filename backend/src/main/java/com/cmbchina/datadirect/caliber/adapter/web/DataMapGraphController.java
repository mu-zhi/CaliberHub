package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.datamap.DataMapImpactAnalysisCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphResponseDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapImpactAnalysisDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapNodeDetailDTO;
import com.cmbchina.datadirect.caliber.application.service.query.datamap.GraphQueryService;
import com.cmbchina.datadirect.caliber.application.service.query.datamap.ImpactAnalysisService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/datamap")
@Validated
@Tag(name = "数据地图", description = "数据地图图谱查询、节点详情和影响范围分析")
public class DataMapGraphController {

    private final GraphQueryService graphQueryService;
    private final ImpactAnalysisService impactAnalysisService;

    public DataMapGraphController(GraphQueryService graphQueryService,
                                  ImpactAnalysisService impactAnalysisService) {
        this.graphQueryService = graphQueryService;
        this.impactAnalysisService = impactAnalysisService;
    }

    @GetMapping("/graph")
    @Operation(summary = "查询数据地图", operationId = "queryDatamapGraph")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回数据地图图谱",
                    content = @Content(schema = @Schema(implementation = DataMapGraphResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<DataMapGraphResponseDTO> graph(@RequestParam("root_type") @NotBlank String rootType,
                                                         @RequestParam("root_id") @NotNull @Positive Long rootId,
                                                         @RequestParam(name = "snapshot_id", required = false) Long snapshotId,
                                                         @RequestParam(name = "object_types", required = false) String objectTypes,
                                                         @RequestParam(name = "statuses", required = false) String statuses,
                                                         @RequestParam(name = "relation_types", required = false) String relationTypes,
                                                         @RequestParam(name = "sensitivity_scopes", required = false) String sensitivityScopes) {
        return ResponseEntity.ok(graphQueryService.queryGraph(rootType, rootId, snapshotId, objectTypes, statuses, relationTypes, sensitivityScopes));
    }

    @GetMapping("/node/{id}/detail")
    @Operation(summary = "查询数据地图节点详情", operationId = "queryDatamapNodeDetail")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回节点详情",
                    content = @Content(schema = @Schema(implementation = DataMapNodeDetailDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<DataMapNodeDetailDTO> nodeDetail(@PathVariable("id") String assetRef) {
        return ResponseEntity.ok(graphQueryService.queryNodeDetail(assetRef));
    }

    @PostMapping("/impact-analysis")
    @Operation(summary = "分析影响范围", operationId = "analyzeDatamapImpact")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回影响分析结果",
                    content = @Content(schema = @Schema(implementation = DataMapImpactAnalysisDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<DataMapImpactAnalysisDTO> impactAnalysis(@RequestBody DataMapImpactAnalysisCmd cmd) {
        return ResponseEntity.ok(impactAnalysisService.analyze(cmd.assetRef(), cmd.snapshotId()));
    }
}
