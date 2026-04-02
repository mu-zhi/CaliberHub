// @vitest-environment jsdom
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, useLocation } from "react-router-dom";
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

vi.mock("../components/knowledge/ImportLiveGraphCanvas", () => ({
  ImportLiveGraphCanvas: ({ graphState }) => (
    <div data-testid="candidate-graph-panel">
      候选实体图谱 · nodes:{graphState?.nodes?.length || 0} · selected:{graphState?.selectedNodeId || "-"}
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

const RESTORED_COMPLETED_IMPORT_TASK = {
  taskId: "task-done-0001",
  status: "COMPLETED",
  sourceType: "PASTE_MD",
  sourceName: "已完成导入任务",
  rawText: "原文第 1 行\n原文第 2 行",
  qualityConfirmed: true,
  compareConfirmed: true,
  preprocessResult: {
    importBatchId: "task-done-0001",
    mode: "RULE_ONLY",
    totalElapsedMs: 1600,
    scenes: [
      {
        scene_title: "已发布场景",
        domain_guess: "人力域",
        quality: {
          confidence: 0.91,
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
        sceneTitle: "已发布场景",
        sceneId: 201,
        status: "PUBLISHED",
        confidenceScore: 0.91,
        lowConfidence: false,
      },
    ],
  },
};

function LocationProbe() {
  const location = useLocation();
  return <div data-testid="location-probe">{location.pathname}</div>;
}

beforeEach(() => {
  window.localStorage.clear();
  window.scrollTo = vi.fn();
  globalThis.ResizeObserver = class {
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
    if (path === "/scenes/101/governance-gaps") {
      return {
        publishReady: false,
        failedRules: [
          {
            ruleCode: "GR-DICT-001",
            name: "字典治理对象",
            message: "场景至少需要 1 个活动中的 Dictionary（字典）",
          },
        ],
        openBlockingGaps: [
          {
            taskCode: "GAP-GOV-101-GR-DICT-001",
            taskTitle: "字典治理对象缺口",
          },
        ],
        summary: "失败规则：字典治理对象；阻断缺口：字典治理对象缺口",
      };
    }
    return {};
  });
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("KnowledgePage status localization", () => {
  it("hides cross-workbench links until the current import task has usable context", async () => {
    render(
      <MemoryRouter>
        <KnowledgePage preset="import" />
      </MemoryRouter>,
    );

    expect(screen.getByRole("button", { name: "载入样例" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "查看数据地图" })).toBeNull();
    expect(screen.queryByRole("button", { name: "查看运行决策台" })).toBeNull();

    fireEvent.click(screen.getByRole("button", { name: "导入并生成草稿" }));
    fireEvent.click(await screen.findByRole("button", { name: "恢复处理" }));

    expect(await screen.findByRole("button", { name: "查看数据地图" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "查看运行决策台" })).toBeTruthy();
  });

  it("renders Chinese labels instead of raw English status codes across import workflow sections", async () => {
    render(
      <MemoryRouter>
        <KnowledgePage preset="import" />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole("button", { name: "导入并生成草稿" }));
    expect(await screen.findByText(/状态 失败/)).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "恢复处理" }));

    expect(await screen.findByText(/状态 处理中/)).toBeTruthy();
    expect(screen.getByRole("button", { name: "继续处理当前场景" })).toBeTruthy();
    expect(screen.getAllByText(/当前任务停留在场景整理与发布/).length).toBeGreaterThan(0);
    fireEvent.click(screen.getByRole("button", { name: "继续处理当前场景" }));
    await waitFor(() => {
      expect(apiRequest).toHaveBeenCalledWith("/scenes/101/governance-gaps", { token: "" });
    });

    expect(screen.getByText(/推荐处理对象：/)).toBeTruthy();
    expect(screen.getByText(/代发协议查询/)).toBeTruthy();

    expect(screen.queryByText("RUNNING")).toBeNull();
    expect(screen.queryByText("FAILED")).toBeNull();
    expect(screen.queryByText("DRAFT")).toBeNull();
    expect(screen.queryByText(/状态 RUNNING/)).toBeNull();
    expect(screen.queryByText(/状态 FAILED/)).toBeNull();
    expect(screen.queryByText(/状态：DRAFT/)).toBeNull();
  });

  it("marks the four-step area as historical summaries and uses contextual rewind labels", async () => {
    render(
      <MemoryRouter>
        <KnowledgePage preset="import" />
      </MemoryRouter>,
    );

    expect(screen.getByText("历史步骤摘要")).toBeTruthy();
    expect(screen.queryByRole("heading", { name: "01 导入并生成草稿", level: 2 })).toBeNull();
    expect(screen.getByRole("region", { name: "历史步骤摘要" })).toBeTruthy();
    expect(screen.getByText("01-04 仅用于回看详情与有限回跳")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "修改" })).toBeNull();
    expect(screen.getAllByRole("button", { name: "回到此阶段" })).toHaveLength(1);
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
              id: "SC-001",
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
              id: "PLN-002",
              nodeType: "CANDIDATE_PLAN",
              label: "历史补查",
            },
          ],
          addedEdges: [
            {
              id: "EDGE-002",
              relationType: "SCENE_HAS_PLAN",
              sourceId: "SC-001",
              targetId: "PLN-002",
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
              id: "SC-001",
              nodeType: "CANDIDATE_SCENE",
              label: "代发明细查询",
            },
            {
              id: "PLN-002",
              nodeType: "CANDIDATE_PLAN",
              label: "历史补查",
            },
          ],
          edges: [
            {
              id: "EDGE-002",
              relationType: "SCENE_HAS_PLAN",
              sourceId: "SC-001",
              targetId: "PLN-002",
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

    fireEvent.click(screen.getByRole("button", { name: "导入并生成草稿" }));
    fireEvent.click(screen.getByRole("button", { name: "填入最佳实践样例" }));
    const importButtons = screen.getAllByRole("button", { name: "导入并生成草稿" });
    fireEvent.click(importButtons[importButtons.length - 1]);

    await waitFor(() => {
      const panelText = screen.getByTestId("candidate-graph-panel").textContent || "";
      expect(panelText).toContain("nodes:2");
      expect(panelText).toContain("selected:-");
    });

    finishStream();

    expect(await screen.findByText(/状态 待质检/)).toBeTruthy();
    const finalPanelText = screen.getByTestId("candidate-graph-panel").textContent || "";
    expect(finalPanelText).toContain("nodes:2");
    expect(finalPanelText).toContain("selected:-");
  });

  it("navigates the completed mainline CTA to map scenes", async () => {
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
        return [
          {
            taskId: "task-done-0001",
            sourceName: "已完成导入任务",
            status: "COMPLETED",
            currentStep: 4,
            updatedAt: "2026-04-01T10:00:00Z",
            draftTotal: 1,
            draftCount: 0,
            publishedCount: 1,
            discardedCount: 0,
            resumable: true,
          },
        ];
      }
      if (path === buildApiPath("importTaskById", { taskId: "task-done-0001" })) {
        return RESTORED_COMPLETED_IMPORT_TASK;
      }
      if (path === buildApiPath("importTaskCandidateGraph", { taskId: "task-done-0001" })) {
        return {
          nodes: [],
          edges: [],
        };
      }
      if (path === buildApiPath("minimumUnitCheck", { id: 201 })) {
        return {};
      }
      if (path === "/scenes/201/governance-gaps") {
        return {
          publishReady: true,
          failedRules: [],
          openBlockingGaps: [],
          summary: "治理规则已通过，当前无阻断级缺口。",
        };
      }
      return {};
    });

    render(
      <MemoryRouter initialEntries={["/production/ingest"]}>
        <KnowledgePage preset="import" />
        <LocationProbe />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole("button", { name: "导入并生成草稿" }));
    fireEvent.click(await screen.findByRole("button", { name: "恢复处理" }));

    expect(await screen.findByRole("heading", { name: "当前导入任务已处理完成" })).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "查看已处理场景" }));

    await waitFor(() => {
      expect(screen.getByTestId("location-probe").textContent).toBe("/map/scenes");
    });
  });
});
