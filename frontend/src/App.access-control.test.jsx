// @vitest-environment jsdom
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

let currentRole = "support";
let currentRoles = ["SUPPORT"];
let currentToken = "";
let currentTokenExpireAt = "";
let currentUsername = "";
const setRoleMock = vi.fn();
const loginByRoleMock = vi.fn();
const logoutMock = vi.fn();

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
    token: currentToken,
    tokenExpireAt: currentTokenExpireAt,
    username: currentUsername,
    setRole: setRoleMock,
    loginByRole: loginByRoleMock,
    logout: logoutMock,
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
    currentToken = "";
    currentTokenExpireAt = "";
    currentUsername = "";
    setRoleMock.mockReset();
    loginByRoleMock.mockReset();
    logoutMock.mockReset();
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

  it("submits current role password from topbar auth entry", async () => {
    loginByRoleMock.mockResolvedValue(true);

    renderAppAt("/map");

    const passwordInput = await screen.findByLabelText("当前角色密码");
    fireEvent.change(passwordInput, { target: { value: "support123" } });
    fireEvent.click(screen.getByRole("button", { name: "登录" }));

    expect(loginByRoleMock).toHaveBeenCalledWith("support", "support123");
  });

  it("shows logged-in summary and logout action", async () => {
    currentToken = "jwt-token";
    currentTokenExpireAt = "2026-04-06T12:00:00.000Z";
    currentUsername = "admin";
    currentRoles = ["ADMIN", "SUPPORT"];

    renderAppAt("/map");

    expect(await screen.findByText("admin")).toBeTruthy();
    expect(screen.getByText("ADMIN / SUPPORT")).toBeTruthy();
    expect(screen.getByRole("button", { name: "退出" })).toBeTruthy();
  });

  it("clears session immediately when switching role", async () => {
    currentToken = "jwt-token";
    currentTokenExpireAt = "2026-04-06T12:00:00.000Z";
    currentUsername = "support";

    renderAppAt("/map");

    fireEvent.change(await screen.findByLabelText("当前角色"), { target: { value: "admin" } });

    expect(setRoleMock).toHaveBeenCalledWith("admin");
  });
});
