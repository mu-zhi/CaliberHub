package com.cmbchina.datadirect.caliber.application.service.query.datamap;

import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphNodeDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImpactAnalysisSupportTest {

    @Test
    void shouldReturnHighRiskForPolicyNode() {
        DataMapGraphNodeDTO node = new DataMapGraphNodeDTO(
                "policy:1", "审批策略", "POLICY", "POL-001", "审批策略",
                "ACTIVE", null, null, null, null, null, null, null, null, null);
        assertThat(ImpactAnalysisSupport.riskLevel(node, 1)).isEqualTo("HIGH");
    }

    @Test
    void shouldReturnMediumRiskForPlanNode() {
        DataMapGraphNodeDTO node = new DataMapGraphNodeDTO(
                "plan:1", "方案", "PLAN", "PLN-001", "方案",
                "ACTIVE", null, null, null, null, null, null, null, null, null);
        assertThat(ImpactAnalysisSupport.riskLevel(node, 2)).isEqualTo("MEDIUM");
    }

    @Test
    void shouldReturnLowRiskForSceneNode() {
        DataMapGraphNodeDTO node = new DataMapGraphNodeDTO(
                "scene:1", "场景", "SCENE", "SCN-001", "场景",
                "ACTIVE", null, null, null, null, null, null, null, null, null);
        assertThat(ImpactAnalysisSupport.riskLevel(node, 1)).isEqualTo("LOW");
    }

    @Test
    void shouldReturnHighRiskWhenManyAffected() {
        DataMapGraphNodeDTO node = new DataMapGraphNodeDTO(
                "scene:1", "场景", "SCENE", "SCN-001", "场景",
                "ACTIVE", null, null, null, null, null, null, null, null, null);
        assertThat(ImpactAnalysisSupport.riskLevel(node, 10)).isEqualTo("HIGH");
    }

    @Test
    void shouldDescribeContractViewImpact() {
        DataMapGraphNodeDTO root = new DataMapGraphNodeDTO(
                "plan:1", "方案", "PLAN", "PLN-001", "方案",
                "ACTIVE", null, null, null, null, null, null, null, null, null);
        DataMapGraphNodeDTO target = new DataMapGraphNodeDTO(
                "cv:1", "视图", "CONTRACT_VIEW", "CV-001", "视图",
                "ACTIVE", null, null, null, null, null, null, null, null, null);
        assertThat(ImpactAnalysisSupport.describeImpact(root, target)).contains("字段级可见范围");
    }

    @Test
    void shouldReturnRecommendedActions() {
        DataMapGraphNodeDTO node = new DataMapGraphNodeDTO(
                "policy:1", "策略", "POLICY", "POL-001", "策略",
                "ACTIVE", null, null, null, null, null, null, null, null, null);
        List<String> actions = ImpactAnalysisSupport.recommendedActions(node);
        assertThat(actions).hasSizeGreaterThanOrEqualTo(2);
        assertThat(actions.get(0)).contains("契约视图");
    }
}
