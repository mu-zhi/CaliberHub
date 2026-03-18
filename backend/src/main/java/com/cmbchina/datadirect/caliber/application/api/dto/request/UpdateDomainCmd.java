package com.cmbchina.datadirect.caliber.application.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateDomainCmd(
        @NotBlank String domainCode,
        @NotBlank String domainName,
        String domainOverview,
        String commonTables,
        String contacts,
        Integer sortOrder,
        String operator
) {
}
