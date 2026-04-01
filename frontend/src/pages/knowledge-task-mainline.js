function toCount(value) {
  const count = Number(value || 0);
  return Number.isFinite(count) ? count : 0;
}

export function deriveKnowledgeTaskMainlineState({
  importTaskId,
  qualityConfirmed,
  compareConfirmed,
  queueStats,
  currentDraft,
}) {
  if (!importTaskId && toCount(queueStats?.draftCount) === 0) {
    return {
      kind: "waiting_import",
      title: "当前还没有有效导入任务",
      primaryActionLabel: "导入并生成草稿",
      blockers: [],
    };
  }

  if (!qualityConfirmed) {
    return {
      kind: "waiting_quality",
      title: "当前任务停留在抽取质量判断",
      primaryActionLabel: "确认质检，进入对照",
      blockers:
        toCount(queueStats?.lowConfidenceCount) > 0
          ? ["仍有低置信度场景待确认"]
          : [],
    };
  }

  if (!compareConfirmed) {
    return {
      kind: "waiting_compare",
      title: "当前任务停留在原文对照",
      primaryActionLabel: "确认对照，进入发布整理",
      blockers: [],
    };
  }

  if (toCount(queueStats?.draftCount) === 0) {
    return {
      kind: "completed",
      title: "当前导入任务已处理完成",
      primaryActionLabel: "查看已处理场景",
      blockers: [],
    };
  }

  return {
    kind: "waiting_publish_prep",
    title: "当前任务停留在场景整理与发布",
    primaryActionLabel: "继续处理当前场景",
    blockers: !currentDraft?.sceneTitle
      ? ["仍有场景待补齐业务名称或业务字段"]
      : [],
  };
}

export function shouldShowDataMapLink({ queueStats, selectedScene, currentDraft }) {
  return Boolean(
    selectedScene?.scene_id
      || selectedScene?.sceneId
      || currentDraft?.sceneId
      || currentDraft?.scene_id
      || toCount(queueStats?.publishedCount) > 0,
  );
}

export function shouldShowRuntimeLink({ queueStats, currentDraft }) {
  return Boolean(currentDraft?.sceneId || currentDraft?.scene_id || toCount(queueStats?.publishedCount) > 0);
}
