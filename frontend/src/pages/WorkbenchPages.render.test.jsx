import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderToString } from "react-dom/server";
import { MemoryRouter } from "react-router-dom";
import { findRoute } from "../routes";
import { ApprovalExportPage } from "./ApprovalExportPage";
import { HomePage } from "./HomePage";
import { KnowledgePackageWorkbenchPage } from "./KnowledgePackageWorkbenchPage";
import { MonitoringAuditPage } from "./MonitoringAuditPage";
import { PublishCenterPage } from "./PublishCenterPage";

function renderPage(element) {
  return renderToString(
    <MemoryRouter>
      {element}
    </MemoryRouter>,
  );
}

let consoleErrorSpy;

beforeEach(() => {
  const originalError = console.error;
  consoleErrorSpy = vi.spyOn(console, "error").mockImplementation((...args) => {
    const [firstArg] = args;
    if (typeof firstArg === "string" && firstArg.includes("useLayoutEffect does nothing on the server")) {
      return;
    }
    originalError(...args);
  });
});

afterEach(() => {
  consoleErrorSpy?.mockRestore();
});

describe("formal workbench routes", () => {
  it("registers official overview and production routes", () => {
    expect(findRoute("/overview")?.label).toBe("首页总览");
    expect(findRoute("/production/ingest")?.label).toBe("材料接入与解析");
    expect(findRoute("/production/modeling")?.label).toBe("资产建模");
  });

  it("registers official runtime and governance routes", () => {
    expect(findRoute("/publish")?.label).toBe("发布中心");
    expect(findRoute("/runtime")?.label).toBe("运行决策台");
    expect(findRoute("/approval")?.label).toBe("审批与导出");
    expect(findRoute("/monitoring")?.label).toBe("监控与审计");
  });
});

describe("formal workbench pages render smoke", () => {
  it("renders overview page without runtime error", () => {
    expect(() => renderPage(<HomePage />)).not.toThrow();
  });

  it("renders approval page without runtime error", () => {
    expect(() => renderPage(<ApprovalExportPage />)).not.toThrow();
  });

  it("renders publish center without runtime error", () => {
    expect(() => renderPage(<PublishCenterPage />)).not.toThrow();
  });

  it("renders runtime page without runtime error", () => {
    expect(() => renderPage(<KnowledgePackageWorkbenchPage />)).not.toThrow();
  });

  it("renders monitoring page without runtime error", () => {
    expect(() => renderPage(<MonitoringAuditPage />)).not.toThrow();
  });
});

describe("home overview hero copy", () => {
  it("keeps only the lightweight overview marker before the status bar", () => {
    const html = renderPage(<HomePage />);

    expect(html).toContain("首页总览 / 指挥页");
    expect(html).toContain("当前全局状态");
    expect(html).toContain("稳定版本");
    expect(html).not.toContain("先看到当前版本、风险和待办，再进入图谱工作台");
    expect(html).not.toContain("首页不再以搜索为第一动作，而是把发布阻断、审批积压、运行健康和近期变更放到同一视野里。");
  });
});
