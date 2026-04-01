const UNKNOWN_STATUS_PRESENTATION = {
  code: "UNKNOWN",
  label: "未知状态",
  tone: "neutral",
};

const SCENE_STATUS_PRESENTATIONS = {
  DRAFT: {
    code: "DRAFT",
    label: "草稿",
    tone: "warn",
  },
  REVIEWED: {
    code: "REVIEWED",
    label: "已复核",
    tone: "good",
  },
  PUBLISHED: {
    code: "PUBLISHED",
    label: "已发布",
    tone: "good",
  },
  RETIRED: {
    code: "RETIRED",
    label: "已退役",
    tone: "neutral",
  },
  DISCARDED: {
    code: "DISCARDED",
    label: "已弃用",
    tone: "bad",
  },
  REJECTED: {
    code: "REJECTED",
    label: "已驳回",
    tone: "bad",
  },
};

const PROJECTION_STATUS_PRESENTATIONS = {
  PENDING: {
    code: "PENDING",
    label: "待处理",
    tone: "warn",
  },
  READY: {
    code: "READY",
    label: "已就绪",
    tone: "good",
  },
  SUCCEEDED: {
    code: "SUCCEEDED",
    label: "已完成",
    tone: "good",
  },
  COMPLETED: {
    code: "COMPLETED",
    label: "已完成",
    tone: "good",
  },
  FAILED: {
    code: "FAILED",
    label: "失败",
    tone: "bad",
  },
  RUNNING: {
    code: "RUNNING",
    label: "处理中",
    tone: "warn",
  },
  CHECKING: {
    code: "CHECKING",
    label: "检查中",
    tone: "warn",
  },
  PASSED: {
    code: "PASSED",
    label: "已通过",
    tone: "good",
  },
  BLOCKED: {
    code: "BLOCKED",
    label: "已阻断",
    tone: "bad",
  },
  SWITCHED: {
    code: "SWITCHED",
    label: "已切换",
    tone: "good",
  },
  ARCHIVED: {
    code: "ARCHIVED",
    label: "已归档",
    tone: "neutral",
  },
  SKIPPED: {
    code: "SKIPPED",
    label: "已关闭",
    tone: "neutral",
  },
  IDLE: {
    code: "IDLE",
    label: "未触发",
    tone: "neutral",
  },
  NOT_FOUND: {
    code: "NOT_FOUND",
    label: "未触发",
    tone: "neutral",
  },
};

const PUBLISH_STATUS_PRESENTATIONS = {
  PUBLISHED: {
    code: "PUBLISHED",
    label: "已发布",
    tone: "good",
  },
  PASSED: {
    code: "PASSED",
    label: "已通过",
    tone: "good",
  },
  BLOCKED: {
    code: "BLOCKED",
    label: "已阻断",
    tone: "bad",
  },
  SWITCHED: {
    code: "SWITCHED",
    label: "已切换",
    tone: "good",
  },
  ARCHIVED: {
    code: "ARCHIVED",
    label: "已归档",
    tone: "neutral",
  },
  FAILED: {
    code: "FAILED",
    label: "失败",
    tone: "bad",
  },
};

const COVERAGE_STATUS_PRESENTATIONS = {
  FULL: {
    code: "FULL",
    label: "完整覆盖",
    tone: "good",
  },
  PARTIAL: {
    code: "PARTIAL",
    label: "部分覆盖",
    tone: "warn",
  },
  GAP: {
    code: "GAP",
    label: "存在缺口",
    tone: "bad",
  },
  NONE: {
    code: "NONE",
    label: "未覆盖",
    tone: "bad",
  },
};

const COVERAGE_STATUS_ALIASES = {
  FULL_MATCH: "FULL",
  PARTIAL_MATCH: "PARTIAL",
  COVERAGE_GAP: "GAP",
  NO_COVERAGE: "NONE",
};

