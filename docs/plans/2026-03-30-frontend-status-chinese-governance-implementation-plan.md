# 前端状态机中文展示治理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为发布中心、运行决策台和数据地图建立统一的前端状态中文展示层，消除业务界面中的英文状态直出。

**Architecture:** 新增一个共享状态展示模块，统一维护英文状态编码到中文展示名和 `tone` 的映射；页面继续使用英文状态编码做逻辑判断，但所有状态徽标、筛选项、概览卡和详情字段都只从共享模块取中文文案。测试按 `TDD（测试驱动开发，Test-Driven Development）` 先补失败断言，再最小改动页面接入共享映射，最后集中回归。

**Tech Stack:** React 18、Vite、Vitest、Testing Library、React Router

---

## 文件结构与职责

- `frontend/src/components/ui/statusPresentation.js`
  - 新增共享状态展示层，负责状态码归一化、中文展示名和 `tone` 输出。
- `frontend/src/components/ui/statusPresentation.test.js`
  - 新增共享状态展示层单测，覆盖状态域映射和未知状态兜底。
- `frontend/src/components/ui/index.js`
  - 导出共享状态展示 helper，供页面统一复用。
- `frontend/src/pages/PublishCenterPage.jsx`
  - 接入共享状态展示层，替换场景状态、图谱投影状态、版本发布状态和详情字段中的英文直出。
- `frontend/src/pages/PublishCenterPage.test.jsx`
  - 新增发布中心页面级测试，验证已加载数据时状态徽标与详情字段展示中文。
- `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
  - 接入共享状态展示层，替换图谱投影、覆盖状态、策略 / 决策状态和知识包摘要中的英文直出。
- `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`
  - 更新 SSR 结果区测试，断言中文状态展示。
- `frontend/src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx`
  - 更新交互测试，断言页面真实交互后展示中文状态。
- `frontend/src/components/datamap/DataMapContainer.jsx`
  - 接入共享状态展示层，把“资产状态”筛选项和节点详情状态徽标改为中文展示。
- `frontend/src/components/datamap/DataMapContainer.render.test.jsx`
  - 新增 SSR 渲染测试，验证数据地图筛选项展示中文状态标签。
- `docs/engineering/current-delivery-status.md`
  - 在本轮实现完成后同步交付状态真源，记录计划来源、最新完成和下一动作。

---

### Task 1: 建立共享状态展示层

**Files:**
- Create: `frontend/src/components/ui/statusPresentation.js`
- Create: `frontend/src/components/ui/statusPresentation.test.js`
- Modify: `frontend/src/components/ui/index.js`

- [ ] **Step 1: Write the failing test**

```js
import { describe, expect, it } from "vitest";
import {
  DATA_MAP_STATUS_OPTIONS,
  describeCoverageStatus,
  describeDecisionStatus,
  describeProjectionStatus,
  describePublishStatus,
  describeSceneStatus,
} from "./statusPresentation";

