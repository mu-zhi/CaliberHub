package com.caliberhub.adapter.scene.web;

import com.caliberhub.application.scene.api.dto.request.CreateSceneCmd;
import com.caliberhub.application.scene.api.dto.request.PublishSceneCmd;
import com.caliberhub.application.scene.api.dto.request.SaveDraftCmd;
import com.caliberhub.application.scene.api.dto.response.SceneDTO;
import com.caliberhub.application.scene.api.dto.response.SceneVersionDTO;
import com.caliberhub.application.scene.service.SceneAppService;
import com.caliberhub.domain.scene.service.LintResult;
import com.caliberhub.infrastructure.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 场景控制器
 */
@RestController
@RequestMapping("/api/scenes")
@RequiredArgsConstructor
public class SceneController {

    private final SceneAppService sceneAppService;

    /**
     * 创建场景
     */
    @PostMapping
    public ApiResponse<SceneDTO> createScene(@Valid @RequestBody CreateSceneCmd cmd) {
        SceneDTO scene = sceneAppService.createScene(cmd);
        return ApiResponse.success(scene);
    }

    /**
     * 获取场景详情
     */
    @GetMapping("/{sceneCode}")
    public ApiResponse<SceneDTO> getScene(@PathVariable String sceneCode) {
        SceneDTO scene = sceneAppService.getScene(sceneCode);
        return ApiResponse.success(scene);
    }

    /**
     * 获取草稿
     */
    @GetMapping("/{sceneCode}/draft")
    public ApiResponse<SceneVersionDTO> getDraft(@PathVariable String sceneCode) {
        SceneVersionDTO draft = sceneAppService.getDraft(sceneCode);
        return ApiResponse.success(draft);
    }

    /**
     * 保存草稿
     */
    @PutMapping("/{sceneCode}/draft")
    public ApiResponse<SceneVersionDTO> saveDraft(
            @PathVariable String sceneCode,
            @Valid @RequestBody SaveDraftCmd cmd) {
        SceneVersionDTO draft = sceneAppService.saveDraft(sceneCode, cmd);
        return ApiResponse.success(draft);
    }

    /**
     * 运行 Lint
     */
    @PostMapping("/{sceneCode}/lint")
    public ApiResponse<LintResult> lint(@PathVariable String sceneCode) {
        LintResult result = sceneAppService.lint(sceneCode);
        return ApiResponse.success(result);
    }

    /**
     * 发布场景
     */
    @PostMapping("/{sceneCode}/publish")
    public ApiResponse<SceneVersionDTO> publish(
            @PathVariable String sceneCode,
            @Valid @RequestBody PublishSceneCmd cmd) {
        SceneVersionDTO version = sceneAppService.publish(sceneCode, cmd);
        return ApiResponse.success(version);
    }

    /**
     * 废弃场景
     */
    @PostMapping("/{sceneCode}/archive")
    public ApiResponse<Void> archive(
            @PathVariable String sceneCode,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        sceneAppService.deprecate(sceneCode, reason);
        return ApiResponse.success(null);
    }

    /**
     * 废弃场景 (Deprecate Alias)
     */
    @PostMapping("/{sceneCode}/deprecate")
    public ApiResponse<Void> deprecate(
            @PathVariable String sceneCode,
            @RequestBody(required = false) Map<String, String> body) {
        return archive(sceneCode, body);
    }

    /**
     * 获取版本列表
     */
    @GetMapping("/{sceneCode}/versions")
    public ApiResponse<List<SceneVersionDTO>> getVersions(@PathVariable String sceneCode) {
        List<SceneVersionDTO> versions = sceneAppService.getVersions(sceneCode);
        return ApiResponse.success(versions);
    }

    /**
     * 创建新版本 (从已发布版本)
     */
    @PostMapping("/{sceneCode}/versions")
    public ApiResponse<SceneVersionDTO> createVersion(
            @PathVariable String sceneCode,
            @RequestBody Map<String, String> body) {
        String baseVersion = body.getOrDefault("base", "PUBLISHED"); // default to PUBLISHED
        SceneVersionDTO version = sceneAppService.createVersion(sceneCode, baseVersion);
        return ApiResponse.success(version);
    }

}
