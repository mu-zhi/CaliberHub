import { LineageGraphView } from "../datamap/LineageGraphView";
import { UiButton, UiInput } from "../ui";
import { normalizeCandidateGraph, summarizeCandidateGraph } from "../../pages/knowledge-import-utils";

function readText(value) {
  return `${value || ""}`.trim();
}

export function CandidateEntityGraphPanel({
  graph,
  loading = false,
  error = "",
  selectedNodeId = "",
  selectedEdgeId = "",
  mergeTargetCode = "",
  splitLabelsText = "",
  onNodeSelect,
  onEdgeSelect,
  onReview,
  onMergeTargetChange,
  onSplitLabelsChange,
  onRetry,
}) {
  const normalizedGraph = normalizeCandidateGraph(graph);
  const summary = summarizeCandidateGraph(graph);
  const selectedNode = graph?.nodes?.find((item) => readText(item?.nodeCode) === readText(selectedNodeId)) || null;
  const selectedEdge = graph?.edges?.find((item) => readText(item?.edgeCode) === readText(selectedEdgeId)) || null;
  const selectedItem = selectedNode || selectedEdge;
  const selectedType = selectedNode ? "NODE" : (selectedEdge ? "EDGE" : "");
  const selectedCode = selectedNode?.nodeCode || selectedEdge?.edgeCode || "";

  return (
    <section className="panel" aria-label="候选实体图谱">
      <div className="panel-head">
        <div>
          <h2>候选实体图谱</h2>
          <p>复核候选节点和关系，接受后会回写到当前导入批次的候选场景载荷。</p>
        </div>
        <p className="subtle-note">
          待确认节点 {summary.pendingNodes} · 已接受节点 {summary.acceptedNodes} · 待确认关系 {summary.pendingEdges}
        </p>
      </div>

      <div className="import-master-detail" style={{ gridTemplateColumns: "minmax(0, 2fr) minmax(280px, 1fr)" }}>
        <div className="import-scene-detail">
          {loading ? <p className="subtle-note">正在加载候选图谱…</p> : null}
          {error ? (
            <div className="actions" style={{ marginBottom: 12 }}>
              <button className="btn btn-ghost" type="button" onClick={onRetry}>
                重试加载
              </button>
            </div>
          ) : null}
          <LineageGraphView
            sceneNode={{ id: normalizedGraph.rootRef || "candidate-graph" }}
            lineageData={normalizedGraph}
            loading={loading}
            error={error}
            showEdgeLabels
            selectedNodeId={selectedNodeId}
            selectedEdgeId={selectedEdgeId}
            onNodeSelect={onNodeSelect}
            onEdgeSelect={onEdgeSelect}
            onRetry={onRetry}
          />
        </div>

        <aside className="import-scene-queue">
          <div className="panel-head">
            <h2>复核操作</h2>
            <p>选择节点或关系后执行人工确认。</p>
          </div>
          <div className="row form-row">
            <label>当前选中</label>
            <p className="subtle-note">
              {selectedItem ? `${selectedType} · ${selectedCode} · ${readText(selectedItem?.label || selectedItem?.edgeType || selectedItem?.nodeType)}` : "请先在图中选择一个节点或关系"}
            </p>
          </div>
          <div className="row form-row">
            <label htmlFor="candidate-merge-target">合并目标</label>
            <UiInput
              id="candidate-merge-target"
              value={mergeTargetCode}
              onChange={(event) => onMergeTargetChange?.(event.target.value)}
              placeholder="输入要合并到的 nodeCode"
            />
          </div>
          <div className="row form-row">
            <label htmlFor="candidate-split-labels">拆分标签</label>
            <UiInput
              id="candidate-split-labels"
              value={splitLabelsText}
              onChange={(event) => onSplitLabelsChange?.(event.target.value)}
              placeholder="多个标签用逗号分隔"
            />
          </div>
          <div className="actions">
            <UiButton type="button" onClick={() => onReview?.(selectedType, selectedCode, "ACCEPT")} disabled={!selectedCode}>
              接受
            </UiButton>
            <UiButton type="button" onClick={() => onReview?.(selectedType, selectedCode, "REJECT")} disabled={!selectedCode}>
              驳回
            </UiButton>
            <UiButton type="button" onClick={() => onReview?.(selectedType, selectedCode, "MERGE")} disabled={!selectedNodeId || !mergeTargetCode.trim()}>
              合并
            </UiButton>
            <UiButton type="button" onClick={() => onReview?.(selectedType, selectedCode, "SPLIT")} disabled={!selectedNodeId || !splitLabelsText.trim()}>
              拆分
            </UiButton>
          </div>
        </aside>
      </div>
    </section>
  );
}
