package com.cmbchina.datadirect.caliber.application.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "SceneDTO", description = "场景核心响应模型，覆盖场景定义、版本、状态及发布元信息。")
public record SceneDTO(
        @Schema(description = "场景主键标识")
        Long id,
        @Schema(description = "场景业务编码")
        String sceneCode,
        @Schema(description = "场景标题")
        String sceneTitle,
        @Schema(description = "归属领域 ID")
        Long domainId,
        @Schema(description = "归属领域英文名称")
        String domain,
        @Schema(description = "归属领域中文名称")
        String domainName,
        @Schema(description = "场景类型，来自统一场景类型词典")
        String sceneType,
        @Schema(description = "场景当前状态")
        String status,
        @Schema(description = "场景描述信息")
        String sceneDescription,
        @Schema(description = "场景定义主文案")
        String caliberDefinition,
        @Schema(description = "适用场景边界")
        String applicability,
        @Schema(description = "场景边界与约束信息")
        String boundaries,
        @Schema(description = "场景输入字段定义（JSON 串）")
        String inputsJson,
        @Schema(description = "场景输出字段定义（JSON 串）")
        String outputsJson,
        @Schema(description = "SQL 变体定义（JSON 串）")
        String sqlVariantsJson,
        @Schema(description = "字段映射关系定义（JSON 串）")
        String codeMappingsJson,
        @Schema(description = "场景贡献者列表")
        String contributors,
        @Schema(description = "SQL 块定义（JSON 串）")
        String sqlBlocksJson,
        @Schema(description = "源表信息（JSON 串）")
        String sourceTablesJson,
        @Schema(description = "说明与边界提示（JSON 串）")
        String caveatsJson,
        @Schema(description = "未结构化原始文本")
        String unmappedText,
        @Schema(description = "质量指标与质量控制信息（JSON 串）")
        String qualityJson,
        @Schema(description = "原始输入文本")
        String rawInput,
        @Schema(description = "审定时间")
        OffsetDateTime verifiedAt,
        @Schema(description = "变更说明")
        String changeSummary,
        @Schema(description = "创建者")
        String createdBy,
        @Schema(description = "创建时间")
        OffsetDateTime createdAt,
        @Schema(description = "更新时间")
        OffsetDateTime updatedAt,
        @Schema(description = "发布者")
        String publishedBy,
        @Schema(description = "发布时间")
        OffsetDateTime publishedAt,
        @Schema(description = "乐观锁版本")
        Long rowVersion,
        @Schema(description = "发布快照 ID，发布场景为空时可能为空")
        Long snapshotId
) {
}
