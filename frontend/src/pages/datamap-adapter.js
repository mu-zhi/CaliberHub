import { apiRequest, parseJsonText } from "../api/client";

const DEFAULT_MAX_NODES = 50;

/**
 * @typedef {import("../types/dataMap").MillerNode} MillerNode
 * @typedef {import("../types/dataMap").FetchColumnResponse} FetchColumnResponse
 * @typedef {import("../types/dataMap").LineageGraphData} LineageGraphData
 */

/**
 * @param {string} columnId
 * @param {{keyword?: string, view?: string}} [options]
 * @returns {Promise<FetchColumnResponse>}
 */
export async function fetchDataMapColumn(columnId, options = {}) {
  const query = {
    columnId,
    keyword: options.keyword || "",
    view: options.view || "",
  };
  const result = await apiRequest("/assets/columns", { query });
  return normalizeColumnResponse(result, columnId);
}

/**
 * @param {number} sceneId
 * @param {{maxNodes?: number}} [options]
 * @returns {Promise<LineageGraphData>}
 */
export async function fetchLineageGraph(sceneId, options = {}) {
  const maxNodes = Number(options.maxNodes || DEFAULT_MAX_NODES);
  const result = await apiRequest(`/assets/lineage/${encodeURIComponent(sceneId)}`, {
    query: {
      maxNodes: Number.isFinite(maxNodes) ? Math.round(maxNodes) : DEFAULT_MAX_NODES,
    },
  });
  return normalizeLineageGraph(result);
}

/**
 * @param {number} sceneId
 */
export async function fetchSceneDetail(sceneId) {
  return apiRequest(`/scenes/${encodeURIComponent(sceneId)}`);
}

/**
 * @param {MillerNode | null} node
 * @returns {number | null}
 */
export function extractSceneId(node) {
  if (!node) {
    return null;
  }
  const fromMeta = Number(node?.meta?.sceneId);
  if (Number.isFinite(fromMeta) && fromMeta > 0) {
    return fromMeta;
  }
  const text = `${node.id || ""}`;
  const matched = text.match(/scene:(\d+)$/i);
  if (!matched) {
    return null;
  }
  const parsed = Number(matched[1]);
  return Number.isFinite(parsed) ? parsed : null;
}

/**
 * @param {any} payload
 * @param {string} fallbackColumnId
 * @returns {FetchColumnResponse}
 */
function normalizeColumnResponse(payload, fallbackColumnId) {
  const items = Array.isArray(payload?.items) ? payload.items : [];
  return {
    columnId: `${payload?.columnId || fallbackColumnId}`,
    items: items.map(normalizeMillerNode),
  };
}

/**
 * @param {any} payload
 * @returns {LineageGraphData}
 */
function normalizeLineageGraph(payload) {
  const nodes = Array.isArray(payload?.nodes) ? payload.nodes : [];
  const edges = Array.isArray(payload?.edges) ? payload.edges : [];
  return {
    nodes: nodes.map((item) => ({
      id: `${item?.id || ""}`.trim(),
      label: `${item?.label || ""}`.trim(),
      type: `${item?.type || "APP"}`.trim().toUpperCase(),
      status: `${item?.status || ""}`.trim() || undefined,
    })).filter((item) => item.id),
    edges: edges.map((item) => ({
      source: `${item?.source || ""}`.trim(),
      target: `${item?.target || ""}`.trim(),
      label: `${item?.label || ""}`.trim() || undefined,
    })).filter((item) => item.source && item.target),
    truncated: Boolean(payload?.truncated),
    hiddenNodeCount: Number(payload?.hiddenNodeCount || 0) || 0,
  };
}

/**
 * @param {any} node
 * @returns {MillerNode}
 */
function normalizeMillerNode(node) {
  const parsedMeta = typeof node?.meta === "string" ? parseJsonText(node.meta, {}) : (node?.meta || {});
  return {
    id: `${node?.id || ""}`.trim(),
    parentId: node?.parentId == null ? null : `${node.parentId}`.trim(),
    label: `${node?.label || ""}`.trim(),
    type: `${node?.type || "TOPIC"}`.trim().toUpperCase(),
    hasChildren: Boolean(node?.hasChildren),
    status: `${node?.status || ""}`.trim() || undefined,
    meta: parsedMeta && typeof parsedMeta === "object" ? parsedMeta : {},
  };
}
