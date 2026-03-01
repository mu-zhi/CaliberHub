import { forwardRef, useLayoutEffect, useRef, useState } from "react";

function CheckCircleIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true" focusable="false">
      <circle cx="10" cy="10" r="8.5" fill="none" stroke="currentColor" strokeWidth="1.6" />
      <path
        d="M6.1 10.4L8.7 12.9L13.9 7.8"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.9"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export const AccordionStepCard = forwardRef(function AccordionStepCard(
  {
    stepNo,
    title,
    state,
    summaryText,
    showEdit,
    onEdit,
    children,
  },
  ref,
) {
  const safeNo = Number(stepNo) > 0 ? String(stepNo).padStart(2, "0") : "--";
  const safeState = state === "expanded" || state === "collapsed" || state === "locked"
    ? state
    : "locked";
  const isExpanded = safeState === "expanded";
  const isCollapsed = safeState === "collapsed";
  const contentInnerRef = useRef(null);
  const [contentMaxHeight, setContentMaxHeight] = useState(0);

  useLayoutEffect(() => {
    if (!isExpanded) {
      setContentMaxHeight(0);
      return undefined;
    }
    const node = contentInnerRef.current;
    if (!node) {
      return undefined;
    }

    const syncHeight = () => {
      setContentMaxHeight(node.scrollHeight);
    };
    syncHeight();

    if (typeof ResizeObserver === "undefined") {
      return undefined;
    }
    const observer = new ResizeObserver(() => syncHeight());
    observer.observe(node);
    return () => observer.disconnect();
  }, [children, isExpanded]);

  const contentStyle = isExpanded
    ? { maxHeight: `${Math.max(1, contentMaxHeight)}px` }
    : { maxHeight: "0px" };

  return (
    <section
      ref={ref}
      className={`panel accordion-step-card is-${safeState}`}
      aria-current={isExpanded ? "step" : undefined}
    >
      <div className="accordion-step-head">
        <div className="accordion-step-head-main">
          <span className={`accordion-step-icon ${isCollapsed ? "is-done" : ""}`} aria-hidden="true">
            {isCollapsed ? <CheckCircleIcon /> : safeNo}
          </span>
          <div className="accordion-step-head-text">
            {isCollapsed ? (
              <p className="accordion-step-summary">{summaryText}</p>
            ) : (
              <>
                <h2>{title}</h2>
                <p>{summaryText}</p>
              </>
            )}
          </div>
        </div>
        {showEdit ? (
          <button className="btn btn-ghost" type="button" onClick={onEdit}>
            修改
          </button>
        ) : null}
      </div>
      <div
        className={`accordion-step-content ${isExpanded ? "is-open" : "is-closed"}`}
        aria-hidden={!isExpanded}
        style={contentStyle}
        {...(isExpanded ? {} : { inert: "" })}
      >
        <div className="accordion-step-content-inner" ref={contentInnerRef}>{children}</div>
      </div>
    </section>
  );
});
