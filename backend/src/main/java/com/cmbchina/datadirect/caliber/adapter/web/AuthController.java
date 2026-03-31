package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.LoginTokenCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.AuthTokenDTO;
import com.cmbchina.datadirect.caliber.application.service.query.AuthQueryAppService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/auth")
@Validated
@Tag(name = "系统鉴权", description = "系统身份鉴权与访问令牌管理")
public class AuthController {

    private final AuthQueryAppService authQueryAppService;

    public AuthController(AuthQueryAppService authQueryAppService) {
        this.authQueryAppService = authQueryAppService;
    }

    @PostMapping("/token")
    @Operation(summary = "获取访问令牌", operationId = "issueAuthToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回访问令牌",
                    content = @Content(schema = @Schema(implementation = AuthTokenDTO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数校验失败",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<AuthTokenDTO> issueToken(@Valid @RequestBody LoginTokenCmd cmd) {
        return ResponseEntity.ok(authQueryAppService.issueToken(cmd));
    }
}
