const WORKBENCH_CODES = new Set([
  "overview",
  "map",
  "production",
  "publish",
  "runtime",
  "approval",
  "monitoring",
]);

const LOCK_MODES = new Set(["latest", "replay", "frozen"]);

export const WORKBENCH_CONTEXT_QUERY_KEY = "ctx";

export type WorkbenchCode =
  | "overview"
  | "map"
  | "production"
  | "publish"
  | "runtime"
  | "approval"
  | "monitoring";

export type LockMode = "latest" | "replay" | "frozen";

export type WorkbenchContext = {
  source_workbench: WorkbenchCode,
  target_workbench: WorkbenchCode,
  intent: string,
  scene_code?: string,
  plan_code?: string,
  asset_ref?: string,
  edge_id?: string,
  relation_type?: string,
  trace_id?: string,
  snapshot_id?: string,
  inference_snapshot_id?: string,
  path_id?: string,
  candidate_path_id?: string,
  coverage_segment_id?: string,
  evidence_refs?: string[],
  requested_fields?: string[],
  purpose?: string,
  lock_mode: LockMode,
};

export type ValidationResult =
  | { ok: true, context: WorkbenchContext | null, message: "" }
  | { ok: false, context: null, message: string };

function trimString(value: unknown) {
  return `${value || ""}`.trim();
}

function normalizeStringList(value: unknown) {
  if (!Array.isArray(value)) {
    return undefined;
  }
  const next = value.map((item) => trimString(item)).filter(Boolean);
  return next.length > 0 ? next : undefined;
}

function normalizeOptionalFields(input: Record<string, unknown>) {
  const next: Record<string, unknown> = {};
  [
    "scene_code",
    "plan_code",
    "asset_ref",
    "edge_id",
    "relation_type",
    "trace_id",
    "snapshot_id",
    "inference_snapshot_id",
    "path_id",
    "candidate_path_id",
    "coverage_segment_id",
    "purpose",
  ].forEach((key) => {
    const value = trimString(input[key]);
    if (value) {
      next[key] = value;
    }
  });

  const evidenceRefs = normalizeStringList(input.evidence_refs);
  const requestedFields = normalizeStringList(input.requested_fields);

  if (evidenceRefs) {
    next.evidence_refs = evidenceRefs;
  }
  if (requestedFields) {
    next.requested_fields = requestedFields;
  }

  return next;
}

function normalizeWorkbenchContext(input: unknown): Partial<WorkbenchContext> {
  const raw = input && typeof input === "object" ? input : {};
  const data = raw as Record<string, unknown>;
  const sourceWorkbench = trimString(data.source_workbench);
  const targetWorkbench = trimString(data.target_workbench);
  const intent = trimString(data.intent);
  const lockMode = trimString(data.lock_mode);

  return {
    source_workbench: sourceWorkbench as WorkbenchCode,
    target_workbench: targetWorkbench as WorkbenchCode,
    intent,
    lock_mode: lockMode as LockMode,
    ...normalizeOptionalFields(data),
  };
}

export function buildWorkbenchHref(targetPath: string, context: Partial<WorkbenchContext>) {
  const payload = encodeURIComponent(JSON.stringify(normalizeWorkbenchContext(context)));
  const separator = `${targetPath || ""}`.includes("?") ? "&" : "?";
  return `${targetPath}${separator}${WORKBENCH_CONTEXT_QUERY_KEY}=${payload}`;
}

export function parseWorkbenchContextFromSearch(search: string) {
  const params = new URLSearchParams(search.startsWith("?") ? search.slice(1) : search);
  const raw = params.get(WORKBENCH_CONTEXT_QUERY_KEY);
  if (!raw) {
    return null;
  }
  try {
    return normalizeWorkbenchContext(JSON.parse(raw));
  } catch (_) {
    return null;
  }
}

export function validateWorkbenchContext(input: unknown): ValidationResult {
  const context = normalizeWorkbenchContext(input);
  const errors: string[] = [];

  if (!WORKBENCH_CODES.has(context.source_workbench || "")) {
    errors.push("source_workbench 无效");
  }
  if (!WORKBENCH_CODES.has(context.target_workbench || "")) {
    errors.push("target_workbench 无效");
  }
  if (!context.intent) {
    errors.push("intent 不能为空");
  }
  if (!LOCK_MODES.has(context.lock_mode || "")) {
    errors.push("lock_mode 无效");
  }

  const hasSnapshotId = Boolean(context.snapshot_id);
  const hasInferenceSnapshotId = Boolean(context.inference_snapshot_id);
  if (hasSnapshotId !== hasInferenceSnapshotId) {
    errors.push("snapshot_id 与 inference_snapshot_id 必须成对出现");
  }
  if ((context.lock_mode === "replay" || context.lock_mode === "frozen") && (!hasSnapshotId || !hasInferenceSnapshotId)) {
    errors.push("replay/frozen 模式必须同时提供 snapshot_id 与 inference_snapshot_id");
  }

  if (errors.length > 0) {
    return {
      ok: false,
      context: null,
      message: errors.join("；"),
    };
  }

  return {
    ok: true,
    context: context as WorkbenchContext,
    message: "",
  };
}

export function readValidatedWorkbenchContext(search: string, targetWorkbench?: WorkbenchCode): ValidationResult {
  const parsed = parseWorkbenchContextFromSearch(search);
  if (!parsed) {
    return {
      ok: true,
      context: null,
      message: "",
    };
  }
  const validated = validateWorkbenchContext(parsed);
  if (!validated.ok) {
    return validated;
  }
  if (targetWorkbench && validated.context.target_workbench !== targetWorkbench) {
    return {
      ok: false,
      context: null,
      message: `target_workbench 必须为 ${targetWorkbench}`,
    };
  }
  return validated;
}
