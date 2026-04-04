import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  extractSceneId,
  fetchDataMapGraph,
  fetchDataMapImpactAnalysis,
  fetchDataMapNodeDetail,
} from "./datamap-adapter";

function mockJsonResponse(payload) {
  return {
    ok: true,
    status: 200,
    headers: {
      get() {
        return "";
      },
    },
    json: async () => payload,
  };
}

describe("datamap-adapter", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("normalizes graph dto and forwards filters", async () => {
    globalThis.fetch.mockResolvedValueOnce(mockJsonResponse({
      rootRef: "scene:12",
      sceneId: 12,
      sceneName: "代发样板场景",
      nodes: [
        {
          id: "scene:12",
          label: "代发样板场景",
          objectType: "SCENE",
          objectCode: "PAYROLL_SAMPLE",
          objectName: "代发样板场景",
          status: "PUBLISHED",
          domainCode: "PAYROLL",
          summaryText: "图谱根节点",
        },
      ],
      edges: [
        {
          id: "scene:12>USES_PLAN>plan:1",
          relationType: "USES_PLAN",
          relationGroup: "control",
          source: "scene:12",
          target: "plan:1",
          traceId: "trace_plan_1",
        },
      ],
    }));

    const graph = await fetchDataMapGraph("scene", 12, {
      snapshotId: 99,
      objectTypes: ["PLAN", "POLICY"],
      relationTypes: ["USES_PLAN"],
    });

    expect(globalThis.fetch).toHaveBeenCalledWith(
      "/api/datamap/graph?root_type=SCENE&root_id=12&snapshot_id=99&object_types=PLAN%2CPOLICY&relation_types=USES_PLAN",
      expect.objectContaining({ method: "GET" }),
    );
    expect(graph.rootNodeId).toBe("scene:12");
    expect(graph.sceneId).toBe(12);
    expect(graph.nodes[0]).toMatchObject({
      id: "scene:12",
      type: "SCENE",
      objectType: "SCENE",
      objectCode: "PAYROLL_SAMPLE",
      status: "PUBLISHED",
    });
    expect(graph.edges[0]).toMatchObject({
      id: "scene:12>USES_PLAN>plan:1",
      relationType: "USES_PLAN",
      relationGroup: "control",
      label: "USES_PLAN",
      source: "scene:12",
      target: "plan:1",
    });
  });

  it("normalizes node detail payload", async () => {
    globalThis.fetch.mockResolvedValueOnce(mockJsonResponse({
      assetRef: "plan:1",
      node: {
        id: "plan:1",
        label: "协议号检索方案",
        objectType: "PLAN",
        objectCode: "PLAN_PAYROLL",
        objectName: "协议号检索方案",
      },
      attributes: {
        object_type: "PLAN",
        time_semantic: "T+1",
      },
    }));

    const detail = await fetchDataMapNodeDetail("plan:1");
    expect(detail.assetRef).toBe("plan:1");
    expect(detail.node.objectType).toBe("PLAN");
    expect(detail.attributes.time_semantic).toBe("T+1");
  });

  it("normalizes impact analysis payload", async () => {
    globalThis.fetch.mockResolvedValueOnce(mockJsonResponse({
      assetRef: "plan:1",
      riskLevel: "HIGH",
      recommendedActions: ["复核契约视图", "回放样板链路"],
      affectedAssets: [
        {
          assetRef: "contract-view:8",
          objectType: "CONTRACT_VIEW",
          objectName: "客服视图",
          relationType: "DERIVED_FROM",
          impactSummary: "字段级权限可能变化",
        },
      ],
      graph: {
        nodes: [{ id: "plan:1", objectType: "PLAN", label: "方案" }],
        edges: [],
      },
    }));

    const impact = await fetchDataMapImpactAnalysis("plan:1", 88);
    expect(globalThis.fetch).toHaveBeenCalledWith(
      "/api/datamap/impact-analysis",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ assetRef: "plan:1", snapshotId: 88 }),
      }),
    );
    expect(impact.riskLevel).toBe("HIGH");
    expect(impact.affectedAssets[0]).toMatchObject({
      assetRef: "contract-view:8",
      objectType: "CONTRACT_VIEW",
    });
    expect(impact.graph?.nodes[0].objectType).toBe("PLAN");
  });

  it("preserves relationType and coverageExplanation for lineage edges", async () => {
    globalThis.fetch.mockResolvedValueOnce(mockJsonResponse({
      rootRef: "scene:5",
      sceneId: 5,
      sceneName: "代发明细查询",
      nodes: [
        {
          id: "scene:5",
          label: "代发明细查询",
          objectType: "SCENE",
          status: "PUBLISHED",
        },
        {
          id: "coverage:1",
          label: "核心覆盖声明",
          objectType: "COVERAGE_DECLARATION",
          status: "PUBLISHED",
        },
      ],
      edges: [
        {
          id: "scene:5>SUPPORTED_BY_COVERAGE>coverage:1",
          source: "scene:5",
          target: "coverage:1",
          relationType: "SUPPORTED_BY_COVERAGE",
          coverageExplanation: "基于发布任务 2026-03-29 的覆盖声明",
        },
      ],
    }));

    const graph = await fetchDataMapGraph("SCENE", 5, { snapshotId: 2026032901 });

    expect(graph.edges[0]).toMatchObject({
      id: "scene:5>SUPPORTED_BY_COVERAGE>coverage:1",
      relationType: "SUPPORTED_BY_COVERAGE",
      coverageExplanation: "基于发布任务 2026-03-29 的覆盖声明",
      label: "SUPPORTED_BY_COVERAGE",
    });
  });

  it("uses the datamap graph main path for high-visibility graph requests", async () => {
    globalThis.fetch.mockResolvedValueOnce(mockJsonResponse({
      rootRef: "scene:12",
      nodes: [],
      edges: [],
    }));

    await fetchDataMapGraph("scene", 12, {});

    expect(globalThis.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/datamap/graph"),
      expect.objectContaining({ method: "GET" }),
    );
  });

  it("uses the datamap node detail main path for high-visibility detail requests", async () => {
    globalThis.fetch.mockResolvedValueOnce(mockJsonResponse({
      assetRef: "plan:1",
      node: { id: "plan:1", objectType: "PLAN", label: "方案" },
      attributes: {},
    }));

    await fetchDataMapNodeDetail("plan:1");

    expect(globalThis.fetch).toHaveBeenCalledWith(
      "/api/datamap/node/plan%3A1/detail",
      expect.objectContaining({ method: "GET" }),
    );
  });

  it("preserves snapshotId on normalized nodes for snapshot-scoped browsing", async () => {
    const SNAPSHOT_ID = 2026032901;
    globalThis.fetch.mockResolvedValueOnce(mockJsonResponse({
      rootRef: "scene:5",
      sceneId: 5,
      sceneName: "代发明细查询",
      nodes: [
        {
          id: "scene:5",
          label: "代发明细查询",
          objectType: "SCENE",
          status: "PUBLISHED",
        },
        {
          id: "plan:10",
          label: "协议号检索方案",
          objectType: "PLAN",
          status: "PUBLISHED",
          snapshotId: SNAPSHOT_ID,
        },
        {
          id: "output-contract:20",
          label: "代发明细输出契约",
          objectType: "OUTPUT_CONTRACT",
          status: "PUBLISHED",
          snapshotId: SNAPSHOT_ID,
        },
      ],
      edges: [
        {
          id: "scene:5>USES_PLAN>plan:10",
          relationType: "USES_PLAN",
          source: "scene:5",
          target: "plan:10",
        },
      ],
    }));

    const graph = await fetchDataMapGraph("SCENE", 5, { snapshotId: SNAPSHOT_ID });

    expect(globalThis.fetch).toHaveBeenCalledWith(
      expect.stringContaining(`snapshot_id=${SNAPSHOT_ID}`),
      expect.objectContaining({ method: "GET" }),
    );

    const planNode = graph.nodes.find((n) => n.id === "plan:10");
    expect(planNode).toBeDefined();
    expect(planNode.snapshotId).toBe(SNAPSHOT_ID);

    const contractNode = graph.nodes.find((n) => n.id === "output-contract:20");
    expect(contractNode).toBeDefined();
    expect(contractNode.snapshotId).toBe(SNAPSHOT_ID);

    const sceneNode = graph.nodes.find((n) => n.id === "scene:5");
    expect(sceneNode).toBeDefined();
    expect(sceneNode.snapshotId).toBeUndefined();
  });

  it("extracts scene id from miller node meta first", () => {
    expect(extractSceneId({ id: "scene:12", meta: { sceneId: 9 } })).toBe(9);
    expect(extractSceneId({ id: "scene:12", meta: {} })).toBe(12);
    expect(extractSceneId({ id: "plan:12", meta: {} })).toBeNull();
  });
});
