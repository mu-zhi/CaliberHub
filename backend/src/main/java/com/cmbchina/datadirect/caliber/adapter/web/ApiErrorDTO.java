package com.cmbchina.datadirect.caliber.adapter.web;

import java.time.OffsetDateTime;

public record ApiErrorDTO(
        String code,
        String message,
        String requestId,
        OffsetDateTime timestamp
) {
}
