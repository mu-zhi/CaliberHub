// @vitest-environment jsdom
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { ApprovalExportPage } from "./ApprovalExportPage";
import { apiRequest } from "../api/client";
import { API_CONTRACTS, buildApiPath } from "../api/contracts";
import { readValidatedWorkbenchContext } from "../navigation/workbenchContext";
import { resolveApprovalContextState } from "../navigation/workbenchContextReceivers";

vi.mock("../api/client", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    apiRequest: vi.fn(),
  };
});

vi.mock("../store/authStore", () => ({
  useAuthStore: (selector) => selector({ token: "", username: "tester" }),
}));

vi.mock("../navigation/workbenchContext", () => ({
  readValidatedWorkbenchContext: vi.fn(() => ({ ok: true, context: null, message: "" })),
}));

vi.mock("../navigation/workbenchContextReceivers", () => ({
  resolveApprovalContextState: vi.fn(() => ({
    readOnly: false,
    banner: null,
    summary: {
      traceId: "",
      sceneCode: "",
      planCode: "",
      snapshotId: "",
      inferenceSnapshotId: "",
      requestedFields: [],
      purpose: "",
    },
  })),
}));

const SCENE_ROWS = [
  {
    id: 101,
    sceneCode: "SCN_PAYROLL_DETAIL",
    sceneTitle: "代发明细查询",
    status: "PUBLISHED",
  },
  {
    id: 202,
    sceneCode: "SCN_ACCOUNT_OPENING_BRANCH_CHANGE",
    sceneTitle: "客户开户机构变更",
    status: "PUBLISHED",
  },
];

const SERVICE_SPEC_HISTORY = [
  {
    sceneId: 101,
    specCode: "SPEC-SCN_PAYROLL_DETAIL",
    specVersion: 2,
    exportedBy: "审批机器人",
    exportedAt: "2026-03-27T10:32:00Z",
  },
  {
    sceneId: 102,
    specCode: "SPEC-SCN_PAYROLL_BATCH",
    specVersion: 1,
    exportedBy: "合规专员-王宁",
    exportedAt: "2026-03-26T07:42:00Z",
  },
];

const SESSION_EXPORT_RESPONSE = {
  sceneId: 101,
  specCode: "SPEC-SCN_PAYROLL_DETAIL",
  specVersion: 3,
  exportedBy: "system",
  exportedAt: "2026-03-28T10:32:00Z",
  specJson: "{\"sceneCode\":\"SCN_PAYROLL_DETAIL\"}",
};

let originalLocalStorage;
let originalSessionStorage;

beforeEach(() => {
  const store = {};
  const mockStorage = {
    getItem: vi.fn((key) => store[key] || null),
    setItem: vi.fn((key, value) => { store[key] = value; }),
    removeItem: vi.fn((key) => { delete store[key]; }),
  };
  originalLocalStorage = globalThis.localStorage;
  originalSessionStorage = globalThis.sessionStorage;
  vi.stubGlobal("localStorage", mockStorage);
  vi.stubGlobal("sessionStorage", mockStorage);
  readValidatedWorkbenchContext.mockReturnValue({ ok: true, context: null, message: "" });
  resolveApprovalContextState.mockReturnValue({
    readOnly: false,
    banner: null,
    summary: {
      traceId: "",
      sceneCode: "",
      planCode: "",
      snapshotId: "",
      inferenceSnapshotId: "",
      requestedFields: [],
      purpose: "",
    },
  });
  apiRequest.mockImplementation(async (path) => {
    if (path === API_CONTRACTS.scenes) {
      return SCENE_ROWS;
    }
    if (path === API_CONTRACTS.serviceSpecs) {
      return SERVICE_SPEC_HISTORY;
    }
    if (path === buildApiPath("serviceSpecExport", { sceneId: 101 })) {
      return SESSION_EXPORT_RESPONSE;
    }
    return null;
  });
});

afterEach(() => {
  if (originalLocalStorage) {
    globalThis.localStorage = originalLocalStorage;
  }
  if (originalSessionStorage) {
    globalThis.sessionStorage = originalSessionStorage;
  }
  vi.restoreAllMocks();
  cleanup();
});

