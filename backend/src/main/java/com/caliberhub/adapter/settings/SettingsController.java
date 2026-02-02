package com.caliberhub.adapter.settings;

import com.caliberhub.application.support.MetadataSupport;
import com.caliberhub.infrastructure.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 设置控制器
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final MetadataSupport metadataSupport;

    /**
     * 测试元数据连接
     */
    @PostMapping("/metadata/test")
    public ApiResponse<String> testMetadataConnection() {
        try {
            // 通过简单的搜索来测试连接是否正常
            metadataSupport.searchTables("test_connection_probe");
            return ApiResponse.success("Connection successful");
        } catch (Exception e) {
            return ApiResponse.error("500", "Connection failed: " + e.getMessage());
        }
    }

    /**
     * 导出全量
     * Placeholder
     */
    @PostMapping("/export")
    public ApiResponse<Void> exportAll() {
        return ApiResponse.success(null);
    }
}
