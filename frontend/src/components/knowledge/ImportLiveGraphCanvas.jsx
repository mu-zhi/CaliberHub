import { useMemo } from "react";
import ReactFlow, { Background, Controls, MarkerType } from "reactflow";
import "reactflow/dist/style.css";
import { getSelectedImportLiveGraphNode } from "./importLiveGraphState";

const TYPE_ORDER = {
  CANDIDATE_SCENE: 0,
  INPUT_FIELD: 1,
  OUTPUT_FIELD: 2,
  SOURCE_TABLE: 3,
  CANDIDATE_EVIDENCE_FRAGMENT: 4,
};

const TYPE_COLOR = {
  CANDIDATE_SCENE: "#0f4d78",
  INPUT_FIELD: "#f59e0b",
  OUTPUT_FIELD: "#16a34a",
  SOURCE_TABLE: "#2563eb",
  CANDIDATE_EVIDENCE_FRAGMENT: "#92400e",
};

function nodeColor(type) {
  return TYPE_COLOR[type] || "#456178";
}

function buildLayout(nodes = [], edges = [], selectedNodeId = "") {
  const groups = new Map();
  nodes.forEach((node) => {
    const order = Number.isFinite(TYPE_ORDER[node.nodeType]) ? TYPE_ORDER[node.nodeType] : 99;
    if (!groups.has(order)) {
      groups.set(order, []);
    }
    groups.get(order).push(node);
  });

  const flowNodes = [];
  [...groups.entries()]
    .sort((a, b) => a[0] - b[0])
    .forEach(([order, items]) => {
      const x = 90 + order * 230;
      items.forEach((node, index) => {
        const isSelected = node.id === selectedNodeId;
        const color = nodeColor(node.nodeType);
        flowNodes.push({
          id: node.id,
          position: { x, y: 80 + index * 110 },
          sourcePosition: "right",
          targetPosition: "left",
          data: {
            label: (
              <div className="import-live-graph-node-label">
                <strong>{node.label}</strong>
                <span>{node.nodeType}</span>
              </div>
            ),
            nodeType: node.nodeType,
          },
          style: {
            width: 190,
            minHeight: 68,
            borderRadius: 20,
            border: `2px solid ${color}`,
            background: isSelected
              ? `color-mix(in srgb, ${color} 18%, #ffffff 82%)`
              : `color-mix(in srgb, ${color} 9%, #ffffff 91%)`,
            color: "#173042",
            boxShadow: isSelected ? "0 18px 30px rgba(12, 39, 58, 0.18)" : "0 10px 18px rgba(12, 39, 58, 0.10)",
            fontSize: 13,
            fontWeight: 600,
            padding: "10px 12px",
          },
        });
      });
    });

  const flowEdges = edges.map((edge) => ({
    id: edge.id,
    source: edge.sourceId,
    target: edge.targetId,
    label: edge.relationType,
    type: "smoothstep",
    animated: false,
    style: {
      stroke: "#8aa4b6",
      strokeWidth: 2.4,
    },
    markerEnd: {
      type: MarkerType.ArrowClosed,
      color: "#8aa4b6",
    },
    labelStyle: {
      fill: "#355267",
      fontSize: 11,
      fontWeight: 700,
    },
    labelBgStyle: {
      fill: "rgba(255, 255, 255, 0.9)",
    },
    labelBgBorderRadius: 6,
  }));

  return { flowNodes, flowEdges };
}

function buildNodeMetrics(nodes, edges, selectedNodeId) {
  if (!selectedNodeId) {
    return { incoming: 0, outgoing: 0 };
  }
  let incoming = 0;
  let outgoing = 0;
  edges.forEach((edge) => {
    if (edge.targetId === selectedNodeId) {
      incoming += 1;
    }
    if (edge.sourceId === selectedNodeId) {
      outgoing += 1;
    }
  });
  return { incoming, outgoing };
}