describe("statusPresentation", () => {
  it("maps scene statuses to Chinese labels and tones", () => {
    expect(describeSceneStatus("DRAFT")).toMatchObject({ label: "草稿", tone: "warn" });
    expect(describeSceneStatus("PUBLISHED")).toMatchObject({ label: "已发布", tone: "good" });
    expect(describeSceneStatus("DISCARDED")).toMatchObject({ label: "已弃用", tone: "bad" });
  });

  it("maps projection, publish, coverage, and decision statuses to Chinese labels", () => {
    expect(describeProjectionStatus("READY")).toMatchObject({ label: "已就绪", tone: "good" });
    expect(describePublishStatus("ARCHIVED")).toMatchObject({ label: "已归档", tone: "neutral" });
    expect(describeCoverageStatus("FULL")).toMatchObject({ label: "完整覆盖", tone: "good" });
    expect(describeDecisionStatus("clarification_only")).toMatchObject({ label: "需澄清", tone: "warn" });
  });

  it("returns a Chinese fallback for unknown statuses instead of leaking raw English text", () => {
    expect(describeProjectionStatus("MYSTERY_STATUS")).toMatchObject({
      code: "MYSTERY_STATUS",
      label: "未知状态",
      tone: "neutral",
    });
  });

  it("exposes Chinese data map status options while preserving English values", () => {
    expect(DATA_MAP_STATUS_OPTIONS).toEqual([
      { value: "DRAFT", label: "草稿" },
      { value: "REVIEWED", label: "已复核" },
      { value: "PUBLISHED", label: "已发布" },
      { value: "RETIRED", label: "已退役" },
    ]);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- src/components/ui/statusPresentation.test.js`

Expected: FAIL with `Cannot find module './statusPresentation'` or equivalent missing-export error.

- [ ] **Step 3: Write minimal implementation**

Create `frontend/src/components/ui/statusPresentation.js`:

```js
const UNKNOWN_PRESENTATION = {
  code: "UNKNOWN",
  label: "未知状态",
  tone: "neutral",
};

const SCENE_STATUS_MAP = {
  DRAFT: { code: "DRAFT", label: "草稿", tone: "warn" },
  REVIEWED: { code: "REVIEWED", label: "已复核", tone: "good" },
  PUBLISHED: { code: "PUBLISHED", label: "已发布", tone: "good" },
  RETIRED: { code: "RETIRED", label: "已退役", tone: "neutral" },
  DISCARDED: { code: "DISCARDED", label: "已弃用", tone: "bad" },
  REJECTED: { code: "REJECTED", label: "已驳回", tone: "bad" },
};

const PROJECTION_STATUS_MAP = {
  PENDING: { code: "PENDING", label: "待处理", tone: "warn" },
  READY: { code: "READY", label: "已就绪", tone: "good" },
  COMPLETED: { code: "COMPLETED", label: "已完成", tone: "good" },
  FAILED: { code: "FAILED", label: "失败", tone: "bad" },
  RUNNING: { code: "RUNNING", label: "处理中", tone: "warn" },
  CHECKING: { code: "CHECKING", label: "检查中", tone: "warn" },
  PASSED: { code: "PASSED", label: "已通过", tone: "good" },
  BLOCKED: { code: "BLOCKED", label: "已阻断", tone: "bad" },
  SWITCHED: { code: "SWITCHED", label: "已切换", tone: "good" },
  ARCHIVED: { code: "ARCHIVED", label: "已归档", tone: "neutral" },
};

const PUBLISH_STATUS_MAP = {
  PUBLISHED: { code: "PUBLISHED", label: "已发布", tone: "good" },
  PASSED: { code: "PASSED", label: "已通过", tone: "good" },
  BLOCKED: { code: "BLOCKED", label: "已阻断", tone: "bad" },
  SWITCHED: { code: "SWITCHED", label: "已切换", tone: "good" },
  ARCHIVED: { code: "ARCHIVED", label: "已归档", tone: "neutral" },
  FAILED: { code: "FAILED", label: "失败", tone: "bad" },
};

const COVERAGE_STATUS_MAP = {
  FULL: { code: "FULL", label: "完整覆盖", tone: "good" },
  PARTIAL: { code: "PARTIAL", label: "部分覆盖", tone: "warn" },
  GAP: { code: "GAP", label: "存在缺口", tone: "bad" },
  NONE: { code: "NONE", label: "未覆盖", tone: "bad" },
};

const DECISION_STATUS_MAP = {
  ALLOW: { code: "ALLOW", label: "允许", tone: "good" },
  NEED_APPROVAL: { code: "NEED_APPROVAL", label: "需审批", tone: "warn" },
  DENY: { code: "DENY", label: "已拒绝", tone: "bad" },
  NEED_CLARIFICATION: { code: "NEED_CLARIFICATION", label: "需澄清", tone: "warn" },
  CLARIFICATION_ONLY: { code: "CLARIFICATION_ONLY", label: "需澄清", tone: "warn" },
  REJECT: { code: "REJECT", label: "已驳回", tone: "bad" },
  REJECTED: { code: "REJECTED", label: "已驳回", tone: "bad" },
  PENDING_CONFIRMATION: { code: "PENDING_CONFIRMATION", label: "待确认", tone: "warn" },
  ACCEPTED: { code: "ACCEPTED", label: "已接受", tone: "good" },
};

function normalizeStatusCode(status) {
  return `${status || ""}`.trim().toUpperCase();
}

function describeStatus(status, mapping) {
  const code = normalizeStatusCode(status);
  if (!code) {
    return UNKNOWN_PRESENTATION;
  }
  return mapping[code] || {
    code,
    label: "未知状态",
    tone: "neutral",
  };
}

export function describeSceneStatus(status) {
  return describeStatus(status, SCENE_STATUS_MAP);
}

export function describeProjectionStatus(status) {
  return describeStatus(status, PROJECTION_STATUS_MAP);
}

export function describePublishStatus(status) {
  return describeStatus(status, PUBLISH_STATUS_MAP);
}

export function describeCoverageStatus(status) {
  return describeStatus(status, COVERAGE_STATUS_MAP);
}

export function describeDecisionStatus(status) {
  return describeStatus(status, DECISION_STATUS_MAP);
}

export const DATA_MAP_STATUS_OPTIONS = [
  { value: "DRAFT", label: describeSceneStatus("DRAFT").label },
  { value: "REVIEWED", label: describeSceneStatus("REVIEWED").label },
  { value: "PUBLISHED", label: describeSceneStatus("PUBLISHED").label },
  { value: "RETIRED", label: describeSceneStatus("RETIRED").label },
];
```

Modify `frontend/src/components/ui/index.js`:

```js
export { UiBadge } from "./UiBadge";
export { UiButton } from "./UiButton";
export { UiCard } from "./UiCard";
export { UiEmptyState } from "./UiEmptyState";
export { UiInlineError } from "./UiInlineError";
export { UiInput } from "./UiInput";
export { UiSegmentedControl } from "./UiSegmentedControl";
export { UiTextarea } from "./UiTextarea";
export {
  DATA_MAP_STATUS_OPTIONS,
  describeCoverageStatus,
  describeDecisionStatus,
  describeProjectionStatus,
  describePublishStatus,
  describeSceneStatus,
} from "./statusPresentation";
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- src/components/ui/statusPresentation.test.js`

Expected: PASS with `4 passed`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/ui/statusPresentation.js frontend/src/components/ui/statusPresentation.test.js frontend/src/components/ui/index.js
git commit -m "feat: add shared status presentation helpers"
```

### Task 2: 发布中心接入共享状态映射

**Files:**
- Create: `frontend/src/pages/PublishCenterPage.test.jsx`
- Modify: `frontend/src/pages/PublishCenterPage.jsx`

- [ ] **Step 1: Write the failing test**

Create `frontend/src/pages/PublishCenterPage.test.jsx`:

```jsx
// @vitest-environment jsdom
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { PublishCenterPage } from "./PublishCenterPage";

const SCENES = [
  {
    id: 1,
    sceneCode: "SCN_PAYROLL_DRAFT",
    sceneTitle: "代发草稿场景",
    sceneType: "FACT_DETAIL",
    status: "DRAFT",
    updatedAt: "2026-03-30T10:00:00Z",
  },
  {
    id: 2,
    sceneCode: "SCN_PAYROLL_PUBLISHED",
    sceneTitle: "代发已发布场景",
    sceneType: "FACT_DETAIL",
    status: "PUBLISHED",
    updatedAt: "2026-03-30T09:00:00Z",
  },
];

const PUBLISH_CHECK = {
  publishReady: true,
  items: [
    { key: "projection", name: "图谱投影", message: "投影已完成", passed: true },
  ],
};

const VERSIONS = [
  {
    id: 11,
    versionTag: "v2026.03.30",
    publishStatus: "PUBLISHED",
    changeSummary: "完成样板发布",
    publishedAt: "2026-03-30T10:05:00Z",
    snapshotId: 101,
    inferenceSnapshotId: 201,
  },
];

const PROJECTION = {
  status: "READY",
  message: "图谱投影已就绪",
  lastProjectedAt: "2026-03-30T10:06:00Z",
};

function mockJsonResponse(payload) {
  return {
    ok: true,
    status: 200,
    headers: { get: () => "" },
    json: async () => payload,
  };
}

function buildFetchMock() {
  return vi.fn(async (url) => {
    const path = typeof url === "string" ? url : url.toString();
    if (path.includes("/api/scenes")) return mockJsonResponse(SCENES);
    if (path.includes("/api/publish-checks")) return mockJsonResponse(PUBLISH_CHECK);
    if (path.includes("/versions")) return mockJsonResponse(VERSIONS);
    if (path.includes("/api/graphrag/projection")) return mockJsonResponse(PROJECTION);
    if (path.includes("/api/plans")) return mockJsonResponse([]);
    if (path.includes("/api/coverage-declarations")) return mockJsonResponse([]);
    if (path.includes("/api/policies")) return mockJsonResponse([]);
    if (path.includes("/api/contract-views")) return mockJsonResponse([]);
    if (path.includes("/api/source-contracts")) return mockJsonResponse([]);
    if (path.includes("/api/output-contracts")) return mockJsonResponse([]);
    if (path.includes("/api/input-slot-schemas")) return mockJsonResponse([]);
    return mockJsonResponse(null);
  });
}

let originalFetch;

beforeEach(() => {
  originalFetch = globalThis.fetch;
  vi.stubGlobal("fetch", buildFetchMock());
});

afterEach(() => {
  globalThis.fetch = originalFetch;
  vi.restoreAllMocks();
  cleanup();
});

describe("PublishCenterPage status presentation", () => {
  it("renders Chinese labels for scene, projection, and version statuses", async () => {
    render(
      <MemoryRouter>
        <PublishCenterPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText("发布中心")).toBeTruthy();
    });

    await waitFor(() => {
      expect(screen.getByText("代发草稿场景")).toBeTruthy();
    });

    expect(screen.getByText("草稿")).toBeTruthy();
    expect(screen.getAllByText("已发布").length).toBeGreaterThan(0);
    expect(screen.getByText("已就绪")).toBeTruthy();
    expect(screen.queryByText("DRAFT")).toBeNull();
    expect(screen.queryByText("PUBLISHED")).toBeNull();
    expect(screen.queryByText("READY")).toBeNull();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- src/pages/PublishCenterPage.test.jsx`

Expected: FAIL because the page still renders `DRAFT` / `PUBLISHED` / `READY`.

- [ ] **Step 3: Write minimal implementation**

Modify the import in `frontend/src/pages/PublishCenterPage.jsx`:

```jsx
import {
  UiBadge,
  UiButton,
  UiCard,
  UiEmptyState,
  UiInlineError,
  UiInput,
  describeProjectionStatus,
  describePublishStatus,
  describeSceneStatus,
} from "../components/ui";
```

Replace the status badge and detail rendering in `frontend/src/pages/PublishCenterPage.jsx`:

```jsx
            {scenes.map((item) => {
              const sceneStatus = describeSceneStatus(item.status);
              return (
                <button
                  key={item.id}
                  type="button"
                  className={`publish-center-scene-item ${selectedSceneId === `${item.id}` ? "is-active" : ""}`}
                  onClick={() => handleSceneChange(`${item.id}`)}
                >
                  <div>
                    <strong>{item.sceneTitle || `场景 #${item.id}`}</strong>
                    <p>{item.domainName || item.domain || "未绑定业务领域"} · {item.sceneType || "未声明类型"}</p>
                  </div>
                  <div className="workbench-row-side">
                    <UiBadge tone={sceneStatus.tone}>{sceneStatus.label}</UiBadge>
                    <span>{formatDateTime(item.updatedAt || item.createdAt)}</span>
                  </div>
                </button>
              );
            })}
