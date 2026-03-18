package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.Map;

public record MillerNodeDTO(
        String id,
        String parentId,
        String label,
        String type,
        boolean hasChildren,
        String status,
        Map<String, Object> meta
) {
}
