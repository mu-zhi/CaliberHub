# 导入中活图谱 SSE/EventSource 回归 v2 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让知识生产台的导入流程在 `SSE（服务端事件流，Server-Sent Events）` 过程中稳定消费 `graph_patch（图谱增量补丁）`，并用浏览器级回归锁住“活图谱实时更新 + 选中态不丢失 + 完成后再收敛”的交互。

**Architecture:** 后端 `/api/import/preprocess-stream` 与前端 `apiSseRequest` 已存在，本轮不改协议，只补前端对 `start` / `stage` / `graph_patch` / `draft` / `done` / `error` 的状态折叠。`KnowledgePage` 负责接收流式事件并把补丁合并进当前候选图谱，`knowledge-import-utils.js` 负责提供可测试的补丁归并函数，浏览器级测试负责验证补丁到达时图谱可见、选中节点/边保持稳定、最终完成态仍能回到完整图谱。

**Tech Stack:** React 18、Vitest、React Testing Library、`apiSseRequest`、现有 `CandidateEntityGraphPanel` 与导入页状态机。

---

### Task 1: 为候选图谱补丁写失败测试并补归并工具

**Files:**
- Modify: `frontend/src/pages/knowledge-import-utils.js`
- Modify: `frontend/src/pages/knowledge-import-utils.test.js`

- [ ] **Step 1: 写失败测试**

```javascript
import { describe, expect, it } from "vitest";
import { mergeCandidateGraphPatch } from "./knowledge-import-utils";

describe("mergeCandidateGraphPatch", () => {
  it("merges graph_patch nodes and edges without dropping existing items", () => {
    const current = {
      graphId: "task-a:material-a",
      nodes: [
        { id: "SC-001", objectType: "CANDIDATE_SCENE", objectCode: "SC-001", objectName: "代发明细查询" },
      ],
      edges: [
        { id: "EDGE-001", relationType: "SCENE_HAS_PLAN", source: "SC-001", target: "PLN-001", label: "候选方案" },
      ],
    };
    const patch = {
      graphId: "task-a:material-a",
      patchSeq: 1,
      addedNodes: [
        { nodeCode: "PLN-002", nodeType: "CANDIDATE_PLAN", label: "历史补查" },
      ],
      updatedNodes: [
        { nodeCode: "SC-001", nodeType: "CANDIDATE_SCENE", label: "代发明细查询（已更新）" },
      ],
      addedEdges: [
        { edgeCode: "EDGE-002", edgeType: "SCENE_HAS_PLAN", sourceNodeCode: "SC-001", targetNodeCode: "PLN-002", label: "候选方案" },
      ],
      focusNodeIds: ["PLN-002"],
      summary: "新增 1 个方案补丁",
    };

    const next = mergeCandidateGraphPatch(current, patch);

    expect(next.nodes.map((item) => item.id)).toEqual(["SC-001", "PLN-002"]);
    expect(next.nodes.find((item) => item.id === "SC-001").objectName).toBe("代发明细查询（已更新）");
    expect(next.edges.map((item) => item.id)).toEqual(["EDGE-001", "EDGE-002"]);
    expect(next.focusNodeIds).toEqual(["PLN-002"]);
    expect(next.summary).toBe("新增 1 个方案补丁");
  });
});
```

- [ ] **Step 2: 运行测试确认先失败**

Run: `cd frontend && npm test -- src/pages/knowledge-import-utils.test.js -t "mergeCandidateGraphPatch" -v`
Expected: FAIL，提示 `mergeCandidateGraphPatch` 未定义或断言不通过。

- [ ] **Step 3: 写最小实现**

