// @vitest-environment jsdom
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { apiRequest, apiRequestWithMeta } from "../api/client";
import { MonitoringAuditPage } from "./MonitoringAuditPage";
import { SystemPage } from "./SystemPage";

vi.mock("../api/client", () => ({
  apiRequest: vi.fn(),
  apiRequestWithMeta: vi.fn(),
}));

const LLM_CONFIG = {
  enabled: true,
  endpoint: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
  model: "qwen3-max",
  timeoutSeconds: 35,
  temperature: 0,
  maxTokens: 4096,
  enableThinking: false,
  fallbackToRule: true,
  hasApiKey: false,
  configSource: "DEFAULT_PROPERTIES",
  endpointHost: "dashscope.aliyuncs.com",
  fallbackStrategy: "RULE",
  providerLabel: "DashScope",
  supportsResponsesApi: false,
  supportsStructuredOutputs: true,
  supportsThinkingToggle: true,
  updatedBy: "ops-admin",
  updatedAt: "2026-04-22 10:20:00",
};

beforeEach(() => {
  apiRequest.mockResolvedValue(LLM_CONFIG);
  apiRequestWithMeta.mockResolvedValue({ data: {}, meta: { requestId: "REQ-08D" } });
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("retrieval experiment monitoring and system governance", () => {
  it("renders retrieval experiment summary on monitoring page", () => {
    render(
      <MemoryRouter>
        <MonitoringAuditPage />
      </MemoryRouter>,
    );

    expect(screen.getByText("检索实验评测")).toBeTruthy();
    expect(screen.getByText("Shadow Mode 已开启")).toBeTruthy();
    expect(screen.getByText("scene hit@5")).toBeTruthy();
    expect(screen.getByText("误放行风险")).toBeTruthy();
    expect(screen.getByText("停灰度 -> 停影子模式 -> 禁用适配器")).toBeTruthy();
  });

  it("renders retrieval experiment controls on system page", async () => {
    render(
      <MemoryRouter>
        <SystemPage view="llm" />
      </MemoryRouter>,
    );

    expect(await screen.findByText("检索实验治理")).toBeTruthy();
    expect(screen.getByLabelText("影子模式开关").checked).toBe(true);
    expect(screen.getByLabelText("灰度范围").value).toBe("domain:payroll");
    expect(screen.getByLabelText("紧急停机开关").checked).toBe(false);
    expect(screen.getByText("场景命中阈值")).toBeTruthy();
  });
});
