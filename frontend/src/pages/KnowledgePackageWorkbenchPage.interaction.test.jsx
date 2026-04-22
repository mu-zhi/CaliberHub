// @vitest-environment jsdom
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent, waitFor, act, cleanup } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { KnowledgePackageWorkbenchPage } from "./KnowledgePackageWorkbenchPage";

/* ------------------------------------------------------------------ */
/* Fixtures                                                           */
/* ------------------------------------------------------------------ */

const PUBLISHED_SCENE = {
  id: 1,
  sceneCode: "SCN_PAYROLL_DETAIL",
  sceneTitle: "代发明细查询",
  sceneType: "FACT_DETAIL",
  sceneDescription: "代发明细场景",
  domainId: 10,
  publishedAt: "2026-03-28T10:00:00Z",
  status: "PUBLISHED",
};

const SCENE_BUNDLE = {
  plans: [{ planId: 1, planCode: "PLAN_PAYROLL_DETAIL", planName: "代发明细方案" }],
  coverages: [],
  policies: [],
  contractViews: [],
  sourceContracts: [],
  publishCheck: null,
  versions: [{ id: 42, versionTag: "v1", publishedAt: "2026-03-28T10:00:00Z" }],
  inputSlots: [{ slotCode: "PROTOCOL_NBR", slotName: "协议号", identifierCandidatesJson: "[\"PROTOCOL_NBR\"]" }],
  outputContracts: [{ fieldsJson: "[\"协议号\",\"交易日期\",\"金额\"]" }],
  projection: { status: "READY", lastProjectedAt: "2026-03-28T10:00:00Z" },
};

const SECOND_PUBLISHED_SCENE = {
  id: 2,
  sceneCode: "SCN_PAYROLL_BATCH",
  sceneTitle: "代发批次结果查询",
  sceneType: "FACT_DETAIL",
  sceneDescription: "代发批次场景",
  domainId: 10,
  publishedAt: "2026-03-29T10:00:00Z",
  status: "PUBLISHED",
};

const CLARIFICATION_RESULT = {
  decision: "clarification_only",
  reasonCode: "MULTI_SCENE_AMBIGUOUS",
  runtimeMode: "CLARIFICATION",
  degradeReasonCodes: ["MULTI_SCENE_AMBIGUOUS"],
  clarification: {
    summary: "当前问题同时命中两个场景，请拆分后分别检索",
    sceneCandidates: [
      { sceneId: 1, sceneCode: "SCN_PAYROLL_DETAIL", sceneTitle: "代发明细查询", snapshotId: 42 },
      { sceneId: 2, sceneCode: "SCN_PAYROLL_BATCH", sceneTitle: "代发批次结果查询", snapshotId: 43 },
    ],
    planCandidates: [
      { sceneCode: "SCN_PAYROLL_DETAIL", planId: 11, planCode: "PLAN_PAYROLL_DETAIL", planName: "代发明细方案" },
      { sceneCode: "SCN_PAYROLL_BATCH", planId: 12, planCode: "PLAN_PAYROLL_BATCH", planName: "代发批次结果方案" },
    ],
    subQuestions: [
      "按协议号查询代发明细",
      "按公司户查询代发批次结果",
    ],
    mergeHints: [
      "请先选择「代发明细查询」或「代发批次结果查询」，再分别提交运行请求",
    ],
    clarificationQuestions: ["本次是查询明细还是批次结果？"],
  },
  trace: {
    traceId: "trace_clar_001",
    snapshotId: null,
    inferenceSnapshotId: 99,
    retrievalAdapter: "LightRAG",
    retrievalStatus: "COMPLETED",
    fallbackToFormal: false,
  },
  experiment: {
    adapterName: "LightRAG",
    adapterVersion: "heuristic-sidecar/v1",
    status: "COMPLETED",
    fallbackToFormal: false,
    summary: "实验侧车确认当前问题命中两个已发布场景，需要先拆分子问题。",
    referenceRefs: ["clarification:scene:1", "clarification:scene:2"],
    candidateScenes: [
      { sceneId: 1, sceneCode: "SCN_PAYROLL_DETAIL", sceneTitle: "代发明细查询", score: 0.74, source: "LightRAG" },
      { sceneId: 2, sceneCode: "SCN_PAYROLL_BATCH", sceneTitle: "代发批次结果查询", score: 0.71, source: "LightRAG" },
    ],
    candidateEvidence: [],
    scoreBreakdown: [
      { label: "scene.lexical", score: 0.44 },
      { label: "slot.identifier", score: 0.17 },
    ],
  },
  risk: { riskLevel: "MEDIUM", riskReasons: ["跨场景多意图"] },
};

