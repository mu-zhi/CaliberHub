// @vitest-environment jsdom
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { HomePage } from "./HomePage";
import { apiRequest } from "../api/client";
import { API_CONTRACTS } from "../api/contracts";

vi.mock("../api/client", () => ({
  apiRequest: vi.fn(),
}));

vi.mock("../api/contracts", () => ({
  API_CONTRACTS: {
    scenes: "/scenes",
  },
}));

const SCENES = [
  {
    id: "scene-reviewed",
    sceneTitle: "已复核场景",
    domainName: "复核域",
    status: "REVIEWED",
    updatedAt: "2026-03-30T10:00:00Z",
  },
  {
    id: "scene-published",
    sceneTitle: "已发布场景",
    domainName: "发布域",
    status: "PUBLISHED",
    updatedAt: "2026-03-30T09:00:00Z",
  },
  {
    id: "scene-discarded",
    sceneTitle: "已弃用场景",
    domainName: "弃用域",
    status: "DISCARDED",
    updatedAt: "2026-03-30T08:00:00Z",
  },
  {
    id: "scene-retired",
    sceneTitle: "已退役场景",
    domainName: "退役域",
    status: "RETIRED",
    updatedAt: "2026-03-30T07:00:00Z",
  },
  {
    id: "scene-draft",
    sceneTitle: "草稿场景",
    domainName: "草稿域",
    status: "DRAFT",
    updatedAt: "2026-03-30T06:00:00Z",
  },
];

beforeEach(() => {
  apiRequest.mockResolvedValue(SCENES);
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("HomePage scene status presentation", () => {
  it("renders chinese status badges for all recent change states", async () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );

    const reviewedRow = await screen.findByText("已复核场景", { selector: "strong" });
    const publishedRow = await screen.findByText("已发布场景", { selector: "strong" });
    const discardedRow = await screen.findByText("已弃用场景", { selector: "strong" });
    const retiredRow = await screen.findByText("已退役场景", { selector: "strong" });
    const draftRow = await screen.findByText("草稿场景", { selector: "strong" });

    expect(within(reviewedRow.closest("article")).getByText("已复核")).toBeTruthy();
    expect(within(publishedRow.closest("article")).getByText("已发布")).toBeTruthy();
    expect(within(discardedRow.closest("article")).getByText("已弃用")).toBeTruthy();
    expect(within(retiredRow.closest("article")).getByText("已退役")).toBeTruthy();
    expect(within(draftRow.closest("article")).getByText("草稿")).toBeTruthy();
    expect(screen.queryByText("REVIEWED")).toBeNull();
    expect(screen.queryByText("PUBLISHED")).toBeNull();
    expect(screen.queryByText("DISCARDED")).toBeNull();
    expect(screen.queryByText("RETIRED")).toBeNull();
    expect(screen.queryByText("DRAFT")).toBeNull();

    expect(apiRequest).toHaveBeenCalledWith(API_CONTRACTS.scenes);
  });
});
