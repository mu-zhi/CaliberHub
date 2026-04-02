package com.cmbchina.datadirect.caliber.application.service.query;

import com.cmbchina.datadirect.caliber.application.api.dto.request.LoginTokenCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.AuthTokenDTO;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.CaliberSecurityProperties;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthQueryAppServiceTest {

    @Test
    void shouldIssueTokenWhenStoredPasswordIsBcryptEncoded() {
        CaliberSecurityProperties properties = buildSecurityProperties("support", new BCryptPasswordEncoder().encode("support123"), List.of("SUPPORT"));
        JwtTokenService jwtTokenService = new JwtTokenService(properties);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthQueryAppService service = new AuthQueryAppService(properties, jwtTokenService, passwordEncoder);

        AuthTokenDTO token = service.issueToken(new LoginTokenCmd("support", "support123"));

        assertThat(token.accessToken()).isNotBlank();
        assertThat(token.username()).isEqualTo("support");
        assertThat(token.roles()).containsExactly("SUPPORT");
    }

    @Test
    void shouldRejectMismatchedPasswordWhenStoredPasswordIsBcryptEncoded() {
        CaliberSecurityProperties properties = buildSecurityProperties("support", new BCryptPasswordEncoder().encode("support123"), List.of("SUPPORT"));
        JwtTokenService jwtTokenService = new JwtTokenService(properties);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthQueryAppService service = new AuthQueryAppService(properties, jwtTokenService, passwordEncoder);

        assertThatThrownBy(() -> service.issueToken(new LoginTokenCmd("support", "wrong-password")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("用户名或密码错误");
    }

    private CaliberSecurityProperties buildSecurityProperties(String username, String password, List<String> roles) {
        CaliberSecurityProperties.UserAccount account = new CaliberSecurityProperties.UserAccount();
        account.setUsername(username);
        account.setPassword(password);
        account.setRoles(roles);

        CaliberSecurityProperties properties = new CaliberSecurityProperties();
        properties.setJwtSecret("test-jwt-secret-for-ut-1234567890");
        properties.setUsers(List.of(account));
        return properties;
    }
}
