package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record AuthTokenDTO(
        String accessToken,
        String tokenType,
        OffsetDateTime expireAt,
        String username,
        List<String> roles
) {
}
