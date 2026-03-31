import { describe, expect, it } from "vitest";
import {
  buildContextRoundKey,
  resolveAutoFocusDecision,
  resolveContextFallbackState,
  resolveDataMapEntryContext,
} from "./dataMapContextBootstrap";

describe("dataMapContextBootstrap", () => {
  it("switches to lineage mode and requests one-shot auto focus when ctx is present", () => {
    const result = resolveDataMapEntryContext({
      context: {
        assetRef: "plan:payroll_detail",
        snapshotId: "42",
        readOnly: true,
      },
      currentMode: "browse",
      hasUserTakenOver: false,
      hasAutoFocusedOnce: false,
    });

    expect(result).toMatchObject({
      nextMode: "lineage",
      shouldAutoFocus: true,
      contextAssetRef: "plan:payroll_detail",
      snapshotId: "42",
      readOnly: true,
    });
  });

  it("does not auto focus again after the same context round was already consumed", () => {
    const result = resolveAutoFocusDecision({
      contextAssetRef: "plan:payroll_detail",
      hasAutoFocusedOnce: true,
      hasUserTakenOver: false,
      graphNodeIds: ["plan:payroll_detail", "scene:12"],
    });

    expect(result).toMatchObject({
      shouldAutoFocus: false,
      reason: "context-already-consumed",
    });
  });

  it("builds a new context round when snapshotId changes", () => {
    const firstRoundKey = buildContextRoundKey({
      contextAssetRef: "plan:payroll_detail",
      snapshotId: "1001",
    });
    const secondRoundKey = buildContextRoundKey({
      contextAssetRef: "plan:payroll_detail",
      snapshotId: "1002",
    });
    const thirdRoundKey = buildContextRoundKey({
      contextAssetRef: "",
      snapshotId: "1002",
    });

    expect(firstRoundKey).toBe("plan:payroll_detail::1001");
    expect(secondRoundKey).toBe("plan:payroll_detail::1002");
    expect(firstRoundKey).not.toBe(secondRoundKey);
    expect(thirdRoundKey).toBe("");
  });

  it("returns a clear fallback state when ctx target node is missing from graph", () => {
    const result = resolveContextFallbackState({
      contextAssetRef: "plan:not-found",
      graphNodeIds: ["scene:12", "plan:payroll_detail"],
    });

    expect(result).toMatchObject({
      shouldFallback: true,
      tone: "warning",
    });
    expect(result.message).toContain("未命中");
  });

  it("yields to user action after manual view switch", () => {
    const result = resolveAutoFocusDecision({
      contextAssetRef: "plan:payroll_detail",
      hasAutoFocusedOnce: false,
      hasUserTakenOver: true,
      graphNodeIds: ["plan:payroll_detail"],
    });

    expect(result).toMatchObject({
      shouldAutoFocus: false,
      reason: "manual-override",
    });
  });
});
