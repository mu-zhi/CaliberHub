package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record FetchColumnResponseDTO(
        String columnId,
        List<MillerNodeDTO> items
) {
}
