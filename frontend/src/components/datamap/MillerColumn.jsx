import { ColumnSkeleton } from "./ColumnSkeleton";
import { UiInlineError } from "../ui";

function statusClass(status) {
  if (status === "PUBLISHED") {
    return "status-published";
  }
  if (status === "DRAFT") {
    return "status-draft";
  }
  if (status === "DISCARDED" || status === "RISK") {
    return "status-risk";
  }
  return "status-unknown";
}

function focusSibling(event, offset) {
  const list = event.currentTarget.closest("ul");
  if (!list) {
    return;
  }
  const buttons = Array.from(list.querySelectorAll("button.miller-item-btn"));
  const index = Number(event.currentTarget.getAttribute("data-item-index"));
  if (!Number.isFinite(index)) {
    return;
  }
  const nextIndex = Math.max(0, Math.min(buttons.length - 1, index + offset));
  buttons[nextIndex]?.focus();
}

export function MillerColumn({
  column,
  columnIndex,
  activePath,
  onSelectNode,
  onRetryColumn,
  onMoveLeft,
}) {
  const currentActiveId = activePath[columnIndex] || "";
  const title = columnIndex === 0 ? "根主题" : `第 ${columnIndex + 1} 列`;

  return (
    <section className="miller-column" data-column-index={columnIndex} aria-label={`米勒列-${columnIndex + 1}`}>
      <header className="miller-column-head">
        <h3>{title}</h3>
        <span>{column.items?.length || 0}</span>
      </header>

      {column.loading ? <ColumnSkeleton /> : null}

      {column.error ? (
        <UiInlineError
          className="miller-column-error"
          message="当前列加载失败，请重试"
          actionText="点击重试"
          onAction={() => onRetryColumn(column.columnId, columnIndex)}
        />
      ) : null}

      {!column.loading && !column.error && (!column.items || column.items.length === 0) ? (
        <p className="miller-column-empty">暂无子业务</p>
      ) : null}

      {!column.loading && !column.error && column.items?.length > 0 ? (
        <nav aria-label={`列${columnIndex + 1}节点`}>
          <ul className="miller-item-list">
            {column.items.map((node, index) => {
              const isActive = currentActiveId === node.id;
              const inPath = !isActive && activePath.includes(node.id);
              return (
                <li key={node.id}>
                  <button
                    type="button"
                    className={`miller-item-btn ${isActive ? "is-active" : ""} ${inPath ? "is-in-path" : ""}`}
                    data-item-index={index}
                    onClick={() => onSelectNode(node, columnIndex)}
                    onKeyDown={(event) => {
                      if (event.key === "ArrowDown") {
                        event.preventDefault();
                        focusSibling(event, 1);
                        return;
                      }
                      if (event.key === "ArrowUp") {
                        event.preventDefault();
                        focusSibling(event, -1);
                        return;
                      }
                      if (event.key === "ArrowRight") {
                        event.preventDefault();
                        onSelectNode(node, columnIndex);
                        return;
                      }
                      if (event.key === "ArrowLeft") {
                        event.preventDefault();
                        onMoveLeft(columnIndex);
                        return;
                      }
                      if (event.key === "Enter" || event.key === " ") {
                        event.preventDefault();
                        onSelectNode(node, columnIndex);
                      }
                    }}
                  >
                    <span className={`miller-status-dot ${statusClass(node.status)}`} aria-hidden="true" />
                    <span className="miller-item-label">{node.label}</span>
                    {node.hasChildren ? <span className="miller-item-arrow" aria-hidden="true">›</span> : null}
                  </button>
                </li>
              );
            })}
          </ul>
        </nav>
      ) : null}
    </section>
  );
}