export function ImportLiveGraphCanvas({
  graphState,
  importPercent = 0,
  importStageText = "待导入",
  onSelectNode,
}) {
  const selectedNode = getSelectedImportLiveGraphNode(graphState);
  const { flowNodes, flowEdges } = useMemo(
    () => buildLayout(graphState?.nodes || [], graphState?.edges || [], graphState?.selectedNodeId || ""),
    [graphState?.edges, graphState?.nodes, graphState?.selectedNodeId],
  );
  const metrics = useMemo(
    () => buildNodeMetrics(graphState?.nodes || [], graphState?.edges || [], selectedNode?.id || ""),
    [graphState?.edges, graphState?.nodes, selectedNode?.id],
  );
  const hasGraph = flowNodes.length > 0;
  const isServer = typeof window === "undefined";

  return (
    <section className="import-live-graph-shell">
      <div className="import-live-graph-main">
        <div className="import-live-graph-head">
          <div>
            <h3>候选实体图谱</h3>
            <p>{graphState?.summaryMessage || "导入开始后显示候选实体图谱"}</p>
          </div>
          <span>
            {Math.max(0, Math.min(100, Number(importPercent || 0)))}% · {importStageText || "待导入"}
          </span>
        </div>
        {!hasGraph ? (
          <div className="import-live-graph-empty">
            <strong>导入开始后显示候选实体图谱</strong>
            <p>导入开始后，候选场景、字段和来源表会在这里实时补充出来。</p>
          </div>
        ) : isServer ? (
          <div className="import-live-graph-empty">
            <strong>候选实体图谱</strong>
            <p>服务端渲染阶段仅输出工作台壳层，客户端加载后会显示可交互图谱。</p>
          </div>
        ) : (
          <div className="import-live-graph-canvas" aria-label="候选实体图谱画布">
            <ReactFlow
              nodes={flowNodes}
              edges={flowEdges}
              fitView
              minZoom={0.35}
              maxZoom={1.6}
              nodesDraggable={false}
              elementsSelectable
              proOptions={{ hideAttribution: true }}
              onNodeClick={(_, node) => onSelectNode?.(node.id)}
              onPaneClick={() => onSelectNode?.("")}
            >
              <Controls position="bottom-right" />
              <Background variant="dots" gap={18} size={1.2} color="#d5e0e7" />
            </ReactFlow>
          </div>
        )}
      </div>

      <aside className="import-live-graph-inspector">
        {selectedNode ? (
          <>
            <div className="import-live-graph-inspector-head">
              <h3>{selectedNode.label}</h3>
              <span>{selectedNode.nodeType}</span>
            </div>
            <dl className="import-live-graph-detail-list">
              <div>
                <dt>状态</dt>
                <dd>{selectedNode.status}</dd>
              </div>
              <div>
                <dt>置信度</dt>
                <dd>{selectedNode.confidenceScore > 0 ? `${(selectedNode.confidenceScore * 100).toFixed(0)}%` : "--"}</dd>
              </div>
              <div>
                <dt>入边数</dt>
                <dd>{metrics.incoming}</dd>
              </div>
              <div>
                <dt>出边数</dt>
                <dd>{metrics.outgoing}</dd>
              </div>
              <div>
                <dt>证据引用</dt>
                <dd>{selectedNode.evidenceRefs.length > 0 ? selectedNode.evidenceRefs.join("，") : "暂无"}</dd>
              </div>
            </dl>
          </>
        ) : (
          <>
            <div className="import-live-graph-inspector-head">
              <h3>抽取进度</h3>
              <span>{graphState?.stageName || "待导入"}</span>
            </div>
            <div className="import-live-graph-default-card">
              <strong>{graphState?.nodes?.length || 0}</strong>
              <span>节点</span>
              <strong>{graphState?.edges?.length || 0}</strong>
              <span>关系</span>
            </div>
            <div className="import-live-graph-recent">
              <p>最近新增</p>
              {graphState?.recentActivity?.length ? (
                <ul>
                  {graphState.recentActivity.map((item) => (
                    <li key={item.id}>
                      <strong>{item.label}</strong>
                      <span>{item.meta}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="subtle-note">导入开始后，这里会显示最近新增的实体与关系。</p>
              )}
            </div>
          </>
        )}
      </aside>
    </section>
  );
}
