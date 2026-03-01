import React from "react";
import { describe, expect, it } from "vitest";
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

describe("KnowledgePage render smoke", () => {
  it("renders import preset without runtime error", () => {
    expect(() => renderKnowledgePage("import")).not.toThrow();
  });

  it("renders manual preset without runtime error", () => {
    expect(() => renderKnowledgePage("manual")).not.toThrow();
  });

  it("renders feedback preset without runtime error", () => {
    expect(() => renderKnowledgePage("feedback")).not.toThrow();
  });
});
