import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderToString } from "react-dom/server";
import { MemoryRouter } from "react-router-dom";
import { DataMapContainer } from "./DataMapContainer";

function renderPage() {
  return renderToString(
    <MemoryRouter>
      <DataMapContainer viewPreset="lineage" />
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

describe("DataMapContainer status presentation", () => {
  it("renders Chinese filter labels for scene statuses", () => {
    const html = renderPage();

    expect(html).toContain("草稿");
    expect(html).toContain("已复核");
    expect(html).toContain("已发布");
    expect(html).toContain("已退役");
    expect(html).not.toContain(">DRAFT<");
    expect(html).not.toContain(">REVIEWED<");
    expect(html).not.toContain(">PUBLISHED<");
    expect(html).not.toContain(">RETIRED<");
  });
});
