import { apiRequest, parseJsonText } from "../api/client";
import { API_CONTRACTS, buildApiPath } from "../api/contracts";

/**
 * @typedef {import("../types/dataMap").MillerNode} MillerNode
 * @typedef {import("../types/dataMap").FetchColumnResponse} FetchColumnResponse
 * @typedef {import("../types/dataMap").LineageGraphData} LineageGraphData
 * @typedef {import("../types/dataMap").DataMapNodeDetail} DataMapNodeDetail
 * @typedef {import("../types/dataMap").DataMapImpactAnalysis} DataMapImpactAnalysis
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
  const result = await apiRequest(API_CONTRACTS.assetsColumns, { query });
  return normalizeColumnResponse(result, columnId);
}

/**
 * @param {number} sceneId
 * @param {{maxNodes?: number}} [options]
 * @returns {Promise<LineageGraphData>}
 */
export async function fetchLineageGraph(sceneId, options = {}) {
  const result = await fetchDataMapGraph("SCENE", sceneId, options);
  return normalizeLineageGraph(result);
}

/**
 * @param {string} rootType
 * @param {number} rootId
 * @param {{
 *   snapshotId?: number | string,
 *   objectTypes?: string[],
 *   statuses?: string[],
 *   relationTypes?: string[],
 *   sensitivityScopes?: string[]
 * }} [options]
 * @returns {Promise<LineageGraphData>}
 */
export async function fetchDataMapGraph(rootType, rootId, options = {}) {
  const query = {
    root_type: `${rootType || "SCENE"}`.trim().toUpperCase(),
    root_id: Number(rootId),
    snapshot_id: toOptionalNumber(options.snapshotId),
    object_types: joinQueryValues(options.objectTypes),
    statuses: joinQueryValues(options.statuses),
    relation_types: joinQueryValues(options.relationTypes),
    sensitivity_scopes: joinQueryValues(options.sensitivityScopes),
  };
  const result = await apiRequest(API_CONTRACTS.datamapGraph, { query });
  return normalizeLineageGraph(result);
}

/**
 * @param {string} assetRef
 * @returns {Promise<DataMapNodeDetail>}
 */
export async function fetchDataMapNodeDetail(assetRef) {
  const result = await apiRequest(buildApiPath("datamapNodeDetail", { id: assetRef }));
  return normalizeNodeDetail(result);
}

/**
 * @param {string} assetRef
 * @param {number | string | null} [snapshotId]
 * @returns {Promise<DataMapImpactAnalysis>}
 */
export async function fetchDataMapImpactAnalysis(assetRef, snapshotId = null) {
  const result = await apiRequest(API_CONTRACTS.datamapImpactAnalysis, {
    method: "POST",
    body: {
      assetRef,
      snapshotId: toOptionalNumber(snapshotId),
    },
  });
  return normalizeImpactAnalysis(result);
}

/**
 * @param {number} sceneId
 */