```javascript
function readArray(value) {
  return Array.isArray(value) ? value : [];
}

function readText(value) {
  return `${value || ""}`.trim();
}

function mergeById(existingItems, incomingItems, readId, mapItem) {
  const next = new Map(existingItems.map((item) => [readId(item), item]));
  incomingItems.forEach((item) => {
    const id = readId(item);
    if (!id) {
      return;
    }
    next.set(id, {
      ...(next.get(id) || {}),
      ...mapItem(item),
    });
  });
  return Array.from(next.values());
}

export function mergeCandidateGraphPatch(currentGraph, patch) {
  const current = currentGraph || {};
  const addedNodes = readArray(patch?.addedNodes);
  const updatedNodes = readArray(patch?.updatedNodes);
  const addedEdges = readArray(patch?.addedEdges);
  const updatedEdges = readArray(patch?.updatedEdges);

  const nextNodes = mergeById(
    readArray(current.nodes),
    [...addedNodes, ...updatedNodes],
    (item) => readText(item?.id),
    (item) => ({
      id: readText(item?.nodeCode || item?.id),
      objectType: readText(item?.nodeType || item?.objectType).toUpperCase(),
      objectCode: readText(item?.nodeCode || item?.objectCode || item?.id),
      objectName: readText(item?.label || item?.objectName || item?.nodeCode || item?.id),
      status: readText(item?.reviewStatus || item?.status),
      summaryText: readText(item?.summaryText),
      meta: {
        riskLevel: readText(item?.riskLevel),
        sceneCandidateCode: readText(item?.sceneCandidateCode),
        confidenceScore: Number(item?.confidenceScore || 0),
      },
    }),
  );

  const nextEdges = mergeById(
    readArray(current.edges),
    [...addedEdges, ...updatedEdges],
    (item) => readText(item?.id),
    (item) => ({
      id: readText(item?.edgeCode || item?.id),
      relationType: readText(item?.edgeType || item?.relationType).toUpperCase(),
      source: readText(item?.sourceNodeCode || item?.source),
      target: readText(item?.targetNodeCode || item?.target),
      label: readText(item?.label || item?.edgeType || item?.relationType),
      confidence: Number(item?.confidenceScore || 0),
      meta: {
        reviewStatus: readText(item?.reviewStatus || item?.status),
        sceneCandidateCode: readText(item?.sceneCandidateCode),
        riskLevel: readText(item?.riskLevel),
      },
    }),
  );

  return {
    ...current,
    graphId: readText(patch?.graphId || current.graphId),
    nodes: nextNodes,
    edges: nextEdges,
    focusNodeIds: readArray(patch?.focusNodeIds).map((item) => readText(item)).filter(Boolean),
    summary: readText(patch?.summary || current.summary),
    patchSeq: Number(patch?.patchSeq || current.patchSeq || 0),
  };
}
```

- [ ] **Step 4: 运行测试确认转绿**

Run: `cd frontend && npm test -- src/pages/knowledge-import-utils.test.js -t "mergeCandidateGraphPatch" -v`
Expected: PASS，且没有额外失败用例。

- [ ] **Step 5: 提交**

```bash
git add frontend/src/pages/knowledge-import-utils.js frontend/src/pages/knowledge-import-utils.test.js
git commit -m "test: cover candidate graph patch merge"
```

**问题清单**
- `graph_patch` 的节点 / 边主键如果和当前归一化字段不一致，合并会丢焦点。
- 需要确认补丁只做增量合并，不做硬删除。

**风险**
- 如果上游补丁字段命名漂移，合并函数会退化为“能渲染但不够精确”的状态。

### Task 2: 把 graph_patch 接到 KnowledgePage 的流式状态机

**Files:**
- Modify: `frontend/src/pages/KnowledgePage.jsx`
- Modify: `frontend/src/pages/knowledge-import-utils.js`
- Modify: `frontend/src/pages/knowledge-import-utils.test.js`

- [ ] **Step 1: 写失败测试**

