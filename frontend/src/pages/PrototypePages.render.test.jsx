import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderToString } from "react-dom/server";
import { MemoryRouter } from "react-router-dom";
import { findRoute } from "../routes";
import { PrototypeIndexPage } from "./PrototypeIndexPage";
import { PrototypeIngestGraphPage } from "./prototypes/PrototypeIngestGraphPage";
import { PrototypeModelingDualPanePage } from "./prototypes/PrototypeModelingDualPanePage";
import { PrototypeRuntimePublishPage } from "./prototypes/PrototypeRuntimePublishPage";

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

describe("prototype route registration", () => {
  it("registers prototype index route", () => {
    expect(findRoute("/prototype")?.label).toBe("原型入口");
  });

  it("registers prototype detail routes", () => {
    expect(findRoute("/prototype/ingest-graph")?.label).toContain("材料接入与图谱构建");
    expect(findRoute("/prototype/modeling-dual-pane")?.label).toContain("双栏校正与资产建模");
    expect(findRoute("/prototype/runtime-publish")?.label).toContain("运行验证与发布检查");
  });
});

describe("prototype pages render smoke", () => {
  it("renders prototype index without runtime error", () => {
    expect(() => renderPage(<PrototypeIndexPage />)).not.toThrow();
  });

  it("renders ingest graph prototype without runtime error", () => {
    expect(() => renderPage(<PrototypeIngestGraphPage />)).not.toThrow();
  });

  it("renders modeling prototype without runtime error", () => {
    expect(() => renderPage(<PrototypeModelingDualPanePage />)).not.toThrow();
  });

  it("renders runtime and publish prototype without runtime error", () => {
    expect(() => renderPage(<PrototypeRuntimePublishPage />)).not.toThrow();
  });

  it("renders modeling prototype without English pending status text", () => {
    const html = renderPage(<PrototypeModelingDualPanePage />);

    expect(html).not.toContain("PENDING");
  });

  it("renders runtime and publish prototype without English approval decision text", () => {
    const html = renderPage(<PrototypeRuntimePublishPage />);

    expect(html).not.toContain("need_approval");
  });
});
