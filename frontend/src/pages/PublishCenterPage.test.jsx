// @vitest-environment jsdom
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { PublishCenterPage } from "./PublishCenterPage";

const SCENES = [
  {
    id: 1,
    sceneCode: "SCN_PAYROLL_DRAFT",
    sceneTitle: "代发草稿场景",
    sceneType: "FACT_DETAIL",
    status: "DRAFT",
    updatedAt: "2026-03-30T10:00:00Z",
  },
  {
    id: 2,
    sceneCode: "SCN_PAYROLL_PUBLISHED",
    sceneTitle: "代发已发布场景",
    sceneType: "FACT_DETAIL",
    status: "PUBLISHED",
    updatedAt: "2026-03-30T09:00:00Z",
  },
];

const PUBLISH_CHECK = {
  publishReady: true,
  items: [
    { key: "projection", name: "图谱投影", message: "投影已完成", passed: true },
  ],
};

const VERSIONS = [
  {
    id: 11,
    versionTag: "v2026.03.30",
    publishStatus: "PUBLISHED",
    changeSummary: "完成样板发布",
    publishedAt: "2026-03-30T10:05:00Z",
    snapshotId: 101,
    inferenceSnapshotId: 201,
  },
];

const PROJECTION = {
  status: "READY",
  message: "图谱投影已就绪",
  lastProjectedAt: "2026-03-30T10:06:00Z",
};

function mockJsonResponse(payload) {
  return {
    ok: true,
    status: 200,
    headers: { get: () => "" },
    json: async () => payload,
  };
}

function buildFetchMock() {
  return vi.fn(async (url) => {
    const path = typeof url === "string" ? url : url.toString();
    if (path.includes("/api/scenes")) return mockJsonResponse(SCENES);
    if (path.includes("/api/publish-checks")) return mockJsonResponse(PUBLISH_CHECK);
    if (path.includes("/versions")) return mockJsonResponse(VERSIONS);
    if (path.includes("/api/graphrag/projection")) return mockJsonResponse(PROJECTION);
    if (path.includes("/api/plans")) return mockJsonResponse([]);
    if (path.includes("/api/coverage-declarations")) return mockJsonResponse([]);
    if (path.includes("/api/policies")) return mockJsonResponse([]);
    if (path.includes("/api/contract-views")) return mockJsonResponse([]);
    if (path.includes("/api/source-contracts")) return mockJsonResponse([]);
    if (path.includes("/api/output-contracts")) return mockJsonResponse([]);
    if (path.includes("/api/input-slot-schemas")) return mockJsonResponse([]);
    return mockJsonResponse(null);
  });
}

let originalFetch;

beforeEach(() => {
  originalFetch = globalThis.fetch;
  vi.stubGlobal("fetch", buildFetchMock());
});

afterEach(() => {
  globalThis.fetch = originalFetch;
  vi.restoreAllMocks();
  cleanup();
});

describe("PublishCenterPage status presentation", () => {
  it("renders Chinese labels for scene, projection, and version statuses", async () => {
    render(
      <MemoryRouter>
        <PublishCenterPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText("发布中心")).toBeTruthy();
    });

    await waitFor(() => {
      expect(screen.getAllByText("代发草稿场景").length).toBeGreaterThan(0);
    });

    expect(screen.getAllByText("草稿").length).toBeGreaterThan(0);
    expect(screen.getAllByText("已发布").length).toBeGreaterThan(0);
    expect(screen.getAllByText("已就绪").length).toBeGreaterThan(0);
    expect(screen.queryByText("DRAFT")).toBeNull();
    expect(screen.queryByText("PUBLISHED")).toBeNull();
    expect(screen.queryByText("READY")).toBeNull();
  });
});
