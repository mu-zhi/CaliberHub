import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { fetchDataMapColumn, fetchLineageGraph, fetchSceneDetail, extractSceneId } from "../../pages/datamap-adapter";
import { LineageGraphView } from "./LineageGraphView";
import { MillerColumnsView } from "./MillerColumnsView";
import { PreviewPanel } from "./PreviewPanel";
import { UiInput, UiSegmentedControl } from "../ui";

const ROOT_COLUMN_ID = "ROOT";

function defaultModeByPreset(viewPreset) {
  return viewPreset === "lineage" ? "lineage" : "browse";
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
    lineage: "血缘分析",
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

export function DataMapContainer({ viewPreset = "map" }) {
  const [mode, setMode] = useState(() => defaultModeByPreset(viewPreset));
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

  const columnsTrackRef = useRef(null);
  const rootRequestRef = useRef(0);
  const childRequestRef = useRef(0);

  useEffect(() => {
    setMode(defaultModeByPreset(viewPreset));
  }, [viewPreset]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedKeyword(keyword.trim());
    }, 260);
    return () => window.clearTimeout(timer);
  }, [keyword]);

  const reloadRoot = useCallback(async () => {
    const requestId = rootRequestRef.current + 1;
    rootRequestRef.current = requestId;

    setColumns([buildEmptyColumn(ROOT_COLUMN_ID, true)]);
    setActivePath([]);
    setSelectedSceneNode(null);
    setSceneDetail(null);
    setSceneError("");
    setLineageData(null);
    setLineageError("");

    try {
      const response = await fetchDataMapColumn(ROOT_COLUMN_ID, {
        keyword: debouncedKeyword,
        view: viewPreset,
      });
      if (rootRequestRef.current !== requestId) {
        return;
      }
      setColumns([{
        columnId: response.columnId || ROOT_COLUMN_ID,
        items: response.items || [],
        loading: false,
        error: "",
      }]);
    } catch (error) {
      if (rootRequestRef.current !== requestId) {
        return;
      }
      setColumns([buildEmptyColumn(ROOT_COLUMN_ID, false, error.message || "根节点加载失败")]);
    }
  }, [debouncedKeyword, viewPreset]);

  useEffect(() => {
    reloadRoot();
  }, [reloadRoot]);

  useEffect(() => {
    if (mode !== "browse") {
      return;
    }
    const track = columnsTrackRef.current;
    if (!track) {
      return;
    }
    track.scrollTo({
      left: track.scrollWidth,
      behavior: "smooth",
    });
  }, [columns.length, mode]);

  const selectedSceneId = useMemo(() => extractSceneId(selectedSceneNode), [selectedSceneNode]);

  useEffect(() => {
    if (!selectedSceneId) {
      setSceneDetail(null);
      setSceneLoading(false);
      setSceneError("");
      return;
    }
    let cancelled = false;
    setSceneLoading(true);
    setSceneError("");
    fetchSceneDetail(selectedSceneId)
      .then((payload) => {
        if (cancelled) {
          return;
        }
        setSceneDetail(payload);
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }
        setSceneError(error.message || "场景详情加载失败");
      })
      .finally(() => {
        if (!cancelled) {
          setSceneLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [selectedSceneId]);

  const loadLineage = useCallback(async () => {
    if (!selectedSceneId) {
      setLineageData(null);
      setLineageError("");
      return;
    }
    setLineageLoading(true);
    setLineageError("");
    try {
      const payload = await fetchLineageGraph(selectedSceneId, { maxNodes: 50 });
      console.log("[Lineage] Fetched data for scene", selectedSceneId, payload);
      setLineageData(payload);
    } catch (error) {
      console.error("[Lineage] Failed to fetch:", error);
      setLineageError(error.message || "血缘链路加载失败");
    } finally {
      setLineageLoading(false);
    }
  }, [selectedSceneId]);

  useEffect(() => {
    if (mode !== "lineage") {
      return;
    }
    loadLineage();
  }, [mode, loadLineage]);

  async function loadChildColumn(node, columnIndex) {
    const requestId = childRequestRef.current + 1;
    childRequestRef.current = requestId;

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
    } catch (error) {
      if (childRequestRef.current !== requestId) {
        return;
      }
      setColumns((prev) => {
        const prefix = prev.slice(0, columnIndex + 1);
        return [...prefix, buildEmptyColumn(node.id, false, error.message || "列加载失败")];
      });
    }
  }

  function handleSelectNode(node, columnIndex) {
    setActivePath((prev) => {
      const next = prev.slice(0, columnIndex);
      next.push(node.id);
      return next;
    });
    setColumns((prev) => prev.slice(0, columnIndex + 1));

    if (node.type === "SCENE") {
      setSelectedSceneNode(node);
    } else {
      setSelectedSceneNode(null);
      setSceneDetail(null);
      setLineageData(null);
    }

    if (node.hasChildren) {
      loadChildColumn(node, columnIndex);
    }
  }

  function handleRetryColumn(columnId, columnIndex) {
    if (columnIndex === 0 || columnId === ROOT_COLUMN_ID) {
      reloadRoot();
      return;
    }
    const parentNodeId = activePath[columnIndex - 1];
    if (!parentNodeId) {
      reloadRoot();
      return;
    }
    loadChildColumn({ id: parentNodeId }, columnIndex - 1);
  }

  function handleMoveLeft(columnIndex) {
    if (columnIndex <= 0) {
      return;
    }
    setColumns((prev) => prev.slice(0, columnIndex));
    setActivePath((prev) => prev.slice(0, columnIndex));
    setSelectedSceneNode(null);
    setSceneDetail(null);
    setLineageData(null);
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
      onRetry={() => {
        if (!selectedSceneId) {
          return;
        }
        setSceneDetail(null);
        setSceneError("");
        setSceneLoading(true);
        fetchSceneDetail(selectedSceneId)
          .then((payload) => setSceneDetail(payload))
          .catch((error) => setSceneError(error.message || "场景详情加载失败"))
          .finally(() => setSceneLoading(false));
      }}
      onSwitchToLineage={() => setMode("lineage")}
    />
  ) : null;

  return (
    <article className="panel datamap-container" aria-label="双模态数据地图容器">
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
            ariaLabel="数据地图模式切换"
            value={mode}
            onChange={setMode}
            options={[
              { value: "browse", label: "浏览模式" },
              { value: "lineage", label: "血缘模式" },
            ]}
          />
        </div>
        <p className="datamap-toolbar-caption">
          当前入口：{viewLabel(viewPreset)} · 路由已统一接入双模态工作台
        </p>
      </header>

      <section className="datamap-content-shell">
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
            onRetry={loadLineage}
            onBackToBrowse={() => setMode("browse")}
          />
        )}
      </section>
    </article>
  );
}
