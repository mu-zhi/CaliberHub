import { describe, expect, it } from "vitest";
import {
  deriveKnowledgeTaskMainlineState,
  shouldShowDataMapLink,
  shouldShowRuntimeLink,
} from "./knowledge-task-mainline";

describe("deriveKnowledgeTaskMainlineState", () => {
  it("returns waiting_import when no task and no queue exist", () => {
    const state = deriveKnowledgeTaskMainlineState({
      importTaskId: "",
      qualityConfirmed: false,
      compareConfirmed: false,
      queueStats: {
        draftCount: 0,
        publishedCount: 0,
        discardedCount: 0,
        lowConfidenceCount: 0,
      },
      currentDraft: {},
    });

    expect(state.kind).toBe("waiting_import");
    expect(state.primaryActionLabel).toBe("导入并生成草稿");
    expect(state.blockers).toEqual([]);
  });

  it("returns waiting_quality when import exists but quality is not confirmed", () => {
    const state = deriveKnowledgeTaskMainlineState({
      importTaskId: "task-1",
      qualityConfirmed: false,
      compareConfirmed: false,
      queueStats: {
        draftCount: 2,
        publishedCount: 0,
        discardedCount: 0,
        lowConfidenceCount: 1,
      },
      currentDraft: {
        sceneId: 12,
        sceneTitle: "代发协议查询",
      },
    });

    expect(state.kind).toBe("waiting_quality");
    expect(state.primaryActionLabel).toBe("确认质检，进入对照");
    expect(state.blockers).toContain("仍有低置信度场景待确认");
  });

  it("returns waiting_compare when quality is confirmed but compare is not", () => {
    const state = deriveKnowledgeTaskMainlineState({
      importTaskId: "task-1",
      qualityConfirmed: true,
      compareConfirmed: false,
      queueStats: {
        draftCount: 2,
        publishedCount: 0,
        discardedCount: 0,
        lowConfidenceCount: 0,
      },
      currentDraft: {
        sceneId: 12,
        sceneTitle: "代发协议查询",
      },
    });

    expect(state.kind).toBe("waiting_compare");
    expect(state.primaryActionLabel).toBe("确认对照，进入发布整理");
    expect(state.blockers).toEqual([]);
  });

  it("returns waiting_publish_prep when compare is confirmed and draft queue remains", () => {
    const state = deriveKnowledgeTaskMainlineState({
      importTaskId: "task-1",
      qualityConfirmed: true,
      compareConfirmed: true,
      queueStats: {
        draftCount: 3,
        publishedCount: 0,
        discardedCount: 0,
        lowConfidenceCount: 1,
      },
      currentDraft: {
        sceneId: 12,
        sceneTitle: "",
      },
    });

    expect(state.kind).toBe("waiting_publish_prep");
    expect(state.primaryActionLabel).toBe("继续处理当前场景");
    expect(state.blockers).toContain("仍有场景待补齐业务名称或业务字段");
  });

  it("returns completed when no draft queue remains after compare confirmation", () => {
    const state = deriveKnowledgeTaskMainlineState({
      importTaskId: "task-1",
      qualityConfirmed: true,
      compareConfirmed: true,
      queueStats: {
        draftCount: 0,
        publishedCount: 2,
        discardedCount: 1,
        lowConfidenceCount: 0,
      },
      currentDraft: {
        sceneId: 12,
        sceneTitle: "代发协议查询",
      },
    });

    expect(state.kind).toBe("completed");
    expect(state.title).toBe("当前导入任务已处理完成");
    expect(state.primaryActionLabel).toBe("查看已处理场景");
    expect(state.blockers).toEqual([]);
  });
});

describe("cross workbench link gating", () => {
  it("shows data map only when a browseable scene exists", () => {
    expect(
      shouldShowDataMapLink({
        queueStats: { publishedCount: 0 },
        selectedScene: null,
        currentDraft: {},
      }),
    ).toBe(false);
    expect(
      shouldShowDataMapLink({
        queueStats: { publishedCount: 1 },
        selectedScene: { scene_id: 9 },
        currentDraft: {},
      }),
    ).toBe(true);
    expect(
      shouldShowDataMapLink({
        queueStats: { publishedCount: 0 },
        selectedScene: null,
        currentDraft: { sceneId: 9 },
      }),
    ).toBe(true);
  });

  it("shows runtime only when a verifiable scene exists", () => {
    expect(
      shouldShowRuntimeLink({
        queueStats: { publishedCount: 0 },
        currentDraft: {},
      }),
    ).toBe(false);
    expect(
      shouldShowRuntimeLink({
        queueStats: { publishedCount: 1 },
        currentDraft: { sceneId: 9 },
      }),
    ).toBe(true);
  });
});