```

```jsx
            {selectedScene ? (
              <>
                {(() => {
                  const selectedSceneStatus = describeSceneStatus(selectedScene.status);
                  const projectionStatus = bundle.projection?.status
                    ? describeProjectionStatus(bundle.projection.status)
                    : { label: "未投影", tone: "neutral" };
                  return (
                    <>
                      <div className="proto-card-head">
                        <div>
                          <h3>当前场景</h3>
                          <p className="subtle-note">发布门禁、快照和投影状态放在一屏里看完。</p>
                        </div>
                        <UiBadge tone={selectedSceneStatus.tone}>{selectedSceneStatus.label}</UiBadge>
                      </div>
                      <dl className="knowledge-package-kv publish-center-kv">
                        <div><dt>场景名称</dt><dd>{selectedScene.sceneTitle}</dd></div>
                        <div><dt>业务领域</dt><dd>{selectedScene.domainName || selectedScene.domain || "未绑定"}</dd></div>
                        <div><dt>场景类型</dt><dd>{selectedScene.sceneType || "未设置"}</dd></div>
                        <div><dt>最近更新时间</dt><dd>{formatDateTime(selectedScene.updatedAt || selectedScene.createdAt)}</dd></div>
                        <div><dt>最新快照</dt><dd>{bundle.versions[0]?.versionTag || "未生成"}</dd></div>
                        <div><dt>图谱投影</dt><dd>{projectionStatus.label}</dd></div>
                      </dl>
                    </>
                  );
                })()}
              </>
            ) : (
              <p className="subtle-note">请选择左侧场景。</p>
            )}
