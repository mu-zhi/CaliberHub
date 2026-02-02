package com.caliberhub.adapter.scene.web;

import com.caliberhub.application.scene.api.dto.response.SceneVersionDTO;
import com.caliberhub.application.scene.service.SceneAppService;
import com.caliberhub.infrastructure.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 导出控制器
 */
@RestController
@RequestMapping("/api/scenes/{sceneCode}/export")
@RequiredArgsConstructor
public class ExportController {

    private final SceneAppService sceneAppService;

    /**
     * 下载 doc.json
     */
    @GetMapping("/doc")
    public ResponseEntity<String> downloadDoc(@PathVariable String sceneCode) {
        var exportResult = sceneAppService.export(sceneCode);
        if (!exportResult.success()) {
            return ResponseEntity.status(500).body("{\"error\":\"" + exportResult.errorMessage() + "\"}");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + sceneCode + "_doc.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(exportResult.docJson());
    }

    /**
     * 下载 chunks.json
     */
    @GetMapping("/chunks")
    public ResponseEntity<String> downloadChunks(@PathVariable String sceneCode) {
        var exportResult = sceneAppService.export(sceneCode);
        if (!exportResult.success()) {
            return ResponseEntity.status(500).body("{\"error\":\"" + exportResult.errorMessage() + "\"}");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + sceneCode + "_chunks.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(exportResult.chunksJson());
    }
}
