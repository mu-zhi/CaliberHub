import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { Maximize2, Minimize2, RefreshCw } from "lucide-react";
import {
  fetchDataMapColumn,
  fetchDataMapGraph,
  fetchDataMapImpactAnalysis,
  fetchDataMapNodeDetail,
  fetchSceneDetail,
  extractSceneId,
} from "../../pages/datamap-adapter";
import { buildWorkbenchHref, readValidatedWorkbenchContext } from "../../navigation/workbenchContext";
import { resolveDataMapContextState } from "../../navigation/workbenchContextReceivers";
import {
  buildContextRoundKey,
  resolveAutoFocusDecision,
  resolveContextFallbackState,
  resolveDataMapEntryContext,
} from "./dataMapContextBootstrap";
import { LineageGraphView } from "./LineageGraphView";
import { MillerColumnsView } from "./MillerColumnsView";
import { PreviewPanel } from "./PreviewPanel";
import { UiButton, UiInlineError, UiInput, UiSegmentedControl } from "../ui";
import { DATA_MAP_STATUS_OPTIONS, describeSceneStatus } from "../ui/statusPresentation";

const ROOT_COLUMN_ID = "ROOT";
const LAYOUT_MODE_STORAGE_KEY = "dd.datamap.layout.mode.v1";
const MAX_EVENT_LOGS = 240;
const COMPACT_VIEWPORT_WIDTH = 1120;

const NODE_COLOR_MAP = {
  TOPIC: "#0f8d94",
  DOMAIN: "#0f4d78",
  SCENE: "#f59e0b",
  PLAN: "#3b82f6",
  OUTPUT_CONTRACT: "#16a34a",
  CONTRACT_VIEW: "#2aa29a",
  COVERAGE_DECLARATION: "#ef8d32",
  POLICY: "#7c3aed",
  EVIDENCE_FRAGMENT: "#b45309",
  PATH_TEMPLATE: "#155e75",
  SOURCE_CONTRACT: "#dc2626",
  SOURCE_INTAKE_CONTRACT: "#92400e",
  VERSION_SNAPSHOT: "#475569",
};

const FILTER_OPTIONS = {
  objectTypes: [
    "SCENE",
    "PLAN",
    "OUTPUT_CONTRACT",
    "CONTRACT_VIEW",
    "COVERAGE_DECLARATION",
    "POLICY",
    "EVIDENCE_FRAGMENT",
    "PATH_TEMPLATE",
    "SOURCE_CONTRACT",
    "SOURCE_INTAKE_CONTRACT",
    "VERSION_SNAPSHOT",
  ],
  statuses: DATA_MAP_STATUS_OPTIONS,
  sensitivityScopes: ["S0", "S1", "S2", "S3", "S4"],
};

const HIGHLIGHT_MODE_OPTIONS = [
  { value: "path", label: "路径高亮" },
  { value: "evidence", label: "证据追踪" },
  { value: "impact", label: "影响分析" },
];

function defaultModeByPreset(viewPreset) {
  return viewPreset === "lineage" ? "lineage" : "browse";
}

function readDefaultLayoutMode() {
  if (typeof window === "undefined") {
    return "split";
  }
  const value = `${window.localStorage.getItem(LAYOUT_MODE_STORAGE_KEY) || ""}`.trim();
  if (value === "graph" || value === "split" || value === "workbench") {
    return value;
  }
  return "split";
}

function viewLabel(viewPreset) {
  const mapping = {
    map: "数据图谱",
    scenes: "业务场景",
    views: "语义视图",
    dicts: "字典",
    rules: "派生规则",
    topics: "主题节点",
    services: "服务说明",
    guide: "取数指南",
    market: "场景集市",
    lineage: "资产图谱",
  };
  return mapping[viewPreset] || "数据地图";
}

function buildEmptyColumn(columnId, loading = false, error = "") {
  return {
    columnId,
    items: [],
    loading,
    error,
  };
}

function toNodeType(type) {
  return `${type || "SCENE"}`.trim().toUpperCase();
}

function toNodeTypeLabel(type) {
  const mapping = {
    TOPIC: "主题",
    DOMAIN: "业务领域",
    SCENE: "业务场景",
    PLAN: "执行方案",
    OUTPUT_CONTRACT: "输出契约",
    CONTRACT_VIEW: "契约视图",
    COVERAGE_DECLARATION: "覆盖声明",
    POLICY: "治理策略",
    EVIDENCE_FRAGMENT: "证据片段",
    PATH_TEMPLATE: "路径模板",
    SOURCE_CONTRACT: "来源契约",
    SOURCE_INTAKE_CONTRACT: "接入契约",
    VERSION_SNAPSHOT: "版本快照",
  };
  return mapping[type] || type;
}

function nodeTypeColor(type) {
  return NODE_COLOR_MAP[type] || "#456178";
}

