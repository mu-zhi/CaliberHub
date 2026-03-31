/// <reference types="vitest" />
/* @vitest-environment jsdom */
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderToString } from "react-dom/server";
import { MemoryRouter } from "react-router-dom";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { DataMapContainer } from "../components/datamap/DataMapContainer";
import { buildWorkbenchHref } from "../navigation/workbenchContext";
import { ApprovalExportPage } from "./ApprovalExportPage";
import { KnowledgePackageWorkbenchPage } from "./KnowledgePackageWorkbenchPage";
import { MonitoringAuditPage } from "./MonitoringAuditPage";

function renderPage(entry, element) {
  return renderToString(
    <MemoryRouter initialEntries={[entry]}>
      {element}
    </MemoryRouter>,
  );
}

let consoleErrorSpy;
let originalFetch;
let originalLocalStorage;
let originalSessionStorage;

const CLARIFICATION_RESULT = {
  decision: "clarification_only",
  reasonCode: "MULTI_SCENE_AMBIGUOUS",
  clarification: {
    summary: "当前问题同时命中代发明细查询和代发批次结果查询，请拆分后分别检索",
    sceneCandidates: [
      { sceneId: 1, sceneCode: "SCN_PAYROLL_DETAIL", sceneTitle: "代发明细查询", snapshotId: 42 },
      { sceneId: 2, sceneCode: "SCN_PAYROLL_BATCH", sceneTitle: "代发批次结果查询", snapshotId: 43 },
    ],
    subQuestions: [
      "按协议号查询代发明细",
      "按公司户查询代发批次结果",
    ],
    clarificationQuestions: [
      "请先确认当前问题的主语是明细查询还是批次结果？",
    ],
  },
  trace: { traceId: "trace_clarification_20260331_01", snapshotId: null },
  risk: { riskLevel: "MEDIUM", riskReasons: ["跨场景多意图需要拆分"] },
};

function mockJsonResponse(payload) {
  return {
    ok: true,
    status: 200,
    headers: { get: () => "" },
    json: async () => payload,
  };
}

function buildFetchMock(graphQueryResult = CLARIFICATION_RESULT) {
  return vi.fn(async (url) => {
    const path = typeof url === "string" ? url : url.toString();

    if (path.includes("/api/scenes")) {
      return mockJsonResponse([
        {
          id: 1,
          sceneCode: "SCN_PAYROLL_DETAIL",
          sceneTitle: "代发明细场景",
          sceneType: "FACT_DETAIL",
          sceneDescription: "代发明细检索",
          status: "PUBLISHED",
          domainId: 10,
          publishedAt: "2026-03-28T10:00:00Z",
        },
      ]);
    }
    if (path.includes("/api/plans")) return mockJsonResponse([{ id: 1, planId: 1, planCode: "PLAN_PAYROLL_DETAIL", planName: "代发明细方案" }]);
    if (path.includes("/api/coverage-declarations")) return mockJsonResponse([]);
    if (path.includes("/api/policies")) return mockJsonResponse([]);
    if (path.includes("/api/contract-views")) return mockJsonResponse([]);
    if (path.includes("/api/source-contracts")) return mockJsonResponse([]);
    if (path.includes("/api/publish-checks")) return mockJsonResponse(null);
    if (path.includes("/api/scenes/1/versions")) return mockJsonResponse([{ id: 42, versionTag: "v1", publishedAt: "2026-03-28T10:00:00Z" }]);
    if (path.includes("/api/input-slot-schemas")) return mockJsonResponse([{
      slotCode: "PROTOCOL_NBR",
      slotName: "协议号",
      identifierCandidatesJson: '["PROTOCOL_NBR"]',
    }]);
    if (path.includes("/api/output-contracts")) return mockJsonResponse([{ fieldsJson: '["协议号","交易日期","金额"]' }]);
    if (path.includes("/api/graphrag/projection")) return mockJsonResponse({ status: "READY", lastProjectedAt: "2026-03-28T10:00:00Z" });

    if (path.includes("/api/scene-search")) {
      return mockJsonResponse({ candidates: [{ sceneId: 1, sceneTitle: "代发明细查询" }], reasons: ["关键词命中"] });
    }
    if (path.includes("/api/plan-select")) {
      return mockJsonResponse({ candidates: [{ sceneId: 1, planId: 1, planName: "代发明细方案", decision: "allow" }], reasons: ["首选方案命中"] });
    }
    if (path.includes("/api/graphrag/query")) {
      return mockJsonResponse(graphQueryResult);
    }
    return mockJsonResponse(null);
  });
}

beforeEach(() => {
  const originalError = console.error;
  consoleErrorSpy = vi.spyOn(console, "error").mockImplementation((...args) => {
    const [firstArg] = args;
    if (typeof firstArg === "string" && firstArg.includes("useLayoutEffect does nothing on the server")) {
      return;
    }
    originalError(...args);
  });

  const store = {};
  const mockStorage = {
    getItem: vi.fn((key) => store[key] || null),
    setItem: vi.fn((key, value) => {
      store[key] = value;
    }),
    removeItem: vi.fn((key) => {
      delete store[key];
    }),
  };
  originalLocalStorage = globalThis.localStorage;
  originalSessionStorage = globalThis.sessionStorage;
  globalThis.localStorage = mockStorage;
  globalThis.sessionStorage = mockStorage;
  originalFetch = globalThis.fetch;
});

