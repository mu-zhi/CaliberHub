import { useMemo } from "react";

function classNames(...values) {
  return values.filter(Boolean).join(" ");
}

export function UiSegmentedControl({
  options = [],
  value = "",
  onChange,
  className = "",
  ariaLabel = "分段选择",
}) {
  const indexByValue = useMemo(() => {
    const map = new Map();
    options.forEach((item, index) => map.set(item.value, index));
    return map;
  }, [options]);

  function moveFocus(currentIndex, offset) {
    if (!options.length) {
      return;
    }
    const next = Math.max(0, Math.min(options.length - 1, currentIndex + offset));
    const nextValue = options[next]?.value;
    if (nextValue && nextValue !== value) {
      onChange?.(nextValue);
    }
  }

  return (
    <nav className={classNames("ui-segment", className)} aria-label={ariaLabel}>
      {options.map((item, index) => {
        const active = item.value === value;
        return (
          <button
            key={item.value}
            type="button"
            className={classNames("ui-segment-btn", active ? "is-active" : "")}
            aria-pressed={active}
            disabled={item.disabled}
            onClick={() => onChange?.(item.value)}
            onKeyDown={(event) => {
              if (event.key === "ArrowRight") {
                event.preventDefault();
                moveFocus(index, 1);
              }
              if (event.key === "ArrowLeft") {
                event.preventDefault();
                moveFocus(index, -1);
              }
            }}
            data-index={indexByValue.get(item.value)}
          >
            {item.icon ? <span className="ui-segment-icon" aria-hidden="true">{item.icon}</span> : null}
            <span>{item.label}</span>
          </button>
        );
      })}
    </nav>
  );
}
