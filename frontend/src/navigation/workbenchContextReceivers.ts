import type { LockMode, WorkbenchContext } from "./workbenchContext";

type BannerTone = "neutral" | "warn" | "bad";

type ContextBanner = {
  tone: BannerTone,
  title: string,
  message: string,
};

function buildLockBanner(lockMode?: LockMode): ContextBanner | null {
  if (lockMode === "replay") {
    return {
      tone: "warn",
      title: "历史回放态",
      message: "当前页面按历史快照回放，不会自动切换到最新版本。",
    };
  }
  if (lockMode === "frozen") {
    return {
      tone: "bad",
      title: "版本冻结态",
      message: "当前页面锁定到指定快照对，禁止自动刷新或版本漂移。",
    };
  }
  return null;
}

function normalizeStringList(value: unknown) {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.map((item) => `${item || ""}`.trim()).filter(Boolean);
}

export function resolveRuntimeContextState(context: WorkbenchContext | null) {
  return {
    readOnly: context?.lock_mode === "replay" || context?.lock_mode === "frozen",
    disableRefresh: context?.lock_mode === "replay" || context?.lock_mode === "frozen",
    disableSubmit: context?.lock_mode === "replay" || context?.lock_mode === "frozen",
    banner: buildLockBanner(context?.lock_mode),
    sceneCode: context?.scene_code || "",
    planCode: context?.plan_code || "",
    assetRef: context?.asset_ref || "",
    traceId: context?.trace_id || "",
    requestedFields: normalizeStringList(context?.requested_fields),
    purpose: context?.purpose || "",
  };
}

export function resolveApprovalContextState(context: WorkbenchContext | null) {
  return {
    readOnly: Boolean(context),
    banner: buildLockBanner(context?.lock_mode),
    summary: {
      traceId: context?.trace_id || "",
      sceneCode: context?.scene_code || "",
      planCode: context?.plan_code || "",
      snapshotId: context?.snapshot_id || "",
      inferenceSnapshotId: context?.inference_snapshot_id || "",
      requestedFields: normalizeStringList(context?.requested_fields),
      purpose: context?.purpose || "",
    },
  };
}

export function resolveDataMapContextState(context: WorkbenchContext | null) {
  return {
    readOnly: context?.lock_mode === "replay" || context?.lock_mode === "frozen",
    banner: buildLockBanner(context?.lock_mode),
    snapshotId: context?.snapshot_id || "",
    inferenceSnapshotId: context?.inference_snapshot_id || "",
    focusSceneCode: context?.scene_code || "",
    focusPlanCode: context?.plan_code || "",
    focusAssetRef: context?.asset_ref || "",
    coverageSegmentId: context?.coverage_segment_id || "",
    evidenceRefs: normalizeStringList(context?.evidence_refs),
  };
}
