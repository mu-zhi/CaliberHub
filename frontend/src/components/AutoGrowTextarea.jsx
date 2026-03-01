import { useLayoutEffect, useRef } from "react";

function toNumber(value, fallback) {
  const num = Number.parseFloat(`${value || ""}`);
  return Number.isFinite(num) ? num : fallback;
}

function computeTargetHeight(node, minRows, maxRows) {
  const style = window.getComputedStyle(node);
  const lineHeight = toNumber(style.lineHeight, 20);
  const paddingTop = toNumber(style.paddingTop, 0);
  const paddingBottom = toNumber(style.paddingBottom, 0);
  const borderTop = toNumber(style.borderTopWidth, 0);
  const borderBottom = toNumber(style.borderBottomWidth, 0);
  const verticalExtras = paddingTop + paddingBottom + borderTop + borderBottom;
  const minHeight = Math.max(1, minRows) * lineHeight + verticalExtras;
  const maxHeight = Math.max(minRows, maxRows) * lineHeight + verticalExtras;
  return {
    minHeight,
    maxHeight,
  };
}

export function AutoGrowTextarea({
  value,
  minRows = 3,
  maxRows = 16,
  className = "",
  ...props
}) {
  const textareaRef = useRef(null);

  useLayoutEffect(() => {
    const node = textareaRef.current;
    if (!node) {
      return;
    }
    const { minHeight, maxHeight } = computeTargetHeight(node, minRows, maxRows);
    node.style.height = "auto";
    const nextHeight = Math.min(Math.max(node.scrollHeight, minHeight), maxHeight);
    node.style.height = `${nextHeight}px`;
    node.style.overflowY = node.scrollHeight > maxHeight ? "auto" : "hidden";
  }, [value, minRows, maxRows]);

  return (
    <textarea
      {...props}
      ref={textareaRef}
      value={value}
      rows={minRows}
      className={`auto-grow-textarea ${className}`.trim()}
    />
  );
}
