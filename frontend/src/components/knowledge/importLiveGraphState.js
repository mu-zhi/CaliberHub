const MAX_RECENT_ACTIVITY = 6;
const EMPTY_MESSAGE = "导入开始后显示候选实体图谱";

function normalizeExperimentSummary(summary) {
  if (!summary || typeof summary !== "object") {
    return null;
  }
  const adapterName = `${summary.adapterName || ""}`.trim();
  const adapterVersion = `${summary.adapterVersion || ""}`.trim();
  const referenceRefs = Array.isArray(summary.referenceRefs) ? summary.referenceRefs.filter(Boolean) : [];
  const warnings = Array.isArray(summary.warnings) ? summary.warnings.filter(Boolean) : [];
  const formalAssetWrites = Array.isArray(summary.formalAssetWrites) ? summary.formalAssetWrites.filter(Boolean) : [];
  if (!adapterName && !adapterVersion && referenceRefs.length === 0 && warnings.length === 0 && formalAssetWrites.length === 0) {
    return null;
  }
  return {
    adapterName,
    adapterVersion,
    referenceRefs,
    warnings,
    formalAssetWrites,
  };
}

function normalizeNode(node) {
  if (!node?.id) {
    return null;
  }
  return {
    id: `${node.id}`,
    nodeType: `${node.nodeType || "UNKNOWN"}`,
    label: `${node.label || node.id}`,
    status: `${node.status || "PENDING_CONFIRMATION"}`,
    confidenceScore: Number(node.confidenceScore || 0),
    evidenceRefs: Array.isArray(node.evidenceRefs) ? node.evidenceRefs : [],
  };
}

function normalizeEdge(edge) {
  if (!edge?.id || !edge?.sourceId || !edge?.targetId) {
    return null;
  }
  return {
    id: `${edge.id}`,
    sourceId: `${edge.sourceId}`,
    targetId: `${edge.targetId}`,
    relationType: `${edge.relationType || "RELATED_TO"}`,
    status: `${edge.status || "PENDING_CONFIRMATION"}`,
    confidenceScore: Number(edge.confidenceScore || 0),
    evidenceRefs: Array.isArray(edge.evidenceRefs) ? edge.evidenceRefs : [],
  };
}

function dedupeNodes(nodes = []) {
  const map = new Map();
  nodes.forEach((node) => {
    const normalized = normalizeNode(node);
    if (normalized) {
      map.set(normalized.id, normalized);
    }
  });
  return [...map.values()];
}

function dedupeEdges(edges = []) {
  const map = new Map();
  edges.forEach((edge) => {
    const normalized = normalizeEdge(edge);
    if (normalized) {
      map.set(normalized.id, normalized);
    }
  });
  return [...map.values()];
}

function buildRecentActivity(patch = {}, existingNodeIds = new Set(), existingEdgeIds = new Set()) {
  const activities = [];
  (patch.addedNodes || []).forEach((node) => {
    const normalized = normalizeNode(node);
    if (normalized && !existingNodeIds.has(normalized.id)) {
      activities.push({
        id: `node:${normalized.id}`,
        type: "node",
        label: normalized.label,
        meta: normalized.nodeType,
      });
    }
  });
  (patch.addedEdges || []).forEach((edge) => {
    const normalized = normalizeEdge(edge);
    if (normalized && !existingEdgeIds.has(normalized.id)) {
      activities.push({
        id: `edge:${normalized.id}`,
        type: "edge",
        label: normalized.relationType,
        meta: `${normalized.sourceId} -> ${normalized.targetId}`,
      });
    }
  });
  return activities;
}

function summarizeSnapshot(nodes, edges) {
  if (!nodes.length && !edges.length) {
    return EMPTY_MESSAGE;
  }
  return `已恢复候选实体图谱，共 ${nodes.length} 个节点 / ${edges.length} 条关系`;
}

