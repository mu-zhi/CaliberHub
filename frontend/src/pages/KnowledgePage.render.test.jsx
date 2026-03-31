import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderToString } from "react-dom/server";
import { MemoryRouter } from "react-router-dom";
import { KnowledgePage } from "./KnowledgePage";

function renderKnowledgePage(preset) {
  return renderToString(
    <MemoryRouter>
      <KnowledgePage preset={preset} />
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

describe("KnowledgePage render smoke", () => {
  it("renders import preset without runtime error", () => {
    const html = renderKnowledgePage("import");
    expect(html).toContain("候选实体图谱");
    expect(html).toContain("正在等待首批实体");
  });

  it("renders manual preset without runtime error", () => {
    expect(() => renderKnowledgePage("manual")).not.toThrow();
  });

  it("renders feedback preset without runtime error", () => {
    expect(() => renderKnowledgePage("feedback")).not.toThrow();
  });
});