```

```jsx
              <article className="workbench-list-row">
                <div>
                  <strong>图谱投影状态</strong>
                  <p>{bundle.projection?.message || "尚未生成投影或尚未返回状态消息"}</p>
                </div>
                <div className="workbench-row-side">
                  {(() => {
                    const projectionStatus = bundle.projection?.status
                      ? describeProjectionStatus(bundle.projection.status)
                      : { label: "未投影", tone: "neutral" };
                    return (
                      <UiBadge tone={projectionStatus.tone}>{projectionStatus.label}</UiBadge>
                    );
                  })()}
                  <span>{formatDateTime(bundle.projection?.lastProjectedAt || bundle.projection?.updatedAt)}</span>
                </div>
              </article>
              {bundle.versions.slice(0, 4).map((item) => {
                const publishStatus = describePublishStatus(item.publishStatus || "PUBLISHED");
                return (
                  <article key={item.id} className="workbench-list-row">
                    <div>
                      <strong>{item.versionTag || `快照 #${item.id}`}</strong>
                      <p>{item.changeSummary || "未填写变更摘要"}</p>
                    </div>
                    <div className="workbench-row-side">
                      <UiBadge tone={publishStatus.tone}>{publishStatus.label}</UiBadge>
                      <span>{formatDateTime(item.publishedAt || item.createdAt)}</span>
                    </div>
                  </article>
                );
              })}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- src/pages/PublishCenterPage.test.jsx`

Expected: PASS with `1 passed`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/PublishCenterPage.jsx frontend/src/pages/PublishCenterPage.test.jsx
git commit -m "feat: localize publish center status presentation"
```

