import { describe, expect, it } from "vitest";
import {
  buildStep1Summary,
  buildStep2Summary,
  buildStep3Summary,
  resolveAccordionStepState,
  toConfidenceLevelZh,
} from "./knowledge-import-utils";

describe("knowledge import accordion utils", () => {
  it("resolves step states by active step", () => {
    expect(resolveAccordionStepState(1, 1)).toBe("expanded");
    expect(resolveAccordionStepState(1, 2)).toBe("collapsed");
    expect(resolveAccordionStepState(3, 2)).toBe("locked");
  });

  it("formats step summaries in expected business copy", () => {
    expect(buildStep1Summary("粘贴文本", "abcd")).toBe("✓ 01 导入口径草稿 | 来源：粘贴文本（约 4 字）");
    expect(buildStep2Summary(5, 0.85)).toBe("✓ 02 抽取质量判断 | 共识别 5 个场景，置信度 85%（高）");
    expect(buildStep3Summary(5)).toBe("✓ 03 结果与原文对照 | 已确认场景 5 个");
  });

  it("maps confidence into chinese levels", () => {
    expect(toConfidenceLevelZh(0.9)).toBe("高");
    expect(toConfidenceLevelZh(0.7)).toBe("中");
    expect(toConfidenceLevelZh(0.55)).toBe("低");
  });
});
