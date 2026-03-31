import { describe, expect, it } from "vitest";
import {
  resolveApprovalContextState,
  resolveDataMapContextState,
  resolveRuntimeContextState,
} from "./workbenchContextReceivers";

describe("workbenchContextReceivers", () => {
  it("marks replay runtime context as readonly and exposes a replay banner", () => {
    const state = resolveRuntimeContextState({
      source_workbench: "monitoring",
      target_workbench: "runtime",
      intent: "replay_trace",
      trace_id: "trace_runtime_20260327_07",
      snapshot_id: "snapshot-20260327-01",
      inference_snapshot_id: "inference-20260327-01",
      lock_mode: "replay",
    });

    expect(state.readOnly).toBe(true);
    expect(state.banner).toMatchObject({
      tone: "warn",
      title: "历史回放态",
    });
    expect(state.traceId).toBe("trace_runtime_20260327_07");
  });

  it("builds frozen approval context summary from runtime output", () => {
    const state = resolveApprovalContextState({
      source_workbench: "runtime",
      target_workbench: "approval",
      intent: "submit_approval",
      trace_id: "trace_runtime_20260327_07",
      scene_code: "SCN_PAYROLL_DETAIL",
      plan_code: "PLAN_PAYROLL_DETAIL",
      snapshot_id: "snapshot-20260327-01",
      inference_snapshot_id: "inference-20260327-01",
      requested_fields: ["协议号", "交易日期", "金额"],
      purpose: "工单核验",
      lock_mode: "frozen",
    });

    expect(state.readOnly).toBe(true);
    expect(state.banner).toMatchObject({
      tone: "bad",
      title: "版本冻结态",
    });
    expect(state.summary).toMatchObject({
      traceId: "trace_runtime_20260327_07",
      sceneCode: "SCN_PAYROLL_DETAIL",
      planCode: "PLAN_PAYROLL_DETAIL",
      purpose: "工单核验",
    });
    expect(state.summary.requestedFields).toEqual(["协议号", "交易日期", "金额"]);
  });

  it("treats replay map context as locked and pre-fills snapshot filters", () => {
    const state = resolveDataMapContextState({
      source_workbench: "monitoring",
      target_workbench: "map",
      intent: "view_node",
      asset_ref: "plan:payroll_detail",
      snapshot_id: "42",
      inference_snapshot_id: "108",
      lock_mode: "replay",
    });

    expect(state.readOnly).toBe(true);
    expect(state.snapshotId).toBe("42");
    expect(state.banner?.title).toBe("历史回放态");
    expect(state.focusAssetRef).toBe("plan:payroll_detail");
  });

  it("returns unlocked defaults when no context is provided", () => {
    expect(resolveRuntimeContextState(null)).toMatchObject({
      readOnly: false,
      traceId: "",
      requestedFields: [],
    });
  });
});