### Task 3: 运行决策与知识包页面接入共享状态映射

**Files:**
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx`

- [ ] **Step 1: Write the failing test**

Modify `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`:

```jsx
describe("knowledge package normal result rendering", () => {
  it("scenario 3: renders Chinese decision and coverage labels", () => {
    const html = renderResultSection(NORMAL_RESULT, PIPELINE_FIXTURE);

    expect(html).toContain("知识包摘要");
    expect(html).toContain("允许");
    expect(html).toContain("完整覆盖");
    expect(html).not.toContain("allow");
    expect(html).not.toContain("FULL");
  });
});

describe("knowledge package clarification result rendering", () => {
  it("scenario 4: renders Chinese clarification decision label", () => {
    const html = renderResultSection(CLARIFICATION_RESULT, PIPELINE_FIXTURE);

    expect(html).toContain("需澄清");
    expect(html).not.toContain("need_clarification");
  });
});

describe("knowledge package deny result rendering", () => {
  it("scenario 6: renders Chinese deny decision label", () => {
    const html = renderResultSection(DENY_RESULT, PIPELINE_FIXTURE);

    expect(html).toContain("已拒绝");
    expect(html).not.toContain("deny");
  });
});
```

Modify `frontend/src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx`:

```jsx
  it("submits query and renders Chinese status labels", async () => {
    renderWorkbench(ALLOW_RESULT);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "生成知识包" })).toBeTruthy();
    });

    const identifierInput = screen.getAllByPlaceholderText("请输入协议号或客户号")[0];
    await act(async () => {
      fireEvent.change(identifierInput, { target: { value: "P001" } });
    });

    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "生成知识包" }));
    });

    await waitFor(() => {
      expect(screen.getByText("知识包摘要")).toBeTruthy();
    });

    expect(screen.getAllByText("允许").length).toBeGreaterThan(0);
    expect(screen.getByText("完整覆盖")).toBeTruthy();
    expect(screen.getByText("已就绪")).toBeTruthy();
    expect(screen.queryByText("allow")).toBeNull();
    expect(screen.queryByText("FULL")).toBeNull();
    expect(screen.queryByText("READY")).toBeNull();
  });
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx`

Expected: FAIL because the page and the SSR helper still output `allow` / `need_clarification` / `FULL` / `READY`.

- [ ] **Step 3: Write minimal implementation**

Modify the import in `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`:

```jsx
import {
  UiBadge,
  UiButton,
  UiCard,
  UiEmptyState,
  UiInlineError,
  UiInput,
  UiTextarea,
  describeCoverageStatus,
  describeDecisionStatus,
  describeProjectionStatus,
} from "../components/ui";
```

Replace the page-level status rendering in `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`:

```jsx
      <div className="workbench-metric-strip">
        <UiCard className="workbench-metric-card"><span>已发布样板</span><strong>{scenes.length}</strong><small>当前只展示可运行场景</small></UiCard>
        <UiCard className="workbench-metric-card"><span>已声明方案</span><strong>{sceneBundle.plans.length}</strong><small>来自真实方案资产</small></UiCard>
        <UiCard className="workbench-metric-card"><span>覆盖分段</span><strong>{sceneBundle.coverages.length}</strong><small>用于时段命中判断</small></UiCard>
        <UiCard className="workbench-metric-card">
          <span>图谱投影</span>
          <strong>{sceneBundle.projection?.status ? describeProjectionStatus(sceneBundle.projection.status).label : "未投影"}</strong>
          <small>{formatDateTime(sceneBundle.projection?.lastProjectedAt)}</small>
        </UiCard>
      </div>
