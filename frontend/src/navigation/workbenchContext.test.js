import { describe, expect, it } from "vitest";
import {
  buildWorkbenchHref,
  parseWorkbenchContextFromSearch,
  validateWorkbenchContext,
} from "./workbenchContext";

describe("workbenchContext", () => {
  it("serializes context into a single ctx query payload and parses it back", () => {
    const href = buildWorkbenchHref("/runtime", {
      source_workbench: "map",
      target_workbench: "runtime",
      intent: "run_query",
      scene_code: "SCN_PAYROLL_DETAIL",
      asset_ref: "plan:payroll_detail",
      lock_mode: "latest",
    });

    expect(href).toMatch(/^\/runtime\?ctx=/);

    const parsed = parseWorkbenchContextFromSearch(href.split("?")[1]);
    expect(parsed).toMatchObject({
      source_workbench: "map",
      target_workbench: "runtime",
      intent: "run_query",
      scene_code: "SCN_PAYROLL_DETAIL",
      asset_ref: "plan:payroll_detail",
      lock_mode: "latest",
    });
  });

  it("returns null when no ctx payload exists", () => {
    expect(parseWorkbenchContextFromSearch("foo=bar")).toBeNull();
  });

  it("rejects replay packages when snapshot pair is incomplete", () => {
    const result = validateWorkbenchContext({
      source_workbench: "monitoring",
      target_workbench: "runtime",
      intent: "replay_trace",
      trace_id: "trace_runtime_20260327_07",
      snapshot_id: "snapshot-20260327-01",
      lock_mode: "replay",
    });

    expect(result.ok).toBe(false);
    expect(result.message).toContain("snapshot_id");
    expect(result.message).toContain("inference_snapshot_id");
  });

  it("rejects unknown workbench codes", () => {
    const result = validateWorkbenchContext({
      source_workbench: "unknown",
      target_workbench: "runtime",
      intent: "run_query",
      lock_mode: "latest",
    });

    expect(result.ok).toBe(false);
    expect(result.message).toContain("source_workbench");
  });
});