export function createImportLiveGraphState() {
  return {
    nodes: [],
    edges: [],
    selectedNodeId: "",
    inspectorMode: "default",
    recentActivity: [],
    lastPatchSeq: 0,
    stageKey: "",
    stageName: "",
    summaryMessage: EMPTY_MESSAGE,
    experimentSummary: null,
  };
}

export function applyImportLiveGraphPatch(state, patch) {
  const current = state || createImportLiveGraphState();
  const existingNodeIds = new Set(current.nodes.map((node) => node.id));
  const existingEdgeIds = new Set(current.edges.map((edge) => edge.id));
  const nextNodes = dedupeNodes([
    ...current.nodes,
    ...(patch?.addedNodes || []),
    ...(patch?.updatedNodes || []),
  ]);
  const nextEdges = dedupeEdges([
    ...current.edges,
    ...(patch?.addedEdges || []),
    ...(patch?.updatedEdges || []),
  ]);
  const nextRecentActivity = [
    ...buildRecentActivity(patch, existingNodeIds, existingEdgeIds),
    ...current.recentActivity,
  ].filter((item, index, collection) => collection.findIndex((entry) => entry.id === item.id) === index).slice(0, MAX_RECENT_ACTIVITY);
  const hasSelectedNode = nextNodes.some((node) => node.id === current.selectedNodeId);
  const experimentSummary = normalizeExperimentSummary(patch?.experimentSummary || patch?.preprocessExperiment) || current.experimentSummary;
  return {
    ...current,
    nodes: nextNodes,
    edges: nextEdges,
    selectedNodeId: hasSelectedNode ? current.selectedNodeId : "",
    inspectorMode: hasSelectedNode ? "node" : "default",
    recentActivity: nextRecentActivity,
    lastPatchSeq: Math.max(current.lastPatchSeq, Number(patch?.patchSeq || 0)),
    stageKey: `${patch?.stageKey || current.stageKey || ""}`,
    stageName: `${patch?.stageName || current.stageName || ""}`,
    summaryMessage: `${patch?.message || `候选实体图谱已更新，共 ${nextNodes.length} 个节点 / ${nextEdges.length} 条关系`}`,
    experimentSummary,
  };
}

export function restoreImportLiveGraphSnapshot(state, snapshot, options = {}) {
  const current = state || createImportLiveGraphState();
  const nextNodes = dedupeNodes(snapshot?.nodes || []);
  const nextEdges = dedupeEdges(snapshot?.edges || []);
  const hasSelectedNode = nextNodes.some((node) => node.id === current.selectedNodeId);
  const experimentSummary = normalizeExperimentSummary(options.experimentSummary || snapshot?.preprocessExperiment) || current.experimentSummary;
  return {
    ...createImportLiveGraphState(),
    nodes: nextNodes,
    edges: nextEdges,
    selectedNodeId: hasSelectedNode ? current.selectedNodeId : "",
    inspectorMode: hasSelectedNode ? "node" : "default",
    recentActivity: [],
    lastPatchSeq: Math.max(current.lastPatchSeq, Number(options.patchSeq || 0)),
    stageKey: `${options.stageKey || current.stageKey || ""}`,
    stageName: `${options.stageName || current.stageName || ""}`,
    summaryMessage: `${options.message || summarizeSnapshot(nextNodes, nextEdges)}`,
    experimentSummary,
  };
}

export function selectImportLiveGraphNode(state, nodeId) {
  const current = state || createImportLiveGraphState();
  const normalizedNodeId = `${nodeId || ""}`.trim();
  const exists = current.nodes.some((node) => node.id === normalizedNodeId);
  return {
    ...current,
    selectedNodeId: exists ? normalizedNodeId : "",
    inspectorMode: exists ? "node" : "default",
  };
}

export function getSelectedImportLiveGraphNode(state) {
  const current = state || createImportLiveGraphState();
  return current.nodes.find((node) => node.id === current.selectedNodeId) || null;
}