export async function fetchSceneDetail(sceneId) {
  return apiRequest(buildApiPath("sceneById", { id: sceneId }));
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
    rootNodeId: `${payload?.rootRef || payload?.rootNodeId || ""}`.trim() || undefined,
    sceneId: toOptionalNumber(payload?.sceneId),
    sceneName: `${payload?.sceneName || ""}`.trim() || undefined,
    nodes: nodes.map((item) => ({
      id: `${item?.id || ""}`.trim(),
      label: `${item?.label || ""}`.trim(),
      type: `${item?.objectType || item?.type || "SCENE"}`.trim().toUpperCase(),
      objectType: `${item?.objectType || item?.type || "SCENE"}`.trim().toUpperCase(),
      objectCode: `${item?.objectCode || ""}`.trim() || undefined,
      objectName: `${item?.objectName || item?.label || ""}`.trim() || undefined,
      status: `${item?.status || ""}`.trim() || undefined,
      snapshotId: toOptionalNumber(item?.snapshotId),
      domainCode: `${item?.domainCode || ""}`.trim() || undefined,
      owner: `${item?.owner || ""}`.trim() || undefined,
      sensitivityScope: `${item?.sensitivityScope || ""}`.trim() || undefined,
      timeSemantic: `${item?.timeSemantic || ""}`.trim() || undefined,
      evidenceCount: toOptionalNumber(item?.evidenceCount),
      lastReviewedAt: `${item?.lastReviewedAt || ""}`.trim() || undefined,
      summaryText: `${item?.summaryText || ""}`.trim() || undefined,
      meta: normalizeMeta(item?.meta),
    })).filter((item) => item.id),
    edges: edges.map((item) => ({
      id: `${item?.id || `${item?.source || ""}-${item?.target || ""}`}`.trim(),
      source: `${item?.source || ""}`.trim(),
      target: `${item?.target || ""}`.trim(),
      label: `${item?.label || item?.relationType || ""}`.trim() || undefined,
      relationType: `${item?.relationType || item?.label || ""}`.trim() || undefined,
      confidence: toOptionalNumber(item?.confidence),
      traceId: `${item?.traceId || ""}`.trim() || undefined,
      sourceRef: `${item?.sourceRef || ""}`.trim() || undefined,
      effectiveFrom: `${item?.effectiveFrom || ""}`.trim() || undefined,
      effectiveTo: `${item?.effectiveTo || ""}`.trim() || undefined,
      policyHit: Boolean(item?.policyHit),
      coverageExplanation: `${item?.coverageExplanation || ""}`.trim() || undefined,
      meta: normalizeMeta(item?.meta),
    })).filter((item) => item.source && item.target),
    truncated: Boolean(payload?.truncated),
    hiddenNodeCount: Number(payload?.hiddenNodeCount || 0) || 0,
  };
}

/**
 * @param {any} payload
 * @returns {DataMapNodeDetail}
 */
function normalizeNodeDetail(payload) {
  return {
    assetRef: `${payload?.assetRef || ""}`.trim(),
    node: normalizeLineageGraph({
      nodes: [payload?.node || {}],
      edges: [],
    }).nodes[0] || null,
    attributes: normalizeMeta(payload?.attributes),
  };
}

/**
 * @param {any} payload
 * @returns {DataMapImpactAnalysis}
 */
function normalizeImpactAnalysis(payload) {
  const affectedAssets = Array.isArray(payload?.affectedAssets) ? payload.affectedAssets : [];
  return {
    assetRef: `${payload?.assetRef || ""}`.trim(),
    riskLevel: `${payload?.riskLevel || "LOW"}`.trim().toUpperCase(),
    recommendedActions: Array.isArray(payload?.recommendedActions)
      ? payload.recommendedActions.map((item) => `${item || ""}`.trim()).filter(Boolean)
      : [],
    affectedAssets: affectedAssets.map((item) => ({
      assetRef: `${item?.assetRef || ""}`.trim(),
      objectType: `${item?.objectType || ""}`.trim().toUpperCase(),
      objectName: `${item?.objectName || ""}`.trim(),
      relationType: `${item?.relationType || ""}`.trim() || undefined,
      impactSummary: `${item?.impactSummary || ""}`.trim() || undefined,
    })).filter((item) => item.assetRef),
    graph: payload?.graph ? normalizeLineageGraph(payload.graph) : null,
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

function normalizeMeta(value) {
  if (!value) {
    return {};
  }
  if (typeof value === "string") {
    return parseJsonText(value, {});
  }
  if (typeof value === "object" && !Array.isArray(value)) {
    return value;
  }
  return {};
}

function joinQueryValues(values) {
  if (!Array.isArray(values) || values.length === 0) {
    return "";
  }
  return values.map((item) => `${item || ""}`.trim()).filter(Boolean).join(",");
}

function toOptionalNumber(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}
