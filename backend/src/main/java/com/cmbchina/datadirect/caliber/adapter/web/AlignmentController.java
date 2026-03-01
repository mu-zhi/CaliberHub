package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateAlignmentReportCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.AlignmentReportDTO;
import com.cmbchina.datadirect.caliber.application.service.command.AlignmentReportAppService;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.SecurityOperator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alignment")
@Validated
public class AlignmentController {

    private final AlignmentReportAppService alignmentReportAppService;

    public AlignmentController(AlignmentReportAppService alignmentReportAppService) {
        this.alignmentReportAppService = alignmentReportAppService;
    }

    @PostMapping("/reports/{sceneId}")
    public ResponseEntity<AlignmentReportDTO> createReport(@PathVariable Long sceneId,
                                                           @Valid @RequestBody CreateAlignmentReportCmd cmd) {
        CreateAlignmentReportCmd payload = new CreateAlignmentReportCmd(
                cmd.status(),
                cmd.message(),
                cmd.tables(),
                cmd.columns(),
                cmd.policies(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.ok(alignmentReportAppService.create(sceneId, payload));
    }

    @GetMapping("/reports/{sceneId}")
    public ResponseEntity<AlignmentReportDTO> latest(@PathVariable Long sceneId) {
        return ResponseEntity.ok(alignmentReportAppService.latest(sceneId));
    }
}

