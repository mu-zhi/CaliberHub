package com.caliberhub.adapter.scene.web;

import com.caliberhub.application.support.MetadataSupport;
import com.caliberhub.application.support.MetadataSupport.TableDetail;
import com.caliberhub.application.support.MetadataSupport.TableSearchResult;
import com.caliberhub.infrastructure.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 元数据控制器
 */
@RestController
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataSupport metadataSupport;

    /**
     * 搜索表
     */
    @GetMapping("/tables/search")
    public ApiResponse<List<TableSearchResult>> searchTables(@RequestParam String keyword) {
        List<TableSearchResult> results = metadataSupport.searchTables(keyword);
        return ApiResponse.success(results);
    }

    /**
     * 获取表详情
     */
    @GetMapping("/tables/{tableFullname}")
    public ApiResponse<TableDetail> getTableDetail(@PathVariable String tableFullname) {
        TableDetail detail = metadataSupport.getTableDetail(tableFullname);
        if (detail == null) {
            return ApiResponse.error("METADATA_TABLE_NOT_FOUND", "表不存在: " + tableFullname);
        }
        return ApiResponse.success(detail);
    }
}
