package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.ExportServiceSpecCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ServiceSpecDTO;
import com.cmbchina.datadirect.caliber.application.service.command.ServiceSpecAppService;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.SecurityOperator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-specs")
@Validated
public class ServiceSpecController {

    private final ServiceSpecAppService serviceSpecAppService;

    public ServiceSpecController(ServiceSpecAppService serviceSpecAppService) {
        this.serviceSpecAppService = serviceSpecAppService;
    }

    @PostMapping("/export/{sceneId}")
    public ResponseEntity<ServiceSpecDTO> export(@PathVariable Long sceneId, @RequestBody(required = false) ExportServiceSpecCmd cmd) {
        ExportServiceSpecCmd payload = cmd == null
                ? new ExportServiceSpecCmd(null, SecurityOperator.currentOperator(null))
                : new ExportServiceSpecCmd(cmd.expectedVersion(), SecurityOperator.currentOperator(cmd.operator()));
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceSpecAppService.export(sceneId, payload));
    }

    @GetMapping("/{specCode}")
    public ResponseEntity<ServiceSpecDTO> getByCode(@PathVariable String specCode,
                                                    @RequestParam(required = false) Integer version) {
        return ResponseEntity.ok(serviceSpecAppService.getByCode(specCode, version));
    }
}

