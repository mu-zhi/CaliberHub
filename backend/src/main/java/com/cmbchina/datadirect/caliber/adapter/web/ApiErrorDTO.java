package com.cmbchina.datadirect.caliber.adapter.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "ApiErrorDTO", description = "统一错误响应模型，承载稳定错误码、可读消息与请求追踪标识")
public record ApiErrorDTO(
        @Schema(description = "错误码，供消费方与日志统一识别")
        String code,
        @Schema(description = "错误说明文案，建议结合 errorCode 做自动化展示与降级")
        String message,
        @Schema(description = "请求追踪标识，用于调用链定位与日志联查")
        String requestId,
        @Schema(description = "错误发生时间，ISO-8601 格式")
        OffsetDateTime timestamp
) {
}
