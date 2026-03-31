import { describe, expect, it } from "vitest";
import {
  DATA_MAP_STATUS_OPTIONS,
  describeCoverageStatus,
  describeDecisionStatus,
  describeImportTaskStatus,
  describeProjectionStatus,
  describePublishStatus,
  describeSceneStatus,
} from "./statusPresentation";
import {
  DATA_MAP_STATUS_OPTIONS as DATA_MAP_STATUS_OPTIONS_FROM_INDEX,
  describeCoverageStatus as describeCoverageStatusFromIndex,
  describeDecisionStatus as describeDecisionStatusFromIndex,
  describeImportTaskStatus as describeImportTaskStatusFromIndex,
  describeProjectionStatus as describeProjectionStatusFromIndex,
  describePublishStatus as describePublishStatusFromIndex,
  describeSceneStatus as describeSceneStatusFromIndex,
} from "./index";

describe("status presentation helpers", () => {
  it("describes scene status values with chinese labels and tones", () => {
    expect(describeSceneStatus("DRAFT")).toEqual({
      code: "DRAFT",
      label: "草稿",
      tone: "warn",
    });
    expect(describeSceneStatus("PUBLISHED")).toEqual({
      code: "PUBLISHED",
      label: "已发布",
      tone: "good",
    });
    expect(describeSceneStatus("DISCARDED")).toEqual({
      code: "DISCARDED",
      label: "已弃用",
      tone: "bad",
    });
    expect(describeSceneStatus("reviewed")).toEqual({
      code: "REVIEWED",
      label: "已复核",
      tone: "good",
    });
    expect(describeSceneStatus("RETIRED")).toEqual({
      code: "RETIRED",
      label: "已退役",
      tone: "neutral",
    });
  });

  it("describes projection status values with chinese labels and tones", () => {
    expect(describeProjectionStatus("PENDING")).toEqual({
      code: "PENDING",
      label: "待处理",
      tone: "warn",
    });
    expect(describeProjectionStatus("READY")).toEqual({
      code: "READY",
      label: "已就绪",
      tone: "good",
    });
    expect(describeProjectionStatus("completed")).toEqual({
      code: "COMPLETED",
      label: "已完成",
      tone: "good",
    });
    expect(describeProjectionStatus("FAILED")).toEqual({
      code: "FAILED",
      label: "失败",
      tone: "bad",
    });
  });

  it("describes publish status values with chinese labels and tones", () => {
    expect(describePublishStatus("PUBLISHED")).toEqual({
      code: "PUBLISHED",
      label: "已发布",
      tone: "good",
    });
    expect(describePublishStatus("BLOCKED")).toEqual({
      code: "BLOCKED",
      label: "已阻断",
      tone: "bad",
    });
    expect(describePublishStatus("ARCHIVED")).toEqual({
      code: "ARCHIVED",
      label: "已归档",
      tone: "neutral",
    });
  });

  it("describes coverage and decision status values", () => {
    expect(describeCoverageStatus("FULL")).toEqual({
      code: "FULL",
      label: "完整覆盖",
      tone: "good",
    });
    expect(describeCoverageStatus("PARTIAL")).toEqual({
      code: "PARTIAL",
      label: "部分覆盖",
      tone: "warn",
    });
    expect(describeCoverageStatus("GAP")).toEqual({
      code: "GAP",
      label: "存在缺口",
      tone: "bad",
    });
    expect(describeDecisionStatus("allow")).toEqual({
      code: "ALLOW",
      label: "允许",
      tone: "good",
    });
    expect(describeDecisionStatus("need_approval")).toEqual({
      code: "NEED_APPROVAL",
      label: "需审批",
      tone: "warn",
    });
    expect(describeDecisionStatus("clarification_only")).toEqual({
      code: "CLARIFICATION_ONLY",
      label: "需澄清",
      tone: "warn",
    });
    expect(describeDecisionStatus("deny")).toEqual({
      code: "DENY",
      label: "已拒绝",
      tone: "bad",
    });
  });

  it("describes import task lifecycle statuses with chinese labels and tones", () => {
    expect(describeImportTaskStatus("RUNNING")).toEqual({
      code: "RUNNING",
      label: "处理中",
      tone: "warn",
    });
    expect(describeImportTaskStatus("quality_reviewing")).toEqual({
      code: "QUALITY_REVIEWING",
      label: "质检中",
      tone: "warn",
    });
    expect(describeImportTaskStatus("SCENE_REVIEWING")).toEqual({
      code: "SCENE_REVIEWING",
      label: "场景复核中",
      tone: "warn",
    });
    expect(describeImportTaskStatus("PUBLISHING")).toEqual({
      code: "PUBLISHING",
      label: "发布中",
      tone: "warn",
    });
    expect(describeImportTaskStatus("COMPLETED")).toEqual({
      code: "COMPLETED",
      label: "已完成",
      tone: "good",
    });
    expect(describeImportTaskStatus("FAILED")).toEqual({
      code: "FAILED",
      label: "失败",
      tone: "bad",
    });
    expect(describeImportTaskStatus("READY_FOR_REVIEW")).toEqual({
      code: "READY_FOR_REVIEW",
      label: "待复核",
      tone: "warn",
    });
    expect(describeImportTaskStatus("CLOSED")).toEqual({
      code: "CLOSED",
      label: "已关闭",
      tone: "neutral",
    });
  });

  it("keeps unknown status codes while falling back to neutral chinese labels", () => {
    expect(describeSceneStatus("MYSTERY_STATUS")).toEqual({
      code: "MYSTERY_STATUS",
      label: "未知状态",
      tone: "neutral",
    });
  });

  it("exports data map status options with chinese labels", () => {
    expect(DATA_MAP_STATUS_OPTIONS).toEqual([
      { value: "DRAFT", label: "草稿" },
      { value: "REVIEWED", label: "已复核" },
      { value: "PUBLISHED", label: "已发布" },
      { value: "RETIRED", label: "已退役" },
    ]);
  });

  it("re-exports the helpers from the ui index barrel", () => {
    expect(DATA_MAP_STATUS_OPTIONS_FROM_INDEX).toEqual(DATA_MAP_STATUS_OPTIONS);
    expect(describeSceneStatusFromIndex("PUBLISHED")).toEqual(describeSceneStatus("PUBLISHED"));
    expect(describeProjectionStatusFromIndex("READY")).toEqual(describeProjectionStatus("READY"));
    expect(describePublishStatusFromIndex("ARCHIVED")).toEqual(describePublishStatus("ARCHIVED"));
    expect(describeCoverageStatusFromIndex("FULL")).toEqual(describeCoverageStatus("FULL"));
    expect(describeDecisionStatusFromIndex("clarification_only")).toEqual(describeDecisionStatus("clarification_only"));
    expect(describeImportTaskStatusFromIndex("RUNNING")).toEqual(describeImportTaskStatus("RUNNING"));
  });
});
