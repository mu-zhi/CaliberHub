// @vitest-environment jsdom
import React from "react";
import "@testing-library/jest-dom/vitest";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { DataMapContainer } from "./DataMapContainer";

const mockRootColumn = {
  columnId: "ROOT",
  items: [],
};

vi.mock("../../pages/datamap-adapter", () => ({
  fetchDataMapColumn: vi.fn(async () => mockRootColumn),
  fetchDataMapGraph: vi.fn(async () => ({
    nodes: [],
    edges: [],
    rootNodeId: "scene:1",
    sceneId: 1,
    sceneName: "代发明细查询",
  })),
  fetchDataMapImpactAnalysis: vi.fn(async () => ({
    assetRef: "",
    riskLevel: "LOW",
    graph: null,
    affectedAssets: [],
    recommendedActions: [],
  })),
  fetchDataMapNodeDetail: vi.fn(async () => ({
    assetRef: "",
    node: null,
    attributes: {},
  })),
  fetchSceneDetail: vi.fn(async () => ({
    sceneId: 1,
    sceneCode: "SCN_PAYROLL_DETAIL",
    sceneTitle: "代发明细查询",
  })),
  extractSceneId: vi.fn((node) => {
    const text = `${node?.id || ""}`.trim();
    if (/^scene:/i.test(text)) {
      return Number(text.replace(/^scene:/i, ""));
    }
    return null;
  }),
}));

function renderContainer() {
  return render(
    <MemoryRouter initialEntries={["/map"]}>
      <DataMapContainer viewPreset="map" />
    </MemoryRouter>,
  );
}

beforeEach(() => {
  window.localStorage.clear();
  window.sessionStorage.clear();
});

afterEach(() => {
  cleanup();
});

describe("DataMapContainer scene switch regression", () => {
  it("keeps coverage filter state after browse ↔ lineage mode switching", async () => {
    renderContainer();

    const lineageModeButton = await screen.findByRole("button", { name: "资产图谱" });
    await act(async () => {
      fireEvent.click(lineageModeButton);
    });

    const snapshotInput = await screen.findByPlaceholderText("输入 snapshot_id");
    const objectTypeChip = await screen.findByRole("button", { name: "执行方案" });

    await act(async () => {
      fireEvent.change(snapshotInput, { target: { value: "2026032901" } });
      fireEvent.click(objectTypeChip);
    });

    expect(snapshotInput).toHaveValue("2026032901");
    expect(objectTypeChip.className).toContain("is-active");

    const browseModeButton = await screen.findByRole("button", { name: "浏览模式" });
    await act(async () => {
      fireEvent.click(browseModeButton);
    });

    const lineageModeAgainButton = await screen.findByRole("button", { name: "资产图谱" });
    await act(async () => {
      fireEvent.click(lineageModeAgainButton);
    });

    const retainedSnapshotInput = screen.getByPlaceholderText("输入 snapshot_id");
    const retainedObjectTypeChip = screen.getByRole("button", { name: "执行方案" });

    expect(retainedSnapshotInput).toHaveValue("2026032901");
    expect(retainedObjectTypeChip.className).toContain("is-active");
  });
});
