import { MillerColumn } from "./MillerColumn";
import { DataMapEmptyState } from "./DataMapEmptyState";

export function MillerColumnsView({
  columns,
  activePath,
  trackRef,
  onSelectNode,
  onRetryColumn,
  onMoveLeft,
  previewPanel,
}) {
  return (
    <section className="datamap-columns-view" aria-label="浏览模式">
      <div className="miller-columns-track" ref={trackRef}>
        {columns.map((column, index) => (
          <MillerColumn
            key={`${column.columnId}-${index}`}
            column={column}
            columnIndex={index}
            activePath={activePath}
            onSelectNode={onSelectNode}
            onRetryColumn={onRetryColumn}
            onMoveLeft={onMoveLeft}
          />
        ))}
        {columns.length === 0 ? (
          <DataMapEmptyState
            title="暂无可浏览节点"
            description="请检查筛选条件，或稍后刷新重试。"
          />
        ) : null}
        {previewPanel}
      </div>
    </section>
  );
}
