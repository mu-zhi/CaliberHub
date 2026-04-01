// @vitest-environment jsdom
import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AccordionStepCard } from "./AccordionStepCard";

vi.mock("../hooks/useIsomorphicLayoutEffect", () => ({
  useIsomorphicLayoutEffect: React.useEffect,
}));

describe("AccordionStepCard", () => {
  it("disables pointer events for collapsed content so hidden sections do not block underlying actions", () => {
    const { container } = render(
      <AccordionStepCard
        stepNo={1}
        title="导入并生成草稿"
        state="collapsed"
        summaryText="摘要"
        showEdit={false}
      >
        <button type="button">隐藏内容按钮</button>
      </AccordionStepCard>,
    );

    expect(screen.queryByRole("button", { name: "隐藏内容按钮" })).toBeNull();

    const hiddenContent = container.querySelector(".accordion-step-content");

    expect(hiddenContent?.getAttribute("aria-hidden")).toBe("true");
    expect(hiddenContent?.style.pointerEvents).toBe("none");
  });

  it("can avoid mounting heavy collapsed content when keepContentMounted is false", () => {
    const { container } = render(
      <AccordionStepCard
        stepNo={1}
        title="导入并生成草稿"
        state="collapsed"
        summaryText="摘要"
        showEdit={false}
        keepContentMounted={false}
      >
        <button type="button">隐藏内容按钮</button>
      </AccordionStepCard>,
    );

    expect(screen.queryByRole("button", { name: "隐藏内容按钮" })).toBeNull();
    expect(container.querySelector(".accordion-step-content")).toBeNull();
  });
});