const ALLOW_RESULT = {
  decision: "allow",
  reasonCode: "ALLOW",
  runtimeMode: "FULL_MATCH",
  degradeReasonCodes: ["ALLOW"],
  scene: { sceneId: 1, sceneCode: "SCN_PAYROLL_DETAIL", sceneTitle: "代发明细查询" },
  plan: { planId: 1, planCode: "PLAN_PAYROLL_DETAIL", planName: "代发明细方案" },
  coverage: { status: "FULL", matchedSegment: "2021-Q1" },
  policy: { decision: "allow" },
  contract: {
    visibleFields: ["协议号", "交易日期", "金额"],
    maskedFields: [],
    restrictedFields: [],
    forbiddenFields: [],
  },
  trace: {
    traceId: "trace_allow_001",
    snapshotId: 42,
    inferenceSnapshotId: 42,
    versionTag: "v1",
    retrievalAdapter: "LightRAG",
    retrievalStatus: "COMPLETED",
    fallbackToFormal: false,
  },
  evidence: [{
    evidenceCode: "EV_001",
    title: "代发交易说明",
    sourceAnchor: "§3.2",
    retrievalSource: "FORMAL_PLAN_EVIDENCE",
    referenceRef: "§3.2",
    retrievalScore: 0.95,
  }],
  experiment: {
    adapterName: "LightRAG",
    adapterVersion: "heuristic-sidecar/v1",
    status: "COMPLETED",
    fallbackToFormal: false,
    summary: "实验侧车补充了候选场景与证据引用，正式决策仍由原链路给出。",
    referenceRefs: ["§3.2", "05-口径文档现状-代发明细查询.sql#current"],
    candidateScenes: [
      { sceneId: 1, sceneCode: "SCN_PAYROLL_DETAIL", sceneTitle: "代发明细查询", score: 0.88, source: "LightRAG" },
    ],
    candidateEvidence: [
      { evidenceCode: "EV_001", title: "代发交易说明", sourceAnchor: "§3.2", referenceRef: "§3.2", score: 0.93 },
    ],
    scoreBreakdown: [
      { label: "scene.lexical", score: 0.56 },
      { label: "slot.identifier", score: 0.22 },
      { label: "evidence.anchor", score: 0.10 },
    ],
  },
  risk: { riskLevel: "LOW", riskReasons: [] },
  path: { resolutionSteps: ["场景命中", "方案选择", "覆盖校验"] },
};

/* ------------------------------------------------------------------ */
/* Mock helpers                                                       */
/* ------------------------------------------------------------------ */

function mockJsonResponse(payload) {
  return {
    ok: true,
    status: 200,
    headers: { get: () => "" },
    json: async () => payload,
  };
}

function mockErrorResponse(status, payload) {
  return {
    ok: false,
    status,
    headers: { get: () => "" },
    json: async () => payload,
  };
}

/**
 * Build a fetch mock that responds to different API paths with fixture data.
 * Accepts an optional `graphQueryResult` to control the /graphrag/query response.
 */
function buildFetchMock(graphQueryResult = ALLOW_RESULT) {
  return vi.fn(async (url) => {
    const path = typeof url === "string" ? url : url.toString();

    // Scene listing
    if (path.includes("/api/scenes")) {
      return mockJsonResponse([PUBLISHED_SCENE]);
    }
    // Scene bundle endpoints
    if (path.includes("/api/plans")) return mockJsonResponse(SCENE_BUNDLE.plans);
    if (path.includes("/api/coverage-declarations")) return mockJsonResponse(SCENE_BUNDLE.coverages);
    if (path.includes("/api/policies")) return mockJsonResponse(SCENE_BUNDLE.policies);
    if (path.includes("/api/contract-views")) return mockJsonResponse(SCENE_BUNDLE.contractViews);
    if (path.includes("/api/source-contracts")) return mockJsonResponse(SCENE_BUNDLE.sourceContracts);
    if (path.includes("/api/publish-checks")) return mockJsonResponse(SCENE_BUNDLE.publishCheck);
    if (path.includes("/api/scenes/1/versions")) return mockJsonResponse(SCENE_BUNDLE.versions);
    if (path.includes("/api/input-slot-schemas")) return mockJsonResponse(SCENE_BUNDLE.inputSlots);
    if (path.includes("/api/output-contracts")) return mockJsonResponse(SCENE_BUNDLE.outputContracts);
    if (path.includes("/api/graphrag/projection")) return mockJsonResponse(SCENE_BUNDLE.projection);

    // Runtime query endpoints
    if (path.includes("/api/scene-search")) {
      return mockJsonResponse({ candidates: [{ sceneId: 1, sceneTitle: "代发明细查询" }], reasons: ["关键词命中"] });
    }
    if (path.includes("/api/plan-select")) {
      return mockJsonResponse({ candidates: [{ planId: 1, planName: "代发明细方案", decision: "allow" }], reasons: ["首选方案命中"] });
    }
    if (path.includes("/api/graphrag/query")) {
      return mockJsonResponse(graphQueryResult);
    }

    // Default fallback
    return mockJsonResponse(null);
  });
}