```

```jsx
              <UiBadge tone={sceneBundle.projection?.status ? describeProjectionStatus(sceneBundle.projection.status).tone : "neutral"}>
                {sceneBundle.projection?.status ? describeProjectionStatus(sceneBundle.projection.status).label : "未投影"}
              </UiBadge>
```

```jsx
              {sceneBundle.coverages.map((item) => {
                const coverageStatus = describeCoverageStatus(item.coverageStatus);
                return (
                  <article key={item.id || item.coverageCode} className="knowledge-package-coverage-item">
                    <div>
                      <strong>{item.coverageTitle || item.coverageCode}</strong>
                      <p>{item.applicablePeriod || item.statementText || "未补充说明"}</p>
                    </div>
                    <UiBadge tone={coverageStatus.tone}>{coverageStatus.label}</UiBadge>
                  </article>
                );
              })}
```

```jsx
                {pipeline.planSelect?.candidates?.slice(0, 3).map((item) => {
                  const decisionStatus = describeDecisionStatus(item.decision);
                  return (
                    <article key={`${item.sceneId}-${item.planId}`} className="knowledge-package-list-item">
                      <div>
                        <strong>{item.planName || item.planCode}</strong>
                        <p>{item.sceneTitle || item.sceneCode} · {item.sourceTables?.join("、") || "未声明来源表"}</p>
                      </div>
                      <UiBadge tone={decisionStatus.tone}>{decisionStatus.label}</UiBadge>
                    </article>
                  );
                })}
```

```jsx
                    <UiBadge tone={describeDecisionStatus(result.decision).tone}>
                      {describeDecisionStatus(result.decision).label}
                    </UiBadge>
```

```jsx
                    <UiBadge tone={describeDecisionStatus(result.decision).tone}>
                      {describeDecisionStatus(result.decision).label}
                    </UiBadge>
```

```jsx
                        <strong>覆盖判定</strong>
                        <p>{result.coverage?.status ? describeCoverageStatus(result.coverage.status).label : "未知状态"} · {result.coverage?.matchedSegment || "未命中分段"}</p>
