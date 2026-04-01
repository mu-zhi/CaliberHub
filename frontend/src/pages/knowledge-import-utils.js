export function resolveAccordionStepState(stepNo, activeStep) {
  const step = Number(stepNo || 0);
  const current = Number(activeStep ?? 0);
  if (current <= 0) {
    return step === 1 ? "collapsed" : "locked";
  }
  if (step === current) {
    return "expanded";
  }
  if (step < current) {
    return "collapsed";
  }
  return "locked";
}

export function toConfidenceLevelZh(confidenceScore) {
  const confidence = Number(confidenceScore || 0);
  if (confidence >= 0.85) {
    return "高";
  }
  if (confidence >= 0.7) {
    return "中";
  }
  return "低";
}

export function buildStep1Summary(sourceLabel, rawText) {
  const label = `${sourceLabel || "粘贴文本"}`.trim() || "粘贴文本";
  const charCount = `${rawText || ""}`.length;
  return `✓ 01 导入口径草稿 | 来源：${label}（约 ${charCount} 字）`;
}

export function buildStep2Summary(sceneCount, confidenceScore) {
  const count = Number(sceneCount || 0);
  const confidence = Number(confidenceScore || 0);
  const percent = confidence > 0 ? `${Math.round(confidence * 100)}%` : "0%";
  return `✓ 02 抽取质量判断 | 共识别 ${count} 个场景，置信度 ${percent}（${toConfidenceLevelZh(confidence)}）`;
}

export function buildStep3Summary(confirmedCount) {
  const count = Number(confirmedCount || 0);
  return `✓ 03 结果与原文对照 | 已确认场景 ${count} 个`;
}

export function buildSceneCandidateCode(taskId, sceneIndex) {
  const compactId = `${taskId || ""}`.replace(/-/g, "").slice(0, 8);
  return `SC-${compactId}-${String(Number(sceneIndex || 0) + 1).padStart(3, "0")}`;
}

function readArray(value) {
  return Array.isArray(value) ? value : [];
}

function readText(value) {
  return `${value || ""}`.trim();
}

function readJsonList(value) {
  return Array.isArray(value) ? value : [];
}

export function normalizeCandidateGraph(graph) {
  const nodes = readArray(graph?.nodes);
  const edges = readArray(graph?.edges);
  return {
    rootRef: readText(graph?.graphId || graph?.taskId || graph?.materialId),
    nodes: nodes.map((node) => ({
      id: readText(node?.nodeCode || node?.id),
      objectType: readText(node?.nodeType || node?.objectType).toUpperCase(),
      objectCode: readText(node?.nodeCode || node?.objectCode || node?.id),
      objectName: readText(node?.label || node?.objectName || node?.nodeCode || node?.id),
      status: readText(node?.reviewStatus || node?.status),
      summaryText: readText(node?.summaryText),
      meta: {
        riskLevel: readText(node?.riskLevel),
        sceneCandidateCode: readText(node?.sceneCandidateCode),
        confidenceScore: Number(node?.confidenceScore || 0),
      },
    })),
    edges: edges.map((edge) => ({
      id: readText(edge?.edgeCode || edge?.id),
      relationType: readText(edge?.edgeType || edge?.relationType).toUpperCase(),
      source: readText(edge?.sourceNodeCode || edge?.source),
      target: readText(edge?.targetNodeCode || edge?.target),
      label: readText(edge?.label || edge?.relationType || edge?.edgeType),
      confidence: Number(edge?.confidenceScore || 0),
      meta: {
        reviewStatus: readText(edge?.reviewStatus || edge?.status),
        sceneCandidateCode: readText(edge?.sceneCandidateCode),
        riskLevel: readText(edge?.riskLevel),
      },
    })),
  };
}

