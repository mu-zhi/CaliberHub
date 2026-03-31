package com.cmbchina.datadirect.caliber.application.service.query.datamap;

import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphNodeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapImpactAssetDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class ImpactAnalysisSupport {

    private ImpactAnalysisSupport() {}

    public static String firstRelationFor(String assetRef, String otherRef, List<DataMapGraphEdgeDTO> edges) {
        return edges.stream()
                .filter(edge -> (Objects.equals(edge.source(), assetRef) && Objects.equals(edge.target(), otherRef))
                        || (Objects.equals(edge.target(), assetRef) && Objects.equals(edge.source(), otherRef)))
                .map(DataMapGraphEdgeDTO::relationType)
                .findFirst()
                .orElse("CHAINED_IMPACT");
    }

    public static String describeImpact(DataMapGraphNodeDTO rootNode, DataMapGraphNodeDTO targetNode) {
        String rootType = rootNode == null ? "ASSET" : rootNode.objectType();
        return switch (targetNode.objectType()) {
            case "CONTRACT_VIEW" -> rootType + " 变化会直接影响字段级可见范围";
            case "POLICY" -> rootType + " 变化会影响审批、脱敏或拒绝策略";
            case "SOURCE_CONTRACT", "PATH_TEMPLATE" -> rootType + " 变化会影响取数路径与来源契约";
            case "EVIDENCE_FRAGMENT", "EVIDENCE" -> rootType + " 变化需要重新确认证据支撑是否充分";
            case "COVERAGE_DECLARATION", "COVERAGE" -> rootType + " 变化会影响覆盖解释与缺口判断";
            default -> rootType + " 变化会联动该资产的治理边界";
        };
    }

    public static String riskLevel(DataMapGraphNodeDTO rootNode, int affectedCount) {
        String objectType = rootNode == null ? "" : rootNode.objectType();
        String sensitivity = rootNode == null ? "" : safeText(rootNode.sensitivityScope());
        if (Set.of("S3", "S4").contains(sensitivity.toUpperCase(Locale.ROOT))
                || Set.of("POLICY", "CONTRACT_VIEW", "SOURCE_CONTRACT", "VERSION_SNAPSHOT").contains(objectType)
                || affectedCount >= 8) {
            return "HIGH";
        }
        if (Set.of("PLAN", "OUTPUT_CONTRACT", "COVERAGE_DECLARATION", "PATH_TEMPLATE").contains(objectType)
                || affectedCount >= 4) {
            return "MEDIUM";
        }
        return "LOW";
    }

    public static List<String> recommendedActions(DataMapGraphNodeDTO rootNode) {
        String objectType = rootNode == null ? "" : rootNode.objectType();
        List<String> actions = new ArrayList<>();
        if (Set.of("POLICY", "CONTRACT_VIEW").contains(objectType)) {
            actions.add("优先检查契约视图、审批模板和高敏字段是否需要同步调整。");
        }
        if (Set.of("SOURCE_CONTRACT", "PATH_TEMPLATE").contains(objectType)) {
            actions.add("回放样板场景，确认路径模板与来源契约仍可稳定落位。");
        }
        if ("EVIDENCE_FRAGMENT".equals(objectType) || "EVIDENCE".equals(objectType)) {
            actions.add("重新核对证据片段是否仍能支撑发布、审计与运行解释。");
        }
        actions.add("在发布中心复核受影响资产，并确认是否需要切换或回滚快照。");
        actions.add("对高风险节点执行一次样板回放，验证路径高亮、策略命中和覆盖解释。");
        return actions;
    }

    public static List<DataMapImpactAssetDTO> buildAffectedAssets(
            String assetRef,
            DataMapGraphNodeDTO rootNode,
            List<DataMapGraphNodeDTO> impactedNodes,
            List<DataMapGraphEdgeDTO> impactedEdges) {
        return impactedNodes.stream()
                .filter(node -> !assetRef.equals(node.id()))
                .sorted(java.util.Comparator.comparing(DataMapGraphNodeDTO::objectType)
                        .thenComparing(DataMapGraphNodeDTO::objectName))
                .map(node -> new DataMapImpactAssetDTO(
                        node.id(),
                        node.objectType(),
                        node.objectName(),
                        firstRelationFor(assetRef, node.id(), impactedEdges),
                        describeImpact(rootNode, node)
                ))
                .toList();
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
