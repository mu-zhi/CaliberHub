package com.cmbchina.datadirect.caliber.application.service.query;

import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphNodeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.ImportCandidateGraphSummaryDTO;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportCandidateGraphEdgeMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ImportCandidateGraphNodeMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportCandidateGraphEdgePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportCandidateGraphNodePO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImportCandidateGraphQueryAppService {

    private final ImportCandidateGraphNodeMapper nodeMapper;
    private final ImportCandidateGraphEdgeMapper edgeMapper;
    private final ObjectMapper objectMapper;

    public ImportCandidateGraphQueryAppService(ImportCandidateGraphNodeMapper nodeMapper,
                                               ImportCandidateGraphEdgeMapper edgeMapper,
                                               ObjectMapper objectMapper) {
        this.nodeMapper = nodeMapper;
        this.edgeMapper = edgeMapper;
        this.objectMapper = objectMapper;
    }

    public ImportCandidateGraphDTO getByTaskId(String taskId) {
        List<ImportCandidateGraphNodePO> nodePOs = nodeMapper.findByTaskId(taskId);
        List<ImportCandidateGraphEdgePO> edgePOs = edgeMapper.findByTaskId(taskId);
        if (nodePOs.isEmpty()) {
            throw new ResourceNotFoundException("candidate graph not found for task: " + taskId);
        }
        String graphId = nodePOs.get(0).getGraphId();
        String materialId = nodePOs.get(0).getMaterialId();
        List<ImportCandidateGraphNodeDTO> nodes = nodePOs.stream().map(this::toNodeDTO).toList();
        List<ImportCandidateGraphEdgeDTO> edges = edgePOs.stream().map(this::toEdgeDTO).toList();
        int pendingReviewTotal = (int) nodes.stream().filter(n -> "PENDING_CONFIRMATION".equals(n.reviewStatus())).count();
        return new ImportCandidateGraphDTO(taskId, materialId, graphId,
                new ImportCandidateGraphSummaryDTO(nodes.size(), edges.size(), pendingReviewTotal),
                nodes, edges);
    }

    private ImportCandidateGraphNodeDTO toNodeDTO(ImportCandidateGraphNodePO po) {
        return new ImportCandidateGraphNodeDTO(
                po.getNodeCode(),
                po.getSceneCandidateCode(),
                po.getNodeType(),
                po.getNodeLabel(),
                po.getReviewStatus(),
                po.getRiskLevel(),
                po.getConfidenceScore(),
                parseJson(po.getPayloadJson()),
                ""
        );
    }

    private ImportCandidateGraphEdgeDTO toEdgeDTO(ImportCandidateGraphEdgePO po) {
        return new ImportCandidateGraphEdgeDTO(
                po.getEdgeCode(),
                po.getSceneCandidateCode(),
                po.getEdgeType(),
                po.getSourceNodeCode(),
                po.getTargetNodeCode(),
                po.getEdgeLabel(),
                po.getReviewStatus(),
                po.getRiskLevel(),
                po.getConfidenceScore(),
                parseJson(po.getPayloadJson())
        );
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) return objectMapper.createObjectNode();
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }
}
