// @vitest-environment jsdom
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

let currentRole = "support";
let currentRoles = ["SUPPORT"];

vi.mock("./pages/HomePage", () => ({
  HomePage: () => <div>home page</div>,
}));
vi.mock("./pages/DomainManagementPage", () => ({
  DomainManagementPage: () => <div>domain page</div>,
}));
vi.mock("./pages/AssetsPage", () => ({
  AssetsPage: () => <div>assets page</div>,
}));
vi.mock("./pages/ApprovalExportPage", () => ({
  ApprovalExportPage: () => <div>approval page</div>,
}));
vi.mock("./pages/MonitoringAuditPage", () => ({
  MonitoringAuditPage: () => <div>monitoring page</div>,
}));
vi.mock("./pages/PublishCenterPage", () => ({
  PublishCenterPage: () => <div>publish page</div>,
}));
vi.mock("./pages/WorkspacePage", () => ({
  WorkspacePage: () => <div>workspace page</div>,
}));
vi.mock("./pages/SystemPage", () => ({
  SystemPage: () => <div>system page</div>,
}));
vi.mock("./pages/NotFoundPage", () => ({
  NotFoundPage: () => <div>not found</div>,
}));
vi.mock("./pages/PrototypeIndexPage", () => ({
  PrototypeIndexPage: () => <div>prototype index</div>,
}));
vi.mock("./pages/KnowledgePage", () => ({
  KnowledgePage: () => <div>knowledge page</div>,
}));
vi.mock("./pages/KnowledgePackageWorkbenchPage", () => ({
  KnowledgePackageWorkbenchPage: () => <div>runtime page</div>,
}));
vi.mock("./pages/prototypes/PrototypeIngestGraphPage", () => ({
  PrototypeIngestGraphPage: () => <div>prototype ingest</div>,
}));
vi.mock("./pages/prototypes/PrototypeModelingDualPanePage", () => ({
  PrototypeModelingDualPanePage: () => <div>prototype modeling</div>,
}));
vi.mock("./pages/prototypes/PrototypeRuntimePublishPage", () => ({
  PrototypeRuntimePublishPage: () => <div>prototype runtime</div>,
}));
vi.mock("./components/BrandMark", () => ({
  BrandMark: () => <span>brand</span>,
}));
vi.mock("./components/AppErrorBoundary", () => ({
  AppErrorBoundary: ({ children }) => children,
}));
vi.mock("./store/authStore", () => ({
  useAuthStore: (selector) => selector({
    role: currentRole,
    roles: currentRoles,
    setRole: vi.fn(),
  }),
}));
vi.mock("./store/appStore", () => ({
  useAppStore: (selector) => selector({
    navCollapsed: false,
    setNavCollapsed: vi.fn(),
    recordRecent: vi.fn(),
  }),
}));

import App from "./App";

function renderAppAt(pathname) {
  return render(
    <MemoryRouter initialEntries={[pathname]}>
      <App />
    </MemoryRouter>,
  );
}

describe("App route access control", () => {
  beforeEach(() => {
    currentRole = "support";
    currentRoles = ["SUPPORT"];
  });

  afterEach(() => {
    cleanup();
  });

  it("redirects unsupported direct route access back to overview", async () => {
    renderAppAt("/approval");

    expect(await screen.findByText("home page")).toBeTruthy();
    expect(screen.queryByText("approval page")).toBeNull();
  });

  it("allows direct route access when the role has permission", async () => {
    currentRole = "compliance";
    currentRoles = ["COMPLIANCE"];

    renderAppAt("/approval");

    expect(await screen.findByText("approval page")).toBeTruthy();
    expect(screen.queryByText("home page")).toBeNull();
  });
});
