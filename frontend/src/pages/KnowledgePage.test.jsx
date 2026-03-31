// @vitest-environment jsdom
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { KnowledgePage } from "./KnowledgePage";
import { apiRequest, apiSseRequest } from "../api/client";
import { API_CONTRACTS, buildApiPath } from "../api/contracts";

vi.mock("../api/client", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    apiRequest: vi.fn(),
    apiSseRequest: vi.fn(),
  };
});

vi.mock("../store/authStore", () => ({
  useAuthStore: (selector) => selector({ token: "", username: "tester" }),
}));

vi.mock("../components/knowledge/CandidateEntityGraphPanel", () => ({
  CandidateEntityGraphPanel: ({ graph, selectedNodeId }) => (
    <div data-testid="candidate-graph-panel">
      候选实体图谱 · nodes:{graph?.nodes?.length || 0} · selected:{selectedNodeId || "-"}
    </div>
  ),
}));

const RECENT_IMPORT_TASKS = [
  {
    taskId: "task-9999",
    sourceName: "测试导入任务",
    status: "FAILED",
    currentStep: 2,
    updatedAt: "2026-03-30T10:00:00Z",
    draftTotal: 1,
    draftCount: 1,
    publishedCount: 0,
    discardedCount: 0,
    resumable: true,
  },
];

const RESTORED_IMPORT_TASK = {
  taskId: "task-9999",
  status: "RUNNING",
  sourceType: "PASTE_MD",
  sourceName: "测试导入任务",
  rawText: "原文第 1 行\n原文第 2 行",
  qualityConfirmed: true,
  compareConfirmed: true,
  preprocessResult: {
    importBatchId: "task-9999",
    mode: "RULE_ONLY",
    totalElapsedMs: 1600,
    scenes: [
      {
        scene_title: "代发协议查询",
        domain_guess: "人力域",
        quality: {
          confidence: 0.82,
        },
        inputs: {
          params: [],
          constraints: [],
        },
        outputs: {
          summary: "",
          fields: [],
        },
        sql_variants: [],
        code_mappings: [],
        caveats: [],
      },
    ],
    sceneDrafts: [
      {
        sceneTitle: "代发协议查询",
        sceneId: 101,
        status: "DRAFT",
        confidenceScore: 0.82,
        lowConfidence: false,
      },
    ],
  },
};

beforeEach(() => {
  window.localStorage.clear();
  window.scrollTo = vi.fn();
  global.ResizeObserver = class {
    observe() {}
    disconnect() {}
  };

  apiRequest.mockImplementation(async (path) => {
    if (path === API_CONTRACTS.domains) {
      return [
        {
          id: 1,
          domainName: "人力域",
          domainCode: "HR",
        },
      ];
    }
    if (path === API_CONTRACTS.importTasks) {
      return RECENT_IMPORT_TASKS;
    }
    if (path === buildApiPath("importTaskById", { taskId: "task-9999" })) {
      return RESTORED_IMPORT_TASK;
    }
    if (path === buildApiPath("importTaskCandidateGraph", { taskId: "task-9999" })) {
      return {
        nodes: [],
        edges: [],
      };
    }
    if (path === buildApiPath("minimumUnitCheck", { id: 101 })) {
      return {};
    }
    return {};
  });
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("KnowledgePage status localization", () => {
  it("renders Chinese labels instead of raw English status codes across import workflow sections", async () => {
    render(
      <MemoryRouter>
        <KnowledgePage preset="import" />
      </MemoryRouter>,
    );

    expect(await screen.findByText(/状态 失败/)).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "恢复处理" }));

    expect(await screen.findByText(/状态 处理中/)).toBeTruthy();
    expect(await screen.findByText(/当前场景：#101 · 状态：草稿/)).toBeTruthy();
    expect(await screen.findByText(/当前#101·草稿/)).toBeTruthy();

    const queueTitle = await screen.findByText("1. 代发协议查询");
    expect(within(queueTitle.closest("button")).getByText("草稿")).toBeTruthy();

    expect(screen.queryByText("RUNNING")).toBeNull();
    expect(screen.queryByText("FAILED")).toBeNull();
    expect(screen.queryByText("DRAFT")).toBeNull();
    expect(screen.queryByText(/状态 RUNNING/)).toBeNull();
    expect(screen.queryByText(/状态 FAILED/)).toBeNull();
    expect(screen.queryByText(/状态：DRAFT/)).toBeNull();
    expect(screen.queryByText(/当前#101·DRAFT/)).toBeNull();
  });

  it("keeps live graph patches visible while import stream is still running", async () => {
    let finishStream;
    const streamDone = new Promise((resolve) => {
      finishStream = resolve;
    });

    apiSseRequest.mockImplementation(async (_path, options) => {
      options.onEvent({
        event: "start",
        data: {
          taskId: "task-live-001",
        },
      });
      options.onEvent({
        event: "graph_patch",
        data: {
          graphId: "task-live-001:material-live-001",
          patchSeq: 1,
          summary: "首批补丁",
          focusNodeIds: ["SC-001"],
          addedNodes: [
            {
              nodeCode: "SC-001",
              nodeType: "CANDIDATE_SCENE",
              label: "代发明细查询",
            },
          ],
          addedEdges: [],
        },
      });
      options.onEvent({
        event: "graph_patch",
        data: {
          graphId: "task-live-001:material-live-001",
          patchSeq: 2,
          summary: "第二批补丁",
          focusNodeIds: ["PLN-002"],
          addedNodes: [
            {
              nodeCode: "PLN-002",
              nodeType: "CANDIDATE_PLAN",
              label: "历史补查",
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
      });

      await streamDone;
      return {
        importBatchId: "task-live-001",
        materialId: "material-live-001",
        scenes: [],
      };
    });

    apiRequest.mockImplementation(async (path) => {
      if (path === API_CONTRACTS.domains) {
        return [
          {
            id: 1,
            domainName: "人力域",
            domainCode: "HR",
          },
        ];
      }
      if (path === API_CONTRACTS.importTasks) {
        return RECENT_IMPORT_TASKS;
      }
      if (path === buildApiPath("importTaskCandidateGraph", { taskId: "task-live-001" })) {
        return {
          graphId: "task-live-001:material-live-001",
          nodes: [
            {
              nodeCode: "SC-001",
              nodeType: "CANDIDATE_SCENE",
              label: "代发明细查询",
            },
            {
              nodeCode: "PLN-002",
              nodeType: "CANDIDATE_PLAN",
              label: "历史补查",
            },
          ],
          edges: [
            {
              edgeCode: "EDGE-002",
              edgeType: "SCENE_HAS_PLAN",
              sourceNodeCode: "SC-001",
              targetNodeCode: "PLN-002",
              label: "候选方案",
            },
          ],
        };
      }
      if (path === API_CONTRACTS.importTasks) {
        return RECENT_IMPORT_TASKS;
      }
      return {};
    });

    render(
      <MemoryRouter>
        <KnowledgePage preset="import" />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole("button", { name: "填入最佳实践样例" }));
    fireEvent.click(screen.getByRole("button", { name: "导入并生成草稿" }));

    await waitFor(() => {
      const panelText = screen.getByTestId("candidate-graph-panel").textContent || "";
      expect(panelText).toContain("nodes:2");
      expect(panelText).toContain("selected:SC-001");
    });

    finishStream();
  });
});
