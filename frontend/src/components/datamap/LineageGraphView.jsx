import { useEffect, useRef, useMemo, useCallback } from "react";
import * as d3 from "d3";
import { ArrowLeftRight } from "lucide-react";
import { DataMapEmptyState } from "./DataMapEmptyState";
import { UiButton, UiInlineError } from "../ui";

const TYPE_COLOR = {
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

function nodeColor(type) {
  return TYPE_COLOR[type] || "#456178";
}

function truncateLabel(text, max = 12) {
  const s = `${text || ""}`.trim();
  return s.length > max ? s.substring(0, max) + "\u2026" : s;
}

function prepareGraphData(data) {
  const safeNodes = Array.isArray(data?.nodes) ? data.nodes : [];
  const safeEdges = Array.isArray(data?.edges) ? data.edges : [];

  const nodeMap = new Map();
  const nodes = safeNodes
    .filter((n) => n && n.id)
    .map((n) => {
      const type = `${n.objectType || n.type || "SCENE"}`.toUpperCase();
      const obj = {
        id: n.id,
        name: n.objectName || n.label || n.id,
        code: n.objectCode || "",
        type,
        rawData: n,
      };
      nodeMap.set(n.id, obj);
      return obj;
    });

  const edgePairCount = {};
  safeEdges.forEach((e) => {
    if (!e || !e.source || !e.target) return;
    const key = [e.source, e.target].sort().join("__");
    edgePairCount[key] = (edgePairCount[key] || 0) + 1;
  });

  const edgePairIndex = {};
  const edges = safeEdges
    .filter((e) => e && e.source && e.target && nodeMap.has(e.source) && nodeMap.has(e.target))
    .map((e, i) => {
      const pairKey = [e.source, e.target].sort().join("__");
      const totalCount = edgePairCount[pairKey];
      const currentIndex = edgePairIndex[pairKey] || 0;
      edgePairIndex[pairKey] = currentIndex + 1;

      let curvature = 0;
      if (totalCount > 1) {
        const range = Math.min(1.2, 0.6 + totalCount * 0.15);
        curvature = ((currentIndex / (totalCount - 1)) - 0.5) * range * 2;
        if (e.source > e.target) curvature = -curvature;
      }

      return {
        id: e.id || `${e.source}-${e.target}-${i}`,
        source: e.source,
        target: e.target,
        label: `${e.label || e.relationType || ""}`.trim(),
        curvature,
        pairTotal: totalCount,
        rawData: e,
      };
    });

  return { nodes, edges };
}

function getLinkPath(d) {
  const sx = d.source.x, sy = d.source.y;
  const tx = d.target.x, ty = d.target.y;

  if (d.source.id === d.target.id) {
    const r = 30;
    return `M${sx + 8},${sy - 4} A${r},${r} 0 1,1 ${sx + 8},${sy + 4}`;
  }

  if (d.curvature === 0) {
    return `M${sx},${sy} L${tx},${ty}`;
  }

  const dx = tx - sx, dy = ty - sy;
  const dist = Math.sqrt(dx * dx + dy * dy) || 1;
  const ratio = 0.25 + (d.pairTotal || 1) * 0.05;
  const base = Math.max(35, dist * ratio);
  const ox = (-dy / dist) * d.curvature * base;
  const oy = (dx / dist) * d.curvature * base;
  const cx = (sx + tx) / 2 + ox;
  const cy = (sy + ty) / 2 + oy;
  return `M${sx},${sy} Q${cx},${cy} ${tx},${ty}`;
}

function getLinkMidpoint(d) {
  const sx = d.source.x, sy = d.source.y;
  const tx = d.target.x, ty = d.target.y;

  if (d.source.id === d.target.id) {
    return { x: sx + 60, y: sy };
  }

  if (d.curvature === 0) {
    return { x: (sx + tx) / 2, y: (sy + ty) / 2 };
  }

  const dx = tx - sx, dy = ty - sy;
  const dist = Math.sqrt(dx * dx + dy * dy) || 1;
  const ratio = 0.25 + (d.pairTotal || 1) * 0.05;
  const base = Math.max(35, dist * ratio);
  const ox = (-dy / dist) * d.curvature * base;
  const oy = (dx / dist) * d.curvature * base;
  const cx = (sx + tx) / 2 + ox;
  const cy = (sy + ty) / 2 + oy;
  return {
    x: 0.25 * sx + 0.5 * cx + 0.25 * tx,
    y: 0.25 * sy + 0.5 * cy + 0.25 * ty,
  };
}

export function LineageGraphView({
  sceneNode,
  lineageData,
  loading,
  error,
  showEdgeLabels = true,
  selectedNodeId = "",
  selectedEdgeId = "",
  highlightedNodeIds = [],
  highlightedEdgeIds = [],
  onNodeSelect,
  onEdgeSelect,
  onRetry,
  onBackToBrowse,
}) {
  const containerRef = useRef(null);
  const svgRef = useRef(null);
  const simulationRef = useRef(null);
  const showEdgeLabelsRef = useRef(showEdgeLabels);
  showEdgeLabelsRef.current = showEdgeLabels;
  const zoomBehaviorRef = useRef(null);

  const graphData = useMemo(() => {
    if (!lineageData?.nodes?.length) return null;
    return prepareGraphData(lineageData);
  }, [lineageData]);

  const onNodeSelectRef = useRef(onNodeSelect);
  const onEdgeSelectRef = useRef(onEdgeSelect);
  onNodeSelectRef.current = onNodeSelect;
  onEdgeSelectRef.current = onEdgeSelect;

  const selectedNodeIdRef = useRef(selectedNodeId);
  selectedNodeIdRef.current = selectedNodeId;
  const selectedEdgeIdRef = useRef(selectedEdgeId);
  selectedEdgeIdRef.current = selectedEdgeId;

  const renderGraph = useCallback(() => {
    const container = containerRef.current;
    const svgEl = svgRef.current;
    if (!container || !svgEl || !graphData) return;

    if (simulationRef.current) {
      simulationRef.current.stop();
      simulationRef.current = null;
    }

    const svg = d3.select(svgEl);
    svg.selectAll("*").remove();

    const width = container.clientWidth || 800;
    const height = container.clientHeight || 600;
    svg.attr("width", width).attr("height", height);

    const nodes = graphData.nodes.map((n) => ({ ...n }));
    const edges = graphData.edges.map((e) => ({ ...e }));

    const simulation = d3.forceSimulation(nodes)
      .force("link", d3.forceLink(edges).id((d) => d.id).distance((d) => {
        const base = 150;
        const count = d.pairTotal || 1;
        return base + (count - 1) * 50;
      }))
      .force("charge", d3.forceManyBody().strength(-400))
      .force("center", d3.forceCenter(width / 2, height / 2))
      .force("collide", d3.forceCollide(50))
      .force("x", d3.forceX(width / 2).strength(0.04))
      .force("y", d3.forceY(height / 2).strength(0.04));

    simulationRef.current = simulation;

    const g = svg.append("g");

    const zoomBehavior = d3.zoom()
      .extent([[0, 0], [width, height]])
      .scaleExtent([0.1, 4])
      .on("zoom", (event) => {
        g.attr("transform", event.transform);
      });
    zoomBehaviorRef.current = zoomBehavior;
    svg.call(zoomBehavior);

    svg.on("click", () => {
      onNodeSelectRef.current?.(null);
      onEdgeSelectRef.current?.(null);
      resetHighlights();
    });

    const defs = svg.append("defs");
    defs.append("marker")
      .attr("id", "arrowhead")
      .attr("viewBox", "0 -5 10 10")
      .attr("refX", 20)
      .attr("refY", 0)
      .attr("markerWidth", 6)
      .attr("markerHeight", 6)
      .attr("orient", "auto")
      .append("path")
      .attr("d", "M0,-5L10,0L0,5")
      .attr("fill", "#C0C0C0");

    defs.append("marker")
      .attr("id", "arrowhead-active")
      .attr("viewBox", "0 -5 10 10")
      .attr("refX", 20)
      .attr("refY", 0)
      .attr("markerWidth", 6)
      .attr("markerHeight", 6)
      .attr("orient", "auto")
      .append("path")
      .attr("d", "M0,-5L10,0L0,5")
      .attr("fill", "#E91E63");

    const linkGroup = g.append("g").attr("class", "links");

    const link = linkGroup.selectAll("path")
      .data(edges)
      .enter().append("path")
      .attr("stroke", "#C0C0C0")
      .attr("stroke-width", 1.5)
      .attr("fill", "none")
      .attr("marker-end", "url(#arrowhead)")
      .style("cursor", "pointer")
      .on("click", (event, d) => {
        event.stopPropagation();
        resetHighlights();
        d3.select(event.target).attr("stroke", "#3498db").attr("stroke-width", 3);
        onEdgeSelectRef.current?.(d.rawData || null);
      });

    const linkLabelBg = linkGroup.selectAll("rect.link-label-bg")
      .data(edges)
      .enter().append("rect")
      .attr("class", "link-label-bg")
      .attr("fill", "rgba(255,255,255,0.92)")
      .attr("rx", 3)
      .attr("ry", 3)
      .style("pointer-events", "none")
      .style("display", showEdgeLabelsRef.current ? "block" : "none");

    const linkLabels = linkGroup.selectAll("text.link-label")
      .data(edges)
      .enter().append("text")
      .attr("class", "link-label")
      .text((d) => d.label)
      .attr("font-size", "9px")
      .attr("fill", "#666")
      .attr("text-anchor", "middle")
      .attr("dominant-baseline", "middle")
      .style("pointer-events", "none")
      .style("font-family", "system-ui, sans-serif")
      .style("display", showEdgeLabelsRef.current ? "block" : "none");

    const nodeGroup = g.append("g").attr("class", "nodes");

    const node = nodeGroup.selectAll("circle")
      .data(nodes)
      .enter().append("circle")
      .attr("r", 10)
      .attr("fill", (d) => nodeColor(d.type))
      .attr("stroke", "#fff")
      .attr("stroke-width", 2.5)
      .style("cursor", "pointer")
      .call(d3.drag()
        .on("start", (event, d) => {
          d.fx = d.x;
          d.fy = d.y;
          d._dragStartX = event.x;
          d._dragStartY = event.y;
          d._isDragging = false;
        })
        .on("drag", (event, d) => {
          const dx = event.x - d._dragStartX;
          const dy = event.y - d._dragStartY;
          if (!d._isDragging && Math.sqrt(dx * dx + dy * dy) > 3) {
            d._isDragging = true;
            simulation.alphaTarget(0.3).restart();
          }
          if (d._isDragging) {
            d.fx = event.x;
            d.fy = event.y;
          }
        })
        .on("end", (_event, d) => {
          if (d._isDragging) {
            simulation.alphaTarget(0);
          }
          d.fx = null;
          d.fy = null;
          d._isDragging = false;
        }),
      )
      .on("click", (event, d) => {
        event.stopPropagation();
        resetHighlights();
        d3.select(event.target).attr("stroke", "#E91E63").attr("stroke-width", 4);
        link.filter((l) => l.source.id === d.id || l.target.id === d.id)
          .attr("stroke", "#E91E63")
          .attr("stroke-width", 2.5)
          .attr("marker-end", "url(#arrowhead-active)");
        onNodeSelectRef.current?.(d.rawData || null);
      })
      .on("mouseenter", (event, d) => {
        if (selectedNodeIdRef.current !== d.id) {
          d3.select(event.target).attr("stroke", "#333").attr("stroke-width", 3);
        }
      })
      .on("mouseleave", (event, d) => {
        if (selectedNodeIdRef.current !== d.id) {
          d3.select(event.target).attr("stroke", "#fff").attr("stroke-width", 2.5);
        }
      });

    const nodeLabels = nodeGroup.selectAll("text")
      .data(nodes)
      .enter().append("text")
      .text((d) => truncateLabel(d.name))
      .attr("font-size", "11px")
      .attr("fill", "#333")
      .attr("font-weight", "500")
      .attr("dx", 14)
      .attr("dy", 4)
      .style("pointer-events", "none")
      .style("font-family", "system-ui, sans-serif");

    function resetHighlights() {
      node.attr("stroke", "#fff").attr("stroke-width", 2.5);
      link.attr("stroke", "#C0C0C0").attr("stroke-width", 1.5).attr("marker-end", "url(#arrowhead)");
      linkLabelBg.attr("fill", "rgba(255,255,255,0.92)");
      linkLabels.attr("fill", "#666");
    }

    function fitGraphToViewport() {
      if (!nodes.length || !zoomBehaviorRef.current) {
        return;
      }
      const xs = nodes.map((item) => item.x).filter((value) => Number.isFinite(value));
      const ys = nodes.map((item) => item.y).filter((value) => Number.isFinite(value));
      if (!xs.length || !ys.length) {
        return;
      }
      const minX = Math.min(...xs);
      const maxX = Math.max(...xs);
      const minY = Math.min(...ys);
      const maxY = Math.max(...ys);
      const graphWidth = Math.max(1, maxX - minX);
      const graphHeight = Math.max(1, maxY - minY);
      const padding = 72;
      const scale = Math.max(
        0.35,
        Math.min(
          1.35,
          Math.min((width - padding * 2) / graphWidth, (height - padding * 2) / graphHeight),
        ),
      );
      const translateX = width / 2 - ((minX + maxX) / 2) * scale;
      const translateY = height / 2 - ((minY + maxY) / 2) * scale;
      svg.call(
        zoomBehaviorRef.current.transform,
        d3.zoomIdentity.translate(translateX, translateY).scale(scale),
      );
    }

    simulation.on("tick", () => {
      link.attr("d", (d) => getLinkPath(d));

      linkLabels.each(function (d) {
        const mid = getLinkMidpoint(d);
        d3.select(this).attr("x", mid.x).attr("y", mid.y);
      });

      linkLabelBg.each(function (d) {
        const mid = getLinkMidpoint(d);
        const labelText = d.label || "";
        const textWidth = labelText.length * 5.5 + 8;
        d3.select(this)
          .attr("x", mid.x - textWidth / 2)
          .attr("y", mid.y - 7)
          .attr("width", textWidth)
          .attr("height", 14);
      });

      node.attr("cx", (d) => d.x).attr("cy", (d) => d.y);
      nodeLabels.attr("x", (d) => d.x).attr("y", (d) => d.y);
    });

    simulation.on("end", () => {
      fitGraphToViewport();
    });

    window.requestAnimationFrame(() => {
      simulation.tick(80);
      fitGraphToViewport();
    });
  }, [graphData]);

  useEffect(() => {
    renderGraph();
    return () => {
      if (simulationRef.current) {
        simulationRef.current.stop();
        simulationRef.current = null;
      }
    };
  }, [renderGraph]);

  useEffect(() => {
    const svgEl = svgRef.current;
    if (!svgEl) return;
    const svg = d3.select(svgEl);
    const display = showEdgeLabels ? "block" : "none";
    svg.selectAll("text.link-label").style("display", display);
    svg.selectAll("rect.link-label-bg").style("display", display);
  }, [showEdgeLabels]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const observer = new ResizeObserver(() => {
      renderGraph();
    });
    observer.observe(container);
    return () => observer.disconnect();
  }, [renderGraph]);

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
    return <p className="subtle-note">正在生成资产图谱…</p>;
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

  if (!graphData || graphData.nodes.length === 0) {
    return (
      <DataMapEmptyState
        title="暂无资产图谱"
        description="当前场景暂未识别到可视化治理资产。"
      />
    );
  }

  return (
    <section className="lineage-canvas-wrap" aria-label="资产图谱画布">
      {lineageData?.truncated ? (
        <p className="lineage-truncated-note">
          节点过多，已默认展示核心链路，隐藏 {lineageData.hiddenNodeCount || 0} 个节点。
        </p>
      ) : null}
      <div ref={containerRef} className="lineage-d3-container">
        <svg ref={svgRef} className="lineage-d3-svg" />
      </div>
    </section>
  );
}
