package com.cmbchina.datadirect.caliber.infrastructure.common.security;

import com.cmbchina.datadirect.caliber.infrastructure.common.config.CaliberSecurityProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    @Test
    void shouldFailFastWhenJwtSecretIsTooShort() {
        CaliberSecurityProperties properties = new CaliberSecurityProperties();
        properties.setJwtSecret("short-secret");
        properties.setUsers(List.of());

        JwtTokenService service = new JwtTokenService(properties);

        assertThatThrownBy(() -> service.issueToken("support", List.of("SUPPORT")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt-secret");
    }
}
