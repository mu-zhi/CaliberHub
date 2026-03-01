package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateDomainCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateDomainCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.DomainDTO;
import com.cmbchina.datadirect.caliber.application.service.command.DomainCommandAppService;
import com.cmbchina.datadirect.caliber.application.service.query.DomainQueryAppService;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.SecurityOperator;
import jakarta.validation.Valid;
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
public class DomainController {

    private final DomainQueryAppService domainQueryAppService;
    private final DomainCommandAppService domainCommandAppService;

    public DomainController(DomainQueryAppService domainQueryAppService,
                            DomainCommandAppService domainCommandAppService) {
        this.domainQueryAppService = domainQueryAppService;
        this.domainCommandAppService = domainCommandAppService;
    }

    @GetMapping
    public ResponseEntity<List<DomainDTO>> list() {
        return ResponseEntity.ok(domainQueryAppService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DomainDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(domainQueryAppService.getById(id));
    }

    @PostMapping
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

    @PutMapping("/{id}")
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
