import { describe, expect, it } from "vitest";
import {
  buildSceneCandidateCode,
  buildStep1Summary,
  buildStep2Summary,
  buildStep3Summary,
  applyGraphPatch,
  mergeCandidateGraphPatch,
  normalizeCandidateGraph,
  resolveAccordionStepState,
  summarizeCandidateGraph,
  toConfidenceLevelZh,
} from "./knowledge-import-utils";

describe("knowledge import accordion utils", () => {
  it("resolves step states by active step", () => {
    expect(resolveAccordionStepState(1, 0)).toBe("collapsed");
    expect(resolveAccordionStepState(2, 0)).toBe("locked");
    expect(resolveAccordionStepState(1, 1)).toBe("expanded");
    expect(resolveAccordionStepState(1, 2)).toBe("collapsed");
    expect(resolveAccordionStepState(3, 2)).toBe("locked");
  });

  it("formats step summaries in expected business copy", () => {
    expect(buildStep1Summary("粘贴文本", "abcd")).toBe("✓ 01 导入口径草稿 | 来源：粘贴文本（约 4 字）");
    expect(buildStep2Summary(5, 0.85)).toBe("✓ 02 抽取质量判断 | 共识别 5 个场景，置信度 85%（高）");
    expect(buildStep3Summary(5)).toBe("✓ 03 结果与原文对照 | 已确认场景 5 个");
  });

  it("builds scene candidate codes with the shared import rule", () => {
    expect(buildSceneCandidateCode("f7adebcf-0ecf-4a14-a561-557e27961cc5", 0)).toBe("SC-f7adebcf-001");
    expect(buildSceneCandidateCode("task-1", 2)).toBe("SC-task1-003");
  });

  it("maps confidence into chinese levels", () => {
    expect(toConfidenceLevelZh(0.9)).toBe("高");
    expect(toConfidenceLevelZh(0.7)).toBe("中");
    expect(toConfidenceLevelZh(0.55)).toBe("低");
  });

  it("normalizes candidate graph payload into graph view data", () => {
    const normalized = normalizeCandidateGraph({
      graphId: "task-a:material-a",
      nodes: [
        {
          nodeCode: "SC-001",
          nodeType: "CANDIDATE_SCENE",
          label: "代发明细查询",
          reviewStatus: "PENDING_CONFIRMATION",
        },
        {
          nodeCode: "ID-001",
          nodeType: "IDENTIFIER",
          label: "协议号",
          reviewStatus: "ACCEPTED",
        },
      ],
      edges: [
        {
          edgeCode: "EDGE-001",
          edgeType: "SCENE_HAS_PLAN",
          sourceNodeCode: "SC-001",
          targetNodeCode: "ID-001",
          label: "标识对象",
        },
      ],
    });

    expect(normalized.rootRef).toBe("task-a:material-a");
    expect(normalized.nodes).toHaveLength(2);
    expect(normalized.edges).toHaveLength(1);
    expect(normalized.nodes[0].objectType).toBe("CANDIDATE_SCENE");
  });

  it("summarizes pending review counts for candidate graph", () => {
    expect(summarizeCandidateGraph({
      nodes: [
        { reviewStatus: "PENDING_CONFIRMATION" },
        { reviewStatus: "ACCEPTED" },
        { reviewStatus: "PENDING_CONFIRMATION" },
      ],
      edges: [{ reviewStatus: "REJECTED" }],
    })).toEqual({
      pendingNodes: 2,
      acceptedNodes: 1,
      pendingEdges: 0,
    });
  });

  it("applies graph patch to null graph without error", () => {
    const patch = applyGraphPatch(null, {
      addedNodes: [
        {
          nodeCode: "N-1",
          nodeType: "CANDIDATE_SCENE",
          label: "代发协议查询",
          reviewStatus: "PENDING_CONFIRMATION",
        },
      ],
      addedEdges: [
        {
          edgeCode: "E-1",
          edgeType: "SCENE_HAS_PLAN",
          sourceNodeCode: "N-1",
          targetNodeCode: "P-1",
          label: "包含",
        },
      ],
      updatedNodes: [],
      updatedEdges: [],
    });

    expect(patch).toMatchObject({
      nodes: [
        {
          nodeCode: "N-1",
          nodeType: "CANDIDATE_SCENE",
          label: "代发协议查询",
          reviewStatus: "PENDING_CONFIRMATION",
        },
      ],
      edges: [
        {
          edgeCode: "E-1",
          edgeType: "SCENE_HAS_PLAN",
          sourceNodeCode: "N-1",
          targetNodeCode: "P-1",
          label: "包含",
        },
      ],
      graphVersion: 1,
    });
  });

  it("merges add and update events by code with full patch overwrite", () => {
    const baseline = {
      graphId: "task-001",
      graphVersion: 2,
      nodes: [
        {
          nodeCode: "N-1",
          nodeType: "CANDIDATE_SCENE",
          label: "旧名",
          reviewStatus: "PENDING_CONFIRMATION",
        },
      ],
      edges: [
        {
          edgeCode: "E-1",
          edgeType: "SCENE_HAS_PLAN",
          reviewStatus: "PENDING_CONFIRMATION",
        },
      ],
    };
    const patch = applyGraphPatch(baseline, {
      addedNodes: [
        {
          nodeCode: "N-2",
          nodeType: "CANDIDATE_PLAN",
          label: "方案 1",
        },
      ],
      updatedNodes: [
        {
          nodeCode: "N-1",
          label: "新名",
          reviewStatus: "ACCEPTED",
        },
      ],
      addedEdges: [
        {
          edgeCode: "E-2",
          edgeType: "SCENE_HAS_PLAN",
          reviewStatus: "PENDING_CONFIRMATION",
        },
      ],
      updatedEdges: [
        {
          edgeCode: "E-1",
          reviewStatus: "ACCEPTED",
        },
      ],
    });

    expect(patch.graphVersion).toBe(3);
    expect(patch.nodes).toHaveLength(2);
    expect(patch.edges).toHaveLength(2);
    expect(patch.nodes.find((item) => item.nodeCode === "N-1")).toEqual({
      nodeCode: "N-1",
      nodeType: "CANDIDATE_SCENE",
      label: "新名",
      reviewStatus: "ACCEPTED",
    });
    expect(patch.nodes).toContainEqual({
      nodeCode: "N-2",
      nodeType: "CANDIDATE_PLAN",
      label: "方案 1",
    });
    expect(patch.edges.find((item) => item.edgeCode === "E-1")).toEqual({
      edgeCode: "E-1",
      edgeType: "SCENE_HAS_PLAN",
      reviewStatus: "ACCEPTED",
    });
    expect(patch.edges).toContainEqual({
      edgeCode: "E-2",
      edgeType: "SCENE_HAS_PLAN",
      reviewStatus: "PENDING_CONFIRMATION",
    });
  });

  it("merges live graph patches without dropping existing raw nodes and edges", () => {
    const merged = mergeCandidateGraphPatch(
      {
        graphId: "task-a:material-a",
        nodes: [
          {
            nodeCode: "SC-001",
            nodeType: "CANDIDATE_SCENE",
            label: "代发明细查询",
            reviewStatus: "PENDING_CONFIRMATION",
          },
        ],
        edges: [
          {
            edgeCode: "EDGE-001",
            edgeType: "SCENE_HAS_PLAN",
            sourceNodeCode: "SC-001",
            targetNodeCode: "PLN-001",
            label: "候选方案",
          },
        ],
      },
      {
        graphId: "task-a:material-a",
        patchSeq: 2,
        summary: "新增补丁",
        focusNodeIds: ["PLN-002"],
        updatedNodes: [
          {
            nodeCode: "SC-001",
            nodeType: "CANDIDATE_SCENE",
            label: "代发明细查询（已更新）",
            reviewStatus: "ACCEPTED",
          },
        ],
        addedNodes: [
          {
            nodeCode: "PLN-002",
            nodeType: "CANDIDATE_PLAN",
            label: "历史补查",
            reviewStatus: "PENDING_CONFIRMATION",
          },
        ],
        addedEdges: [
          {
            edgeCode: "EDGE-002",
            edgeType: "SCENE_HAS_PLAN",
            sourceNodeCode: "SC-001",
            targetNodeCode: "PLN-002",
            label: "候选方案",
          },
        ],
      },
    );

    expect(merged.graphId).toBe("task-a:material-a");
    expect(merged.summary).toBe("新增补丁");
    expect(merged.patchSeq).toBe(2);
    expect(merged.focusNodeIds).toEqual(["PLN-002"]);
    expect(merged.nodes).toHaveLength(2);
    expect(merged.nodes.find((item) => item.nodeCode === "SC-001").label).toBe("代发明细查询（已更新）");
    expect(merged.edges).toHaveLength(2);
    expect(merged.edges.find((item) => item.edgeCode === "EDGE-002").targetNodeCode).toBe("PLN-002");
  });
});