function nowTimeText(value = new Date()) {
  return new Intl.DateTimeFormat("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(value);
}

function normalizeErrorMessage(error, fallback) {
  if (!error) {
    return fallback;
  }
  if (typeof error === "string") {
    return error;
  }
  if (error instanceof Error) {
    return error.message || fallback;
  }
  try {
    return JSON.stringify(error);
  } catch (_) {
    return fallback;
  }
}

function toTechnicalText(technical) {
  if (technical == null) {
    return "";
  }
  if (typeof technical === "string") {
    return technical;
  }
  try {
    return JSON.stringify(technical, null, 2);
  } catch (_) {
    return String(technical);
  }
}

function normalizeFilterState(filters) {
  return {
    objectTypes: Array.isArray(filters?.objectTypes) ? filters.objectTypes : [],
    statuses: Array.isArray(filters?.statuses) ? filters.statuses : [],
    relationTypes: Array.isArray(filters?.relationTypes) ? filters.relationTypes : [],
    sensitivityScopes: Array.isArray(filters?.sensitivityScopes) ? filters.sensitivityScopes : [],
    snapshotId: `${filters?.snapshotId || ""}`.trim(),
  };
}

function toggleValue(list, value) {
  const normalized = `${value || ""}`.trim().toUpperCase();
  if (!normalized) {
    return Array.isArray(list) ? list : [];
  }
  const current = Array.isArray(list) ? list : [];
  return current.includes(normalized)
    ? current.filter((item) => item !== normalized)
    : [...current, normalized];
}

function formatAttributeValue(value) {
  if (value == null || value === "") {
    return "—";
  }
  if (Array.isArray(value)) {
    return value.length > 0 ? value.join("，") : "—";
  }
  if (typeof value === "object") {
    try {
      return JSON.stringify(value);
    } catch (_) {
      return String(value);
    }
  }
  return `${value}`;
}

function buildAdjacency(edges = []) {
  const adjacency = new Map();
  edges.forEach((edge) => {
    adjacency.set(edge.source, [...(adjacency.get(edge.source) || []), edge]);
    adjacency.set(edge.target, [...(adjacency.get(edge.target) || []), edge]);
  });
  return adjacency;
}

function collectHighlightedGraph(lineageData, selectedNode, highlightMode, impactAnalysis) {
  if (!lineageData || !selectedNode || !selectedNode.id) {
    return { nodeIds: [], edgeIds: [] };
  }
  if (highlightMode === "impact" && impactAnalysis?.graph) {
    return {
      nodeIds: Array.isArray(impactAnalysis?.graph?.nodes) ? impactAnalysis.graph.nodes.map((item) => item.id) : [],
      edgeIds: Array.isArray(impactAnalysis?.graph?.edges) ? impactAnalysis.graph.edges.map((item) => item.id) : [],
    };
  }

  const allowedRelations = highlightMode === "evidence"
    ? new Set(["SUPPORTED_BY_EVIDENCE"])
    : new Set(["USES_PLAN", "REQUIRES_CONTRACT", "RESOLVES_TO_PATH", "MAPS_TO_TABLE", "DERIVED_FROM"]);
  const maxDepth = highlightMode === "evidence" ? 1 : 2;
  const adjacency = buildAdjacency(lineageData?.edges || []);
  const nodeIds = new Set([selectedNode.id]);
  const edgeIds = new Set();
  const queue = [{ id: selectedNode.id, depth: 0 }];

  while (queue.length > 0) {
    const current = queue.shift();
    if (!current || current.depth >= maxDepth) {
      continue;
    }
    (adjacency.get(current.id) || []).forEach((edge) => {
      const relationType = `${edge?.relationType || edge?.label || ""}`.trim().toUpperCase();
      if (!allowedRelations.has(relationType)) {
        return;
      }
      edgeIds.add(edge.id);
      const nextId = current.id === edge.source ? edge.target : edge.source;
      if (!nodeIds.has(nextId)) {
        nodeIds.add(nextId);
        queue.push({ id: nextId, depth: current.depth + 1 });
      }
    });
  }

  return {
    nodeIds: [...nodeIds],
    edgeIds: [...edgeIds],
  };
}

export function DataMapContainer({ viewPreset = "map" }) {
  const [debugLogSent, setDebugLogSent] = useState(false);
  try {
    return <DataMapContainerInner viewPreset={viewPreset} />;
  } catch (err) {
    if (!debugLogSent) {
      console.error("DIAGNOSTIC_UI_CRASH", err);
      setDebugLogSent(true);
    }
    return (
      <div className="empty-state-card">
        <h3>加载资产图谱时出错</h3>
        <p className="subtle-note">{err.message}</p>
        <button className="mini-link top-link" onClick={() => window.location.reload()}>刷新重试</button>
      </div>
    );
  }
}

function DataMapContainerInner({ viewPreset = "map" }) {
  const location = useLocation();
  const [mode, setMode] = useState(() => defaultModeByPreset(viewPreset));
  const [layoutMode, setLayoutMode] = useState(readDefaultLayoutMode);
  const [showEdgeLabels, setShowEdgeLabels] = useState(true);
  const [isCompactViewport, setIsCompactViewport] = useState(false);
  const [lastDataSyncAt, setLastDataSyncAt] = useState("");

  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");

  const [columns, setColumns] = useState([buildEmptyColumn(ROOT_COLUMN_ID, true)]);
  const [activePath, setActivePath] = useState([]);
  const [selectedSceneNode, setSelectedSceneNode] = useState(null);

  const [sceneDetail, setSceneDetail] = useState(null);
  const [sceneLoading, setSceneLoading] = useState(false);
  const [sceneError, setSceneError] = useState("");

  const [previewWidth, setPreviewWidth] = useState(460);

  const [lineageData, setLineageData] = useState(null);
  const [lineageLoading, setLineageLoading] = useState(false);
  const [lineageError, setLineageError] = useState("");
  const [selectedGraphNode, setSelectedGraphNode] = useState(null);
  const [selectedGraphEdge, setSelectedGraphEdge] = useState(null);
  const [nodeDetail, setNodeDetail] = useState(null);
  const [nodeDetailLoading, setNodeDetailLoading] = useState(false);
  const [nodeDetailError, setNodeDetailError] = useState("");
  const [impactAnalysis, setImpactAnalysis] = useState(null);
  const [impactLoading, setImpactLoading] = useState(false);
  const [impactError, setImpactError] = useState("");
  const [contextFallbackMessage, setContextFallbackMessage] = useState("");
  const [highlightMode, setHighlightMode] = useState("path");
  const [graphFilters, setGraphFilters] = useState(() => normalizeFilterState());

  const [manualRefreshing, setManualRefreshing] = useState(false);
  const [legendCollapsed, setLegendCollapsed] = useState(false);

  const [logsExpanded, setLogsExpanded] = useState(false);
  const [logView, setLogView] = useState("business");
  const [eventLogs, setEventLogs] = useState([]);

  const columnsTrackRef = useRef(null);
  const rootRequestRef = useRef(0);
  const childRequestRef = useRef(0);
  const initializedRef = useRef(false);
  const keywordInitRef = useRef(false);
  const logScrollRef = useRef(null);
  const autoFocusConsumedRef = useRef(false);
  const userTookOverContextRef = useRef(false);
  const lastContextRoundRef = useRef("");
  const contextValidation = useMemo(
    () => readValidatedWorkbenchContext(location.search, "map"),
    [location.search],
  );
  const mapContextState = useMemo(
    () => resolveDataMapContextState(contextValidation.ok ? contextValidation.context : null),
    [contextValidation],
  );
  const contextError = contextValidation.ok ? "" : contextValidation.message;
  const entryContext = useMemo(
    () => resolveDataMapEntryContext({
      context: {
        assetRef: mapContextState.focusAssetRef,
        snapshotId: mapContextState.snapshotId,
        readOnly: mapContextState.readOnly,
      },
      currentMode: mode,
      hasUserTakenOver: userTookOverContextRef.current,
      hasAutoFocusedOnce: autoFocusConsumedRef.current,
    }),
    [mapContextState.focusAssetRef, mapContextState.readOnly, mapContextState.snapshotId, mode],
  );
  const contextRoundKey = useMemo(
    () => buildContextRoundKey({
      contextAssetRef: mapContextState.focusAssetRef,
      snapshotId: mapContextState.snapshotId,
    }),
    [mapContextState.focusAssetRef, mapContextState.snapshotId],
  );



  const appendLog = useCallback((level, businessText, technical = null) => {
    const log = {
      id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
      level,
      business: businessText,
      technical: toTechnicalText(technical),
      at: nowTimeText(),
    };
    setEventLogs((prev) => {
      const next = [...prev, log];
      if (next.length > MAX_EVENT_LOGS) {
        return next.slice(next.length - MAX_EVENT_LOGS);
      }
      return next;
    });
  }, []);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }
    window.localStorage.setItem(LAYOUT_MODE_STORAGE_KEY, layoutMode);
  }, [layoutMode]);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }
    const handleResize = () => {
      setIsCompactViewport(window.innerWidth <= COMPACT_VIEWPORT_WIDTH);
    };
    handleResize();
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  useEffect(() => {
    if (!initializedRef.current) {
      initializedRef.current = true;
      appendLog("info", "数据地图工作台已就绪", { viewPreset });
      return;
    }
    appendLog("info", `已切换到“${viewLabel(viewPreset)}”入口`, { viewPreset });
  }, [appendLog, viewPreset]);

  useEffect(() => {
    setMode(defaultModeByPreset(viewPreset));
  }, [viewPreset]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedKeyword(keyword.trim());
    }, 260);
    return () => window.clearTimeout(timer);
  }, [keyword]);

  useEffect(() => {
    if (!keywordInitRef.current) {
      keywordInitRef.current = true;
      return;
    }
    appendLog(
      "info",
      debouncedKeyword ? `已应用关键词筛选：${debouncedKeyword}` : "已清空关键词筛选",
      { keyword: debouncedKeyword },
    );
  }, [appendLog, debouncedKeyword]);

  useEffect(() => {
    if (!logsExpanded) {
      return;
    }
    const ele = logScrollRef.current;
    if (!ele) {
      return;
    }
    ele.scrollTop = ele.scrollHeight;
  }, [eventLogs.length, logsExpanded, logView]);

  const selectedSceneId = useMemo(() => extractSceneId(selectedSceneNode), [selectedSceneNode]);
  useEffect(() => {
    if (lastContextRoundRef.current === contextRoundKey) {
      return;
    }
    lastContextRoundRef.current = contextRoundKey;
    autoFocusConsumedRef.current = false;
    userTookOverContextRef.current = false;
    setContextFallbackMessage("");
  }, [contextRoundKey]);

  useEffect(() => {
    if (entryContext.nextMode !== "lineage" || mode === "lineage") {
      return;
    }
    setMode("lineage");
    appendLog("info", "已根据上下文自动切换到资产图谱模式", {
      assetRef: entryContext.contextAssetRef,
      snapshotId: entryContext.snapshotId,
    });
  }, [appendLog, entryContext.contextAssetRef, entryContext.nextMode, entryContext.snapshotId, mode]);

  useEffect(() => {
    if (!mapContextState.snapshotId) {
      return;
    }
    setGraphFilters((prev) => (
      prev.snapshotId === mapContextState.snapshotId
        ? prev
        : { ...prev, snapshotId: mapContextState.snapshotId }
    ));
  }, [mapContextState.snapshotId]);
  const activeSnapshotId = useMemo(() => {
    const parsed = Number(graphFilters.snapshotId);
    return Number.isFinite(parsed) && parsed > 0 ? Math.round(parsed) : undefined;
  }, [graphFilters.snapshotId]);
  const runtimeSceneCode = useMemo(
    () => `${sceneDetail?.sceneCode || selectedSceneNode?.meta?.sceneCode || selectedSceneNode?.meta?.scene_code || ""}`.trim(),
    [sceneDetail?.sceneCode, selectedSceneNode?.meta?.sceneCode, selectedSceneNode?.meta?.scene_code],
  );
  const runtimePlanCode = useMemo(
    () => `${selectedGraphNode?.objectType === "PLAN" ? selectedGraphNode?.objectCode : ""}`.trim(),
    [selectedGraphNode?.objectCode, selectedGraphNode?.objectType],
  );
  const runtimeJumpHref = useMemo(
    () => buildWorkbenchHref("/runtime", {
      source_workbench: "map",
      target_workbench: "runtime",
      intent: "run_query",
      scene_code: runtimeSceneCode,
      plan_code: runtimePlanCode,
      asset_ref: selectedGraphNode?.id,
      edge_id: selectedGraphEdge?.id,
      lock_mode: "latest",
    }),
    [runtimePlanCode, runtimeSceneCode, selectedGraphEdge?.id, selectedGraphNode?.id],
  );
  const canJumpToRuntime = Boolean(runtimeSceneCode || runtimePlanCode);

  const reloadRoot = useCallback(async (reason = "auto") => {
    const requestId = rootRequestRef.current + 1;
    rootRequestRef.current = requestId;

    setColumns([buildEmptyColumn(ROOT_COLUMN_ID, true)]);
    setActivePath([]);
    setSelectedSceneNode(null);
    setSceneDetail(null);
    setSceneError("");
    setLineageData(null);
    setLineageError("");
    setSelectedGraphNode(null);
    setSelectedGraphEdge(null);
    setNodeDetail(null);
    setNodeDetailError("");
    setImpactAnalysis(null);
    setImpactError("");

    appendLog("info", reason === "manual" ? "开始刷新图谱根节点" : "开始加载图谱根节点", {
      reason,
      keyword: debouncedKeyword,
      viewPreset,
    });

    try {
      const response = await fetchDataMapColumn(ROOT_COLUMN_ID, {
        keyword: debouncedKeyword,
        view: viewPreset,
      });
      if (rootRequestRef.current !== requestId) {
        return { success: false, ignored: true };
      }
      setColumns([{
        columnId: response.columnId || ROOT_COLUMN_ID,
        items: response.items || [],
        loading: false,
        error: "",
      }]);
      setLastDataSyncAt(nowTimeText());
      appendLog("success", `图谱根节点加载完成（${response.items?.length || 0} 个）`, {
        reason,
        columnId: response.columnId,
      });
      return { success: true };
    } catch (error) {
      if (rootRequestRef.current !== requestId) {
        return { success: false, ignored: true };
      }
      const message = normalizeErrorMessage(error, "根节点加载失败");
      setColumns([buildEmptyColumn(ROOT_COLUMN_ID, false, message)]);
      appendLog("error", `图谱根节点加载失败：${message}`, {
        reason,
        error: message,
      });
      return { success: false, error: message };
    }
  }, [appendLog, debouncedKeyword, viewPreset]);

  useEffect(() => {
    reloadRoot("auto");
  }, [reloadRoot]);

  useEffect(() => {
    if (mode !== "browse") {
      return;
    }
    const track = columnsTrackRef.current;
    if (!track) {
      return;
    }
    if (typeof track.scrollTo === "function") {
      track.scrollTo({
        left: track.scrollWidth,
        behavior: "smooth",
      });
      return;
    }
    track.scrollLeft = track.scrollWidth;
  }, [columns.length, mode]);

  const reloadSceneDetail = useCallback(async (sceneId, reason = "auto") => {
    if (!sceneId) {
      return;
    }
    setSceneLoading(true);
    setSceneError("");
    appendLog("info", reason === "manual" ? "开始刷新场景详情" : "开始加载场景详情", {
      sceneId,
      reason,
    });
    try {
      const payload = await fetchSceneDetail(sceneId);
      setSceneDetail(payload);
      setLastDataSyncAt(nowTimeText());
      appendLog("success", "场景详情加载完成", { sceneId });
    } catch (error) {
      const message = normalizeErrorMessage(error, "场景详情加载失败");
      setSceneError(message);
      appendLog("error", `场景详情加载失败：${message}`, { sceneId, error: message });
    } finally {
      setSceneLoading(false);
    }
  }, [appendLog]);

  useEffect(() => {
    let cancelled = false;
    if (!selectedSceneId) {
      setSceneDetail(null);
      setSceneLoading(false);
      setSceneError("");
      setLineageData(null);
      setLineageError("");
      setSelectedGraphNode(null);
      setSelectedGraphEdge(null);
      setNodeDetail(null);
      setNodeDetailError("");
      setImpactAnalysis(null);
      setImpactError("");
      return () => {
        cancelled = true;
      };
    }

    setSceneLoading(true);
    setSceneError("");
    appendLog("info", "开始加载场景详情", { sceneId: selectedSceneId, reason: "auto" });

    fetchSceneDetail(selectedSceneId)
      .then((payload) => {
        if (cancelled) {
          return;
        }
        setSceneDetail(payload);
        setLastDataSyncAt(nowTimeText());
        appendLog("success", "场景详情加载完成", { sceneId: selectedSceneId });
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }
        const message = normalizeErrorMessage(error, "场景详情加载失败");
        setSceneError(message);
        appendLog("error", `场景详情加载失败：${message}`, { sceneId: selectedSceneId, error: message });
      })
      .finally(() => {
        if (!cancelled) {
          setSceneLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [appendLog, selectedSceneId]);

  const loadLineage = useCallback(async (reason = "auto") => {
    if (!selectedSceneId) {
      setLineageData(null);
      setLineageError("");
      if (reason !== "auto") {
        appendLog("warn", "请先选择业务场景再查看资产图谱", { reason });
      }
      return { success: false, empty: true };
    }

    setLineageLoading(true);
    setLineageError("");
    appendLog("info", reason === "manual" ? "开始刷新资产图谱" : "开始加载资产图谱", {
      sceneId: selectedSceneId,
      reason,
      filters: graphFilters,
    });

    try {
      const payload = await fetchDataMapGraph("SCENE", selectedSceneId, {
        snapshotId: activeSnapshotId,
        objectTypes: graphFilters.objectTypes,
        statuses: graphFilters.statuses,
        relationTypes: graphFilters.relationTypes,
        sensitivityScopes: graphFilters.sensitivityScopes,
      });
      setLineageData(payload);
      setSelectedGraphNode((prev) => {
        if (prev?.id) {
          const matched = payload.nodes?.find((item) => item.id === prev.id);
          if (matched) {
            return matched;
          }
        }
        return payload.nodes?.find((item) => item.id === `scene:${selectedSceneId}`) || payload.nodes?.[0] || null;
      });
      setSelectedGraphEdge((prev) => {
        if (!prev?.id) {
          return null;
        }
        return payload.edges?.find((item) => item.id === prev.id) || null;
      });
      setLastDataSyncAt(nowTimeText());
      appendLog("success", `资产图谱加载完成（${payload.nodes?.length || 0} 节点 / ${payload.edges?.length || 0} 关系）`, {
        sceneId: selectedSceneId,
        snapshotId: activeSnapshotId,
      });
      return { success: true };
    } catch (error) {
      const message = normalizeErrorMessage(error, "资产图谱加载失败");
      setLineageError(message);
      appendLog("error", `资产图谱加载失败：${message}`, { sceneId: selectedSceneId, error: message });
      return { success: false, error: message };
    } finally {
      setLineageLoading(false);
    }
  }, [activeSnapshotId, appendLog, graphFilters, selectedSceneId]);

  useEffect(() => {
    if (mode !== "lineage") {
      return;
    }
    loadLineage("auto");
  }, [mode, loadLineage]);

  useEffect(() => {
    if (mode !== "lineage" || !lineageData) {
      return;
    }

    const autoFocusDecision = resolveAutoFocusDecision({
      contextAssetRef: entryContext.contextAssetRef,
      hasAutoFocusedOnce: autoFocusConsumedRef.current,
      hasUserTakenOver: userTookOverContextRef.current,
      graphNodeIds: Array.isArray(lineageData?.nodes) ? lineageData.nodes.map((item) => item.id) : [],
    });

    if (autoFocusDecision.shouldAutoFocus) {
      const matchedNode = Array.isArray(lineageData?.nodes) ? lineageData.nodes.find((item) => item.id === autoFocusDecision.targetAssetRef) || null : null;
      if (matchedNode) {
        setSelectedGraphNode(matchedNode);
        setSelectedGraphEdge(null);
        autoFocusConsumedRef.current = true;
        setContextFallbackMessage("");
        appendLog("info", "已根据上下文自动定位图谱焦点节点", {
          assetRef: autoFocusDecision.targetAssetRef,
        });
        return;
      }
    }

    const fallbackState = resolveContextFallbackState({
      contextAssetRef: entryContext.contextAssetRef,
      graphNodeIds: Array.isArray(lineageData?.nodes) ? lineageData.nodes.map((item) => item.id) : [],
    });

    if (fallbackState.shouldFallback) {
      setContextFallbackMessage(fallbackState.message);
      if (!autoFocusConsumedRef.current) {
        autoFocusConsumedRef.current = true;
        appendLog("warn", fallbackState.message, {
          assetRef: entryContext.contextAssetRef,
          reason: fallbackState.reason,
        });
      }
      return;
    }

    setContextFallbackMessage("");
  }, [appendLog, entryContext.contextAssetRef, lineageData, mode]);

  useEffect(() => {
    let cancelled = false;
    if (mode !== "lineage" || !selectedGraphNode?.id) {
      setNodeDetail(null);
      setNodeDetailLoading(false);
      setNodeDetailError("");
      return () => {
        cancelled = true;
      };
    }

    setNodeDetailLoading(true);
    setNodeDetailError("");
    appendLog("info", "开始加载资产详情", { assetRef: selectedGraphNode.id });

    fetchDataMapNodeDetail(selectedGraphNode.id)
      .then((payload) => {
        if (cancelled) {
          return;
        }
        setNodeDetail(payload);
        appendLog("success", "资产详情加载完成", { assetRef: selectedGraphNode.id });
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }
        const message = normalizeErrorMessage(error, "资产详情加载失败");
        setNodeDetailError(message);
        appendLog("error", `资产详情加载失败：${message}`, { assetRef: selectedGraphNode.id, error: message });
      })
      .finally(() => {
        if (!cancelled) {
          setNodeDetailLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [appendLog, mode, selectedGraphNode?.id]);

  useEffect(() => {
    let cancelled = false;
    if (mode !== "lineage" || highlightMode !== "impact" || !selectedGraphNode?.id) {
      setImpactLoading(false);
      setImpactError("");
      if (highlightMode !== "impact") {
        setImpactAnalysis(null);
      }
      return () => {
        cancelled = true;
      };
    }

    setImpactLoading(true);
    setImpactError("");
    appendLog("info", "开始计算影响分析", { assetRef: selectedGraphNode.id, snapshotId: activeSnapshotId });

    fetchDataMapImpactAnalysis(selectedGraphNode.id, activeSnapshotId)
      .then((payload) => {
        if (cancelled) {
          return;
        }
        setImpactAnalysis(payload);
        appendLog("success", "影响分析已生成", {
          assetRef: selectedGraphNode.id,
          riskLevel: payload.riskLevel,
          affectedCount: payload.affectedAssets?.length || 0,
        });
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }
        const message = normalizeErrorMessage(error, "影响分析生成失败");
        setImpactError(message);
        appendLog("error", `影响分析生成失败：${message}`, { assetRef: selectedGraphNode.id, error: message });
      })
      .finally(() => {
        if (!cancelled) {
          setImpactLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [activeSnapshotId, appendLog, highlightMode, mode, selectedGraphNode?.id]);

  async function loadChildColumn(node, columnIndex) {
    const requestId = childRequestRef.current + 1;
    childRequestRef.current = requestId;

    appendLog("info", `加载下一级节点：${node.label || node.id}`, {
      columnIndex,
      nodeId: node.id,
    });

    setColumns((prev) => {
      const prefix = prev.slice(0, columnIndex + 1);
      return [...prefix, buildEmptyColumn(node.id, true)];
    });

    try {
      const response = await fetchDataMapColumn(node.id, {
        keyword: debouncedKeyword,
        view: viewPreset,
      });
      if (childRequestRef.current !== requestId) {
        return;
      }
      setColumns((prev) => {
        const prefix = prev.slice(0, columnIndex + 1);
        return [
          ...prefix,
          {
            columnId: response.columnId || node.id,
            items: response.items || [],
            loading: false,
            error: "",
          },
        ];
      });
      setLastDataSyncAt(nowTimeText());
      appendLog("success", `下一级节点加载完成（${response.items?.length || 0} 个）`, {
        parentNode: node.id,
        columnIndex,
      });
    } catch (error) {
      if (childRequestRef.current !== requestId) {
        return;
      }
      const message = normalizeErrorMessage(error, "列加载失败");
      setColumns((prev) => {
        const prefix = prev.slice(0, columnIndex + 1);
        return [...prefix, buildEmptyColumn(node.id, false, message)];
      });
      appendLog("error", `下一级节点加载失败：${message}`, {
        parentNode: node.id,
        columnIndex,
      });
    }
  }

  function handleSelectNode(node, columnIndex) {
    appendLog("info", `选中节点：${node.label}`, {
      nodeId: node.id,
      nodeType: node.type,
      columnIndex,
    });

    setActivePath((prev) => {
      const next = prev.slice(0, columnIndex);
      next.push(node.id);
      return next;
    });
    setColumns((prev) => prev.slice(0, columnIndex + 1));

    if (node.type === "SCENE") {
      setSelectedSceneNode(node);
      setSelectedGraphEdge(null);
    } else {
      setSelectedSceneNode(null);
      setSceneDetail(null);
      setLineageData(null);
      setSelectedGraphNode(null);
      setSelectedGraphEdge(null);
      setNodeDetail(null);
      setImpactAnalysis(null);
    }

    if (node.hasChildren) {
      loadChildColumn(node, columnIndex);
    }
  }

  function handleRetryColumn(columnId, columnIndex) {
    appendLog("warn", `重试节点列加载（第 ${columnIndex + 1} 列）`, {
      columnId,
      columnIndex,
    });

    if (columnIndex === 0 || columnId === ROOT_COLUMN_ID) {
      reloadRoot("manual");
      return;
    }
    const parentNodeId = activePath[columnIndex - 1];
    if (!parentNodeId) {
      reloadRoot("manual");
      return;
    }
    loadChildColumn({ id: parentNodeId, label: parentNodeId }, columnIndex - 1);
  }

  function handleMoveLeft(columnIndex) {
    if (columnIndex <= 0) {
      return;
    }
    appendLog("info", `回退到第 ${columnIndex} 列`, { columnIndex });
    setColumns((prev) => prev.slice(0, columnIndex));
    setActivePath((prev) => prev.slice(0, columnIndex));
    setSelectedSceneNode(null);
    setSceneDetail(null);
    setLineageData(null);
    setSelectedGraphNode(null);
    setSelectedGraphEdge(null);
    setNodeDetail(null);
    setImpactAnalysis(null);
    if (mode === "lineage") {
      setMode("browse");
    }
  }

  const previewPanel = mode === "browse" && selectedSceneNode ? (
    <PreviewPanel
      sceneNode={selectedSceneNode}
      sceneDetail={sceneDetail}
      loading={sceneLoading}
      error={sceneError}
      width={previewWidth}
      onWidthChange={setPreviewWidth}
      onRetry={() => reloadSceneDetail(selectedSceneId, "manual")}
      onSwitchToGraph={() => {
        setMode("lineage");
        appendLog("info", "从场景预览切换到资产图谱模式", { sceneId: selectedSceneId });
      }}
    />
  ) : null;

  const graphMetrics = useMemo(() => {
    if (mode === "lineage") {
      const nodes = Array.isArray(lineageData?.nodes) ? lineageData.nodes : [];
      const edges = Array.isArray(lineageData?.edges) ? lineageData.edges : [];
      const typeCounter = new Map();
      nodes.forEach((node) => {
        const type = toNodeType(node?.type);
        typeCounter.set(type, (typeCounter.get(type) || 0) + 1);
      });
      const typeStats = [...typeCounter.entries()].map(([type, count]) => ({
        type,
        label: toNodeTypeLabel(type),
        count,
        color: nodeTypeColor(type),
      }));
      return {
        nodeCount: nodes.length,
        edgeCount: edges.length,
        typeCount: typeStats.length,
        typeStats,
      };
    }

    const nodeMap = new Map();
    columns.forEach((column) => {
      (column.items || []).forEach((item) => {
        if (item?.id && !nodeMap.has(item.id)) {
          nodeMap.set(item.id, item);
        }
      });
    });

    const typeCounter = new Map();
    [...nodeMap.values()].forEach((node) => {
      const type = toNodeType(node?.type || "TOPIC");
      typeCounter.set(type, (typeCounter.get(type) || 0) + 1);
    });

    const typeStats = [...typeCounter.entries()].map(([type, count]) => ({
      type,
      label: toNodeTypeLabel(type),
      count,
      color: nodeTypeColor(type),
    }));

    return {
      nodeCount: nodeMap.size,
      edgeCount: 0,
      typeCount: typeStats.length,
      typeStats,
    };
  }, [columns, lineageData, mode]);

  const relationTags = useMemo(() => {
    if (mode === "lineage") {
      const labels = new Set();
      (lineageData?.edges || []).forEach((edge) => {
        const label = `${edge?.relationType || edge?.label || "关联"}`.trim();
        if (label) {
          labels.add(label);
        }
      });
      return [...labels].slice(0, 8);
    }

    const tags = [];
    if (debouncedKeyword) {
      tags.push(`关键词：${debouncedKeyword}`);
    }
    if (activePath.length > 0) {
      tags.push(`路径深度：${activePath.length}`);
    }
    if (selectedSceneNode?.label) {
      tags.push(`当前场景：${selectedSceneNode.label}`);
    }
    return tags;
  }, [activePath.length, debouncedKeyword, lineageData?.edges, mode, selectedSceneNode?.label]);

  const relationFilterOptions = useMemo(() => {
    const relationTypes = new Set(graphFilters.relationTypes);
    (lineageData?.edges || []).forEach((edge) => {
      const relationType = `${edge?.relationType || edge?.label || ""}`.trim().toUpperCase();
      if (relationType) {
        relationTypes.add(relationType);
      }
    });
    return [...relationTypes];
  }, [graphFilters.relationTypes, lineageData?.edges]);

  const activeFilterCount = useMemo(() => (
    graphFilters.objectTypes.length
    + graphFilters.statuses.length
    + graphFilters.relationTypes.length
    + graphFilters.sensitivityScopes.length
    + (graphFilters.snapshotId ? 1 : 0)
  ), [graphFilters]);

  const highlightedGraph = useMemo(
    () => {
      try {
        if (!lineageData || !selectedGraphNode) {
          return { nodeIds: [], edgeIds: [] };
        }
        return collectHighlightedGraph(lineageData, selectedGraphNode, highlightMode, impactAnalysis);
      } catch (err) {
        console.error("DEBUG_MAP_CRASH_PREVENTION", err);
        return { nodeIds: [], edgeIds: [] };
      }
    },
    [highlightMode, impactAnalysis, lineageData, selectedGraphNode],
  );

  const detailEntries = useMemo(() => {
    const entries = Object.entries(nodeDetail?.attributes || {});
    return entries.slice(0, 14);
  }, [nodeDetail?.attributes]);

  const isGraphBusy = manualRefreshing
    || lineageLoading
    || sceneLoading
    || nodeDetailLoading
    || impactLoading
    || columns.some((column) => column.loading);

  const processSteps = useMemo(() => {
    const hasScene = Boolean(selectedSceneNode);
    const hasFilter = Boolean(debouncedKeyword) || activePath.length > 0 || activeFilterCount > 0;
    const hasLineageNodes = mode === "lineage" && (lineageData?.nodes?.length || 0) > 0;
    const hasLineageEdges = mode === "lineage" && (lineageData?.edges?.length || 0) > 0;
    const hasActionBasis = mode === "lineage" ? Boolean(selectedGraphNode) : Boolean(sceneDetail);
    const hasImpact = highlightMode !== "impact" || Boolean(impactAnalysis);

    return [
      {
        key: "locate",
        no: 1,
        title: "场景定位",
        description: "定位当前业务场景与目标节点。",
        summary: hasScene ? `已定位：${selectedSceneNode?.label || "业务场景"}` : "等待选择业务场景",
        tags: hasScene ? [selectedSceneNode?.label || "业务场景"] : ["未选择场景"],
        done: hasScene,
      },
      {
        key: "filter",
        no: 2,
        title: "路径筛选",
        description: "通过关键词、状态、版本和关系过滤收敛分析范围。",
        summary: hasFilter ? `已收敛（路径 ${activePath.length} / 过滤 ${activeFilterCount}）` : "建议先输入关键词或配置过滤条件",
        tags: [debouncedKeyword ? `关键词：${debouncedKeyword}` : "无关键词", `路径：${activePath.length}`, `过滤：${activeFilterCount}`],
        done: hasFilter,
      },
      {
        key: "verify",
        no: 3,
        title: "关系核验",
        description: "进入资产图谱核验关键关系、路径模板与契约连线。",
        summary: hasLineageNodes ? `已核验节点 ${lineageData?.nodes?.length || 0}` : "切换到资产图谱模式后开始核验",
        tags: [mode === "lineage" ? "资产图谱模式" : "浏览模式"],
        done: hasLineageNodes,
      },
      {
        key: "impact",
        no: 4,
        title: "影响判断",
        description: "基于关系数量、证据与策略命中评估影响范围。",
        summary: hasLineageEdges ? `已识别关系 ${lineageData?.edges?.length || 0}` : "等待关系链路数据",
        tags: [hasImpact ? "影响可解释" : "影响待计算"],
        done: hasLineageEdges && hasImpact,
      },
      {
        key: "action",
        no: 5,
        title: "行动建议",
        description: "依据资产详情、影响分析和快照状态形成下一步动作。",
        summary: hasActionBasis ? "可进入发布、复核或回写动作" : "先加载资产详情后再给建议",
        tags: [hasActionBasis ? "详情可用" : "详情加载中", highlightMode === "impact" ? "影响模式" : "常规模式"],
        done: hasActionBasis,
      },
    ];
  }, [
    activeFilterCount,
    activePath.length,
    debouncedKeyword,
    highlightMode,
    impactAnalysis,
    lineageData?.edges?.length,
    lineageData?.nodes?.length,
    mode,
    sceneDetail,
    selectedGraphNode,
    selectedSceneNode,
  ]);

  const completedStepCount = useMemo(
    () => processSteps.filter((item) => item.done).length,
    [processSteps],
  );

  const activeStepNo = useMemo(() => {
    const pending = processSteps.find((item) => !item.done);
    return pending ? pending.no : processSteps[processSteps.length - 1]?.no || 1;
  }, [processSteps]);

  const workbenchProgress = Math.round((completedStepCount / Math.max(1, processSteps.length)) * 100);

  const workbenchStatus = useMemo(() => {
    if (completedStepCount >= processSteps.length) {
      return { text: "已完成", tone: "done" };
    }
    if (isGraphBusy) {
      return { text: "处理中", tone: "active" };
    }
    return { text: "待继续", tone: "pending" };
  }, [completedStepCount, isGraphBusy, processSteps.length]);

  const handleRefreshGraph = useCallback(async () => {
    setManualRefreshing(true);
    appendLog("info", "手动触发图谱刷新", {
      mode,
      selectedSceneId,
    });
    try {
      if (mode === "lineage") {
        await loadLineage("manual");
      } else {
        await reloadRoot("manual");
      }
    } finally {
      setManualRefreshing(false);
    }
  }, [appendLog, loadLineage, mode, reloadRoot, selectedSceneId]);

  const handleLayoutModeChange = useCallback((nextMode) => {
    setLayoutMode(nextMode);
    appendLog("info", `布局切换为：${nextMode === "graph" ? "图谱" : nextMode === "workbench" ? "工作台" : "双栏"}`, {
      layoutMode: nextMode,
    });
  }, [appendLog]);

  const handleToggleMaximize = useCallback(() => {
    const nextMode = layoutMode === "graph" ? "split" : "graph";
    handleLayoutModeChange(nextMode);
  }, [handleLayoutModeChange, layoutMode]);

  const handleModeChange = useCallback((nextMode) => {
    userTookOverContextRef.current = true;
    setMode(nextMode);
    appendLog("info", `分析模式切换为：${nextMode === "lineage" ? "资产图谱模式" : "浏览模式"}`, {
      mode: nextMode,
    });
  }, [appendLog]);

  const effectiveLayoutMode = useMemo(() => {
    if (isCompactViewport && layoutMode === "split") {
      return "workbench";
    }
    return layoutMode;
  }, [isCompactViewport, layoutMode]);

  const refreshCaliber = mode === "lineage"
    ? "基于已选场景、快照和治理过滤器刷新资产关系"
    : "基于关键词与路径刷新节点视图";
  const dataSourceText = `${viewLabel(viewPreset)} · ${mode === "lineage" ? "资产图谱数据" : "目录节点数据"}`;

  return (
    <article className="panel datamap-container" aria-label="双模态数据地图容器">
      {mapContextState.banner ? (
        <div className={`workbench-route-notice ${mapContextState.banner.tone}`} role="status">
          <strong>{mapContextState.banner.title}</strong>
          <span>{mapContextState.banner.message}</span>
        </div>
      ) : null}
      {contextError ? <UiInlineError message={contextError} /> : null}
      {contextFallbackMessage ? <UiInlineError message={contextFallbackMessage} /> : null}
      <header className="datamap-palette-toolbar">
        <div className="datamap-toolbar-pill">
          <div className="datamap-toolbar-search">
            <label htmlFor="datamapKeyword" className="visually-hidden">搜索节点</label>
            <UiInput
              id="datamapKeyword"
              name="datamapKeyword"
              autoComplete="off"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="搜索业务领域、场景、来源表"
            />
          </div>
          <UiSegmentedControl
            className="datamap-segment"
            ariaLabel="数据地图分析模式切换"
            value={mode}
            onChange={handleModeChange}
            options={[
              { value: "browse", label: "浏览模式" },
              { value: "lineage", label: "资产图谱" },
            ]}
          />
          <UiSegmentedControl
            className="datamap-layout-segment"
            ariaLabel="图谱布局切换"
            value={layoutMode}
            onChange={handleLayoutModeChange}
            options={[
              { value: "graph", label: "图谱" },
              { value: "split", label: "双栏" },
              { value: "workbench", label: "工作台" },
            ]}
          />
        </div>
        <p className="datamap-toolbar-caption">
          当前入口：{viewLabel(viewPreset)} · Step {activeStepNo}/5 · 进度 {workbenchProgress}%
          {isCompactViewport && layoutMode === "split" ? " · 窄屏优先展示工作台" : ""}
        </p>
      </header>

      <section className={`datamap-workspace layout-${effectiveLayoutMode}`}>
        <section className="datamap-graph-stage" aria-label="图谱画布区">
          <div className="datamap-graph-actions" role="toolbar" aria-label="图谱操作区">
            <button
              className="datamap-tool-btn"
              type="button"
              onClick={handleRefreshGraph}
              disabled={manualRefreshing || mapContextState.readOnly}
            >
              <RefreshCw size={14} className={manualRefreshing ? "is-spinning" : ""} />
              <span>{manualRefreshing ? "刷新中" : "刷新"}</span>
            </button>
            <button className="datamap-tool-btn" type="button" onClick={handleToggleMaximize}>
              {layoutMode === "graph" ? <Minimize2 size={14} /> : <Maximize2 size={14} />}
              <span>{layoutMode === "graph" ? "还原" : "最大化"}</span>
            </button>
            <label className={`datamap-toggle-chip ${mode !== "lineage" ? "is-disabled" : ""}`}>
              <input
                type="checkbox"
                checked={showEdgeLabels}
                disabled={mode !== "lineage"}
                onChange={(event) => {
                  setShowEdgeLabels(event.target.checked);
                  appendLog("info", `${event.target.checked ? "开启" : "关闭"}关系标签显示`, {
                    enabled: event.target.checked,
                  });
                }}
              />
              <span>显示关系标签</span>
            </label>
          </div>

          <div className="datamap-graph-canvas-wrap">
            <div className="datamap-graph-canvas">
              {mode === "browse" ? (
                <MillerColumnsView
                  columns={columns}
                  activePath={activePath}
                  trackRef={columnsTrackRef}
                  onSelectNode={handleSelectNode}
                  onRetryColumn={handleRetryColumn}
                  onMoveLeft={handleMoveLeft}
                  previewPanel={previewPanel}
                />
              ) : (
                <LineageGraphView
                  sceneNode={selectedSceneNode}
                  lineageData={lineageData}
                  loading={lineageLoading}
                  error={lineageError}
                  showEdgeLabels={showEdgeLabels}
                  selectedNodeId={selectedGraphNode?.id || ""}
                  selectedEdgeId={selectedGraphEdge?.id || ""}
                  highlightedNodeIds={highlightedGraph.nodeIds}
                  highlightedEdgeIds={highlightedGraph.edgeIds}
                  onNodeSelect={(node) => {
                    userTookOverContextRef.current = true;
                    setSelectedGraphNode(node);
                    if (node) {
                      setSelectedGraphEdge(null);
                    }
                  }}
                  onEdgeSelect={(edge) => {
                    userTookOverContextRef.current = true;
                    setSelectedGraphEdge(edge);
                  }}
                  onRetry={() => loadLineage("manual")}
                  onBackToBrowse={() => handleModeChange("browse")}
                />
              )}
            </div>

            {isGraphBusy ? (
              <div className="datamap-realtime-hint" role="status" aria-live="polite">
                <span className="dot" />
                实时更新中…
              </div>
            ) : null}

            {graphMetrics.typeStats.length > 0 ? (
              <aside className={`datamap-graph-legend ${legendCollapsed ? "is-collapsed" : ""}`} aria-label="实体类型图例">
                <button
                  type="button"
                  className="datamap-legend-toggle"
                  onClick={() => setLegendCollapsed((prev) => !prev)}
                >
                  {legendCollapsed ? "展开图例" : "收起图例"}
                </button>
                {!legendCollapsed ? (
                  <div className="datamap-legend-list">
                    {graphMetrics.typeStats.map((item) => (
                      <span key={`legend-${item.type}`} className="datamap-legend-item">
                        <i style={{ background: item.color }} aria-hidden="true" />
                        <em>{item.label}</em>
                        <b>{item.count}</b>
                      </span>
                    ))}
                  </div>
                ) : null}
              </aside>
            ) : null}
          </div>
        </section>

        <aside className="datamap-workbench-stage" aria-label="数据地图工作台">
          <header className="datamap-workbench-head">
            <div>
              <p className="datamap-workbench-step">Step {activeStepNo}/5 数据地图流程</p>
              <h3>图谱工作台</h3>
              <p>围绕场景定位、关系核验和影响判断的可观测分析面板。</p>
            </div>
            <div className="datamap-workbench-head-side">
              <span className="datamap-progress-pill">{workbenchProgress}%</span>
              <span className={`datamap-status-pill tone-${workbenchStatus.tone}`}>{workbenchStatus.text}</span>
            </div>
          </header>

          <section className="datamap-source-panel" aria-label="数据来源与刷新口径">
            <article>
              <h4>数据来源</h4>
              <p>{dataSourceText}</p>
            </article>
            <article>
              <h4>刷新口径</h4>
              <p>{refreshCaliber}</p>
            </article>
            <article>
              <h4>最近更新时间</h4>
              <p>{lastDataSyncAt || "--:--:--"}</p>
            </article>
            <div className="proto-action-row">
              <UiButton as={Link} to={runtimeJumpHref} variant="secondary" disabled={!canJumpToRuntime}>
                带上下文跳转到运行决策台
              </UiButton>
            </div>
          </section>

          <section className="datamap-filter-panel" aria-label="治理过滤器">
            <header className="datamap-filter-head">
              <div>
                <h4>治理过滤器</h4>
                <p>按对象类型、状态、关系、敏感级别和版本快照裁剪资产图谱。</p>
              </div>
              <button
                type="button"
                className="datamap-filter-reset"
                onClick={() => setGraphFilters(normalizeFilterState())}
                disabled={activeFilterCount === 0 || mapContextState.readOnly}
              >
                重置过滤
              </button>
            </header>

            {mode === "lineage" ? (
              <>
                <div className="datamap-filter-row">
                  <div className="datamap-filter-block">
                    <span>聚焦模式</span>
                    <UiSegmentedControl
                      className="datamap-highlight-segment"
                      ariaLabel="图谱聚焦模式切换"
                      value={highlightMode}
                      onChange={setHighlightMode}
                      options={HIGHLIGHT_MODE_OPTIONS}
                    />
                  </div>
                  <div className="datamap-filter-block">
                    <span>快照过滤</span>
                    <UiInput
                      id="datamapSnapshotId"
                      name="datamapSnapshotId"
                      autoComplete="off"
                      value={graphFilters.snapshotId}
                      disabled={mapContextState.readOnly}
                      onChange={(event) => setGraphFilters((prev) => ({
                        ...prev,
                        snapshotId: event.target.value.replace(/\D/g, ""),
                      }))}
                      placeholder="输入 snapshot_id"
                    />
                  </div>
                </div>

                <div className="datamap-filter-row">
                  <div className="datamap-filter-block">
                    <span>当前领域</span>
                    <div className="datamap-tag-list">
                      <span className="datamap-chip" style={{ "--chip-color": nodeTypeColor("DOMAIN") }}>
                        {selectedSceneNode?.meta?.domainName || selectedSceneNode?.meta?.domain || selectedGraphNode?.domainCode || "未绑定领域"}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="datamap-filter-row">
                  <div className="datamap-filter-block">
                    <span>对象类型</span>
                    <div className="datamap-filter-chip-list">
                      {FILTER_OPTIONS.objectTypes.map((item) => {
                        const active = graphFilters.objectTypes.includes(item);
                        return (
                          <button
                            key={item}
                            type="button"
                            className={`datamap-filter-chip ${active ? "is-active" : ""}`}
                            onClick={() => setGraphFilters((prev) => ({
                              ...prev,
                              objectTypes: toggleValue(prev.objectTypes, item),
                            }))}
                          >
                            {toNodeTypeLabel(item)}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                </div>

                <div className="datamap-filter-row">
                  <div className="datamap-filter-block">
                    <span>资产状态</span>
                    <div className="datamap-filter-chip-list">
                      {FILTER_OPTIONS.statuses.map((item) => {
                        const active = graphFilters.statuses.includes(item.value);
                        return (
                          <button
                            key={item.value}
                            type="button"
                            className={`datamap-filter-chip ${active ? "is-active" : ""}`}
                            onClick={() => setGraphFilters((prev) => ({
                              ...prev,
                              statuses: toggleValue(prev.statuses, item.value),
                            }))}
                          >
                            {item.label}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                </div>

                <div className="datamap-filter-row">
                  <div className="datamap-filter-block">
                    <span>关系类型</span>
                    <div className="datamap-filter-chip-list">
                      {relationFilterOptions.length === 0 ? <span className="datamap-empty-tag">暂无</span> : null}
                      {relationFilterOptions.map((item) => {
                        const active = graphFilters.relationTypes.includes(item);
                        return (
                          <button
                            key={item}
                            type="button"
                            className={`datamap-filter-chip ${active ? "is-active" : ""}`}
                            onClick={() => setGraphFilters((prev) => ({
                              ...prev,
                              relationTypes: toggleValue(prev.relationTypes, item),
                            }))}
                          >
                            {item}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                </div>

                <div className="datamap-filter-row">
                  <div className="datamap-filter-block">
                    <span>敏感级别</span>
                    <div className="datamap-filter-chip-list">
                      {FILTER_OPTIONS.sensitivityScopes.map((item) => {
                        const active = graphFilters.sensitivityScopes.includes(item);
                        return (
                          <button
                            key={item}
                            type="button"
                            className={`datamap-filter-chip ${active ? "is-active" : ""}`}
                            onClick={() => setGraphFilters((prev) => ({
                              ...prev,
                              sensitivityScopes: toggleValue(prev.sensitivityScopes, item),
                            }))}
                          >
                            {item}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                </div>
              </>
            ) : (
              <p className="subtle-note">浏览模式下先定位业务场景，再切到资产图谱应用治理过滤器。</p>
            )}
          </section>

          <section className="datamap-kpi-grid" aria-label="图谱关键指标">
            <article className="datamap-kpi-card">
              <strong>{graphMetrics.nodeCount}</strong>
              <span>节点数</span>
            </article>
            <article className="datamap-kpi-card">
              <strong>{graphMetrics.edgeCount}</strong>
              <span>关系数</span>
            </article>
            <article className="datamap-kpi-card">
              <strong>{graphMetrics.typeCount}</strong>
              <span>类型数</span>
            </article>
          </section>

          <section className="datamap-tag-panel" aria-label="类型和关系标签">
            <div>
              <h4>节点类型</h4>
              <div className="datamap-tag-list">
                {graphMetrics.typeStats.length === 0 ? <span className="datamap-empty-tag">暂无</span> : null}
                {graphMetrics.typeStats.map((item) => (
                  <span key={`type-${item.type}`} className="datamap-chip" style={{ "--chip-color": item.color }}>
                    {item.label}
                  </span>
                ))}
              </div>
            </div>
            <div>
              <h4>关系标签</h4>
              <div className="datamap-tag-list">
                {relationTags.length === 0 ? <span className="datamap-empty-tag">暂无</span> : null}
                {relationTags.map((item) => (
                  <span key={`relation-${item}`} className="datamap-chip is-relation">{item}</span>
                ))}
              </div>
            </div>
          </section>

          <section className="datamap-detail-panel" aria-label="焦点详情">
            <header className="datamap-detail-head">
              <div>
                <h4>焦点详情</h4>
                <p>节点详情来自只读 Graph DTO Adapter，边详情直接取自当前图谱关系。</p>
              </div>
            </header>

            {mode !== "lineage" ? (
              <p className="subtle-note">切换到资产图谱模式后可查看节点与关系详情。</p>
            ) : null}

            {mode === "lineage" && nodeDetailLoading ? <p className="subtle-note">正在加载资产详情…</p> : null}
            {mode === "lineage" && nodeDetailError ? (
              <UiInlineError className="datamap-preview-error" message={nodeDetailError} />
            ) : null}

            {mode === "lineage" && !nodeDetailLoading && !nodeDetailError && selectedGraphNode ? (
              <div className="datamap-detail-body">
                <article className="datamap-focus-card">
                  <div className="datamap-focus-head">
                    <div>
                      <strong>{selectedGraphNode.objectName || selectedGraphNode.label}</strong>
                      <span>{toNodeTypeLabel(selectedGraphNode.objectType)}</span>
                    </div>
                    <span className="datamap-chip" style={{ "--chip-color": nodeTypeColor(selectedGraphNode.objectType) }}>
                      {selectedGraphNode.status ? describeSceneStatus(selectedGraphNode.status).label : "未知状态"}
                    </span>
                  </div>
                  <p>{selectedGraphNode.summaryText || "暂无摘要说明"}</p>
                  <div className="datamap-focus-meta">
                    <span>编码：{selectedGraphNode.objectCode || "—"}</span>
                    <span>领域：{selectedGraphNode.domainCode || "—"}</span>
                    <span>快照：{selectedGraphNode.snapshotId || "—"}</span>
                    <span>敏感级别：{selectedGraphNode.sensitivityScope || "—"}</span>
                  </div>
                </article>

                <div className="datamap-detail-grid">
                  {detailEntries.length === 0 ? <p className="subtle-note">暂无扩展属性</p> : null}
                  {detailEntries.map(([key, value]) => (
                    <article key={key} className="datamap-detail-item">
                      <span>{key}</span>
                      <strong>{formatAttributeValue(value)}</strong>
                    </article>
                  ))}
                </div>
              </div>
            ) : null}

            {mode === "lineage" && selectedGraphEdge ? (
              <article className="datamap-edge-card">
                <header>
                  <strong>{selectedGraphEdge.relationType || selectedGraphEdge.label || "关系详情"}</strong>
                  {selectedGraphEdge.policyHit ? <span className="datamap-edge-badge">策略命中</span> : null}
                </header>
                <p>{selectedGraphEdge.coverageExplanation || "当前关系暂无额外解释。"} </p>
                <div className="datamap-focus-meta">
                  <span>来源节点：{selectedGraphEdge.source}</span>
                  <span>目标节点：{selectedGraphEdge.target}</span>
                  <span>Trace：{selectedGraphEdge.traceId || "—"}</span>
                  <span>Confidence：{selectedGraphEdge.confidence ?? "—"}</span>
                </div>
              </article>
            ) : null}
          </section>

          <section className="datamap-process-list" aria-label="流程步骤">
            {processSteps.map((step) => {
              const status = step.done ? "done" : step.no === activeStepNo ? "active" : "todo";
              return (
                <article key={step.key} className={`datamap-process-card status-${status}`}>
                  <header>
                    <span className="step-no">{String(step.no).padStart(2, "0")}</span>
                    <h4>{step.title}</h4>
                    <span className={`step-badge tone-${status}`}>{status === "done" ? "已完成" : status === "active" ? "进行中" : "待处理"}</span>
                  </header>
                  <p>{step.description}</p>
                  <p className="step-summary">{step.summary}</p>
                  <div className="step-tag-list">
                    {step.tags.map((tag) => <span key={`${step.key}-${tag}`}>{tag}</span>)}
                  </div>
                </article>
              );
            })}
          </section>

          <section className="datamap-impact-panel" aria-label="影响分析">
            <header className="datamap-detail-head">
              <div>
                <h4>影响分析</h4>
                <p>支持路径高亮、证据追踪和影响分析三种聚焦模式。</p>
              </div>
              {mode === "lineage" ? (
                <span className={`datamap-risk-pill tone-${(impactAnalysis?.riskLevel || "LOW").toLowerCase()}`}>
                  {highlightMode === "impact" ? `风险 ${impactAnalysis?.riskLevel || "LOW"}` : `当前模式：${HIGHLIGHT_MODE_OPTIONS.find((item) => item.value === highlightMode)?.label || "路径高亮"}`}
                </span>
              ) : null}
            </header>

            {mode !== "lineage" ? (
              <p className="subtle-note">浏览模式下先选择场景，再切到资产图谱查看影响分析。</p>
            ) : null}

            {mode === "lineage" && highlightMode !== "impact" ? (
              <p className="subtle-note">切换到“影响分析”模式后，会基于当前焦点资产生成影响子图与建议动作。</p>
            ) : null}
            {mode === "lineage" && highlightMode === "impact" && impactLoading ? <p className="subtle-note">正在生成影响分析…</p> : null}
            {mode === "lineage" && highlightMode === "impact" && impactError ? (
              <UiInlineError className="datamap-preview-error" message={impactError} />
            ) : null}

            {mode === "lineage" && highlightMode === "impact" && !impactLoading && !impactError && impactAnalysis ? (
              <div className="datamap-impact-body">
                <div className="datamap-impact-actions">
                  {impactAnalysis.recommendedActions?.length === 0 ? <span className="datamap-empty-tag">暂无建议</span> : null}
                  {(impactAnalysis.recommendedActions || []).map((item) => (
                    <article key={item} className="datamap-impact-action">
                      {item}
                    </article>
                  ))}
                </div>
                <div className="datamap-impact-assets">
                  {(impactAnalysis.affectedAssets || []).length === 0 ? <span className="datamap-empty-tag">暂无受影响资产</span> : null}
                  {(impactAnalysis.affectedAssets || []).slice(0, 8).map((item) => (
                    <article key={item.assetRef} className="datamap-impact-asset">
                      <strong>{item.objectName}</strong>
                      <span>{toNodeTypeLabel(item.objectType)}</span>
                      <p>{item.impactSummary || "暂无说明"}</p>
                    </article>
                  ))}
                </div>
              </div>
            ) : null}
          </section>

          <section className={`datamap-log-panel ${logsExpanded ? "is-open" : ""}`} aria-label="系统日志面板">
            <header className="datamap-log-head">
              <div>
                <strong>SYSTEM DASHBOARD</strong>
                <p>默认折叠，按需查看排障与行为轨迹。</p>
              </div>
              <button type="button" className="datamap-log-expand" onClick={() => setLogsExpanded((prev) => !prev)}>
                {logsExpanded ? "收起" : "展开"}
              </button>
            </header>

            {logsExpanded ? (
              <>
                <UiSegmentedControl
                  className="datamap-log-segment"
                  ariaLabel="日志视图切换"
                  value={logView}
                  onChange={setLogView}
                  options={[
                    { value: "business", label: "业务日志" },
                    { value: "technical", label: "技术日志" },
                  ]}
                />
                <div className="datamap-log-body" ref={logScrollRef}>
                  {eventLogs.length === 0 ? <p className="datamap-log-empty">暂无日志</p> : null}
                  {eventLogs.map((item) => (
                    <article key={item.id} className={`datamap-log-row level-${item.level}`}>
                      <div className="row-head">
                        <span className="time">{item.at}</span>
                        <span className="msg">{item.business}</span>
                      </div>
                      {logView === "technical" && item.technical ? (
                        <pre>{item.technical}</pre>
                      ) : null}
                    </article>
                  ))}
                </div>
              </>
            ) : null}
          </section>
        </aside>
      </section>
    </article>
  );
}