/* ------------------------------------------------------------------ */
/* Setup                                                              */
/* ------------------------------------------------------------------ */

let originalFetch;

beforeEach(() => {
  // Mock localStorage and sessionStorage for zustand auth store
  const store = {};
  const mockStorage = {
    getItem: vi.fn((key) => store[key] || null),
    setItem: vi.fn((key, value) => { store[key] = value; }),
    removeItem: vi.fn((key) => { delete store[key]; }),
  };
  vi.stubGlobal("localStorage", mockStorage);
  vi.stubGlobal("sessionStorage", mockStorage);

  originalFetch = globalThis.fetch;
});

afterEach(() => {
  vi.useRealTimers();
  globalThis.fetch = originalFetch;
  vi.restoreAllMocks();
  cleanup();
});

function renderWorkbench(graphQueryResult) {
  vi.stubGlobal("fetch", buildFetchMock(graphQueryResult));
  return render(
    <MemoryRouter>
      <KnowledgePackageWorkbenchPage />
    </MemoryRouter>,
  );
}

/* ------------------------------------------------------------------ */
/* Tests                                                              */
/* ------------------------------------------------------------------ */

describe("KnowledgePackageWorkbenchPage interactive flow", () => {
  it("shows timeout feedback when initial loading takes too long", async () => {
    vi.useFakeTimers();
    vi.stubGlobal("fetch", vi.fn(() => new Promise(() => {})));

    render(
      <MemoryRouter>
        <KnowledgePackageWorkbenchPage />
      </MemoryRouter>,
    );

    await act(async () => {
      vi.advanceTimersByTime(8000);
    });

    expect(screen.getByText("加载超时，请检查后端服务或稍后重试。")).toBeTruthy();
    expect(screen.getByRole("button", { name: "重新加载" })).toBeTruthy();

    vi.useRealTimers();
  });

  it("loads published scenes and renders the form", async () => {
    renderWorkbench();

    await waitFor(() => {
      expect(screen.getByText("运行决策台")).toBeTruthy();
    });

    // After scenes load, should show scene selector and form
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "生成知识包" })).toBeTruthy();
    });

    expect(screen.getByText("当前样板")).toBeTruthy();
    expect(screen.getByText("结构化查询")).toBeTruthy();
    expect(screen.getAllByText("已就绪").length).toBeGreaterThan(0);
    expect(screen.queryByText("READY")).toBeNull();
  });

  it("submits query and renders allow result", async () => {
    renderWorkbench(ALLOW_RESULT);

    // Wait for scene loading to complete
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "生成知识包" })).toBeTruthy();
    });

    // Fill in identifier value
    const identifierInput = screen.getAllByPlaceholderText("请输入协议号或客户号")[0];
    await act(async () => {
      fireEvent.change(identifierInput, { target: { value: "P001" } });
    });

    // Submit the form
    const submitButton = screen.getByRole("button", { name: "生成知识包" });
    await act(async () => {
      fireEvent.click(submitButton);
    });

    // Wait for the result to render
    await waitFor(() => {
      expect(screen.getByText("知识包摘要")).toBeTruthy();
    });

    expect(screen.getAllByText("允许").length).toBeGreaterThan(0);
    expect(screen.getByText("完整覆盖 · 2021-Q1")).toBeTruthy();
    expect(screen.queryByText("allow")).toBeNull();
    expect(screen.getByText("检索过程")).toBeTruthy();
    expect(screen.getByText("1 个候选场景")).toBeTruthy();
    expect(screen.getByText("实验检索调试")).toBeTruthy();
    expect(screen.getAllByText("LightRAG").length).toBeGreaterThan(0);
    expect(screen.getByText("候选引用 2 条")).toBeTruthy();
  });

  it("submits query and renders clarification result with interactive buttons", async () => {
    renderWorkbench(CLARIFICATION_RESULT);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "生成知识包" })).toBeTruthy();
    });

    const identifierInput = screen.getAllByPlaceholderText("请输入协议号或客户号")[0];
    await act(async () => {
      fireEvent.change(identifierInput, { target: { value: "P001" } });
    });

    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "生成知识包" }));
    });

    // Clarification card should appear
    await waitFor(() => {
      expect(screen.getByText("需要补充条件")).toBeTruthy();
    });

    expect(screen.getAllByText("需澄清").length).toBeGreaterThan(0);
    expect(screen.queryByText("clarification_only")).toBeNull();

    // Sub-questions with action buttons
    expect(screen.getByText("按协议号查询代发明细")).toBeTruthy();
    expect(screen.getByText("按公司户查询代发批次结果")).toBeTruthy();
    expect(screen.getAllByText("用此子问题检索").length).toBe(2);
    expect(screen.getByText("候选方案")).toBeTruthy();
    expect(screen.getAllByText("代发明细方案").length).toBeGreaterThan(0);
    expect(screen.getByText("合并提示")).toBeTruthy();

    // Return button
    expect(screen.getByText("返回修改查询")).toBeTruthy();

    // Should NOT show normal summary
    expect(screen.queryByText("知识包摘要")).toBeNull();
  });

  it("clicking sub-question button triggers a new query", async () => {
    const fetchMock = buildFetchMock(CLARIFICATION_RESULT);
    vi.stubGlobal("fetch", fetchMock);

    render(
      <MemoryRouter>
        <KnowledgePackageWorkbenchPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "生成知识包" })).toBeTruthy();
    });

    // First submit to get clarification
    const identifierInput = screen.getAllByPlaceholderText("请输入协议号或客户号")[0];
    await act(async () => {
      fireEvent.change(identifierInput, { target: { value: "P001" } });
    });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "生成知识包" }));
    });

    await waitFor(() => {
      expect(screen.getByText("需要补充条件")).toBeTruthy();
    });

    // Count fetch calls before clicking sub-question
    const callCountBefore = fetchMock.mock.calls.length;

    // Now switch mock to return allow result for the sub-question query
    fetchMock.mockImplementation(async (url) => {
      const path = typeof url === "string" ? url : url.toString();
      if (path.includes("/api/scene-search")) return mockJsonResponse({ candidates: [], reasons: [] });
      if (path.includes("/api/plan-select")) return mockJsonResponse({ candidates: [], reasons: [] });
      if (path.includes("/api/graphrag/query")) return mockJsonResponse(ALLOW_RESULT);
      return mockJsonResponse(null);
    });

    // Click first sub-question button
    const subQuestionButtons = screen.getAllByText("用此子问题检索");
    await act(async () => {
      fireEvent.click(subQuestionButtons[0]);
    });

    // Should make new API calls (scene-search, plan-select, graphrag/query)
    await waitFor(() => {
      expect(fetchMock.mock.calls.length).toBeGreaterThan(callCountBefore);
    });

    // After the sub-question resolves to allow, should show summary
    await waitFor(() => {
      expect(screen.getByText("知识包摘要")).toBeTruthy();
    });
  });

  it("clicking return button clears result and shows empty state", async () => {
    renderWorkbench(CLARIFICATION_RESULT);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "生成知识包" })).toBeTruthy();
    });

    const identifierInput = screen.getAllByPlaceholderText("请输入协议号或客户号")[0];
    await act(async () => {
      fireEvent.change(identifierInput, { target: { value: "P001" } });
    });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "生成知识包" }));
    });

    await waitFor(() => {
      expect(screen.getByText("返回修改查询")).toBeTruthy();
    });

    // Click return button
    await act(async () => {
      fireEvent.click(screen.getByText("返回修改查询"));
    });

    // Should show empty state again
    await waitFor(() => {
      expect(screen.getByText("尚未生成知识包")).toBeTruthy();
    });

    expect(screen.queryByText("需要补充条件")).toBeNull();
    expect(screen.queryByText("知识包摘要")).toBeNull();
  });

  it("shows IDENTIFIER_REQUIRED error when server returns that code", async () => {
    const fetchMock = buildFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    render(
      <MemoryRouter>
        <KnowledgePackageWorkbenchPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "生成知识包" })).toBeTruthy();
    });

    // Override graphrag/query to return error
    fetchMock.mockImplementation(async (url) => {
      const path = typeof url === "string" ? url : url.toString();
      if (path.includes("/api/scenes")) return mockJsonResponse([PUBLISHED_SCENE]);
      if (path.includes("/api/scene-search")) return mockJsonResponse({ candidates: [], reasons: [] });
      if (path.includes("/api/plan-select")) return mockJsonResponse({ candidates: [], reasons: [] });
      if (path.includes("/api/graphrag/query")) {
        return mockErrorResponse(400, { code: "IDENTIFIER_REQUIRED", message: "标识值不能为空" });
      }
      return mockJsonResponse(null);
    });

    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "生成知识包" }));
    });

    await waitFor(() => {
      expect(screen.getByText("请提供协议号或客户号以执行检索。")).toBeTruthy();
    });
  });

  it("switches scenes and keeps coverage badges localized in multi-scene fixtures", async () => {
    const sceneOneBundle = {
      ...SCENE_BUNDLE,
      coverages: [
        {
          id: 101,
          coverageCode: "COV_DETAIL_2021Q1",
          coverageTitle: "代发明细覆盖",
          coverageStatus: "FULL",
          applicablePeriod: "2021-Q1",
        },
      ],
    };
    const sceneTwoBundle = {
      ...SCENE_BUNDLE,
      plans: [{ planId: 2, planCode: "PLAN_PAYROLL_BATCH", planName: "代发批次结果方案" }],
      coverages: [
        {
          id: 201,
          coverageCode: "COV_BATCH_2021Q2",
          coverageTitle: "代发批次覆盖",
          status: "PARTIAL",
          applicablePeriod: "2021-Q2",
        },
      ],
      versions: [{ id: 43, versionTag: "v2", publishedAt: "2026-03-29T10:00:00Z" }],
      projection: { status: "READY", lastProjectedAt: "2026-03-29T10:00:00Z" },
    };

    const fetchMock = vi.fn(async (url) => {
      const path = typeof url === "string" ? url : url.toString();

      if (path.includes("/api/scenes")) {
        return mockJsonResponse([PUBLISHED_SCENE, SECOND_PUBLISHED_SCENE]);
      }
      if (path.includes("/api/scenes/1/versions")) return mockJsonResponse(sceneOneBundle.versions);
      if (path.includes("/api/scenes/2/versions")) return mockJsonResponse(sceneTwoBundle.versions);
      if (path.includes("/api/plans")) return mockJsonResponse(path.includes("sceneId=2") ? sceneTwoBundle.plans : sceneOneBundle.plans);
      if (path.includes("/api/coverage-declarations")) {
        return mockJsonResponse(path.includes("sceneId=2") ? sceneTwoBundle.coverages : sceneOneBundle.coverages);
      }
      if (path.includes("/api/policies")) return mockJsonResponse([]);
      if (path.includes("/api/contract-views")) return mockJsonResponse([]);
      if (path.includes("/api/source-contracts")) return mockJsonResponse([]);
      if (path.includes("/api/publish-checks")) return mockJsonResponse(null);
      if (path.includes("/api/input-slot-schemas")) return mockJsonResponse(sceneOneBundle.inputSlots);
      if (path.includes("/api/output-contracts")) return mockJsonResponse(sceneOneBundle.outputContracts);
      if (path.includes("/api/graphrag/projection")) {
        return mockJsonResponse(path.includes("sceneId=2") ? sceneTwoBundle.projection : sceneOneBundle.projection);
      }
      if (path.includes("/api/scene-search")) return mockJsonResponse({ candidates: [], reasons: [] });
      if (path.includes("/api/plan-select")) return mockJsonResponse({ candidates: [], reasons: [] });
      if (path.includes("/api/graphrag/query")) return mockJsonResponse(ALLOW_RESULT);

      return mockJsonResponse(null);
    });

    vi.stubGlobal("fetch", fetchMock);

    render(
      <MemoryRouter>
        <KnowledgePackageWorkbenchPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "生成知识包" })).toBeTruthy();
    });

    expect(screen.getByText("代发明细覆盖")).toBeTruthy();
    expect(screen.getByText("完整覆盖")).toBeTruthy();

    await act(async () => {
      fireEvent.change(screen.getByLabelText("选择场景"), { target: { value: "2" } });
    });

    await waitFor(() => {
      expect(screen.getByText("代发批次覆盖")).toBeTruthy();
    });
    expect(screen.getByText("部分覆盖")).toBeTruthy();
    expect(screen.queryByText("UNKNOWN")).toBeNull();
  });
});