```javascript
// @vitest-environment jsdom
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { KnowledgePage } from "./KnowledgePage";
import { apiRequest, apiSseRequest } from "../api/client";

vi.mock("../api/client", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    apiRequest: vi.fn(),
    apiSseRequest: vi.fn(),
  };
});

vi.mock("../components/knowledge/CandidateEntityGraphPanel", () => ({
  CandidateEntityGraphPanel: ({ graph }) => (
    <div data-testid="candidate-graph-panel">
      graph:{graph?.nodes?.length || 0}/{graph?.edges?.length || 0}
    </div>
  ),
}));

it("renders live graph_patch updates before done and keeps the graph panel visible", async () => {
  apiSseRequest.mockImplementation(async (_path, options) => {
    options.onEvent({ event: "start", data: { taskId: "task-live-1" } });
    options.onEvent({ event: "graph_patch", data: {
      graphId: "task-live-1:material-live-1",
      patchSeq: 1,
      addedNodes: [{ nodeCode: "SC-001", nodeType: "CANDIDATE_SCENE", label: "代发明细查询" }],
      addedEdges: [],
      focusNodeIds: ["SC-001"],
      summary: "首批补丁",
    }});
    options.onEvent({ event: "draft", data: { sceneIndex: 0, sceneTitle: "代发明细查询", status: "DRAFT" } });
    return { importBatchId: "task-live-1", materialId: "material-live-1", scenes: [] };
  });

  apiRequest.mockResolvedValue([]);

  render(<MemoryRouter><KnowledgePage preset="import" /></MemoryRouter>);

  await waitFor(() => {
    expect(screen.getByTestId("candidate-graph-panel")).toHaveTextContent("graph:1/0");
  });
});
```

- [ ] **Step 2: 运行测试确认先失败**

Run: `cd frontend && npm test -- src/pages/KnowledgePage.test.jsx -t "live graph_patch" -v`
Expected: FAIL，页面还没有把 `graph_patch` 合并进候选图谱状态。

- [ ] **Step 3: 写最小实现**

```javascript
// 在 KnowledgePage.jsx 内增加 graph_patch 处理
if (event.event === "graph_patch") {
  const detail = event.data || {};
  setCandidateGraph((current) => mergeCandidateGraphPatch(current, detail));
  setCandidateGraphError("");
}

// 只在导入流结束后再做一次最终图谱回读，避免 RUNNING 阶段的空查把 live preview 覆盖掉
useEffect(() => {
  if (preset !== "import") {
    return;
  }
  if (!importBatchId) {
    return;
  }
  if (importTaskStatus === "RUNNING") {
    return;
  }
  loadCandidateGraph(importBatchId);
}, [importBatchId, importTaskStatus, loadCandidateGraph, preset]);
```

- [ ] **Step 4: 运行测试确认转绿**

Run: `cd frontend && npm test -- src/pages/KnowledgePage.test.jsx -t "live graph_patch" -v`
Expected: PASS，补丁先出现，最终图谱回读不覆盖 live preview。

- [ ] **Step 5: 提交**

```bash
git add frontend/src/pages/KnowledgePage.jsx frontend/src/pages/knowledge-import-utils.js frontend/src/pages/KnowledgePage.test.jsx
git commit -m "feat: stream live import graph patches"
```

**问题清单**
- 当前导入页在 `start` 后会尽早回读候选图谱，可能和 live preview 竞争。
- `RUNNING` 状态下的回读要避免把补丁图谱误判为错误态。

**风险**
- 如果后端最终没有产出 `graph_patch`，页面仍会依赖完成后的完整图谱回读，live preview 会退化但不会阻断导入。

### Task 3: 补浏览器级回归，锁住补丁展示与选中态稳定

**Files:**
- Modify: `frontend/src/pages/KnowledgePage.test.jsx`
- Modify: `frontend/src/pages/KnowledgePage.render.test.jsx`（如需补 SSR 兜底）
- Modify: `frontend/src/pages/knowledge-import-utils.test.js`

- [ ] **Step 1: 写失败测试**

```javascript
it("keeps the selected node stable when later graph_patch events add more graph data", async () => {
  // 先返回一个图谱补丁，随后再补充更多节点
  // 断言：首次选中节点保留，图谱节点数增长，未丢失当前选中态
});
```

- [ ] **Step 2: 运行测试确认先失败**

Run: `cd frontend && npm test -- src/pages/KnowledgePage.test.jsx -t "selected node stable" -v`
Expected: FAIL，当前还没有“补丁后保留选中态”的回归保护。

