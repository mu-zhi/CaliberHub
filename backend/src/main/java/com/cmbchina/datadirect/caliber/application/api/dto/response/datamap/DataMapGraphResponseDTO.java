package com.cmbchina.datadirect.caliber.application.api.dto.response.datamap;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(name = "DataMapGraphResponseDTO", description = "数据地图查询主响应，包含根对象、快照与图节点边列表。")
public record DataMapGraphResponseDTO(
        @Schema(description = "根对象标识，例如 scene-1 或 domain-2")
        String rootRef,
        @Schema(description = "根场景 ID")
        Long sceneId,
        @Schema(description = "根场景中文名")
        String sceneName,
        @Schema(description = "快照标识，用于幂等与回放对齐")
        Long snapshotId,
        @Schema(description = "数据来源说明")
        ReadSource readSource,
        @Schema(description = "投影状态")
        ProjectionVerificationStatus projectionVerificationStatus,
        @Schema(description = "投影校验时间")
        OffsetDateTime projectionVerifiedAt,
        @Schema(description = "图节点列表")
        List<DataMapGraphNodeDTO> nodes,
        @Schema(description = "图边列表")
        List<DataMapGraphEdgeDTO> edges
) {
}
