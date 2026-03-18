import { Inbox } from "lucide-react";

function classNames(...values) {
  return values.filter(Boolean).join(" ");
}

export function UiEmptyState({
  icon = null,
  title = "暂无数据",
  description = "",
  action = null,
  className = "",
}) {
  const iconNode = icon || <Inbox size={22} strokeWidth={1.8} />;
  return (
    <section className={classNames("ui-empty-state", className)} aria-live="polite">
      <div className="ui-empty-icon" aria-hidden="true">{iconNode}</div>
      <h3>{title}</h3>
      {description ? <p>{description}</p> : null}
      {action ? <div className="ui-empty-action">{action}</div> : null}
    </section>
  );
}