afterEach(() => {
  consoleErrorSpy?.mockRestore();
  vi.restoreAllMocks();
  globalThis.fetch = originalFetch;
  if (originalLocalStorage) {
    globalThis.localStorage = originalLocalStorage;
  }
  if (originalSessionStorage) {
    globalThis.sessionStorage = originalSessionStorage;
  }
  cleanup();
});

describe("workbench context page rendering", () => {
  it("renders runtime page with replay banner", () => {
    const entry = buildWorkbenchHref("/runtime", {
      source_workbench: "monitoring",
      target_workbench: "runtime",
      intent: "replay_trace",
      trace_id: "trace_runtime_20260327_07",
      snapshot_id: "snapshot-20260327-01",
      inference_snapshot_id: "inference-20260327-01",
      lock_mode: "replay",
    });

    const html = renderPage(entry, <KnowledgePackageWorkbenchPage />);
    expect(html).toContain("历史回放态");
  });

  it("renders approval page with frozen context summary", () => {
    const entry = buildWorkbenchHref("/approval", {
      source_workbench: "runtime",
      target_workbench: "approval",
      intent: "submit_approval",
      trace_id: "trace_runtime_20260327_07",
      scene_code: "SCN_PAYROLL_DETAIL",
      plan_code: "PLAN_PAYROLL_DETAIL",
      snapshot_id: "snapshot-20260327-01",
      inference_snapshot_id: "inference-20260327-01",
      requested_fields: ["协议号", "交易日期", "金额"],
      purpose: "工单核验",
      lock_mode: "frozen",
    });

    const html = renderPage(entry, <ApprovalExportPage />);
    expect(html).toContain("版本冻结态");
    expect(html).toContain("trace_runtime_20260327_07");
    expect(html).toContain("协议号");
    expect(html).toContain("工单核验");
  });

  it("renders approval page with structured error when frozen context is invalid", () => {
    const entry = buildWorkbenchHref("/approval", {
      source_workbench: "runtime",
      target_workbench: "approval",
      intent: "submit_approval",
      trace_id: "trace_runtime_20260327_07",
      snapshot_id: "snapshot-20260327-01",
      lock_mode: "frozen",
    });

    const html = renderPage(entry, <ApprovalExportPage />);
    expect(html).toContain("snapshot_id");
    expect(html).toContain("inference_snapshot_id");
  });

  it("renders monitoring page with context-package links", () => {
    const html = renderPage("/monitoring", <MonitoringAuditPage />);
    expect(html).toContain("/runtime?ctx=");
    expect(html).toContain("/map?ctx=");
  });

  it("renders data map with replay banner", () => {
    const entry = buildWorkbenchHref("/map", {
      source_workbench: "monitoring",
      target_workbench: "map",
      intent: "view_node",
      asset_ref: "plan:PLAN_PAYROLL_DETAIL",
      snapshot_id: "snapshot-20260327-01",
      inference_snapshot_id: "inference-20260327-01",
      lock_mode: "replay",
    });

    const html = renderPage(entry, <DataMapContainer viewPreset="map" />);
    expect(html).toContain("历史回放态");
  });

  it("renders data map with snapshot-scoped context from monitoring", () => {
    const entry = buildWorkbenchHref("/map", {
      source_workbench: "monitoring",
      target_workbench: "map",
      intent: "view_node",
      asset_ref: "plan:PLAN_PAYROLL_DETAIL",
      snapshot_id: "2026032901",
      inference_snapshot_id: "inference-2026032901",
      lock_mode: "replay",
    });

    const html = renderPage(entry, <DataMapContainer viewPreset="map" />);
    expect(html).toContain("历史回放态");
  });

  it("renders clarification instead of fake mixed knowledge package for ambiguous payroll question", async () => {
    globalThis.fetch = buildFetchMock(CLARIFICATION_RESULT);

    render(
      <MemoryRouter>
        <KnowledgePackageWorkbenchPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "生成知识包" })).toBeTruthy();
    });

    const questionInput = screen.getAllByRole("textbox").find((node) => {
      if (node.tagName !== "TEXTAREA") {
        return false;
      }
      return node.value.includes("代发明细场景");
    });

    if (!questionInput) {
      throw new Error("问题输入 textarea 未命中");
    }

    questionInput.readOnly = false;
    fireEvent.change(questionInput, { target: { value: "查询公司户最近一年代发批次和协议号明细" } });
    fireEvent.click(screen.getByRole("button", { name: "生成知识包" }));

    expect(await screen.findByText("需要补充条件")).toBeTruthy();
    expect(await screen.findByText("按协议号查询代发明细")).toBeTruthy();
    expect(await screen.findByText("按公司户查询代发批次结果")).toBeTruthy();
    expect(await screen.findByText("当前问题同时命中代发明细查询和代发批次结果查询，请拆分后分别检索")).toBeTruthy();
  });
});
