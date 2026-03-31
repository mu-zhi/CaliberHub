package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record DomainBootstrapResultDTO(
        int createdCount,
        int totalCount,
        List<DomainDTO> domains
) {
}