describe("ApprovalExportPage actions", () => {
  it("loads real export history and appends the returned record after masked export", async () => {
    render(
      <MemoryRouter>
        <ApprovalExportPage />
      </MemoryRouter>,
    );

    await screen.findByText("SPEC-SCN_PAYROLL_DETAIL#2");

    fireEvent.click(screen.getByRole("button", { name: "改为脱敏导出" }));

    await waitFor(() => {
      expect(apiRequest).toHaveBeenCalledWith(
        buildApiPath("serviceSpecExport", { sceneId: 101 }),
        expect.objectContaining({
          method: "POST",
          body: { operator: "system" },
        }),
      );
    });

    expect(await screen.findByText("SPEC-SCN_PAYROLL_DETAIL#3")).toBeTruthy();
    expect(screen.getByText("已将「代发明细查询」改为脱敏导出，已生成真实导出记录。")).toBeTruthy();
  });

  it("keeps loaded history visible when the real export call fails", async () => {
    apiRequest.mockImplementation(async (path) => {
      if (path === API_CONTRACTS.scenes) {
        return SCENE_ROWS;
      }
      if (path === API_CONTRACTS.serviceSpecs) {
        return SERVICE_SPEC_HISTORY;
      }
      if (path === buildApiPath("serviceSpecExport", { sceneId: 101 })) {
        throw new Error("真实导出失败");
      }
      return null;
    });

    render(
      <MemoryRouter>
        <ApprovalExportPage />
      </MemoryRouter>,
    );

    await screen.findByText("SPEC-SCN_PAYROLL_DETAIL#2");

    fireEvent.click(screen.getByRole("button", { name: "改为脱敏导出" }));

    expect(await screen.findByText("脱敏导出失败，请稍后重试。")).toBeTruthy();
    expect(screen.getByText("SPEC-SCN_PAYROLL_DETAIL#2")).toBeTruthy();
    expect(screen.getByText("SPEC-SCN_PAYROLL_BATCH#1")).toBeTruthy();
  });

  it("uses the newly selected approval scene instead of the frozen context scene when exporting", async () => {
    resolveApprovalContextState.mockReturnValue({
      readOnly: true,
      banner: null,
      summary: {
        traceId: "trace-approval-001",
        sceneCode: "SCN_PAYROLL_DETAIL",
        planCode: "PLAN_PAYROLL_DETAIL",
        snapshotId: "42",
        inferenceSnapshotId: "42",
        requestedFields: ["协议号"],
        purpose: "工单核验",
      },
    });
    apiRequest.mockImplementation(async (path) => {
      if (path === API_CONTRACTS.scenes) {
        return SCENE_ROWS;
      }
      if (path === API_CONTRACTS.serviceSpecs) {
        return SERVICE_SPEC_HISTORY;
      }
      if (path === buildApiPath("serviceSpecExport", { sceneId: 202 })) {
        return {
          sceneId: 202,
          specCode: "SPEC-SCN_ACCOUNT_OPENING_BRANCH_CHANGE",
          specVersion: 2,
          exportedBy: "system",
          exportedAt: "2026-03-28T10:42:00Z",
        };
      }
      throw new Error(`unexpected path: ${path}`);
    });

    render(
      <MemoryRouter>
        <ApprovalExportPage />
      </MemoryRouter>,
    );

    await screen.findByText("SPEC-SCN_PAYROLL_DETAIL#2");

    fireEvent.click(screen.getByRole("button", { name: /客户开户机构变更/ }));
    fireEvent.click(screen.getByRole("button", { name: "改为脱敏导出" }));

    await waitFor(() => {
      expect(apiRequest).toHaveBeenCalledWith(
        buildApiPath("serviceSpecExport", { sceneId: 202 }),
        expect.objectContaining({
          method: "POST",
        }),
      );
    });

    expect(await screen.findByText("SPEC-SCN_ACCOUNT_OPENING_BRANCH_CHANGE#2")).toBeTruthy();
    expect(screen.getByText("已将「客户开户机构变更」改为脱敏导出，已生成真实导出记录。")).toBeTruthy();
  });
});