```

```jsx
                        <strong>策略结果</strong>
                        <p>{result.policy?.decision ? describeDecisionStatus(result.policy.decision).label : describeDecisionStatus(result.decision).label} · 风险等级 {result.risk?.riskLevel || "未知"}</p>
```

Modify `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx` to mirror the page-level helper usage:

```jsx
import {
  describeCoverageStatus,
  describeDecisionStatus,
} from "../components/ui/statusPresentation";

function renderResultSection(result, pipeline) {
  const tree = (
    <MemoryRouter>
      <div>
        {result?.clarification ? (
          <UiCard className="knowledge-package-card" elevation="card">
            <div className="knowledge-package-card-head">
              <div>
                <h3>需要补充条件</h3>
                <p className="subtle-note">当前请求已命中跨场景多意图，系统返回知识拆解结果而不是伪造混合知识包。</p>
              </div>
              <UiBadge tone={describeDecisionStatus(result.decision).tone}>{describeDecisionStatus(result.decision).label}</UiBadge>
            </div>
            <p>{result.clarification.summary || "请先拆分子问题后再检索。"}</p>
          </UiCard>
        ) : result ? (
          <UiCard className="knowledge-package-card" elevation="card">
            <div className="knowledge-package-card-head">
              <div>
                <h3>知识包摘要</h3>
              </div>
              <UiBadge tone={describeDecisionStatus(result.decision).tone}>{describeDecisionStatus(result.decision).label}</UiBadge>
            </div>
            <div className="knowledge-package-summary-grid">
              <div className="knowledge-package-summary-item">
                <div>
                  <strong>覆盖判定</strong>
                  <p>{result.coverage?.status ? describeCoverageStatus(result.coverage.status).label : "未知状态"} · {result.coverage?.matchedSegment || "未命中分段"}</p>
                </div>
              </div>
            </div>
          </UiCard>
        ) : null}
      </div>
    </MemoryRouter>
  );
  return renderToString(tree);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx`

Expected: PASS with all scenarios green and no English status labels in the asserted output.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/KnowledgePackageWorkbenchPage.jsx frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx frontend/src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx
git commit -m "feat: localize runtime knowledge package statuses"
```

### Task 4: 数据地图筛选项与节点状态接入共享映射

**Files:**
- Create: `frontend/src/components/datamap/DataMapContainer.render.test.jsx`
- Modify: `frontend/src/components/datamap/DataMapContainer.jsx`

- [ ] **Step 1: Write the failing test**

Create `frontend/src/components/datamap/DataMapContainer.render.test.jsx`:

```jsx
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderToString } from "react-dom/server";
import { MemoryRouter } from "react-router-dom";
import { DataMapContainer } from "./DataMapContainer";

function renderPage() {
  return renderToString(
    <MemoryRouter>
      <DataMapContainer viewPreset="map" />
    </MemoryRouter>,
  );
}

let consoleErrorSpy;

beforeEach(() => {
  const originalError = console.error;
  consoleErrorSpy = vi.spyOn(console, "error").mockImplementation((...args) => {
    const [firstArg] = args;
    if (typeof firstArg === "string" && firstArg.includes("useLayoutEffect does nothing on the server")) {
      return;
    }
    originalError(...args);
  });
});

afterEach(() => {
  consoleErrorSpy?.mockRestore();
});

describe("DataMapContainer status presentation", () => {
  it("renders Chinese filter labels for scene statuses", () => {
    const html = renderPage();

    expect(html).toContain("草稿");
    expect(html).toContain("已复核");
    expect(html).toContain("已发布");
    expect(html).toContain("已退役");
    expect(html).not.toContain(">DRAFT<");
    expect(html).not.toContain(">REVIEWED<");
    expect(html).not.toContain(">PUBLISHED<");
    expect(html).not.toContain(">RETIRED<");
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- src/components/datamap/DataMapContainer.render.test.jsx`

Expected: FAIL because the filter chips still render `DRAFT / REVIEWED / PUBLISHED / RETIRED`.

- [ ] **Step 3: Write minimal implementation**

Modify the import in `frontend/src/components/datamap/DataMapContainer.jsx`:

```jsx
import {
  UiButton,
  UiInlineError,
  UiInput,
  UiSegmentedControl,
} from "../ui";
import {
  DATA_MAP_STATUS_OPTIONS,
  describeSceneStatus,
} from "../ui/statusPresentation";
```

Replace the filter options in `frontend/src/components/datamap/DataMapContainer.jsx`:

```jsx
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
```

Replace the filter chip rendering in `frontend/src/components/datamap/DataMapContainer.jsx`:

```jsx
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
```

Replace the selected-node status chip in `frontend/src/components/datamap/DataMapContainer.jsx`:

```jsx
                    <span className="datamap-chip" style={{ "--chip-color": nodeTypeColor(selectedGraphNode.objectType) }}>
                      {selectedGraphNode.status ? describeSceneStatus(selectedGraphNode.status).label : "未知状态"}
                    </span>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- src/components/datamap/DataMapContainer.render.test.jsx`

Expected: PASS with `1 passed`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/datamap/DataMapContainer.jsx frontend/src/components/datamap/DataMapContainer.render.test.jsx
git commit -m "feat: localize data map status labels"
```

### Task 5: 全量回归与交付状态同步

**Files:**
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: Run the targeted frontend regression suite**

Run:

```bash
cd frontend && npm test -- \
  src/components/ui/statusPresentation.test.js \
  src/pages/PublishCenterPage.test.jsx \
  src/pages/KnowledgePackageWorkbenchPage.test.jsx \
  src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx \
  src/components/datamap/DataMapContainer.render.test.jsx
```

Expected: PASS with all targeted tests green.

- [ ] **Step 2: Run the frontend production build**

Run: `cd frontend && npm run build`

Expected: PASS with Vite build success; existing chunk-size warning can remain if no new error is introduced.

- [ ] **Step 3: Sync delivery status source of truth**

Append or update the implementation row in `docs/engineering/current-delivery-status.md` with this exact content:

```md
| 前端状态机中文展示治理（发布中心 / 运行决策台 / 数据地图） | 运行与治理工作台已上线功能口径收口 | [前端状态机中文展示治理设计稿](../plans/2026-03-30-frontend-status-chinese-governance-design.md)、[发布检查、灰度发布与回滚](../architecture/features/iteration-02-runtime-and-governance/07-发布检查、灰度发布与回滚.md)、[运行决策与知识包生成](../architecture/features/iteration-02-runtime-and-governance/08-运行决策与知识包生成.md)、[数据地图浏览与覆盖追踪](../architecture/features/iteration-02-runtime-and-governance/10-数据地图浏览与覆盖追踪.md) | [2026-03-30-frontend-status-chinese-governance-implementation-plan.md](../plans/2026-03-30-frontend-status-chinese-governance-implementation-plan.md) | 纯前端回归，豁免独立测试文档 | `reviewing（测试与评审中）` | 已新增共享状态映射层，并把发布中心、运行决策台和数据地图中的状态徽标、筛选项、概览卡和详情字段统一切到中文展示；定向 Vitest 与 `frontend build` 已通过。 | 进入 `code-reviewing（代码检视技能）`，核对共享映射是否覆盖全部首批状态域，并检查未知状态兜底是否稳定。 | 业务界面不再直接暴露英文状态值，且三页的定向测试和构建均通过。 | 无 | Codex（实现） | 2026-03-30 |
```

- [ ] **Step 4: Re-run a lightweight status-specific grep check**

Run:

```bash
rg -n '>{0,1}(DRAFT|REVIEWED|PUBLISHED|RETIRED|READY|FULL|allow|need_clarification|deny)(<|$)' \
  frontend/src/pages/PublishCenterPage.jsx \
  frontend/src/pages/KnowledgePackageWorkbenchPage.jsx \
  frontend/src/components/datamap/DataMapContainer.jsx
```

Expected: no matches in user-facing JSX text nodes; remaining matches are only internal logic comparisons.

- [ ] **Step 5: Commit**

```bash
git add docs/engineering/current-delivery-status.md
git commit -m "docs: sync status after Chinese status governance"
```

