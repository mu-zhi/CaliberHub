import { useMemo } from "react";
import ReactFlow, { Background, Controls, MarkerType, MiniMap } from "reactflow";
import "reactflow/dist/style.css";
import { ArrowLeftRight } from "lucide-react";
import { DataMapEmptyState } from "./DataMapEmptyState";
import { UiButton, UiInlineError } from "../ui";

const TYPE_ORDER = {
  SOURCE: 0,
  WAREHOUSE: 1,
  MART: 2,
  APP: 3,
};

const TYPE_COLOR = {
  SOURCE: "#d64545",
  WAREHOUSE: "#2f7de1",
  MART: "#22a6a0",
  APP: "#7a4bd9",
};

function nodeColor(type) {
  return TYPE_COLOR[type] || "#456178";
}

function buildLayout(data) {
  const groups = new Map();
  (data?.nodes || []).forEach((node) => {
    const type = `${node?.type || "APP"}`.toUpperCase();
    const key = Number.isFinite(TYPE_ORDER[type]) ? TYPE_ORDER[type] : 3;
    if (!groups.has(key)) {
      groups.set(key, []);
    }
    groups.get(key).push({ ...node, type });
  });

  const flowNodes = [];
  [...groups.entries()].sort((a, b) => a[0] - b[0]).forEach(([order, nodes]) => {
    const x = 120 + order * 280;
    nodes.forEach((node, index) => {
      const y = 100 + index * 140;
      const color = nodeColor(node.type);
      flowNodes.push({
        id: node.id,
        position: { x, y },
        sourcePosition: "right",
        targetPosition: "left",
        data: { label: node.label, type: node.type },
        style: {
          border: `2px solid ${color}`,
          borderRadius: node.type === "APP" ? "18px" : "999px",
          background: node.type === "APP" ? "rgba(122, 75, 217, 0.12)" : "#ffffff",
          color: "#1f2d38",
          width: 190,
          minHeight: 56,
          fontSize: 13,
          fontWeight: 600,
          textAlign: "center",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          boxShadow: "0 10px 22px rgba(9, 34, 53, 0.10)",
        },
      });
    });
  });

  const typeMap = new Map(flowNodes.map((node) => [node.id, node?.data?.type]));
  const flowEdges = (data?.edges || []).map((edge, index) => {
    const sourceType = typeMap.get(edge.source);
    const stroke = nodeColor(sourceType);
    return {
      id: `${edge.source}-${edge.target}-${index}`,
      source: edge.source,
      target: edge.target,
      label: edge.label || "",
      type: "bezier",
      style: {
        stroke,
        strokeWidth: 3,
      },
      markerEnd: {
        type: MarkerType.ArrowClosed,
        color: stroke,
      },
      labelStyle: {
        fill: "#284a61",
        fontSize: 12,
        fontWeight: 600,
      },
    };
  });
  return { flowNodes, flowEdges };
}

export function LineageGraphView({
  sceneNode,
  lineageData,
  loading,
  error,
  onRetry,
  onBackToBrowse,
}) {
  const { flowNodes, flowEdges } = useMemo(() => buildLayout(lineageData), [lineageData]);

  if (!sceneNode) {
    return (
      <DataMapEmptyState
        title="尚未选择业务场景"
        description="请先在浏览模式中选择一个具体的业务场景"
        action={(
          <UiButton type="button" onClick={onBackToBrowse} icon={<ArrowLeftRight size={15} />}>
            返回浏览模式
          </UiButton>
        )}
      />
    );
  }

  if (loading) {
    return <p className="subtle-note">正在生成血缘链路…</p>;
  }

  if (error) {
    return (
      <UiInlineError
        className="lineage-error"
        message={error}
        actionText="重试"
        onAction={onRetry}
      />
    );
  }

  if (flowNodes.length === 0) {
    return (
      <DataMapEmptyState
        title="暂无血缘链路"
        description="当前场景暂未识别到可视化血缘节点。"
      />
    );
  }

  return (
    <section className="lineage-canvas-wrap" aria-label="血缘图画布">
      {lineageData?.truncated ? (
        <p className="lineage-truncated-note">
          节点过多，已默认展示核心链路，隐藏 {lineageData.hiddenNodeCount || 0} 个节点。
        </p>
      ) : null}
      <ReactFlow nodes={flowNodes} edges={flowEdges} fitView minZoom={0.2} maxZoom={1.8}>
        <MiniMap
          nodeColor={(node) => nodeColor(node?.data?.type)}
          nodeStrokeWidth={2}
          pannable
          zoomable
        />
        <Controls position="bottom-right" />
        <Background variant="dots" gap={18} size={1.2} color="#d9e3ea" />
      </ReactFlow>
    </section>
  );
}
