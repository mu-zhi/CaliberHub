package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.PreprocessImportCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportTaskLifecycleDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportTaskDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.PreprocessResultDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.service.command.ImportCommandAppService;
import com.cmbchina.datadirect.caliber.application.service.command.ImportTaskCommandAppService;
import com.cmbchina.datadirect.caliber.application.service.query.ImportTaskQueryAppService;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.SecurityOperator;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/import")
@Validated
public class ImportController {

    private final ImportCommandAppService importCommandAppService;
    private final ImportTaskCommandAppService importTaskCommandAppService;
    private final ImportTaskQueryAppService importTaskQueryAppService;

    public ImportController(ImportCommandAppService importCommandAppService,
                            ImportTaskCommandAppService importTaskCommandAppService,
                            ImportTaskQueryAppService importTaskQueryAppService) {
        this.importCommandAppService = importCommandAppService;
        this.importTaskCommandAppService = importTaskCommandAppService;
        this.importTaskQueryAppService = importTaskQueryAppService;
    }

    @PostMapping("/preprocess")
    public ResponseEntity<PreprocessResultDTO> preprocess(@Valid @RequestBody PreprocessImportCmd cmd) {
        String taskId = UUID.randomUUID().toString();
        return ResponseEntity.ok(importCommandAppService.preprocess(normalizeCmd(cmd), taskId));
    }

    @PostMapping("/preprocess-llm")
    public ResponseEntity<PreprocessResultDTO> preprocessByLlm(@Valid @RequestBody PreprocessImportCmd cmd) {
        String taskId = UUID.randomUUID().toString();
        return ResponseEntity.ok(importCommandAppService.preprocessByLlm(normalizeCmd(cmd), taskId));
    }

    @PostMapping(value = "/preprocess-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter preprocessStream(@Valid @RequestBody PreprocessImportCmd cmd) {
        SseEmitter emitter = new SseEmitter(180_000L);
        PreprocessImportCmd payload = normalizeCmd(cmd);
        String taskId = UUID.randomUUID().toString();
        CompletableFuture.runAsync(() -> {
            try {
                sendEvent(emitter, "start", Map.of("message", "导入任务已启动", "taskId", taskId));
                PreprocessResultDTO result = importCommandAppService.preprocessChunked(
                        payload,
                        taskId,
                        stage -> sendEventUnchecked(emitter, "stage", stage),
                        draft -> sendEventUnchecked(emitter, "draft", draft),
                        graphPatch -> sendEventUnchecked(emitter, "graph_patch", graphPatch)
                );
                sendEvent(emitter, "done", result);
                emitter.complete();
            } catch (Exception ex) {
                sendEventSafely(emitter, "error", Map.of("message", ex.getMessage() == null ? "导入失败" : ex.getMessage()));
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ImportTaskDTO> getTask(@PathVariable String taskId) {
        return ResponseEntity.ok(importTaskQueryAppService.getByTaskId(taskId));
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<ImportTaskLifecycleDTO>> listTasks(@RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) String operator,
                                                                  @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(importTaskQueryAppService.listRecent(status, operator, limit));
    }

    @GetMapping("/tasks/{taskId}/scenes")
    public ResponseEntity<List<SceneDTO>> listTaskScenes(@PathVariable String taskId) {
        return ResponseEntity.ok(importTaskQueryAppService.listTaskScenes(taskId));
    }

    @PostMapping("/tasks/{taskId}/quality-confirm")
    public ResponseEntity<ImportTaskDTO> confirmQuality(@PathVariable String taskId) {
        return ResponseEntity.ok(importTaskCommandAppService.confirmQuality(taskId, SecurityOperator.currentOperator("")));
    }

    @PostMapping("/tasks/{taskId}/compare-confirm")
    public ResponseEntity<ImportTaskDTO> confirmCompare(@PathVariable String taskId) {
        return ResponseEntity.ok(importTaskCommandAppService.confirmCompare(taskId, SecurityOperator.currentOperator("")));
    }

    @PostMapping("/tasks/{taskId}/rewind/{step}")
    public ResponseEntity<ImportTaskDTO> rewind(@PathVariable String taskId, @PathVariable Integer step) {
        return ResponseEntity.ok(importTaskCommandAppService.rewindToStep(taskId, step, SecurityOperator.currentOperator("")));
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<ImportTaskDTO> complete(@PathVariable String taskId) {
        return ResponseEntity.ok(importTaskCommandAppService.complete(taskId, SecurityOperator.currentOperator("")));
    }

    private void sendEventUnchecked(SseEmitter emitter, String event, Object payload) {
        try {
            sendEvent(emitter, event, payload);
        } catch (IOException ex) {
            throw new IllegalStateException("sse send failed", ex);
        }
    }

    private void sendEventSafely(SseEmitter emitter, String event, Object payload) {
        try {
            sendEvent(emitter, event, payload);
        } catch (Exception ignore) {
            // ignore
        }
    }

    private void sendEvent(SseEmitter emitter, String event, Object payload) throws IOException {
        emitter.send(SseEmitter.event().name(event).data(payload));
    }

    private PreprocessImportCmd normalizeCmd(PreprocessImportCmd cmd) {
        return new PreprocessImportCmd(
                cmd.rawText(),
                cmd.sourceType(),
                cmd.sourceName(),
                cmd.preprocessMode(),
                cmd.autoCreateDrafts(),
                SecurityOperator.currentOperator(cmd.operator())
        );
    }
}
