package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.response.FetchColumnResponseDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.LineageGraphDataDTO;
import com.cmbchina.datadirect.caliber.application.service.query.DataMapQueryAppService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets")
@Validated
public class DataMapController {

    private final DataMapQueryAppService dataMapQueryAppService;

    public DataMapController(DataMapQueryAppService dataMapQueryAppService) {
        this.dataMapQueryAppService = dataMapQueryAppService;
    }

    @GetMapping("/columns")
    public ResponseEntity<FetchColumnResponseDTO> columns(@RequestParam @NotBlank String columnId,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) String view) {
        return ResponseEntity.ok(dataMapQueryAppService.fetchColumn(columnId, keyword, view));
    }

    @GetMapping("/lineage/{sceneId}")
    public ResponseEntity<LineageGraphDataDTO> lineage(@PathVariable Long sceneId,
                                                       @RequestParam(required = false) Integer maxNodes) {
        return ResponseEntity.ok(dataMapQueryAppService.fetchLineage(sceneId, maxNodes));
    }
}
