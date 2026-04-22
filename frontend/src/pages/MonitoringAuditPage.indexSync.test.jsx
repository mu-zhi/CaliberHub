// @vitest-environment jsdom
import React from "react";
import { afterEach, describe, expect, it } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { MonitoringAuditPage } from "./MonitoringAuditPage";

afterEach(() => {
  cleanup();
});

describe("MonitoringAuditPage index sync summary", () => {
  it("renders published snapshot index lock summary", () => {
    render(
      <MemoryRouter>
        <MonitoringAuditPage />
      </MemoryRouter>,
    );

    expect(screen.getByText("实验索引版本")).toBeTruthy();
    expect(screen.getByText("ACTIVE")).toBeTruthy();
    expect(screen.getByText("SCN_PAYROLL_DETAIL::SCN_PAYROLL_DETAIL-V001::42")).toBeTruthy();
    expect(screen.getByText("snapshot mismatch 0")).toBeTruthy();
  });
});