export function applyGraphPatch(graph, graphPatch) {
  const currentNodes = readArray(graph?.nodes);
  const currentEdges = readArray(graph?.edges);
  const addedNodes = readJsonList(graphPatch?.addedNodes);
  const updatedNodes = readJsonList(graphPatch?.updatedNodes);
  const addedEdges = readJsonList(graphPatch?.addedEdges);
  const updatedEdges = readJsonList(graphPatch?.updatedEdges);

  const mergedNodes = new Map();
  const mergedEdges = new Map();

  currentNodes.forEach((item) => {
    const nodeCode = readText(item?.nodeCode || item?.id);
    if (!nodeCode) {
      return;
    }
    mergedNodes.set(nodeCode, item);
  });

  addedNodes.forEach((item) => {
    const nodeCode = readText(item?.nodeCode || item?.id);
    if (!nodeCode) {
      return;
    }
    mergedNodes.set(nodeCode, item);
  });

  updatedNodes.forEach((item) => {
    const nodeCode = readText(item?.nodeCode || item?.id);
    if (!nodeCode) {
      return;
    }
    mergedNodes.set(nodeCode, {
      ...mergedNodes.get(nodeCode),
      ...item,
    });
  });

  currentEdges.forEach((item) => {
    const edgeCode = readText(item?.edgeCode || item?.id);
    if (!edgeCode) {
      return;
    }
    mergedEdges.set(edgeCode, item);
  });

  addedEdges.forEach((item) => {
    const edgeCode = readText(item?.edgeCode || item?.id);
    if (!edgeCode) {
      return;
    }
    mergedEdges.set(edgeCode, item);
  });

  updatedEdges.forEach((item) => {
    const edgeCode = readText(item?.edgeCode || item?.id);
    if (!edgeCode) {
      return;
    }
    mergedEdges.set(edgeCode, {
      ...mergedEdges.get(edgeCode),
      ...item,
    });
  });

  return {
    ...graph,
    nodes: Array.from(mergedNodes.values()),
    edges: Array.from(mergedEdges.values()),
    graphId: readText(graph?.graphId || graph?.taskId || graph?.materialId),
    graphVersion: Math.max(0, Number(graph?.graphVersion || 0)) + 1,
  };
}

export function summarizeCandidateGraph(graph) {
  const nodes = readArray(graph?.nodes);
  const edges = readArray(graph?.edges);
  return {
    pendingNodes: nodes.filter((item) => `${item?.reviewStatus || ""}`.toUpperCase() === "PENDING_CONFIRMATION").length,
    acceptedNodes: nodes.filter((item) => `${item?.reviewStatus || ""}`.toUpperCase() === "ACCEPTED").length,
    pendingEdges: edges.filter((item) => `${item?.reviewStatus || ""}`.toUpperCase() === "PENDING_CONFIRMATION").length,
  };
}

function mergeByKey(existingItems, incomingItems, readKey) {
  const merged = new Map();
  readArray(existingItems).forEach((item) => {
    const key = readKey(item);
    if (key) {
      merged.set(key, item);
    }
  });
  readArray(incomingItems).forEach((item) => {
    const key = readKey(item);
    if (!key) {
      return;
    }
    merged.set(key, {
      ...(merged.get(key) || {}),
      ...item,
    });
  });
  return Array.from(merged.values());
}

export function mergeCandidateGraphPatch(currentGraph, patch) {
  const current = currentGraph && typeof currentGraph === "object" ? currentGraph : {};
  const addedNodes = readArray(patch?.addedNodes);
  const updatedNodes = readArray(patch?.updatedNodes);
  const addedEdges = readArray(patch?.addedEdges);
  const updatedEdges = readArray(patch?.updatedEdges);

  return {
    ...current,
    graphId: readText(patch?.graphId || current.graphId),
    taskId: readText(patch?.taskId || current.taskId),
    materialId: readText(patch?.materialId || current.materialId),
    summary: readText(patch?.summary || current.summary),
    patchSeq: Number(patch?.patchSeq || current.patchSeq || 0),
    focusNodeIds: readArray(patch?.focusNodeIds).map((item) => readText(item)).filter(Boolean),
    nodes: mergeByKey(
      current.nodes,
      [...addedNodes, ...updatedNodes],
      (item) => readText(item?.nodeCode || item?.id),
    ),
    edges: mergeByKey(
      current.edges,
      [...addedEdges, ...updatedEdges],
      (item) => readText(item?.edgeCode || item?.id),
    ),
  };
}
