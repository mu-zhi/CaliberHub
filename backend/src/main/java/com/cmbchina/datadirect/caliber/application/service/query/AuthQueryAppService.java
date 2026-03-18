package com.cmbchina.datadirect.caliber.application.service.query;

import com.cmbchina.datadirect.caliber.application.api.dto.request.LoginTokenCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.AuthTokenDTO;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.CaliberSecurityProperties;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.JwtTokenService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AuthQueryAppService {

    private final CaliberSecurityProperties caliberSecurityProperties;
    private final JwtTokenService jwtTokenService;

    public AuthQueryAppService(CaliberSecurityProperties caliberSecurityProperties, JwtTokenService jwtTokenService) {
        this.caliberSecurityProperties = caliberSecurityProperties;
        this.jwtTokenService = jwtTokenService;
    }

    public AuthTokenDTO issueToken(LoginTokenCmd cmd) {
        CaliberSecurityProperties.UserAccount account = caliberSecurityProperties.getUsers().stream()
                .filter(item -> item.getUsername() != null && item.getUsername().equals(cmd.username()))
                .findFirst()
                .orElseThrow(() -> new DomainValidationException("用户名或密码错误"));

        String password = account.getPassword() == null ? "" : account.getPassword();
        if (!password.equals(cmd.password())) {
            throw new DomainValidationException("用户名或密码错误");
        }
        List<String> roles = account.getRoles() == null ? List.of() : account.getRoles().stream()
                .map(role -> role == null ? "" : role.trim().toUpperCase())
                .filter(role -> !role.isBlank())
                .distinct()
                .toList();
        String token = jwtTokenService.issueToken(cmd.username(), roles);
        OffsetDateTime expireAt = jwtTokenService.extractExpireAt(token);
        return new AuthTokenDTO(token, "Bearer", expireAt, cmd.username(), roles);
    }
}