- [ ] **Step 3: 写最小实现**

```javascript
function mergeGraphSelection(currentSelection, patch, graph) {
  const nextSelectedNodeId = currentSelection.nodeId
    && graph?.nodes?.some((node) => node.id === currentSelection.nodeId)
      ? currentSelection.nodeId
      : `${readArray(patch?.focusNodeIds)[0] || ""}`.trim();
  const nextSelectedEdgeId = currentSelection.edgeId
    && graph?.edges?.some((edge) => edge.id === currentSelection.edgeId)
      ? currentSelection.edgeId
      : "";
  return {
    selectedCandidateNodeId: nextSelectedNodeId,
    selectedCandidateEdgeId: nextSelectedEdgeId,
  };
}
```

- [ ] **Step 4: 运行测试确认转绿**

Run: `cd frontend && npm test -- src/pages/KnowledgePage.test.jsx -t "selected node stable" -v && npm test -- src/pages/knowledge-import-utils.test.js -v`
Expected: PASS，且现有导入页状态本地化用例不回归。

- [ ] **Step 5: 提交**

```bash
git add frontend/src/pages/KnowledgePage.test.jsx frontend/src/pages/KnowledgePage.render.test.jsx frontend/src/pages/knowledge-import-utils.test.js
git commit -m "test: lock live import graph browser regression"
```

**问题清单**
- 浏览器级测试需要稳定的异步事件顺序，mock 里必须明确 `start -> graph_patch -> draft -> done`。
- 如果图谱面板仍被 mock 成静态文本，测试只能验证状态传播，不能验证真实渲染细节。

**风险**
- 测试过度依赖具体事件顺序时，后端后续重排 `stage` / `draft` 事件可能造成脆弱性。

### Task 4: 同步交付状态与验收命令

**Files:**
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 写失败检查**

```bash
rg -n "导入中活图谱|graph_patch|SSE（服务端事件流|EventSource（浏览器事件流）" docs/engineering/current-delivery-status.md
```

Expected: 先命中当前风险与新任务状态；如果没有命中，说明需要把本次实现写回交付状态真源。

- [ ] **Step 2: 写最小同步**

```markdown
| 导入中活图谱 SSE/EventSource 回归 v2 | 知识生产台 / 导入流式交互 | [解析抽取与证据确认](../architecture/features/iteration-01-knowledge-production/02-解析抽取与证据确认.md) | [2026-03-31-import-live-sse-regression-v2-implementation-plan.md](../plans/2026-03-31-import-live-sse-regression-v2-implementation-plan.md) | [import-live-sse-regression-v2-test-report.md](../testing/features/iteration-01-knowledge-production/import-live-sse-regression-v2-test-report.md) | `implementing（实现中）` | 已确认 `/api/import/preprocess-stream` 与前端 `apiSseRequest` 基线可用，当前工作仅补 `graph_patch` 合并与浏览器级回归。 | 等待前端补丁合并与回归测试通过后回写完成态。 | SSE 流中的活图谱补丁可稳定渲染，完成后再收敛到完整候选图谱。 | 如果上游没有产出 `graph_patch`，仅保留完成态回读。 | Codex（实现） | 2026-03-31 |
```

- [ ] **Step 3: 运行验收命令**

Run:

```bash
cd frontend && npm test -- src/pages/knowledge-import-utils.test.js src/pages/KnowledgePage.test.jsx
cd frontend && npm run build
```

Expected:
- `vitest` 通过，包含 `mergeCandidateGraphPatch` 与 live graph 回归用例。
- `vite build` 通过，没有语法或打包错误。

- [ ] **Step 4: 提交**

```bash
git add docs/engineering/current-delivery-status.md
git commit -m "docs: record live import graph regression work"
```

**问题清单**
- 交付状态表需要和本次真实实现保持同一口径，不能只写计划不写结果。
- 验收命令必须和前端实际测试文件名一致，否则会产生“文档通过、实际没跑”的假阳性。

**风险**
- 如果后续把 SSE 补丁改成别的事件名，交付状态和测试用例必须同步改写，避免多口径。
