package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.LoginTokenCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.AuthTokenDTO;
import com.cmbchina.datadirect.caliber.application.service.query.AuthQueryAppService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/auth")
@Validated
public class AuthController {

    private final AuthQueryAppService authQueryAppService;

    public AuthController(AuthQueryAppService authQueryAppService) {
        this.authQueryAppService = authQueryAppService;
    }

    @PostMapping("/token")
    public ResponseEntity<AuthTokenDTO> issueToken(@Valid @RequestBody LoginTokenCmd cmd) {
        return ResponseEntity.ok(authQueryAppService.issueToken(cmd));
    }
}
