import { describe, expect, it } from "vitest";
import {
  applyImportLiveGraphPatch,
  createImportLiveGraphState,
  restoreImportLiveGraphSnapshot,
  selectImportLiveGraphNode,
} from "./importLiveGraphState";

describe("importLiveGraphState", () => {
  it("applies graph patches with node and edge dedupe", () => {
    const initialState = createImportLiveGraphState();
    const firstPatch = {
      patchSeq: 1,
      stageKey: "normalize",
      stageName: "结果归一",
      message: "候选实体图谱已更新",
      addedNodes: [
        { id: "scene-1", nodeType: "CANDIDATE_SCENE", label: "代发明细查询", status: "PENDING_CONFIRMATION", confidenceScore: 0.9, evidenceRefs: [] },
        { id: "field-1", nodeType: "OUTPUT_FIELD", label: "交易金额", status: "PENDING_CONFIRMATION", confidenceScore: 0.9, evidenceRefs: [] },
      ],
      addedEdges: [
        { id: "edge-1", sourceId: "scene-1", targetId: "field-1", relationType: "DECLARES_OUTPUT", status: "PENDING_CONFIRMATION", confidenceScore: 0.9, evidenceRefs: [] },
      ],
    };
    const secondPatch = {
      patchSeq: 2,
      stageKey: "normalize",
      stageName: "结果归一",
      message: "候选实体图谱已更新",
      addedNodes: [
        { id: "scene-1", nodeType: "CANDIDATE_SCENE", label: "代发明细查询", status: "PENDING_CONFIRMATION", confidenceScore: 0.9, evidenceRefs: [] },
        { id: "table-1", nodeType: "SOURCE_TABLE", label: "PDM_VHIS.T05_AGN_DTL", status: "PENDING_CONFIRMATION", confidenceScore: 0.9, evidenceRefs: [] },
      ],
      addedEdges: [
        { id: "edge-2", sourceId: "scene-1", targetId: "table-1", relationType: "USES_SOURCE_TABLE", status: "PENDING_CONFIRMATION", confidenceScore: 0.9, evidenceRefs: [] },
      ],
    };

    const withFirstPatch = applyImportLiveGraphPatch(initialState, firstPatch);
    const withSecondPatch = applyImportLiveGraphPatch(withFirstPatch, secondPatch);

    expect(withSecondPatch.nodes).toHaveLength(3);
    expect(withSecondPatch.edges).toHaveLength(2);
    expect(withSecondPatch.recentActivity[0].label).toContain("PDM_VHIS.T05_AGN_DTL");
    expect(withSecondPatch.lastPatchSeq).toBe(2);
  });

  it("restores final snapshot and keeps valid node selection", () => {
    const state = selectImportLiveGraphNode(
      restoreImportLiveGraphSnapshot(createImportLiveGraphState(), {
        nodes: [
          { id: "scene-1", nodeType: "CANDIDATE_SCENE", label: "代发明细查询", status: "PENDING_CONFIRMATION", confidenceScore: 0.9, evidenceRefs: [] },
          { id: "field-1", nodeType: "OUTPUT_FIELD", label: "交易金额", status: "PENDING_CONFIRMATION", confidenceScore: 0.9, evidenceRefs: [] },
        ],
        edges: [
          { id: "edge-1", sourceId: "scene-1", targetId: "field-1", relationType: "DECLARES_OUTPUT", status: "PENDING_CONFIRMATION", confidenceScore: 0.9, evidenceRefs: [] },
        ],
      }),
      "scene-1",
    );

    const restored = restoreImportLiveGraphSnapshot(state, {
      nodes: [
        { id: "scene-1", nodeType: "CANDIDATE_SCENE", label: "代发明细查询", status: "PENDING_CONFIRMATION", confidenceScore: 0.9, evidenceRefs: [] },
      ],
      edges: [],
    });

    expect(restored.selectedNodeId).toBe("scene-1");
    expect(restored.nodes).toHaveLength(1);
    expect(restored.inspectorMode).toBe("node");
  });

  it("falls back to default inspector when selected node disappears", () => {
    const selected = selectImportLiveGraphNode(createImportLiveGraphState(), "missing-node");
    const restored = restoreImportLiveGraphSnapshot(selected, {
      nodes: [
        { id: "scene-2", nodeType: "CANDIDATE_SCENE", label: "协议定位", status: "PENDING_CONFIRMATION", confidenceScore: 0.8, evidenceRefs: [] },
      ],
      edges: [],
    });

    expect(restored.selectedNodeId).toBe("");
    expect(restored.inspectorMode).toBe("default");
    expect(restored.summaryMessage).toContain("已恢复");
  });
});
