function normalizeText(value) {
  return `${value || ""}`.trim();
}

export function resolveDataMapEntryContext({
  context,
  currentMode,
  hasUserTakenOver,
  hasAutoFocusedOnce,
}) {
  const contextAssetRef = normalizeText(context?.assetRef || context?.focusAssetRef);
  const snapshotId = normalizeText(context?.snapshotId);
  const readOnly = Boolean(context?.readOnly);

  if (!contextAssetRef) {
    return {
      nextMode: currentMode || "browse",
      shouldAutoFocus: false,
      contextAssetRef: "",
      snapshotId,
      readOnly,
    };
  }

  return {
    nextMode: hasUserTakenOver ? (currentMode || "browse") : "lineage",
    shouldAutoFocus: !hasUserTakenOver && !hasAutoFocusedOnce,
    contextAssetRef,
    snapshotId,
    readOnly,
  };
}

export function buildContextRoundKey({ contextAssetRef, snapshotId }) {
  const normalizedContextAssetRef = normalizeText(contextAssetRef);
  if (!normalizedContextAssetRef) {
    return "";
  }
  return `${normalizedContextAssetRef}::${normalizeText(snapshotId)}`;
}

export function resolveAutoFocusDecision({
  contextAssetRef,
  hasAutoFocusedOnce,
  hasUserTakenOver,
  graphNodeIds,
}) {
  const normalizedAssetRef = normalizeText(contextAssetRef);
  const availableNodeIds = Array.isArray(graphNodeIds)
    ? graphNodeIds.map((item) => normalizeText(item)).filter(Boolean)
    : [];

  if (!normalizedAssetRef) {
    return {
      shouldAutoFocus: false,
      reason: "no-context-asset",
      targetAssetRef: "",
    };
  }
  if (hasUserTakenOver) {
    return {
      shouldAutoFocus: false,
      reason: "manual-override",
      targetAssetRef: normalizedAssetRef,
    };
  }
  if (hasAutoFocusedOnce) {
    return {
      shouldAutoFocus: false,
      reason: "context-already-consumed",
      targetAssetRef: normalizedAssetRef,
    };
  }
  if (!availableNodeIds.includes(normalizedAssetRef)) {
    return {
      shouldAutoFocus: false,
      reason: "context-target-missing",
      targetAssetRef: normalizedAssetRef,
    };
  }
  return {
    shouldAutoFocus: true,
    reason: "context-target-found",
    targetAssetRef: normalizedAssetRef,
  };
}

export function resolveContextFallbackState({
  contextAssetRef,
  graphNodeIds,
}) {
  const normalizedAssetRef = normalizeText(contextAssetRef);
  const availableNodeIds = Array.isArray(graphNodeIds)
    ? graphNodeIds.map((item) => normalizeText(item)).filter(Boolean)
    : [];

  if (!normalizedAssetRef) {
    return {
      shouldFallback: false,
      tone: "neutral",
      message: "",
      reason: "no-context-asset",
    };
  }
  if (availableNodeIds.includes(normalizedAssetRef)) {
    return {
      shouldFallback: false,
      tone: "neutral",
      message: "",
      reason: "context-target-found",
    };
  }
  return {
    shouldFallback: true,
    tone: "warning",
    message: `上下文目标 ${normalizedAssetRef} 未命中当前图谱，已退化为提示态，请检查快照或过滤条件。`,
    reason: "context-target-missing",
  };
}
