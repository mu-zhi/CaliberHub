package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.FetchLlmModelListCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.PreviewLlmPromptCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.TestLlmPreprocessConfigCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateLlmPreprocessConfigCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateLlmPromptConfigCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.LlmPreprocessConfigDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.LlmModelListResultDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.LlmPromptPreviewDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.LlmPromptConfigDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.TestLlmPreprocessConfigResultDTO;
import com.cmbchina.datadirect.caliber.application.service.command.LlmPreprocessConfigCommandAppService;
import com.cmbchina.datadirect.caliber.application.service.query.LlmPreprocessConfigQueryAppService;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.SecurityOperator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/llm-preprocess-config")
@Validated
public class LlmPreprocessConfigController {

    private final LlmPreprocessConfigQueryAppService llmPreprocessConfigQueryAppService;
    private final LlmPreprocessConfigCommandAppService llmPreprocessConfigCommandAppService;

    public LlmPreprocessConfigController(LlmPreprocessConfigQueryAppService llmPreprocessConfigQueryAppService,
                                         LlmPreprocessConfigCommandAppService llmPreprocessConfigCommandAppService) {
        this.llmPreprocessConfigQueryAppService = llmPreprocessConfigQueryAppService;
        this.llmPreprocessConfigCommandAppService = llmPreprocessConfigCommandAppService;
    }

    @GetMapping
    public ResponseEntity<LlmPreprocessConfigDTO> getCurrentConfig() {
        return ResponseEntity.ok(llmPreprocessConfigQueryAppService.getCurrentConfig());
    }

    @PutMapping
    public ResponseEntity<LlmPreprocessConfigDTO> update(@Valid @RequestBody UpdateLlmPreprocessConfigCmd cmd) {
        UpdateLlmPreprocessConfigCmd payload = new UpdateLlmPreprocessConfigCmd(
                cmd.enabled(),
                cmd.endpoint(),
                cmd.model(),
                cmd.timeoutSeconds(),
                cmd.temperature(),
                cmd.maxTokens(),
                cmd.enableThinking(),
                cmd.fallbackToRule(),
                cmd.apiKey(),
                cmd.clearApiKey(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.ok(llmPreprocessConfigCommandAppService.update(payload));
    }

    @PostMapping("/test")
    public ResponseEntity<TestLlmPreprocessConfigResultDTO> test(@Valid @RequestBody TestLlmPreprocessConfigCmd cmd) {
        return ResponseEntity.ok(llmPreprocessConfigCommandAppService.test(cmd));
    }

    @PostMapping("/models")
    public ResponseEntity<LlmModelListResultDTO> listModels(@RequestBody(required = false) FetchLlmModelListCmd cmd) {
        FetchLlmModelListCmd payload = cmd == null ? new FetchLlmModelListCmd(null, null, null) : cmd;
        return ResponseEntity.ok(llmPreprocessConfigCommandAppService.fetchModels(payload));
    }

    @GetMapping("/prompts")
    public ResponseEntity<LlmPromptConfigDTO> getPrompts() {
        return ResponseEntity.ok(llmPreprocessConfigQueryAppService.getCurrentPromptConfig());
    }

    @PutMapping("/prompts")
    public ResponseEntity<LlmPromptConfigDTO> updatePrompts(@Valid @RequestBody UpdateLlmPromptConfigCmd cmd) {
        UpdateLlmPromptConfigCmd payload = new UpdateLlmPromptConfigCmd(
                cmd.preprocessSystemPrompt(),
                cmd.preprocessUserPromptTemplate(),
                cmd.prepSchemaJson(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.ok(llmPreprocessConfigCommandAppService.updatePrompts(payload));
    }

    @PostMapping("/prompts/reset")
    public ResponseEntity<LlmPromptConfigDTO> resetPrompts() {
        return ResponseEntity.ok(llmPreprocessConfigCommandAppService.resetPrompts(SecurityOperator.currentOperator(null)));
    }

    @PostMapping("/prompts/preview")
    public ResponseEntity<LlmPromptPreviewDTO> previewPrompts(@Valid @RequestBody PreviewLlmPromptCmd cmd) {
        return ResponseEntity.ok(llmPreprocessConfigCommandAppService.previewPrompts(cmd));
    }
}