const DECISION_STATUS_PRESENTATIONS = {
  ALLOW: {
    code: "ALLOW",
    label: "允许",
    tone: "good",
  },
  NEED_APPROVAL: {
    code: "NEED_APPROVAL",
    label: "需审批",
    tone: "warn",
  },
  DENY: {
    code: "DENY",
    label: "已拒绝",
    tone: "bad",
  },
  NEED_CLARIFICATION: {
    code: "NEED_CLARIFICATION",
    label: "需澄清",
    tone: "warn",
  },
  CLARIFICATION_ONLY: {
    code: "CLARIFICATION_ONLY",
    label: "需澄清",
    tone: "warn",
  },
  REJECT: {
    code: "REJECT",
    label: "已驳回",
    tone: "bad",
  },
  REJECTED: {
    code: "REJECTED",
    label: "已驳回",
    tone: "bad",
  },
  PENDING_CONFIRMATION: {
    code: "PENDING_CONFIRMATION",
    label: "待确认",
    tone: "warn",
  },
  ACCEPTED: {
    code: "ACCEPTED",
    label: "已接受",
    tone: "good",
  },
};

const IMPORT_TASK_STATUS_PRESENTATIONS = {
  CREATED: {
    code: "CREATED",
    label: "已创建",
    tone: "neutral",
  },
  VALIDATED: {
    code: "VALIDATED",
    label: "已校验",
    tone: "warn",
  },
  PARSED: {
    code: "PARSED",
    label: "已解析",
    tone: "warn",
  },
  MODELLED: {
    code: "MODELLED",
    label: "已建模",
    tone: "warn",
  },
  ALIGNED: {
    code: "ALIGNED",
    label: "已对齐",
    tone: "warn",
  },
  READY_FOR_REVIEW: {
    code: "READY_FOR_REVIEW",
    label: "待复核",
    tone: "warn",
  },
  CLOSED: {
    code: "CLOSED",
    label: "已关闭",
    tone: "neutral",
  },
  RUNNING: {
    code: "RUNNING",
    label: "处理中",
    tone: "warn",
  },
  QUALITY_REVIEWING: {
    code: "QUALITY_REVIEWING",
    label: "质检中",
    tone: "warn",
  },
  SCENE_REVIEWING: {
    code: "SCENE_REVIEWING",
    label: "场景复核中",
    tone: "warn",
  },
  PUBLISHING: {
    code: "PUBLISHING",
    label: "发布中",
    tone: "warn",
  },
  COMPLETED: {
    code: "COMPLETED",
    label: "已完成",
    tone: "good",
  },
  FAILED: {
    code: "FAILED",
    label: "失败",
    tone: "bad",
  },
};

function normalizeStatusCode(status) {
  const raw = `${status ?? ""}`.trim();
  if (!raw) {
    return UNKNOWN_STATUS_PRESENTATION.code;
  }
  return raw.toUpperCase();
}

function describeStatus(status, presentations) {
  const code = normalizeStatusCode(status);
  return presentations[code] || { ...UNKNOWN_STATUS_PRESENTATION, code };
}

export function describeSceneStatus(status) {
  return describeStatus(status, SCENE_STATUS_PRESENTATIONS);
}

export function describeProjectionStatus(status) {
  return describeStatus(status, PROJECTION_STATUS_PRESENTATIONS);
}

export function describePublishStatus(status) {
  return describeStatus(status, PUBLISH_STATUS_PRESENTATIONS);
}

export function describeCoverageStatus(status) {
  const code = normalizeStatusCode(status);
  const normalizedCode = COVERAGE_STATUS_ALIASES[code] || code;
  return COVERAGE_STATUS_PRESENTATIONS[normalizedCode] || { ...UNKNOWN_STATUS_PRESENTATION, code: normalizedCode };
}

export function describeDecisionStatus(status) {
  return describeStatus(status, DECISION_STATUS_PRESENTATIONS);
}

export function describeImportTaskStatus(status) {
  return describeStatus(status, IMPORT_TASK_STATUS_PRESENTATIONS);
}

export const DATA_MAP_STATUS_OPTIONS = [
  { value: "DRAFT", label: describeSceneStatus("DRAFT").label },
  { value: "REVIEWED", label: describeSceneStatus("REVIEWED").label },
  { value: "PUBLISHED", label: describeSceneStatus("PUBLISHED").label },
  { value: "RETIRED", label: describeSceneStatus("RETIRED").label },
];
