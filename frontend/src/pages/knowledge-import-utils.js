export function resolveAccordionStepState(stepNo, activeStep) {
  const step = Number(stepNo || 0);
  const current = Number(activeStep || 1);
  if (step === current) {
    return "expanded";
  }
  if (step < current) {
    return "collapsed";
  }
  return "locked";
}

export function toConfidenceLevelZh(confidenceScore) {
  const confidence = Number(confidenceScore || 0);
  if (confidence >= 0.85) {
    return "高";
  }
  if (confidence >= 0.7) {
    return "中";
  }
  return "低";
}

export function buildStep1Summary(sourceLabel, rawText) {
  const label = `${sourceLabel || "粘贴文本"}`.trim() || "粘贴文本";
  const charCount = `${rawText || ""}`.length;
  return `✓ 01 导入口径草稿 | 来源：${label}（约 ${charCount} 字）`;
}

export function buildStep2Summary(sceneCount, confidenceScore) {
  const count = Number(sceneCount || 0);
  const confidence = Number(confidenceScore || 0);
  const percent = confidence > 0 ? `${Math.round(confidence * 100)}%` : "0%";
  return `✓ 02 抽取质量判断 | 共识别 ${count} 个场景，置信度 ${percent}（${toConfidenceLevelZh(confidence)}）`;
}

export function buildStep3Summary(confirmedCount) {
  const count = Number(confirmedCount || 0);
  return `✓ 03 结果与原文对照 | 已确认场景 ${count} 个`;
}
