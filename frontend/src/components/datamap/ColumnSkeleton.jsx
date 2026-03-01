const DEFAULT_COUNT = 8;

export function ColumnSkeleton({ count = DEFAULT_COUNT }) {
  return (
    <ul className="miller-skeleton-list" aria-hidden="true">
      {Array.from({ length: count }).map((_, index) => (
        <li key={`skeleton-${index}`} className="miller-skeleton-item" />
      ))}
    </ul>
  );
}
