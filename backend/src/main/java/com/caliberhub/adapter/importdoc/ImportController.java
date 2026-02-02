package com.caliberhub.adapter.importdoc;

import com.caliberhub.application.importdoc.dto.*;
import com.caliberhub.application.importdoc.service.ImportAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 文档导入控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ImportController {

    private final ImportAppService importAppService;

    /**
     * 解析预览
     * POST /api/import/parse
     */
    @PostMapping("/parse")
    public ResponseEntity<ParseResponse> parse(@RequestBody ParseRequest request) {
        log.info("接收解析请求: sourceType={}, mode={}", request.getSourceType(), request.getMode());
        ParseResponse response = importAppService.parse(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 确认导入
     * POST /api/import/commit
     */
    @PostMapping("/commit")
    public ResponseEntity<CommitResponse> commit(@RequestBody CommitRequest request) {
        log.info("接收提交请求: sourceType={}, selectedCount={}",
                request.getSourceType(),
                request.getSelectedTempIds() != null ? request.getSelectedTempIds().size() : 0);
        CommitResponse response = importAppService.commit(request);
        return ResponseEntity.ok(response);
    }
}
