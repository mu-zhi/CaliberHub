package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.NlFeedbackCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.NlQueryCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.NlFeedbackResultDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.NlQueryResultDTO;
import com.cmbchina.datadirect.caliber.application.service.command.NlPlanAppService;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.SecurityOperator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nl")
@Validated
public class NlController {

    private final NlPlanAppService nlPlanAppService;

    public NlController(NlPlanAppService nlPlanAppService) {
        this.nlPlanAppService = nlPlanAppService;
    }

    @PostMapping("/query")
    public ResponseEntity<NlQueryResultDTO> query(@Valid @RequestBody NlQueryCmd cmd) {
        NlQueryCmd payload = new NlQueryCmd(cmd.queryText(), SecurityOperator.currentOperator(cmd.operator()));
        return ResponseEntity.ok(nlPlanAppService.query(payload));
    }

    @PostMapping("/feedback")
    public ResponseEntity<NlFeedbackResultDTO> feedback(@Valid @RequestBody NlFeedbackCmd cmd) {
        return ResponseEntity.ok(nlPlanAppService.feedback(cmd));
    }
}

