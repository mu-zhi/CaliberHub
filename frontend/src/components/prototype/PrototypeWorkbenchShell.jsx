import { useEffect, useMemo, useState } from "react";
import { ChevronDown, ChevronUp } from "lucide-react";
import { UiBadge, UiButton, UiCard, UiSegmentedControl } from "../ui";

function classNames(...values) {
  return values.filter(Boolean).join(" ");
}

function resolveStepTone(status) {
  if (status === "done") {
    return "good";
  }
  if (status === "active") {
    return "warn";
  }
  if (status === "bad") {
    return "bad";
  }
  return "neutral";
}

export function PrototypeWorkbenchShell({
  kicker = "",
  title,
  description,
  tags = [],
  steps = [],
  toolbar = null,
  main,
  side,
  consoleTabs = [],
  defaultConsoleExpanded = false,
}) {
  const [consoleExpanded, setConsoleExpanded] = useState(defaultConsoleExpanded);
  const [consoleTab, setConsoleTab] = useState(consoleTabs[0]?.value || "");

  useEffect(() => {
    setConsoleTab(consoleTabs[0]?.value || "");
  }, [consoleTabs]);

  const activeConsole = useMemo(() => {
    return consoleTabs.find((item) => item.value === consoleTab) || consoleTabs[0] || null;
  }, [consoleTab, consoleTabs]);

  return (
    <section className="panel proto-page">
      <header className="proto-page-head">
        <div className="proto-page-kicker-row">
          <p className="proto-page-kicker">{kicker}</p>
          <div className="proto-page-tag-list">
            {tags.map((item) => (
              <UiBadge key={item.label} tone={item.tone || "neutral"}>{item.label}</UiBadge>
            ))}
          </div>
        </div>
        <div className="proto-page-title-row">
          <div>
            <h2>{title}</h2>
            <p className="subtle-note">{description}</p>
          </div>
        </div>
      </header>

      <ol className="proto-stage-strip" aria-label="原型流程阶段">
        {steps.map((item) => (
          <li key={`${item.no}-${item.label}`} className={classNames("proto-stage-item", `status-${item.status || "pending"}`)}>
            <span className="proto-stage-no">{String(item.no).padStart(2, "0")}</span>
            <div className="proto-stage-text">
              <strong>{item.label}</strong>
              <span>{item.summary}</span>
            </div>
            <UiBadge tone={resolveStepTone(item.status)}>{item.statusLabel || item.label}</UiBadge>
          </li>
        ))}
      </ol>

      {toolbar ? <div className="proto-toolbar">{toolbar}</div> : null}

      <div className="proto-workbench">
        <section className="proto-main-stage" aria-label="主工作区">
          {main}
        </section>
        <aside className="proto-side-stage" aria-label="检查与操作区">
          {side}
        </aside>
      </div>

      {activeConsole ? (
        <section className={classNames("proto-console", consoleExpanded ? "is-open" : "")} aria-label="运行输出区">
          <div className="proto-console-head">
            <div>
              <strong>{activeConsole.title || "运行输出"}</strong>
              <p>{activeConsole.description || "当前阶段的日志、校验输出与追踪摘要"}</p>
            </div>
            <UiButton
              variant="secondary"
              size="sm"
              icon={consoleExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
              onClick={() => setConsoleExpanded((prev) => !prev)}
            >
              {consoleExpanded ? "折叠输出区" : "展开输出区"}
            </UiButton>
          </div>

          {consoleTabs.length > 1 ? (
            <UiSegmentedControl
              className="proto-console-segment"
              ariaLabel="控制台标签切换"
              value={consoleTab}
              onChange={setConsoleTab}
              options={consoleTabs.map((item) => ({ value: item.value, label: item.label }))}
            />
          ) : null}

          <div className="proto-console-body">
            {(activeConsole.logs || []).map((log) => (
              <article key={`${activeConsole.value}-${log.at}-${log.message}`} className={classNames("proto-console-row", `level-${log.level || "info"}`)}>
                <div className="proto-console-row-head">
                  <span className="proto-console-time">{log.at}</span>
                  <span className="proto-console-message">{log.message}</span>
                </div>
                {log.detail ? <pre>{log.detail}</pre> : null}
              </article>
            ))}
          </div>
        </section>
      ) : null}
    </section>
  );
}

export function PrototypeMetricStrip({ items = [] }) {
  return (
    <div className="proto-metric-strip">
      {items.map((item) => (
        <UiCard key={item.label} className="proto-metric-card" elevation="card">
          <span>{item.label}</span>
          <strong>{item.value}</strong>
          <small>{item.hint}</small>
        </UiCard>
      ))}
    </div>
  );
}

export function PrototypeGraphCanvas({
  title,
  description,
  nodes = [],
  edges = [],
  legend = [],
  selectedId = "",
  onSelect,
}) {
  const nodeMap = useMemo(() => {
    return new Map(nodes.map((item) => [item.id, item]));
  }, [nodes]);

  return (
    <UiCard className="proto-graph-card" elevation="card">
      <div className="proto-card-head">
        <div>
          <h3>{title}</h3>
          <p className="subtle-note">{description}</p>
        </div>
      </div>

      <div className="proto-graph-wrap">
        <svg className="proto-graph-svg" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
          {edges.map((edge) => {
            const from = nodeMap.get(edge.from);
            const to = nodeMap.get(edge.to);
            if (!from || !to) {
              return null;
            }
            const midX = (from.x + to.x) / 2;
            const midY = (from.y + to.y) / 2;
            return (
              <g key={`${edge.from}-${edge.to}-${edge.label || ""}`} className={classNames("proto-edge-group", edge.dashed ? "is-dashed" : "")}>
                <line
                  className={classNames("proto-edge-line", edge.emphasis ? "is-emphasis" : "")}
                  x1={from.x}
                  y1={from.y}
                  x2={to.x}
                  y2={to.y}
                />
                {edge.label ? (
                  <text className="proto-edge-label" x={midX} y={midY}>
                    {edge.label}
                  </text>
                ) : null}
              </g>
            );
          })}
        </svg>

        {nodes.map((node) => {
          const active = node.id === selectedId;
          return (
            <button
              key={node.id}
              type="button"
              className={classNames("proto-graph-node", `tone-${node.tone || "neutral"}`, active ? "is-active" : "")}
              style={{ left: `${node.x}%`, top: `${node.y}%` }}
              onClick={() => onSelect?.(node.id)}
            >
              <strong>{node.label}</strong>
              <span>{node.hint}</span>
            </button>
          );
        })}

        {legend.length > 0 ? (
          <div className="proto-graph-legend" aria-label="图谱图例">
            {legend.map((item) => (
              <span key={item.label} className={classNames("proto-legend-chip", `tone-${item.tone || "neutral"}`)}>
                <i aria-hidden="true" />
                <span>{item.label}</span>
              </span>
            ))}
          </div>
        ) : null}
      </div>
    </UiCard>
  );
}

export function PrototypeKvList({ items = [] }) {
  return (
    <dl className="proto-kv-list">
      {items.map((item) => (
        <div key={item.label}>
          <dt>{item.label}</dt>
          <dd>{item.value}</dd>
        </div>
      ))}
    </dl>
  );
}
