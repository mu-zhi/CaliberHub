package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateDomainCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateDomainCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.DomainBootstrapResultDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.DomainDTO;
import com.cmbchina.datadirect.caliber.application.service.command.DomainCommandAppService;
import com.cmbchina.datadirect.caliber.application.service.query.DomainQueryAppService;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.SecurityOperator;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/domains")
@Validated
@Tag(name = "业务领域", description = "场景与知识生产依赖的业务领域管理")
public class DomainController {

    private final DomainQueryAppService domainQueryAppService;
    private final DomainCommandAppService domainCommandAppService;

    public DomainController(DomainQueryAppService domainQueryAppService,
                            DomainCommandAppService domainCommandAppService) {
        this.domainQueryAppService = domainQueryAppService;
        this.domainCommandAppService = domainCommandAppService;
    }

    @GetMapping
    @Operation(summary = "查询业务领域列表", operationId = "listDomains")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回业务领域列表",
                    content = @Content(schema = @Schema(implementation = DomainDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<List<DomainDTO>> list() {
        return ResponseEntity.ok(domainQueryAppService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询业务领域详情", operationId = "getDomainById")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回领域详情",
                    content = @Content(schema = @Schema(implementation = DomainDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<DomainDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(domainQueryAppService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建业务领域", operationId = "createDomain")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "领域创建成功",
                    content = @Content(schema = @Schema(implementation = DomainDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<DomainDTO> create(@Valid @RequestBody CreateDomainCmd cmd) {
        CreateDomainCmd payload = new CreateDomainCmd(
                cmd.domainCode(),
                cmd.domainName(),
                cmd.domainOverview(),
                cmd.commonTables(),
                cmd.contacts(),
                cmd.sortOrder(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(domainCommandAppService.create(payload));
    }

    @PostMapping("/bootstrap-from-categories")
    @Operation(summary = "初始化业务领域字典", operationId = "bootstrapFromCategories")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "初始化完成",
                    content = @Content(schema = @Schema(implementation = DomainBootstrapResultDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<DomainBootstrapResultDTO> bootstrapFromCategories() {
        return ResponseEntity.ok(
                domainCommandAppService.bootstrapFromBusinessCategories(SecurityOperator.currentOperator("system"))
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新业务领域", operationId = "updateDomain")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "领域更新成功",
                    content = @Content(schema = @Schema(implementation = DomainDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "系统内部错误",
                    content = @Content(schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<DomainDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateDomainCmd cmd) {
        UpdateDomainCmd payload = new UpdateDomainCmd(
                cmd.domainCode(),
                cmd.domainName(),
                cmd.domainOverview(),
                cmd.commonTables(),
                cmd.contacts(),
                cmd.sortOrder(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.ok(domainCommandAppService.update(id, payload));
    }
}
